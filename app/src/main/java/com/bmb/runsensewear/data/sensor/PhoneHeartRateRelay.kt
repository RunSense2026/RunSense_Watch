package com.bmb.runsensewear.data.sensor

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.bmb.runsensewear.R
import com.bmb.runsensewear.RunSenseApp
import com.bmb.runsensewear.data.sync.PhoneSyncContract
import com.bmb.runsensewear.data.sync.PhoneSyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private const val TAG = "PhoneHrRelay"

/** 같은 bpm 이 이어져도 폰 화면이 stale 로 보지 않도록 이 간격마다는 다시 보냅니다. */
private const val RESEND_INTERVAL_MS = 2_000L

/**
 * 폰에서 러닝(DashboardA)을 하는 동안 워치 심박을 폰으로 실시간 전송합니다.
 *
 * 워치에서 러닝을 시작하지 않아도 [PhoneSyncContract.RUN_PHONE] active=true 가 오면
 * 심박 센서를 켜고 [PhoneSyncContract.HEART_RATE] 로 bpm 을 보냅니다.
 * 화면이 꺼져도 끊기지 않도록 포그라운드 서비스([PhoneHeartRateService])에서 돌리고,
 * 백그라운드라 서비스를 못 띄우면 앱 스코프에서라도 측정합니다(프로세스가 살아 있는 동안).
 */
class PhoneHeartRateRelay(
    private val context: Context,
    private val scope: CoroutineScope,
    private val phoneSync: PhoneSyncManager
) {
    private val sensor = HeartRateSensorManager(context)
    private var fallbackJob: Job? = null
    private var serviceRunning = false

    fun start() {
        scope.launch {
            phoneSync.phoneRunActive.collect { active ->
                if (active) ensureRunning() else stopAll()
            }
        }
    }

    /** 폰 러닝 중이면 측정을 켭니다. 앱이 포그라운드로 올라올 때 다시 불러 서비스로 옮겨 탑니다. */
    fun ensureRunning() {
        if (!phoneSync.phoneRunActive.value || serviceRunning) return
        val started = runCatching {
            context.startForegroundService(Intent(context, PhoneHeartRateService::class.java))
        }.onFailure { Log.w(TAG, "포그라운드 서비스 시작 실패 — 앱 스코프에서 측정", it) }.isSuccess
        if (started) {
            serviceRunning = true
            fallbackJob?.cancel()
            fallbackJob = null
        } else if (fallbackJob == null) {
            fallbackJob = scope.launch { relay() }
        }
    }

    private fun stopAll() {
        fallbackJob?.cancel()
        fallbackJob = null
        if (serviceRunning) {
            context.stopService(Intent(context, PhoneHeartRateService::class.java))
            serviceRunning = false
        }
    }

    internal fun onServiceDestroyed() {
        serviceRunning = false
    }

    /** 센서 값을 폰으로 보냅니다. 수집하는 코루틴이 취소되면 센서도 꺼집니다. */
    internal suspend fun relay() {
        var lastBpm = -1
        var lastSentAt = 0L
        sensor.heartRateFlow().collect { state ->
            val bpm = state.bpmOrZero
            val now = System.currentTimeMillis()
            if (bpm == lastBpm && now - lastSentAt < RESEND_INTERVAL_MS) return@collect
            lastBpm = bpm
            lastSentAt = now
            phoneSync.sendJson(
                PhoneSyncContract.HEART_RATE,
                """{"heartRate":$bpm,"state":"${state.wireName}"}"""
            )
        }
    }
}

private val HeartRateState.wireName: String
    get() = when (this) {
        is HeartRateState.Measuring -> "measuring"
        HeartRateState.Acquiring -> "acquiring"
        HeartRateState.OffBody -> "offBody"
        HeartRateState.Unavailable -> "unavailable"
        HeartRateState.Unsupported -> "unsupported"
    }

/** [PhoneHeartRateRelay] 가 화면이 꺼져도 심박을 보낼 수 있게 프로세스를 살려 두는 서비스. */
class PhoneHeartRateService : LifecycleService() {

    private val relay: PhoneHeartRateRelay
        get() = (application as RunSenseApp).container.phoneHeartRateRelay

    override fun onCreate() {
        super.onCreate()
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.phone_hr_channel_name),
                NotificationManager.IMPORTANCE_LOW
            )
        )
        startForeground(NOTIFICATION_ID, buildNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH)
        lifecycleScope.launch { relay.relay() }
    }

    override fun onDestroy() {
        relay.onServiceDestroyed()
        super.onDestroy()
    }

    private fun buildNotification(): Notification {
        val launch = PendingIntent.getActivity(
            this, 0,
            Intent(this, com.bmb.runsensewear.presentation.MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_run)
            .setContentTitle(getString(R.string.phone_hr_notification_title))
            .setContentText(getString(R.string.phone_hr_notification_text))
            .setContentIntent(launch)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .build()
    }

    private companion object {
        const val CHANNEL_ID = "phone_hr_relay"
        const val NOTIFICATION_ID = 1002
    }
}
