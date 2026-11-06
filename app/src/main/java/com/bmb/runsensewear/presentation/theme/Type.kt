package com.bmb.runsensewear.presentation.theme

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Typography
import com.bmb.runsensewear.R

/**
 * 폰트.
 *
 * - Pretendard (가변 폰트, KS X 1001 한글 2,350자 + 라틴으로 서브셋): 본문·라벨·버튼
 * - Rajdhani: 페이스·시간·거리·심박 같은 큰 숫자. 스포츠 워치 느낌의 살짝 좁은 자폭이라
 *   원형 화면에서도 큰 숫자가 잘리지 않습니다.
 */
@OptIn(ExperimentalTextApi::class)
private fun pretendard(weight: FontWeight) = Font(
    resId = R.font.pretendard,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight))
)

val PretendardFamily = FontFamily(
    pretendard(FontWeight.Normal),
    pretendard(FontWeight.Medium),
    pretendard(FontWeight.SemiBold),
    pretendard(FontWeight.Bold),
    pretendard(FontWeight.ExtraBold)
)

val RajdhaniFamily = FontFamily(
    Font(R.font.rajdhani_medium, FontWeight.Medium),
    Font(R.font.rajdhani_semibold, FontWeight.SemiBold),
    Font(R.font.rajdhani_bold, FontWeight.Bold)
)

val RunSenseFontFamily = PretendardFamily

/** 페이스·시간·심박수 등 숫자 전용 */
val RunSenseNumberFamily = RajdhaniFamily

/** 숫자 전용 스타일. Wear Typography 슬롯과 별개로 화면에서 직접 씁니다. */
object RunSenseNumbers {
    /** 러닝 중 대형 페이스 */
    val hero = TextStyle(
        fontFamily = RajdhaniFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 54.sp,
        letterSpacing = (-0.5).sp
    )

    /** 대기화면 시계, 심박 상세 큰 숫자 */
    val large = TextStyle(
        fontFamily = RajdhaniFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 44.sp,
        letterSpacing = (-0.5).sp
    )

    /** 폼 점수 게이지, 완료 화면 요약 */
    val medium = TextStyle(
        fontFamily = RajdhaniFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp
    )

    /** 거리/시간/심박 3분할 지표 */
    val metric = TextStyle(
        fontFamily = RajdhaniFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 21.sp
    )

    /** 카드 안 작은 숫자 */
    val small = TextStyle(
        fontFamily = RajdhaniFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp
    )
}

/**
 * Wear Compose Typography.
 * 워치 화면은 정보 밀도가 낮아 대형 텍스트(display) 비중이 큽니다.
 */
val RunSenseTypography = Typography(
    display1 = RunSenseNumbers.hero,
    display2 = RunSenseNumbers.large,
    display3 = RunSenseNumbers.medium,
    // "러닝 완료!" 같은 화면 제목
    title1 = TextStyle(
        fontFamily = PretendardFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 19.sp,
        letterSpacing = (-0.3).sp
    ),
    title2 = TextStyle(
        fontFamily = PretendardFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        letterSpacing = (-0.2).sp
    ),
    title3 = TextStyle(
        fontFamily = PretendardFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        letterSpacing = (-0.2).sp
    ),
    // AI 코치 말풍선 본문, 카드 안 라벨
    body1 = TextStyle(
        fontFamily = PretendardFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 19.sp,
        letterSpacing = (-0.2).sp
    ),
    body2 = TextStyle(
        fontFamily = PretendardFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.5.sp,
        letterSpacing = (-0.1).sp
    ),
    // 버튼 텍스트 (시작 / 재개 / 종료)
    button = TextStyle(
        fontFamily = PretendardFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        letterSpacing = (-0.1).sp
    ),
    // 대문자 라벨 (RUNSENSE, 페이스 / KM)
    caption1 = TextStyle(
        fontFamily = PretendardFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        letterSpacing = 1.2.sp
    ),
    caption2 = TextStyle(
        fontFamily = PretendardFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp
    ),
    // 단위 라벨 (KM, BPM)
    caption3 = TextStyle(
        fontFamily = PretendardFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 9.5.sp,
        letterSpacing = 0.8.sp
    )
)
