package com.bmb.runsensewear.presentation.screens

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.tooling.preview.devices.WearDevices
import com.bmb.runsensewear.presentation.components.GlassCard
import com.bmb.runsensewear.presentation.components.GradientButton
import com.bmb.runsensewear.presentation.components.MetricColumn
import com.bmb.runsensewear.presentation.components.MetricDivider
import com.bmb.runsensewear.presentation.components.PulsingRing
import com.bmb.runsensewear.presentation.components.RunSenseScreen
import com.bmb.runsensewear.presentation.theme.RunSenseColors
import com.bmb.runsensewear.presentation.theme.RunSenseWearTheme

/**
 * 08 러닝 완료.
 *
 * 시안: 퍼지는 링 + ✓ 뱃지 · "러닝 완료!" · 거리/시간 2분할 ·
 * "폰에서 상세 리포트 확인하기" 카드.
 *
 * 시안에는 버튼이 없지만 NavGraph가 대기화면 복귀를 위해 onDoneClick을 요구해서
 * 하단에 작은 확인 버튼을 두었습니다.
 *
 * TODO: 여기서 PhoneSyncManager로 세션 결과를 폰 앱에 전송하세요.
 */
@Composable
fun RunCompleteScreen(
    onDoneClick: () -> Unit,
    modifier: Modifier = Modifier,
    distanceKm: Double = 0.0,
    elapsedText: String = "00:00"
) {
    RunSenseScreen(modifier = modifier, showTime = false) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CompleteBadge()

            Text(
                text = "러닝 완료!",
                style = MaterialTheme.typography.title1,
                color = RunSenseColors.TextPrimary,
                modifier = Modifier.padding(top = 4.dp)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                MetricColumn(value = String.format("%.2f", distanceKm), label = "KM")
                MetricDivider(height = 24.dp)
                MetricColumn(value = elapsedText, label = "시간")
            }

            GlassCard(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                horizontalPadding = 10.dp,
                verticalPadding = 6.dp
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    PhoneIcon()
                    Text(
                        text = "폰에서 상세 리포트 확인하기",
                        style = MaterialTheme.typography.caption2,
                        color = RunSenseColors.TextSecondary,
                        textAlign = TextAlign.Start
                    )
                }
            }

            GradientButton(
                text = "확인",
                onClick = onDoneClick,
                verticalPadding = 6.dp,
                modifier = Modifier.padding(top = 7.dp)
            )
        }
    }
}

/** 퍼지는 링 + 체크마크 */
@Composable
private fun CompleteBadge(modifier: Modifier = Modifier) {
    Box(modifier = modifier.size(46.dp), contentAlignment = Alignment.Center) {
        PulsingRing()
        Box(
            modifier = Modifier
                .size(38.dp)
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
            elapsedText = "27:14"
        )
    }
}
