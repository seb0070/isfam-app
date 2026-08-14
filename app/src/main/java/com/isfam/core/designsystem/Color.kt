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

// 카드형 일러스트 배경 (160deg 그라데이션)
val CardIllustStart = Color(0xFFFFF6E4)
val CardIllustEnd = Color(0xFFFFEBD2)     // 09 등록 안내
val SentenceCardEnd = Color(0xFFFFEEDB)   // 10 문장 카드
val ProcessingEnd = Color(0xFFFFE3BE)     // 11 성문 생성

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
val EyebrowBrown = Color(0xFFC08A3A)    // "아래 문장을 읽어주세요"
val WaveInactive = Color(0xFFEBE2D4)    // 파형 미재생 구간
val TrackBeige = Color(0xFFEEE6DA)      // 진행바 배경
val InkPlaceholder = Color(0xFFC9BFB3)  // 입력 전 값 · "보기" 링크
val ProgressTrack = Color(0xFFEAE1D5)   // 단계 진행바 배경
val DividerLight = Color(0xFFF2ECE3)    // 카드 내부 구분선
val CheckboxOffBg = Color(0xFFF5F0E8)   // 선택 안 된 체크박스
val OtpEmptyBg = Color(0xFFF6F1E9)      // 빈 OTP 칸
val KeypadBg = Color(0xFFEFEAE2)        // 시스템 키패드 배경
val CodeEmptyBg = Color(0xFFF3EDE4)     // 빈 코드 칸
val LabelBrown = Color(0xFF9A8D7D)      // "초대 코드", "링크로 초대" 라벨
val DashedBorder = Color(0xFFE0D3C0)    // 미연결 아바타 점선
val IconTintStart = Color(0xFFFFEFD2)   // 작은 아이콘 그라데이션
val IconTintEnd = Color(0xFFFFD79A)
val KakaoYellow = Color(0xFFFFE300)     // 카카오톡 버튼
val TrackWarm = Color(0xFFF1EAE0)       // 요약 카드 진행바 배경
val RowDivider = Color(0xFFF4EEE6)      // 목록 행 구분선
val TabInactiveStart = Color(0xFFE8E0D3) // 하단 탭 비활성 아이콘
val TabInactiveEnd = Color(0xFFD3C8B8)
val DashedAvatarBg = Color(0xFFF5F0E8)
val DashedAvatarBorder = Color(0xFFDDD0BC)
val CautionBgAlt = Color(0xFFFFF4E0)    // 확인 필요 배지 배경 (목록용)
// 기록 상세 (33) 전용
val SegmentDangerLight = Color(0xFFE88A85)  // 위험 구간 (약)
val SegmentInactive = Color(0xFFE7DED2)     // 미분석 구간
val MatchBarStart = Color(0xFFF3C0A0)       // 일치도 막대
val MatchBarEnd = Color(0xFFE08A5B)
val SpoofBarStart = Color(0xFFF0A9A6)       // 합성 확률 막대
val SpoofBarEnd = Color(0xFFC93B35)
val TrustBarStart = Color(0xFF9FBBE6)       // 신뢰도 막대
val TrustBarEnd = Color(0xFF5D82C9)
val OutlineWarm = Color(0xFFE4DACB)         // 흰 버튼 테두리
val Divider = Color(0xFFE7E2DC)
val White = Color(0xFFFFFFFF)