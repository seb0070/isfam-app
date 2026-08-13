package com.isfam.core.designsystem

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * IsFam 타이포그래피.
 * UI 키트 "타이포그래피" 정의를 Material 3 스케일에 매핑했습니다.
 *
 *   화면 제목  27 Bold      · tracking -3%
 *   섹션 제목  16 SemiBold
 *   본문       14 Regular   · 행간 1.65
 *   캡션       12 Medium
 *
 * ── Pretendard 적용 방법 ──────────────────────────────────────
 * 1. pretendard-{regular,medium,semibold,bold}.ttf 를 내려받아
 *    app/src/main/res/font/ 에 넣습니다 (파일명은 소문자·언더스코어만)
 * 2. 아래 IsFamFontFamily 주석을 해제합니다
 *
 * 지금은 시스템 기본 폰트로 동작합니다. 폰트만 나중에 갈아 끼우면 됩니다.
 */

// val IsFamFontFamily = FontFamily(
//     Font(R.font.pretendard_regular, FontWeight.Normal),
//     Font(R.font.pretendard_medium, FontWeight.Medium),
//     Font(R.font.pretendard_semibold, FontWeight.SemiBold),
//     Font(R.font.pretendard_bold, FontWeight.Bold),
// )
private val IsFamFontFamily = FontFamily.Default

val IsFamTypography = Typography(
    // 화면 제목 27 Bold · tracking -3%
    headlineLarge = TextStyle(
        fontFamily = IsFamFontFamily,
        fontSize = 27.sp,
        lineHeight = 36.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.03).em,
    ),
    // 큰 숫자 강조 (결과 화면의 일치 점수 등)
    displayMedium = TextStyle(
        fontFamily = IsFamFontFamily,
        fontSize = 48.sp,
        lineHeight = 56.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.03).em,
    ),
    headlineMedium = TextStyle(
        fontFamily = IsFamFontFamily,
        fontSize = 22.sp,
        lineHeight = 30.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.02).em,
    ),
    // 섹션 제목 16 SemiBold
    titleMedium = TextStyle(
        fontFamily = IsFamFontFamily,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    titleSmall = TextStyle(
        fontFamily = IsFamFontFamily,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    // 본문 14 Regular · 행간 1.65 (14 × 1.65 ≈ 23)
    bodyMedium = TextStyle(
        fontFamily = IsFamFontFamily,
        fontSize = 14.sp,
        lineHeight = 23.sp,
        fontWeight = FontWeight.Normal,
    ),
    bodyLarge = TextStyle(
        fontFamily = IsFamFontFamily,
        fontSize = 16.sp,
        lineHeight = 26.sp,
        fontWeight = FontWeight.Normal,
    ),
    // 캡션 12 Medium
    bodySmall = TextStyle(
        fontFamily = IsFamFontFamily,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.Medium,
    ),
    // 버튼 라벨
    labelLarge = TextStyle(
        fontFamily = IsFamFontFamily,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.SemiBold,
    ),
)