package com.bmb.runsensewear.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.tooling.preview.devices.WearDevices
import com.bmb.runsensewear.presentation.components.RunSenseScreen
import com.bmb.runsensewear.presentation.components.ShimmerGlow
import com.bmb.runsensewear.presentation.components.StatusChip
import com.bmb.runsensewear.presentation.theme.RunSenseColors
import com.bmb.runsensewear.presentation.theme.RunSenseGradients
import com.bmb.runsensewear.presentation.theme.RunSenseWearTheme
import kotlinx.coroutines.delay
import java.util.Calendar

/**
 * 01 메인 대기화면.
 *
 * 시안: RUNSENSE 라벨 · 큰 현재 시각 · 센서 연결 칩 2개 · 원형 "시작 / RUN" 버튼.
 *
 * @param insolesConnected 좌우 인솔 연결 여부
 * @param heartBandConnected 심박밴드 연결 여부
 */
@Composable
fun HomeScreen(
    onStartRunClick: () -> Unit,
    modifier: Modifier = Modifier,
    insolesConnected: Boolean = false,
    heartBandConnected: Boolean = false
) {
    val clock = rememberClockText()

    // 시안 01은 큰 시계가 주인공이라 시스템 TimeText와 겹치지 않게 숨깁니다.
    RunSenseScreen(modifier = modifier, showTime = false) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(RunSenseGradients.PrimaryAction, CircleShape)
                )
                Text(
                    text = "RUNSENSE",
                    style = MaterialTheme.typography.caption1,
                    color = RunSenseColors.TextTertiary
                )
            }

            Text(
                text = clock,
                style = MaterialTheme.typography.display2.copy(fontSize = 38.sp),
                color = RunSenseColors.TextPrimary,
                modifier = Modifier.padding(top = 2.dp)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 6.dp)
            ) {
                StatusChip(
                    label = "인솔 L·R",
                    dotColor = if (insolesConnected) RunSenseColors.Good
                    else RunSenseColors.TextTertiary
                )
                StatusChip(
                    label = "심박밴드",
                    dotColor = if (heartBandConnected) RunSenseColors.Good
                    else RunSenseColors.TextTertiary,
                    pulsing = heartBandConnected
                )
            }

            StartRunButton(
                onClick = onStartRunClick,
                modifier = Modifier.padding(top = 10.dp)
            )
        }
    }
}

/** 시안의 원형 시작 버튼 — 보라 글로우가 숨쉬는 그라데이션 원 */
@Composable
private fun StartRunButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    Box(modifier = modifier.size(88.dp), contentAlignment = Alignment.Center) {
        ShimmerGlow(modifier = Modifier.size(88.dp))
        Column(
            modifier = Modifier
                .graphicsLayer { alpha = if (pressed) 0.72f else 1f }
                .size(68.dp)
                .background(RunSenseGradients.PrimaryAction, CircleShape)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = onClick
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "시작",
                style = MaterialTheme.typography.title2,
                color = RunSenseColors.TextPrimary
            )
            Text(
                text = "RUN",
                style = MaterialTheme.typography.caption3,
                color = RunSenseColors.TextSecondary
            )
        }
    }
}

/** 분이 바뀔 때마다 갱신되는 "H:mm" 문자열 */
@Composable
private fun rememberClockText(): String {
    var text by remember { mutableStateOf(currentClockText()) }
    LaunchedEffect(Unit) {
        while (true) {
            text = currentClockText()
            delay(10_000)
        }
    }
    return text
}

private fun currentClockText(): String {
    val now = Calendar.getInstance()
    return String.format(
        "%d:%02d",
        now.get(Calendar.HOUR).let { if (it == 0) 12 else it },
        now.get(Calendar.MINUTE)
    )
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
private fun HomeScreenPreview() {
    RunSenseWearTheme {
        HomeScreen(
            onStartRunClick = {},
            insolesConnected = true,
            heartBandConnected = true
        )
    }
}
