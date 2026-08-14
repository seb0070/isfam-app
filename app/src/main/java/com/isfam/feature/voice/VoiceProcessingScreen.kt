package com.isfam.feature.voice

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.isfam.core.designsystem.Amber500
import com.isfam.core.designsystem.CardIllustStart
import com.isfam.core.designsystem.DisabledBg
import com.isfam.core.designsystem.Honey300
import com.isfam.core.designsystem.IndicatorOff
import com.isfam.core.designsystem.Ink
import com.isfam.core.designsystem.InkFaint
import com.isfam.core.designsystem.InkMuted
import com.isfam.core.designsystem.InkPlaceholder
import com.isfam.core.designsystem.IsFamScaffold
import com.isfam.core.designsystem.IsFamTheme
import com.isfam.core.designsystem.Mascot
import com.isfam.core.designsystem.MascotImage
import com.isfam.core.designsystem.ProcessingEnd
import com.isfam.core.designsystem.Safe
import com.isfam.core.designsystem.White
import kotlinx.coroutines.delay

/**
 * 11. 성문 생성 중
 *
 * UI 키트 실측값
 *   마스코트 172 원형 · gradient(160deg #FFF6E4→#FFE3BE) · padding 14
 *   제목 800 23 · 부제 500 13.5/1.6
 *   진행바 height 10 · radius 6 · 배경 #EFE7DA
 *   퍼센트 800 13 앰버
 *   단계 카드 radius 22 · padding 18 · gap 13
 *     완료 22원 #5AA97A ✓ / 진행중 border 2.5 앰버(pulse) / 대기 border #E4D9C9
 */
enum class ProcessingStep(val label: String) {
    QualityCheck("음성 품질 검사"),
    NoiseCleanup("노이즈 정리"),
    Voiceprint("성문(Voiceprint) 생성"),
    SecureStore("보안 저장"),
}

@Composable
fun VoiceProcessingRoute(onComplete: () -> Unit) {
    var doneCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        // TODO: POST /api/v1/family/register 업로드 진행률과 연동
        delay(900); doneCount = 1
        delay(900); doneCount = 2
        delay(1400); doneCount = 3
        delay(700); doneCount = 4
        delay(400)
        onComplete()
    }

    VoiceProcessingScreen(doneCount = doneCount)
}

@Composable
fun VoiceProcessingScreen(
    doneCount: Int,
    modifier: Modifier = Modifier,
) {
    val progress = doneCount / ProcessingStep.entries.size.toFloat()
    val animated by animateFloatAsState(progress, tween(400), label = "progress")

    IsFamScaffold(
        modifier = modifier,
        bottomBar = {
            Text(
                "앱을 종료하지 말고 잠시 기다려 주세요",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                color = InkFaint,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(172.dp)
                    .shadow(10.dp, CircleShape, clip = false)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(CardIllustStart, ProcessingEnd))
                    )
                    .padding(14.dp),
                contentAlignment = Alignment.Center,
            ) {
                MascotImage(
                    mascot = Mascot.Analyzing,
                    size = 144.dp,
                    cornerRadius = 72.dp,
                    background = Color.Transparent,
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(
                "성문을 만들고 있어요",
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 23.sp),
                color = Ink,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "잠시만 기다려 주세요. 보통 10초 이내에 끝나요.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.5.sp, lineHeight = 22.sp,
                ),
                color = InkMuted,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(DisabledBg),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animated)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Brush.horizontalGradient(listOf(Honey300, Amber500))),
                )
            }
            Spacer(Modifier.height(9.dp))
            Text(
                "${(animated * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                color = Amber500,
            )

            Spacer(Modifier.height(24.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(5.dp, RoundedCornerShape(22.dp), clip = false)
                    .clip(RoundedCornerShape(22.dp))
                    .background(White)
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(13.dp),
            ) {
                ProcessingStep.entries.forEachIndexed { index, step ->
                    StepRow(
                        label = step.label,
                        done = index < doneCount,
                        active = index == doneCount,
                    )
                }
            }
        }
    }
}

@Composable
private fun StepRow(label: String, done: Boolean, active: Boolean) {
    val pulse = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by pulse.animateFloat(
        initialValue = 1f, targetValue = 0.35f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "pulseAlpha",
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .then(if (active) Modifier.alpha(pulseAlpha) else Modifier)
                .clip(CircleShape)
                .background(if (done) Safe else Color.Transparent)
                .then(
                    when {
                        done -> Modifier
                        active -> Modifier.border(2.5.dp, Amber500, CircleShape)
                        else -> Modifier.border(2.5.dp, IndicatorOff, CircleShape)
                    }
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (done) {
                Text(
                    "✓",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = White,
                )
            }
        }

        Text(
            label,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
            color = when {
                done -> Ink
                active -> Amber500
                else -> InkPlaceholder
            },
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun VoiceProcessingPreview() = IsFamTheme {
    VoiceProcessingScreen(doneCount = 2)
}
