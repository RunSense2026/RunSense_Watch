package com.bmb.runsensewear.data.exercise

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.health.services.client.ExerciseUpdateCallback
import androidx.health.services.client.HealthServices
import androidx.health.services.client.data.Availability
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.DataTypeAvailability
import androidx.health.services.client.data.ExerciseConfig
import androidx.health.services.client.data.ExerciseLapSummary
import androidx.health.services.client.data.ExerciseTrackedStatus
import androidx.health.services.client.data.ExerciseType
import androidx.health.services.client.data.ExerciseUpdate
import androidx.health.services.client.data.LocationAvailability
import androidx.health.services.client.data.WarmUpConfig
import androidx.health.services.client.endExercise
import androidx.health.services.client.getCapabilities
import androidx.health.services.client.getCurrentExerciseInfo
import androidx.health.services.client.pauseExercise
import androidx.health.services.client.prepareExercise
import androidx.health.services.client.resumeExercise
import androidx.health.services.client.startExercise
import com.bmb.runsensewear.data.run.HeartRateZones
import com.bmb.runsensewear.data.run.LapSplit
import com.bmb.runsensewear.data.run.RunPhase
import com.bmb.runsensewear.data.run.RunSession
import com.bmb.runsensewear.data.run.paceStateOf
import com.bmb.runsensewear.data.sensor.HeartRateSensorManager
import com.bmb.runsensewear.data.sensor.HeartRateState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Duration
import java.time.Instant
import kotlin.math.roundToInt

private const val TAG = "RunSessionManager"

/**
 * Health Services ExerciseClient로 러닝 한 세션을 추적합니다.
 *
 * 거리·페이스(GPS)·심박·케이던스·칼로리를 받아 [session]으로 내보내고,
 * 1km마다 구간 기록을 쌓습니다. 워치가 Health Services 러닝을 지원하지 않거나
 * 시작에 실패하면(일부 에뮬레이터) 시간 + MeasureClient 심박만 재는 폴백 모드로 동작합니다.
 *
 * 앱 프로세스가 죽어도 Health Services가 세션을 유지하므로, 다시 뜰 때
 * [restoreIfRunning]으로 콜백을 다시 붙입니다.
 */
class RunSessionManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val exerciseClient = HealthServices.getClient(context).exerciseClient
    private val heartRateSensor = HeartRateSensorManager(context)

    private val _session = MutableStateFlow(RunSession())
    val session: StateFlow<RunSession> = _session

    var zones: HeartRateZones = HeartRateZones()

    private var tickerJob: Job? = null
    private var fallbackHeartRateJob: Job? = null
    private var callbackRegistered = false

    /** Health Services가 준 마지막 활동시간 체크포인트. 일시정지 동안은 멈춥니다. */
    private var checkpoint: ExerciseUpdate.ActiveDurationCheckpoint? = null

    /** 폴백 모드용 수동 스톱워치 */
    private var fallbackActiveMillis = 0L
    private var fallbackResumedAt = 0L

    // 1km 구간 계산용
    private var lapStartDistance = 0.0
    private var lapStartElapsed = 0
    private var lapHrSum = 0L
    private var lapHrSamples = 0

    private val hasLocationPermission: Boolean
        get() = context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    private val callback = object : ExerciseUpdateCallback {
        override fun onExerciseUpdateReceived(update: ExerciseUpdate) = handleUpdate(update)

        override fun onLapSummaryReceived(lapSummary: ExerciseLapSummary) = Unit

        override fun onRegistered() {
            callbackRegistered = true
        }

        override fun onRegistrationFailed(throwable: Throwable) {
            Log.w(TAG, "ExerciseUpdateCallback 등록 실패", throwable)
        }

        override fun onAvailabilityChanged(dataType: DataType<*, *>, availability: Availability) {
            when (dataType) {
                DataType.LOCATION -> _session.update {
                    it.copy(gpsReady = availability == LocationAvailability.ACQUIRED_TETHERED ||
                        availability == LocationAvailability.ACQUIRED_UNTETHERED)
                }

                DataType.HEART_RATE_BPM -> if (availability is DataTypeAvailability) {
                    val state = when (availability) {
                        DataTypeAvailability.AVAILABLE -> return
                        DataTypeAvailability.ACQUIRING -> HeartRateState.Acquiring
                        DataTypeAvailability.UNAVAILABLE_DEVICE_OFF_BODY -> HeartRateState.OffBody
                        else -> HeartRateState.Unavailable
                    }
                    _session.update { it.copy(heartRateState = state) }
                }
            }
        }
    }

    // ── 공개 API ─────────────────────────────────────────

    /** 이 워치가 Health Services 러닝 추적을 지원하는지 */
    suspend fun isRunningSupported(): Boolean = runCatching {
        ExerciseType.RUNNING in exerciseClient.getCapabilities().supportedExerciseTypes
    }.getOrElse {
        Log.w(TAG, "ExerciseCapabilities 조회 실패", it)
        false
    }

    /**
     * 센서 예열 — GPS 고정과 심박 센서를 미리 켜 둡니다. (02 센서 확인 화면)
     * 실패해도 러닝 시작은 가능하므로 예외를 삼킵니다.
     */
    suspend fun prepare() {
        if (_session.value.isActive) return
        _session.update { RunSession(phase = RunPhase.Preparing) }
        runCatching {
            exerciseClient.setUpdateCallback(callback)
            val types = buildSet {
                add(DataType.HEART_RATE_BPM)
                if (hasLocationPermission) add(DataType.LOCATION)
            }
            exerciseClient.prepareExercise(WarmUpConfig(ExerciseType.RUNNING, types))
        }.onFailure { Log.w(TAG, "prepareExercise 실패 (무시)", it) }
    }

    /** 러닝 시작. Health Services 시작에 실패하면 폴백 모드로 시작합니다. */
    suspend fun start() {
        if (_session.value.phase == RunPhase.Running) return
        resetLapCounters()

        val started = runCatching {
            if (!isRunningSupported()) error("RUNNING 미지원 기기")
            exerciseClient.setUpdateCallback(callback)
            val config = ExerciseConfig.builder(ExerciseType.RUNNING)
                .setDataTypes(
                    setOf(
                        DataType.HEART_RATE_BPM,
                        DataType.DISTANCE_TOTAL,
                        DataType.SPEED,
                        DataType.PACE,
                        DataType.STEPS_PER_MINUTE,
                        DataType.CALORIES_TOTAL,
                        DataType.LOCATION
                    )
                )
                .setIsGpsEnabled(hasLocationPermission)
                .setIsAutoPauseAndResumeEnabled(false)
                .build()
            withTimeoutOrNull(10_000) { exerciseClient.startExercise(config) }
                ?: error("startExercise 응답 없음")
        }.isSuccess

        checkpoint = null
        _session.update {
            RunSession(
                phase = RunPhase.Running,
                startedAtMillis = System.currentTimeMillis(),
                fallbackMode = !started
            )
        }
        if (!started) startFallback()
        startTicker()
    }

    suspend fun pause() {
        val s = _session.value
        if (s.phase != RunPhase.Running) return
        if (s.fallbackMode) {
            fallbackActiveMillis += System.currentTimeMillis() - fallbackResumedAt
        } else {
            runCatching { exerciseClient.pauseExercise() }
                .onFailure { Log.w(TAG, "pauseExercise 실패", it) }
        }
        _session.update { it.copy(phase = RunPhase.Paused) }
    }

    suspend fun resume() {
        val s = _session.value
        if (s.phase != RunPhase.Paused) return
        if (s.fallbackMode) {
            fallbackResumedAt = System.currentTimeMillis()
        } else {
            runCatching { exerciseClient.resumeExercise() }
                .onFailure { Log.w(TAG, "resumeExercise 실패", it) }
        }
        _session.update { it.copy(phase = RunPhase.Running) }
    }

    /** 러닝 종료. 마지막 미완성 구간도 기록에 포함합니다. */
    suspend fun end(): RunSession {
        val s = _session.value
        if (!s.isActive) return s
        _session.update { it.copy(phase = RunPhase.Ending) }
        tickerJob?.cancel()
        fallbackHeartRateJob?.cancel()

        if (s.fallbackMode) {
            if (s.phase == RunPhase.Running) {
                fallbackActiveMillis += System.currentTimeMillis() - fallbackResumedAt
            }
        } else {
            runCatching { withTimeoutOrNull(5_000) { exerciseClient.endExercise() } }
                .onFailure { Log.w(TAG, "endExercise 실패", it) }
            runCatching { exerciseClient.clearUpdateCallbackAsync(callback) }
        }
        callbackRegistered = false

        closeOpenLap(force = true)
        _session.update { it.copy(phase = RunPhase.Completed) }
        return _session.value
    }

    /** 완료 화면을 닫고 대기화면으로 돌아갈 때 */
    fun reset() {
        tickerJob?.cancel()
        fallbackHeartRateJob?.cancel()
        _session.value = RunSession()
    }

    /** 프로세스가 다시 떴을 때 Health Services에 살아 있는 세션이 있으면 다시 붙습니다. */
    @SuppressLint("RestrictedApi") // ExerciseTrackedStatus 상수가 라이브러리 내부용으로 표시돼 있지만 공개 API 값입니다.
    suspend fun restoreIfRunning() {
        val info = runCatching { exerciseClient.getCurrentExerciseInfo() }.getOrNull() ?: return
        if (info.exerciseTrackedStatus != ExerciseTrackedStatus.OWNED_EXERCISE_IN_PROGRESS) return
        Log.i(TAG, "진행 중이던 러닝 세션 복구")
        runCatching { exerciseClient.setUpdateCallback(callback) }
        _session.update { it.copy(phase = RunPhase.Running) }
        startTicker()
    }

    // ── 내부 ─────────────────────────────────────────────

    private fun handleUpdate(update: ExerciseUpdate) {
        val state = update.exerciseStateInfo.state
        val metrics = update.latestMetrics

        checkpoint = update.activeDurationCheckpoint ?: checkpoint

        val hr = metrics.getData(DataType.HEART_RATE_BPM).lastOrNull()?.value?.roundToInt()
        val speed = metrics.getData(DataType.SPEED).lastOrNull()?.value
        val cadence = metrics.getData(DataType.STEPS_PER_MINUTE).lastOrNull()?.value?.toInt()
        val distance = metrics.getData(DataType.DISTANCE_TOTAL)?.total
        val calories = metrics.getData(DataType.CALORIES_TOTAL)?.total

        _session.update { s ->
            var next = s
            if (hr != null && hr > 0) {
                val zone = zones.zoneOf(hr)
                val zoneSamples = s.zoneSamples.toMutableList()
                if (zone in 1..5) zoneSamples[zone - 1]++
                lapHrSum += hr
                lapHrSamples++
                next = next.copy(
                    heartRate = hr,
                    heartRateState = HeartRateState.Measuring(hr),
                    zoneSamples = zoneSamples,
                    heartRateSum = s.heartRateSum + hr,
                    heartRateSamples = s.heartRateSamples + 1
                )
            }
            if (cadence != null && cadence > 0) {
                next = next.copy(
                    cadence = cadence,
                    cadenceSum = s.cadenceSum + cadence,
                    cadenceSamples = s.cadenceSamples + 1
                )
            }
            if (speed != null) {
                // 0.5 m/s(≈33분/km) 미만은 걷기·정지로 보고 페이스를 비웁니다.
                next = next.copy(currentPaceSecPerKm = if (speed >= 0.5) 1000.0 / speed else null)
            }
            if (distance != null) next = next.copy(distanceMeters = distance)
            if (calories != null) next = next.copy(calories = calories)

            // 사용자가 누른 일시정지/재개는 이미 반영돼 있고, 여기서는 HS 쪽 상태만 동기화합니다.
            next = when {
                state.isEnded && s.isActive -> next.copy(phase = RunPhase.Ending)
                state.isPaused && s.phase == RunPhase.Running -> next.copy(phase = RunPhase.Paused)
                state.isResuming && s.phase == RunPhase.Paused -> next.copy(phase = RunPhase.Running)
                else -> next
            }
            next
        }
        closeOpenLap(force = false)
    }

    /** 1초마다 경과시간을 갱신합니다. 체크포인트 기반이라 일시정지 중엔 자연히 멈춥니다. */
    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (isActive) {
                val s = _session.value
                if (!s.isActive) break
                val elapsed = if (s.fallbackMode) fallbackElapsedSeconds(s)
                else checkpointElapsedSeconds(s)
                if (elapsed != s.elapsedSeconds) {
                    _session.update { it.copy(elapsedSeconds = elapsed) }
                }
                delay(1000)
            }
        }
    }

    private fun checkpointElapsedSeconds(s: RunSession): Int {
        val cp = checkpoint
        if (cp == null) {
            // 아직 첫 업데이트 전이면 시작 시각으로 대략 계산
            val start = s.startedAtMillis ?: return s.elapsedSeconds
            return if (s.phase == RunPhase.Running) ((System.currentTimeMillis() - start) / 1000).toInt()
            else s.elapsedSeconds
        }
        val active = cp.activeDuration
        val extra = if (s.phase == RunPhase.Running) Duration.between(cp.time, Instant.now()) else Duration.ZERO
        return (active + extra).seconds.toInt().coerceAtLeast(0)
    }

    private fun fallbackElapsedSeconds(s: RunSession): Int {
        val running = if (s.phase == RunPhase.Running) System.currentTimeMillis() - fallbackResumedAt else 0L
        return ((fallbackActiveMillis + running) / 1000).toInt()
    }

    private fun startFallback() {
        fallbackActiveMillis = 0L
        fallbackResumedAt = System.currentTimeMillis()
        fallbackHeartRateJob?.cancel()
        fallbackHeartRateJob = scope.launch {
            heartRateSensor.heartRateFlow().collect { state ->
                val bpm = state.bpmOrZero
                _session.update { s ->
                    if (bpm <= 0) s.copy(heartRateState = state)
                    else {
                        val zone = zones.zoneOf(bpm)
                        val zoneSamples = s.zoneSamples.toMutableList()
                        if (zone in 1..5 && s.phase == RunPhase.Running) zoneSamples[zone - 1]++
                        lapHrSum += bpm
                        lapHrSamples++
                        s.copy(
                            heartRate = bpm,
                            heartRateState = state,
                            zoneSamples = zoneSamples,
                            heartRateSum = s.heartRateSum + bpm,
                            heartRateSamples = s.heartRateSamples + 1
                        )
                    }
                }
            }
        }
    }

    private fun resetLapCounters() {
        lapStartDistance = 0.0
        lapStartElapsed = 0
        lapHrSum = 0
        lapHrSamples = 0
    }

    /** 1km를 넘겼거나(force=false) 종료 시점(force=true)에 구간을 마감합니다. */
    private fun closeOpenLap(force: Boolean) {
        val s = _session.value
        val lapDistance = s.distanceMeters - lapStartDistance
        if (!force && lapDistance < 1000.0) return
        if (force && lapDistance < 50.0) return  // 너무 짧은 꼬리 구간은 버립니다

        val lapSeconds = (s.elapsedSeconds - lapStartElapsed).coerceAtLeast(1)
        val pace = lapSeconds / (lapDistance / 1000.0)
        val avgHr = if (lapHrSamples == 0) s.heartRate else (lapHrSum / lapHrSamples).toInt()
        val lap = LapSplit(
            lapNumber = s.laps.size + 1,
            paceSecPerKm = pace,
            avgHeartRate = avgHr,
            paceState = paceStateOf(pace, s.avgPaceSecPerKm, s.distanceMeters)
        )
        _session.update { it.copy(laps = it.laps + lap) }

        lapStartDistance = s.distanceMeters
        lapStartElapsed = s.elapsedSeconds
        lapHrSum = 0
        lapHrSamples = 0
    }
}
