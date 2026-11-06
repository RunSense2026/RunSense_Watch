package com.bmb.runsensewear

import android.app.Application
import com.bmb.runsensewear.data.auth.TokenStore
import com.bmb.runsensewear.data.coach.CoachingRepository
import com.bmb.runsensewear.data.coach.CoachVoicePlayer
import com.bmb.runsensewear.data.exercise.RunSessionManager
import com.bmb.runsensewear.data.network.NetworkModule
import com.bmb.runsensewear.data.profile.ProfileRepository
import com.bmb.runsensewear.data.run.RunCoordinator
import com.bmb.runsensewear.data.run.RunRepository
import com.bmb.runsensewear.data.sensor.PhoneHeartRateRelay
import com.bmb.runsensewear.data.sync.PhoneSyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 앱 전역에서 하나씩만 있어야 하는 객체 모음 (수동 DI).
 * 서비스·ViewModel이 모두 여기서 같은 인스턴스를 꺼내 씁니다.
 */
class AppContainer(app: Application) {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val tokenStore = TokenStore(app)
    val network = NetworkModule(tokenStore)
    val phoneSyncManager = PhoneSyncManager(app, tokenStore, appScope, network.json)
    val runSessionManager = RunSessionManager(app, appScope)
    val profileRepository = ProfileRepository(network, tokenStore, phoneSyncManager.phoneProfile)
    val coachingRepository = CoachingRepository(network, tokenStore)
    val runRepository = RunRepository(app, network, tokenStore, appScope)
    val voicePlayer = CoachVoicePlayer(app, appScope)
    val phoneHeartRateRelay = PhoneHeartRateRelay(app, appScope, phoneSyncManager)

    init {
        // 토큰이 거부되면 워치가 직접 재발급하지 않고 폰에 새 토큰을 요청합니다 (리프레시 토큰 충돌 방지).
        network.onAuthRejected = { phoneSyncManager.requestAuth() }
    }
    val runCoordinator = RunCoordinator(
        context = app,
        scope = appScope,
        sessionManager = runSessionManager,
        phoneSync = phoneSyncManager,
        profileRepository = profileRepository,
        coachingRepository = coachingRepository,
        runRepository = runRepository,
        voicePlayer = voicePlayer
    )
}

class RunSenseApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        container.phoneSyncManager.loadPersistedItems()
        // 폰 러닝 중이면 워치 심박을 폰으로 보냅니다 (워치에서 러닝을 시작하지 않아도).
        container.phoneHeartRateRelay.start()
        container.appScope.launch {
            // 프로세스가 죽었다 살아난 경우 진행 중이던 러닝을 이어 받습니다.
            container.runSessionManager.restoreIfRunning()
            // 지난번에 서버 저장이 안 된 러닝 기록이 있으면 다시 시도합니다.
            container.runRepository.retryPending()
        }
    }
}
