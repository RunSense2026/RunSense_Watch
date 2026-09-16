package com.bmb.runsensewear.presentation.sensor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.bmb.runsensewear.data.sensor.HeartRateSensorManager
import com.bmb.runsensewear.data.sensor.HeartRateState

/** 나이를 아직 입력받지 않아 쓰는 기본 최대 심박 (220 - 30세). */
const val DEFAULT_MAX_HEART_RATE = 190

/** 존 경계 — 최대 심박 대비 비율. Z1 <60% / Z2 60~70 / Z3 70~80 / Z4 80~90 / Z5 90%↑ */
private val ZoneUpperBounds = listOf(0.60f, 0.70f, 0.80f, 0.90f)

/**
 * 심박수를 존(1~5)으로 변환합니다. 측정 전(0 bpm)이면 0을 돌려줍니다.
 *
 * @param maxHeartRate 최대 심박. 사용자 프로필이 생기면 220 - 나이로 계산해 넘기세요.
 */
fun heartRateZone(bpm: Int, maxHeartRate: Int = DEFAULT_MAX_HEART_RATE): Int {
    if (bpm <= 0 || maxHeartRate <= 0) return 0
    val ratio = bpm.toFloat() / maxHeartRate
    return ZoneUpperBounds.indexOfFirst { ratio < it }.let { if (it == -1) 5 else it + 1 }
}

/**
 * 04 심박 상세 화면이 필요로 하는 값 묶음.
 *
 * @param zoneShares 존별 체류 비율 0~1. 가장 오래 머문 존이 1이 되도록 정규화합니다.
 */
@Immutable
data class HeartRateUiState(
    val state: HeartRateState = HeartRateState.Acquiring,
    val bpm: Int = 0,
    val zone: Int = 0,
    val zoneShares: List<Float> = List(5) { 0f }
) {
    /** 01 대기화면 칩처럼 "센서가 붙어 있나"만 알면 되는 곳에서 씁니다. */
    val isConnected: Boolean
        get() = state is HeartRateState.Measuring || state is HeartRateState.Acquiring

    /** 측정이 안 될 때 화면에 대신 띄울 짧은 안내 문구. 정상 측정 중이면 null. */
    val statusLabel: String?
        get() = when (state) {
            is HeartRateState.Measuring -> null
            HeartRateState.Acquiring -> "측정 대기"
            HeartRateState.OffBody -> "워치를 착용하세요"
            HeartRateState.Unavailable -> "측정 불가"
            HeartRateState.Unsupported -> "심박 센서 없음"
        }
}

/**
 * 실제 측정을 시작하지 않고 "이 워치에서 심박을 읽을 수 있는가"만 확인합니다.
 * 01 대기화면 칩처럼 센서를 켤 필요는 없는 곳에서 씁니다.
 *
 * 권한 대화상자는 별도 화면으로 뜨기 때문에, 돌아왔을 때(ON_RESUME) 다시 확인해야
 * 사용자가 방금 허용한 결과가 칩에 반영됩니다.
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

/**
 * 워치 내장 심박 센서를 구독해 화면용 상태로 바꿔 주는 상태 홀더.
 *
 * [enabled]가 false가 되면 구독이 해제되어 센서가 꺼지므로, 러닝 중이 아닌 화면에서는
 * false로 두어 배터리를 아낍니다.
 *
 * ViewModel이 생기면 이 로직을 그대로 옮기고 여기서는 state만 읽게 하세요.
 */
@Composable
fun rememberHeartRate(
    enabled: Boolean = true,
    maxHeartRate: Int = DEFAULT_MAX_HEART_RATE
): HeartRateUiState {
    val context = LocalContext.current
    val manager = remember(context) { HeartRateSensorManager(context) }

    var uiState by remember { mutableStateOf(HeartRateUiState()) }
    // 존별 누적 샘플 수. 측정 주기가 일정해 샘플 수를 체류시간의 근사로 씁니다.
    val zoneSamples = remember { IntArray(5) }

    LaunchedEffect(enabled, maxHeartRate) {
        if (!enabled) {
            uiState = uiState.copy(state = HeartRateState.Acquiring)
            return@LaunchedEffect
        }
        // 새 러닝을 시작할 때마다 지난 세션의 존 체류시간은 버립니다.
        zoneSamples.fill(0)
        uiState = HeartRateUiState()

        manager.heartRateFlow().collect { state ->
            val bpm = state.bpmOrZero
            val zone = heartRateZone(bpm, maxHeartRate)
            if (zone in 1..5) zoneSamples[zone - 1]++

            val peak = zoneSamples.max()
            uiState = HeartRateUiState(
                state = state,
                // 손목에서 떨어진 순간 숫자가 0으로 튀지 않게 마지막 값을 유지합니다.
                bpm = if (bpm > 0) bpm else uiState.bpm,
                zone = if (zone > 0) zone else uiState.zone,
                zoneShares = if (peak == 0) List(5) { 0f }
                else zoneSamples.map { it.toFloat() / peak }
            )
        }
    }

    return uiState
}
