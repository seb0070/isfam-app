package com.isfam.feature.home

import com.isfam.core.designsystem.RiskLevel

/**
 * 19. 홈 대시보드의 화면 상태.
 *
 * 서버가 준비되면 Repository 가 이 모델을 채웁니다.
 * 지금은 FakeHomeData 가 채웁니다.
 */
data class HomeUiState(
    val dateLabel: String,
    val greetingName: String,
    val protectionActive: Boolean,
    val familyCount: Int,
    val todayAnalysisCount: Int,
    val todayBlockedCount: Int,
    val summary: ProtectionSummary,
    val urgentAlert: UrgentAlert?,
    val blockedNumberCount: Int,
    val members: List<MemberStatus>,
    val recentAnalyses: List<RecentAnalysis>,
) {
    val registeredCount: Int get() = members.count { it.registered }
    val unregisteredMember: MemberStatus? get() = members.firstOrNull { !it.registered }
}

/** 오늘의 AI 보호 요약 3줄 */
data class ProtectionSummary(
    val matchedCount: Int,
    val cautionCount: Int,
    val blockedCount: Int,
) {
    private val total get() = (matchedCount + cautionCount + blockedCount).coerceAtLeast(1)
    val matchedRatio get() = matchedCount / total.toFloat()
    val cautionRatio get() = cautionCount / total.toFloat()
    val blockedRatio get() = blockedCount / total.toFloat()
}

/** 위험 감지 배너 */
data class UrgentAlert(
    val analysisId: Long,
    val timeLabel: String,
    val caller: String,
    val message: String,
)

data class MemberStatus(
    val memberId: Int,
    val name: String,
    val initial: String,
    val relation: String,
    val registered: Boolean,
    val isMe: Boolean = false,
)

data class RecentAnalysis(
    val analysisId: Long,
    val caller: String,
    val timeLabel: String,
    val durationLabel: String,
    val riskLevel: RiskLevel,
)

/**
 * 서버 연동 전까지 화면을 채우는 데이터.
 * Repository 가 생기면 이 객체를 지우고 교체하면 됩니다.
 */
object FakeHomeData {
    val state = HomeUiState(
        dateLabel = "2026년 7월 26일 일요일",
        greetingName = "서연",
        protectionActive = true,
        familyCount = 4,
        todayAnalysisCount = 7,
        todayBlockedCount = 1,
        summary = ProtectionSummary(matchedCount = 6, cautionCount = 1, blockedCount = 1),
        urgentAlert = UrgentAlert(
            analysisId = 1,
            timeLabel = "오후 12:03",
            caller = "010-4482-9917",
            message = "AI 합성 음성이 감지되었어요",
        ),
        blockedNumberCount = 3,
        members = listOf(
            MemberStatus(1, "김상호", "상", "아버지", registered = true),
            MemberStatus(2, "이정영", "영", "어머니", registered = true),
            MemberStatus(3, "나", "서", "본인", registered = true, isMe = true),
            MemberStatus(4, "김도현", "?", "아들", registered = false),
        ),
        recentAnalyses = listOf(
            RecentAnalysis(1, "이정영 (어머니)", "오후 2:14", "00:42", RiskLevel.SAFE),
            RecentAnalysis(2, "010-4482-9917", "오후 12:03", "01:07", RiskLevel.DANGER),
            RecentAnalysis(3, "김도현 (동생)", "오전 9:38", "00:26", RiskLevel.CAUTION),
        ),
    )
}
