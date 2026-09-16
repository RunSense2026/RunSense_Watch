package com.bmb.runsensewear.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import com.bmb.runsensewear.presentation.theme.RunSenseColors
import com.bmb.runsensewear.presentation.theme.RunSenseGradients

/**
 * 모든 화면의 공통 껍데기.
 * 원형 워치 화면 가장자리는 잘려나가므로 안쪽 패딩을 넉넉히 둡니다.
 */
@Composable
fun RunSenseScreen(
    modifier: Modifier = Modifier,
    background: Brush = RunSenseGradients.MainBackground,
    showTime: Boolean = true,
    overlay: Color = Color.Transparent,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(background),
        contentAlignment = Alignment.Center
    ) {
        if (overlay != Color.Transparent) {
            Box(modifier = Modifier.fillMaxSize().background(overlay))
        }
        if (showTime) TimeText()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 26.dp, vertical = 22.dp),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}

/** 그라데이션 버튼. 시안의 재개/종료는 캡슐이 아니라 둥근 사각(18dp)입니다. */
@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    brush: Brush = RunSenseGradients.PrimaryAction,
    shape: Shape = CircleShape,
    horizontalPadding: Dp = 22.dp,
    verticalPadding: Dp = 10.dp
) {
    // Wear Compose 1.2의 LocalIndication은 구버전 ripple이라 새 clickable과 호환되지 않습니다.
    // indication을 끄고 누름 상태를 직접 알파로 표현합니다.
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    Box(
        modifier = modifier
            .graphicsLayer { alpha = if (pressed) 0.72f else 1f }
            .background(brush, shape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.button,
            color = RunSenseColors.TextPrimary,
            textAlign = TextAlign.Center
        )
    }
}

/** 시안 03의 원형 일시정지 버튼 (‖ 두 줄 아이콘) */
@Composable
fun PauseCircleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 34.dp
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    Box(
        modifier = modifier
            .graphicsLayer { alpha = if (pressed) 0.72f else 1f }
            .size(size)
            .background(RunSenseColors.Purple300.copy(alpha = 0.14f), CircleShape)
            .border(1.dp, RunSenseColors.Purple300.copy(alpha = 0.32f), CircleShape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            repeat(2) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(size * 0.34f)
                        .background(RunSenseColors.Purple400, RoundedCornerShape(1.dp))
                )
            }
        }
    }
}

/** 반투명 카드 (센서 항목, 지표 카드 등) */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    background: Color = RunSenseColors.CardBg,
    borderColor: Color = RunSenseColors.CardBorder,
    horizontalPadding: Dp = 12.dp,
    verticalPadding: Dp = 8.dp,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .background(background, shape)
            .border(1.dp, borderColor, shape)
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/** 시안 01의 센서 상태 칩 ("인솔 L·R", "워치 심박") */
@Composable
fun StatusChip(
    label: String,
    modifier: Modifier = Modifier,
    dotColor: Color = RunSenseColors.Good,
    pulsing: Boolean = false
) {
    GlassCard(
        modifier = modifier,
        shape = CircleShape,
        horizontalPadding = 8.dp,
        verticalPadding = 4.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (pulsing) {
                PulsingDot(color = dotColor, size = 5.dp)
            } else {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .background(dotColor, CircleShape)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.caption3,
                color = RunSenseColors.TextSecondary
            )
        }
    }
}

/** 상태 뱃지 ("적정 페이스", "Z3 · 유산소" 처럼 테두리 있는 알약) */
@Composable
fun StatusBadge(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(color.copy(alpha = 0.14f), CircleShape)
            .border(1.dp, color.copy(alpha = 0.30f), CircleShape)
            .padding(horizontal = 10.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.caption2,
            color = color
        )
    }
}

/** 둥근 사각 아이콘 뱃지 (센서 항목의 L / R / ♥, AI 코치의 "AI") */
@Composable
fun IconBadge(
    text: String,
    modifier: Modifier = Modifier,
    size: Dp = 18.dp,
    background: Color = RunSenseColors.CardBg,
    contentColor: Color = RunSenseColors.TextPrimary,
    shape: Shape = RoundedCornerShape(6.dp),
    brush: Brush? = null
) {
    Box(
        modifier = modifier
            .size(size)
            .then(
                if (brush != null) Modifier.background(brush, shape)
                else Modifier.background(background, shape)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.caption3,
            color = contentColor
        )
    }
}

/** 시안의 지표 한 칸: 큰 mono 숫자 + 대문자 라벨 */
@Composable
fun MetricColumn(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    valueColor: Color = RunSenseColors.TextPrimary
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.title2,
            color = valueColor
        )
        Text(
            text = label,
            style = MaterialTheme.typography.caption3,
            color = RunSenseColors.TextTertiary
        )
    }
}

/** 지표 사이 세로 구분선 */
@Composable
fun MetricDivider(modifier: Modifier = Modifier, height: Dp = 20.dp) {
    Box(
        modifier = modifier
            .width(1.dp)
            .height(height)
            .background(RunSenseColors.CardBorder)
    )
}

/** 좌우로 넘길 수 있다는 힌트 화살표 (‹ ›) */
@Composable
fun SwipeArrows(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "‹",
            style = MaterialTheme.typography.title2,
            color = RunSenseColors.DotInactive
        )
        Text(
            text = "›",
            style = MaterialTheme.typography.title2,
            color = RunSenseColors.DotInactive
        )
    }
}

/**
 * 페이지 위치 표시.
 * 시안대로 현재 페이지는 알약(18×7), 나머지는 원(7×7)입니다.
 */
@Composable
fun PageDots(
    count: Int,
    selectedIndex: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(count) { index ->
            val active = index == selectedIndex
            Box(
                modifier = Modifier
                    .width(if (active) 12.dp else 4.dp)
                    .height(4.dp)
                    .background(
                        if (active) RunSenseColors.DotActive else RunSenseColors.DotInactive,
                        CircleShape
                    )
            )
        }
    }
}

/**
 * 왼쪽으로 미는 제스처만 감지합니다.
 *
 * detectHorizontalDragGestures는 방향과 무관하게 터치 슬롭을 소비해버려서
 * 부모 SwipeToDismissBox(오른쪽 스와이프 뒤로가기)를 막습니다.
 * 그래서 직접 이벤트를 보면서 왼쪽으로 threshold를 넘었을 때만 소비합니다.
 */
fun Modifier.onSwipeLeft(threshold: Float = 60f, onSwipeLeft: () -> Unit): Modifier =
    this.pointerInput(onSwipeLeft) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            var dragged = 0f
            var triggered = false

            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if (!change.pressed) break

                dragged += change.positionChange().x
                if (!triggered && dragged < -threshold) triggered = true

                // 오른쪽 드래그는 절대 소비하지 않아야 뒤로가기가 살아납니다.
                if (triggered) change.consume() else if (change.isConsumed) break
            }

            if (triggered) onSwipeLeft()
        }
    }
