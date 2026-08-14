package com.isfam.core.designsystem

import androidx.compose.ui.graphics.Color

/**
 * 분석 결과 화면(28·29·30) 전용 색상.
 *
 * 세 등급이 배경색부터 완전히 다릅니다.
 * 특히 위험은 다크 테마라 다른 화면과 색 체계를 공유할 수 없어
 * 별도 파일로 분리했습니다.
 */

// ── 안전 ──────────────────────────────────────────────────────
val SafeBgTop = Color(0xFFF7FCF9)
val SafeBgMid = Color(0xFFE9F4EE)
val SafeBgBottom = Color(0xFFE2F0E8)
val SafeRingLight = Color(0xFF8FD9AE)
val SafeRingMid = Color(0xFF4FA97A)
val SafeRingDeep = Color(0xFF2F7551)
val SafeScoreText = Color(0xFF2F7551)
val SafeScoreLabel = Color(0xFF6B8B78)
val SafeBadgeStart = Color(0xFF57B183)
val SafeMascotBg = Color(0xFFE7F2EB)
val SafeBodyText = Color(0xFF5F6E65)
val SafeRowLabel = Color(0xFF6E7B73)
val SafeRowDivider = Color(0xFFEFF5F1)
val SafeCtaStart = Color(0xFF2F3B34)

// ── 확인 필요 ─────────────────────────────────────────────────
val CautionBgTop = Color(0xFFFFFCF4)
val CautionBgMid = Color(0xFFFDF3E1)
val CautionBgBottom = Color(0xFFFAEBD5)
val CautionRingLight = Color(0xFFFFD873)
val CautionRingMid = Color(0xFFF0A93A)
val CautionRingDeep = Color(0xFFD2822A)
val CautionScoreText = Color(0xFFC0700A)
val CautionScoreLabel = Color(0xFF9A7B4A)
val CautionBadgeStart = Color(0xFFF5B94E)
val CautionMascotBg = Color(0xFFFCEFD6)
val CautionBodyText = Color(0xFF7A6A52)
val CautionRowLabel = Color(0xFF8A7856)
val CautionRowDivider = Color(0xFFF5EDDF)

// ── 위험 (다크) ───────────────────────────────────────────────
val DangerBgDark = Color(0xFF2A1112)
val DangerRingLight = Color(0xFFFF9C8F)
val DangerRingDeep = Color(0xFFFF5A50)
val DangerScoreText = Color(0xFFFF8A80)
val DangerCtaRed = Color(0xFFD9453F)
