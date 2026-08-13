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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * IsFam 테마.
 * UI 키트 HTML 의 실제 스타일 값을 옮겼습니다.
 *   화면 배경 #FAF7F2 · 버튼 radius 18 · 카드 radius 24 · 일러스트 radius 26
 */
private val IsFamColorScheme = lightColorScheme(
    primary = Amber500,
    onPrimary = White,
    primaryContainer = Tint50,
    onPrimaryContainer = Amber700,
    secondary = Honey300,
    onSecondary = Ink,
    background = ScreenBg,
    onBackground = Ink,
    surface = White,
    onSurface = Ink,
    surfaceVariant = Tint50,
    onSurfaceVariant = InkMuted,
    outline = Divider,
    error = Danger,
    onError = White,
    errorContainer = DangerBadgeBg,
    onErrorContainer = DangerBadgeFg,
)

private val IsFamShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),   // 버튼
    large = RoundedCornerShape(24.dp),    // 카드
    extraLarge = RoundedCornerShape(26.dp),
)

@Composable
fun IsFamTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = IsFamColorScheme,
        typography = IsFamTypography,
        shapes = IsFamShapes,
        content = content,
    )
}

// ══════════════════════════════════════════════════════════════
//  버튼 4종
//  UI 키트 "컴포넌트" 카드의 Primary / Secondary / Outline / Disabled
// ══════════════════════════════════════════════════════════════

/** 그라데이션 배경이라 Material Button 대신 Box 로 만듭니다 */
@Composable
private fun BaseButton(
    text: String,
    onClick: () -> Unit,
    background: Brush,
    contentColor: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: androidx.compose.ui.unit.Dp = 56.dp,
    elevation: androidx.compose.ui.unit.Dp = 0.dp,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .then(
                if (elevation > 0.dp && enabled)
                    Modifier.shadow(elevation, RoundedCornerShape(18.dp), clip = false)
                else Modifier
            )
            .clip(RoundedCornerShape(18.dp))
            .background(background)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = contentColor)
    }
}

/** Primary — 앰버 그라데이션. 높이 56 */
@Composable
fun IsFamButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    if (enabled) {
        BaseButton(
            text = text, onClick = onClick, modifier = modifier,
            background = Brush.linearGradient(listOf(Honey400, Amber500)),
            contentColor = White,
            elevation = 10.dp,
        )
    } else {
        IsFamDisabledButton(text, modifier)
    }
}

/** 온보딩·주요 진행 버튼 — 잉크 블랙. 높이 56 */
@Composable
fun IsFamDarkButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    if (enabled) {
        BaseButton(
            text = text, onClick = onClick, modifier = modifier,
            background = Brush.linearGradient(listOf(Ink, Ink)),
            contentColor = White,
        )
    } else {
        IsFamDisabledButton(text, modifier)
    }
}

/** Secondary — 연한 틴트 배경. 높이 52 */
@Composable
fun IsFamSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    BaseButton(
        text = text, onClick = onClick, modifier = modifier, enabled = enabled,
        background = Brush.linearGradient(listOf(Tint50, Tint50)),
        contentColor = Amber700,
        height = 52.dp,
    )
}

/** Outline — 흰 배경 + 그림자. 높이 52 */
@Composable
fun IsFamOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    BaseButton(
        text = text, onClick = onClick, modifier = modifier, enabled = enabled,
        background = Brush.linearGradient(listOf(White, White)),
        contentColor = Ink,
        height = 52.dp,
        elevation = 6.dp,
    )
}

@Composable
private fun IsFamDisabledButton(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(DisabledBg),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = DisabledFg)
    }
}

/** "나중에 초대하고 홈으로" 같은 보조 텍스트 액션 */
@Composable
fun IsFamTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxWidth().height(48.dp).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = InkMuted)
    }
}

// ── 판별 결과 배지 (pill, radius 999) ─────────────────────────

enum class RiskLevel(val label: String, val fg: Color, val bg: Color) {
    SAFE("안전", SafeBadgeFg, SafeBadgeBg),
    CAUTION("확인 필요", CautionBadgeFg, CautionBadgeBg),
    DANGER("위험", DangerBadgeFg, DangerBadgeBg),
    INSUFFICIENT("판정 보류", InkMuted, DisabledBg),
}

@Composable
fun RiskBadge(level: RiskLevel, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(level.bg)
            .padding(horizontal = 12.dp, vertical = 7.dp),
    ) {
        Text(level.label, color = level.fg, style = MaterialTheme.typography.bodySmall)
    }
}

/** 앰버 pill 라벨 — "Flow A 시작" 같은 태그 */
@Composable
fun AmberPill(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.clip(CircleShape).background(Tint50)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(text, color = Amber500, style = MaterialTheme.typography.bodySmall)
    }
}

// ── 레이아웃 ──────────────────────────────────────────────────

/** 상단 바 — ← / 제목 / "3 / 3 단계" */
@Composable
fun IsFamTopBar(
    title: String? = null,
    step: String? = null,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로", tint = Ink)
            }
        } else {
            Spacer(Modifier.size(48.dp))
        }

        Text(
            text = title.orEmpty(),
            style = MaterialTheme.typography.titleMedium,
            color = Ink,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )

        Box(Modifier.width(72.dp).padding(end = 10.dp), contentAlignment = Alignment.CenterEnd) {
            if (step != null) {
                Text(step, style = MaterialTheme.typography.bodySmall, color = InkMuted2)
            }
        }
    }
}

/**
 * 본문 + 하단 고정 CTA.
 * 화면 좌우 여백은 26dp 가 기본입니다 (UI 키트 기준).
 */
@Composable
fun IsFamScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable (() -> Unit)? = null,
    bottomBar: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier.fillMaxSize().background(ScreenBg)) {
        topBar?.invoke()
        Box(Modifier.weight(1f)) { content() }
        if (bottomBar != null) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(start = 26.dp, end = 26.dp, bottom = 34.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) { bottomBar() }
        }
    }
}

/** 아이브로우 + 제목 + 본문. 온보딩과 대부분의 화면이 이 구조입니다. */
@Composable
fun ScreenHeadline(
    title: String,
    eyebrow: String? = null,
    body: String? = null,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (eyebrow != null) {
            Text(eyebrow, style = MaterialTheme.typography.labelMedium, color = Amber500)
            Spacer(Modifier.height(10.dp))
        }
        Text(title, style = MaterialTheme.typography.headlineMedium, color = Ink)
        if (body != null) {
            Spacer(Modifier.height(14.dp))
            Text(body, style = MaterialTheme.typography.bodyLarge, color = InkBody2)
        }
    }
}