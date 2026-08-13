package com.isfam.navigation

import kotlinx.serialization.Serializable

/**
 * IsFam 화면 경로.
 * UI 키트 33개 화면을 기준으로 정의했습니다.
 *
 * 33개 중 실제 라우트는 24개입니다. 나머지는 화면이 아니라 상태입니다.
 *   20, 22 → 바텀시트 (21번 위에 표시)
 *   23     → 토스트 (22번의 상태)
 *   25     → 24번의 빈 상태
 *   27     → 시스템 알림
 *   28·29·30 → 같은 화면. riskLevel 값만 다름
 *
 * Navigation Compose 2.8부터 문자열이 아니라 @Serializable 타입으로
 * 경로를 정의합니다. 오타가 컴파일 에러로 잡히고 인자 전달도 타입 안전합니다.
 *
 *   ❌ navController.navigate("result/$id")
 *   ✅ navController.navigate(Route.AnalysisResult(id))
 */
sealed interface Route {

    // ── 1. 진입 · 인증 ────────────────────────────────────────
    /** 01 스플래시 */
    @Serializable data object Splash : Route

    /** 02·03·04 온보딩 3단 — 페이저 한 화면으로 처리 */
    @Serializable data object Onboarding : Route

    /** 05 로그인 */
    @Serializable data object Login : Route

    /** 06 회원가입 (이름·번호·약관) */
    @Serializable data object SignUp : Route

    /** 07 휴대폰 OTP 인증 */
    @Serializable data class OtpVerify(val phoneNumber: String) : Route

    /** 08 권한 허용 안내 */
    @Serializable data object Permission : Route

    // ── 2. 가족 · 목소리 등록 ─────────────────────────────────
    /** 09 목소리 등록 안내 */
    @Serializable data object VoiceIntro : Route

    /** 10 목소리 녹음 — 문장 1~3 */
    @Serializable data class VoiceRecord(val sentenceIndex: Int) : Route

    /** 11 성문 생성 중 */
    @Serializable data object VoiceProcessing : Route

    /** 12 등록 완료 */
    @Serializable data object VoiceComplete : Route

    /** 13 가족 공간 진입 선택 (만들기 / 참여) */
    @Serializable data object FamilyEntry : Route

    /** 14 가족 공간 만들기 */
    @Serializable data object FamilyCreate : Route

    /** 15 가족 초대 — 온보딩 직후 */
    @Serializable data object FamilyInvite : Route

    /** 16 가족 초대 — 가족 관리에서 재진입 (초대 현황 포함) */
    @Serializable data object FamilyInviteManage : Route

    /** 17 초대 코드 직접 입력 */
    @Serializable data object InviteCodeInput : Route

    /** 18 초대 수락 · 연결 (Flow B) */
    @Serializable data class InviteAccept(val inviteCode: String) : Route

    // ── 3. 메인 (하단 탭 4개) ─────────────────────────────────
    /** 19 홈 대시보드 · 20 등록 요청 시트 포함 */
    @Serializable data object Home : Route

    /** 21 가족 관리 · 22 프로필 바텀시트 · 23 토스트 포함 */
    @Serializable data object FamilyManage : Route

    /** 32 분석 기록 */
    @Serializable data object History : Route

    /** 26 설정 */
    @Serializable data object Settings : Route

    /** 24 가족 차단 번호 관리 · 25 빈 상태 포함 */
    @Serializable data object BlockedNumbers : Route

    // ── 4. 분석 · 결과 ────────────────────────────────────────
    /** 분석 중 (통화 종료 후 10~30초) */
    @Serializable data class Analyzing(val callEventId: Long) : Route

    /** 28·29·30 결과 — 안전 / 확인 필요 / 위험이 모두 이 화면 */
    @Serializable data class AnalysisResult(val analysisId: Long) : Route

    /** 31 가족 위험 공유 */
    @Serializable data class ShareDanger(val analysisId: Long) : Route

    /** 33 기록 상세 */
    @Serializable data class HistoryDetail(val analysisId: Long) : Route
}

/** 하단 탭 4개 */
enum class BottomTab(val route: Route, val label: String) {
    HOME(Route.Home, "홈"),
    FAMILY(Route.FamilyManage, "가족"),
    HISTORY(Route.History, "기록"),
    SETTINGS(Route.Settings, "설정"),
}