package com.bmb.runsensewear.presentation.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.tooling.preview.devices.WearDevices
import com.bmb.runsensewear.presentation.components.GradientButton
import com.bmb.runsensewear.presentation.components.RunSenseScreen
import com.bmb.runsensewear.presentation.components.ShimmerGlow
import com.bmb.runsensewear.presentation.components.StatusChip
import com.bmb.runsensewear.presentation.theme.RunSenseColors
import com.bmb.runsensewear.presentation.theme.RunSenseGradients
import com.bmb.runsensewear.presentation.theme.RunSenseNumbers
import com.bmb.runsensewear.presentation.theme.RunSenseWearTheme
import kotlinx.coroutines.delay
import java.util.Calendar

/**
 * 01 메인 대기화면.
 *
 * 시안: RUNSENSE 라벨 · 큰 현재 시각 · 센서 연결 칩 2개 · 원형 "시작 / RUN" 버튼.
 *
 * 심박은 별도 밴드 없이 워치 내장 센서로 측정하므로, 두 번째 칩은
 * 페어링 상태가 아니라 "이 워치에서 심박을 읽을 수 있는가"를 나타냅니다.
 *
 * @param insolesConnected 좌우 인솔 연결 여부 (폰 앱이 BLE로 연결해 알려줌)
 * @param heartRateReady 워치 심박 센서 사용 가능 여부 (권한 + 기기 지원)
 * @param phoneConnected 폰이 블루투스로 붙어 있는지
 * @param userName 폰에서 로그인 정보가 넘어왔으면 이름
 * @param runInProgress 러닝이 진행 중인데 대기화면으로 밀려 나온 경우 → "돌아가기" 버튼
 */
@Composable
fun HomeScreen(
    onStartRunClick: () -> Unit,
    modifier: Modifier = Modifier,
    insolesConnected: Boolean = false,
    heartRateReady: Boolean = false,
    phoneConnected: Boolean = false,
    userName: String? = null,
    runInProgress: Boolean = false,
    onResumeRunClick: () -> Unit = {}
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
                style = RunSenseNumbers.large.copy(fontSize = 42.sp),
                color = RunSenseColors.TextPrimary,
                modifier = Modifier.padding(top = 2.dp)
            )

            Text(
                text = when {
                    userName != null -> "$userName 님, 달릴 준비 됐어요"
                    phoneConnected -> "폰 연결됨 · 로그인 대기"
                    else -> "폰 미연결 · 워치 단독 모드"
                },
                style = MaterialTheme.typography.caption3,
                color = RunSenseColors.TextTertiary
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
                    label = "워치 심박",
                    dotColor = if (heartRateReady) RunSenseColors.Good
                    else RunSenseColors.TextTertiary,
                    pulsing = heartRateReady
                )
            }

            if (runInProgress) {
                GradientButton(
                    text = "▶ 러닝으로 돌아가기",
                    onClick = onResumeRunClick,
                    verticalPadding = 8.dp,
                    horizontalPadding = 16.dp,
                    modifier = Modifier.padding(top = 12.dp)
                )
            } else {
                StartRunButton(
                    onClick = onStartRunClick,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

/** 시안의 원형 시작 버튼 — 보라 글로우가 숨쉬고, 바깥에 점선 링이 천천히 돕니다 */
@Composable
private fun StartRunButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    val transition = rememberInfiniteTransition(label = "startRing")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(14_000, easing = LinearEasing)),
        label = "ringAngle"
    )

    Box(modifier = modifier.size(88.dp), contentAlignment = Alignment.Center) {
        ShimmerGlow(modifier = Modifier.size(88.dp))
        Canvas(modifier = Modifier.size(82.dp).graphicsLayer { rotationZ = angle }) {
            val stroke = Stroke(
                width = 1.5.dp.toPx(),
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 9f))
            )
            drawCircle(
                brush = Brush.sweepGradient(
                    listOf(
                        RunSenseColors.Purple300.copy(alpha = 0.05f),
                        RunSenseColors.Purple300.copy(alpha = 0.6f),
                        RunSenseColors.Blue300.copy(alpha = 0.05f)
                    ),
                    center = Offset(size.width / 2, size.height / 2)
                ),
                radius = size.minDimension / 2 - stroke.width,
                style = stroke
            )
        }
        Column(
            modifier = Modifier
                .graphicsLayer {
                    alpha = if (pressed) 0.78f else 1f
                    val s = if (pressed) 0.94f else 1f
                    scaleX = s
                    scaleY = s
                }
                .size(66.dp)
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
            heartRateReady = true,
            userName = "홍길동"
        )
    }
}
