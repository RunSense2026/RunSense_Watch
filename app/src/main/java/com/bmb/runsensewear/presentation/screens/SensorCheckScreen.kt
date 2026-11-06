package com.bmb.runsensewear.presentation.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.tooling.preview.devices.WearDevices
import com.bmb.runsensewear.presentation.components.GlassCard
import com.bmb.runsensewear.presentation.components.IconBadge
import com.bmb.runsensewear.presentation.components.PulsingDot
import com.bmb.runsensewear.presentation.components.RunSenseScreen
import com.bmb.runsensewear.presentation.components.RunSenseSpinner
import com.bmb.runsensewear.presentation.components.popIn
import com.bmb.runsensewear.presentation.run.SensorCheckState
import com.bmb.runsensewear.presentation.run.SensorRow
import com.bmb.runsensewear.presentation.run.SensorStatus
import com.bmb.runsensewear.presentation.theme.RunSenseColors
import com.bmb.runsensewear.presentation.theme.RunSenseWearTheme

/**
 * 02 센서 연결 확인 중.
 *
 * 왼쪽/오른쪽 인솔은 폰 앱이 BLE로 연결해 Data Layer로 상태를 알려주고,
 * GPS는 워치가 직접 잡습니다. 실제 확인 흐름은 [com.bmb.runsensewear.presentation.run.RunViewModel.beginSensorCheck].
 *
 * 폰이나 인솔이 없어도 시간이 지나면 "미연결"로 표시하고 워치 단독으로 러닝을 시작합니다.
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SensorCheckScreen(
    state: SensorCheckState,
    onEnter: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(Unit) { onEnter() }

    RunSenseScreen(modifier = modifier, showTime = false) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            RunSenseSpinner(size = 22.dp, strokeWidth = 2.5.dp)

            AnimatedContent(
                targetState = state.headline,
                transitionSpec = { fadeIn(tween(250)).togetherWith(fadeOut(tween(150))) },
                label = "headline"
            ) { headline ->
                Text(
                    text = headline,
                    style = MaterialTheme.typography.title3,
                    color = RunSenseColors.TextPrimary,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            Column(
                modifier = Modifier.padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                state.rows.forEach { row -> SensorRowItem(row) }
            }
        }
    }
}

@Composable
private fun SensorRowItem(row: SensorRow, modifier: Modifier = Modifier) {
    val checking = row.status == SensorStatus.Checking
    val connected = row.status == SensorStatus.Connected
    val unavailable = row.status == SensorStatus.Unavailable

    // 연결 중인 항목은 시안처럼 보라 배경/테두리로 강조하고, 상태가 바뀌면 색이 부드럽게 넘어갑니다.
    val background by animateColorAsState(
        targetValue = if (checking) RunSenseColors.Purple300.copy(alpha = 0.12f) else RunSenseColors.CardBg,
        animationSpec = tween(300),
        label = "rowBg"
    )
    val border by animateColorAsState(
        targetValue = when {
            checking -> RunSenseColors.Purple300.copy(alpha = 0.28f)
            connected -> RunSenseColors.Good.copy(alpha = 0.22f)
            else -> RunSenseColors.CardBorder
        },
        animationSpec = tween(300),
        label = "rowBorder"
    )
    val badgeColor = when {
        unavailable -> RunSenseColors.TextTertiary
        row.badge == "G" -> RunSenseColors.Blue300
        else -> RunSenseColors.Good
    }

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(13.dp),
        background = background,
        borderColor = border,
        horizontalPadding = 9.dp,
        verticalPadding = 5.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconBadge(
                text = row.badge,
                size = 19.dp,
                background = badgeColor.copy(alpha = 0.16f),
                contentColor = badgeColor,
                shape = RoundedCornerShape(7.dp)
            )
            Text(
                text = row.label,
                style = MaterialTheme.typography.body2,
                color = if (connected || checking) RunSenseColors.TextPrimary
                else RunSenseColors.TextTertiary,
                modifier = Modifier.weight(1f)
            )
            when (row.status) {
                SensorStatus.Connected -> Text(
                    text = "✓",
                    style = MaterialTheme.typography.title3,
                    color = RunSenseColors.Good,
                    modifier = Modifier.popIn()
                )

                SensorStatus.Checking -> PulsingDot(color = RunSenseColors.Purple300, size = 7.dp)

                SensorStatus.Unavailable -> Text(
                    text = "미연결",
                    style = MaterialTheme.typography.caption3,
                    color = RunSenseColors.Warn.copy(alpha = 0.8f),
                    modifier = Modifier.popIn()
                )

                SensorStatus.Waiting -> Text(
                    text = "–",
                    style = MaterialTheme.typography.caption2,
                    color = RunSenseColors.TextTertiary
                )
            }
        }
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
private fun SensorCheckScreenPreview() {
    RunSenseWearTheme {
        SensorCheckScreen(
            state = SensorCheckState(
                rows = listOf(
                    SensorRow("L", "왼쪽 인솔", SensorStatus.Connected),
                    SensorRow("R", "오른쪽 인솔", SensorStatus.Checking),
                    SensorRow("G", "워치 GPS", SensorStatus.Waiting)
                )
            ),
            onEnter = {}
        )
    }
}
