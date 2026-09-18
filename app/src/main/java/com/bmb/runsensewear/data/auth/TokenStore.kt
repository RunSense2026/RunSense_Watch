package com.bmb.runsensewear.data.auth

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** 폰 앱에서 Data Layer로 넘어온 로그인 정보 */
data class AuthState(
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val userId: Long? = null,
    val userName: String? = null
) {
    val isLoggedIn: Boolean get() = !accessToken.isNullOrBlank()
}

/**
 * JWT 저장소.
 *
 * 워치에는 키보드가 없어 직접 로그인하지 않고, 폰 앱이 Data Layer(`/runsense/auth`)로
 * 보내 준 토큰을 받아 씁니다. 액세스 토큰이 만료되면 refreshToken으로 워치가 직접 재발급합니다.
 */
class TokenStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("runsense_auth", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(load())
    val state: StateFlow<AuthState> = _state

    val current: AuthState get() = _state.value

    private fun load() = AuthState(
        accessToken = prefs.getString(KEY_ACCESS, null),
        refreshToken = prefs.getString(KEY_REFRESH, null),
        userId = prefs.getLong(KEY_USER_ID, -1L).takeIf { it >= 0 },
        userName = prefs.getString(KEY_USER_NAME, null)
    )

    fun save(
        accessToken: String,
        refreshToken: String?,
        userId: Long? = current.userId,
        userName: String? = current.userName
    ) {
        prefs.edit()
            .putString(KEY_ACCESS, accessToken)
            .putString(KEY_REFRESH, refreshToken ?: current.refreshToken)
            .putLong(KEY_USER_ID, userId ?: -1L)
            .putString(KEY_USER_NAME, userName)
            .apply()
        _state.value = load()
    }

    fun clear() {
        prefs.edit().clear().apply()
        _state.value = AuthState()
    }

    private companion object {
        const val KEY_ACCESS = "access_token"
        const val KEY_REFRESH = "refresh_token"
        const val KEY_USER_ID = "user_id"
        const val KEY_USER_NAME = "user_name"
    }
}
