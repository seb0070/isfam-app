package com.isfam.feature.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.isfam.core.designsystem.Amber700
import com.isfam.core.designsystem.Caution
import com.isfam.core.designsystem.CautionBadgeBg
import com.isfam.core.designsystem.CautionBadgeFg
import com.isfam.core.designsystem.Danger
import com.isfam.core.designsystem.DangerBadgeBg
import com.isfam.core.designsystem.DangerBadgeFg
import com.isfam.core.designsystem.Ink
import com.isfam.core.designsystem.InkBody2
import com.isfam.core.designsystem.InkFaint
import com.isfam.core.designsystem.InkMuted
import com.isfam.core.designsystem.IsFamTheme
import com.isfam.core.designsystem.MainTab
import com.isfam.core.designsystem.MainTabScaffold
import com.isfam.core.designsystem.Mascot
import com.isfam.core.designsystem.MascotImage
import com.isfam.core.designsystem.RiskLevel
import com.isfam.core.designsystem.RowDivider
import com.isfam.core.designsystem.Safe
import com.isfam.core.designsystem.SafeBadgeBg
import com.isfam.core.designsystem.SafeBadgeFg
import com.isfam.core.designsystem.Tint50
import com.isfam.core.designsystem.White

/**
 * 기록 탭 — 32 분석 기록 · 24·25 차단 번호
 *
 * 두 화면을 세그먼트로 묶었습니다.
 * 가족 관리에 있던 차단 번호를 여기로 옮긴 이유는 성격이 맞기 때문입니다.
 * 가족 관리는 "구성원", 기록은 "지나간 통화의 결과물"이고
 * 차단 번호는 후자입니다.
 *
 * UI 키트 실측값
 *   제목 800 24 · 필터 pill padding 9/15 · 좌우 여백 22
 *   목록 카드 radius 22 · 행 padding 15/18
 *   등급 막대 8×38 radius 5 · 점수 800 16
 */
@Composable
fun HistoryRoute(
    onTabSelected: (MainTab) -> Unit,
    onItemClick: (Long) -> Unit,
    initialSegment: HistorySegment = HistorySegment.Analysis,
) {
    var segment by remember { mutableStateOf(initialSegment) }
    var filter by remember { mutableStateOf(RiskFilter.All) }
    var month by remember { mutableStateOf(FakeHistoryData.analysis.selectedMonth) }
    var showMonthPicker by remember { mutableStateOf(false) }

    // TODO: Repository 연결 시 교체. month 가 바뀌면 서버에서 다시 불러옵니다.
    val analysisState = FakeHistoryData.analysis.copy(
        selectedFilter = filter,
        selectedMonth = month,
    )
    val blockedState = FakeHistoryData.blocked

    if (showMonthPicker) {
        MonthPickerSheet(
            options = analysisState.monthOptions,
            selected = month,
            onSelect = {
                month = it
                showMonthPicker = false
            },
            onDismiss = { showMonthPicker = false },
        )
    }

    HistoryScreen(
        segment = segment,
        analysisState = analysisState,
        blockedState = blockedState,
        onSegmentChange = { segment = it },
        onFilterChange = { filter = it },
        onTabSelected = onTabSelected,
        onItemClick = onItemClick,
        onMonthClick = { showMonthPicker = true },
        onUnblock = { /* TODO: DELETE /family/blocked-numbers/{id} */ },
    )
}

@Composable
fun HistoryScreen(
    segment: HistorySegment,
    analysisState: HistoryUiState,
    blockedState: BlockedUiState,
    onSegmentChange: (HistorySegment) -> Unit,
    onFilterChange: (RiskFilter) -> Unit,
    onTabSelected: (MainTab) -> Unit,
    onItemClick: (Long) -> Unit,
    onMonthClick: () -> Unit,
    onUnblock: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    MainTabScaffold(
        currentTab = MainTab.History,
        onTabSelected = onTabSelected,
        modifier = modifier,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 제목 + 월 선택
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 22.dp, end = 22.dp, top = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "기록",
                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = 24.sp),
                    color = Ink,
                )
                if (segment == HistorySegment.Analysis) {
                    Text(
                        "${analysisState.selectedMonthLabel} ▾",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                        color = InkMuted,
                        modifier = Modifier.clickable(onClick = onMonthClick).padding(6.dp),
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            SegmentTabs(segment, onSegmentChange)

            when (segment) {
                HistorySegment.Analysis ->
                    AnalysisSegment(analysisState, onFilterChange, onItemClick)

                HistorySegment.Blocked ->
                    BlockedSegment(blockedState, onUnblock)
            }
        }
    }
}

// ── 세그먼트 ──────────────────────────────────────────────────

@Composable
private fun SegmentTabs(
    current: HistorySegment,
    onChange: (HistorySegment) -> Unit,
) {
    Row(
        modifier = Modifier
            .padding(horizontal = 22.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Tint50)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        HistorySegment.entries.forEach { seg ->
            val selected = seg == current
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(11.dp))
                    .background(if (selected) White else Color.Transparent)
                    .clickable { onChange(seg) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    seg.label,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                    color = if (selected) Ink else Amber700,
                )
            }
        }
    }
}

// ── 32. 분석 기록 ─────────────────────────────────────────────

@Composable
private fun AnalysisSegment(
    state: HistoryUiState,
    onFilterChange: (RiskFilter) -> Unit,
    onItemClick: (Long) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 22.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RiskFilter.entries.forEach { f ->
                FilterPill(
                    label = "${f.label} ${state.counts[f] ?: 0}",
                    selected = f == state.selectedFilter,
                    filter = f,
                    onClick = { onFilterChange(f) },
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp),
        ) {
            Spacer(Modifier.height(18.dp))

            state.groups.forEach { group ->
                val visible = group.items.filter {
                    state.selectedFilter == RiskFilter.All ||
                            it.riskLevel.matches(state.selectedFilter)
                }
                if (visible.isEmpty()) return@forEach

                Text(
                    group.dateLabel,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                    color = InkMuted,
                    modifier = Modifier.padding(start = 4.dp),
                )
                Spacer(Modifier.height(10.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(5.dp, RoundedCornerShape(22.dp), clip = false)
                        .clip(RoundedCornerShape(22.dp))
                        .background(White),
                ) {
                    visible.forEachIndexed { index, item ->
                        HistoryRow(item) { onItemClick(item.analysisId) }
                        if (index < visible.lastIndex) {
                            Box(Modifier.fillMaxWidth().height(1.dp).background(RowDivider))
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            Spacer(Modifier.height(12.dp))
        }
    }
}

private fun RiskLevel.matches(filter: RiskFilter): Boolean = when (filter) {
    RiskFilter.All -> true
    RiskFilter.Safe -> this == RiskLevel.SAFE
    RiskFilter.Caution -> this == RiskLevel.CAUTION || this == RiskLevel.INSUFFICIENT
    RiskFilter.Danger -> this == RiskLevel.DANGER
}

@Composable
private fun FilterPill(
    label: String,
    selected: Boolean,
    filter: RiskFilter,
    onClick: () -> Unit,
) {
    val (fg, bg) = when {
        selected && filter == RiskFilter.All -> White to Ink
        filter == RiskFilter.All -> InkBody2 to White
        filter == RiskFilter.Safe -> SafeBadgeFg to SafeBadgeBg
        filter == RiskFilter.Caution -> CautionBadgeFg to CautionBadgeBg
        else -> DangerBadgeFg to DangerBadgeBg
    }

    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 15.dp, vertical = 9.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
            color = fg,
        )
    }
}

@Composable
private fun HistoryRow(item: HistoryItem, onClick: () -> Unit) {
    val accent = when (item.riskLevel) {
        RiskLevel.SAFE -> Safe
        RiskLevel.CAUTION -> Caution
        RiskLevel.DANGER -> Danger
        RiskLevel.INSUFFICIENT -> InkFaint
    }
    val scoreColor = when (item.riskLevel) {
        RiskLevel.SAFE -> SafeBadgeFg
        RiskLevel.CAUTION -> CautionBadgeFg
        RiskLevel.DANGER -> DangerBadgeFg
        RiskLevel.INSUFFICIENT -> InkFaint
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(width = 8.dp, height = 38.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(accent),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                item.caller,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.5.sp),
                color = Ink,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                "${item.timeLabel} · ${item.durationLabel} · ${item.methodLabel}",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = InkMuted,
            )
        }
        Text(
            "${item.score}",
            style = MaterialTheme.typography.headlineMedium.copy(fontSize = 16.sp),
            color = scoreColor,
        )
    }
}

// ── 24 · 25. 차단 번호 ────────────────────────────────────────

@Composable
private fun BlockedSegment(state: BlockedUiState, onUnblock: (Long) -> Unit) {
    if (state.isEmpty) {
        BlockedEmptyState()
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp),
    ) {
        Spacer(Modifier.height(16.dp))

        Text(
            "위험으로 판별된 통화에서 가족이 공유한 번호입니다.",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 13.sp, lineHeight = 21.sp,
            ),
            color = InkBody2,
            modifier = Modifier.padding(horizontal = 4.dp),
        )

        Spacer(Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "차단 중 ${state.items.size}건",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                color = InkMuted,
            )
            Text(
                "최근 추가순 ▾",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                color = InkFaint,
            )
        }

        Spacer(Modifier.height(12.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(5.dp, RoundedCornerShape(22.dp), clip = false)
                .clip(RoundedCornerShape(22.dp))
                .background(White),
        ) {
            state.items.forEachIndexed { index, item ->
                BlockedRow(item) { onUnblock(item.id) }
                if (index < state.items.lastIndex) {
                    Box(Modifier.fillMaxWidth().height(1.dp).background(RowDivider))
                }
            }
        }

        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun BlockedRow(item: BlockedNumber, onUnblock: () -> Unit) {
    val (fg, bg) = when (item.reasonLevel) {
        RiskLevel.DANGER -> DangerBadgeFg to DangerBadgeBg
        RiskLevel.CAUTION -> CautionBadgeFg to CautionBadgeBg
        else -> InkMuted to RowDivider
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                item.phoneNumber,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                color = Ink,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                item.addedByLabel,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = InkMuted,
            )
            Spacer(Modifier.height(7.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(bg)
                    .padding(horizontal = 9.dp, vertical = 4.dp),
            ) {
                Text(
                    item.reasonLabel,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = fg,
                )
            }
        }

        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(RowDivider)
                .clickable(onClick = onUnblock)
                .padding(horizontal = 13.dp, vertical = 9.dp),
        ) {
            Text(
                "차단 해제",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                color = InkBody2,
            )
        }
    }
}

/** 25. 빈 상태 */
@Composable
private fun BlockedEmptyState() {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        MascotImage(mascot = Mascot.Safe, size = 132.dp, cornerRadius = 38.dp)

        Spacer(Modifier.height(24.dp))

        Text(
            "아직 차단된\n가족 위험 번호가 없어요",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = 22.sp, lineHeight = 31.sp,
            ),
            color = Ink,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "위험으로 판별된 통화에서 [가족에게 알리기]를 누르면 이 목록에 추가됩니다.",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 14.sp, lineHeight = 24.sp,
            ),
            color = InkBody2,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(Tint50)
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            Text(
                "지금까지 안전한 통화만 있었어요",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                color = Amber700,
            )
        }
    }
}

// ── Preview ───────────────────────────────────────────────────

@Preview(name = "32 분석 기록", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun HistoryAnalysisPreview() = IsFamTheme {
    HistoryScreen(
        segment = HistorySegment.Analysis,
        analysisState = FakeHistoryData.analysis,
        blockedState = FakeHistoryData.blocked,
        onSegmentChange = {}, onFilterChange = {}, onTabSelected = {},
        onItemClick = {}, onMonthClick = {}, onUnblock = {},
    )
}

@Preview(name = "24 차단 번호", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun HistoryBlockedPreview() = IsFamTheme {
    HistoryScreen(
        segment = HistorySegment.Blocked,
        analysisState = FakeHistoryData.analysis,
        blockedState = FakeHistoryData.blocked,
        onSegmentChange = {}, onFilterChange = {}, onTabSelected = {},
        onItemClick = {}, onMonthClick = {}, onUnblock = {},
    )
}

@Preview(name = "25 빈 상태", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun HistoryBlockedEmptyPreview() = IsFamTheme {
    HistoryScreen(
        segment = HistorySegment.Blocked,
        analysisState = FakeHistoryData.analysis,
        blockedState = FakeHistoryData.blockedEmpty,
        onSegmentChange = {}, onFilterChange = {}, onTabSelected = {},
        onItemClick = {}, onMonthClick = {}, onUnblock = {},
    )
}