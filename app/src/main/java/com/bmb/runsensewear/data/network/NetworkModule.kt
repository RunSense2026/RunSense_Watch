package com.bmb.runsensewear.data.network

import android.util.Log
import com.bmb.runsensewear.BuildConfig
import com.bmb.runsensewear.data.auth.TokenStore
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

private const val TAG = "RunSenseNetwork"

/**
 * Retrofit/OkHttp 조립.
 *
 * - 백엔드: Bearer 토큰 자동 첨부, 401/403이면 refreshToken으로 한 번 재발급 후 재시도
 * - AI 서버: 인증 없음. Gemini 응답이 느릴 수 있어 읽기 타임아웃을 넉넉히 둡니다.
 */
class NetworkModule(private val tokenStore: TokenStore) {

    val json: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
        isLenient = true
    }

    private val logging = HttpLoggingInterceptor { Log.d(TAG, it) }.apply {
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC
        else HttpLoggingInterceptor.Level.NONE
    }

    private val baseClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .addInterceptor(logging)
        .build()

    private val contentType = "application/json".toMediaType()

    /** 재발급 전용 (인증 인터셉터 없음 — 재귀 방지) */
    private val plainBackendApi: RunSenseBackendApi = Retrofit.Builder()
        .baseUrl(BuildConfig.BACKEND_BASE_URL)
        .client(baseClient)
        .addConverterFactory(json.asConverterFactory(contentType))
        .build()
        .create(RunSenseBackendApi::class.java)

    val backendApi: RunSenseBackendApi = Retrofit.Builder()
        .baseUrl(BuildConfig.BACKEND_BASE_URL)
        .client(baseClient.newBuilder().addInterceptor(AuthInterceptor()).build())
        .addConverterFactory(json.asConverterFactory(contentType))
        .build()
        .create(RunSenseBackendApi::class.java)

    val aiApi: AiCoachApi = Retrofit.Builder()
        .baseUrl(BuildConfig.AI_BASE_URL)
        .client(baseClient)
        .addConverterFactory(json.asConverterFactory(contentType))
        .build()
        .create(AiCoachApi::class.java)

    /** AI 서버가 돌려주는 `/audio/xxx.mp3` 같은 상대 경로를 절대 URL로 */
    fun resolveVoiceUrl(voiceFile: String?): String? {
        if (voiceFile.isNullOrBlank()) return null
        if (voiceFile.startsWith("http")) return voiceFile
        return BuildConfig.AI_BASE_URL.trimEnd('/') + "/" + voiceFile.trimStart('/')
    }

    private inner class AuthInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val original = chain.request()
            val token = tokenStore.current.accessToken
            val first = chain.proceed(original.withBearer(token))
            if (first.code != 401 && first.code != 403) return first
            if (token.isNullOrBlank()) return first

            val refreshed = synchronized(this@NetworkModule) {
                // 다른 요청이 먼저 재발급했으면 그 토큰으로 바로 재시도
                val latest = tokenStore.current.accessToken
                if (latest != null && latest != token) latest else reissueBlocking()
            } ?: return first

            first.close()
            return chain.proceed(original.withBearer(refreshed))
        }

        private fun Request.withBearer(token: String?): Request =
            if (token.isNullOrBlank()) this
            else newBuilder().header("Authorization", "Bearer $token").build()

        private fun reissueBlocking(): String? {
            val refresh = tokenStore.current.refreshToken ?: return null
            val tokens = runCatching {
                runBlocking { plainBackendApi.reissue(TokenReissueRequest(refresh)) }
            }.onFailure {
                Log.w(TAG, "토큰 재발급 실패", it)
            }.getOrNull()?.data

            return if (tokens != null) {
                tokenStore.save(tokens.accessToken, tokens.refreshToken)
                tokens.accessToken
            } else {
                // 리프레시도 만료됐으면 폰에서 다시 로그인해야 합니다.
                tokenStore.clear()
                null
            }
        }
    }
}
