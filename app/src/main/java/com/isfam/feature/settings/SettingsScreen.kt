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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.isfam.core.designsystem.Danger
import com.isfam.core.designsystem.Ink
import com.isfam.core.designsystem.InkFaint
import com.isfam.core.designsystem.InkMuted
import com.isfam.core.designsystem.IsFamTheme
import com.isfam.core.rememberAppContainer
import com.isfam.core.designsystem.IsFamToggle
import com.isfam.core.designsystem.MainTab
import com.isfam.core.designsystem.MainTabScaffold
import com.isfam.core.designsystem.RowDivider
import com.isfam.core.designsystem.SafeBadgeBg
import com.isfam.core.designsystem.SafeBadgeFg
import com.isfam.core.designsystem.Tint50
import com.isfam.core.designsystem.White

/**
 * 26. 설정
 *
 * 구성
 *   프로필 카드 (이름 · 번호 · 보호 중 배지)
 *   계정      — 내 정보 / 내 목소리 관리 / 가족 관리
 *   권한 및 알림 — 자동 분석 마스터 토글 / 마이크 권한 / 위험 알림 / 민감도
 *   개인정보   — 성문 데이터 관리 / 음성 데이터 전체 삭제
 *   로그아웃 + 버전
 *
 * "음성 데이터 전체 삭제"는 개인정보보호법상 정보주체의 삭제권에
 * 해당하므로 반드시 노출되어야 합니다.
 */
data class SettingsUiState(
    val userName: String,
    val phoneNumber: String,
    val protectionActive: Boolean,
    val voiceRegistered: Boolean,
    val familyCount: Int,
    /** 통화 종료 후 자동 분석. 끄면 서비스 전체가 멈춥니다. */
    val autoAnalysis: Boolean,
    val microphoneGranted: Boolean,
    val dangerPush: Boolean,
    val alertSensitivity: AlertSensitivity,
    val voiceprintStorageLabel: String,
    val appVersion: String,
)

enum class AlertSensitivity(val label: String) {
    Low("낮음"), Medium("보통"), High("높음"),
}

object FakeSettingsData {
    val state = SettingsUiState(
        userName = "김서연",
        phoneNumber = "010-2847-1123",
        protectionActive = true,
        voiceRegistered = true,
        familyCount = 5,
        autoAnalysis = true,
        microphoneGranted = true,
        dangerPush = true,
        alertSensitivity = AlertSensitivity.High,
        voiceprintStorageLabel = "암호화 보관",
        appVersion = "v1.2.0",
    )
}

@Composable
fun SettingsRoute(
    onTabSelected: (MainTab) -> Unit,
    onMyInfo: () -> Unit,
    onVoiceManage: () -> Unit,
    onFamilyManage: () -> Unit,
    onOpenSystemSettings: () -> Unit,
    onVoiceprintStorage: () -> Unit,
    onDeleteAllVoiceData: () -> Unit,
    onLogout: () -> Unit,
) {
    val container = rememberAppContainer()
    val scope = rememberCoroutineScope()

    var state by remember { mutableStateOf(FakeSettingsData.state) }

    // 서버 상태로 덮어씁니다.
    // 실패해도 화면은 기본값으로 뜨므로 흐름을 막지 않습니다.
    LaunchedEffect(Unit) {
        container.authRepository.getMe().onSuccess { me ->
            state = state.copy(
                userName = me.displayName,
                phoneNumber = me.phoneNumber,
            )
        }
        container.voiceprintRepository.getStatus().onSuccess { voice ->
            state = state.copy(voiceRegistered = voice.registered)
        }
        container.familyRepository.getFamily().onSuccess { family ->
            state = state.copy(familyCount = family.members.size)
        }
        container.settingsRepository.getSettings().onSuccess { settings ->
            state = state.copy(dangerPush = settings.notificationEnabled)
        }
    }

    SettingsScreen(
        state = state,
        onTabSelected = onTabSelected,
        onAutoAnalysisChange = { state = state.copy(autoAnalysis = it) },
        onDangerPushChange = { enabled ->
            state = state.copy(dangerPush = enabled)
            scope.launch {
                container.settingsRepository.updateNotificationEnabled(enabled)
            }
        },
        onSensitivityClick = {
            state = state.copy(
                alertSensitivity = when (state.alertSensitivity) {
                    AlertSensitivity.Low -> AlertSensitivity.Medium
                    AlertSensitivity.Medium -> AlertSensitivity.High
                    AlertSensitivity.High -> AlertSensitivity.Low
                }
            )
        },
        onMyInfo = onMyInfo,
        onVoiceManage = onVoiceManage,
        onFamilyManage = onFamilyManage,
        onOpenSystemSettings = onOpenSystemSettings,
        onVoiceprintStorage = onVoiceprintStorage,
        onDeleteAllVoiceData = onDeleteAllVoiceData,
        onLogout = onLogout,
    )
}

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onTabSelected: (MainTab) -> Unit,
    onAutoAnalysisChange: (Boolean) -> Unit,
    onDangerPushChange: (Boolean) -> Unit,
    onSensitivityClick: () -> Unit,
    onMyInfo: () -> Unit,
    onVoiceManage: () -> Unit,
    onFamilyManage: () -> Unit,
    onOpenSystemSettings: () -> Unit,
    onVoiceprintStorage: () -> Unit,
    onDeleteAllVoiceData: () -> Unit,
    onLogout: () -> Unit,
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

            Spacer(Modifier.height(16.dp))
            ProfileCard(state)

            // ── 계정 ──────────────────────────────────────────
            Spacer(Modifier.height(20.dp))
            SectionLabel("계정")
            Spacer(Modifier.height(8.dp))
            SettingsCard {
                ValueRow("내 정보", state.userName, onMyInfo)
                RowLine()
                ValueRow(
                    "내 목소리 관리",
                    if (state.voiceRegistered) "등록 완료" else "미등록",
                    onVoiceManage,
                )
                RowLine()
                ValueRow("가족 관리", "${state.familyCount}명", onFamilyManage)
            }

            // ── 권한 및 알림 ──────────────────────────────────
            Spacer(Modifier.height(20.dp))
            SectionLabel("권한 및 알림")
            Spacer(Modifier.height(8.dp))
            SettingsCard {
                ToggleRow(
                    "통화 종료 후 자동 분석",
                    state.autoAnalysis,
                    onAutoAnalysisChange,
                )
                RowLine()
                ValueRow(
                    "마이크 권한",
                    if (state.microphoneGranted) "허용됨" else "필요",
                    onOpenSystemSettings,
                )
                RowLine()
                ToggleRow("위험 알림 푸시", state.dangerPush, onDangerPushChange)
                RowLine()
                ValueRow("알림 민감도", state.alertSensitivity.label, onSensitivityClick)
            }

            // ── 개인정보 ──────────────────────────────────────
            Spacer(Modifier.height(20.dp))
            SectionLabel("개인정보")
            Spacer(Modifier.height(8.dp))
            SettingsCard {
                ValueRow(
                    "성문 데이터 관리",
                    state.voiceprintStorageLabel,
                    onVoiceprintStorage,
                )
                RowLine()
                // 개인정보보호법상 정보주체의 삭제권. 반드시 노출되어야 합니다.
                ValueRow("음성 데이터 전체 삭제", null, onDeleteAllVoiceData)
            }

            // ── 로그아웃 ──────────────────────────────────────
            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "로그아웃",
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                    color = Danger,
                    modifier = Modifier.clickable(onClick = onLogout),
                )
                Text(
                    state.appVersion,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = InkFaint,
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ── 프로필 ────────────────────────────────────────────────────

@Composable
private fun ProfileCard(state: SettingsUiState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(5.dp, RoundedCornerShape(22.dp), clip = false)
            .clip(RoundedCornerShape(22.dp))
            .background(White)
            .padding(horizontal = 18.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Tint50),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                state.userName.take(1),
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 17.sp),
                color = Ink,
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

        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(SafeBadgeBg)
                .padding(horizontal = 11.dp, vertical = 6.dp),
        ) {
            Text(
                if (state.protectionActive) "보호 중" else "중지됨",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = SafeBadgeFg,
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
            .shadow(5.dp, RoundedCornerShape(20.dp), clip = false)
            .clip(RoundedCornerShape(20.dp))
            .background(White),
    ) { content() }
}

@Composable
private fun RowLine() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(RowDivider))
}

/** 라벨 + 값 + › */
@Composable
private fun ValueRow(
    title: String,
    value: String?,
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
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.5.sp),
            color = Ink,
            modifier = Modifier.weight(1f),
        )
        if (value != null) {
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                color = InkMuted,
            )
            Spacer(Modifier.size(6.dp))
        }
        Text("›", style = MaterialTheme.typography.titleMedium, color = InkFaint)
    }
}

/** 라벨 + 토글 */
@Composable
private fun ToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.5.sp),
            color = Ink,
            modifier = Modifier.weight(1f),
        )
        IsFamToggle(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun SettingsPreview() = IsFamTheme {
    SettingsScreen(
        state = FakeSettingsData.state,
        onTabSelected = {}, onAutoAnalysisChange = {}, onDangerPushChange = {},
        onSensitivityClick = {}, onMyInfo = {}, onVoiceManage = {},
        onFamilyManage = {}, onOpenSystemSettings = {}, onVoiceprintStorage = {},
        onDeleteAllVoiceData = {}, onLogout = {},
    )
}