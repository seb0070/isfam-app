package com.isfam.feature.history

import com.isfam.core.designsystem.RiskLevel

/**
 * 32 분석 기록 · 24 차단 번호
 *
 * 두 화면을 기록 탭 안에 세그먼트로 묶었습니다.
 * 성격이 같기 때문입니다 — 둘 다 "지나간 통화의 결과물"이고,
 * 분석에서 위험이 나오면 그중 일부가 차단 목록으로 넘어갑니다.
 */
enum class HistorySegment(val label: String) {
    Analysis("분석 기록"),
    Blocked("차단 번호"),
}

enum class RiskFilter(val label: String) {
    All("전체"), Safe("안전"), Caution("주의"), Danger("위험"),
}

// ── 32. 분석 기록 ─────────────────────────────────────────────

data class HistoryUiState(
    val monthLabel: String,
    val counts: Map<RiskFilter, Int>,
    val selectedFilter: RiskFilter,
    val groups: List<HistoryGroup>,
) {
    val isEmpty: Boolean get() = groups.all { it.items.isEmpty() }
}

/** 날짜별 묶음 — "오늘", "어제", "7월 24일" */
data class HistoryGroup(
    val dateLabel: String,
    val items: List<HistoryItem>,
)

data class HistoryItem(
    val analysisId: Long,
    val caller: String,
    val timeLabel: String,
    val durationLabel: String,
    /** "자동 분석 · 차단됨" 같은 부가 설명 */
    val methodLabel: String,
    val score: Int,
    val riskLevel: RiskLevel,
)

// ── 24 · 25. 차단 번호 ────────────────────────────────────────

data class BlockedUiState(
    val items: List<BlockedNumber>,
) {
    val isEmpty: Boolean get() = items.isEmpty()
}

data class BlockedNumber(
    val id: Long,
    val phoneNumber: String,
    val addedByLabel: String,
    val reasonLabel: String,
    val reasonLevel: RiskLevel,
)

// ── 목 데이터 ─────────────────────────────────────────────────

object FakeHistoryData {
    val analysis = HistoryUiState(
        monthLabel = "7월",
        counts = mapOf(
            RiskFilter.All to 32,
            RiskFilter.Safe to 26,
            RiskFilter.Caution to 4,
            RiskFilter.Danger to 2,
        ),
        selectedFilter = RiskFilter.All,
        groups = listOf(
            HistoryGroup(
                "오늘",
                listOf(
                    HistoryItem(3, "010 5521 8842", "14:12", "42초",
                        "자동 분석 · 차단됨", 18, RiskLevel.DANGER),
                    HistoryItem(1, "아버지", "09:30", "2분 08초",
                        "통화 후 분석", 96, RiskLevel.SAFE),
                    HistoryItem(4, "어머니", "08:05", "51초",
                        "통화 후 분석", 93, RiskLevel.SAFE),
                ),
            ),
            HistoryGroup(
                "어제",
                listOf(
                    HistoryItem(2, "010 3388 1204", "20:41", "1분 12초",
                        "자동 분석", 62, RiskLevel.CAUTION),
                    HistoryItem(7, "아버지", "18:22", "3분 40초",
                        "통화 후 분석", 97, RiskLevel.SAFE),
                ),
            ),
        ),
    )

    val blocked = BlockedUiState(
        items = listOf(
            BlockedNumber(1, "010-5521-8842", "서연님이 추가함 · 7월 28일",
                "AI 합성 음성 의심", RiskLevel.DANGER),
            BlockedNumber(2, "010-3388-1204", "김상호님이 추가함 · 7월 27일",
                "판별 확인 필요", RiskLevel.CAUTION),
            BlockedNumber(3, "02-6712-0043", "이정영님이 추가함 · 7월 21일",
                "기관 사칭 의심", RiskLevel.DANGER),
        ),
    )

    val blockedEmpty = BlockedUiState(items = emptyList())
}
