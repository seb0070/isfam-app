package com.isfam.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Shapes
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * IsFam 테마.
 *
 * UI 키트의 디자인 방향을 반영했습니다.
 *   한 화면 한 목적 · 큰 타이포 · 라운드 16~26 · 하단 고정 CTA
 *
 * 다크 모드는 범위 밖입니다. 라이트 전용.
 */
private val IsFamColorScheme = lightColorScheme(
    primary = Amber500,
    onPrimary = White,
    primaryContainer = Tint50,
    onPrimaryContainer = Amber700,
    secondary = Honey300,
    onSecondary = Ink,
    background = White,
    onBackground = Ink,
    surface = White,
    onSurface = Ink,
    surfaceVariant = Ink50,
    onSurfaceVariant = Ink500,
    outline = Ink100,
    error = Danger,
    onError = White,
    errorContainer = DangerBg,
    onErrorContainer = Danger,
)

/** 라운드 16~26 규칙 */
private val IsFamShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
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
//  공통 컴포넌트
//  화면마다 Button 을 직접 스타일링하지 마세요.
// ══════════════════════════════════════════════════════════════

/** 하단 고정 CTA용 기본 버튼 */
@Composable
fun IsFamButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Amber500,
            contentColor = White,
            disabledContainerColor = Ink100,
            disabledContentColor = Ink300,
        ),
        contentPadding = PaddingValues(vertical = 16.dp),
        modifier = modifier.fillMaxWidth().heightIn(min = 56.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun IsFamOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        modifier = modifier.fillMaxWidth().heightIn(min = 56.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = Ink700)
    }
}

/** "나중에 초대하고 홈으로" 같은 보조 액션 */
@Composable
fun IsFamTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TextButton(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = Ink500)
    }
}

// ── 판별 결과 3단 ─────────────────────────────────────────────

enum class RiskLevel(val label: String, val fg: Color, val bg: Color) {
    SAFE("안전", Safe, SafeBg),
    CAUTION("확인 필요", Caution, CautionBg),
    DANGER("위험", Danger, DangerBg),
    INSUFFICIENT("판정 보류", Ink500, Ink50),
}

@Composable
fun RiskBadge(level: RiskLevel, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(level.bg, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(level.label, color = level.fg, style = MaterialTheme.typography.bodySmall)
    }
}

// ── 레이아웃 ──────────────────────────────────────────────────

/**
 * 상단 바. 뒤로가기 · 제목 · 단계 표시를 한 줄에 배치합니다.
 * 예: ← / 권한 허용 / 3 / 3 단계
 */
@Composable
fun IsFamTopBar(
    title: String? = null,
    step: String? = null,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(horizontal = 8.dp),
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

        Box(
            modifier = Modifier.width(64.dp).padding(end = 8.dp),
            contentAlignment = Alignment.CenterEnd,
        ) {
            if (step != null) {
                Text(step, style = MaterialTheme.typography.bodySmall, color = Ink500)
            }
        }
    }
}

/**
 * 본문 + 하단 고정 CTA 레이아웃.
 *
 * bottomBar 에 버튼을 여러 개 넣으면 세로로 8dp 간격으로 쌓입니다.
 *   bottomBar = {
 *       IsFamButton("다음", onNext)
 *       IsFamTextButton("건너뛰기", onSkip)
 *   }
 */
@Composable
fun IsFamScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable (() -> Unit)? = null,
    bottomBar: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier.fillMaxSize().background(White)) {
        topBar?.invoke()

        Box(Modifier.weight(1f)) { content() }

        if (bottomBar != null) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                bottomBar()
            }
        }
    }
}

/** 화면 제목 + 부제 블록. 대부분의 화면 상단에 반복됩니다. */
@Composable
fun ScreenHeadline(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.headlineLarge, color = Ink)
        if (subtitle != null) {
            Spacer(Modifier.height(10.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Ink500)
        }
    }
}