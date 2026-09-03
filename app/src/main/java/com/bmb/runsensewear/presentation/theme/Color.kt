package com.bmb.runsensewear.presentation.theme

import androidx.compose.ui.graphics.Color

/**
 * 색상 토큰
 */
object RunSenseColors {

    // ── 보라 계열 (Primary) ──────────────────────────
    val Purple300 = Color(0xFFA78BFA)
    val Purple400 = Color(0xFFC4B5FD)
    val Purple700 = Color(0xFF7C3AED)
    val Purple900 = Color(0xFF5B21B6)

    // ── 파랑 계열 (Secondary) ────────────────────────
    val Blue300 = Color(0xFF93C5FD)
    val Blue500 = Color(0xFF60A5FA)
    val Blue600 = Color(0xFF3B82F6)
    val Blue700 = Color(0xFF2563EB)

    // ── 상태 색상 (좋음 / 주의 / 위험) ──────────────
    val Good = Color(0xFF6EE7B7)      // 폼 좋음, Z2, 연결 완료 상태
    val Good2 = Color(0xFF10B981)
    val Warn = Color(0xFFFBBF24)      // 좌우 균형 주의, Z4
    val Warn2 = Color(0xFFF59E0B)
    val Bad = Color(0xFFF87171)       // 심박 하트, Z5
    val Bad2 = Color(0xFFEF4444)      // 종료 버튼 그라데이션 시작
    val BadDeep = Color(0xFFDC2626)   // 종료 버튼 그라데이션 끝

    // ── 배경 그라데이션 (기본 상태) ──────────────────
    val BgGradientStart = Color(0xFF0F0A2E)
    val BgGradientMid = Color(0xFF1A1250)
    val BgGradientEnd = Color(0xFF0E2A50)

    // ── 배경 그라데이션 (일시정지 · 톤다운) ──────────
    val BgPausedStart = Color(0xFF0B0820)
    val BgPausedMid = Color(0xFF120D38)
    val BgPausedEnd = Color(0xFF0A1D38)

    // ── 워치 베젤 링 ─────────────────────────────────
    val BezelOuter = Color(0xFF060412)
    val BezelInner = Color(0x38A78BFA) // rgba(167,139,250,0.22)

    // ── 카드/표면 ────────────────────────────────────
    val CardBg = Color(0x0FFFFFFF)       // rgba(255,255,255,0.06)
    val CardBorder = Color(0x14FFFFFF)   // rgba(255,255,255,0.08)
    val SurfaceSolid = Color(0xFF120C33) // 폼 점수 게이지 안쪽처럼 불투명이 필요한 곳

    // ── 텍스트 ───────────────────────────────────────
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xA8FFFFFF) // rgba(255,255,255,0.66)
    val TextTertiary = Color(0x66FFFFFF)  // rgba(255,255,255,0.40)

    // ── 페이지 인디케이터(점) ────────────────────────
    val DotInactive = Color(0x38FFFFFF)  // rgba(255,255,255,0.22)
    val DotActive = Purple300
}