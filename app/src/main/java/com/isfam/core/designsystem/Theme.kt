package com.isfam.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * 앱 테마.
 *
 * 다크 모드는 3일 스프린트 범위 밖입니다. 라이트 전용으로 고정합니다.
 * 나중에 darkColorScheme 을 추가하면 됩니다.
 */
private val IsFamColorScheme = lightColorScheme(
    primary = IsFamBlue,
    onPrimary = Neutral0,
    primaryContainer = IsFamBlueLight,
    onPrimaryContainer = IsFamBlueDark,
    background = Neutral0,
    onBackground = Neutral900,
    surface = Neutral0,
    onSurface = Neutral900,
    surfaceVariant = Neutral100,
    onSurfaceVariant = Neutral600,
    outline = Neutral200,
    error = RiskDanger,
    onError = Neutral0,
    errorContainer = RiskDangerBg,
)

@Composable
fun IsFamTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = IsFamColorScheme,
        typography = IsFamTypography,
        content = content,
    )
}

// ══════════════════════════════════════════════════════════════
//  공통 컴포넌트
//
//  화면마다 Button 을 직접 스타일링하지 마세요.
//  33개 화면에서 버튼 높이가 제각각이 되면 수습이 안 됩니다.
// ══════════════════════════════════════════════════════════════

/**
 * 기본 버튼.
 * 최소 높이 56dp — 고령 사용자 터치 정확도를 위해 Material 기본(40dp)보다 큽니다.
 */
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
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(vertical = 16.dp, horizontal = 24.dp),
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
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(vertical = 16.dp, horizontal = 24.dp),
        modifier = modifier.fillMaxWidth().heightIn(min = 56.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

/**
 * 위험도 배지.
 *
 * 색만으로 구분하지 않고 항상 텍스트를 함께 표시합니다.
 * 색각 이상 사용자도 판단할 수 있어야 합니다.
 */
enum class RiskLevel(
    val label: String,
    val fg: Color,
    val bg: Color,
) {
    SAFE("안전", RiskSafe, RiskSafeBg),
    CAUTION("확인 필요", RiskCaution, RiskCautionBg),
    DANGER("위험", RiskDanger, RiskDangerBg),
    INSUFFICIENT("판정 보류", RiskUnknown, RiskUnknownBg),
}

@Composable
fun RiskBadge(level: RiskLevel, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(level.bg, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            text = level.label,
            color = level.fg,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

/**
 * 화면 하단 고정 버튼 영역을 가진 표준 레이아웃.
 * 온보딩 화면 대부분이 이 형태입니다.
 */
@Composable
fun IsFamScaffold(
    modifier: Modifier = Modifier,
    bottomBar: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) { content() }
        bottomBar?.let {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) { it() }
        }
    }
}

@Composable
fun LabeledRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Neutral600)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}
