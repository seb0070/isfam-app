package com.isfam.core.designsystem

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * IsFam 타이포그래피.
 * UI 키트 HTML 의 font 선언을 그대로 옮겼습니다.
 *
 *   화면 제목  800 27px  · tracking -3%
 *   온보딩 제목 800 26px/1.35 · tracking -3%
 *   섹션 제목  700 16px
 *   본문       500 14px/1.65   (온보딩 본문은 500 15px/1.7)
 *   캡션       600 12px
 *
 * ── Pretendard 적용 ──────────────────────────────────────────
 * res/font/ 에 pretendard_{medium,semibold,bold,extrabold}.ttf 를 넣고
 * 아래 IsFamFontFamily 주석을 해제하세요. 화면 코드는 안 바꿔도 됩니다.
 */

// val IsFamFontFamily = FontFamily(
//     Font(R.font.pretendard_medium, FontWeight.Medium),
//     Font(R.font.pretendard_semibold, FontWeight.SemiBold),
//     Font(R.font.pretendard_bold, FontWeight.Bold),
//     Font(R.font.pretendard_extrabold, FontWeight.ExtraBold),
// )
private val IsFamFontFamily = FontFamily.Default

val IsFamTypography = Typography(
    /** 스플래시 로고 40px */
    displayLarge = TextStyle(
        fontFamily = IsFamFontFamily, fontSize = 40.sp, lineHeight = 40.sp,
        fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.03).em,
    ),
    /** 큰 숫자 (결과 화면 점수) */
    displayMedium = TextStyle(
        fontFamily = IsFamFontFamily, fontSize = 48.sp, lineHeight = 52.sp,
        fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.03).em,
    ),
    /** 화면 제목 27 */
    headlineLarge = TextStyle(
        fontFamily = IsFamFontFamily, fontSize = 27.sp, lineHeight = 36.sp,
        fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.03).em,
    ),
    /** 온보딩 제목 26 / 1.35 */
    headlineMedium = TextStyle(
        fontFamily = IsFamFontFamily, fontSize = 26.sp, lineHeight = 35.sp,
        fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.03).em,
    ),
    /** 섹션 제목 16 */
    titleMedium = TextStyle(
        fontFamily = IsFamFontFamily, fontSize = 16.sp, lineHeight = 24.sp,
        fontWeight = FontWeight.Bold,
    ),
    titleSmall = TextStyle(
        fontFamily = IsFamFontFamily, fontSize = 13.5f.sp, lineHeight = 20.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    /** 온보딩 본문 15 / 1.7 */
    bodyLarge = TextStyle(
        fontFamily = IsFamFontFamily, fontSize = 15.sp, lineHeight = 25.5f.sp,
        fontWeight = FontWeight.Medium,
    ),
    /** 본문 14 / 1.65 */
    bodyMedium = TextStyle(
        fontFamily = IsFamFontFamily, fontSize = 14.sp, lineHeight = 23.sp,
        fontWeight = FontWeight.Medium,
    ),
    /** 캡션 12 */
    bodySmall = TextStyle(
        fontFamily = IsFamFontFamily, fontSize = 12.sp, lineHeight = 18.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    /** 버튼 라벨 16 */
    labelLarge = TextStyle(
        fontFamily = IsFamFontFamily, fontSize = 16.sp, lineHeight = 24.sp,
        fontWeight = FontWeight.Bold,
    ),
    /** 아이브로우 · 작은 강조 12 */
    labelMedium = TextStyle(
        fontFamily = IsFamFontFamily, fontSize = 12.sp, lineHeight = 16.sp,
        fontWeight = FontWeight.Bold,
    ),
)