package com.isfam.core.designsystem

import androidx.compose.ui.graphics.Color

/**
 * IsFam 컬러 팔레트.
 * UI 키트 "5. 디자인 시스템 · 컬러 팔레트" 값을 그대로 옮겼습니다.
 *
 * 화면에서 Color(0xFF...) 를 직접 쓰지 마세요.
 * 33개 화면에 하드코딩된 색이 퍼지면 톤 하나 바꾸는 데 하루가 걸립니다.
 */

// ── 브랜드 · 허니 → 앰버 ──────────────────────────────────────
val Tint50 = Color(0xFFFFF1DE)
val Honey300 = Color(0xFFFFC53D)
val Amber500 = Color(0xFFF26A0A)
val Amber700 = Color(0xFFC05C05)

// ── 판별 결과 3단 시맨틱 ──────────────────────────────────────
//
// 색만으로 구분하면 색각 이상 사용자가 판단할 수 없습니다.
// 반드시 텍스트 라벨과 함께 쓰세요. (RiskBadge 참고)
val Safe = Color(0xFF5AA97A)
val Caution = Color(0xFFE8A32B)
val Danger = Color(0xFFD9453F)

// 배경용 연한 톤 — 팔레트에 없어서 브랜드 톤에 맞춰 파생시킨 값입니다
val SafeBg = Color(0xFFE8F4ED)
val CautionBg = Color(0xFFFDF3DF)
val DangerBg = Color(0xFFFBE9E8)

// ── 중립 · Ink 기준 ───────────────────────────────────────────
val Ink = Color(0xFF17130F)
val Ink700 = Color(0xFF4A423B)
val Ink500 = Color(0xFF7A6F65)
val Ink300 = Color(0xFFB5ACA3)
val Ink100 = Color(0xFFE7E2DC)
val Ink50 = Color(0xFFF6F3EF)
val White = Color(0xFFFFFFFF)