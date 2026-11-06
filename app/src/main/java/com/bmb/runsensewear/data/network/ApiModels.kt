package com.bmb.runsensewear.data.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/*
 * 런센스 백엔드(Spring) / AI 코칭 서버(FastAPI) DTO.
 *
 * 백엔드는 camelCase, AI 서버(및 백엔드의 코칭 프록시)는 snake_case를 씁니다.
 * 두 서버가 같은 모양을 쓰는 코칭 요청/응답은 하나의 클래스로 공유합니다.
 */

/** 백엔드 공통 응답 껍데기 `{ success, message, data }` */
@Serializable
data class ApiResponse<T>(
    val success: Boolean = false,
    val message: String? = null,
    val data: T? = null
)

// ── 인증 ─────────────────────────────────────────────

@Serializable
data class TokenReissueRequest(val refreshToken: String)

@Serializable
data class TokenResponse(
    val tokenType: String? = null,
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long = 3600
)

// ── 프로필 ───────────────────────────────────────────

/** GET /api/users — 심박존 계산에 필요한 LTHR·생년월일·안정시 심박을 가져옵니다. */
@Serializable
data class UserProfile(
    val userId: Long? = null,
    val userName: String? = null,
    val email: String? = null,
    val birthDate: String? = null,
    val gender: String? = null,
    val height: Double? = null,
    val weight: Double? = null,
    val restingHeartRate: Double? = null,
    val lthr: Int? = null
)

// ── 러닝 기록 저장 ───────────────────────────────────

@Serializable
data class RunningActivityCreateRequest(
    val distance: Double,
    val time: String,
    val pace: String,
    val formScore: Int,
    val lapSplits: List<LapSplitRequest>,
    val footSensorData: List<FootSensorDataRequest>,
    val aiCoachings: List<AiCoachingRequest>
)

@Serializable
data class LapSplitRequest(
    val lapNumber: Long,
    val lapPace: String,
    val avgHeartRate: Int,
    val paceStatue: String
)

@Serializable
data class FootSensorDataRequest(
    val footSide: String,
    val pronationType: String,
    val heelStrikeRate: Double,
    val hitmapJson: String
)

@Serializable
data class AiCoachingRequest(
    val feedbackText: String,
    val focusPoint: String? = null,
    val recExercise: String? = null
)

@Serializable
data class RunningActivityResponse(
    val activityId: Long? = null,
    val distance: Double? = null,
    val time: String? = null,
    val pace: String? = null,
    val activityDate: String? = null,
    val formScore: Int? = null
)

// ── AI 코칭 (백엔드 프록시 / AI 서버 공통) ────────────

@Serializable
data class FootHitmap(
    val heel: Int = 0,
    @SerialName("forefoot_left") val forefootLeft: Int = 0,
    @SerialName("forefoot_right") val forefootRight: Int = 0,
    @SerialName("midfoot_left") val midfootLeft: Int = 0,
    @SerialName("midfoot_right") val midfootRight: Int = 0
) {
    val total: Int get() = heel + forefootLeft + forefootRight + midfootLeft + midfootRight
}

@Serializable
data class FootData(
    val hitmap: FootHitmap = FootHitmap(),
    @SerialName("pronation_type") val pronationType: String? = null,
    @SerialName("heel_strike_rate") val heelStrikeRate: Int? = null
)

/** 프로필 컨텍스트. 백엔드 프록시는 서버가 직접 채우므로 AI 서버 직접 호출 때만 보냅니다. */
@Serializable
data class UserProfileContext(
    val age: Int? = null,
    val height: Double? = null,
    val weight: Double? = null,
    @SerialName("resting_heart_rate") val restingHeartRate: Int? = null,
    val lthr: Int? = null
)

@Serializable
data class CoachingRequest(
    /** 현재 페이스 (초/km) — AI 서버 문서 기준 */
    val pace: Double,
    @SerialName("heart_rate") val heartRate: Int,
    val cadence: Int,
    @SerialName("left_foot") val leftFoot: FootData,
    @SerialName("right_foot") val rightFoot: FootData,
    val profile: UserProfileContext? = null
)

@Serializable
data class CoachingResponse(
    @SerialName("feedback_text") val feedbackText: String,
    @SerialName("focus_point") val focusPoint: String? = null,
    @SerialName("rec_exercise") val recExercise: String? = null,
    @SerialName("voice_file") val voiceFile: String? = null
)
