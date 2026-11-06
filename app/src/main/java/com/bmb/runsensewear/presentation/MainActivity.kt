package com.bmb.runsensewear.presentation

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.bmb.runsensewear.RunSenseApp
import com.bmb.runsensewear.data.sensor.HeartRatePermission
import com.bmb.runsensewear.presentation.navigation.WearNavGraph
import com.bmb.runsensewear.presentation.theme.RunSenseWearTheme

/**
 * 러닝 추적에 필요한 런타임 권한.
 * 매니페스트 선언만으로는 부족하고 사용자가 직접 허용해야 하는 dangerous 권한들입니다.
 */
private val RequiredPermissions: Array<String> = buildList {
    // API 36부터는 BODY_SENSORS 대신 health.READ_HEART_RATE를 요청해야 합니다.
    add(HeartRatePermission)
    add(Manifest.permission.ACTIVITY_RECOGNITION)
    add(Manifest.permission.ACCESS_FINE_LOCATION)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        add(Manifest.permission.POST_NOTIFICATIONS)
    }
}.toTypedArray()

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)

        setTheme(android.R.style.Theme_DeviceDefault)
        // 러닝 중 손목을 들었을 때 바로 지표가 보이도록 화면을 켜 둡니다. (앰비언트는 시스템이 처리)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            WearApp()
        }
    }

    override fun onResume() {
        super.onResume()
        // 백그라운드라 심박 전송 서비스를 못 띄웠던 경우, 앱이 열리면 다시 시도합니다.
        (application as RunSenseApp).container.phoneHeartRateRelay.ensureRunning()
    }
}

@Composable
fun WearApp() {
    RunSenseWearTheme {
        RequestRunPermissions()
        WearNavGraph()
    }
}

/**
 * 앱 진입 시 한 번 권한을 요청합니다.
 * 결과는 대기화면이 다시 보일 때 [com.bmb.runsensewear.presentation.run.RunViewModel.refreshSensorReadiness]가
 * 다시 확인해 "워치 심박" 칩에 반영합니다. 거절하면 심박 없이 시간·GPS만으로 러닝이 진행됩니다.
 */
@Composable
private fun RequestRunPermissions() {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { /* 결과는 HomeScreen 진입 시 refreshSensorReadiness()로 반영 */ }

    LaunchedEffect(Unit) {
        launcher.launch(RequiredPermissions)
    }
}
