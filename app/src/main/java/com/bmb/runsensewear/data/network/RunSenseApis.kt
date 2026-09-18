package com.bmb.runsensewear.data.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/** 런센스 백엔드 (https://runsense-backend-server-9he7.onrender.com) */
interface RunSenseBackendApi {

    @POST("api/users/reissue")
    suspend fun reissue(@Body body: TokenReissueRequest): ApiResponse<TokenResponse>

    @GET("api/users")
    suspend fun getProfile(): ApiResponse<UserProfile>

    @POST("api/run")
    suspend fun createRun(@Body body: RunningActivityCreateRequest): ApiResponse<RunningActivityResponse>

    /** 백엔드가 프로필을 붙여 AI 서버로 넘겨주는 코칭 프록시 (로그인 필요) */
    @POST("api/run/coaching")
    suspend fun coaching(@Body body: CoachingRequest): ApiResponse<CoachingResponse>
}

/** 런센스 AI 코칭 서버 (https://runsense-ai-server.onrender.com) — 인증 없음 */
interface AiCoachApi {

    @POST("ai/coaching")
    suspend fun coaching(@Body body: CoachingRequest): CoachingResponse
}
