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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.em
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

// ══════════════════════════════════════════════════════════════
//  가족 · 초대 관련 컴포넌트
// ══════════════════════════════════════════════════════════════

/**
 * 선택 카드. 13번 가족 공간 진입 선택에 사용합니다.
 * 강조된 카드는 앰버 1.5dp 테두리가 붙습니다.
 */
@Composable
fun SelectionCard(
    title: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    eyebrow: String? = null,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(if (highlighted) 8.dp else 5.dp, RoundedCornerShape(24.dp), clip = false)
            .clip(RoundedCornerShape(24.dp))
            .background(White)
            .then(
                if (highlighted) Modifier.border(1.5.dp, Amber500, RoundedCornerShape(24.dp))
                else Modifier
            )
            .clickable(onClick = onClick)
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                if (eyebrow != null) {
                    Text(eyebrow, style = MaterialTheme.typography.labelMedium, color = Amber500)
                    Spacer(Modifier.height(7.dp))
                }
                Text(
                    title,
                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = 18.sp),
                    color = Ink,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    description,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.sp, lineHeight = 21.sp,
                    ),
                    color = InkBody2,
                )
            }
            Spacer(Modifier.size(10.dp))
            Text(
                "›",
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
                color = if (highlighted) Amber500 else InkPlaceholder,
            )
        }
    }
}

/** 코드·링크를 보여주고 복사할 수 있는 행 */
@Composable
fun CopyableRow(
    label: String,
    value: String,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier,
    valueLarge: Boolean = false,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(if (valueLarge) 16.dp else 14.dp))
            .background(ScreenBg)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            if (valueLarge) {
                Text(
                    label,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = LabelBrown,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    value,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 21.sp, letterSpacing = 0.08.em,
                    ),
                    color = Ink,
                )
            } else {
                Text(
                    value,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 13.sp),
                    color = Ink,
                )
            }
        }
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(DisabledBg)
                .clickable(onClick = onCopy)
                .padding(horizontal = 12.dp, vertical = if (valueLarge) 8.dp else 7.dp),
        ) {
            Text(
                "복사",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = InkBody2,
            )
        }
    }
}

/** 카카오톡 공유 버튼 */
@Composable
fun KakaoShareButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .shadow(8.dp, RoundedCornerShape(16.dp), clip = false)
            .clip(RoundedCornerShape(16.dp))
            .background(KakaoYellow)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Box(
                modifier = Modifier.size(20.dp).clip(RoundedCornerShape(7.dp)).background(Ink)
            )
            Text(text, style = MaterialTheme.typography.labelLarge, color = Ink)
        }
    }
}

/** 이니셜 아바타. 등록 완료는 앰버, 미등록은 점선 테두리 */
@Composable
fun InitialAvatar(
    initial: String,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 64.dp,
    registered: Boolean = true,
) {
    val radius = (size.value * 0.34f).dp
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(radius))
            .then(
                if (registered)
                    Modifier.background(Brush.linearGradient(listOf(Honey300, Amber500)))
                else
                    Modifier.background(White).border(1.dp, DashedBorder, RoundedCornerShape(radius))
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            initial,
            style = MaterialTheme.typography.headlineMedium.copy(fontSize = (size.value * 0.34f).sp),
            color = if (registered) White else InkPlaceholder,
        )
    }
}

/** 라벨 : 값 한 줄. 정보 카드 안에 반복됩니다. */
@Composable
fun InfoRow(
    label: String,
    modifier: Modifier = Modifier,
    value: String? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
            color = InkMuted,
        )
        if (value != null) {
            Text(
                value,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                color = Ink,
            )
        }
        trailing?.invoke()
    }
}

/**
 * 초대 코드 입력. 영숫자 6자리를 3-3 으로 나눠 표시합니다.
 *   F 7 K – 2 M 9
 *
 * OtpInput 과 달리 대문자 영문도 받습니다.
 */
@Composable
fun InviteCodeInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    BasicTextField(
        value = value,
        onValueChange = { raw ->
            val cleaned = raw.uppercase().filter { it.isLetterOrDigit() }.take(6)
            onValueChange(cleaned)
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Characters,
        ),
        modifier = modifier.fillMaxWidth(),
        decorationBox = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(6) { index ->
                    CodeBox(
                        char = value.getOrNull(index),
                        focused = index == value.length,
                        modifier = Modifier.weight(1f),
                    )
                    if (index == 2) {
                        Text(
                            "–",
                            style = MaterialTheme.typography.headlineMedium.copy(fontSize = 22.sp),
                            color = InkPlaceholder,
                            modifier = Modifier.padding(horizontal = 2.dp),
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun CodeBox(
    char: Char?,
    focused: Boolean,
    modifier: Modifier = Modifier,
) {
    val filled = char != null
    Box(
        modifier = modifier
            .height(62.dp)
            .then(
                if (filled || focused)
                    Modifier.shadow(5.dp, RoundedCornerShape(16.dp), clip = false)
                else Modifier
            )
            .clip(RoundedCornerShape(16.dp))
            .background(if (filled || focused) White else CodeEmptyBg)
            .then(
                if (focused) Modifier.border(2.dp, Amber500, RoundedCornerShape(16.dp))
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

/**
 * QR 코드 자리.
 *
 * 실제 QR 생성에는 ZXing 이 필요합니다.
 *   libs.versions.toml:  zxing = { group = "com.google.zxing", name = "core", version = "3.5.3" }
 *   app/build.gradle.kts: implementation(libs.zxing)
 *
 * 의존성을 추가한 뒤 아래 주석의 generateQrBitmap 을 사용하세요.
 */
@Composable
fun QrCodePlaceholder(
    content: String,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 196.dp,
) {
    // val bitmap = remember(content, size) { generateQrBitmap(content, size) }
    // Image(bitmap.asImageBitmap(), contentDescription = "초대 QR",
    //       modifier = modifier.size(size))

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(ScreenBg),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("QR", style = MaterialTheme.typography.headlineMedium, color = InkPlaceholder)
            Spacer(Modifier.height(4.dp))
            Text(
                content,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                color = InkPlaceholder,
            )
        }
    }
}

/*
 * ZXing 을 추가한 뒤 사용할 QR 생성 함수
 *
 * private fun generateQrBitmap(content: String, size: Dp): Bitmap {
 *     val px = size.value.toInt() * 3
 *     val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, px, px)
 *     val bmp = Bitmap.createBitmap(px, px, Bitmap.Config.RGB_565)
 *     for (x in 0 until px) for (y in 0 until px) {
 *         bmp.setPixel(x, y, if (matrix[x, y]) android.graphics.Color.BLACK
 *                            else android.graphics.Color.WHITE)
 *     }
 *     return bmp
 * }
 */

// ══════════════════════════════════════════════════════════════
//  토글 · 토스트
// ══════════════════════════════════════════════════════════════

/**
 * 스위치 토글.
 * UI 키트 실측값 — 46×28 · radius 15 · 노브 22 · 여백 3
 */
@Composable
fun IsFamToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val offset by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (checked) 21.dp else 3.dp,
        animationSpec = androidx.compose.animation.core.tween(180),
        label = "toggleKnob",
    )

    Box(
        modifier = modifier
            .size(width = 46.dp, height = 28.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(if (checked) Amber500 else ToggleOff)
            .clickable { onCheckedChange(!checked) },
    ) {
        Box(
            modifier = Modifier
                .padding(start = offset, top = 3.dp)
                .size(22.dp)
                .clip(CircleShape)
                .background(White),
        )
    }
}

/**
 * 다크 토스트.
 * UI 키트 실측값 — rgba(23,19,15,.94) · radius 18 · padding 15/18
 */
@Composable
fun IsFamToast(
    message: String,
    modifier: Modifier = Modifier,
    iconText: String = "!",
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(14.dp, RoundedCornerShape(18.dp), clip = false)
            .clip(RoundedCornerShape(18.dp))
            .background(Ink.copy(alpha = 0.94f))
            .padding(horizontal = 18.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        Box(
            modifier = Modifier.size(22.dp).clip(CircleShape).background(Honey300),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                iconText,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = Ink,
            )
        }
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 13.sp, lineHeight = 20.sp,
            ),
            color = White,
        )
    }
}

/** 목소리 등록 상태 배지 */
@Composable
fun RegistrationBadge(
    registered: Boolean,
    modifier: Modifier = Modifier,
) {
    if (registered) {
        Box(
            modifier = modifier
                .clip(CircleShape)
                .background(SafeBadgeBg)
                .padding(horizontal = 11.dp, vertical = 6.dp),
        ) {
            Text(
                "등록 완료",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = SafeBadgeFg,
            )
        }
    } else {
        Box(
            modifier = modifier
                .clip(CircleShape)
                .background(Amber500)
                .padding(horizontal = 11.dp, vertical = 7.dp),
        ) {
            Text(
                "알림",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = White,
            )
        }
    }
}

/** 이름 옆 역할 태그 — "관리자", "나" */
@Composable
fun RoleTag(
    text: String,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (highlighted) Tint50 else RowDivider)
            .padding(horizontal = 7.dp, vertical = 3.dp),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
            color = if (highlighted) Amber700 else InkBody2,
        )
    }
}