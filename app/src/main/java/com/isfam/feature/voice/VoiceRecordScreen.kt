package com.isfam.feature.voice

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.isfam.core.designsystem.Amber500
import com.isfam.core.designsystem.CardIllustStart
import com.isfam.core.designsystem.Danger
import com.isfam.core.designsystem.EyebrowBrown
import com.isfam.core.designsystem.Honey300
import com.isfam.core.designsystem.Honey400
import com.isfam.core.designsystem.Ink
import com.isfam.core.designsystem.InkMuted
import com.isfam.core.designsystem.IsFamScaffold
import com.isfam.core.designsystem.IsFamSecondaryButton
import com.isfam.core.designsystem.IsFamTheme
import com.isfam.core.designsystem.IsFamTopBar
import com.isfam.core.designsystem.Mascot
import com.isfam.core.designsystem.MascotImage
import com.isfam.core.designsystem.SentenceCardEnd
import com.isfam.core.designsystem.Tint50
import com.isfam.core.designsystem.TrackBeige
import com.isfam.core.designsystem.WaveInactive
import com.isfam.core.designsystem.White
import kotlinx.coroutines.delay
import java.io.File

/**
 * 10. 목소리 녹음
 *
 * UI 키트 실측값
 *   문장 카드 radius 22 · gradient(160deg #FFF6E4→#FFEEDB) · padding 18
 *   아이브로우 700 11.5 #C08A3A · 문장 800 20/1.5
 *   마스코트 112 원형 #FFF1DE padding 8
 *   파형 height 62 · 막대 40개 · gap 2 · 활성 #F26A0A / 비활성 #EBE2D4
 *   품질바 height 6 · radius 4
 *   녹음 버튼 84 원형 · gradient(140deg) · 내부 26 흰 사각(radius 9)
 *
 * 마이크 권한은 이 화면에서 요청합니다.
 * 08 권한 화면에서 미리 받지 않는 이유는, 마이크가 실제로 쓰이는
 * 맥락에서 요청해야 수락률이 높기 때문입니다.
 */
@Composable
fun VoiceRecordRoute(
    sentenceIndex: Int,
    onComplete: (File) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val recorder = remember { VoiceRecorder(context) }
    var hasMicPermission by remember {
        mutableStateOf(
            androidx.core.content.ContextCompat.checkSelfPermission(
                context, Manifest.permission.RECORD_AUDIO
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    val micLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
        if (granted) recorder.start(sentenceIndex)
    }

    // 녹음 중 진폭 수집
    LaunchedEffect(recorder.state) {
        while (recorder.state == VoiceRecorder.State.Recording) {
            delay(VoiceRecorder.TICK_MS)
            if (recorder.tick()) recorder.stop()
        }
    }

    // 화면을 벗어나면 반드시 해제
    DisposableEffect(Unit) {
        onDispose { recorder.reset() }
    }

    VoiceRecordScreen(
        sentenceIndex = sentenceIndex,
        sentence = EnrollmentSentences.getOrElse(sentenceIndex - 1) { "" },
        state = recorder.state,
        elapsedMs = recorder.elapsedMs,
        amplitudes = recorder.amplitudes,
        quality = recorder.quality,
        errorMessage = recorder.errorMessage,
        canSubmit = recorder.canSubmit,
        onToggleRecord = {
            when (recorder.state) {
                VoiceRecorder.State.Recording -> recorder.stop()
                VoiceRecorder.State.Idle,
                VoiceRecorder.State.Error -> {
                    if (hasMicPermission) recorder.start(sentenceIndex)
                    else micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
                VoiceRecorder.State.Stopped -> recorder.start(sentenceIndex)
            }
        },
        onSubmit = { recorder.recordedFile?.let(onComplete) },
        onBack = onBack,
    )
}

@Composable
fun VoiceRecordScreen(
    sentenceIndex: Int,
    sentence: String,
    state: VoiceRecorder.State,
    elapsedMs: Int,
    amplitudes: List<Float>,
    quality: Float,
    errorMessage: String?,
    canSubmit: Boolean,
    onToggleRecord: () -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val recording = state == VoiceRecorder.State.Recording
    val stopped = state == VoiceRecorder.State.Stopped

    IsFamScaffold(
        modifier = modifier,
        topBar = { IsFamTopBar(title = "문장 $sentenceIndex / 3", onBack = onBack) },
        bottomBar = {
            if (stopped && canSubmit) {
                com.isfam.core.designsystem.IsFamButton(
                    text = if (sentenceIndex < 3) "다음 문장" else "등록 완료하기",
                    onClick = onSubmit,
                )
                IsFamSecondaryButton(text = "다시 녹음", onClick = onToggleRecord)
            } else {
                RecordButton(recording = recording, onClick = onToggleRecord)
            }
        },
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 문장 카드
            Box(
                modifier = Modifier
                    .padding(horizontal = 22.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.linearGradient(listOf(CardIllustStart, SentenceCardEnd))
                    )
                    .padding(18.dp),
            ) {
                Column {
                    Text(
                        "아래 문장을 읽어주세요",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                        color = EyebrowBrown,
                    )
                    Spacer(Modifier.height(9.dp))
                    Text(
                        sentence,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = 20.sp, lineHeight = 30.sp,
                        ),
                        color = Ink,
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(112.dp)
                        .clip(CircleShape)
                        .background(Tint50)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    MascotImage(
                        mascot = if (recording) Mascot.Listening else Mascot.Watching,
                        size = 92.dp,
                        cornerRadius = 46.dp,
                        background = Color.Transparent,
                    )
                }

                Spacer(Modifier.height(14.dp))

                Text(
                    when {
                        recording -> "듣고 있어요"
                        stopped -> "잘 녹음됐어요"
                        else -> "준비되면 눌러주세요"
                    },
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
                    color = Ink,
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    errorMessage ?: "마이크에서 20cm 정도 거리를 두세요",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                    color = if (errorMessage != null) Danger else InkMuted,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(20.dp))

                Waveform(amplitudes = amplitudes, active = recording || stopped)

                Spacer(Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        formatTime(0),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = InkMuted,
                    )
                    Text(
                        formatTime(elapsedMs),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = InkMuted,
                    )
                }

                Spacer(Modifier.height(18.dp))

                QualityBar(quality)
            }
        }
    }
}

// ── 파형 ──────────────────────────────────────────────────────

@Composable
private fun Waveform(
    amplitudes: List<Float>,
    active: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().height(62.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(VoiceRecorder.WAVE_BARS) { index ->
            // 최신 진폭이 오른쪽으로 흐르도록 뒤에서부터 채웁니다
            val filledFrom = VoiceRecorder.WAVE_BARS - amplitudes.size
            val amp = if (index >= filledFrom) amplitudes[index - filledFrom] else null

            val ratio = amp?.coerceIn(0.18f, 1f) ?: (0.24f + (index % 5) * 0.11f)
            val animated by animateFloatAsState(ratio, tween(120), label = "wave$index")

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(animated)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (amp != null && active) Amber500 else WaveInactive),
            )
        }
    }
}

// ── 품질 표시 ─────────────────────────────────────────────────

@Composable
private fun QualityBar(quality: Float, modifier: Modifier = Modifier) {
    val percent = (quality * 100).toInt()
    val animated by animateFloatAsState(quality, tween(240), label = "quality")

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                "음성 품질",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                color = InkMuted,
            )
            Text(
                "$percent%",
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                color = if (percent >= 60) Amber500 else Danger,
            )
        }
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(TrackBeige),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animated)
                    .fillMaxSize()
                    .background(Brush.horizontalGradient(listOf(Honey300, Amber500))),
            )
        }
    }
}

// ── 녹음 버튼 ─────────────────────────────────────────────────

@Composable
private fun RecordButton(recording: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().height(84.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(84.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Honey400, Amber500)))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            // 대기 상태는 둥근 사각, 녹음 중에는 정지 사각
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(RoundedCornerShape(if (recording) 4.dp else 9.dp))
                    .background(White),
            )
        }
    }
}

private fun formatTime(ms: Int): String =
    "%02d:%02d".format(ms / 1000 / 60, ms / 1000 % 60)

// ── Preview ───────────────────────────────────────────────────

@Preview(name = "대기", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun VoiceRecordIdlePreview() = IsFamTheme {
    VoiceRecordScreen(
        sentenceIndex = 1, sentence = EnrollmentSentences[0],
        state = VoiceRecorder.State.Idle, elapsedMs = 0,
        amplitudes = emptyList(), quality = 0f, errorMessage = null, canSubmit = false,
        onToggleRecord = {}, onSubmit = {}, onBack = {},
    )
}

@Preview(name = "녹음 중", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun VoiceRecordingPreview() = IsFamTheme {
    VoiceRecordScreen(
        sentenceIndex = 1, sentence = EnrollmentSentences[0],
        state = VoiceRecorder.State.Recording, elapsedMs = 8_000,
        amplitudes = List(12) { 0.2f + (it % 6) * 0.13f },
        quality = 0.72f, errorMessage = null, canSubmit = false,
        onToggleRecord = {}, onSubmit = {}, onBack = {},
    )
}

@Preview(name = "완료", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun VoiceRecordDonePreview() = IsFamTheme {
    VoiceRecordScreen(
        sentenceIndex = 2, sentence = EnrollmentSentences[1],
        state = VoiceRecorder.State.Stopped, elapsedMs = 9_600,
        amplitudes = List(40) { 0.2f + (it % 7) * 0.11f },
        quality = 0.84f, errorMessage = null, canSubmit = true,
        onToggleRecord = {}, onSubmit = {}, onBack = {},
    )
}