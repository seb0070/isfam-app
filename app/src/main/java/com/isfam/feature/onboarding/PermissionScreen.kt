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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.isfam.core.designsystem.Divider
import com.isfam.core.designsystem.InkBody
import com.isfam.core.designsystem.InkMuted
import com.isfam.core.designsystem.Danger
import com.isfam.core.designsystem.IsFamButton
import com.isfam.data.repository.ApiFailure
import com.isfam.core.designsystem.IsFamScaffold
import com.isfam.core.designsystem.IsFamTheme
import com.isfam.core.designsystem.IsFamTopBar
import com.isfam.core.designsystem.Safe
import com.isfam.core.designsystem.ScreenHeadline
import com.isfam.core.permission.PermissionChecker
import com.isfam.core.permission.PermissionStatus
import com.isfam.core.permission.PermissionUiState
import com.isfam.core.rememberAppContainer
import com.isfam.core.permission.RuntimePermission
import com.isfam.core.permission.SettingsIntents
import com.isfam.feature.auth.SignUpSession

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
    /** 06에서 모은 가입 정보. 여기서 권한 상태를 더해 서버로 보냅니다. */
    session: SignUpSession,
    onSignUpComplete: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val activity = context as Activity
    val checker = remember { PermissionChecker(context) }

    var state by remember { mutableStateOf(checker.snapshot(activity)) }

    val container = rememberAppContainer()
    val auth = container.authRepository
    val device = container.deviceRepository
    val scope = rememberCoroutineScope()

    var submitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

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
        submitting = submitting,
        errorMessage = errorMessage,
        onNext = {
            // 권한 상태를 세션에 반영한 뒤 가입 API 를 호출합니다.
            session.applyPermissions(
                notification = checker.isGranted(RuntimePermission.Notification),
                microphone = checker.isGranted(RuntimePermission.Microphone),
                file = checker.isGranted(
                    if (android.os.Build.VERSION.SDK_INT >= 33) RuntimePermission.MediaAudio
                    else RuntimePermission.ExternalStorage
                ),
                // ⚠️ 앱이 확인할 수 없는 값입니다. 자동녹음 안내 화면에서
                //    사용자가 "설정했어요"를 누르면 true 가 됩니다.
                callRecording = session.callRecordingEnabled,
            )

            scope.launch {
                submitting = true
                errorMessage = null

                auth.signUp(session.toSignUpParams())
                    .onSuccess {
                        // 가입 직후 단말을 등록해 둡니다.
                        // 실패해도 가입 자체는 성공이므로 흐름을 막지 않습니다.
                        // TODO: FCM 토큰으로 교체
                        device.registerDevice(pushToken = "")

                        session.reset()
                        onSignUpComplete()
                    }
                    .onFailure {
                        // 서버가 어느 필드가 틀렸는지 알려주면 그 문구를 씁니다.
                        // "입력값을 확인해 주세요"만 보여주면 뭘 고칠지 알 수 없습니다.
                        errorMessage = (it as? ApiFailure)?.displayMessage
                            ?: "가입에 실패했어요. 잠시 후 다시 시도해 주세요"
                    }
                submitting = false
            }
        },
    )
}

@Composable
fun PermissionScreen(
    state: PermissionUiState,
    /** 가입 요청 중이면 버튼을 잠급니다 */
    submitting: Boolean = false,
    errorMessage: String? = null,
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
                state.canProceed -> {
                    IsFamButton(
                        text = if (submitting) "가입하는 중…" else "허용하고 가입 완료",
                        onClick = onNext,
                        enabled = !submitting,
                    )
                    errorMessage?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                            color = Danger,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                state.hasPermanentDenial -> {
                    Text(
                        "일부 권한이 차단되어 있어요. 설정에서 직접 켜주셔야 합니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = InkMuted,
                    )
                    IsFamButton(text = "설정 열기", onClick = onOpenSettings)
                }

                else ->
                    IsFamButton(text = "권한 허용하기", onClick = onRequestClick)
            }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 26.dp),
        ) {
            Spacer(Modifier.height(12.dp))
            ScreenHeadline(
                title = "두 가지만\n허용하면 끝나요",
                body = "통화 내용은 저장하지 않고, 분석이 끝나면 바로 지웁니다.",
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
            tint = if (granted) Safe else Divider,
            modifier = Modifier.size(26.dp),
        )
        Spacer(Modifier.size(12.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(reason, style = MaterialTheme.typography.bodyMedium, color = InkMuted)
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