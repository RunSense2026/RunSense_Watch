package com.bmb.runsensewear.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.bmb.runsensewear.presentation.theme.RunSenseColors
import com.bmb.runsensewear.presentation.theme.RunSenseWearTheme
import kotlinx.coroutines.delay

/** 02 화면의 센서 항목. 워치 내장 심박은 페어링이 필요 없어 좌우 인솔 2종만 확인합니다. */
data class SensorItem(
    val badge: String,
    val label: String,
    val badgeColor: Color
)

private val SensorItems = listOf(
    SensorItem(badge = "L", label = "왼쪽 인솔", badgeColor = RunSenseColors.Good),
    SensorItem(badge = "R", label = "오른쪽 인솔", badgeColor = RunSenseColors.Good)
)

/**
 * 02 센서 연결 확인 중.
 *
 * 심박은 별도 밴드 없이 워치 내장 센서를 쓰므로 여기서 확인하지 않습니다.
 *
 * TODO: 지금은 순차 연결을 시간으로 흉내냅니다.
 *       인솔 BLE 연동이 끝나면 아래 LaunchedEffect를
 *       실제 센서 상태 Flow 구독으로 교체하세요.
 */
@Composable
fun SensorCheckScreen(
    onAllSensorsConnected: () -> Unit,
    modifier: Modifier = Modifier
) {
    var connectedCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (connectedCount < SensorItems.size) {
            delay(700)
            connectedCount++
        }
        delay(400)
        onAllSensorsConnected()
    }

    RunSenseScreen(modifier = modifier, showTime = false) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            RunSenseSpinner(size = 22.dp, strokeWidth = 2.5.dp)

            Text(
                text = "센서 연결 확인 중…",
                style = MaterialTheme.typography.title3,
                color = RunSenseColors.TextPrimary,
                modifier = Modifier.padding(top = 6.dp)
            )

            Column(
                modifier = Modifier.padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                SensorItems.forEachIndexed { index, item ->
                    SensorRow(
                        item = item,
                        connected = index < connectedCount,
                        connecting = index == connectedCount
                    )
                }
            }
        }
    }
}

@Composable
private fun SensorRow(
    item: SensorItem,
    connected: Boolean,
    connecting: Boolean,
    modifier: Modifier = Modifier
) {
    // 연결 중인 항목은 시안처럼 보라 배경/테두리로 강조합니다.
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        background = if (connecting) RunSenseColors.Purple300.copy(alpha = 0.12f)
        else RunSenseColors.CardBg,
        borderColor = if (connecting) RunSenseColors.Purple300.copy(alpha = 0.28f)
        else RunSenseColors.CardBorder,
        horizontalPadding = 10.dp,
        verticalPadding = 6.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconBadge(
                text = item.badge,
                size = 20.dp,
                background = item.badgeColor.copy(alpha = 0.16f),
                contentColor = item.badgeColor,
                shape = RoundedCornerShape(7.dp)
            )
            Text(
                text = item.label,
                style = MaterialTheme.typography.body2,
                color = if (connected || connecting) RunSenseColors.TextPrimary
                else RunSenseColors.TextTertiary,
                modifier = Modifier.weight(1f)
            )
            when {
                connected -> Text(
                    text = "✓",
                    style = MaterialTheme.typography.title3,
                    color = RunSenseColors.Good
                )

                connecting -> PulsingDot(color = RunSenseColors.Purple300, size = 7.dp)

                else -> Text(
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
    RunSenseWearTheme { SensorCheckScreen(onAllSensorsConnected = {}) }
}
