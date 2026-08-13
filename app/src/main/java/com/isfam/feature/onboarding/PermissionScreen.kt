package com.isfam.feature.onboarding

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.isfam.core.designsystem.Ink300
import com.isfam.core.designsystem.Ink500
import com.isfam.core.designsystem.IsFamButton
import com.isfam.core.designsystem.IsFamScaffold
import com.isfam.core.designsystem.IsFamTheme
import com.isfam.core.designsystem.IsFamTopBar
import com.isfam.core.designsystem.Safe
import com.isfam.core.designsystem.ScreenHeadline
import com.isfam.core.permission.PermissionChecker
import com.isfam.core.permission.PermissionStatus
import com.isfam.core.permission.PermissionUiState
import com.isfam.core.permission.RuntimePermission
import com.isfam.core.permission.SettingsIntents

/**
 * 08. 권한 허용 안내
 *
 * 대충 만들면 실기기에서 반드시 깨지는 세 가지를 처리합니다.
 *   1. 거부      → 이유를 설명하고 다시 요청
 *   2. 영구 거부 → 팝업이 안 뜨므로 설정 앱으로 안내
 *   3. 설정에서 복귀 → 상태를 다시 읽어야 함 (ON_RESUME)
 *
 * 3번을 빠뜨리면 사용자가 설정에서 권한을 켜고 돌아와도
 * 화면이 계속 "거부됨"으로 남아 있습니다. 흔한 버그입니다.
 */
@Composable
fun PermissionRoute(
    onAllGranted: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val activity = context as Activity
    val checker = remember { PermissionChecker(context) }

    var state by remember { mutableStateOf(checker.snapshot(activity)) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        // 결과를 직접 쓰지 않고 스냅샷을 다시 읽습니다.
        // 영구 거부 판정이 여기서만 정확하게 나옵니다.
        state = checker.snapshot(activity)
    }

    // 설정 앱에 다녀오면 상태가 바뀌어 있을 수 있으므로 재확인
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                state = checker.snapshot(activity)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    PermissionScreen(
        state = state,
        onBack = onBack,
        onRequestClick = {
            val targets = RuntimePermission.required()
                .filter { checker.statusOf(activity, it) != PermissionStatus.Granted }
            targets.forEach(checker::markRequested)
            launcher.launch(targets.map { it.manifestKey }.toTypedArray())
        },
        onOpenSettings = {
            SettingsIntents.safeStart(context, SettingsIntents.appDetails(context))
        },
        onNext = onAllGranted,
    )
}

@Composable
fun PermissionScreen(
    state: PermissionUiState,
    onBack: () -> Unit,
    onRequestClick: () -> Unit,
    onOpenSettings: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IsFamScaffold(
        modifier = modifier,
        topBar = { IsFamTopBar(title = "권한 허용", step = "3 / 3 단계", onBack = onBack) },
        bottomBar = {
            when {
                state.canProceed ->
                    IsFamButton("권한 허용하고 시작하기", onNext)

                state.hasPermanentDenial -> {
                    Text(
                        "일부 권한이 차단되어 있어요. 설정에서 직접 켜주셔야 합니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Ink500,
                    )
                    IsFamButton("설정 열기", onOpenSettings)
                }

                else ->
                    IsFamButton("권한 허용하고 시작하기", onRequestClick)
            }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(12.dp))
            ScreenHeadline(
                title = "두 가지만\n허용하면 끝나요",
                subtitle = "통화 내용은 저장하지 않고, 분석이 끝나면 바로 지웁니다.",
            )
            Spacer(Modifier.height(32.dp))

            RuntimePermission.required().forEach { permission ->
                PermissionRow(
                    title = permission.title,
                    reason = permission.reason,
                    status = state.statusOf(permission),
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun PermissionRow(
    title: String,
    reason: String,
    status: PermissionStatus,
) {
    val granted = status == PermissionStatus.Granted

    Row(modifier = Modifier.fillMaxWidth()) {
        Icon(
            imageVector = if (granted) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
            contentDescription = if (granted) "허용됨" else "허용 필요",
            tint = if (granted) Safe else Ink300,
            modifier = Modifier.size(26.dp),
        )
        Spacer(Modifier.size(12.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(reason, style = MaterialTheme.typography.bodyMedium, color = Ink500)
            if (status == PermissionStatus.PermanentlyDenied) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "설정에서 직접 켜주세요",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

// ── Preview — 상태별로 만들어두면 실행 없이 확인됩니다 ──────────

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun PermissionScreenInitialPreview() {
    IsFamTheme {
        PermissionScreen(
            state = PermissionUiState(
                runtime = RuntimePermission.required()
                    .associateWith { PermissionStatus.NotRequested }
            ),
            onBack = {}, onRequestClick = {}, onOpenSettings = {}, onNext = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun PermissionScreenPartialPreview() {
    IsFamTheme {
        val required = RuntimePermission.required()
        PermissionScreen(
            state = PermissionUiState(
                runtime = required.mapIndexed { i, p ->
                    p to if (i == 0) PermissionStatus.Granted
                    else PermissionStatus.PermanentlyDenied
                }.toMap()
            ),
            onBack = {}, onRequestClick = {}, onOpenSettings = {}, onNext = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun PermissionScreenGrantedPreview() {
    IsFamTheme {
        PermissionScreen(
            state = PermissionUiState(
                runtime = RuntimePermission.required()
                    .associateWith { PermissionStatus.Granted }
            ),
            onBack = {}, onRequestClick = {}, onOpenSettings = {}, onNext = {},
        )
    }
}