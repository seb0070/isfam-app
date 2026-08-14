package com.isfam.feature.family

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.isfam.core.designsystem.Ink
import com.isfam.core.designsystem.InkBody2
import com.isfam.core.designsystem.IsFamScaffold
import com.isfam.core.designsystem.IsFamTheme
import com.isfam.core.designsystem.Mascot
import com.isfam.core.designsystem.MascotImage
import com.isfam.core.designsystem.SelectionCard
import com.isfam.core.designsystem.Tint50

/**
 * 13. 가족 공간 진입 선택
 *
 * UI 키트 실측값
 *   마스코트 132 · radius 42 · #FFF1DE · padding 8 (내부 radius 34)
 *   제목 800 27/1.4 · 부제 500 14.5/1.65 #6E655C
 *   카드 radius 24 · padding 20 · gap 12
 *   첫 카드만 앰버 강조 (권장 경로)
 */
@Composable
fun FamilyEntryRoute(
    onCreate: () -> Unit,
    onJoin: () -> Unit,
) {
    FamilyEntryScreen(onCreate = onCreate, onJoin = onJoin)
}

@Composable
fun FamilyEntryScreen(
    onCreate: () -> Unit,
    onJoin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IsFamScaffold(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(26.dp))

            Box(
                modifier = Modifier
                    .size(132.dp)
                    .clip(RoundedCornerShape(42.dp))
                    .background(Tint50)
                    .padding(8.dp),
                contentAlignment = Alignment.Center,
            ) {
                MascotImage(
                    mascot = Mascot.Safe,
                    size = 116.dp,
                    cornerRadius = 34.dp,
                    background = Color.Transparent,
                )
            }

            Spacer(Modifier.height(22.dp))

            Text(
                "IsFam에\n오신 것을 환영해요!",
                style = MaterialTheme.typography.headlineLarge.copy(lineHeight = 38.sp),
                color = Ink,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "목소리 등록이 끝났어요. 어떻게 시작할까요?",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 14.5.sp, lineHeight = 24.sp,
                ),
                color = InkBody2,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(30.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SelectionCard(
                    title = "새로운 가족 공간 만들기",
                    description = "공간 이름을 정하고\nQR · 링크를 카카오톡으로 공유해요.",
                    onClick = onCreate,
                    highlighted = true,
                )
                SelectionCard(
                    title = "초대 코드로 참여하기",
                    description = "6자리 코드를 입력하면\n기존 가족 공간에 바로 연결돼요.",
                    onClick = onJoin,
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun FamilyEntryPreview() = IsFamTheme {
    FamilyEntryScreen(onCreate = {}, onJoin = {})
}
