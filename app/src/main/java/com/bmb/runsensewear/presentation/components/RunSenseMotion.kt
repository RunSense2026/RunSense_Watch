package com.bmb.runsensewear.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bmb.runsensewear.presentation.theme.RunSenseColors
import com.bmb.runsensewear.presentation.theme.RunSenseGradients

/**
 * 디자인 시안의 CSS @keyframes를 Compose 애니메이션으로 옮긴 모음.
 * rs-pulse / rs-beat / rs-shimmer / rs-wave / rs-spin / rs-timer / rs-ring + 워치용 추가(edge arc, pop-in)
 */

/** rs-pulse — 연결 상태 점이 커졌다 작아지는 표시 */
@Composable
fun PulsingDot(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 7.dp
) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val scale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "pulseScale"
    )
    val dotAlpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "pulseAlpha"
    )
    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = dotAlpha
            }
            .background(color, CircleShape)
    )
}

/**
 * rs-beat — 심박에 맞춰 두 번 튀는 배율. 하트 아이콘에 scale로 적용합니다.
 * @param bpm 실제 심박수에 맞춰 박동 주기를 정합니다 (0이면 60bpm 기준).
 */
@Composable
fun rememberBeatScale(bpm: Int = 0): Float {
    val period = if (bpm in 30..220) (60_000 / bpm) else 1000
    val transition = rememberInfiniteTransition(label = "beat")
    val scale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = period
                1f at 0
                1.18f at (period * 0.15f).toInt()
                1f at (period * 0.30f).toInt()
                1.08f at (period * 0.45f).toInt()
                1f at (period * 0.70f).toInt()
            }
        ),
        label = "beatScale"
    )
    return scale
}

/**
 * rs-shimmer — 보라색 글로우가 숨쉬듯 번지는 효과.
 * CSS box-shadow에 대응하는 API가 없어 뒤에 radial gradient를 깔아 근사합니다.
 * 강조하려는 요소와 같은 크기의 Box에 배경으로 두고 쓰세요.
 */
@Composable
fun ShimmerGlow(
    modifier: Modifier = Modifier,
    color: Color = RunSenseColors.Purple300
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val glow by transition.animateFloat(
        initialValue = 0.18f,
        targetValue = 0.40f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse),
        label = "glowAlpha"
    )
    Canvas(modifier = modifier) {
        val radius = size.minDimension * 0.75f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(color.copy(alpha = glow), Color.Transparent),
                radius = radius
            ),
            radius = radius
        )
    }
}

/** rs-wave — AI 코치 음성 안내 파형 5개 막대 */
@Composable
fun VoiceWaveBars(
    modifier: Modifier = Modifier,
    barHeight: Dp = 22.dp,
    active: Boolean = true
) {
    val transition = rememberInfiniteTransition(label = "wave")
    val colors = listOf(
        RunSenseColors.Purple300,
        RunSenseColors.Purple300,
        RunSenseColors.Blue300,
        RunSenseColors.Purple300,
        RunSenseColors.Purple300
    )
    Row(
        modifier = modifier.height(barHeight),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        colors.forEachIndexed { index, color ->
            val spec: InfiniteRepeatableSpec<Float> = infiniteRepeatable(
                animation = tween(900),
                repeatMode = RepeatMode.Reverse,
                initialStartOffset = StartOffset(index * 120)
            )
            val animated by transition.animateFloat(
                initialValue = 0.35f,
                targetValue = 1f,
                animationSpec = spec,
                label = "wave$index"
            )
            val scaleY by animateFloatAsState(
                targetValue = if (active) animated else 0.25f,
                label = "waveActive$index"
            )
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(barHeight)
                    .scale(scaleX = 1f, scaleY = scaleY)
                    .background(color, RoundedCornerShape(2.dp))
            )
        }
    }
}

/** rs-spin — 센서 연결 확인 중 스피너 */
@Composable
fun RunSenseSpinner(
    modifier: Modifier = Modifier,
    size: Dp = 26.dp,
    strokeWidth: Dp = 3.dp
) {
    val transition = rememberInfiniteTransition(label = "spin")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing)),
        label = "spinAngle"
    )
    Canvas(modifier = modifier.size(size)) {
        val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
        val inset = stroke.width / 2f
        val arcSize = Size(this.size.width - stroke.width, this.size.height - stroke.width)
        val topLeft = Offset(inset, inset)
        drawArc(
            color = RunSenseColors.Purple300.copy(alpha = 0.18f),
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = stroke
        )
        drawArc(
            brush = Brush.sweepGradient(
                listOf(Color.Transparent, RunSenseColors.Purple300, RunSenseColors.Blue500),
                center = Offset(this.size.width / 2, this.size.height / 2)
            ),
            startAngle = angle,
            sweepAngle = 110f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = stroke
        )
    }
}

/** rs-timer — AI 코치가 자동으로 닫히기까지 남은 시간 막대 (한 번만 줄어듭니다) */
@Composable
fun AutoDismissBar(
    durationMillis: Int,
    modifier: Modifier = Modifier
) {
    val fraction = remember { Animatable(1f) }
    LaunchedEffect(durationMillis) {
        fraction.snapTo(1f)
        fraction.animateTo(0f, tween(durationMillis, easing = LinearEasing))
    }
    Box(
        modifier = modifier
            .height(4.dp)
            .background(RunSenseColors.CardBorder, CircleShape)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.value.coerceIn(0f, 1f))
                .height(4.dp)
                .background(RunSenseGradients.PrimaryAction, CircleShape)
        )
    }
}

/** rs-ring — 러닝 완료 체크마크 뒤로 퍼져나가는 링 */
@Composable
fun PulsingRing(
    modifier: Modifier = Modifier,
    color: Color = RunSenseColors.Good
) {
    val transition = rememberInfiniteTransition(label = "ring")
    val scale by transition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.45f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing)),
        label = "ringScale"
    )
    val ringAlpha by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing)),
        label = "ringAlpha"
    )
    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = ringAlpha
            }
            .background(color.copy(alpha = 0.22f), CircleShape)
    )
}

/**
 * 화면 가장자리를 따라 도는 진행 링 (러닝 중 페이스 · 심박 상세 강도).
 * 원형 워치 베젤 안쪽을 따라 그려져 삼성 헬스 같은 느낌을 냅니다.
 *
 * @param progress 0~1. 12시 방향에서 시계 방향으로 찹니다.
 * @param sweepDegrees 링이 차지하는 최대 각도 (기본 300° — 아래쪽을 비워 둡니다)
 */
@Composable
fun EdgeArc(
    progress: Float,
    modifier: Modifier = Modifier,
    brush: Brush = Brush.sweepGradient(
        0f to RunSenseColors.Blue500,
        0.5f to RunSenseColors.Purple700,
        1f to RunSenseColors.Purple300
    ),
    trackColor: Color = Color.White.copy(alpha = 0.08f),
    strokeWidth: Dp = 5.dp,
    inset: Dp = 5.dp,
    sweepDegrees: Float = 300f
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioNoBouncy),
        label = "edgeArc"
    )
    val startAngle = -90f - sweepDegrees / 2f
    Canvas(modifier = modifier.fillMaxSize()) {
        val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
        val pad = inset.toPx() + stroke.width / 2f
        val arcSize = Size(size.width - pad * 2, size.height - pad * 2)
        val topLeft = Offset(pad, pad)
        drawArc(
            color = trackColor,
            startAngle = startAngle,
            sweepAngle = sweepDegrees,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = stroke
        )
        if (animated > 0f) {
            drawArc(
                brush = brush,
                startAngle = startAngle,
                sweepAngle = sweepDegrees * animated,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke
            )
        }
    }
}

/** 처음 나타날 때 살짝 커지며 등장하는 효과 (체크마크·뱃지) */
@Composable
fun Modifier.popIn(visible: Boolean = true): Modifier {
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "popIn"
    )
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
        alpha = scale.coerceIn(0f, 1f)
    }
}
