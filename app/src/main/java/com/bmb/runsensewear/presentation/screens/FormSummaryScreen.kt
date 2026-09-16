package com.bmb.runsensewear.presentation.screens

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
import com.bmb.runsensewear.presentation.theme.RunSenseWearTheme

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
 * 왼쪽으로 밀면 마지막 페이지인 06 AI 코치로 넘어갑니다.
 *
 * @param balancePercent 좌우 균형 중 왼발 비율(%). 50이 완벽한 균형.
 *
 * TODO: Health Services의 러닝 폼 지표(케이던스·접지시간 등)는 기기마다 지원 여부가
 *       다릅니다. ExerciseCapabilities로 지원 여부를 먼저 확인한 뒤 연결하세요.
 */
@Composable
fun FormSummaryScreen(
    onSwipeToAiCoach: () -> Unit,
    @Suppress("UNUSED_PARAMETER") onSwipeBack: () -> Unit,
    modifier: Modifier = Modifier,
    formScore: Int = 0,
    cadence: Int = 0,
    balancePercent: Int = 50
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
                        value = cadence.toString(),
                        modifier = Modifier.weight(1f)
                    )
                    FormMetricCard(
                        label = "좌우 균형",
                        value = "$balancePercent:${100 - balancePercent}",
                        valueColor = if (balancePercent in 48..52) RunSenseColors.TextPrimary
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
                    .padding(bottom = 4.dp)
            )
        }
    }
}

/** 시안의 도넛 게이지 — 바깥 링이 점수만큼 차고 안쪽은 불투명한 원 */
@Composable
private fun ScoreDonut(score: Int, modifier: Modifier = Modifier) {
    val color = formScoreColor(score)
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
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * (score.coerceIn(0, 100) / 100f),
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = score.toString(),
                style = MaterialTheme.typography.display3,
                color = RunSenseColors.TextPrimary
            )
            Text(
                text = formScoreGrade(score),
                style = MaterialTheme.typography.caption3,
                color = color
            )
        }
    }
}

@Composable
private fun FormMetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = RunSenseColors.TextPrimary
) {
    GlassCard(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        horizontalPadding = 8.dp,
        verticalPadding = 6.dp
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.caption3,
                color = RunSenseColors.TextTertiary
            )
            Text(
                text = value,
                style = MaterialTheme.typography.title2,
                color = valueColor
            )
        }
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
private fun FormSummaryScreenPreview() {
    RunSenseWearTheme {
        FormSummaryScreen(
            onSwipeToAiCoach = {},
            onSwipeBack = {},
            formScore = 82,
            cadence = 178,
            balancePercent = 52
        )
    }
}
