package com.isfam.feature.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.isfam.core.designsystem.CheckboxRow
import com.isfam.core.designsystem.CompletedRow
import com.isfam.core.designsystem.DividerLight
import com.isfam.core.designsystem.FieldHelper
import com.isfam.core.designsystem.Ink
import com.isfam.core.designsystem.InkMuted
import com.isfam.core.designsystem.IsFamButton
import com.isfam.core.designsystem.IsFamScaffold
import com.isfam.core.designsystem.IsFamSecondaryButton
import com.isfam.core.designsystem.IsFamTextField
import com.isfam.core.designsystem.IsFamTheme
import com.isfam.core.designsystem.OtpTimer
import com.isfam.core.designsystem.RevealSection
import com.isfam.core.designsystem.StepProgressBar
import com.isfam.core.designsystem.White
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 06. 회원가입 — 순차 노출(Progressive Disclosure)
 *
 * 순서
 *   약관 동의 → 이름 → 닉네임 → 휴대폰 번호 → [인증번호 받기]
 *   → 인증번호(타이머 내장) → [인증 확인] → ✓ 접힘
 *   → 비밀번호 → 비밀번호 확인 → [다음] → 08 권한 화면에서 가입 완료
 *
 * 약관을 맨 앞에 두는 이유:
 * 개인정보 처리 동의를 받기 전에 이름·번호를 수집하면 순서가 어긋납니다.
 * 특히 음성 데이터 처리 동의가 필수인 서비스라 더 그렇습니다.
 * 국내 앱 대부분(토스·카카오·배민)이 같은 순서입니다.
 *
 * 새 입력칸은 항상 화면 아래쪽(키보드 바로 위)에 추가됩니다.
 * 위로 쌓으면 키보드에 가려집니다.
 */

private const val OTP_LENGTH = 6
private const val OTP_TIMEOUT_SEC = 180
private const val MIN_PASSWORD_LENGTH = 8

enum class PhoneVerifyState { Idle, CodeSent, Verifying, Verified, Failed }

data class SignUpForm(
    /**
     * 만 14세 이상 확인.
     * 개인정보보호법상 14세 미만은 법정대리인 동의가 필요한데
     * SMS 인증만으로는 나이를 알 수 없어 자기 확인으로 처리합니다.
     */
    val ageOver14: Boolean = false,
    val terms: Boolean = false,
    val voiceData: Boolean = false,
    val marketing: Boolean = false,
    val name: String = "",
    val displayName: String = "",
    val phone: String = "",
    val code: String = "",
    val password: String = "",
    val passwordConfirm: String = "",
) {
    val allAgreed get() = ageOver14 && terms && voiceData && marketing
    val requiredAgreed get() = ageOver14 && terms && voiceData
    val nameDone get() = name.trim().length >= 2
    val displayNameDone get() = displayName.isNotBlank()
    val phoneDone get() = phone.filter(Char::isDigit).length >= 10
    val passwordValid get() = password.length >= MIN_PASSWORD_LENGTH
    val passwordMatched get() = passwordValid && password == passwordConfirm
}

@Composable
fun SignUpRoute(
    /** 수집한 입력값을 세션에 담아 다음(권한) 화면으로 넘깁니다 */
    onNext: (SignUpForm) -> Unit,
    onBack: () -> Unit,
) {
    var form by remember { mutableStateOf(SignUpForm()) }
    var verifyState by remember { mutableStateOf(PhoneVerifyState.Idle) }
    var remainSec by remember { mutableIntStateOf(OTP_TIMEOUT_SEC) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(verifyState) {
        if (verifyState == PhoneVerifyState.CodeSent) {
            remainSec = OTP_TIMEOUT_SEC
            while (remainSec > 0 && verifyState == PhoneVerifyState.CodeSent) {
                delay(1000); remainSec -= 1
            }
        }
    }

    SignUpScreen(
        form = form,
        verifyState = verifyState,
        remainSec = remainSec,
        onFormChange = { form = it },
        onSendCode = {
            // TODO: 인증번호 발송 API 연결
            verifyState = PhoneVerifyState.CodeSent
        },
        onVerifyCode = {
            scope.launch {
                verifyState = PhoneVerifyState.Verifying
                delay(400)
                // TODO: 서버 검증으로 교체. 현재 서버는 "123456" 고정입니다.
                verifyState =
                    if (form.code == "123456") PhoneVerifyState.Verified
                    else PhoneVerifyState.Failed
            }
        },
        onResend = { verifyState = PhoneVerifyState.CodeSent },
        onEditPhone = {
            verifyState = PhoneVerifyState.Idle
            form = form.copy(code = "")
        },
        // 실제 가입 API 호출은 08 권한 화면에서 이뤄집니다.
        // signup 요청 본문에 권한 상태가 포함되기 때문입니다.
        onSubmit = { onNext(form) },
        onBack = onBack,
    )
}

@Composable
fun SignUpScreen(
    form: SignUpForm,
    verifyState: PhoneVerifyState,
    remainSec: Int,
    onFormChange: (SignUpForm) -> Unit,
    onSendCode: () -> Unit,
    onVerifyCode: () -> Unit,
    onResend: () -> Unit,
    onEditPhone: () -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val verified = verifyState == PhoneVerifyState.Verified

    val showName = form.requiredAgreed
    val showDisplayName = showName && form.nameDone
    val showPhone = showDisplayName && form.displayNameDone
    val showOtp = verifyState == PhoneVerifyState.CodeSent ||
            verifyState == PhoneVerifyState.Verifying ||
            verifyState == PhoneVerifyState.Failed
    val showPassword = verified
    val showPasswordConfirm = showPassword && form.passwordValid

    val canSubmit = verified && form.passwordMatched

    val progress = listOf(
        form.requiredAgreed, form.nameDone, form.displayNameDone,
        verified, form.passwordMatched,
    ).count { it } / 5f

    val scrollState = rememberScrollState()

    LaunchedEffect(showName, showDisplayName, showPhone, showOtp, showPassword, showPasswordConfirm) {
        delay(300)
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    IsFamScaffold(
        modifier = modifier,
        topBar = {
            StepProgressBar(
                currentStep = 1, totalSteps = 3,
                onBack = onBack,
                progressOverride = 0.1f + progress * 0.3f,
            )
        },
        bottomBar = {
            IsFamButton(text = "다음", onClick = onSubmit, enabled = canSubmit)
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .imePadding()
                .padding(start = 26.dp, end = 26.dp),
        ) {
            Spacer(Modifier.height(26.dp))
            Text(
                "약관에 동의하고\n시작해 주세요",
                style = MaterialTheme.typography.headlineLarge,
                color = Ink,
            )
            Spacer(Modifier.height(30.dp))

            // ── 1. 약관 ──────────────────────────────────────
            TermsCard(form = form, onFormChange = onFormChange)

            // ── 2. 이름 ──────────────────────────────────────
            RevealSection(showName) {
                Column {
                    Spacer(Modifier.height(18.dp))
                    IsFamTextField(
                        label = "이름",
                        value = form.name,
                        onValueChange = { onFormChange(form.copy(name = it)) },
                        placeholder = "김서연",
                    )
                }
            }

            // ── 3. 닉네임 ────────────────────────────────────
            RevealSection(showDisplayName) {
                Column {
                    Spacer(Modifier.height(12.dp))
                    IsFamTextField(
                        label = "닉네임 · 앱 내 호칭",
                        value = form.displayName,
                        onValueChange = { onFormChange(form.copy(displayName = it)) },
                        placeholder = "서연",
                    )
                }
            }

            // ── 4. 휴대폰 번호 ───────────────────────────────
            RevealSection(showPhone) {
                Column {
                    Spacer(Modifier.height(12.dp))

                    if (verified) {
                        CompletedRow(
                            label = "휴대폰 번호",
                            value = form.phone,
                            verified = true,
                        )
                    } else {
                        IsFamTextField(
                            label = "휴대폰 번호",
                            value = form.phone,
                            onValueChange = { onFormChange(form.copy(phone = it)) },
                            placeholder = "010 1234 5678",
                            keyboardType = KeyboardType.Phone,
                            enabled = verifyState == PhoneVerifyState.Idle,
                        )

                        if (verifyState == PhoneVerifyState.Idle) {
                            Spacer(Modifier.height(10.dp))
                            IsFamSecondaryButton(
                                text = "인증번호 받기",
                                onClick = onSendCode,
                                enabled = form.phoneDone,
                            )
                        }
                    }
                }
            }

            // ── 5. 인증번호 (타이머를 입력창 안에) ───────────
            AnimatedVisibility(
                visible = showOtp,
                enter = fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column {
                    Spacer(Modifier.height(12.dp))

                    IsFamTextField(
                        label = "인증번호",
                        value = form.code,
                        onValueChange = {
                            if (it.length <= OTP_LENGTH && it.all(Char::isDigit)) {
                                onFormChange(form.copy(code = it))
                            }
                        },
                        placeholder = "6자리 숫자",
                        keyboardType = KeyboardType.NumberPassword,
                        enabled = verifyState != PhoneVerifyState.Verifying,
                        trailing = { OtpTimer(remainSec) },
                    )

                    Spacer(Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "번호를 잘못 입력했나요?",
                            style = MaterialTheme.typography.bodySmall,
                            color = InkMuted,
                            modifier = Modifier.clickable(onClick = onEditPhone),
                        )
                        Text(
                            "인증번호 재전송",
                            style = MaterialTheme.typography.bodySmall,
                            color = InkMuted,
                            textDecoration = TextDecoration.Underline,
                            modifier = Modifier.clickable(onClick = onResend),
                        )
                    }

                    if (verifyState == PhoneVerifyState.Failed) {
                        FieldHelper("인증번호가 일치하지 않아요. 다시 확인해 주세요.", isError = true)
                    } else {
                        FieldHelper("문자가 오지 않으면 스팸함도 확인해 보세요. (데모: 123456)")
                    }

                    Spacer(Modifier.height(12.dp))

                    IsFamSecondaryButton(
                        text = if (verifyState == PhoneVerifyState.Verifying) "확인 중…" else "인증 확인",
                        onClick = onVerifyCode,
                        enabled = form.code.length == OTP_LENGTH &&
                                verifyState != PhoneVerifyState.Verifying,
                    )
                }
            }

            // ── 6. 비밀번호 ──────────────────────────────────
            RevealSection(showPassword) {
                Column {
                    Spacer(Modifier.height(12.dp))
                    IsFamTextField(
                        label = "비밀번호",
                        value = form.password,
                        onValueChange = { onFormChange(form.copy(password = it)) },
                        placeholder = "8자 이상",
                        isPassword = true,
                    )
                    if (form.password.isNotEmpty() && !form.passwordValid) {
                        FieldHelper("8자 이상 입력해 주세요", isError = true)
                    }
                }
            }

            // ── 7. 비밀번호 확인 ─────────────────────────────
            RevealSection(showPasswordConfirm) {
                Column {
                    Spacer(Modifier.height(12.dp))
                    IsFamTextField(
                        label = "비밀번호 확인",
                        value = form.passwordConfirm,
                        onValueChange = { onFormChange(form.copy(passwordConfirm = it)) },
                        placeholder = "한 번 더 입력",
                        isPassword = true,
                    )
                    if (form.passwordConfirm.isNotEmpty() && !form.passwordMatched) {
                        FieldHelper("비밀번호가 일치하지 않아요", isError = true)
                    }
                }
            }

            Spacer(Modifier.height(28.dp))
        }
    }
}

/** 약관 카드. 전체 동의하면 세부 항목이 접힙니다. */
@Composable
private fun TermsCard(
    form: SignUpForm,
    onFormChange: (SignUpForm) -> Unit,
) {
    var expanded by remember { mutableStateOf(true) }
    LaunchedEffect(form.allAgreed) { if (form.allAgreed) expanded = false }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(5.dp, RoundedCornerShape(20.dp), clip = false)
            .clip(RoundedCornerShape(20.dp))
            .background(White)
            .padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CheckboxRow(
                checked = form.allAgreed,
                onCheckedChange = { c ->
                    onFormChange(
                        form.copy(ageOver14 = c, terms = c, voiceData = c, marketing = c)
                    )
                },
                label = "약관 전체 동의",
                strong = true,
                checkboxSize = 22.dp,
                labelBold = true,
                modifier = Modifier.weight(1f),
            )
            Text(
                if (expanded) "접기 ⌃" else "자세히 ⌄",
                style = MaterialTheme.typography.bodySmall,
                color = InkMuted,
                modifier = Modifier.clickable { expanded = !expanded },
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column {
                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = DividerLight)
                Spacer(Modifier.height(14.dp))

                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    CheckboxRow(
                        checked = form.ageOver14,
                        onCheckedChange = { onFormChange(form.copy(ageOver14 = it)) },
                        label = "[필수] 만 14세 이상입니다",
                    )
                    CheckboxRow(
                        checked = form.terms,
                        onCheckedChange = { onFormChange(form.copy(terms = it)) },
                        label = "[필수] 서비스 이용약관",
                        trailingText = "보기",
                        onTrailingClick = { /* TODO: 약관 화면 */ },
                    )
                    CheckboxRow(
                        checked = form.voiceData,
                        onCheckedChange = { onFormChange(form.copy(voiceData = it)) },
                        label = "[필수] 음성 데이터 처리 동의",
                        trailingText = "보기",
                        onTrailingClick = { /* TODO */ },
                    )
                    CheckboxRow(
                        checked = form.marketing,
                        onCheckedChange = { onFormChange(form.copy(marketing = it)) },
                        label = "[선택] 마케팅 정보 수신",
                        trailingText = "보기",
                        onTrailingClick = { /* TODO */ },
                    )
                }
            }
        }

        if (!expanded && form.requiredAgreed) {
            Spacer(Modifier.height(6.dp))
            Text(
                "필수 약관에 모두 동의했어요",
                style = MaterialTheme.typography.bodySmall,
                color = InkMuted,
            )
        }
    }
}

// ── Preview ───────────────────────────────────────────────────

@Preview(name = "1. 약관만", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun SignUpStep1Preview() = IsFamTheme {
    SignUpScreen(
        form = SignUpForm(),
        verifyState = PhoneVerifyState.Idle, remainSec = 180,
        onFormChange = {}, onSendCode = {}, onVerifyCode = {}, onResend = {},
        onEditPhone = {}, onSubmit = {}, onBack = {},
    )
}

@Preview(name = "2. 인증번호 입력", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun SignUpStep2Preview() = IsFamTheme {
    SignUpScreen(
        form = SignUpForm(
            ageOver14 = true, terms = true, voiceData = true, marketing = true,
            name = "김서연", displayName = "서연",
            phone = "010 1234 5678", code = "482",
        ),
        verifyState = PhoneVerifyState.CodeSent, remainSec = 151,
        onFormChange = {}, onSendCode = {}, onVerifyCode = {}, onResend = {},
        onEditPhone = {}, onSubmit = {}, onBack = {},
    )
}

@Preview(name = "3. 인증 후 비밀번호", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun SignUpStep3Preview() = IsFamTheme {
    SignUpScreen(
        form = SignUpForm(
            ageOver14 = true, terms = true, voiceData = true, marketing = true,
            name = "김서연", displayName = "서연",
            phone = "010 1234 5678", code = "123456",
            password = "12345678", passwordConfirm = "12345678",
        ),
        verifyState = PhoneVerifyState.Verified, remainSec = 0,
        onFormChange = {}, onSendCode = {}, onVerifyCode = {}, onResend = {},
        onEditPhone = {}, onSubmit = {}, onBack = {},
    )
}