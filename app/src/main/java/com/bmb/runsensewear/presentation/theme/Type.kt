package com.bmb.runsensewear.presentation.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Typography

/**
 * 폰트 패밀리.
 *
 * 디자인은 -apple-system / "SF Pro Display" / "Pretendard" 순으로 폴백합니다.
 * 워치(Android)에서는 Pretendard를 직접 res/font에 넣고 아래처럼 교체하세요.
 *
 *   1) Pretendard-Regular.otf, Pretendard-SemiBold.otf, Pretendard-Bold.otf 를
 *      app/src/main/res/font/ 에 추가
 *   2) 아래 RunSenseFontFamily 를 다음으로 교체
 *      val RunSenseFontFamily = FontFamily(
 *          Font(R.font.pretendard_regular, FontWeight.Normal),
 *          Font(R.font.pretendard_semibold, FontWeight.SemiBold),
 *          Font(R.font.pretendard_bold, FontWeight.Bold),
 *      )
 *
 * 폰트 파일을 아직 안 붙이셨다면 지금은 시스템 기본 폰트(FontFamily.Default)로 둬도
 * 한글은 정상 표시되니 급하지 않으면 나중에 교체하셔도 됩니다.
 */
val RunSenseFontFamily = FontFamily.Default

/** 페이스·시간·심박수 등 숫자 전용 (디자인의 'SF Mono' 대응, tabular-nums 느낌) */
val RunSenseMonoFontFamily = FontFamily.Monospace

/**
 * Wear Compose Typography.
 * 워치 화면은 정보 밀도가 낮아 대형 텍스트(display) 비중이 큽니다.
 */
val RunSenseTypography = Typography(
    // 러닝 중 화면의 대형 페이스 숫자 (예: 5'28")
    display1 = TextStyle(
        fontFamily = RunSenseMonoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 60.sp,
        letterSpacing = (-0.02).sp
    ),
    // 대기화면 시간, 심박 상세 큰 숫자
    display2 = TextStyle(
        fontFamily = RunSenseMonoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 44.sp,
        letterSpacing = (-0.02).sp
    ),
    // 폼 점수 원형 게이지 숫자
    display3 = TextStyle(
        fontFamily = RunSenseMonoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp
    ),
    // "러닝 완료!" 같은 화면 제목
    title1 = TextStyle(
        fontFamily = RunSenseFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        letterSpacing = (-0.01).sp
    ),
    title2 = TextStyle(
        fontFamily = RunSenseFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 17.sp
    ),
    title3 = TextStyle(
        fontFamily = RunSenseFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp
    ),
    // AI 코치 말풍선 본문, 카드 안 라벨
    body1 = TextStyle(
        fontFamily = RunSenseFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp
    ),
    body2 = TextStyle(
        fontFamily = RunSenseFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp
    ),
    // 버튼 텍스트 (시작 / 재개 / 종료)
    button = TextStyle(
        fontFamily = RunSenseFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        letterSpacing = 0.01.sp
    ),
    // 소문자 라벨 (KM, BPM, 시간 같은 단위)
    caption1 = TextStyle(
        fontFamily = RunSenseFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        letterSpacing = 0.08.sp
    ),
    caption2 = TextStyle(
        fontFamily = RunSenseFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp
    ),
    caption3 = TextStyle(
        fontFamily = RunSenseFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp
    )
)
