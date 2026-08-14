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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.isfam.core.designsystem.Amber500
import com.isfam.core.designsystem.DividerLight
import com.isfam.core.designsystem.Honey300
import com.isfam.core.designsystem.Ink
import com.isfam.core.designsystem.InkBody2
import com.isfam.core.designsystem.InkMuted
import com.isfam.core.designsystem.IsFamButton
import com.isfam.core.designsystem.IsFamScaffold
import com.isfam.core.designsystem.IsFamTheme
import com.isfam.core.designsystem.Safe
import com.isfam.core.designsystem.White

/**
 * 12. 등록 완료
 *
 * UI 키트 실측값
 *   ✓ 아이콘 120 · radius 40 · gradient(140deg #FFC53D→#F26A0A) · 흰 ✓ 52px
 *   제목 800 26/1.4 · 부제 500 15/1.65 #6E655C
 *   카드 radius 22 · padding 20 · gap 14
 */
@Composable
fun VoiceCompleteRoute(
    displayName: String,
    onNext: () -> Unit,
) {
    VoiceCompleteScreen(displayName = displayName, onNext = onNext)
}

@Composable
fun VoiceCompleteScreen(
    displayName: String,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IsFamScaffold(
        modifier = modifier,
        bottomBar = {
            IsFamButton(text = "다음 단계로 (가족 공간 연결)", onClick = onNext)
        },
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .shadow(12.dp, RoundedCornerShape(40.dp), clip = false)
                    .clip(RoundedCornerShape(40.dp))
                    .background(Brush.linearGradient(listOf(Honey300, Amber500))),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "✓",
                    style = MaterialTheme.typography.displayMedium.copy(fontSize = 52.sp),
                    color = White,
                )
            }

            Spacer(Modifier.height(26.dp))

            Text(
                "${displayName}님의\n목소리 등록 완료!",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 26.sp, lineHeight = 36.sp,
                ),
                color = Ink,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "이제 가족을 보호할 준비가 됐어요",
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                color = InkBody2,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(26.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(5.dp, RoundedCornerShape(22.dp), clip = false)
                    .clip(RoundedCornerShape(22.dp))
                    .background(White)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "목소리 등록",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                        color = InkMuted,
                    )
                    Text(
                        "등록 완료",
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                        color = Safe,
                    )
                }

                HorizontalDivider(color = DividerLight)

                Text(
                    "${displayName}님의 목소리가 안전하게\n암호화 보관되었습니다",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.sp, lineHeight = 21.sp,
                    ),
                    color = InkBody2,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun VoiceCompletePreview() = IsFamTheme {
    VoiceCompleteScreen(displayName = "서연", onNext = {})
}
