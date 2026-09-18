package com.bmb.runsensewear.data.profile

import android.util.Log
import com.bmb.runsensewear.data.auth.TokenStore
import com.bmb.runsensewear.data.network.NetworkModule
import com.bmb.runsensewear.data.network.UserProfile
import com.bmb.runsensewear.data.network.UserProfileContext
import com.bmb.runsensewear.data.run.HeartRateZones
import com.bmb.runsensewear.data.sync.PhoneProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine

private const val TAG = "ProfileRepository"

/**
 * 내 프로필(LTHR·생년월일·키·몸무게).
 *
 * 두 곳에서 옵니다.
 *  - 백엔드 GET /api/users (로그인돼 있을 때)
 *  - 폰 앱이 Data Layer로 보내는 프로필 — LTHR·안정시 심박은 폰 앱이 기기에만 저장하므로
 *    서버 값이 비어 있으면 폰 값을 씁니다.
 */
class ProfileRepository(
    private val network: NetworkModule,
    private val tokenStore: TokenStore,
    private val phoneProfile: StateFlow<PhoneProfile?>
) {
    private val serverProfile = MutableStateFlow<UserProfile?>(null)

    /** 서버 + 폰 값을 합친 최종 프로필 (변경 알림용) */
    val profile: Flow<UserProfile?> =
        combine(serverProfile, phoneProfile) { server, phone -> merge(server, phone) }

    /** 지금 시점의 최종 프로필 */
    val current: UserProfile?
        get() = merge(serverProfile.value, phoneProfile.value)

    val zones: HeartRateZones
        get() = current.let { HeartRateZones.fromProfile(it?.lthr, it?.birthDate) }

    /** AI 서버 직접 호출 시 함께 보내는 프로필 컨텍스트 */
    val context: UserProfileContext?
        get() = current?.let { p ->
            UserProfileContext(
                age = HeartRateZones.ageFrom(p.birthDate),
                height = p.height,
                weight = p.weight,
                restingHeartRate = p.restingHeartRate?.toInt(),
                lthr = p.lthr
            )
        }

    suspend fun refresh(): UserProfile? {
        if (!tokenStore.current.isLoggedIn) return current
        runCatching { network.backendApi.getProfile().data }
            .onFailure { Log.w(TAG, "프로필 조회 실패", it) }
            .getOrNull()
            ?.also { serverProfile.value = it }
        return current
    }

    private fun merge(server: UserProfile?, phone: PhoneProfile?): UserProfile? {
        if (server == null && phone == null) return null
        return UserProfile(
            userId = server?.userId,
            userName = server?.userName ?: phone?.userName,
            email = server?.email,
            birthDate = server?.birthDate ?: phone?.birthDate,
            gender = server?.gender,
            height = server?.height ?: phone?.height,
            weight = server?.weight ?: phone?.weight,
            restingHeartRate = server?.restingHeartRate ?: phone?.restingHeartRate?.toDouble(),
            lthr = server?.lthr?.takeIf { it > 0 } ?: phone?.lthr
        )
    }
}
