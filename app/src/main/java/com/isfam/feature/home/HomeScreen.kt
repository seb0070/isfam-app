package com.isfam.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.isfam.core.designsystem.Amber500
import com.isfam.core.designsystem.Amber700
import com.isfam.core.designsystem.Caution
import com.isfam.core.designsystem.CautionBgAlt
import com.isfam.core.designsystem.Danger
import com.isfam.core.designsystem.DangerBadgeBg
import com.isfam.core.designsystem.DashedAvatarBg
import com.isfam.core.designsystem.DashedAvatarBorder
import com.isfam.core.designsystem.Honey300
import com.isfam.core.designsystem.Ink
import com.isfam.core.designsystem.InkBody
import com.isfam.core.designsystem.InkFaint
import com.isfam.core.designsystem.InkMuted
import com.isfam.core.designsystem.InkPlaceholder
import com.isfam.core.designsystem.IsFamTheme
import com.isfam.core.designsystem.LabelBrown
import com.isfam.core.designsystem.MainTab
import com.isfam.core.designsystem.MainTabScaffold
import com.isfam.core.designsystem.Mascot
import com.isfam.core.designsystem.MascotImage
import com.isfam.core.designsystem.RiskBadge
import com.isfam.core.designsystem.RiskLevel
import com.isfam.core.designsystem.RowDivider
import com.isfam.core.designsystem.Safe
import com.isfam.core.designsystem.SafeBadgeBg
import com.isfam.core.designsystem.ScreenBg
import com.isfam.core.designsystem.Tint50
import com.isfam.core.designsystem.TrackWarm
import com.isfam.core.designsystem.White

/**
 * 19. 홈 대시보드
 *
 * UI 키트 실측값
 *   좌우 여백 22
 *   보호 상태 카드 radius 24 · gradient(150deg #FFC53D→#F26A0A) · padding 18
 *   흰 카드 radius 22 · padding 17/18
 *   위험 배너 #FDECEC · radius 20
 *   퀵 액션 3개 radius 18 · 아이콘 34 radius 12
 *   최근 분석 목록 radius 20 · 행 padding 13/16
 */
@Composable
fun HomeRoute(
    onTabSelected: (MainTab) -> Unit,
    onAnalysisClick: (Long) -> Unit,
    onInvite: () -> Unit,
    onBlockedNumbers: () -> Unit,
    onRequestRegistration: (MemberStatus) -> Unit,
    onSeeAllHistory: () -> Unit,
) {
    // TODO: Repository 연결 시 교체
    val state = FakeHomeData.state

    HomeScreen(
        state = state,
        onTabSelected = onTabSelected,
        onAnalysisClick = onAnalysisClick,
        onInvite = onInvite,
        onBlockedNumbers = onBlockedNumbers,
        onRequestRegistration = onRequestRegistration,
        onSeeAllHistory = onSeeAllHistory,
    )
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    onTabSelected: (MainTab) -> Unit,
    onAnalysisClick: (Long) -> Unit,
    onInvite: () -> Unit,
    onBlockedNumbers: () -> Unit,
    onRequestRegistration: (MemberStatus) -> Unit,
    onSeeAllHistory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MainTabScaffold(
        currentTab = MainTab.Home,
        onTabSelected = onTabSelected,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp),
        ) {
            Spacer(Modifier.height(16.dp))
            GreetingRow(state)

            Spacer(Modifier.height(14.dp))
            ProtectionCard(state)

            Spacer(Modifier.height(14.dp))
            SummaryCard(state.summary, onSeeAllHistory)

            state.urgentAlert?.let {
                Spacer(Modifier.height(14.dp))
                UrgentBanner(it) { onAnalysisClick(it.analysisId) }
            }

            Spacer(Modifier.height(14.dp))
            QuickActions(
                blockedCount = state.blockedNumberCount,
                onInvite = onInvite,
                onBlockedNumbers = onBlockedNumbers,
            )

            Spacer(Modifier.height(14.dp))
            MemberStatusCard(state, onRequestRegistration)

            Spacer(Modifier.height(14.dp))
            RecentAnalysisSection(state.recentAnalyses, onAnalysisClick, onSeeAllHistory)

            Spacer(Modifier.height(20.dp))
        }
    }
}

// ── 인사 ──────────────────────────────────────────────────────

@Composable
private fun GreetingRow(state: HomeUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                state.dateLabel,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                color = InkMuted,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "${state.greetingName}님, 오늘도 가족을 지키고 있어요",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 19.5.sp, lineHeight = 26.sp,
                ),
                color = Ink,
            )
        }
        Spacer(Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .size(40.dp)
                .shadow(4.dp, RoundedCornerShape(14.dp), clip = false)
                .clip(RoundedCornerShape(14.dp))
                .background(White),
            contentAlignment = Alignment.Center,
        ) {
            Text("🔔", style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp))
        }
    }
}

// ── 보호 상태 ─────────────────────────────────────────────────

@Composable
private fun ProtectionCard(state: HomeUiState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(14.dp, RoundedCornerShape(24.dp), clip = false)
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(Honey300, Amber500)))
            .padding(18.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(62.dp)
                    .clip(CircleShape)
                    .background(White.copy(alpha = 0.26f))
                    .border(1.dp, White.copy(alpha = 0.45f), CircleShape)
                    .padding(4.dp),
                contentAlignment = Alignment.Center,
            ) {
                MascotImage(
                    mascot = Mascot.Watching,
                    size = 52.dp,
                    cornerRadius = 26.dp,
                    background = Color.Transparent,
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(White.copy(alpha = 0.24f))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                ) {
                    Text(
                        if (state.protectionActive) "보호 작동 중" else "보호 중지됨",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = White,
                    )
                }
                Spacer(Modifier.height(7.dp))
                Text(
                    "가족 ${state.familyCount}명이 서로를 지키고 있어요",
                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = 18.sp),
                    color = White,
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    "통화 종료 후 자동 분석 · 권한 정상",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = White.copy(alpha = 0.9f),
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(White.copy(alpha = 0.18f))
                .padding(vertical = 12.dp),
        ) {
            StatCell("${state.todayAnalysisCount}건", "오늘 분석", Modifier.weight(1f))
            Box(Modifier.width(1.dp).height(36.dp).background(White.copy(alpha = 0.28f)))
            StatCell("${state.todayBlockedCount}건", "차단 · 경고", Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatCell(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            value,
            style = MaterialTheme.typography.headlineMedium.copy(fontSize = 19.sp),
            color = White,
        )
        Spacer(Modifier.height(3.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
            color = White.copy(alpha = 0.85f),
        )
    }
}

// ── 오늘의 보호 요약 ──────────────────────────────────────────

@Composable
private fun SummaryCard(summary: ProtectionSummary, onSeeAll: () -> Unit) {
    WhiteCard {
        SectionHeader("오늘의 AI 보호 요약", "자세히 ›", onSeeAll)
        Spacer(Modifier.height(14.dp))
        Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
            SummaryBar("가족 목소리 일치 통화", summary.matchedCount, summary.matchedRatio, Safe)
            SummaryBar("추가 확인 권장", summary.cautionCount, summary.cautionRatio, Caution)
            SummaryBar("AI 합성 의심 차단", summary.blockedCount, summary.blockedRatio, Danger)
        }
    }
}

@Composable
private fun SummaryBar(label: String, count: Int, ratio: Float, color: Color) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                color = InkBody,
            )
            Text(
                "${count}건",
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 13.sp),
                color = Ink,
            )
        }
        Spacer(Modifier.height(7.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(TrackWarm),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(ratio.coerceIn(0.04f, 1f))
                    .fillMaxSize()
                    .background(color),
            )
        }
    }
}

// ── 위험 배너 ─────────────────────────────────────────────────

@Composable
private fun UrgentBanner(alert: UrgentAlert, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(20.dp), clip = false)
            .clip(RoundedCornerShape(20.dp))
            .background(DangerBadgeBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(Color(0xFFF5C9C6)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "!",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                color = Color(0xFFB33A35),
            )
        }
        Text(
            "${alert.timeLabel} · ${alert.caller} 통화에서\n${alert.message}",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 13.sp, lineHeight = 20.sp,
            ),
            color = Color(0xFFA83C36),
            modifier = Modifier.weight(1f),
        )
        Text("›", style = MaterialTheme.typography.titleMedium, color = Color(0xFFC4736E))
    }
}

// ── 퀵 액션 ───────────────────────────────────────────────────

@Composable
private fun QuickActions(
    blockedCount: Int,
    onInvite: () -> Unit,
    onBlockedNumbers: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        QuickAction(
            title = "가족 초대", subtitle = "카톡 · QR",
            tint = listOf(Color(0xFFE4F3E9), Color(0xFFC3E3CE)),
            onClick = onInvite, modifier = Modifier.weight(1f),
        )
        QuickAction(
            title = "차단 번호", subtitle = "${blockedCount}건",
            tint = listOf(Color(0xFFFBDEDC), Color(0xFFF2B7B3)),
            onClick = onBlockedNumbers, modifier = Modifier.weight(1f),
        )
        QuickAction(
            title = "우리 집 암호", subtitle = "확인 질문",
            tint = listOf(Color(0xFFE2E7F7), Color(0xFFBCC8EC)),
            onClick = { /* TODO: v2 */ }, modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun QuickAction(
    title: String,
    subtitle: String,
    tint: List<Color>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .shadow(5.dp, RoundedCornerShape(18.dp), clip = false)
            .clip(RoundedCornerShape(18.dp))
            .background(White)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Brush.linearGradient(tint)),
        )
        Spacer(Modifier.height(9.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 13.sp),
            color = Ink,
        )
        Spacer(Modifier.height(3.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
            color = InkMuted,
            textAlign = TextAlign.Center,
        )
    }
}

// ── 가족 등록 현황 ────────────────────────────────────────────

@Composable
private fun MemberStatusCard(
    state: HomeUiState,
    onRequestRegistration: (MemberStatus) -> Unit,
) {
    WhiteCard(paddingVertical = 16.dp) {
        SectionHeader(
            "가족 목소리 등록",
            "${state.registeredCount}/${state.members.size}명 ›",
        ) { }
        Spacer(Modifier.height(13.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(13.dp)) {
            state.members.forEach { member ->
                MemberAvatar(member) { onRequestRegistration(member) }
            }
        }
    }
}

@Composable
private fun MemberAvatar(member: MemberStatus, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(enabled = !member.registered, onClick = onClick),
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .then(
                        if (member.registered)
                            Modifier.background(if (member.isMe) Safe else Amber500)
                        else
                            Modifier.background(DashedAvatarBg)
                                .border(1.dp, DashedAvatarBorder, RoundedCornerShape(14.dp))
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (member.registered) member.initial else "미등록",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = if (member.registered) 15.sp else 11.sp,
                    ),
                    color = if (member.registered) White else InkPlaceholder,
                )
            }
            if (!member.registered) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Amber500),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "!",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                        color = White,
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            member.name,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
            color = if (member.registered) InkBody else Amber700,
        )
    }
}

// ── 최근 분석 ─────────────────────────────────────────────────

@Composable
private fun RecentAnalysisSection(
    items: List<RecentAnalysis>,
    onItemClick: (Long) -> Unit,
    onSeeAll: () -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 0.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "최근 분석",
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                color = Ink,
            )
            Text(
                "기록 전체 ›",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = LabelBrown,
                modifier = Modifier.clickable(onClick = onSeeAll),
            )
        }
        Spacer(Modifier.height(12.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(5.dp, RoundedCornerShape(20.dp), clip = false)
                .clip(RoundedCornerShape(20.dp))
                .background(White),
        ) {
            items.forEachIndexed { index, item ->
                AnalysisRow(item) { onItemClick(item.analysisId) }
                if (index < items.lastIndex) {
                    Box(Modifier.fillMaxWidth().height(1.dp).background(RowDivider))
                }
            }
        }
    }
}

@Composable
private fun AnalysisRow(item: RecentAnalysis, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    when (item.riskLevel) {
                        RiskLevel.SAFE -> SafeBadgeBg
                        RiskLevel.CAUTION -> CautionBgAlt
                        RiskLevel.DANGER -> DangerBadgeBg
                        RiskLevel.INSUFFICIENT -> ScreenBg
                    }
                ),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                item.caller,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 13.5.sp),
                color = Ink,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "${item.timeLabel} · 통화 후 분석 · ${item.durationLabel}",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                color = InkMuted,
            )
        }
        RiskBadge(item.riskLevel)
    }
}

// ── 공통 ──────────────────────────────────────────────────────

@Composable
private fun WhiteCard(
    paddingVertical: androidx.compose.ui.unit.Dp = 17.dp,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(5.dp, RoundedCornerShape(22.dp), clip = false)
            .clip(RoundedCornerShape(22.dp))
            .background(White)
            .padding(horizontal = 18.dp, vertical = paddingVertical),
        content = content,
    )
}

@Composable
private fun SectionHeader(title: String, action: String, onAction: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
            color = Ink,
        )
        Text(
            action,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
            color = LabelBrown,
            modifier = Modifier.clickable(onClick = onAction),
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 1170)
@Composable
private fun HomePreview() = IsFamTheme {
    HomeScreen(
        state = FakeHomeData.state,
        onTabSelected = {}, onAnalysisClick = {}, onInvite = {},
        onBlockedNumbers = {}, onRequestRegistration = {}, onSeeAllHistory = {},
    )
}
