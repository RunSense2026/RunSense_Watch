package com.bmb.runsensewear.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.tooling.preview.devices.WearDevices
import com.bmb.runsensewear.presentation.components.PageDots
import com.bmb.runsensewear.presentation.components.RunSenseScreen
import com.bmb.runsensewear.presentation.components.StatusBadge
import com.bmb.runsensewear.presentation.components.SwipeArrows
import com.bmb.runsensewear.presentation.components.onSwipeLeft
import com.bmb.runsensewear.presentation.components.rememberBeatScale
import com.bmb.runsensewear.presentation.theme.RunSenseColors
import com.bmb.runsensewear.presentation.theme.RunSenseWearTheme

/** 심박존 1~5. 색상은 Color.kt의 상태 색상 토큰을 따릅니다. */
private val ZoneColors = listOf(
    RunSenseColors.Blue300,
    RunSenseColors.Good,
    RunSenseColors.Good2,
    RunSenseColors.Warn,
    RunSenseColors.Bad
)

private val ZoneNames = listOf("회복", "지구력", "유산소", "무산소", "최대")

/**
 * 04 심박 상세.
 *
 * 시안: 뛰는 하트 + 큰 심박수 · "BPM · LTHR n" · "Z3 · 유산소" 뱃지 ·
 * 존별 체류시간 막대 5개 · 좌우 화살표 · 페이지 점 4개.
 *
 * @param currentZone 1~5. 0이면 아직 측정 전.
 * @param zoneShares 존별 체류 비율 0~1. 막대 높이로 표현합니다.
 *
 * TODO: heartRate / currentZone / zoneShares 를 HeartRateSensorManager 구독 결과로 교체하세요.
 */
@Composable
fun HeartRateScreen(
    onSwipeToFormSummary: () -> Unit,
    @Suppress("UNUSED_PARAMETER") onSwipeBackToRunning: () -> Unit,
    modifier: Modifier = Modifier,
    heartRate: Int = 0,
    currentZone: Int = 0,
    lactateThresholdHr: Int = 0,
    zoneShares: List<Float> = listOf(0f, 0f, 0f, 0f, 0f)
) {
    RunSenseScreen(
        modifier = modifier.onSwipeLeft { onSwipeToFormSummary() },
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val beat = rememberBeatScale()
                    Text(
                        text = "♥",
                        style = MaterialTheme.typography.title1,
                        color = RunSenseColors.Bad,
                        modifier = Modifier.scale(beat)
                    )
                    Text(
                        text = heartRate.toString(),
                        style = MaterialTheme.typography.display2,
                        color = RunSenseColors.TextPrimary
                    )
                }

                Text(
                    text = if (lactateThresholdHr > 0) "BPM · LTHR $lactateThresholdHr" else "BPM",
                    style = MaterialTheme.typography.caption3,
                    color = RunSenseColors.TextTertiary
                )

                StatusBadge(
                    text = if (currentZone in 1..5) "Z$currentZone · ${ZoneNames[currentZone - 1]}"
                    else "측정 대기",
                    color = if (currentZone in 1..5) RunSenseColors.Purple300
                    else RunSenseColors.TextTertiary,
                    modifier = Modifier.padding(top = 4.dp)
                )

                ZoneBars(
                    currentZone = currentZone,
                    shares = zoneShares,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            PageDots(
                count = RUN_PAGE_COUNT,
                selectedIndex = 1,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 4.dp)
            )
        }
    }
}

/**
 * 존별 체류시간 막대.
 * 시안처럼 막대 높이는 체류 비율, 현재 존만 보라색으로 채웁니다.
 */
@Composable
private fun ZoneBars(
    currentZone: Int,
    shares: List<Float>,
    modifier: Modifier = Modifier,
    minHeight: Dp = 8.dp,
    maxHeight: Dp = 26.dp
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        ZoneColors.forEachIndexed { index, color ->
            val active = index == currentZone - 1
            val share = shares.getOrElse(index) { 0f }.coerceIn(0f, 1f)
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(minHeight + (maxHeight - minHeight) * share)
                        .background(
                            if (active) RunSenseColors.Purple300 else color.copy(alpha = 0.28f),
                            RoundedCornerShape(3.dp)
                        )
                )
                Text(
                    text = "Z${index + 1}",
                    style = MaterialTheme.typography.caption3,
                    color = if (active) RunSenseColors.Purple300 else RunSenseColors.TextTertiary
                )
            }
        }
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
private fun HeartRateScreenPreview() {
    RunSenseWearTheme {
        HeartRateScreen(
            onSwipeToFormSummary = {},
            onSwipeBackToRunning = {},
            heartRate = 156,
            currentZone = 3,
            lactateThresholdHr = 164,
            zoneShares = listOf(0.39f, 0.61f, 1f, 0.54f, 0.32f)
        )
    }
}
