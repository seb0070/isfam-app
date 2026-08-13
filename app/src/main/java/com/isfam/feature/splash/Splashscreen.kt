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

@Composable
fun SplashRoute(
    onLoggedIn: () -> Unit,
    onNeedLogin: () -> Unit,
) {
    var doneCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        delay(600); doneCount = 1   // TODO: 서버 연결 확인 (GET /health)
        delay(600); doneCount = 2   // TODO: 보안 세션 준비
        delay(600); doneCount = 3   // TODO: 로그인 상태 확인 (GET /auth/me)

        delay(300)
        val isLoggedIn = false      // TODO: TokenStore 결과로 교체
        if (isLoggedIn) onLoggedIn() else onNeedLogin()
    }

    SplashScreen(doneCount = doneCount)
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