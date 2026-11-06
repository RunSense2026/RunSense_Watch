package com.bmb.runsensewear.data.coach

import android.util.Log
import com.bmb.runsensewear.data.auth.TokenStore
import com.bmb.runsensewear.data.network.CoachingRequest
import com.bmb.runsensewear.data.network.CoachingResponse
import com.bmb.runsensewear.data.network.FootData
import com.bmb.runsensewear.data.network.FootHitmap
import com.bmb.runsensewear.data.network.NetworkModule
import com.bmb.runsensewear.data.network.UserProfileContext
import com.bmb.runsensewear.data.run.InsoleSample
import kotlin.math.roundToInt

private const val TAG = "CoachingRepository"

/** 한 번의 AI 코칭 결과 (화면 표시 + 완료 시 서버 저장용) */
data class CoachingMessage(
    val text: String,
    val focusPoint: String?,
    val recExercise: String?,
    val voiceUrl: String?,
    /** 폰 앱이 만들어 보낸 코칭인지 (음성은 폰이 재생) */
    val fromPhone: Boolean = false,
    val receivedAtMillis: Long = System.currentTimeMillis()
)

/**
 * 인솔 미연결 시 보내는 중립 족압 분포 — 폰 앱(insoleMetrics.ts NEUTRAL_FOOT)과 같은 값.
 * AI 서버가 양발을 필수로 요구해 요청이 거절되지 않게 합니다.
 */
private val NeutralFoot = FootData(
    hitmap = FootHitmap(heel = 46, forefootLeft = 22, forefootRight = 20, midfootLeft = 16, midfootRight = 16),
    pronationType = "neutral",
    heelStrikeRate = 46
)

/**
 * 실시간 AI 코칭.
 *
 * 로그인돼 있으면 백엔드 프록시(/api/run/coaching)를 씁니다 — 서버가 프로필을 붙여 줍니다.
 * 로그인 전이거나 백엔드가 실패하면 AI 서버(/ai/coaching)를 직접 호출합니다.
 */
class CoachingRepository(
    private val network: NetworkModule,
    private val tokenStore: TokenStore
) {
    suspend fun requestCoaching(
        paceSecPerKm: Double,
        heartRate: Int,
        cadence: Int,
        insole: InsoleSample?,
        profile: UserProfileContext?
    ): CoachingMessage? {
        val body = CoachingRequest(
            // 폰 앱(spkToMinutes)과 같은 단위: 분/km 실수, 소수 둘째 자리 (예: 329초 → 5.48)
            pace = if (paceSecPerKm > 0) (paceSecPerKm / 60.0 * 100).roundToInt() / 100.0 else 0.0,
            heartRate = heartRate,
            cadence = cadence,
            leftFoot = insole?.left?.takeIf { it.hitmap.total > 0 } ?: NeutralFoot,
            rightFoot = insole?.right?.takeIf { it.hitmap.total > 0 } ?: NeutralFoot
        )

        val response: CoachingResponse? =
            if (tokenStore.current.isLoggedIn) {
                runCatching { network.backendApi.coaching(body).data }
                    .onFailure { Log.w(TAG, "백엔드 코칭 실패, AI 서버로 폴백", it) }
                    .getOrNull()
            } else null

        val finalResponse = response ?: runCatching {
            network.aiApi.coaching(body.copy(profile = profile))
        }.onFailure { Log.w(TAG, "AI 서버 코칭 실패", it) }.getOrNull()

        return finalResponse?.let {
            CoachingMessage(
                text = it.feedbackText.trim(),
                focusPoint = it.focusPoint,
                recExercise = it.recExercise,
                voiceUrl = network.resolveVoiceUrl(it.voiceFile)
            )
        }
    }
}
