package com.bmb.runsensewear.data.run

import android.content.Context
import android.util.Log
import com.bmb.runsensewear.data.auth.TokenStore
import com.bmb.runsensewear.data.coach.CoachingMessage
import com.bmb.runsensewear.data.network.AiCoachingRequest
import com.bmb.runsensewear.data.network.FootHitmap
import com.bmb.runsensewear.data.network.FootSensorDataRequest
import com.bmb.runsensewear.data.network.LapSplitRequest
import com.bmb.runsensewear.data.network.NetworkModule
import com.bmb.runsensewear.data.network.RunningActivityCreateRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.encodeToString

private const val TAG = "RunRepository"

/** 서버 저장 결과 */
sealed interface SaveResult {
    data object NotLoggedIn : SaveResult
    /** 폰 러닝 화면이 열려 있어 기록 저장을 폰에 맡김 */
    data object DelegatedToPhone : SaveResult
    data class Saved(val activityId: Long?) : SaveResult
    data class Failed(val reason: String) : SaveResult
}

/**
 * 러닝 완료 기록을 백엔드(POST /api/run)에 저장합니다.
 * 실패하면 워치에 보관했다가 다음 앱 실행 때 다시 보냅니다.
 */
class RunRepository(
    context: Context,
    private val network: NetworkModule,
    private val tokenStore: TokenStore,
    private val scope: CoroutineScope
) {
    private val prefs = context.applicationContext
        .getSharedPreferences("runsense_pending_runs", Context.MODE_PRIVATE)
    private val pendingMutex = Mutex()

    /** 세션 원본 → 백엔드 DTO */
    fun buildRequest(
        session: RunSession,
        formScore: Int,
        insoleSummary: InsoleSummary,
        coachings: List<CoachingMessage>
    ): RunningActivityCreateRequest = RunningActivityCreateRequest(
        distance = (session.distanceKm * 100).toInt() / 100.0,
        time = formatElapsedForApi(session.elapsedSeconds),
        pace = formatPaceForApi(session.avgPaceSecPerKm),
        formScore = formScore.coerceIn(0, 100),
        // 서버 제약: 최대 5개
        lapSplits = session.laps.take(5).map {
            LapSplitRequest(
                lapNumber = it.lapNumber.toLong(),
                lapPace = formatPaceForApi(it.paceSecPerKm),
                avgHeartRate = it.avgHeartRate,
                paceStatue = it.paceState.apiValue
            )
        },
        // 서버 제약: 정확히 2개 (Left, Right)
        footSensorData = listOf(
            insoleSummary.toRequest("Left", insoleSummary.left),
            insoleSummary.toRequest("Right", insoleSummary.right)
        ),
        // 서버 제약: 최대 3개 → 가장 최근 3개
        aiCoachings = coachings.takeLast(3).map {
            AiCoachingRequest(
                feedbackText = it.text,
                focusPoint = it.focusPoint,
                recExercise = it.recExercise
            )
        }
    )

    suspend fun save(request: RunningActivityCreateRequest): SaveResult {
        if (!tokenStore.current.isLoggedIn) {
            // 로그인 뒤 재시도할 수 있도록 보관만 합니다.
            enqueuePending(request)
            return SaveResult.NotLoggedIn
        }
        return runCatching { network.backendApi.createRun(request) }
            .fold(
                onSuccess = { res ->
                    if (res.success) SaveResult.Saved(res.data?.activityId)
                    else SaveResult.Failed(res.message ?: "저장 실패").also { enqueuePending(request) }
                },
                onFailure = {
                    Log.w(TAG, "러닝 기록 저장 실패", it)
                    enqueuePending(request)
                    SaveResult.Failed(it.message ?: "네트워크 오류")
                }
            )
    }

    /** 보관해 둔 기록을 다시 보내 봅니다. */
    fun retryPending() {
        scope.launch(Dispatchers.IO) {
            if (!tokenStore.current.isLoggedIn) return@launch
            val pending = pendingMutex.withLock { readPending().also { writePending(emptyList()) } }
            pending.forEach { req ->
                val result = save(req)
                Log.i(TAG, "보관 기록 재전송: $result")
            }
        }
    }

    private suspend fun enqueuePending(request: RunningActivityCreateRequest) {
        pendingMutex.withLock {
            val list = readPending() + request
            writePending(list.takeLast(10))
        }
    }

    private fun readPending(): List<RunningActivityCreateRequest> =
        runCatching {
            prefs.getString(KEY_PENDING, null)?.let {
                network.json.decodeFromString(ListSerializer(RunningActivityCreateRequest.serializer()), it)
            }
        }.getOrNull() ?: emptyList()

    private fun writePending(list: List<RunningActivityCreateRequest>) {
        prefs.edit().putString(
            KEY_PENDING,
            network.json.encodeToString(ListSerializer(RunningActivityCreateRequest.serializer()), list)
        ).apply()
    }

    private companion object {
        const val KEY_PENDING = "pending"
    }
}

/** 러닝 동안 모은 족압 데이터 요약 (양발) */
data class InsoleSummary(
    val left: FootSummary = FootSummary(),
    val right: FootSummary = FootSummary()
) {
    data class FootSummary(
        val hitmap: FootHitmap = FootHitmap(),
        val pronationType: String? = null,
        val heelStrikeRate: Double = 0.0
    )

    fun toRequest(side: String, foot: FootSummary) = FootSensorDataRequest(
        footSide = side,
        pronationType = foot.pronationType ?: "미측정",
        heelStrikeRate = foot.heelStrikeRate,
        hitmapJson = """{"heel":${foot.hitmap.heel},"forefoot_left":${foot.hitmap.forefootLeft},""" +
            """"forefoot_right":${foot.hitmap.forefootRight},"midfoot_left":${foot.hitmap.midfootLeft},""" +
            """"midfoot_right":${foot.hitmap.midfootRight}}"""
    )

    companion object {
        fun from(samples: List<InsoleSample>): InsoleSummary {
            if (samples.isEmpty()) return InsoleSummary()
            fun side(pick: (InsoleSample) -> com.bmb.runsensewear.data.network.FootData): FootSummary {
                val feet = samples.map(pick)
                return FootSummary(
                    hitmap = averageHitmap(feet.map { it.hitmap }),
                    pronationType = feet.mapNotNull { it.pronationType }
                        .groupingBy { it }.eachCount().maxByOrNull { it.value }?.key,
                    heelStrikeRate = feet.mapNotNull { it.heelStrikeRate }
                        .takeIf { it.isNotEmpty() }?.average() ?: 0.0
                )
            }
            return InsoleSummary(left = side { it.left }, right = side { it.right })
        }
    }
}
