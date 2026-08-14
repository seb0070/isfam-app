package com.isfam.core.designsystem

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 하단 탭 4개.
 *
 * UI 키트 실측값
 *   아이콘 26 · radius 9
 *   활성 gradient(150deg #FFB020→#EF6A05) · 라벨 700 10.5 앰버
 *   비활성 gradient(150deg #E8E0D3→#D3C8B8) · 라벨 600 10.5 #A2968A
 *   패딩 11 / 26 / 26 · 상단 경계선
 */
enum class MainTab(val label: String) {
    Home("홈"),
    Family("가족"),
    History("기록"),
    Settings("설정"),
}

@Composable
fun IsFamBottomNav(
    current: MainTab,
    onTabSelected: (MainTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().background(ScreenBg)) {
        HorizontalDivider(color = Divider.copy(alpha = 0.5f))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 26.dp, end = 26.dp, top = 11.dp, bottom = 26.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            MainTab.entries.forEach { tab ->
                TabItem(
                    tab = tab,
                    selected = tab == current,
                    onClick = { onTabSelected(tab) },
                )
            }
        }
    }
}

@Composable
private fun TabItem(tab: MainTab, selected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick).padding(horizontal = 12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(
                    if (selected) Brush.linearGradient(listOf(Honey400, Amber600))
                    else Brush.linearGradient(listOf(TabInactiveStart, TabInactiveEnd))
                ),
        )
        Spacer(Modifier.height(5.dp))
        Text(
            tab.label,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
            color = if (selected) Amber500 else InkFaint,
        )
    }
}

/**
 * 하단 탭이 있는 화면의 공통 레이아웃.
 * 홈 · 가족 · 기록 · 설정 네 화면이 사용합니다.
 */
@Composable
fun MainTabScaffold(
    currentTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier.fillMaxSize().background(ScreenBg)) {
        Box(Modifier.weight(1f)) { content() }
        IsFamBottomNav(current = currentTab, onTabSelected = onTabSelected)
    }
}
