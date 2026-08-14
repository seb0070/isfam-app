package com.isfam.feature.family

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.isfam.core.designsystem.Amber500
import com.isfam.core.designsystem.Divider
import com.isfam.core.designsystem.Ink
import com.isfam.core.designsystem.InkBody2
import com.isfam.core.designsystem.InkFaint
import com.isfam.core.designsystem.IsFamButton
import com.isfam.core.designsystem.IsFamTextField
import com.isfam.core.designsystem.IsFamTheme
import com.isfam.core.designsystem.IsFamToggle
import com.isfam.core.designsystem.RegistrationBadge
import com.isfam.core.designsystem.Safe
import com.isfam.core.designsystem.ScreenBg
import com.isfam.core.designsystem.White

/**
 * 22. 가족 프로필 설정 바텀시트  ·  23. 자동 분석 OFF 토스트
 *
 * UI 키트 실측값
 *   시트 radius 30 상단 · padding 20/24/32 · 핸들 44×5
 *   아바타 52 radius 18
 *   표시 이름 입력 포커스 상태 · "가입 이름 김상호" 우측 표시
 *   정보 카드 #FAF7F2 radius 16 · padding 14/16
 *   토글 46×28
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberProfileSheet(
    member: FamilyMemberItem,
    onSave: (displayName: String, autoAnalysis: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = White,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
    ) {
        MemberProfileContent(member = member, onSave = onSave)
    }
}

@Composable
fun MemberProfileContent(
    member: FamilyMemberItem,
    onSave: (displayName: String, autoAnalysis: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var displayName by remember { mutableStateOf(member.relation) }
    var autoAnalysis by remember { mutableStateOf(member.autoAnalysis) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 32.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (member.isMe) Safe else Amber500),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    member.initial,
                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = 18.sp),
                    color = White,
                )
            }
            Text(
                "가족 프로필 설정",
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 19.sp),
                color = Ink,
            )
        }

        Spacer(Modifier.height(18.dp))

        // 표시 이름
        IsFamTextField(
            label = "표시 이름",
            value = displayName,
            onValueChange = { displayName = it },
            placeholder = "아빠",
            trailing = {
                Text(
                    "가입 이름 ${member.name}",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = InkFaint,
                )
            },
        )

        Spacer(Modifier.height(10.dp))

        // 정보
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(ScreenBg)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "전화번호",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
                    color = InkBody2,
                )
                Text(
                    member.phoneNumber,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 13.5.sp),
                    color = Ink,
                )
            }

            HorizontalDivider(color = Divider)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "목소리 등록 상태",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
                    color = InkBody2,
                )
                RegistrationBadge(registered = member.registered)
            }
        }

        Spacer(Modifier.height(10.dp))

        // 자동 분석 토글
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(ScreenBg)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "이 가족 통화 자동 분석",
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.5.sp),
                color = Ink,
            )
            IsFamToggle(checked = autoAnalysis, onCheckedChange = { autoAnalysis = it })
        }

        Spacer(Modifier.height(18.dp))

        IsFamButton(text = "완료", onClick = { onSave(displayName, autoAnalysis) })
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 560)
@Composable
private fun MemberProfilePreview() = IsFamTheme {
    MemberProfileContent(
        member = FakeFamilyData.state.members[0],
        onSave = { _, _ -> },
    )
}
