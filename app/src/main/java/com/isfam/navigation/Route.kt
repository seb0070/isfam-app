package com.isfam.navigation

import kotlinx.serialization.Serializable

/**
 * 모든 화면 경로를 여기 한 곳에 선언합니다.
 *
 * Navigation Compose 2.8부터 문자열 조립 방식이 아니라
 * @Serializable 타입으로 경로를 정의합니다.
 * 오타가 컴파일 에러로 잡히고, 인자 전달도 타입 안전합니다.
 *
 *   ❌ 예전:  navController.navigate("history/$id")
 *   ✅ 지금:  navController.navigate(Route.AnalysisDetail(id))
 *
 * Day 1 목표: 33개 화면을 전부 여기 선언하고 빈 화면으로 연결해서
 *             처음부터 끝까지 클릭으로 돌아다닐 수 있게 만들기.
 */
sealed interface Route {

    // ── 온보딩 ────────────────────────────────────────────────
    @Serializable data object Splash : Route
    @Serializable data object Intro : Route
    @Serializable data object RoleSelect : Route          // 자녀 / 부모

    // ── 인증 ─────────────────────────────────────────────────
    @Serializable data object PhoneInput : Route
    @Serializable data class PhoneVerify(val phoneNumber: String) : Route
    @Serializable data object SignUp : Route
    @Serializable data object Login : Route
    @Serializable data object Terms : Route

    // ── 권한 · 기기 설정 ──────────────────────────────────────
    @Serializable data object PermissionIntro : Route
    @Serializable data object PermissionRequest : Route
    @Serializable data object BatteryOptimization : Route
    @Serializable data object AutoRecordingGuide : Route  // 삼성 설정 딥링크 안내
    @Serializable data object SetupComplete : Route

    // ── 목소리 등록 ───────────────────────────────────────────
    @Serializable data object VoiceIntro : Route
    @Serializable data class VoiceRecord(val sentenceId: Int) : Route
    @Serializable data class VoiceConfirm(val sentenceId: Int) : Route
    @Serializable data object VoiceComplete : Route

    // ── 가족 ─────────────────────────────────────────────────
    @Serializable data object FamilyCreate : Route
    @Serializable data object FamilyInvite : Route        // 초대 링크 / QR
    @Serializable data class InvitePreview(val inviteCode: String) : Route
    @Serializable data object FamilyList : Route
    @Serializable data class FamilyMemberDetail(val memberId: Int) : Route

    // ── 홈 ───────────────────────────────────────────────────
    @Serializable data object Home : Route
    @Serializable data object ProtectionStatus : Route

    // ── 분석 ─────────────────────────────────────────────────
    @Serializable data class Analyzing(val callEventId: Int) : Route
    @Serializable data class AnalysisResult(val analysisId: Int) : Route
    @Serializable data object History : Route
    @Serializable data class AnalysisDetail(val analysisId: Int) : Route

    // ── 위험 대응 ─────────────────────────────────────────────
    @Serializable data class DangerAlert(val analysisId: Int) : Route
    @Serializable data class ShareWithFamily(val analysisId: Int) : Route
    @Serializable data object SharedNumbers : Route

    // ── 데모 ─────────────────────────────────────────────────
    @Serializable data object DemoIntro : Route
    @Serializable data object DemoPlay : Route
    @Serializable data object DemoResult : Route

    // ── 설정 ─────────────────────────────────────────────────
    @Serializable data object Settings : Route
    @Serializable data object NotificationSettings : Route
    @Serializable data object AccountSettings : Route
}
