package com.bmb.runsensewear.presentation.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.tooling.preview.devices.WearDevices
import com.bmb.runsensewear.data.run.PaceState
import com.bmb.runsensewear.presentation.components.EdgeArc
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
import com.bmb.runsensewear.presentation.theme.RunSenseNumbers
import com.bmb.runsensewear.presentation.theme.RunSenseWearTheme
import com.bmb.runsensewear.presentation.theme.isCompactWatch

/** 러닝 중 ↔ 심박 ↔ 폼요약 ↔ AI코치 가로 페이지 수 (시안의 점 4개) */
internal const val RUN_PAGE_COUNT = 4

/**
 * 페이스 링 채움 비율 — 폰 앱 DashboardA의 PaceRing과 같은 공식입니다.
 * 기준(목표 또는 평균)보다 30초 빠르면 1, 30초 느리면 0, 같으면 0.5.
 */
internal fun paceRingProgress(currentSecPerKm: Double?, referenceSecPerKm: Double?): Float {
    if (currentSecPerKm == null || referenceSecPerKm == null || referenceSecPerKm <= 0) return 0.5f
    val deviation = (currentSecPerKm - referenceSecPerKm).coerceIn(-30.0, 30.0)
    return (0.5 + -deviation / 60.0).toFloat()
}

/**
 * 03 러닝 중 (메인 화면).
 *
 * 시안: 상단 "페이스 / KM" 라벨 · 대형 그라데이션 페이스 · 상태 뱃지 ·
 * 거리/시간/심박 3분할 · 좌우 화살표 · 페이지 점 4개 · 원형 일시정지 버튼.
 * 여기에 화면 가장자리를 따라 페이스 링(폰 앱의 Pace Hero 링과 같은 규칙)을 더했습니다.
 *
 * 왼쪽으로 밀면 04 심박 상세로, 오른쪽으로 밀면 뒤로가기(SwipeDismiss)입니다.
 *
 * @param paceReferenceSecPerKm 링/뱃지 기준 페이스 — 폰에서 정한 목표, 없으면 세션 평균
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun RunningScreen(
    onSwipeToHeartRate: () -> Unit,
    onPauseClick: () -> Unit,
    modifier: Modifier = Modifier,
    paceText: String = "--'--\"",
    paceState: PaceState = PaceState.Good,
    currentPaceSecPerKm: Double? = null,
    paceReferenceSecPerKm: Double? = null,
    distanceKm: Double = 0.0,
    elapsedText: String = "00:00",
    heartRate: Int = 0,
    gpsReady: Boolean = false
) {
    // 40mm 워치(≈198dp)에서는 큰 숫자·버튼을 한 단계 줄여 세로로 다 들어가게 합니다.
    val compact = isCompactWatch()
    val heroSize = if (compact) 40.sp else 46.sp
    val metricStyle = if (compact) RunSenseNumbers.metric.copy(fontSize = 18.sp) else RunSenseNumbers.metric
    val gap = if (compact) 5.dp else 7.dp

    RunSenseScreen(
        modifier = modifier.onSwipeLeft { onSwipeToHeartRate() },
        showTime = false,
        decor = {
            EdgeArc(progress = paceRingProgress(currentPaceSecPerKm, paceReferenceSecPerKm))
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            SwipeArrows(showLeft = false)

            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    PulsingDot(
                        color = if (gpsReady) RunSenseColors.Good else RunSenseColors.Purple300,
                        size = 5.dp
                    )
                    Text(
                        text = if (gpsReady) "페이스 / KM · GPS" else "페이스 / KM",
                        style = MaterialTheme.typography.caption1,
                        color = RunSenseColors.TextTertiary
                    )
                }

                AnimatedContent(
                    targetState = paceText,
                    transitionSpec = { fadeIn(tween(220)).togetherWith(fadeOut(tween(120))) },
                    label = "pace"
                ) { value ->
                    Text(
                        // 40mm 워치(198dp)에서도 좌우가 잘리지 않는 크기
                        text = value,
                        style = RunSenseNumbers.hero.copy(fontSize = heroSize).merge(
                            TextStyle(brush = RunSenseGradients.PaceTextGradient)
                        )
                    )
                }

                StatusBadge(
                    text = paceState.label,
                    color = when (paceState) {
                        PaceState.Good -> RunSenseColors.Good
                        PaceState.TooSlow -> RunSenseColors.Warn
                        PaceState.TooFast -> RunSenseColors.Bad
                    }
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = gap)
                ) {
                    MetricColumn(value = String.format("%.2f", distanceKm), label = "KM", valueStyle = metricStyle)
                    MetricDivider()
                    MetricColumn(value = elapsedText, label = "시간", valueStyle = metricStyle, animated = false)
                    MetricDivider()
                    MetricColumn(
                        value = if (heartRate > 0) heartRate.toString() else "--",
                        label = "BPM",
                        valueColor = RunSenseColors.Bad,
                        valueStyle = metricStyle
                    )
                }

                // 점/버튼을 하단 정렬로 따로 두면 지표와 겹치므로 같은 Column에 이어 붙입니다.
                PageDots(
                    count = RUN_PAGE_COUNT,
                    selectedIndex = 0,
                    modifier = Modifier.padding(top = gap)
                )

                PauseCircleButton(
                    onClick = onPauseClick,
                    size = if (compact) 26.dp else 30.dp,
                    modifier = Modifier.padding(top = if (compact) 4.dp else 5.dp)
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
            paceText = "5'28\"",
            currentPaceSecPerKm = 328.0,
            paceReferenceSecPerKm = 330.0,
            distanceKm = 3.24,
            elapsedText = "17:48",
            heartRate = 156,
            gpsReady = true
        )
    }
}
