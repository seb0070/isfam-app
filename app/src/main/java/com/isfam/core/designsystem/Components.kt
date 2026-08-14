package com.isfam.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 입력 · 선택 컴포넌트.
 * UI 키트의 카드형 입력 필드, 체크박스, 단계 진행바, OTP 칸을 구현합니다.
 */

// ── 카드 (흰 배경 + 부드러운 그림자) ──────────────────────────

/** 흰 배경 카드. 화면 곳곳에서 쓰입니다. */
@Composable
fun IsFamCard(
    modifier: Modifier = Modifier,
    cornerRadius: androidx.compose.ui.unit.Dp = 16.dp,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .shadow(5.dp, RoundedCornerShape(cornerRadius), clip = false)
            .clip(RoundedCornerShape(cornerRadius))
            .background(White),
    ) { content() }
}

// ── 카드형 입력 필드 ──────────────────────────────────────────

/**
 * 라벨이 안에 들어간 카드형 입력 필드.
 *
 * 포커스되면 앰버 1.5dp 테두리가 생기고 라벨도 앰버로 바뀝니다.
 * UI 키트의 box-shadow: 0 0 0 1.5px #F26A0A 를 border 로 구현했습니다.
 */
@Composable
fun IsFamTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    enabled: Boolean = true,
    /** 입력창 오른쪽 끝에 붙는 요소. 타이머·단위·아이콘 등 */
    trailing: @Composable (() -> Unit)? = null,
) {
    var focused by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(if (focused) 8.dp else 5.dp, RoundedCornerShape(16.dp), clip = false)
            .clip(RoundedCornerShape(16.dp))
            .background(White)
            .then(
                if (focused) Modifier.border(1.5.dp, Amber500, RoundedCornerShape(16.dp))
                else Modifier
            )
            .padding(horizontal = 18.dp, vertical = 13.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (focused) Amber500 else InkMuted2,
                )
                Spacer(Modifier.height(4.dp))

                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    enabled = enabled,
                    singleLine = true,
                    cursorBrush = SolidColor(Amber500),
                    textStyle = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 16.5.sp, fontWeight = FontWeight.Bold, color = Ink,
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                    visualTransformation =
                        if (isPassword) PasswordVisualTransformation('•')
                        else VisualTransformation.None,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { focused = it.isFocused },
                    decorationBox = { inner ->
                        if (value.isEmpty() && placeholder.isNotEmpty()) {
                            Text(
                                placeholder,
                                style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.5.sp),
                                color = InkPlaceholder,
                            )
                        }
                        inner()
                    },
                )
            }

            if (trailing != null) {
                Spacer(Modifier.size(12.dp))
                trailing()
            }
        }
    }
}

/** 인증 제한 시간. 입력창 trailing 에 넣습니다. */
@Composable
fun OtpTimer(remainSec: Int, modifier: Modifier = Modifier) {
    Text(
        text = "%02d:%02d".format(remainSec / 60, remainSec % 60),
        style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
        color = if (remainSec > 30) Amber500 else Danger,
        modifier = modifier,
    )
}

// ── 체크박스 ──────────────────────────────────────────────────

/** 라운드 사각 체크박스. 크기와 강조 정도를 조절할 수 있습니다. */
@Composable
fun IsFamCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 20.dp,
    strong: Boolean = true,
) {
    val bg = when {
        checked && strong -> Amber500
        checked -> Tint50
        else -> CheckboxOffBg
    }
    val fg = when {
        checked && strong -> White
        checked -> Amber500
        else -> InkPlaceholder
    }
    val radius = (size.value * 0.32f).dp

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(radius))
            .background(bg)
            .clickable { onCheckedChange(!checked) },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "✓",
            color = fg,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = (size.value * 0.55f).sp,
                fontWeight = FontWeight.ExtraBold,
            ),
        )
    }
}

/** 체크박스 + 라벨 한 줄. 오른쪽에 "보기" 같은 보조 액션을 붙일 수 있습니다. */
@Composable
fun CheckboxRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    strong: Boolean = false,
    checkboxSize: androidx.compose.ui.unit.Dp = 18.dp,
    labelBold: Boolean = false,
    trailingText: String? = null,
    onTrailingClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IsFamCheckbox(checked, onCheckedChange, size = checkboxSize, strong = strong)
        Spacer(Modifier.size(10.dp))
        Text(
            label,
            style = if (labelBold) MaterialTheme.typography.titleMedium.copy(fontSize = 14.5.sp)
            else MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
            color = if (labelBold) Ink else InkBody,
            modifier = Modifier.weight(1f),
        )
        if (trailingText != null) {
            Text(
                trailingText,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                color = InkPlaceholder,
                modifier = Modifier.clickable(enabled = onTrailingClick != null) {
                    onTrailingClick?.invoke()
                },
            )
        }
    }
}

// ── 단계 진행바 ───────────────────────────────────────────────

/** ← / 진행바 / "1/3" 한 줄. 회원가입·OTP 화면 상단입니다. */
@Composable
fun StepProgressBar(
    currentStep: Int,
    totalSteps: Int,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    progressOverride: Float? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 26.dp, end = 26.dp, top = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (onBack != null) {
            Text(
                "←",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 22.sp, fontWeight = FontWeight.Bold,
                ),
                color = Ink,
                modifier = Modifier.clickable(onClick = onBack),
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(ProgressTrack),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progressOverride ?: (currentStep.toFloat() / totalSteps))
                    .fillMaxSize()
                    .background(Amber500),
            )
        }

        Text(
            "$currentStep/$totalSteps",
            style = MaterialTheme.typography.bodySmall,
            color = InkMuted2,
        )
    }
}

// ── OTP 입력 ──────────────────────────────────────────────────

/**
 * 6자리 OTP 칸.
 *
 * 실제 입력은 화면 전체를 덮는 투명 BasicTextField 가 받고,
 * 칸은 그 값을 보여주기만 합니다. 칸마다 필드를 두면 포커스 이동이
 * 지저분해지고 자동완성(SMS Retriever)도 붙이기 어렵습니다.
 */
@Composable
fun OtpInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    length: Int = 6,
) {
    BasicTextField(
        value = value,
        onValueChange = { if (it.length <= length && it.all(Char::isDigit)) onValueChange(it) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        modifier = modifier.fillMaxWidth(),
        decorationBox = {
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                repeat(length) { index ->
                    val char = value.getOrNull(index)
                    val isCursor = index == value.length
                    OtpBox(char = char, focused = isCursor, modifier = Modifier.weight(1f))
                }
            }
        },
    )
}

@Composable
private fun OtpBox(
    char: Char?,
    focused: Boolean,
    modifier: Modifier = Modifier,
) {
    val filled = char != null
    Box(
        modifier = modifier
            .height(64.dp)
            .then(
                if (filled || focused)
                    Modifier.shadow(5.dp, RoundedCornerShape(16.dp), clip = false)
                else Modifier
            )
            .clip(RoundedCornerShape(16.dp))
            .background(if (filled || focused) White else OtpEmptyBg)
            .then(
                if (focused) Modifier.border(1.5.dp, Amber500, RoundedCornerShape(16.dp))
                else Modifier
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (char != null) {
            Text(
                char.toString(),
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 24.sp),
                color = Ink,
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════
//  순차 노출 (Progressive Disclosure) 지원
// ══════════════════════════════════════════════════════════════

/**
 * 단계가 진행되면 아래에서 위로 부드럽게 나타나는 영역.
 *
 * 새 입력칸은 항상 화면 아래쪽(키보드 바로 위)에 추가됩니다.
 * 위로 쌓으면 키보드에 가려집니다.
 */
@Composable
fun RevealSection(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    androidx.compose.animation.AnimatedVisibility(
        visible = visible,
        enter = androidx.compose.animation.expandVertically(
            animationSpec = androidx.compose.animation.core.tween(260),
        ) + androidx.compose.animation.fadeIn(
            animationSpec = androidx.compose.animation.core.tween(260, delayMillis = 80),
        ),
        exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut(),
        modifier = modifier,
    ) { content() }
}

/**
 * 입력이 끝나 확정된 항목.
 * 입력칸을 그대로 두면 화면이 길어지므로 한 줄로 접습니다.
 */
@Composable
fun CompletedRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    verified: Boolean = false,
    onEdit: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(White)
            .padding(horizontal = 18.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = InkMuted2)
            Spacer(Modifier.height(3.dp))
            Text(
                value,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
                color = Ink,
            )
        }

        if (verified) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "✓",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 13.sp, fontWeight = FontWeight.ExtraBold,
                    ),
                    color = Safe,
                )
                Spacer(Modifier.size(4.dp))
                Text(
                    "인증 완료",
                    style = MaterialTheme.typography.bodySmall,
                    color = Safe,
                )
            }
        } else if (onEdit != null) {
            Text(
                "수정",
                style = MaterialTheme.typography.bodySmall,
                color = InkPlaceholder,
                modifier = Modifier.clickable(onClick = onEdit),
            )
        }
    }
}

/** 입력 아래 작은 안내·오류 문구 */
@Composable
fun FieldHelper(
    text: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = if (isError) Danger else InkMuted,
        modifier = modifier.padding(start = 6.dp, top = 6.dp),
    )
}