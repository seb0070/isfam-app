package com.isfam.feature.voice

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.isfam.core.designsystem.CardIllustEnd
import com.isfam.core.designsystem.CardIllustStart
import com.isfam.core.designsystem.Ink
import com.isfam.core.designsystem.InkMuted
import com.isfam.core.designsystem.IsFamButton
import com.isfam.core.designsystem.IsFamScaffold
import com.isfam.core.designsystem.IsFamTheme
import com.isfam.core.designsystem.Mascot
import com.isfam.core.designsystem.MascotImage
import com.isfam.core.designsystem.StepProgressBar
import com.isfam.core.designsystem.White

/**
 * 09. 목소리 등록 안내
 *
 * UI 키트 실측값
 *   진행바 66% · "2 / 3 단계"
 *   일러스트 카드 height 196 · radius 24 · gradient(160deg #FFF6E4→#FFEBD2) · padding 14
 *   내부 원 168 · 흰 배경 · inset shadow · padding 12
 *   제목 800 24/1.4 · 부제 500 13.5/1.6
 *   정보 카드 radius 20 · padding 15/16 · gap 9
 *   좌우 여백 22 (다른 화면의 26 과 다릅니다)
 */
private data class InfoItem(val title: String, val body: String)

private val infoItems = listOf(
    InfoItem("문장 3개, 약 20초", "조용한 곳에서 평소 통화하듯 읽어주세요."),
    InfoItem("원본은 저장하지 않아요", "분석 후 즉시 삭제하고 성문만 암호화 보관합니다."),
    InfoItem("가족 모두가 등록해야 해요", "서로의 목소리를 알아야 서로를 지킬 수 있어요."),
)

@Composable
fun VoiceIntroRoute(
    onStart: () -> Unit,
    onBack: () -> Unit,
) {
    VoiceIntroScreen(onStart = onStart, onBack = onBack)
}

@Composable
fun VoiceIntroScreen(
    onStart: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IsFamScaffold(
        modifier = modifier,
        topBar = {
            StepProgressBar(
                currentStep = 2, totalSteps = 3,
                onBack = onBack, progressOverride = 0.66f,
            )
        },
        bottomBar = {
            IsFamButton(text = "녹음 시작하기", onClick = onStart)
        },
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 22.dp)) {
            Spacer(Modifier.height(18.dp))

            // 일러스트 카드
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(196.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(listOf(CardIllustStart, CardIllustEnd))
                    )
                    .padding(14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(168.dp)
                        .clip(CircleShape)
                        .background(White)
                        .padding(12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    MascotImage(
                        mascot = Mascot.Listening,
                        size = 140.dp,
                        cornerRadius = 70.dp,
                        background = Color.Transparent,
                    )
                }
            }

            Spacer(Modifier.height(22.dp))

            Text(
                "내 목소리를 등록하면\n가족이 나를 알아볼 수 있어요",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 24.sp, lineHeight = 34.sp,
                ),
                color = Ink,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "등록된 성문은 의심 통화를 판별하는 기준이 됩니다.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.5.sp, lineHeight = 22.sp,
                ),
                color = InkMuted,
            )

            Spacer(Modifier.height(18.dp))

            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                infoItems.forEach { InfoCard(it) }
            }
        }
    }
}

@Composable
private fun InfoCard(item: InfoItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(5.dp, RoundedCornerShape(20.dp), clip = false)
            .clip(RoundedCornerShape(20.dp))
            .background(White)
            .padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        Column {
            Text(
                item.title,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.5.sp),
                color = Ink,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                item.body,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.5.sp, lineHeight = 19.sp,
                ),
                color = InkMuted,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun VoiceIntroPreview() = IsFamTheme {
    VoiceIntroScreen(onStart = {}, onBack = {})
}