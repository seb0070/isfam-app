package com.isfam.feature.family

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.compose.ui.unit.sp
import com.isfam.core.designsystem.Amber700
import com.isfam.core.designsystem.CheckboxRow
import com.isfam.core.designsystem.DividerLight
import com.isfam.core.designsystem.IndicatorOff
import com.isfam.core.designsystem.InfoRow
import com.isfam.core.designsystem.InitialAvatar
import com.isfam.core.designsystem.Ink
import com.isfam.core.designsystem.InkBody2
import com.isfam.core.rememberAppContainer
import com.isfam.core.designsystem.Danger
import com.isfam.core.designsystem.IsFamButton
import com.isfam.data.repository.ApiFailure
import com.isfam.core.designsystem.IsFamScaffold
import com.isfam.core.designsystem.IsFamTextButton
import com.isfam.core.designsystem.IsFamTheme
import com.isfam.core.designsystem.Tint50
import com.isfam.core.designsystem.White

/**
 * 18. 초대 수락 · 연결 (Flow B)
 *
 * 딥링크 진입과 코드 입력, 두 경로가 여기서 합류합니다.
 *
 * UI 키트 실측값
 *   아바타 64 radius 22 · 연결선 26×2 #E4D9C9
 *   pill 700 11.5 #C05C05 배경 #FFF1DE
 *   제목 800 26/1.42 · 부제 500 14.5/1.65
 *   정보 카드 radius 22 · padding 20 · gap 14
 */
data class InvitePreview(
    val inviterName: String,
    val spaceName: String,
    val memberInitials: List<String>,
    val enteredByLink: Boolean,
)

@Composable
fun InviteAcceptRoute(
    inviteCode: String,
    myInitial: String,
    /** 딥링크로 들어왔는지. 화면 문구가 달라집니다 */
    enteredByLink: Boolean = false,
    onAccepted: () -> Unit,
    onDecline: () -> Unit,
) {
    val familyRepo = rememberAppContainer().familyRepository
    val scope = rememberCoroutineScope()

    var preview by remember { mutableStateOf<InvitePreview?>(null) }
    var consent by remember { mutableStateOf(false) }
    var submitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // 어느 가족 공간인지 먼저 보여줍니다.
    // 코드만 보고 수락하게 하면 잘못 입력했을 때
    // 남의 가족 공간에 들어가게 됩니다.
    LaunchedEffect(inviteCode) {
        familyRepo.previewInvitation(inviteCode)
            .onSuccess { server ->
                preview = InvitePreview(
                    // 서버는 초대한 사람 이름을 주지 않습니다.
                    // 공간 이름으로 대신 안내합니다.
                    inviterName = server.familyName,
                    spaceName = server.familyName,
                    // 구성원 이니셜도 주지 않아 인원수만 표시합니다.
                    // 참여 전에 남의 가족 이름을 노출하지 않으려는 서버 설계입니다.
                    memberInitials = List(server.memberCount) { "가" },
                    enteredByLink = enteredByLink,
                )
            }
            .onFailure {
                errorMessage = (it as? ApiFailure)?.displayMessage
                    ?: "초대 정보를 불러오지 못했어요"
            }
    }

    InviteAcceptScreen(
        preview = preview ?: InvitePreview("", "", emptyList(), enteredByLink),
        myInitial = myInitial,
        voiceSharingConsent = consent,
        submitting = submitting,
        errorMessage = errorMessage,
        onConsentChange = { consent = it },
        onAccept = {
            scope.launch {
                submitting = true
                errorMessage = null

                familyRepo.acceptInvitation(inviteCode, consent)
                    .onSuccess { onAccepted() }
                    .onFailure {
                        errorMessage = (it as? ApiFailure)?.displayMessage
                            ?: "가족 공간에 참여하지 못했어요"
                    }
                submitting = false
            }
        },
        onDecline = onDecline,
    )
}

@Composable
fun InviteAcceptScreen(
    preview: InvitePreview,
    myInitial: String,
    submitting: Boolean = false,
    errorMessage: String? = null,
    voiceSharingConsent: Boolean,
    onConsentChange: (Boolean) -> Unit,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IsFamScaffold(
        modifier = modifier,
        bottomBar = {
            IsFamButton(
                text = if (submitting) "참여하는 중…" else "수락하고 목소리 등록하기",
                onClick = onAccept,
                enabled = !submitting && voiceSharingConsent,
            )
            errorMessage?.let {
                Spacer(Modifier.height(8.dp))
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                    color = Danger,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            IsFamTextButton(text = "거절", onClick = onDecline)
        },
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(44.dp))

            // 아바타 연결
            Row(verticalAlignment = Alignment.CenterVertically) {
                InitialAvatar(initial = preview.inviterName.take(1), size = 64.dp)
                Box(
                    modifier = Modifier
                        .padding(horizontal = 6.dp)
                        .width(26.dp)
                        .height(2.dp)
                        .background(IndicatorOff),
                )
                InitialAvatar(initial = myInitial, size = 64.dp, registered = false)
            }

            Spacer(Modifier.height(24.dp))

            if (preview.enteredByLink) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Tint50)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text(
                        "초대 링크로 자동 연결됨 · 코드 입력 없음",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                        color = Amber700,
                    )
                }
                Spacer(Modifier.height(14.dp))
            }

            Text(
                "${preview.inviterName}님이\n가족으로 초대했어요",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 26.sp, lineHeight = 37.sp,
                ),
                color = Ink,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "수락하면 서로의 목소리를 지켜줄 수 있어요",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 14.5.sp, lineHeight = 24.sp,
                ),
                color = InkBody2,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(30.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(5.dp, RoundedCornerShape(22.dp), clip = false)
                    .clip(RoundedCornerShape(22.dp))
                    .background(White)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                InfoRow(label = "가족 이름", value = preview.spaceName)

                HorizontalDivider(color = DividerLight)

                InfoRow(label = "구성원") {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        preview.memberInitials.forEach {
                            InitialAvatar(initial = it, size = 26.dp)
                        }
                    }
                }

                HorizontalDivider(color = DividerLight)

                CheckboxRow(
                    checked = voiceSharingConsent,
                    onCheckedChange = onConsentChange,
                    label = "[필수] 가족과 목소리 정보 공유에 동의합니다",
                    strong = true,
                    checkboxSize = 20.dp,
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun InviteAcceptPreview() = IsFamTheme {
    InviteAcceptScreen(
        preview = InvitePreview(
            inviterName = "김상호",
            spaceName = "김서연님의 가족 공간",
            memberInitials = listOf("상", "서"),
            enteredByLink = true,
        ),
        myInitial = "나",
        voiceSharingConsent = true,
        onConsentChange = {}, onAccept = {}, onDecline = {},
    )
}