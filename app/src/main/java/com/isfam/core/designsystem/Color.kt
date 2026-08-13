package com.isfam.core.designsystem

import androidx.compose.ui.graphics.Color

/**
 * IsFam 컬러.
 * UI 키트 HTML 에서 실제 사용된 값을 그대로 추출했습니다.
 * 화면에서 Color(0xFF...) 를 직접 쓰지 마세요.
 */

// ── 브랜드 ────────────────────────────────────────────────────
val Tint50 = Color(0xFFFFF1DE)
val Honey300 = Color(0xFFFFC53D)
val Honey400 = Color(0xFFFFB020)   // 버튼 그라데이션 시작
val Amber500 = Color(0xFFF26A0A)
val Amber600 = Color(0xFFEF6A05)
val Amber700 = Color(0xFFC05C05)

// 스플래시 그라데이션 (170deg, #FFD873 → #F98F16 62% → #EF6A05)
val SplashTop = Color(0xFFFFD873)
val SplashMid = Color(0xFFF98F16)
val SplashBottom = Color(0xFFEF6A05)

// 온보딩 일러스트 배경 (radial #FFF1DE → #F7F2E9)
val IllustBgStart = Color(0xFFFFF1DE)
val IllustBgEnd = Color(0xFFF7F2E9)

// ── 판별 결과 3단 ─────────────────────────────────────────────
val Safe = Color(0xFF5AA97A)
val Caution = Color(0xFFE8A32B)
val Danger = Color(0xFFD9453F)

// 배지 전용 (pill) — 본문 색과 다릅니다
val SafeBadgeFg = Color(0xFF3E8C61)
val SafeBadgeBg = Color(0xFFE8F3EC)
val CautionBadgeFg = Color(0xFFC0700A)
val CautionBadgeBg = Color(0xFFFCEFD6)
val DangerBadgeFg = Color(0xFFD9453F)
val DangerBadgeBg = Color(0xFFFDECEC)

// ── 중립 ──────────────────────────────────────────────────────
val ScreenBg = Color(0xFFFAF7F2)   // 화면 기본 배경. 흰색이 아닙니다
val Ink = Color(0xFF17130F)        // 제목 · 검정 버튼
val InkBody = Color(0xFF4A423B)    // 본문
val InkBody2 = Color(0xFF6E655C)   // 온보딩 본문 (조금 더 연함)
val InkMuted = Color(0xFF8A7F72)   // 캡션
val InkMuted2 = Color(0xFF9C9084)  // "1 / 3", "건너뛰기"
val InkFaint = Color(0xFFA2968A)
val DisabledBg = Color(0xFFEFE7DA)
val DisabledFg = Color(0xFFB5A794)
val ToggleOff = Color(0xFFE2DACE)
val IndicatorOff = Color(0xFFE4D9C9)
val Divider = Color(0xFFE7E2DC)
val White = Color(0xFFFFFFFF)