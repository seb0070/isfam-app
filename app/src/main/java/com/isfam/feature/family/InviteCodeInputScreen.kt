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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.compose.ui.unit.sp
import com.isfam.core.designsystem.FieldHelper
import com.isfam.core.designsystem.IconTintEnd
import com.isfam.core.designsystem.IconTintStart
import com.isfam.core.designsystem.Ink
import com.isfam.core.designsystem.InkBody2
import com.isfam.core.rememberAppContainer
import com.isfam.core.designsystem.InviteCodeInput
import com.isfam.data.api.ApiError
import com.isfam.data.repository.ApiFailure
import com.isfam.core.designsystem.IsFamButton
import com.isfam.core.designsystem.IsFamScaffold
import com.isfam.core.designsystem.IsFamTheme
import com.isfam.core.designsystem.IsFamTopBar
import com.isfam.core.designsystem.White

/**
 * 17. 초대 코드 입력
 *
 * ⚠️ 이 화면은 폴백이 아니라 주 경로입니다.
 *    딥링크는 앱 미설치 상태에서 동작하지 않고, 부모님이 링크를
 *    열지 못하는 경우도 흔합니다. 코드 직접 입력이 가장 확실합니다.
 *
 * UI 키트 실측값
 *   제목 800 25/1.4
 *   코드 칸 height 62 · radius 16 · gap 7 · 3-3 사이에 하이픈
 *   포커스 칸 2dp 앰버 테두리 · 빈 칸 #F3EDE4
 *   안내 카드 radius 20 · padding 15/16 · 아이콘 30 radius 10
 */
enum class CodeError(val message: String) {
    Expired("만료된 초대 코드예요. 가족에게 새 코드를 요청해 주세요."),
    NotFound("만료되었거나 존재하지 않는 코드예요. 다시 확인해 주세요."),
    AlreadyJoined("이미 참여한 가족 공간이에요."),
    Unknown("코드를 확인하지 못했어요. 잠시 후 다시 시도해 주세요."),
}

@Composable
fun InviteCodeInputRoute(
    onVerified: (code: String) -> Unit,
    onBack: () -> Unit,
) {
    val familyRepo = rememberAppContainer().familyRepository
    val scope = rememberCoroutineScope()

    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<CodeError?>(null) }
    var checking by remember { mutableStateOf(false) }

    InviteCodeInputScreen(
        code = code,
        error = error,
        checking = checking,
        onCodeChange = {
            code = it
            error = null
        },
        onSubmit = {
            scope.launch {
                checking = true
                error = null

                // 수락 전에 미리보기로 유효성만 확인합니다.
                // 바로 수락하면 잘못 입력한 코드로도 남의 가족에
                // 들어가게 되므로, 어느 가족인지 보여주고 동의를 받습니다.
                familyRepo.previewInvitation(code.uppercase())
                    .onSuccess { onVerified(code.uppercase()) }
                    .onFailure {
                        error = when ((it as? ApiFailure)?.error) {
                            ApiError.InviteInvalid -> CodeError.NotFound
                            ApiError.InviteAlreadyMember -> CodeError.AlreadyJoined
                            else -> CodeError.Unknown
                        }
                    }
                checking = false
            }
        },
        onBack = onBack,
    )
}

@Composable
fun InviteCodeInputScreen(
    code: String,
    error: CodeError?,
    checking: Boolean,
    onCodeChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IsFamScaffold(
        modifier = modifier,
        topBar = { IsFamTopBar(title = "초대 코드 입력", onBack = onBack) },
        bottomBar = {
            IsFamButton(
                text = if (checking) "확인 중…" else "가족으로 연결하기",
                onClick = onSubmit,
                enabled = code.length == 6 && !checking,
            )
        },
    ) {
        Column(
            modifier = Modifier.fillMaxSize().imePadding().padding(horizontal = 26.dp),
        ) {
            Spacer(Modifier.height(22.dp))

            Text(
                "받은 초대 코드\n6자리를 입력해 주세요",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 25.sp, lineHeight = 35.sp,
                ),
                color = Ink,
            )

            Spacer(Modifier.height(24.dp))

            InviteCodeInput(value = code, onValueChange = onCodeChange)

            if (error != null) {
                FieldHelper(error.message, isError = true)
            }

            Spacer(Modifier.height(18.dp))

            // 안내 카드
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(5.dp, RoundedCornerShape(20.dp), clip = false)
                    .clip(RoundedCornerShape(20.dp))
                    .background(White)
                    .padding(horizontal = 16.dp, vertical = 15.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(11.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.linearGradient(listOf(IconTintStart, IconTintEnd))
                        ),
                )
                Text(
                    "초대 코드는 초대한 가족의\n[가족 관리 > 초대하기] 화면에 있어요",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.5.sp, lineHeight = 20.sp,
                    ),
                    color = InkBody2,
                )
            }
        }
    }
}

@Preview(name = "입력 중", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun InviteCodePreview() = IsFamTheme {
    InviteCodeInputScreen(
        code = "F7K2", error = null, checking = false,
        onCodeChange = {}, onSubmit = {}, onBack = {},
    )
}

@Preview(name = "오류", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun InviteCodeErrorPreview() = IsFamTheme {
    InviteCodeInputScreen(
        code = "F7K2M9", error = CodeError.Expired, checking = false,
        onCodeChange = {}, onSubmit = {}, onBack = {},
    )
}