package com.bmb.runsensewear.data.exercise

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import androidx.wear.ongoing.OngoingActivity
import androidx.wear.ongoing.Status
import com.bmb.runsensewear.R
import com.bmb.runsensewear.RunSenseApp
import com.bmb.runsensewear.data.run.RunPhase
import com.bmb.runsensewear.data.run.formatElapsed
import com.bmb.runsensewear.presentation.MainActivity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * 러닝 중 프로세스를 살려 두는 포그라운드 서비스.
 *
 * 실제 추적은 [RunSessionManager]가 하고, 이 서비스는
 *  - 화면이 꺼져도 Health Services 업데이트가 끊기지 않도록 프로세스를 유지하고
 *  - 워치페이스에 "러닝 중 · 12:34 · 2.10km" 진행 칩(Ongoing Activity)을 띄웁니다.
 *
 * 세션이 끝나면 스스로 종료합니다.
 */
class RunTrackingService : LifecycleService() {

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startAsForeground(buildNotification("러닝 시작", "00:00 · 0.00km"))

        val manager = (application as RunSenseApp).container.runSessionManager
        lifecycleScope.launch {
            manager.session.collectLatest { s ->
                when (s.phase) {
                    RunPhase.Running, RunPhase.Paused -> {
                        val title = if (s.phase == RunPhase.Paused) "일시정지됨" else "러닝 중"
                        val text = "${formatElapsed(s.elapsedSeconds)} · ${"%.2f".format(s.distanceKm)}km"
                        notificationManager.notify(NOTIFICATION_ID, buildNotification(title, text))
                    }

                    RunPhase.Completed, RunPhase.Idle -> {
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    }

                    else -> Unit
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_NOT_STICKY
    }

    private val notificationManager: NotificationManager
        get() = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private fun startAsForeground(notification: Notification) {
        var type = ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        }
        startForeground(NOTIFICATION_ID, notification, type)
    }

    private fun buildNotification(title: String, text: String): Notification {
        val launch = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_run)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(launch)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        // 워치페이스/최근 앱에 뜨는 진행 중 활동 칩
        OngoingActivity.Builder(applicationContext, NOTIFICATION_ID, builder)
            .setStaticIcon(R.drawable.ic_notification_run)
            .setTouchIntent(launch)
            .setStatus(Status.forPart(Status.TextPart(text)))
            .build()
            .apply(applicationContext)

        return builder.build()
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.tracking_channel_name),
            NotificationManager.IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "run_tracking"
        private const val NOTIFICATION_ID = 1001

        fun start(context: Context) {
            val intent = Intent(context, RunTrackingService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent)
            else context.startService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, RunTrackingService::class.java))
        }
    }
}
