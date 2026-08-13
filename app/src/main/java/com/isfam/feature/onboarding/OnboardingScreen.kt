package com.isfam.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.isfam.core.designsystem.Amber500
import com.isfam.core.designsystem.IllustBgEnd
import com.isfam.core.designsystem.IllustBgStart
import com.isfam.core.designsystem.IndicatorOff
import com.isfam.core.designsystem.Ink
import com.isfam.core.designsystem.InkBody2
import com.isfam.core.designsystem.InkMuted2
import com.isfam.core.designsystem.IsFamDarkButton
import com.isfam.core.designsystem.IsFamScaffold
import com.isfam.core.designsystem.IsFamTheme
import com.isfam.core.designsystem.Mascot
import com.isfam.core.designsystem.MascotImage
import kotlinx.coroutines.launch

/**
 * 02·03·04. 온보딩 3단
 *
 * UI 키트 실측값
 *   일러스트 카드 height 330 · radius 26 · radial(#FFF1DE → #F7F2E9) · padding 18
 *   아이브로우 700 12px #F26A0A · 제목 800 26/1.35 · 본문 500 15/1.7 #6E655C
 *   인디케이터 활성 24×8 radius4 #F26A0A / 비활성 8×8 원 #E4D9C9 · gap 6
 *   버튼 height 56 · radius 18 · #17130F
 */
private data class OnboardingPage(
    val mascot: Mascot,
    val eyebrow: String,
    val title: String,
    val body: String,
)

private val pages = listOf(
    OnboardingPage(
        mascot = Mascot.Analyzing,
        eyebrow = "앱을 켜두지 않아도",
        title = "통화가 끝나면\n알아서 검사해요",
        body = "IsFam은 백그라운드에서 통화 음성을 분석합니다. " +
                "매번 앱을 열거나 녹음할 필요가 없어요.",
    ),
    OnboardingPage(
        mascot = Mascot.Listening,
        eyebrow = "20초 만에 완료",
        title = "가족의 목소리를\n한 번만 등록하면 끝",
        body = "문장 3개를 읽으면 성문이 만들어집니다. " +
                "원본 음성은 저장하지 않고 기기 안에서만 비교해요.",
    ),
    OnboardingPage(
        mascot = Mascot.Watching,
        eyebrow = "위험할 땐 즉시",
        title = "가족 모두에게\n바로 알려드려요",
        body = "보이스피싱이 의심되면 가족 전체에게 긴급 알림을 보내 " +
                "같은 수법에 당하지 않게 지켜줍니다.",
    ),
)

@Composable
fun OnboardingRoute(onFinish: () -> Unit) {
    OnboardingScreen(onFinish = onFinish, onSkip = onFinish)
}

@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
    initialPage: Int = 0,
) {
    val pagerState = rememberPagerState(initialPage = initialPage) { pages.size }
    val scope = rememberCoroutineScope()
    val isLast = pagerState.currentPage == pages.lastIndex

    IsFamScaffold(
        modifier = modifier,
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 26.dp, end = 26.dp, top = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "${pagerState.currentPage + 1} / ${pages.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkMuted2,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "건너뛰기",
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkMuted2,
                    modifier = Modifier.clickable(onClick = onSkip).padding(8.dp),
                )
            }
        },
        bottomBar = {
            PageIndicator(pages.size, pagerState.currentPage)
            Spacer(Modifier.height(12.dp))
            IsFamDarkButton(
                text = if (isLast) "시작하기" else "다음",
                onClick = {
                    if (isLast) onFinish()
                    else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                },
            )
        },
    ) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { index ->
            OnboardingPageContent(pages[index])
        }
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    Column(modifier = Modifier.fillMaxSize()) {
        // 일러스트 카드
        Box(
            modifier = Modifier
                .padding(start = 26.dp, end = 26.dp, top = 18.dp)
                .fillMaxWidth()
                .height(330.dp)
                .shadow(6.dp, RoundedCornerShape(26.dp), clip = false)
                .clip(RoundedCornerShape(26.dp))
                .background(
                    Brush.radialGradient(
                        colors = listOf(IllustBgStart, IllustBgEnd),
                        center = Offset.Unspecified,
                    )
                )
                .padding(18.dp),
            contentAlignment = Alignment.Center,
        ) {
            MascotImage(
                mascot = page.mascot,
                size = 280.dp,
                cornerRadius = 20.dp,
                background = androidx.compose.ui.graphics.Color.Transparent,
            )
        }

        Column(modifier = Modifier.padding(start = 26.dp, end = 26.dp, top = 28.dp)) {
            Text(page.eyebrow, style = MaterialTheme.typography.labelMedium, color = Amber500)
            Spacer(Modifier.height(10.dp))
            Text(page.title, style = MaterialTheme.typography.headlineMedium, color = Ink)
            Spacer(Modifier.height(14.dp))
            Text(page.body, style = MaterialTheme.typography.bodyLarge, color = InkBody2)
        }
    }
}

@Composable
private fun PageIndicator(pageCount: Int, currentPage: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(pageCount) { index ->
            val selected = index == currentPage
            Box(
                modifier = Modifier
                    .size(width = if (selected) 24.dp else 8.dp, height = 8.dp)
                    .clip(if (selected) RoundedCornerShape(4.dp) else CircleShape)
                    .background(if (selected) Amber500 else IndicatorOff),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun OnboardingP1Preview() =
    IsFamTheme { OnboardingScreen(onFinish = {}, onSkip = {}, initialPage = 0) }

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun OnboardingP2Preview() =
    IsFamTheme { OnboardingScreen(onFinish = {}, onSkip = {}, initialPage = 1) }

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun OnboardingP3Preview() =
    IsFamTheme { OnboardingScreen(onFinish = {}, onSkip = {}, initialPage = 2) }