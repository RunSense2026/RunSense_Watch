package com.bmb.runsensewear.presentation.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.tooling.preview.devices.WearDevices
import com.bmb.runsensewear.data.run.SaveResult
import com.bmb.runsensewear.data.run.SaveState
import com.bmb.runsensewear.presentation.components.GlassCard
import com.bmb.runsensewear.presentation.components.GradientButton
import com.bmb.runsensewear.presentation.components.MetricColumn
import com.bmb.runsensewear.presentation.components.MetricDivider
import com.bmb.runsensewear.presentation.components.PulsingRing
import com.bmb.runsensewear.presentation.components.RunSenseScreen
import com.bmb.runsensewear.presentation.components.RunSenseSpinner
import com.bmb.runsensewear.presentation.components.popIn
import com.bmb.runsensewear.presentation.theme.RunSenseColors
import com.bmb.runsensewear.presentation.theme.RunSenseNumbers
import com.bmb.runsensewear.presentation.theme.RunSenseWearTheme
import com.bmb.runsensewear.presentation.theme.isCompactWatch
import androidx.compose.ui.unit.sp

/**
 * 08 러닝 완료.
 *
 * 시안: 퍼지는 링 + ✓ 뱃지 · "러닝 완료!" · 거리/시간 2분할 ·
 * "폰에서 상세 리포트 확인하기" 카드.
 *
 * 카드 아래 한 줄로 서버 저장 상태(저장 중 → 저장 완료 / 폰에서 저장 / 로그인 필요)를 보여주고,
 * 카드를 누르면 폰 앱에 리포트 열기 요청을 보냅니다.
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun RunCompleteScreen(
    onDoneClick: () -> Unit,
    modifier: Modifier = Modifier,
    distanceKm: Double = 0.0,
    elapsedText: String = "00:00",
    avgHeartRate: Int = 0,
    saveState: SaveState = SaveState.Idle,
    phoneConnected: Boolean = false,
    onOpenReportClick: () -> Unit = {}
) {
    val compact = isCompactWatch()
    val metricStyle = if (compact) RunSenseNumbers.metric.copy(fontSize = 18.sp) else RunSenseNumbers.metric

    // 완료 뱃지는 화면이 뜬 직후 튀어나오듯 등장합니다.
    var badgeVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { badgeVisible = true }

    RunSenseScreen(modifier = modifier, showTime = false) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CompleteBadge(modifier = Modifier.popIn(badgeVisible), size = if (compact) 30.dp else 46.dp)

            Text(
                text = "러닝 완료!",
                style = if (compact) MaterialTheme.typography.title2 else MaterialTheme.typography.title1,
                color = RunSenseColors.TextPrimary,
                modifier = Modifier.padding(top = if (compact) 2.dp else 4.dp)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                MetricColumn(value = String.format("%.2f", distanceKm), label = "KM", valueStyle = metricStyle, animated = false)
                MetricDivider(height = 24.dp)
                MetricColumn(value = elapsedText, label = "시간", valueStyle = metricStyle, animated = false)
                if (avgHeartRate > 0) {
                    MetricDivider(height = 24.dp)
                    MetricColumn(
                        value = avgHeartRate.toString(),
                        label = if (compact) "BPM" else "평균 BPM",
                        valueColor = RunSenseColors.Bad,
                        valueStyle = metricStyle,
                        animated = false
                    )
                }
            }

            GlassCard(
                modifier = Modifier
                    .padding(top = if (compact) 4.dp else 6.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                horizontalPadding = if (compact) 8.dp else 10.dp,
                verticalPadding = if (compact) 4.dp else 6.dp,
                onClick = onOpenReportClick
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    PhoneIcon()
                    Column {
                        Text(
                            text = if (compact) "폰에서 리포트 보기" else "폰에서 상세 리포트 확인하기",
                            style = MaterialTheme.typography.caption2,
                            color = RunSenseColors.TextSecondary,
                            textAlign = TextAlign.Start,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        SaveStatusLine(saveState = saveState, phoneConnected = phoneConnected)
                    }
                }
            }

            GradientButton(
                text = "확인",
                onClick = onDoneClick,
                verticalPadding = if (compact) 3.dp else 6.dp,
                horizontalPadding = if (compact) 18.dp else 22.dp,
                modifier = Modifier.padding(top = if (compact) 4.dp else 7.dp)
            )
        }
    }
}

/** 서버 저장 진행 상황 한 줄 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun SaveStatusLine(saveState: SaveState, phoneConnected: Boolean) {
    val (text, color) = when (saveState) {
        SaveState.Idle -> "" to RunSenseColors.TextTertiary
        SaveState.Saving -> "기록 저장 중…" to RunSenseColors.TextTertiary
        is SaveState.Done -> when (val r = saveState.result) {
            is SaveResult.Saved -> (if (r.activityId != null) "서버 저장 완료 · #${r.activityId}" else "서버 저장 완료") to RunSenseColors.Good
            SaveResult.DelegatedToPhone -> "폰 앱에서 기록 저장" to RunSenseColors.Blue300
            SaveResult.NotLoggedIn -> "로그인하면 자동 저장" to RunSenseColors.Warn
            is SaveResult.Failed -> "저장 실패 · 재시도 예정" to RunSenseColors.Warn
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        if (saveState == SaveState.Saving) RunSenseSpinner(size = 9.dp, strokeWidth = 1.5.dp)
        AnimatedContent(
            targetState = text,
            transitionSpec = { fadeIn(tween(250)).togetherWith(fadeOut(tween(120))) },
            label = "saveStatus"
        ) { value ->
            Text(
                text = if (value.isEmpty() && !phoneConnected) "폰 미연결" else value,
                style = MaterialTheme.typography.caption3,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** 퍼지는 링 + 체크마크 */
@Composable
private fun CompleteBadge(modifier: Modifier = Modifier, size: androidx.compose.ui.unit.Dp = 46.dp) {
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        PulsingRing()
        Box(
            modifier = Modifier
                .size(size - 8.dp)
                .background(RunSenseColors.Good.copy(alpha = 0.16f), CircleShape)
                .border(1.dp, RunSenseColors.Good.copy(alpha = 0.34f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "✓",
                style = MaterialTheme.typography.title1,
                color = RunSenseColors.Good
            )
        }
    }
}

/** 시안의 폰 아이콘 (테두리만 있는 둥근 사각) */
@Composable
private fun PhoneIcon(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .width(10.dp)
            .height(14.dp)
            .border(1.5.dp, RunSenseColors.Purple300, RoundedCornerShape(3.dp))
    )
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
private fun RunCompleteScreenPreview() {
    RunSenseWearTheme {
        RunCompleteScreen(
            onDoneClick = {},
            distanceKm = 5.02,
            elapsedText = "27:14",
            avgHeartRate = 154,
            saveState = SaveState.Done(SaveResult.Saved(42)),
            phoneConnected = true
        )
    }
}
