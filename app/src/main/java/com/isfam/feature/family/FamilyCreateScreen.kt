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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.isfam.core.designsystem.Amber500
import com.isfam.core.designsystem.Ink
import com.isfam.core.rememberAppContainer
import com.isfam.core.designsystem.Danger
import com.isfam.core.designsystem.IsFamButton
import com.isfam.data.api.ApiError
import com.isfam.data.repository.ApiFailure
import com.isfam.core.designsystem.IsFamScaffold
import com.isfam.core.designsystem.IsFamTextField
import com.isfam.core.designsystem.IsFamTheme
import com.isfam.core.designsystem.Mascot
import com.isfam.core.designsystem.MascotImage
import com.isfam.core.designsystem.StepProgressBar
import com.isfam.core.designsystem.Tint50

/**
 * 14. 가족 공간 만들기
 *
 * UI 키트 실측값
 *   진행바 50% · "1 / 2 단계"
 *   아이브로우 700 12 앰버 · 제목 800 25/1.35 · 마스코트 88 radius 28
 *   입력 필드 포커스 상태
 */
@Composable
fun FamilyCreateRoute(
    ownerName: String,
    onCreated: (spaceName: String) -> Unit,
    onBack: () -> Unit,
) {
    val familyRepo = rememberAppContainer().familyRepository
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf("${ownerName}님의 가족 공간") }
    var submitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    FamilyCreateScreen(
        spaceName = name,
        submitting = submitting,
        errorMessage = errorMessage,
        onSpaceNameChange = {
            name = it
            errorMessage = null
        },
        onCreate = {
            scope.launch {
                submitting = true
                errorMessage = null

                familyRepo.createFamily(name.trim())
                    .onSuccess { onCreated(it.name) }
                    .onFailure { error ->
                        // 이미 공간이 있으면(FAMILY_004) 실패로 막지 않고
                        // 그대로 다음 단계로 보냅니다. 사용자가 두 번 눌렀거나
                        // 뒤로가기로 되돌아온 경우이므로 목적은 이미 달성됐습니다.
                        if ((error as? ApiFailure)?.error == ApiError.FamilyAlreadyJoined) {
                            familyRepo.getFamily()
                                .onSuccess { onCreated(it.name) }
                                .onFailure { errorMessage = "가족 공간을 불러오지 못했어요" }
                        } else {
                            errorMessage = (error as? ApiFailure)?.displayMessage
                                ?: "가족 공간을 만들지 못했어요"
                        }
                    }
                submitting = false
            }
        },
        onBack = onBack,
    )
}

@Composable
fun FamilyCreateScreen(
    spaceName: String,
    submitting: Boolean = false,
    errorMessage: String? = null,
    onSpaceNameChange: (String) -> Unit,
    onCreate: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IsFamScaffold(
        modifier = modifier,
        topBar = {
            StepProgressBar(
                currentStep = 1, totalSteps = 2,
                onBack = onBack, progressOverride = 0.5f,
            )
        },
        bottomBar = {
            IsFamButton(
                text = if (submitting) "만드는 중…" else "가족 공간 만들고 초대하기",
                onClick = onCreate,
                enabled = !submitting && spaceName.isNotBlank(),
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
        },
    ) {
        Column(
            modifier = Modifier.fillMaxSize().imePadding().padding(horizontal = 26.dp),
        ) {
            Spacer(Modifier.height(22.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "내 목소리 보호 준비 완료!",
                        style = MaterialTheme.typography.labelMedium,
                        color = Amber500,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "함께 보호받을\n가족 공간 이름을 정해 주세요",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = 25.sp, lineHeight = 34.sp,
                        ),
                        color = Ink,
                    )
                }

                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(Tint50)
                        .padding(6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    MascotImage(
                        mascot = Mascot.Safe,
                        size = 76.dp,
                        cornerRadius = 22.dp,
                        background = Color.Transparent,
                    )
                }
            }

            Spacer(Modifier.height(30.dp))

            IsFamTextField(
                label = "가족 공간 이름",
                value = spaceName,
                onValueChange = onSpaceNameChange,
                placeholder = "우리 가족",
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun FamilyCreatePreview() = IsFamTheme {
    FamilyCreateScreen(
        spaceName = "김서연님의 가족 공간",
        onSpaceNameChange = {}, onCreate = {}, onBack = {},
    )
}