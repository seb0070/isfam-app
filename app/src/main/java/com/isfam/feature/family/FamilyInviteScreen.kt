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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.isfam.core.designsystem.CopyableRow
import com.isfam.core.designsystem.Ink
import com.isfam.core.designsystem.InkBody
import com.isfam.core.designsystem.InkFaint
import com.isfam.core.designsystem.InkMuted
import com.isfam.core.designsystem.IsFamScaffold
import com.isfam.core.designsystem.IsFamTextButton
import com.isfam.core.designsystem.IsFamTheme
import com.isfam.core.designsystem.IsFamTopBar
import com.isfam.core.designsystem.KakaoShareButton
import com.isfam.core.designsystem.LabelBrown
import com.isfam.core.designsystem.Mascot
import com.isfam.core.designsystem.MascotImage
import com.isfam.core.designsystem.QrCodeImage
import com.isfam.core.designsystem.StepProgressBar
import com.isfam.core.designsystem.Tint50
import com.isfam.core.designsystem.White

/**
 * 15. 가족 초대 (온보딩 직후)  ·  16. 가족 초대 (가족 관리 재진입)
 *
 * 두 화면은 구조가 같고 세 가지만 다릅니다.
 *   15: 진행바 100% "2/2" · QR 196 · "나중에 초대하고 홈으로"
 *   16: 진행바 없음        · QR 142 · 안내 문구가 다름
 *
 * UI 키트 실측값
 *   QR 카드 radius 26 · padding 24 · gap 16
 *   코드 박스 radius 16 · inset shadow · 코드 800 21 letterSpacing .08em
 *   카카오 버튼 #FFE300 · height 52 · radius 16
 */
@Composable
fun FamilyInviteRoute(
    inviteCode: String,
    qrCodeUrl: String?,
    expiresInText: String,
    onSkip: (() -> Unit)? = null,
    onBack: () -> Unit,
    isReentry: Boolean = false,
) {
    val clipboard = LocalClipboardManager.current
    val link = "isfam.app/join/$inviteCode"

    FamilyInviteScreen(
        inviteCode = inviteCode,
        inviteLink = link,
        qrCodeUrl = qrCodeUrl,
        expiresInText = expiresInText,
        isReentry = isReentry,
        onCopyCode = { clipboard.setText(AnnotatedString(inviteCode)) },
        onCopyLink = { clipboard.setText(AnnotatedString("https://$link")) },
        // TODO: 카카오 SDK 연동
        onKakaoShare = { clipboard.setText(AnnotatedString("https://$link")) },
        onSkip = onSkip,
        onBack = onBack,
    )
}

@Composable
fun FamilyInviteScreen(
    inviteCode: String,
    inviteLink: String,
    qrCodeUrl: String?,
    expiresInText: String,
    isReentry: Boolean,
    onCopyCode: () -> Unit,
    onCopyLink: () -> Unit,
    onKakaoShare: () -> Unit,
    onSkip: (() -> Unit)?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IsFamScaffold(
        modifier = modifier,
        topBar = {
            if (isReentry) {
                IsFamTopBar(title = "가족 초대", onBack = onBack)
            } else {
                StepProgressBar(
                    currentStep = 2, totalSteps = 2,
                    onBack = onBack, progressOverride = 1f,
                )
            }
        },
        bottomBar = {
            if (onSkip != null) {
                IsFamTextButton(text = "나중에 초대하고 홈으로", onClick = onSkip)
            }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp),
        ) {
            Spacer(Modifier.height(18.dp))

            // 안내 행
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Tint50)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    MascotImage(
                        mascot = Mascot.Safe,
                        size = 48.dp,
                        cornerRadius = 24.dp,
                        background = Color.Transparent,
                    )
                }
                Text(
                    if (isReentry) "아직 연결되지 않은 가족에게\n초대를 다시 보낼 수 있어요."
                    else "초대받은 가족은 목소리 등록만 하면\n바로 연결돼요.",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 14.sp, lineHeight = 22.sp,
                    ),
                    color = InkBody,
                )
            }

            Spacer(Modifier.height(16.dp))

            // QR + 코드 카드
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(5.dp, RoundedCornerShape(if (isReentry) 24.dp else 26.dp), clip = false)
                    .clip(RoundedCornerShape(if (isReentry) 24.dp else 26.dp))
                    .background(White)
                    .padding(if (isReentry) 16.dp else 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(if (isReentry) 12.dp else 16.dp),
            ) {
                QrCodeImage(
                    qrCodeUrl = qrCodeUrl,
                    size = if (isReentry) 142.dp else 196.dp,
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "QR을 카메라로 비추면 바로 참여",
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 13.sp),
                        color = Ink,
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        expiresInText,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                        color = InkFaint,
                        textAlign = TextAlign.Center,
                    )
                }

                CopyableRow(
                    label = "초대 코드",
                    value = formatCode(inviteCode),
                    onCopy = onCopyCode,
                    valueLarge = true,
                )
            }

            Spacer(Modifier.height(10.dp))

            // 링크 카드
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(5.dp, RoundedCornerShape(24.dp), clip = false)
                    .clip(RoundedCornerShape(24.dp))
                    .background(White)
                    .padding(16.dp),
            ) {
                Text(
                    "링크로 초대",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = LabelBrown,
                    modifier = Modifier.padding(start = 2.dp, bottom = 10.dp),
                )
                CopyableRow(label = "", value = inviteLink, onCopy = onCopyLink)
                Spacer(Modifier.height(10.dp))
                KakaoShareButton(text = "카카오톡으로 초대하기", onClick = onKakaoShare)
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

/** F7K2M9 → F7K–2M9 */
private fun formatCode(code: String): String =
    if (code.length == 6) "${code.take(3)}–${code.drop(3)}" else code

@Preview(name = "15 온보딩", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun FamilyInvitePreview() = IsFamTheme {
    FamilyInviteScreen(
        inviteCode = "AB12CD", inviteLink = "isfam.app/i/AB12CD", qrCodeUrl = null,
        expiresInText = "유효시간 23시간 58분 남음", isReentry = false,
        onCopyCode = {}, onCopyLink = {}, onKakaoShare = {}, onSkip = {}, onBack = {},
    )
}

@Preview(name = "16 재진입", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun FamilyInviteReentryPreview() = IsFamTheme {
    FamilyInviteScreen(
        inviteCode = "AB12CD", inviteLink = "isfam.app/i/AB12CD", qrCodeUrl = null,
        expiresInText = "유효시간 23시간 58분 남음", isReentry = true,
        onCopyCode = {}, onCopyLink = {}, onKakaoShare = {}, onSkip = null, onBack = {},
    )
}