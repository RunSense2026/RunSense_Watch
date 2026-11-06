package com.bmb.runsensewear.data.run

import android.content.Context
import android.util.Log
import com.bmb.runsensewear.data.coach.CoachVoicePlayer
import com.bmb.runsensewear.data.coach.CoachingMessage
import com.bmb.runsensewear.data.coach.CoachingRepository
import com.bmb.runsensewear.data.exercise.RunSessionManager
import com.bmb.runsensewear.data.exercise.RunTrackingService
import com.bmb.runsensewear.data.profile.ProfileRepository
import com.bmb.runsensewear.data.sync.PhoneSyncContract
import com.bmb.runsensewear.data.sync.PhoneSyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

private const val TAG = "RunCoordinator"

/** 첫 코칭까지 대기 / 이후 코칭 간격 */
private const val FIRST_COACHING_DELAY_MS = 60_000L
private const val COACHING_INTERVAL_MS = 120_000L
private const val LIVE_SYNC_INTERVAL_MS = 5_000L

sealed interface SaveState {
    data object Idle : SaveState
    data object Saving : SaveState
    data class Done(val result: SaveResult) : SaveState
}

/**
 * 러닝 한 번의 "비즈니스 흐름"을 한 곳에서 조율합니다.
 *
 *  - 센서 확인 → 프로필/심박존 준비 → 러닝 시작 (포그라운드 서비스 + 폰 알림)
 *  - 러닝 중: 주기적 AI 코칭 요청 + 음성 안내, 폰으로 실시간 지표 전송, 인솔 샘플 수집
 *  - 종료: 서버 저장(POST /api/run) + 폰으로 요약 전송
 *
 * 폰 러닝 화면이 열려 있으면(폰이 기록 주체) 워치는 센서 값만 폰에 보내고
 * 코칭 생성·기록 저장은 폰에 맡깁니다. 폰이 만든 코칭은 워치 화면에도 표시됩니다.
 *
 * Activity/ViewModel보다 오래 살아야 해서 AppContainer 스코프에 둡니다.
 */
class RunCoordinator(
    private val context: Context,
    private val scope: CoroutineScope,
    val sessionManager: RunSessionManager,
    private val phoneSync: PhoneSyncManager,
    private val profileRepository: ProfileRepository,
    private val coachingRepository: CoachingRepository,
    private val runRepository: RunRepository,
    val voicePlayer: CoachVoicePlayer
) {
    private val _coachingLog = MutableStateFlow<List<CoachingMessage>>(emptyList())
    val coachingLog: StateFlow<List<CoachingMessage>> = _coachingLog

    /** 새 코칭이 도착했을 때 화면 전환용 이벤트 */
    private val _coachingEvents = MutableSharedFlow<CoachingMessage>(extraBufferCapacity = 1)
    val coachingEvents: SharedFlow<CoachingMessage> = _coachingEvents

    private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveState: StateFlow<SaveState> = _saveState

    private val _latestInsole = MutableStateFlow<InsoleSample?>(null)
    val latestInsole: StateFlow<InsoleSample?> = _latestInsole

    val phoneRunActive: StateFlow<Boolean> get() = phoneSync.phoneRunActive

    private val insoleSamples = mutableListOf<InsoleSample>()
    private var runJobs = mutableListOf<Job>()
    private var coachingInFlight = false

    init {
        // 폰이 만든 코칭은 러닝 중일 때 워치 화면에도 띄웁니다 (음성은 폰이 재생).
        scope.launch {
            phoneSync.phoneCoaching.collect { c ->
                if (sessionManager.session.value.phase != RunPhase.Running) return@collect
                val message = CoachingMessage(
                    text = c.feedbackText.trim(),
                    focusPoint = c.focusPoint,
                    recExercise = c.recExercise,
                    voiceUrl = null,
                    fromPhone = true
                )
                _coachingLog.update { it + message }
                _coachingEvents.tryEmit(message)
            }
        }
    }

    /** 02 센서 확인 화면 진입: 프로필을 받아 심박존을 맞추고 센서를 예열합니다. */
    suspend fun prepare() {
        _saveState.value = SaveState.Idle
        _coachingLog.value = emptyList()
        insoleSamples.clear()
        _latestInsole.value = null

        sessionManager.zones = profileRepository.zones
        scope.launch {
            profileRepository.refresh()
            sessionManager.zones = profileRepository.zones
        }
        phoneSync.requestInsoleStatus()
        sessionManager.prepare()
    }

    suspend fun startRun() {
        sessionManager.start()
        RunTrackingService.start(context)
        val s = sessionManager.session.value
        phoneSync.sendRunState("started", s.elapsedSeconds, s.distanceKm)
        startRunLoops()
    }

    suspend fun pause() {
        sessionManager.pause()
        val s = sessionManager.session.value
        phoneSync.sendRunState("paused", s.elapsedSeconds, s.distanceKm)
    }

    suspend fun resume() {
        sessionManager.resume()
        val s = sessionManager.session.value
        phoneSync.sendRunState("resumed", s.elapsedSeconds, s.distanceKm)
    }

    /** 종료 + 저장. 완료 화면은 saveState로 진행 상황을 보여 줍니다. */
    suspend fun finish() {
        stopRunLoops()
        voicePlayer.stop()
        val session = sessionManager.end()
        RunTrackingService.stop(context)
        phoneSync.sendRunState("ended", session.elapsedSeconds, session.distanceKm)

        val formScore = currentFormScore(session)
        val result: SaveResult = if (phoneSync.phoneRunActive.value) {
            // 폰 러닝 화면이 기록 주체 — 워치는 저장하지 않고 요약만 넘깁니다 (중복 저장 방지).
            SaveResult.DelegatedToPhone
        } else {
            _saveState.value = SaveState.Saving
            val insoleSummary = InsoleSummary.from(insoleSamples)
            val request = runRepository.buildRequest(session, formScore, insoleSummary, _coachingLog.value)
            runRepository.save(request)
        }
        _saveState.value = SaveState.Done(result)

        val activityId = (result as? SaveResult.Saved)?.activityId
        phoneSync.sendJson(
            PhoneSyncContract.RUN_COMPLETE,
            """{"distanceKm":${session.distanceKm},"elapsedSeconds":${session.elapsedSeconds},""" +
                """"avgPaceSecPerKm":${session.avgPaceSecPerKm ?: 0},"avgHeartRate":${session.avgHeartRate},""" +
                """"formScore":$formScore,"saved":${result is SaveResult.Saved},"activityId":${activityId ?: "null"}}"""
        )
    }

    /** 완료 화면 확인 → 대기화면 */
    fun reset() {
        stopRunLoops()
        sessionManager.reset()
        _saveState.value = SaveState.Idle
    }

    fun openReportOnPhone() {
        val activityId = ((_saveState.value as? SaveState.Done)?.result as? SaveResult.Saved)?.activityId
        phoneSync.sendJson(PhoneSyncContract.OPEN_REPORT, """{"activityId":${activityId ?: "null"}}""")
    }

    fun currentFormScore(session: RunSession = sessionManager.session.value): Int =
        formScoreOf(
            cadence = if (session.cadence > 0) session.cadence else session.avgCadence,
            insole = _latestInsole.value
        )

    // ── 러닝 중 백그라운드 루프 ────────────────────────────

    private fun startRunLoops() {
        stopRunLoops()
        runJobs += scope.launch { coachingLoop() }
        runJobs += scope.launch { liveSyncLoop() }
        runJobs += scope.launch { collectInsole() }
    }

    private fun stopRunLoops() {
        runJobs.forEach { it.cancel() }
        runJobs.clear()
    }

    private suspend fun collectInsole() {
        phoneSync.insoleSample.collect { sample ->
            if (sample == null) return@collect
            _latestInsole.value = sample
            if (sessionManager.session.value.phase == RunPhase.Running) {
                insoleSamples += sample
                if (insoleSamples.size > 3600) insoleSamples.removeAt(0)
            }
        }
    }

    private suspend fun liveSyncLoop() {
        while (scope.isActive) {
            delay(LIVE_SYNC_INTERVAL_MS)
            val s = sessionManager.session.value
            if (!s.isActive) break
            phoneSync.sendJson(
                PhoneSyncContract.RUN_LIVE,
                """{"phase":"${s.phase.name}","elapsedSeconds":${s.elapsedSeconds},"distanceKm":${s.distanceKm},""" +
                    """"paceSecPerKm":${s.currentPaceSecPerKm ?: 0},"heartRate":${s.heartRate},""" +
                    """"cadence":${s.cadence},"formScore":${currentFormScore(s)}}"""
            )
        }
    }

    private suspend fun coachingLoop() {
        delay(FIRST_COACHING_DELAY_MS)
        while (scope.isActive) {
            val s = sessionManager.session.value
            if (!s.isActive) break
            // 폰 러닝 화면이 열려 있으면 폰이 코칭을 만들어 보내 주므로 워치는 요청하지 않습니다.
            if (s.phase == RunPhase.Running && !phoneSync.phoneRunActive.value) requestCoachingNow()
            delay(COACHING_INTERVAL_MS)
        }
    }

    /** 즉시 코칭 요청 (주기 루프와 화면의 수동 요청이 공유) */
    suspend fun requestCoachingNow() {
        if (coachingInFlight) return
        coachingInFlight = true
        try {
            val s = sessionManager.session.value
            val pace = s.currentPaceSecPerKm ?: s.avgPaceSecPerKm ?: 0.0
            // Render 무료 티어 콜드 스타트 + Gemini + TTS 합성까지 1분을 넘길 수 있습니다.
            val message = withTimeoutOrNull(120_000) {
                coachingRepository.requestCoaching(
                    paceSecPerKm = pace,
                    heartRate = s.heartRate,
                    cadence = if (s.cadence > 0) s.cadence else s.avgCadence,
                    insole = _latestInsole.value,
                    profile = profileRepository.context
                )
            }
            if (message == null) {
                Log.w(TAG, "코칭 응답 없음")
                return
            }
            // 러닝이 끝났거나 일시정지 중이면 알림을 띄우지 않습니다.
            if (sessionManager.session.value.phase != RunPhase.Running) return
            _coachingLog.update { it + message }
            _coachingEvents.tryEmit(message)
            voicePlayer.speak(message)
        } finally {
            coachingInFlight = false
        }
    }
}
