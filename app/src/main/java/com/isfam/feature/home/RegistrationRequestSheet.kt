package com.isfam.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.isfam.core.designsystem.Amber700
import com.isfam.core.designsystem.DashedAvatarBg
import com.isfam.core.designsystem.DashedAvatarBorder
import com.isfam.core.designsystem.Ink
import com.isfam.core.designsystem.InkBody2
import com.isfam.core.designsystem.InkMuted
import com.isfam.core.designsystem.InkPlaceholder
import com.isfam.core.designsystem.IsFamSecondaryButton
import com.isfam.core.designsystem.IsFamTextButton
import com.isfam.core.designsystem.IsFamTheme
import com.isfam.core.designsystem.KakaoShareButton
import com.isfam.core.designsystem.ScreenBg

/**
 * 20. 등록 요청 바텀시트
 *
 * 홈에서 미등록 가족을 탭하면 올라옵니다.
 *
 * UI 키트 실측값
 *   카카오 버튼 #FFE300 · height 54 · radius 18
 *   앰버 버튼 #FFF1DE · height 52 · 텍스트 #C05C05
 *   닫기 흰 배경 · height 50
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrationRequestSheet(
    member: MemberStatus,
    onKakaoRequest: () -> Unit,
    onPushRequest: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = ScreenBg,
        dragHandle = null,
    ) {
        RegistrationRequestContent(
            member = member,
            onKakaoRequest = onKakaoRequest,
            onPushRequest = onPushRequest,
            onDismiss = onDismiss,
        )
    }
}

@Composable
fun RegistrationRequestContent(
    member: MemberStatus,
    onKakaoRequest: () -> Unit,
    onPushRequest: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 22.dp, end = 22.dp, top = 24.dp, bottom = 34.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(DashedAvatarBg)
                .border(1.dp, DashedAvatarBorder, RoundedCornerShape(22.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "?",
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 22.sp),
                color = InkPlaceholder,
            )
        }

        Spacer(Modifier.height(16.dp))

        Text(
            "${member.name}님은\n아직 목소리를 등록하지 않았어요",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = 20.sp, lineHeight = 29.sp,
            ),
            color = Ink,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "등록해야 ${member.name}님을 사칭하는 통화를\n걸러낼 수 있어요.",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 13.sp, lineHeight = 21.sp,
            ),
            color = InkBody2,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(18.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            KakaoShareButton(text = "카카오톡으로 등록 요청", onClick = onKakaoRequest)
            IsFamSecondaryButton(text = "앱 알림으로 요청 보내기", onClick = onPushRequest)
            IsFamTextButton(text = "닫기", onClick = onDismiss)
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 480)
@Composable
private fun RegistrationRequestPreview() = IsFamTheme {
    RegistrationRequestContent(
        member = MemberStatus(4, "김도현", "?", "아들", registered = false),
        onKakaoRequest = {}, onPushRequest = {}, onDismiss = {},
    )
}
