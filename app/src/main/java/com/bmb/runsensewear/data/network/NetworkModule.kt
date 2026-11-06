package com.bmb.runsensewear.data.network

import android.util.Log
import com.bmb.runsensewear.BuildConfig
import com.bmb.runsensewear.data.auth.TokenStore
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
 * - 백엔드: Bearer 토큰 자동 첨부. 401/403이면 폰에 새 토큰을 요청합니다 (재발급은 폰만 수행)
 * - AI 서버: 인증 없음. Gemini 응답이 느릴 수 있어 읽기 타임아웃을 넉넉히 둡니다.
 */
class NetworkModule(private val tokenStore: TokenStore) {

    val json: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
        isLenient = true
    }

    /**
     * 백엔드가 토큰을 거부(401/403)했을 때 호출됩니다. RunSenseApp 이 "폰에 새 토큰 요청"으로 연결합니다.
     *
     * 리프레시 토큰은 서버가 회전(rotation)시키고 계정당 하나만 저장하므로, 워치가 직접 재발급하면
     * 폰이 가진 리프레시 토큰이 무효가 되어 폰이 로그아웃됩니다. 그래서 재발급은 폰만 합니다.
     */
    @Volatile
    var onAuthRejected: (() -> Unit)? = null

    @Volatile
    private var lastAuthRequestAt = 0L

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
            val response = chain.proceed(original.withBearer(token))
            if ((response.code == 401 || response.code == 403) && !token.isNullOrBlank()) {
                // 짧은 시간에 여러 요청이 실패해도 폰에는 한 번만 요청합니다.
                val now = System.currentTimeMillis()
                if (now - lastAuthRequestAt > 10_000) {
                    lastAuthRequestAt = now
                    Log.w(TAG, "토큰 거부(${response.code}) — 폰에 새 토큰 요청")
                    onAuthRejected?.invoke()
                }
            }
            return response
        }

        private fun Request.withBearer(token: String?): Request =
            if (token.isNullOrBlank()) this
            else newBuilder().header("Authorization", "Bearer $token").build()
    }
}
