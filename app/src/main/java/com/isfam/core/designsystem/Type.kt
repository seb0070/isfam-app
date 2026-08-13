package com.isfam.core.designsystem

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * 타이포그래피.
 *
 * 고령 사용자를 고려해 Material 3 기본값보다 한 단계씩 키웠습니다.
 * bodyLarge 가 18sp 인 것은 의도적입니다 (기본값 16sp).
 *
 * 시스템 글꼴 크기 설정을 따라가야 하므로 단위는 dp 가 아니라 sp 를 씁니다.
 */
val IsFamTypography = Typography(
    displaySmall = TextStyle(
        fontSize = 34.sp, lineHeight = 42.sp, fontWeight = FontWeight.Bold,
    ),
    headlineLarge = TextStyle(
        fontSize = 30.sp, lineHeight = 38.sp, fontWeight = FontWeight.Bold,
    ),
    headlineMedium = TextStyle(
        fontSize = 26.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold,
    ),
    headlineSmall = TextStyle(
        fontSize = 22.sp, lineHeight = 30.sp, fontWeight = FontWeight.SemiBold,
    ),
    titleLarge = TextStyle(
        fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold,
    ),
    titleMedium = TextStyle(
        fontSize = 18.sp, lineHeight = 26.sp, fontWeight = FontWeight.Medium,
    ),
    bodyLarge = TextStyle(
        fontSize = 18.sp, lineHeight = 28.sp,
    ),
    bodyMedium = TextStyle(
        fontSize = 16.sp, lineHeight = 24.sp,
    ),
    bodySmall = TextStyle(
        fontSize = 14.sp, lineHeight = 20.sp,
    ),
    labelLarge = TextStyle(
        fontSize = 17.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold,
    ),
)
