package com.isfam.feature.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.isfam.core.designsystem.Amber500
import com.isfam.core.designsystem.Ink
import com.isfam.core.designsystem.InkBody2
import com.isfam.core.designsystem.InkMuted
import com.isfam.core.designsystem.IsFamButton
import com.isfam.core.designsystem.IsFamScaffold
import com.isfam.core.designsystem.IsFamTheme
import com.isfam.core.designsystem.OtpInput
import com.isfam.core.designsystem.StepProgressBar
import kotlinx.coroutines.delay

/**
 * 07. 휴대폰 OTP 인증
 *
 * ⚠️ 서버는 SMS 를 보내지 않습니다.
 *    인증번호는 "123456" 고정입니다 (auth_fixed_verification_code).
 *    시연 편의를 위해 안내 문구를 넣어두었습니다.
 *
 * UI 키트 실측값
 *   진행바 72% · "2/3"
 *   OTP 칸 height 64 · radius 16 · gap 9
 *   타이머 700 14 앰버 / 재전송 600 13.5 밑줄
 */
private const val OTP_LENGTH = 6
private const val OTP_TIMEOUT_SEC = 180

@Composable
fun OtpRoute(
    phoneNumber: String,
    onVerified: () -> Unit,
    onBack: () -> Unit,
) {
    var code by remember { mutableStateOf("") }
    var remainSec by remember { mutableIntStateOf(OTP_TIMEOUT_SEC) }

    LaunchedEffect(Unit) {
        while (remainSec > 0) {
            delay(1000)
            remainSec -= 1
        }
    }

    // 6자리가 채워지면 자동 확인
    LaunchedEffect(code) {
        if (code.length == OTP_LENGTH) {
            // TODO: POST /api/v1/auth/signup 에 verification_code 로 전달
            delay(300)
            onVerified()
        }
    }

    OtpScreen(
        phoneNumber = phoneNumber,
        code = code,
        remainSec = remainSec,
        onCodeChange = { code = it },
        onResend = { remainSec = OTP_TIMEOUT_SEC },
        onSubmit = onVerified,
        onBack = onBack,
    )
}

@Composable
fun OtpScreen(
    phoneNumber: String,
    code: String,
    remainSec: Int,
    onCodeChange: (String) -> Unit,
    onResend: () -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IsFamScaffold(
        modifier = modifier,
        topBar = {
            StepProgressBar(
                currentStep = 2, totalSteps = 3,
                onBack = onBack, progressOverride = 0.72f,
            )
        },
        bottomBar = {
            IsFamButton(
                text = "확인",
                onClick = onSubmit,
                enabled = code.length == OTP_LENGTH,
            )
        },
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.padding(start = 26.dp, end = 26.dp, top = 26.dp)) {
                Text(
                    "인증번호 6자리를\n입력해 주세요",
                    style = MaterialTheme.typography.headlineLarge,
                    color = Ink,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "${phoneNumber}로 문자를 보냈어요",
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.5.sp),
                    color = InkBody2,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "지금은 데모 단계라 123456 을 입력하면 돼요",
                    style = MaterialTheme.typography.bodySmall,
                    color = Amber500,
                )
            }

            Column(modifier = Modifier.padding(start = 26.dp, end = 26.dp, top = 34.dp)) {
                OtpInput(value = code, onValueChange = onCodeChange, length = OTP_LENGTH)

                Spacer(Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "%02d:%02d 남음".format(remainSec / 60, remainSec % 60),
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                        color = Amber500,
                    )
                    Text(
                        "인증번호 재전송",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
                        color = InkMuted,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier.clickable(onClick = onResend),
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun OtpPreview() = IsFamTheme {
    OtpScreen(
        phoneNumber = "010 1234 5678", code = "4821", remainSec = 151,
        onCodeChange = {}, onResend = {}, onSubmit = {}, onBack = {},
    )
}
