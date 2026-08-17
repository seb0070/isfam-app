package com.isfam.feature.onboarding

import android.app.Activity
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.isfam.core.designsystem.Amber500
import com.isfam.core.designsystem.CardIllustEnd
import com.isfam.core.designsystem.CardIllustStart
import com.isfam.core.designsystem.Ink
import com.isfam.core.designsystem.InkBody2
import com.isfam.core.designsystem.InkMuted
import com.isfam.core.designsystem.IsFamButton
import com.isfam.core.designsystem.IsFamScaffold
import com.isfam.core.designsystem.IsFamSecondaryButton
import com.isfam.core.designsystem.IsFamTextButton
import com.isfam.core.designsystem.IsFamTheme
import com.isfam.core.designsystem.Safe
import com.isfam.core.designsystem.SafeBadgeBg
import com.isfam.core.designsystem.SafeBadgeFg
import com.isfam.core.designsystem.ScreenBg
import com.isfam.core.designsystem.StepProgressBar
import com.isfam.core.designsystem.Tint50
import com.isfam.core.designsystem.White
import com.isfam.core.permission.PermissionChecker
import com.isfam.core.permission.SettingsIntents

/**
 * 08-b. 마지막 설정 안내
 *
 * OS 권한(08번) 다음에 옵니다. 두 항목 모두 다른 앱으로 나갔다 와야 해서
 * 한 화면에 묶었습니다.
 *
 *   ① 통화 자동 녹음 — 삼성 전화 앱 안에 있음. 필수
 *   ② 배터리 최적화 해제 — 시스템 다이얼로그. 권장
 *
 * ⚠️ 자동 녹음은 앱이 확인할 수 없습니다.
 *    삼성 전화 앱의 설정이라 읽기·제어가 불가능하며(실기기 검증 완료),
 *    첫 통화 후 녹음 파일이 생겨야만 실제 동작 여부를 알 수 있습니다.
 *    그래서 CTA 가 "완료"가 아니라 "설정했어요"입니다.
 */
@Composable
fun CallRecordingSetupRoute(
    onNext: (callRecordingEnabled: Boolean) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val activity = context as Activity
    val checker = remember { PermissionChecker(context) }

    var batteryIgnored by remember { mutableStateOf(checker.isBatteryOptimizationIgnored()) }
    var markedRecordingOn by remember { mutableStateOf(false) }

    // 설정 앱·전화 앱에 다녀오면 상태가 바뀌었을 수 있습니다
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                batteryIgnored = checker.isBatteryOptimizationIgnored()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    CallRecordingSetupScreen(
        isSamsung = SettingsIntents.isSamsungDevice(),
        hasDialer = SettingsIntents.hasSamsungDialer(context),
        recordingMarked = markedRecordingOn,
        batteryIgnored = batteryIgnored,
        onOpenDialer = {
            markedRecordingOn = true
            SettingsIntents.safeStart(context, SettingsIntents.dialerApp(context))
        },
        onRequestBattery = {
            SettingsIntents.safeStart(
                context,
                SettingsIntents.batteryOptimization(context),
                SettingsIntents.batteryOptimizationFallback(),
            )
        },
        onNext = { onNext(markedRecordingOn) },
        onSkip = { onNext(false) },
        onBack = onBack,
    )
}

@Composable
fun CallRecordingSetupScreen(
    isSamsung: Boolean,
    hasDialer: Boolean,
    recordingMarked: Boolean,
    batteryIgnored: Boolean,
    onOpenDialer: () -> Unit,
    onRequestBattery: () -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IsFamScaffold(
        modifier = modifier,
        topBar = {
            StepProgressBar(
                currentStep = 3, totalSteps = 3,
                onBack = onBack, progressOverride = 0.95f,
            )
        },
        bottomBar = {
            IsFamButton(
                text = if (recordingMarked) "설정했어요, 계속하기" else "계속하기",
                onClick = onNext,
            )
            IsFamTextButton(text = "나중에 설정할게요", onClick = onSkip)
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 26.dp),
        ) {
            Spacer(Modifier.height(20.dp))

            Text(
                "마지막으로\n두 가지만 설정해 주세요",
                style = MaterialTheme.typography.headlineLarge.copy(lineHeight = 37.sp),
                color = Ink,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "앱에서 대신 켤 수 없는 설정이라 직접 해주셔야 해요.",
                style = MaterialTheme.typography.bodyMedium,
                color = InkMuted,
            )

            Spacer(Modifier.height(24.dp))

            if (!isSamsung || !hasDialer) {
                UnsupportedNotice()
                Spacer(Modifier.height(14.dp))
            }

            // ① 자동 통화녹음
            SetupCard(
                order = "1",
                title = "통화 자동 녹음 켜기",
                required = true,
                done = recordingMarked,
                doneLabel = "설정함",
            ) {
                Text(
                    "IsFam은 삼성 전화 앱이 저장한 녹음 파일을 읽어 분석해요. " +
                        "이 설정이 꺼져 있으면 파일이 만들어지지 않아 보호가 시작되지 않습니다.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.sp, lineHeight = 21.sp,
                    ),
                    color = InkBody2,
                )

                Spacer(Modifier.height(14.dp))

                StepGuide()

                Spacer(Modifier.height(14.dp))

                IsFamSecondaryButton(
                    text = if (recordingMarked) "전화 앱 다시 열기" else "전화 앱 열기",
                    onClick = onOpenDialer,
                )
            }

            Spacer(Modifier.height(12.dp))

            // ② 배터리 최적화
            SetupCard(
                order = "2",
                title = "배터리 최적화 해제",
                required = false,
                done = batteryIgnored,
                doneLabel = "해제됨",
            ) {
                Text(
                    "절전 기능이 앱을 멈추면 통화를 놓칠 수 있어요. " +
                        "해제해두면 오랫동안 앱을 열지 않아도 계속 지켜드립니다.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.sp, lineHeight = 21.sp,
                    ),
                    color = InkBody2,
                )

                if (!batteryIgnored) {
                    Spacer(Modifier.height(14.dp))
                    IsFamSecondaryButton(text = "해제하기", onClick = onRequestBattery)
                }
            }

            Spacer(Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Tint50)
                    .padding(16.dp),
            ) {
                Text(
                    "설정이 제대로 됐는지는 첫 통화가 끝난 뒤 자동으로 확인해 드려요.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.5.sp, lineHeight = 20.sp,
                    ),
                    color = Amber500,
                )
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

// ── 단계 안내 ─────────────────────────────────────────────────

/**
 * 전화 앱 안에서의 경로.
 * 해당 화면으로 직접 보내는 딥링크가 없어 단계로 안내합니다.
 */
@Composable
private fun StepGuide() {
    val steps = listOf(
        "전화 앱에서 오른쪽 위 ⋮ 누르기",
        "설정 → 통화 녹음",
        "통화 자동 녹음 켜기",
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(ScreenBg)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        steps.forEachIndexed { index, step ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier.size(20.dp).clip(CircleShape).background(Amber500),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "${index + 1}",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = White,
                    )
                }
                Text(
                    step,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                    color = Ink,
                )
            }
        }

        // TODO: 실기기 스크린샷을 넣으면 고령 사용자에게 훨씬 효과적입니다.
        //       res/drawable/guide_call_recording.png
    }
}

// ── 카드 ──────────────────────────────────────────────────────

@Composable
private fun SetupCard(
    order: String,
    title: String,
    required: Boolean,
    done: Boolean,
    doneLabel: String,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(5.dp, RoundedCornerShape(22.dp), clip = false)
            .clip(RoundedCornerShape(22.dp))
            .background(White)
            .padding(18.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(
                        if (done) Brush.linearGradient(listOf(Safe, Safe))
                        else Brush.linearGradient(listOf(CardIllustStart, CardIllustEnd))
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (done) "✓" else order,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = if (done) White else Amber500,
                )
            }

            Text(
                title,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                color = Ink,
                modifier = Modifier.weight(1f),
            )

            if (done) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(SafeBadgeBg)
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                ) {
                    Text(
                        doneLabel,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                        color = SafeBadgeFg,
                    )
                }
            } else {
                Text(
                    if (required) "필수" else "권장",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                    color = if (required) Amber500 else InkMuted,
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        content()
    }
}

@Composable
private fun UnsupportedNotice() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFFDECEC))
            .padding(16.dp),
    ) {
        Text(
            "이 기기에서는 통화 자동 녹음을 지원하지 않을 수 있어요. " +
                "삼성 갤럭시에서 가장 잘 동작합니다.",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 13.sp, lineHeight = 21.sp,
            ),
            color = Color(0xFFA83C36),
        )
    }
}

// ── Preview ───────────────────────────────────────────────────

@Preview(name = "기본", showBackground = true, widthDp = 390, heightDp = 950)
@Composable
private fun SetupPreview() = IsFamTheme {
    CallRecordingSetupScreen(
        isSamsung = true, hasDialer = true,
        recordingMarked = false, batteryIgnored = false,
        onOpenDialer = {}, onRequestBattery = {},
        onNext = {}, onSkip = {}, onBack = {},
    )
}

@Preview(name = "설정 완료", showBackground = true, widthDp = 390, heightDp = 950)
@Composable
private fun SetupDonePreview() = IsFamTheme {
    CallRecordingSetupScreen(
        isSamsung = true, hasDialer = true,
        recordingMarked = true, batteryIgnored = true,
        onOpenDialer = {}, onRequestBattery = {},
        onNext = {}, onSkip = {}, onBack = {},
    )
}

@Preview(name = "미지원 기기", showBackground = true, widthDp = 390, heightDp = 950)
@Composable
private fun SetupUnsupportedPreview() = IsFamTheme {
    CallRecordingSetupScreen(
        isSamsung = false, hasDialer = false,
        recordingMarked = false, batteryIgnored = false,
        onOpenDialer = {}, onRequestBattery = {},
        onNext = {}, onSkip = {}, onBack = {},
    )
}
