package com.bmb.runsensewear.presentation

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
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

        setContent {
            WearApp()
        }
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
 *
 * TODO: 거절당했을 때의 안내 화면(설정으로 유도)은 아직 없습니다.
 *       센서 연동 작업과 함께 SensorCheckScreen에서 처리하는 게 자연스럽습니다.
 */
@Composable
private fun RequestRunPermissions() {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { /* 결과 처리는 센서 연동 시 SensorCheckScreen에서 */ }

    LaunchedEffect(Unit) {
        launcher.launch(RequiredPermissions)
    }
}
