package com.isfam.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.isfam.core.designsystem.CheckboxRow
import com.isfam.core.designsystem.DividerLight
import com.isfam.core.designsystem.Ink
import com.isfam.core.designsystem.IsFamButton
import com.isfam.core.designsystem.IsFamScaffold
import com.isfam.core.designsystem.IsFamTextField
import com.isfam.core.designsystem.IsFamTheme
import com.isfam.core.designsystem.StepProgressBar
import com.isfam.core.designsystem.White

/**
 * 06. 회원가입
 *
 * UI 키트 실측값
 *   진행바 40% · "1/3"
 *   제목 padding 26 26 0 · 800 27/1.35
 *   입력 padding 30 26 0 · gap 12
 *   약관 카드 radius 20 · padding 16 18 · gap 14 · 전체동의 아래 구분선
 */
data class SignUpAgreements(
    val terms: Boolean = false,
    val voiceData: Boolean = false,
    val marketing: Boolean = false,
) {
    val allChecked: Boolean get() = terms && voiceData && marketing
    /** 필수 항목만 충족되면 진행 가능 */
    val requiredMet: Boolean get() = terms && voiceData
}

@Composable
fun SignUpRoute(
    onRequestOtp: (phoneNumber: String) -> Unit,
    onBack: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var agreements by remember { mutableStateOf(SignUpAgreements()) }

    SignUpScreen(
        name = name,
        displayName = displayName,
        phone = phone,
        agreements = agreements,
        onNameChange = { name = it },
        onDisplayNameChange = { displayName = it },
        onPhoneChange = { phone = it },
        onAgreementsChange = { agreements = it },
        // TODO: POST /api/v1/auth/signup 은 OTP 확인 후 호출합니다
        onRequestOtp = { onRequestOtp(phone) },
        onBack = onBack,
    )
}

@Composable
fun SignUpScreen(
    name: String,
    displayName: String,
    phone: String,
    agreements: SignUpAgreements,
    onNameChange: (String) -> Unit,
    onDisplayNameChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onAgreementsChange: (SignUpAgreements) -> Unit,
    onRequestOtp: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val canProceed = name.isNotBlank() && phone.isNotBlank() && agreements.requiredMet

    IsFamScaffold(
        modifier = modifier,
        topBar = {
            StepProgressBar(
                currentStep = 1, totalSteps = 3,
                onBack = onBack, progressOverride = 0.4f,
            )
        },
        bottomBar = {
            IsFamButton(text = "인증번호 받기", onClick = onRequestOtp, enabled = canProceed)
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                "이름과 번호를\n알려주세요",
                style = MaterialTheme.typography.headlineLarge,
                color = Ink,
                modifier = Modifier.padding(start = 26.dp, end = 26.dp, top = 26.dp),
            )

            Column(
                modifier = Modifier.padding(start = 26.dp, end = 26.dp, top = 30.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                IsFamTextField("이름", name, onNameChange, placeholder = "김서연")
                IsFamTextField(
                    "닉네임 · 앱 내 호칭", displayName, onDisplayNameChange,
                    placeholder = "서연",
                )
                IsFamTextField(
                    "휴대폰 번호", phone, onPhoneChange,
                    placeholder = "010 1234 5678",
                    keyboardType = KeyboardType.Phone,
                )

                Spacer(Modifier.height(10.dp))

                // 약관 카드
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(5.dp, RoundedCornerShape(20.dp), clip = false)
                        .clip(RoundedCornerShape(20.dp))
                        .background(White)
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    CheckboxRow(
                        checked = agreements.allChecked,
                        onCheckedChange = { checked ->
                            onAgreementsChange(SignUpAgreements(checked, checked, checked))
                        },
                        label = "약관 전체 동의",
                        strong = true,
                        checkboxSize = 22.dp,
                        labelBold = true,
                    )

                    HorizontalDivider(color = DividerLight)

                    CheckboxRow(
                        checked = agreements.terms,
                        onCheckedChange = { onAgreementsChange(agreements.copy(terms = it)) },
                        label = "[필수] 서비스 이용약관",
                        trailingText = "보기",
                        onTrailingClick = { /* TODO: 약관 화면 */ },
                    )
                    CheckboxRow(
                        checked = agreements.voiceData,
                        onCheckedChange = { onAgreementsChange(agreements.copy(voiceData = it)) },
                        label = "[필수] 음성 데이터 처리 동의",
                        trailingText = "보기",
                        onTrailingClick = { /* TODO */ },
                    )
                    CheckboxRow(
                        checked = agreements.marketing,
                        onCheckedChange = { onAgreementsChange(agreements.copy(marketing = it)) },
                        label = "[선택] 마케팅 정보 수신",
                        trailingText = "보기",
                        onTrailingClick = { /* TODO */ },
                    )
                }

                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun SignUpPreview() = IsFamTheme {
    SignUpScreen(
        name = "김서연", displayName = "서연", phone = "010 1234 5678",
        agreements = SignUpAgreements(terms = true, voiceData = true, marketing = false),
        onNameChange = {}, onDisplayNameChange = {}, onPhoneChange = {},
        onAgreementsChange = {}, onRequestOtp = {}, onBack = {},
    )
}
