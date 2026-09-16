package com.bmb.runsensewear.presentation.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * 워치 화면 배경/버튼에 쓰이는 그라데이션 모음.
 * Wear Compose의 Colors는 단색만 지원하므로,
 * 그라데이션이 필요한 배경/버튼은 Modifier.background(brush)로
 * 이 값들을 직접 사용합니다.
 */
object RunSenseGradients {

    /** 기본 화면 배경 (대기·러닝중·심박·폼요약·AI코치·완료 화면 공통) */
    val MainBackground = Brush.linearGradient(
        0f to RunSenseColors.BgGradientStart,
        0.4f to RunSenseColors.BgGradientMid,
        1f to RunSenseColors.BgGradientEnd
    )

    /** 일시정지 화면 전용 톤다운 배경 */
    val PausedBackground = Brush.linearGradient(
        0f to RunSenseColors.BgPausedStart,
        0.4f to RunSenseColors.BgPausedMid,
        1f to RunSenseColors.BgPausedEnd
    )

    /** "시작" 버튼, AI 코치 카드, 재개 버튼 등 강조 그라데이션 */
    val PrimaryAction = Brush.linearGradient(
        listOf(RunSenseColors.Purple700, RunSenseColors.Blue600)
    )

    /** 종료 버튼 그라데이션 */
    val DangerAction = Brush.linearGradient(
        listOf(RunSenseColors.Bad2, RunSenseColors.BadDeep)
    )

    /** 러닝 중 화면 페이스 숫자에 쓰이는 텍스트 그라데이션 */
    val PaceTextGradient = Brush.linearGradient(
        listOf(Color.White, RunSenseColors.Purple400, RunSenseColors.Purple300)
    )

    /** AI 코치 말풍선 카드 (PrimaryAction보다 옅은 반투명 버전) */
    val CoachCard = Brush.linearGradient(
        listOf(
            RunSenseColors.Purple700.copy(alpha = 0.32f),
            RunSenseColors.Blue600.copy(alpha = 0.24f)
        )
    )
}
