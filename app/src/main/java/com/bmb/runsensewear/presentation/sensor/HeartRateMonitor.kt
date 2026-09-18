package com.bmb.runsensewear.presentation.sensor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.bmb.runsensewear.data.sensor.HeartRateSensorManager

/**
 * 실제 측정을 시작하지 않고 "이 워치에서 심박을 읽을 수 있는가"만 확인합니다.
 * 01 대기화면 칩처럼 센서를 켤 필요는 없는 곳에서 씁니다.
 *
 * 권한 대화상자는 별도 화면으로 뜨기 때문에, 돌아왔을 때(ON_RESUME) 다시 확인해야
 * 사용자가 방금 허용한 결과가 칩에 반영됩니다.
 *
 * 러닝 중 심박은 Health Services ExerciseClient가 주므로
 * [com.bmb.runsensewear.data.exercise.RunSessionManager]를 보세요.
 */
@Composable
fun rememberHeartRateSensorReady(): Boolean {
    val context = LocalContext.current
    val manager = remember(context) { HeartRateSensorManager(context) }
    val lifecycleOwner = LocalLifecycleOwner.current

    var ready by remember { mutableStateOf(false) }
    var checkCount by remember { mutableIntStateOf(0) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) checkCount++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(checkCount) {
        ready = manager.hasPermission() && manager.isSupported()
    }
    return ready
}
