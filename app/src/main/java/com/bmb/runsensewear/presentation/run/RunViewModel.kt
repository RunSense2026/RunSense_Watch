package com.bmb.runsensewear.presentation.run

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bmb.runsensewear.RunSenseApp
import com.bmb.runsensewear.data.coach.CoachingMessage
import com.bmb.runsensewear.data.run.InsoleStatus
import com.bmb.runsensewear.data.run.PaceState
import com.bmb.runsensewear.data.run.RunPhase
import com.bmb.runsensewear.data.run.SaveState
import com.bmb.runsensewear.data.run.formatElapsed
import com.bmb.runsensewear.data.run.formatPace
import com.bmb.runsensewear.data.run.paceStateOf
import com.bmb.runsensewear.data.sensor.HeartRateSensorManager
import com.bmb.runsensewear.data.sensor.HeartRateState
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** 02 센서 확인 화면의 한 줄 */
enum class SensorStatus { Waiting, Checking, Connected, Unavailable }

@Immutable
data class SensorRow(val badge: String, val label: String, val status: SensorStatus)

@Immutable
data class SensorCheckState(
    val rows: List<SensorRow> = listOf(
        SensorRow("L", "왼쪽 인솔", SensorStatus.Waiting),
        SensorRow("R", "오른쪽 인솔", SensorStatus.Waiting),
        SensorRow("G", "워치 GPS", SensorStatus.Waiting)
    ),
    val headline: String = "센서 연결 확인 중…"
)

/** 화면들이 그대로 그릴 수 있게 다듬은 상태 */
@Immutable
data class RunUiState(
    val phase: RunPhase = RunPhase.Idle,
    val elapsedSeconds: Int = 0,
    val elapsedText: String = "00:00",
    val distanceKm: Double = 0.0,
    val paceText: String = "--'--\"",
    val paceState: PaceState = PaceState.Good,
    val heartRate: Int = 0,
    val avgHeartRate: Int = 0,
    /** 측정이 안 될 때 화면에 띄울 안내 문구. 정상 측정 중이면 null */
    val heartRateLabel: String? = null,
    val zone: Int = 0,
    val zoneShares: List<Float> = List(5) { 0f },
    val lthr: Int = 0,
    val cadence: Int = 0,
    val calories: Int = 0,
    val formScore: Int = 0,
    val balancePercent: Int = 50,
    val insoleStatus: InsoleStatus = InsoleStatus(),
    val phoneConnected: Boolean = false,
    val loggedIn: Boolean = false,
    /** 폰 러닝 화면이 열려 있어 폰이 기록 주체인지 */
    val phoneRunActive: Boolean = false,
    /** 폰에서 정한 목표 페이스(초/km). 없으면 null → 세션 평균 기준으로 링을 그립니다 */
    val targetPaceSecPerKm: Double? = null,
    val currentPaceSecPerKm: Double? = null,
    val avgPaceSecPerKm: Double? = null,
    val userName: String? = null,
    val heartRateReady: Boolean = false,
    val gpsReady: Boolean = false,
    val coaching: CoachingMessage? = null,
    val speaking: Boolean = false,
    val saveState: SaveState = SaveState.Idle,
    val sensorCheck: SensorCheckState = SensorCheckState()
)

/** 화면 전환이 필요한 일회성 사건 */
sealed interface RunEvent {
    data object RunStarted : RunEvent
    data class CoachingArrived(val message: CoachingMessage) : RunEvent
}

class RunViewModel(app: Application) : AndroidViewModel(app) {

    private val container = (app as RunSenseApp).container
    private val coordinator = container.runCoordinator
    private val phoneSync = container.phoneSyncManager
    private val heartRateSensor = HeartRateSensorManager(app)

    private val _uiState = MutableStateFlow(RunUiState())
    val uiState: StateFlow<RunUiState> = _uiState

    private val events = Channel<RunEvent>(Channel.BUFFERED)
    val eventFlow: Flow<RunEvent> = events.receiveAsFlow()

    private val sensorCheck = MutableStateFlow(SensorCheckState())
    private var heartRateReady = false
    private var sensorCheckJob: Job? = null

    init {
        listOf(
            coordinator.sessionManager.session,
            coordinator.latestInsole,
            coordinator.coachingLog,
            coordinator.saveState,
            coordinator.voicePlayer.speaking,
            phoneSync.insoleStatus,
            phoneSync.phoneConnected,
            container.tokenStore.state,
            container.profileRepository.profile,
            phoneSync.phoneRunActive,
            sensorCheck
        ).forEach { flow -> flow.onEach { recompute() }.launchIn(viewModelScope) }

        coordinator.coachingEvents
            .onEach { events.send(RunEvent.CoachingArrived(it)) }
            .launchIn(viewModelScope)

        refreshSensorReadiness()
    }

    // ── 화면에서 부르는 동작 ─────────────────────────────

    /** 01 대기화면이 다시 보일 때: 권한 결과·폰 연결을 다시 확인 */
    fun refreshSensorReadiness() {
        viewModelScope.launch {
            heartRateReady = heartRateSensor.hasPermission() && heartRateSensor.isSupported()
            phoneSync.refreshConnection()
            phoneSync.requestInsoleStatus()
            recompute()
        }
    }

    /**
     * 02 센서 확인: 인솔(폰 경유) → GPS 순서로 확인하고 러닝을 시작합니다.
     * 폰이나 인솔이 없어도 워치 단독으로 러닝을 시작할 수 있게 시간 제한을 둡니다.
     */
    fun beginSensorCheck() {
        if (sensorCheckJob?.isActive == true) return
        sensorCheckJob = viewModelScope.launch {
            sensorCheck.value = SensorCheckState()
            coordinator.prepare()

            val phone = phoneSync.refreshConnection()
            phoneSync.requestInsoleStatus()

            // 왼쪽 인솔
            setRow(0, SensorStatus.Checking)
            val status = withTimeoutOrNull(if (phone) 6_000 else 2_500) {
                phoneSync.insoleStatus.first { it.known }
            } ?: InsoleStatus()
            delay(500)
            setRow(0, if (status.leftConnected) SensorStatus.Connected else SensorStatus.Unavailable)

            // 오른쪽 인솔 (같은 메시지로 오지만 순차 연출)
            setRow(1, SensorStatus.Checking)
            delay(650)
            setRow(1, if (status.rightConnected) SensorStatus.Connected else SensorStatus.Unavailable)

            // 워치 GPS
            setRow(2, SensorStatus.Checking)
            val hasLocation = getApplication<Application>()
                .checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            val gps = if (!hasLocation) false else withTimeoutOrNull(6_000) {
                coordinator.sessionManager.session.first { it.gpsReady }
                true
            } ?: false
            delay(400)
            setRow(2, if (gps) SensorStatus.Connected else SensorStatus.Unavailable)

            sensorCheck.value = sensorCheck.value.copy(headline = "러닝을 시작할게요")
            delay(600)
            coordinator.startRun()
            events.send(RunEvent.RunStarted)
        }
    }

    fun pause() = viewModelScope.launch { coordinator.pause() }

    fun resume() = viewModelScope.launch { coordinator.resume() }

    /** 종료 → 완료 화면. 저장은 앱 스코프에서 이어서 진행됩니다. */
    fun finish() {
        container.appScope.launch { coordinator.finish() }
    }

    fun done() {
        coordinator.reset()
        sensorCheck.value = SensorCheckState()
    }

    fun openReportOnPhone() = coordinator.openReportOnPhone()

    /** AI 코치 페이지에서 눌러 즉시 코칭을 받을 때 */
    fun requestCoachingNow() = viewModelScope.launch { coordinator.requestCoachingNow() }

    fun stopVoice() = coordinator.voicePlayer.stop()

    // ── 상태 계산 ───────────────────────────────────────

    private fun setRow(index: Int, status: SensorStatus) {
        sensorCheck.value = sensorCheck.value.copy(
            rows = sensorCheck.value.rows.mapIndexed { i, row ->
                if (i == index) row.copy(status = status) else row
            }
        )
    }

    private fun recompute() {
        val s = coordinator.sessionManager.session.value
        val insole = coordinator.latestInsole.value
        val auth = container.tokenStore.state.value
        val profile = container.profileRepository.current
        val currentPace = s.currentPaceSecPerKm

        _uiState.value = RunUiState(
            phase = s.phase,
            elapsedSeconds = s.elapsedSeconds,
            elapsedText = formatElapsed(s.elapsedSeconds),
            distanceKm = s.distanceKm,
            paceText = formatPace(currentPace ?: s.avgPaceSecPerKm),
            paceState = paceStateOf(currentPace, s.avgPaceSecPerKm, s.distanceMeters, phoneSync.targetPaceSecPerKm.value),
            heartRate = s.heartRate,
            avgHeartRate = s.avgHeartRate,
            heartRateLabel = when (s.heartRateState) {
                is HeartRateState.Measuring -> null
                HeartRateState.Acquiring -> "측정 대기"
                HeartRateState.OffBody -> "워치를 착용하세요"
                HeartRateState.Unavailable -> "측정 불가"
                HeartRateState.Unsupported -> "심박 센서 없음"
            },
            zone = coordinator.sessionManager.zones.zoneOf(s.heartRate),
            zoneShares = s.zoneShares,
            lthr = profile?.lthr ?: 0,
            cadence = s.cadence,
            calories = s.calories.toInt(),
            formScore = coordinator.currentFormScore(s),
            balancePercent = insole?.leftBalancePercent ?: 50,
            insoleStatus = phoneSync.insoleStatus.value,
            phoneConnected = phoneSync.phoneConnected.value,
            loggedIn = auth.isLoggedIn,
            phoneRunActive = coordinator.phoneRunActive.value,
            targetPaceSecPerKm = phoneSync.targetPaceSecPerKm.value,
            currentPaceSecPerKm = currentPace,
            avgPaceSecPerKm = s.avgPaceSecPerKm,
            userName = auth.userName ?: profile?.userName,
            heartRateReady = heartRateReady,
            gpsReady = s.gpsReady,
            coaching = coordinator.coachingLog.value.lastOrNull(),
            speaking = coordinator.voicePlayer.speaking.value,
            saveState = coordinator.saveState.value,
            sensorCheck = sensorCheck.value
        )
    }
}
