package com.isfam.core.designsystem

import androidx.compose.ui.graphics.Color

/**
 * 색상 토큰.
 *
 * 화면에서 Color(0xFF...) 를 직접 쓰지 마세요.
 * 33개 화면에 하드코딩된 색이 퍼지면 나중에 톤 하나 바꾸는 데 하루가 걸립니다.
 *
 * 고령 사용자가 주 사용자이므로 명도 대비를 넉넉하게 잡았습니다.
 * (WCAG AA 기준 4.5:1 이상)
 */

// ── 브랜드 ────────────────────────────────────────────────────
val IsFamBlue = Color(0xFF2563EB)
val IsFamBlueDark = Color(0xFF1D4ED8)
val IsFamBlueLight = Color(0xFFDBEAFE)

// ── 위험도 — 서비스의 핵심 시각 언어 ──────────────────────────
//
// 색만으로 구분하면 색각 이상 사용자가 판단할 수 없습니다.
// 반드시 아이콘·텍스트와 함께 사용하세요.

val RiskSafe = Color(0xFF15803D)
val RiskSafeBg = Color(0xFFDCFCE7)

val RiskCaution = Color(0xFFB45309)
val RiskCautionBg = Color(0xFFFEF3C7)

val RiskDanger = Color(0xFFB91C1C)
val RiskDangerBg = Color(0xFFFEE2E2)

val RiskUnknown = Color(0xFF52525B)
val RiskUnknownBg = Color(0xFFF4F4F5)

// ── 중립 ─────────────────────────────────────────────────────
val Neutral0 = Color(0xFFFFFFFF)
val Neutral50 = Color(0xFFFAFAFA)
val Neutral100 = Color(0xFFF4F4F5)
val Neutral200 = Color(0xFFE4E4E7)
val Neutral400 = Color(0xFFA1A1AA)
val Neutral600 = Color(0xFF52525B)
val Neutral800 = Color(0xFF27272A)
val Neutral900 = Color(0xFF18181B)
