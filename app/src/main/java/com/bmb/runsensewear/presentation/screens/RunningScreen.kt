package com.bmb.runsensewear.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.tooling.preview.devices.WearDevices
import com.bmb.runsensewear.presentation.components.MetricColumn
import com.bmb.runsensewear.presentation.components.MetricDivider
import com.bmb.runsensewear.presentation.components.PageDots
import com.bmb.runsensewear.presentation.components.PauseCircleButton
import com.bmb.runsensewear.presentation.components.PulsingDot
import com.bmb.runsensewear.presentation.components.RunSenseScreen
import com.bmb.runsensewear.presentation.components.StatusBadge
import com.bmb.runsensewear.presentation.components.SwipeArrows
import com.bmb.runsensewear.presentation.components.onSwipeLeft
import com.bmb.runsensewear.presentation.theme.RunSenseColors
import com.bmb.runsensewear.presentation.theme.RunSenseGradients
import com.bmb.runsensewear.presentation.theme.RunSenseWearTheme
import kotlinx.coroutines.delay

/** 러닝 중 ↔ 심박 ↔ 폼요약 ↔ AI코치 가로 페이지 수 (시안의 점 4개) */
internal const val RUN_PAGE_COUNT = 4

/** 페이스 상태. 시안의 "적정 페이스" 초록 뱃지에 대응합니다. */
enum class PaceState(val label: String) {
    TooSlow("페이스 느림"),
    Good("적정 페이스"),
    TooFast("페이스 빠름")
}

/** 초 단위 경과시간을 mm:ss 로 (한 시간 넘으면 h:mm:ss) */
internal fun formatElapsed(totalSeconds: Int): String {
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return if (h > 0) String.format("%d:%02d:%02d", h, m, s)
    else String.format("%02d:%02d", m, s)
}

/**
 * 03 러닝 중 (메인 화면).
 *
 * 시안: 상단 "페이스 / KM" 라벨 · 대형 그라데이션 페이스 · 상태 뱃지 ·
 * 거리/시간/심박 3분할 · 좌우 화살표 · 페이지 점 4개 · 원형 일시정지 버튼.
 *
 * 왼쪽으로 밀면 04 심박 상세로, 오른쪽으로 밀면 뒤로가기(SwipeDismiss)입니다.
 *
 * @param onAiCoachTriggered 코칭 메시지 생성 이벤트가 오면 ViewModel이 호출합니다.
 *
 * TODO: elapsedSeconds / paceText / distanceKm / heartRate 는 지금 화면 안에서
 *       임시로 만들고 있습니다. Health Services ExerciseClient 연동 후
 *       ViewModel state로 끌어올리세요.
 */
@Composable
fun RunningScreen(
    onSwipeToHeartRate: () -> Unit,
    onPauseClick: () -> Unit,
    @Suppress("UNUSED_PARAMETER") onAiCoachTriggered: () -> Unit,
    modifier: Modifier = Modifier,
    paceText: String = "5'28\"",
    paceState: PaceState = PaceState.Good,
    distanceKm: Double = 0.0,
    heartRate: Int = 0
) {
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            elapsedSeconds++
        }
    }

    RunSenseScreen(
        modifier = modifier.onSwipeLeft { onSwipeToHeartRate() },
        showTime = false
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            SwipeArrows()

            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    PulsingDot(color = RunSenseColors.Purple300, size = 5.dp)
                    Text(
                        text = "페이스 / KM",
                        style = MaterialTheme.typography.caption1,
                        color = RunSenseColors.TextTertiary
                    )
                }

                Text(
                    // display1(60sp)은 40mm 워치(198dp)에서 좌우가 잘려 display2를 씁니다.
                    text = paceText,
                    style = MaterialTheme.typography.display2.merge(
                        TextStyle(brush = RunSenseGradients.PaceTextGradient)
                    )
                )

                StatusBadge(
                    text = paceState.label,
                    color = when (paceState) {
                        PaceState.Good -> RunSenseColors.Good
                        PaceState.TooSlow -> RunSenseColors.Warn
                        PaceState.TooFast -> RunSenseColors.Bad
                    }
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    MetricColumn(value = String.format("%.2f", distanceKm), label = "KM")
                    MetricDivider()
                    MetricColumn(value = formatElapsed(elapsedSeconds), label = "시간")
                    MetricDivider()
                    MetricColumn(
                        value = heartRate.toString(),
                        label = "BPM",
                        valueColor = RunSenseColors.Bad
                    )
                }

                // 점/버튼을 하단 정렬로 따로 두면 지표와 겹치므로 같은 Column에 이어 붙입니다.
                PageDots(
                    count = RUN_PAGE_COUNT,
                    selectedIndex = 0,
                    modifier = Modifier.padding(top = 8.dp)
                )

                PauseCircleButton(
                    onClick = onPauseClick,
                    modifier = Modifier.padding(top = 5.dp)
                )
            }
        }
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
private fun RunningScreenPreview() {
    RunSenseWearTheme {
        RunningScreen(
            onSwipeToHeartRate = {},
            onPauseClick = {},
            onAiCoachTriggered = {},
            distanceKm = 3.24,
            heartRate = 156
        )
    }
}
