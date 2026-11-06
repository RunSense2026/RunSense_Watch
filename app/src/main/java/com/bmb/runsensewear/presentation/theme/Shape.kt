package com.bmb.runsensewear.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Shapes

/**
 * 디자인에서 실제 쓰인 border-radius 값들을 그대로 반영.
 * 작은 뱃지(999px = 완전한 원/캡슐)는 각 화면에서
 * CircleShape 또는 RoundedCornerShape(percent = 50)를 직접 쓰면 됩니다.
 */
val RunSenseShapes = Shapes(
    small = RoundedCornerShape(12.dp),   // 심박존 막대, 작은 뱃지
    medium = RoundedCornerShape(16.dp),  // 센서 연결 리스트 아이템
    large = RoundedCornerShape(22.dp)    // AI 코치 카드, 큰 카드
)
