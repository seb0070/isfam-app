package com.isfam.feature.splash

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.isfam.core.designsystem.Amber600
import com.isfam.core.rememberAppContainer
import com.isfam.core.designsystem.IsFamTheme
import com.isfam.core.designsystem.Mascot
import com.isfam.core.designsystem.MascotImage
import com.isfam.core.designsystem.SplashBottom
import com.isfam.core.designsystem.SplashMid
import com.isfam.core.designsystem.SplashTop
import com.isfam.core.designsystem.White
import kotlinx.coroutines.delay

/**
 * 01. 스플래시
 *
 * UI 키트 실측값
 *   배경 linear-gradient(170deg, #FFD873, #F98F16 62%, #EF6A05)
 *   마스코트 200×200 · radius 64 · 흰색 20% 배경 · 42% 테두리 · 여백 10
 *   로고 800 40px, tracking -3%
 *   부제 600 15px/1.6, 흰색 90%
 *   하단 padding 0 26 34, 항목 간격 9
 */
enum class BootStep(val label: String) {
    ServerCheck("서버 연결 확인"),
    SecureSession("보안 세션 준비"),
    AuthCheck("로그인 상태 확인"),
}

/**
 * 진입 지점 판단.
 *
 * 온보딩을 어디까지 마쳤는지에 따라 갈라집니다.
 * 이 판단을 여기서 한 번에 하지 않으면 각 화면이 스스로 확인하게 되고,
 * 화면이 떴다가 곧바로 튕기는 깜빡임이 생깁니다.
 */
@Composable
fun SplashRoute(
    onReady: (SplashDestination) -> Unit,
) {
    val container = rememberAppContainer()
    var doneCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        // ① 서버 연결 확인
        val serverAlive = container.api.runCatching { health() }.isSuccess
        doneCount = 1
        delay(200)

        // ② 로그인 상태
        val loggedIn = container.authRepository.hasSession()
        doneCount = 2
        delay(200)

        if (!loggedIn) {
            doneCount = 3
            delay(300)
            onReady(SplashDestination.Onboarding)
            return@LaunchedEffect
        }

        // ③ 목소리 등록 · 가족 참여 여부
        //
        // 서버가 죽어 있으면 확인할 수 없습니다. 그때는 홈으로 보냅니다.
        // 온보딩을 처음부터 다시 시키는 것보다 낫습니다.
        val destination = if (!serverAlive) {
            SplashDestination.Home
        } else {
            val voiceRegistered = container.voiceprintRepository
                .getStatus().getOrNull()?.registered == true
            val hasFamily = container.familyRepository.getFamily().isSuccess

            when {
                !voiceRegistered -> SplashDestination.VoiceEnrollment
                !hasFamily -> SplashDestination.FamilySetup
                else -> SplashDestination.Home
            }
        }

        doneCount = 3
        delay(300)
        onReady(destination)
    }

    SplashScreen(doneCount = doneCount)
}

/** 스플래시 이후 어디로 갈지 */
enum class SplashDestination {
    /** 로그인 전 */
    Onboarding,
    /** 계정은 있으나 목소리 미등록 */
    VoiceEnrollment,
    /** 목소리는 등록했으나 가족 공간 없음 */
    FamilySetup,
    Home,
}

@Composable
fun SplashScreen(
    doneCount: Int,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize().background(
            Brush.linearGradient(
                colorStops = arrayOf(
                    0f to SplashTop,
                    0.62f to SplashMid,
                    1f to SplashBottom,
                ),
                start = androidx.compose.ui.geometry.Offset.Zero,
                end = androidx.compose.ui.geometry.Offset(200f, 2000f),
            )
        ),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .background(White.copy(alpha = 0.20f), RoundedCornerShape(64.dp))
                    .border(1.dp, White.copy(alpha = 0.42f), RoundedCornerShape(64.dp))
                    .padding(10.dp),
                contentAlignment = Alignment.Center,
            ) {
                MascotImage(
                    mascot = Mascot.Watching,
                    size = 180.dp,
                    cornerRadius = 54.dp,
                    background = androidx.compose.ui.graphics.Color.Transparent,
                )
            }

            Spacer(Modifier.height(22.dp))

            Text("IsFam", style = MaterialTheme.typography.displayLarge, color = White)

            Spacer(Modifier.height(14.dp))

            Text(
                "가족의 목소리를 기억하는\nAI 보이스피싱 보호",
                style = MaterialTheme.typography.bodyLarge,
                color = White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = 26.dp, end = 26.dp, bottom = 34.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                BootStep.entries.forEachIndexed { index, step ->
                    BootStepRow(label = step.label, done = index < doneCount)
                }
            }

            Text(
                "안전하게 준비하고 있어요",
                style = MaterialTheme.typography.bodySmall,
                color = White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun BootStepRow(label: String, done: Boolean) {
    val alpha by animateFloatAsState(
        targetValue = if (done) 1f else 0.5f,
        animationSpec = tween(300),
        label = "bootStepAlpha",
    )

    Row(
        modifier = Modifier.fillMaxWidth().alpha(alpha),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Box(
            modifier = Modifier.size(16.dp).background(
                if (done) White else White.copy(alpha = 0.35f), CircleShape
            ),
            contentAlignment = Alignment.Center,
        ) {
            if (done) {
                Text(
                    "✓",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                    color = Amber600,
                )
            }
        }
        Text(
            label,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
            color = White.copy(alpha = 0.95f),
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun SplashScreenStartPreview() = IsFamTheme { SplashScreen(doneCount = 0) }

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun SplashScreenDonePreview() = IsFamTheme { SplashScreen(doneCount = 3) }