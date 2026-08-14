package com.isfam.feature.settings

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
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.isfam.core.designsystem.Amber500
import com.isfam.core.designsystem.Danger
import com.isfam.core.designsystem.Ink
import com.isfam.core.designsystem.InkFaint
import com.isfam.core.designsystem.InkMuted
import com.isfam.core.designsystem.IsFamTheme
import com.isfam.core.designsystem.IsFamToggle
import com.isfam.core.designsystem.MainTab
import com.isfam.core.designsystem.MainTabScaffold
import com.isfam.core.designsystem.RowDivider
import com.isfam.core.designsystem.Safe
import com.isfam.core.designsystem.SafeBadgeBg
import com.isfam.core.designsystem.SafeBadgeFg
import com.isfam.core.designsystem.White

/**
 * 26. 설정
 *
 * 권한 상태는 사용자가 OS 설정에서 언제든 바꿀 수 있으므로
 * 화면에 진입할 때마다 실제 상태를 다시 읽어야 합니다.
 */
data class SettingsUiState(
    val userName: String,
    val phoneNumber: String,
    val voiceRegistered: Boolean,
    val dangerAlert: Boolean,
    val familyShareAlert: Boolean,
    val quietHours: Boolean,
    val permissions: List<PermissionStatusItem>,
)

data class PermissionStatusItem(
    val label: String,
    val granted: Boolean,
    /** 앱이 확인할 수 없는 항목 (삼성 자동 통화녹음) */
    val unverifiable: Boolean = false,
)

object FakeSettingsData {
    val state = SettingsUiState(
        userName = "김서연",
        phoneNumber = "010 1234 5678",
        voiceRegistered = true,
        dangerAlert = true,
        familyShareAlert = true,
        quietHours = false,
        permissions = listOf(
            PermissionStatusItem("통화 녹음 파일 접근", true),
            PermissionStatusItem("통화 상태 확인", true),
            PermissionStatusItem("알림", true),
            PermissionStatusItem("배터리 최적화 예외", false),
            PermissionStatusItem("자동 통화녹음", true, unverifiable = true),
        ),
    )
}

@Composable
fun SettingsRoute(
    onTabSelected: (MainTab) -> Unit,
    onReRegisterVoice: () -> Unit,
    onOpenSystemSettings: () -> Unit,
    onLogout: () -> Unit,
    onWithdraw: () -> Unit,
) {
    // TODO: Repository + PermissionChecker 연결 시 교체
    var state by remember { mutableStateOf(FakeSettingsData.state) }

    SettingsScreen(
        state = state,
        onTabSelected = onTabSelected,
        onDangerAlertChange = { state = state.copy(dangerAlert = it) },
        onFamilyShareChange = { state = state.copy(familyShareAlert = it) },
        onQuietHoursChange = { state = state.copy(quietHours = it) },
        onReRegisterVoice = onReRegisterVoice,
        onOpenSystemSettings = onOpenSystemSettings,
        onLogout = onLogout,
        onWithdraw = onWithdraw,
    )
}

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onTabSelected: (MainTab) -> Unit,
    onDangerAlertChange: (Boolean) -> Unit,
    onFamilyShareChange: (Boolean) -> Unit,
    onQuietHoursChange: (Boolean) -> Unit,
    onReRegisterVoice: () -> Unit,
    onOpenSystemSettings: () -> Unit,
    onLogout: () -> Unit,
    onWithdraw: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MainTabScaffold(
        currentTab = MainTab.Settings,
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
            Text(
                "설정",
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 24.sp),
                color = Ink,
            )

            Spacer(Modifier.height(18.dp))
            ProfileCard(state, onReRegisterVoice)

            Spacer(Modifier.height(18.dp))
            SectionLabel("알림")
            Spacer(Modifier.height(8.dp))
            SettingsCard {
                ToggleRow("위험 감지 알림", "위험 판정 시 즉시 알려드려요",
                    state.dangerAlert, onDangerAlertChange)
                RowLine()
                ToggleRow("가족 공유 알림", "가족이 위험 번호를 공유하면 알려드려요",
                    state.familyShareAlert, onFamilyShareChange)
                RowLine()
                ToggleRow("방해 금지 시간", "밤 10시 ~ 오전 7시에는 알림을 미뤄요",
                    state.quietHours, onQuietHoursChange)
            }

            Spacer(Modifier.height(18.dp))
            SectionLabel("권한 상태")
            Spacer(Modifier.height(8.dp))
            SettingsCard {
                state.permissions.forEachIndexed { index, item ->
                    PermissionRow(item)
                    if (index < state.permissions.lastIndex) RowLine()
                }
            }
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(White)
                    .clickable(onClick = onOpenSystemSettings)
                    .padding(horizontal = 18.dp, vertical = 15.dp),
            ) {
                Text(
                    "시스템 설정 열기",
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                    color = Amber500,
                )
            }

            Spacer(Modifier.height(18.dp))
            SectionLabel("계정")
            Spacer(Modifier.height(8.dp))
            SettingsCard {
                LinkRow("개인정보 처리방침") { }
                RowLine()
                LinkRow("서비스 이용약관") { }
                RowLine()
                LinkRow("로그아웃", onClick = onLogout)
                RowLine()
                LinkRow("회원 탈퇴", danger = true, onClick = onWithdraw)
            }

            Spacer(Modifier.height(24.dp))
            Text(
                "IsFam 0.1.0",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = InkFaint,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))
        }
    }
}

// ── 프로필 ────────────────────────────────────────────────────

@Composable
private fun ProfileCard(state: SettingsUiState, onReRegister: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(5.dp, RoundedCornerShape(22.dp), clip = false)
            .clip(RoundedCornerShape(22.dp))
            .background(White)
            .padding(18.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Safe),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    state.userName.take(1),
                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = 17.sp),
                    color = White,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    state.userName,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
                    color = Ink,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    state.phoneNumber,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                    color = InkMuted,
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        RowLine()
        Spacer(Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    "내 목소리",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                    color = Ink,
                )
                Spacer(Modifier.height(3.dp))
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(SafeBadgeBg)
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                ) {
                    Text(
                        if (state.voiceRegistered) "등록 완료" else "미등록",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                        color = SafeBadgeFg,
                    )
                }
            }
            Text(
                "다시 등록 ›",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                color = Amber500,
                modifier = Modifier.clickable(onClick = onReRegister),
            )
        }
    }
}

// ── 공통 ──────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
        color = InkMuted,
        modifier = Modifier.padding(start = 4.dp),
    )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(5.dp, RoundedCornerShape(22.dp), clip = false)
            .clip(RoundedCornerShape(22.dp))
            .background(White),
    ) { content() }
}

@Composable
private fun RowLine() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(RowDivider))
}

@Composable
private fun ToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.5.sp),
                color = Ink,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                description,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = InkMuted,
            )
        }
        Spacer(Modifier.size(12.dp))
        IsFamToggle(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun PermissionRow(item: PermissionStatusItem) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                item.label,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                color = Ink,
            )
            if (item.unverifiable) {
                Spacer(Modifier.height(3.dp))
                Text(
                    // 앱이 삼성 전화 앱 설정을 읽을 수 없다는 점을 사용자에게 알립니다
                    "첫 통화 후 자동으로 확인됩니다",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                    color = InkFaint,
                )
            }
        }
        Text(
            if (item.granted) "허용됨" else "필요",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
            color = if (item.granted) SafeBadgeFg else Amber500,
        )
    }
}

@Composable
private fun LinkRow(
    title: String,
    danger: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 15.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
            color = if (danger) Danger else Ink,
        )
        Text("›", style = MaterialTheme.typography.titleMedium, color = InkFaint)
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 1000)
@Composable
private fun SettingsPreview() = IsFamTheme {
    SettingsScreen(
        state = FakeSettingsData.state,
        onTabSelected = {}, onDangerAlertChange = {}, onFamilyShareChange = {},
        onQuietHoursChange = {}, onReRegisterVoice = {}, onOpenSystemSettings = {},
        onLogout = {}, onWithdraw = {},
    )
}
