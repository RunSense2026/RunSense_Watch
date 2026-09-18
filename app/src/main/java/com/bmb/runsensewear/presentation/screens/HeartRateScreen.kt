package com.bmb.runsensewear.presentation.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.tooling.preview.devices.WearDevices
import com.bmb.runsensewear.presentation.components.EdgeArc
import com.bmb.runsensewear.presentation.components.PageDots
import com.bmb.runsensewear.presentation.components.RunSenseScreen
import com.bmb.runsensewear.presentation.components.StatusBadge
import com.bmb.runsensewear.presentation.components.SwipeArrows
import com.bmb.runsensewear.presentation.components.onSwipeLeft
import com.bmb.runsensewear.presentation.components.rememberBeatScale
import com.bmb.runsensewear.presentation.theme.RunSenseColors
import com.bmb.runsensewear.presentation.theme.RunSenseNumbers
import com.bmb.runsensewear.presentation.theme.RunSenseWearTheme
import com.bmb.runsensewear.presentation.theme.isCompactWatch
import androidx.compose.ui.unit.sp

/** 심박존 1~5. 색상은 Color.kt의 상태 색상 토큰을 따릅니다. */
private val ZoneColors = listOf(
    RunSenseColors.Blue300,
    RunSenseColors.Good,
    RunSenseColors.Good2,
    RunSenseColors.Warn,
    RunSenseColors.Bad
)

/** 폰 앱(format.ts hrZone)과 같은 존 이름 */
private val ZoneNames = listOf("회복", "유산소", "템포", "역치", "무산소")

/**
 * 04 심박 상세.
 *
 * 시안: 뛰는 하트 + 큰 심박수 · "BPM · LTHR n" · "Z3 · 템포" 뱃지 ·
 * 존별 체류시간 막대 5개 · 좌우 화살표 · 페이지 점 4개.
 * 화면 가장자리 링은 현재 존을 색으로, 채움은 심박 강도(존 위치)를 나타냅니다.
 *
 * 심박수는 워치 내장 광학 센서(Health Services)에서 받아옵니다.
 *
 * @param currentZone 1~5. 0이면 아직 측정 전.
 * @param zoneShares 존별 체류 비율 0~1. 막대 높이로 표현합니다.
 * @param statusLabel 측정이 안 될 때 존 뱃지 자리에 띄울 안내 문구. 정상 측정 중이면 null.
 * @param lactateThresholdHr 프로필/폰에서 받은 LTHR. 0이면 표시하지 않습니다.
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun HeartRateScreen(
    onSwipeToFormSummary: () -> Unit,
    modifier: Modifier = Modifier,
    heartRate: Int = 0,
    currentZone: Int = 0,
    lactateThresholdHr: Int = 0,
    zoneShares: List<Float> = listOf(0f, 0f, 0f, 0f, 0f),
    statusLabel: String? = null
) {
    val compact = isCompactWatch()
    val zoneColor = ZoneColors.getOrElse(currentZone - 1) { RunSenseColors.Purple300 }
    val ringColor by animateColorAsState(
        targetValue = if (currentZone in 1..5) zoneColor else RunSenseColors.Purple300,
        animationSpec = tween(400),
        label = "ringColor"
    )

    RunSenseScreen(
        modifier = modifier.onSwipeLeft { onSwipeToFormSummary() },
        showTime = false,
        decor = {
            EdgeArc(
                progress = if (currentZone in 1..5) (currentZone - 0.5f) / 5f else 0f,
                brush = Brush.sweepGradient(listOf(ringColor.copy(alpha = 0.35f), ringColor))
            )
        }
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
                    val beat = rememberBeatScale(bpm = heartRate)
                    Text(
                        text = "♥",
                        style = MaterialTheme.typography.title1,
                        color = RunSenseColors.Bad,
                        modifier = Modifier.scale(beat)
                    )
                    AnimatedContent(
                        targetState = if (heartRate > 0) heartRate.toString() else "--",
                        transitionSpec = {
                            (fadeIn(tween(200)) + slideInVertically(tween(200)) { it / 4 })
                                .togetherWith(fadeOut(tween(120)))
                        },
                        label = "bpm"
                    ) { value ->
                        Text(
                            text = value,
                            style = if (compact) RunSenseNumbers.large.copy(fontSize = 38.sp) else RunSenseNumbers.large,
                            color = RunSenseColors.TextPrimary
                        )
                    }
                }

                Text(
                    text = if (lactateThresholdHr > 0) "BPM · LTHR $lactateThresholdHr" else "BPM",
                    style = MaterialTheme.typography.caption3,
                    color = RunSenseColors.TextTertiary
                )

                // 센서가 값을 못 주는 동안에는 존 대신 이유를 보여줍니다.
                val showZone = statusLabel == null && currentZone in 1..5
                StatusBadge(
                    text = if (showZone) "Z$currentZone · ${ZoneNames[currentZone - 1]}"
                    else statusLabel ?: "측정 대기",
                    color = if (showZone) zoneColor else RunSenseColors.TextTertiary,
                    modifier = Modifier.padding(top = 4.dp)
                )

                ZoneBars(
                    currentZone = currentZone,
                    shares = zoneShares,
                    maxHeight = if (compact) 18.dp else 24.dp,
                    modifier = Modifier.padding(top = if (compact) 6.dp else 8.dp)
                )
            }

            PageDots(
                count = RUN_PAGE_COUNT,
                selectedIndex = 1,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 2.dp)
            )
        }
    }
}

/**
 * 존별 체류시간 막대.
 * 막대 높이는 체류 비율(스프링으로 자라남), 현재 존은 존 색으로 채웁니다.
 */
@Composable
private fun ZoneBars(
    currentZone: Int,
    shares: List<Float>,
    modifier: Modifier = Modifier,
    minHeight: Dp = 6.dp,
    maxHeight: Dp = 24.dp
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        ZoneColors.forEachIndexed { index, color ->
            val active = index == currentZone - 1
            val share = shares.getOrElse(index) { 0f }.coerceIn(0f, 1f)
            val height by animateDpAsState(
                targetValue = minHeight + (maxHeight - minHeight) * share,
                animationSpec = spring(stiffness = Spring.StiffnessLow),
                label = "zone$index"
            )
            val barColor by animateColorAsState(
                targetValue = if (active) color else color.copy(alpha = 0.26f),
                animationSpec = tween(300),
                label = "zoneColor$index"
            )
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(height)
                        .background(barColor, RoundedCornerShape(3.dp))
                )
                Text(
                    text = "Z${index + 1}",
                    style = MaterialTheme.typography.caption3,
                    color = if (active) color else RunSenseColors.TextTertiary
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
            heartRate = 156,
            currentZone = 3,
            lactateThresholdHr = 164,
            zoneShares = listOf(0.39f, 0.61f, 1f, 0.54f, 0.32f)
        )
    }
}

@Suppress("unused")
private val previewColor: Color = Color.Unspecified
