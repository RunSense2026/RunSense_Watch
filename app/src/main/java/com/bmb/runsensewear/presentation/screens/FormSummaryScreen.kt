package com.bmb.runsensewear.presentation.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.tooling.preview.devices.WearDevices
import com.bmb.runsensewear.presentation.components.GlassCard
import com.bmb.runsensewear.presentation.components.PageDots
import com.bmb.runsensewear.presentation.components.RunSenseScreen
import com.bmb.runsensewear.presentation.components.SwipeArrows
import com.bmb.runsensewear.presentation.components.onSwipeLeft
import com.bmb.runsensewear.presentation.theme.RunSenseColors
import com.bmb.runsensewear.presentation.theme.RunSenseNumbers
import com.bmb.runsensewear.presentation.theme.RunSenseWearTheme
import kotlin.math.roundToInt

/** 점수 구간별 색상: 80↑ 좋음 / 60↑ 주의 / 그 미만 나쁨 */
internal fun formScoreColor(score: Int): Color = when {
    score >= 80 -> RunSenseColors.Good
    score >= 60 -> RunSenseColors.Warn
    else -> RunSenseColors.Bad
}

/** 시안의 점수 아래 등급 텍스트 */
internal fun formScoreGrade(score: Int): String = when {
    score >= 80 -> "좋음"
    score >= 60 -> "주의"
    else -> "개선 필요"
}

/**
 * 05 폼 요약.
 *
 * 시안: 도넛 게이지(점수 + 등급) · 케이던스 / 좌우 균형 카드 2개 ·
 * 좌우 화살표 · 페이지 점 4개.
 *
 * 폼 점수는 케이던스(워치) + 좌우 균형·뒤꿈치 착지율(폰 인솔)로 계산합니다
 * ([com.bmb.runsensewear.data.run.formScoreOf]). 인솔이 없으면 케이던스만 반영합니다.
 *
 * 왼쪽으로 밀면 마지막 페이지인 06 AI 코치로 넘어갑니다.
 *
 * @param balancePercent 좌우 균형 중 왼발 비율(%). 50이 완벽한 균형.
 * @param insoleConnected 인솔 데이터가 들어오고 있는지 (없으면 균형 카드에 안내)
 */
@Composable
fun FormSummaryScreen(
    onSwipeToAiCoach: () -> Unit,
    modifier: Modifier = Modifier,
    formScore: Int = 0,
    cadence: Int = 0,
    balancePercent: Int = 50,
    insoleConnected: Boolean = false
) {
    RunSenseScreen(
        modifier = modifier.onSwipeLeft { onSwipeToAiCoach() },
        showTime = false
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            SwipeArrows()

            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "폼 점수",
                    style = MaterialTheme.typography.caption1,
                    color = RunSenseColors.TextTertiary
                )

                ScoreDonut(
                    score = formScore,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Row(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FormMetricCard(
                        label = "케이던스",
                        value = if (cadence > 0) cadence.toString() else "--",
                        unit = "spm",
                        modifier = Modifier.weight(1f)
                    )
                    FormMetricCard(
                        label = "좌우 균형",
                        value = if (insoleConnected) "$balancePercent:${100 - balancePercent}" else "--",
                        valueColor = if (!insoleConnected || balancePercent in 47..53) RunSenseColors.TextPrimary
                        else RunSenseColors.Warn,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            PageDots(
                count = RUN_PAGE_COUNT,
                selectedIndex = 2,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 2.dp)
            )
        }
    }
}

/** 시안의 도넛 게이지 — 바깥 링이 점수만큼 스프링으로 차오르고 숫자도 따라 올라갑니다 */
@Composable
private fun ScoreDonut(score: Int, modifier: Modifier = Modifier) {
    val progress by animateFloatAsState(
        targetValue = score.coerceIn(0, 100) / 100f,
        animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioNoBouncy),
        label = "score"
    )
    val shown = (progress * 100).roundToInt()
    val color by animateColorAsState(
        targetValue = formScoreColor(shown),
        animationSpec = tween(300),
        label = "scoreColor"
    )

    Box(modifier = modifier.size(74.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
            val inset = stroke.width / 2f
            val arcSize = androidx.compose.ui.geometry.Size(
                size.width - stroke.width,
                size.height - stroke.width
            )
            val topLeft = androidx.compose.ui.geometry.Offset(inset, inset)
            drawArc(
                color = RunSenseColors.CardBorder,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke
            )
            if (progress > 0f) {
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = stroke
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (score > 0) shown.toString() else "--",
                style = RunSenseNumbers.medium,
                color = RunSenseColors.TextPrimary
            )
            Text(
                text = if (score > 0) formScoreGrade(shown) else "측정 중",
                style = MaterialTheme.typography.caption3,
                color = if (score > 0) color else RunSenseColors.TextTertiary
            )
        }
    }
}

@Composable
private fun FormMetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    unit: String? = null,
    valueColor: Color = RunSenseColors.TextPrimary
) {
    GlassCard(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        horizontalPadding = 6.dp,
        verticalPadding = 6.dp
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.caption3,
                color = RunSenseColors.TextTertiary
            )
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = value,
                    style = RunSenseNumbers.small,
                    color = valueColor
                )
                if (unit != null && value != "--") {
                    Text(
                        text = unit,
                        style = MaterialTheme.typography.caption3,
                        color = RunSenseColors.TextTertiary,
                        modifier = Modifier.padding(bottom = 3.dp)
                    )
                }
            }
        }
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
private fun FormSummaryScreenPreview() {
    RunSenseWearTheme {
        FormSummaryScreen(
            onSwipeToAiCoach = {},
            formScore = 82,
            cadence = 178,
            balancePercent = 52,
            insoleConnected = true
        )
    }
}
