package com.isfam.feature.splash

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.isfam.core.designsystem.Amber500
import com.isfam.core.designsystem.Honey300
import com.isfam.core.designsystem.Ink
import com.isfam.core.designsystem.IsFamTheme
import com.isfam.core.designsystem.White
import kotlinx.coroutines.delay

/**
 * 01. 스플래시
 *
 * 준비 단계 3개를 순서대로 체크하며 보여줍니다.
 *   서버 연결 확인 → 보안 세션 준비 → 로그인 상태 확인
 *
 * 실제로는 이 시간에 세션 확인과 단말 등록을 수행하고,
 * 결과에 따라 로그인 화면 또는 홈으로 분기합니다.
 */

/** 스플래시가 수행하는 준비 단계 */
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
    // 완료된 단계 수 (0~3)
    var doneCount by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        // TODO: 실제 서버 연결 확인으로 교체
        delay(600); doneCount = 1
        // TODO: 실제 보안 세션 준비로 교체
        delay(600); doneCount = 2
        // TODO: 저장된 토큰으로 로그인 상태 확인 (GET /auth/me)
        delay(600); doneCount = 3

        delay(300)
        val isLoggedIn = false   // TODO: TokenStore 확인 결과로 교체
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
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Honey300, Amber500))),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // TODO: 미어켓 마스코트 200px 로 교체 (UI 키트 마스코트 가이드라인)
            Box(
                modifier = Modifier.size(120.dp).background(White.copy(alpha = 0.22f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text("🦡", style = MaterialTheme.typography.displayMedium)
            }

            Spacer(Modifier.height(28.dp))

            Text(
                "IsFam",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = White,
            )

            Spacer(Modifier.height(12.dp))

            Text(
                "가족의 목소리를 기억하는\nAI 보이스피싱 보호",
                style = MaterialTheme.typography.bodyLarge,
                color = White.copy(alpha = 0.92f),
                textAlign = TextAlign.Center,
            )
        }

        // 하단 준비 단계 표시
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 40.dp, vertical = 48.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BootStep.entries.forEachIndexed { index, step ->
                BootStepRow(label = step.label, done = index < doneCount)
            }

            Spacer(Modifier.height(8.dp))

            Text(
                "안전하게 준비하고 있어요",
                style = MaterialTheme.typography.bodySmall,
                color = White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun BootStepRow(label: String, done: Boolean) {
    val alpha by animateFloatAsState(
        targetValue = if (done) 1f else 0.45f,
        animationSpec = tween(300),
        label = "bootStepAlpha",
    )

    Row(
        modifier = Modifier.fillMaxWidth().alpha(alpha),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(20.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (done) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = White,
                    modifier = Modifier.size(18.dp),
                )
            } else {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    color = White,
                )
            }
        }
        Spacer(Modifier.size(10.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = White)
    }
}

// ── Preview ───────────────────────────────────────────────────

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun SplashScreenStartPreview() {
    IsFamTheme { SplashScreen(doneCount = 0) }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun SplashScreenMidPreview() {
    IsFamTheme { SplashScreen(doneCount = 2) }
}