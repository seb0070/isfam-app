package com.isfam.feature.auth

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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.compose.ui.unit.sp
import com.isfam.core.designsystem.Ink
import com.isfam.core.designsystem.InkBody
import com.isfam.core.designsystem.InkMuted
import com.isfam.core.designsystem.InkMuted2
import com.isfam.core.rememberAppContainer
import com.isfam.core.designsystem.IsFamButton
import com.isfam.data.repository.ApiFailure
import com.isfam.core.designsystem.IsFamCheckbox
import com.isfam.core.designsystem.IsFamScaffold
import com.isfam.core.designsystem.IsFamTextField
import com.isfam.core.designsystem.IsFamTheme
import com.isfam.core.designsystem.Mascot
import com.isfam.core.designsystem.MascotImage
import com.isfam.core.designsystem.Tint50

/**
 * 05. 로그인
 *
 * UI 키트 실측값
 *   ← padding 14 26 0 · font 700 22
 *   제목 행 padding 22 26 0 · gap 14 · 마스코트 84×84 radius 28 bg #FFF1DE padding 6
 *   입력 영역 padding 34 26 0 · gap 12
 *   하단 padding 0 26 34 · gap 14
 */
@Composable
fun LoginRoute(
    onLoginSuccess: () -> Unit,
    onSignUp: () -> Unit,
    onBack: () -> Unit,
) {
    val auth = rememberAppContainer().authRepository
    val scope = rememberCoroutineScope()

    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var autoLogin by remember { mutableStateOf(true) }
    var submitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LoginScreen(
        phone = phone,
        password = password,
        autoLogin = autoLogin,
        submitting = submitting,
        errorMessage = errorMessage,
        onPhoneChange = {
            phone = it
            errorMessage = null
        },
        onPasswordChange = {
            password = it
            errorMessage = null
        },
        onAutoLoginChange = { autoLogin = it },
        onLogin = {
            scope.launch {
                submitting = true
                errorMessage = null

                auth.login(phone.filter(Char::isDigit), password)
                    .onSuccess { onLoginSuccess() }
                    .onFailure {
                        // 가입되지 않은 번호인지 비밀번호가 틀렸는지
                        // 서버가 구분해 주므로 그대로 보여줍니다.
                        errorMessage = (it as? ApiFailure)?.error?.message
                            ?: "로그인하지 못했어요. 잠시 후 다시 시도해 주세요"
                    }
                submitting = false
            }
        },
        onSignUp = onSignUp,
        onFindPassword = { /* TODO: 비밀번호 재설정 */ },
        onBack = onBack,
    )
}

@Composable
fun LoginScreen(
    phone: String,
    password: String,
    autoLogin: Boolean,
    submitting: Boolean = false,
    errorMessage: String? = null,
    onPhoneChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onAutoLoginChange: (Boolean) -> Unit,
    onLogin: () -> Unit,
    onSignUp: () -> Unit,
    onFindPassword: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IsFamScaffold(
        modifier = modifier,
        topBar = {
            Text(
                "←",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 22.sp, fontWeight = FontWeight.Bold,
                ),
                color = Ink,
                modifier = Modifier
                    .padding(start = 26.dp, top = 14.dp)
                    .clickable(onClick = onBack),
            )
        },
        bottomBar = {
            IsFamButton(
                text = if (submitting) "로그인 중…" else "로그인",
                onClick = onLogin,
                enabled = !submitting &&
                        phone.filter(Char::isDigit).length >= 10 &&
                        password.isNotBlank(),
            )
            errorMessage?.let {
                Spacer(Modifier.height(8.dp))
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                    color = com.isfam.core.designsystem.Danger,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = buildAnnotatedString {
                    append("처음이신가요? ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Ink)) {
                        append("회원가입")
                    }
                },
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
                color = InkMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().clickable(onClick = onSignUp),
            )
        },
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 제목 + 마스코트
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 26.dp, end = 26.dp, top = 22.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "다시 만나서 반가워요\n번호로 로그인해 주세요",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Ink,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .background(Tint50, RoundedCornerShape(28.dp))
                        .padding(6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    MascotImage(
                        mascot = Mascot.Watching,
                        size = 72.dp,
                        cornerRadius = 22.dp,
                        background = androidx.compose.ui.graphics.Color.Transparent,
                    )
                }
            }

            // 입력
            Column(
                modifier = Modifier.padding(start = 26.dp, end = 26.dp, top = 34.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                IsFamTextField(
                    label = "휴대폰 번호",
                    value = phone,
                    onValueChange = onPhoneChange,
                    placeholder = "010 1234 5678",
                    keyboardType = KeyboardType.Phone,
                )
                IsFamTextField(
                    label = "비밀번호",
                    value = password,
                    onValueChange = onPasswordChange,
                    placeholder = "••••••",
                    isPassword = true,
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IsFamCheckbox(autoLogin, onAutoLoginChange, size = 20.dp, strong = true)
                    Spacer(Modifier.size(8.dp))
                    Text(
                        "자동 로그인",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
                        color = InkBody,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "비밀번호 찾기",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
                        color = InkMuted2,
                        modifier = Modifier.clickable(onClick = onFindPassword),
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun LoginPreview() = IsFamTheme {
    LoginScreen(
        phone = "010 1234 5678", password = "123456", autoLogin = true,
        onPhoneChange = {}, onPasswordChange = {}, onAutoLoginChange = {},
        onLogin = {}, onSignUp = {}, onFindPassword = {}, onBack = {},
    )
}