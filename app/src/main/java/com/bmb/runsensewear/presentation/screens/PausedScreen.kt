package com.bmb.runsensewear.presentation.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.tooling.preview.devices.WearDevices
import com.bmb.runsensewear.presentation.components.GradientButton
import com.bmb.runsensewear.presentation.components.RunSenseScreen
import com.bmb.runsensewear.presentation.theme.RunSenseColors
import com.bmb.runsensewear.presentation.theme.RunSenseGradients
import com.bmb.runsensewear.presentation.theme.RunSenseNumbers
import com.bmb.runsensewear.presentation.theme.RunSenseWearTheme
import com.bmb.runsensewear.presentation.theme.isCompactWatch

/**
 * 07 일시정지.
 *
 * 시안: 어두운 오버레이 · ‖ 원형 뱃지 · "일시정지됨" · 센서 안내 문구 ·
 * "17:48 · 3.24km" 한 줄 요약 · 둥근 사각 버튼 2개(재개 / 종료).
 *
 * Health Services 세션도 함께 멈추므로 거리·시간이 더 쌓이지 않습니다.
 */
@Composable
fun PausedScreen(
    onResumeClick: () -> Unit,
    onStopClick: () -> Unit,
    modifier: Modifier = Modifier,
    elapsedText: String = "00:00",
    distanceKm: Double = 0.0
) {
    val compact = isCompactWatch()

    RunSenseScreen(
        modifier = modifier,
        background = RunSenseGradients.PausedBackground,
        overlay = Color(0x59080518),
        showTime = false
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            PauseBadge(size = if (compact) 30.dp else 38.dp)

            Text(
                text = "일시정지됨",
                style = MaterialTheme.typography.title2,
                color = RunSenseColors.TextPrimary,
                modifier = Modifier.padding(top = 5.dp)
            )

            Text(
                text = "센서 수집도 함께 멈췄어요",
                style = MaterialTheme.typography.caption3,
                color = RunSenseColors.TextTertiary,
                modifier = Modifier.padding(top = 1.dp)
            )

            Text(
                text = "$elapsedText · ${String.format("%.2f", distanceKm)}km",
                style = RunSenseNumbers.small.copy(fontSize = if (compact) 16.sp else 18.sp),
                color = RunSenseColors.TextSecondary,
                modifier = Modifier.padding(top = 4.dp)
            )

            Column(
                modifier = Modifier
                    .padding(top = if (compact) 6.dp else 8.dp)
                    .width(126.dp),
                verticalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 5.dp)
            ) {
                GradientButton(
                    text = "▶ 재개",
                    onClick = onResumeClick,
                    shape = RoundedCornerShape(16.dp),
                    verticalPadding = if (compact) 6.dp else 8.dp,
                    modifier = Modifier.fillMaxWidth()
                )
                GradientButton(
                    text = "■ 종료",
                    onClick = onStopClick,
                    brush = RunSenseGradients.DangerAction,
                    shape = RoundedCornerShape(16.dp),
                    verticalPadding = if (compact) 5.dp else 7.dp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/** 시안의 ‖ 원형 뱃지 — 멈춘 상태를 나타내듯 천천히 숨쉬는 밝기 */
@Composable
private fun PauseBadge(modifier: Modifier = Modifier, size: androidx.compose.ui.unit.Dp = 38.dp) {
    val transition = rememberInfiniteTransition(label = "pauseBreath")
    val alpha by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Reverse),
        label = "pauseAlpha"
    )
    Box(
        modifier = modifier
            .size(size)
            .background(RunSenseColors.CardBg, CircleShape)
            .border(1.dp, RunSenseColors.CardBorder, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.graphicsLayer { this.alpha = alpha }
        ) {
            repeat(2) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(size * 0.4f)
                        .background(RunSenseColors.TextSecondary, RoundedCornerShape(2.dp))
                )
            }
        }
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
private fun PausedScreenPreview() {
    RunSenseWearTheme {
        PausedScreen(
            onResumeClick = {},
            onStopClick = {},
            elapsedText = "17:48",
            distanceKm = 3.24
        )
    }
}
