package com.isfam.feature.family

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.isfam.core.designsystem.Amber500
import com.isfam.core.designsystem.Amber700
import com.isfam.core.designsystem.DashedAvatarBg
import com.isfam.core.designsystem.DashedAvatarBorder
import com.isfam.core.designsystem.Honey300
import com.isfam.core.designsystem.Ink
import com.isfam.core.designsystem.InkFaint
import com.isfam.core.designsystem.InkMuted
import com.isfam.core.designsystem.InkPlaceholder
import com.isfam.core.designsystem.IsFamTheme
import com.isfam.core.designsystem.MainTab
import com.isfam.core.designsystem.MainTabScaffold
import com.isfam.core.designsystem.RegistrationBadge
import com.isfam.core.designsystem.RoleTag
import com.isfam.core.designsystem.RowDivider
import com.isfam.core.designsystem.Safe
import com.isfam.core.designsystem.TrackWarm
import com.isfam.core.designsystem.White

/**
 * 21. 가족 관리
 *
 * UI 키트 실측값
 *   제목 800 24 · 초대 버튼 pill #F26A0A padding 10/15
 *   등록 현황 카드 radius 22 · padding 18 · 진행바 height 8 radius 5
 *   구성원 목록 radius 22 · 행 padding 16/18 · 아바타 44 radius 15
 *   좌우 여백 22
 */
data class FamilyMemberItem(
    val memberId: Int,
    val name: String,
    val initial: String,
    val relation: String,
    val registered: Boolean,
    val lastCheckLabel: String,
    val isAdmin: Boolean = false,
    val isMe: Boolean = false,
    val phoneNumber: String = "",
    val autoAnalysis: Boolean = true,
)

data class FamilyManageUiState(
    val members: List<FamilyMemberItem>,
    val blockedNumberCount: Int,
) {
    val registeredCount get() = members.count { it.registered }
    val total get() = members.size
    val progress get() = if (total == 0) 0f else registeredCount / total.toFloat()
    val pendingName get() = members.firstOrNull { !it.registered }?.name
}

object FakeFamilyData {
    val state = FamilyManageUiState(
        members = listOf(
            FamilyMemberItem(1, "김상호", "상", "아버지", true, "최근 검사 오늘 09:30",
                isAdmin = true, phoneNumber = "010 2214 7788"),
            FamilyMemberItem(2, "이정영", "영", "어머니", true, "최근 검사 오늘 08:05",
                phoneNumber = "010 3345 1122"),
            FamilyMemberItem(3, "김서연", "서", "본인", true, "최근 검사 어제 18:22",
                isMe = true, phoneNumber = "010 1234 5678"),
            FamilyMemberItem(4, "김도현", "?", "아들", false, "아들 · 목소리 미등록",
                phoneNumber = "010 8899 4433"),
        ),
        blockedNumberCount = 3,
    )
}

@Composable
fun FamilyManageRoute(
    onTabSelected: (MainTab) -> Unit,
    onInvite: () -> Unit,
    onBlockedNumbers: () -> Unit,
    onMemberClick: (FamilyMemberItem) -> Unit,
    onRequestRegistration: (FamilyMemberItem) -> Unit,
) {
    // TODO: Repository 연결 시 교체
    val state = FakeFamilyData.state

    FamilyManageScreen(
        state = state,
        onTabSelected = onTabSelected,
        onInvite = onInvite,
        onBlockedNumbers = onBlockedNumbers,
        onMemberClick = onMemberClick,
        onRequestRegistration = onRequestRegistration,
    )
}

@Composable
fun FamilyManageScreen(
    state: FamilyManageUiState,
    onTabSelected: (MainTab) -> Unit,
    onInvite: () -> Unit,
    onBlockedNumbers: () -> Unit,
    onMemberClick: (FamilyMemberItem) -> Unit,
    onRequestRegistration: (FamilyMemberItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    MainTabScaffold(
        currentTab = MainTab.Family,
        onTabSelected = onTabSelected,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp),
        ) {
            Spacer(Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "가족 관리",
                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = 24.sp),
                    color = Ink,
                )
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Amber500)
                        .clickable(onClick = onInvite)
                        .padding(horizontal = 15.dp, vertical = 10.dp),
                ) {
                    Text(
                        "+ 가족 초대하기",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                        color = White,
                    )
                }
            }

            Spacer(Modifier.height(18.dp))
            ProgressCard(state)

            Spacer(Modifier.height(18.dp))
            Text(
                "구성원 ${state.total}",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                color = InkMuted,
                modifier = Modifier.padding(start = 4.dp),
            )

            Spacer(Modifier.height(8.dp))
            MemberList(state.members, onMemberClick, onRequestRegistration)

            Spacer(Modifier.height(12.dp))
            BlockedNumberEntry(state.blockedNumberCount, onBlockedNumbers)

            Spacer(Modifier.height(20.dp))
        }
    }
}

// ── 등록 현황 ─────────────────────────────────────────────────

@Composable
private fun ProgressCard(state: FamilyManageUiState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(5.dp, RoundedCornerShape(22.dp), clip = false)
            .clip(RoundedCornerShape(22.dp))
            .background(White)
            .padding(18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "가족 목소리 등록 현황",
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                color = Ink,
            )
            Text(
                "${state.registeredCount}/${state.total}명 완료",
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 15.sp),
                color = Amber500,
            )
        }

        Spacer(Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(TrackWarm),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(state.progress)
                    .fillMaxSize()
                    .background(Brush.horizontalGradient(listOf(Honey300, Amber500))),
            )
        }

        Spacer(Modifier.height(10.dp))

        Text(
            state.pendingName?.let { "${it}님만 목소리 등록이 남았어요" }
                ?: "가족 모두 등록을 마쳤어요",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
            color = InkMuted,
        )
    }
}

// ── 구성원 목록 ───────────────────────────────────────────────

@Composable
private fun MemberList(
    members: List<FamilyMemberItem>,
    onMemberClick: (FamilyMemberItem) -> Unit,
    onRequestRegistration: (FamilyMemberItem) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(5.dp, RoundedCornerShape(22.dp), clip = false)
            .clip(RoundedCornerShape(22.dp))
            .background(White),
    ) {
        members.forEachIndexed { index, member ->
            MemberRow(member, onMemberClick, onRequestRegistration)
            if (index < members.lastIndex) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(RowDivider))
            }
        }
    }
}

@Composable
private fun MemberRow(
    member: FamilyMemberItem,
    onMemberClick: (FamilyMemberItem) -> Unit,
    onRequestRegistration: (FamilyMemberItem) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(15.dp))
                .then(
                    if (member.registered)
                        Modifier.background(if (member.isMe) Safe else Amber500)
                    else
                        Modifier.background(DashedAvatarBg)
                            .border(1.dp, DashedAvatarBorder, RoundedCornerShape(15.dp))
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                member.initial,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = if (member.registered) 15.sp else 13.sp,
                ),
                color = if (member.registered) White else InkPlaceholder,
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    member.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                    color = Ink,
                )
                if (member.isAdmin) {
                    Spacer(Modifier.size(4.dp))
                    RoleTag("관리자", highlighted = true)
                } else if (member.isMe) {
                    Spacer(Modifier.size(4.dp))
                    RoleTag("나")
                }
            }
            Spacer(Modifier.height(3.dp))
            Text(
                member.lastCheckLabel,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                color = if (member.registered) InkMuted else Amber700,
            )
        }

        if (member.registered) {
            Text(
                "등록완료",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = Safe,
            )
        } else {
            Box(modifier = Modifier.clickable { onRequestRegistration(member) }) {
                RegistrationBadge(registered = false)
            }
        }

        Text(
            "⋮",
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
            color = InkPlaceholder,
            modifier = Modifier.clickable { onMemberClick(member) }.padding(start = 4.dp),
        )
    }
}

// ── 차단 번호 진입 ────────────────────────────────────────────

@Composable
private fun BlockedNumberEntry(count: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(5.dp, RoundedCornerShape(22.dp), clip = false)
            .clip(RoundedCornerShape(22.dp))
            .background(White)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                "가족 차단 번호 목록",
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.5.sp),
                color = Ink,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                "가족 전체에 공유된 위험 번호",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                color = InkMuted,
            )
        }
        Text(
            "${count}건 ›",
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
            color = InkFaint,
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(
    showBackground = true, widthDp = 390, heightDp = 844,
)
@Composable
private fun FamilyManagePreview() = IsFamTheme {
    FamilyManageScreen(
        state = FakeFamilyData.state,
        onTabSelected = {}, onInvite = {}, onBlockedNumbers = {},
        onMemberClick = {}, onRequestRegistration = {},
    )
}
