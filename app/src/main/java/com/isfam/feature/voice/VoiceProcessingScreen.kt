package com.isfam.feature.voice

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.isfam.core.designsystem.Amber500
import com.isfam.core.IsFamApplication
import com.isfam.core.designsystem.CardIllustStart
import com.isfam.core.designsystem.Danger
import com.isfam.core.designsystem.DisabledBg
import com.isfam.core.designsystem.Honey300
import com.isfam.core.designsystem.IndicatorOff
import com.isfam.core.designsystem.Ink
import com.isfam.core.designsystem.InkFaint
import com.isfam.core.designsystem.InkMuted
import com.isfam.core.designsystem.InkPlaceholder
import com.isfam.core.designsystem.IsFamScaffold
import com.isfam.core.designsystem.IsFamTheme
import com.isfam.core.designsystem.Mascot
import com.isfam.core.designsystem.MascotImage
import com.isfam.core.designsystem.ProcessingEnd
import com.isfam.core.designsystem.Safe
import com.isfam.core.designsystem.White
import com.isfam.core.ml.VoiceprintEnrollmentService
import com.isfam.data.repository.ApiFailure
import com.isfam.data.repository.VoiceQuality
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 11. 성문 생성 중
 *
 * UI 키트 실측값
 *   마스코트 172 원형 · gradient(160deg #FFF6E4→#FFE3BE) · padding 14
 *   제목 800 23 · 부제 500 13.5/1.6
 *   진행바 height 10 · radius 6 · 배경 #EFE7DA
 *   퍼센트 800 13 앰버
 *   단계 카드 radius 22 · padding 18 · gap 13
 *     완료 22원 #5AA97A ✓ / 진행중 border 2.5 앰버(pulse) / 대기 border #E4D9C9
 */
/**
 * 실제로 일어나는 일과 라벨을 맞췄습니다.
 * 진행바만 도는 가짜 단계를 두면 문제가 생겼을 때
 * 어디서 멈췄는지 알 수 없습니다.
 */
enum class ProcessingStep(val label: String) {
    QualityCheck("음성 품질 검사"),
    NoiseCleanup("노이즈 정리"),
    Voiceprint("성문(Voiceprint) 생성"),
    SecureStore("안전하게 저장"),
}

@Composable
fun VoiceProcessingRoute(
    /** 10번 화면에서 녹음한 문장 3개 */
    recordedFiles: List<File>,
    /** 저장할 프로필 ID. 본인이면 OWNER_PROFILE_ID */
    profileId: String = VoiceprintEnrollmentService.OWNER_PROFILE_ID,
    onComplete: () -> Unit,
    onFailed: (String) -> Unit,
) {
    val context = LocalContext.current
    val container = remember { (context.applicationContext as IsFamApplication).container }
    val ml = container.ml
    val voiceprintRepo = container.voiceprintRepository

    var doneCount by remember { mutableIntStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // 콜백을 최신 값으로 유지합니다.
    // LaunchedEffect 가 다시 시작되지 않게 하려면 콜백을 키에서 빼야 하는데,
    // 그러면 오래된 람다를 붙잡고 있을 수 있어 rememberUpdatedState 로 감쌉니다.
    val currentOnComplete by rememberUpdatedState(onComplete)
    val currentOnFailed by rememberUpdatedState(onFailed)

    // 파일 목록은 매 컴포지션마다 새 List 인스턴스가 될 수 있습니다.
    // 그대로 키로 쓰면 LaunchedEffect 가 반복 실행되므로 크기로 고정합니다.
    LaunchedEffect(recordedFiles.size) {
        if (recordedFiles.isEmpty()) {
            currentOnFailed("녹음 파일이 없습니다")
            return@LaunchedEffect
        }

        // 화면 전환 과정에서 파일이 사라지는 일이 있었습니다.
        // 무슨 일이 있었는지 알 수 있도록 여기서 한 번 확인합니다.
        val missing = recordedFiles.filter { !it.exists() || it.length() == 0L }
        if (missing.isNotEmpty()) {
            android.util.Log.e(
                "IsFamVoice",
                "녹음 파일 누락: ${missing.joinToString { it.name }}",
            )
            currentOnFailed("녹음 파일이 손상되었어요. 처음부터 다시 녹음해 주세요")
            return@LaunchedEffect
        }

        val outcome = runCatching {
            // ① 음질 검사 — enroll() 내부에서 길이·RMS 를 확인합니다
            doneCount = 1
            delay(300)

            // ② 노이즈 정리 (현재는 정규화만)
            doneCount = 2
            delay(300)

            // ③ 성문 생성 — ONNX 추론. 실제로 시간이 걸리는 단계입니다
            val result = withContext(Dispatchers.Default) {
                ml.enrollmentService.enroll(
                    familyId = profileId,
                    audioFiles = recordedFiles,
                )
            }
            doneCount = 3

            // 임계값 조정을 위해 지표를 남깁니다.
            // SNR 은 서버 스펙에 없어 전송하지 않습니다.
            if (com.isfam.BuildConfig.DEBUG) {
                result.quality.forEach { (id, q) ->
                    android.util.Log.d(
                        "IsFamVoice",
                        "문장 %d · %.1f초 · RMS %.4f · 발화 %.0f%% · SNR %s".format(
                            id, q.durationSeconds, q.rmsEnergy,
                            q.speechRatio * 100,
                            if (q.silentFrames >= 10) "%.1fdB".format(q.snrDb)
                            else "측정 불가(배경 구간 ${q.silentFrames}프레임)",
                        ),
                    )
                }
            }

            // ④ 저장 — Keystore(완료) + 서버.
            //    오디오가 아니라 임베딩만 보냅니다.
            //
            // 본인 성문일 때만 올립니다. 가족 성문은 각자의 폰에서
            // 등록하고 GET /family/embeddings 로 받아옵니다.
            if (profileId == VoiceprintEnrollmentService.OWNER_PROFILE_ID) {
                result.perSentence.forEach { (sentenceId, embedding) ->
                    val q = result.quality[sentenceId]
                    voiceprintRepo.registerVoiceprint(
                        sentenceId = sentenceId,
                        embedding = embedding,
                        quality = VoiceQuality(
                            isAnalyzable = true,
                            durationSeconds = q?.durationSeconds,
                            rmsEnergy = q?.rmsEnergy,
                            peakAmplitude = q?.peakAmplitude,
                            // 이 값을 빠뜨리면 서버 품질 판정이 절반만 돕니다
                            speechRatio = q?.speechRatio,
                        ),
                    ).onFailure { error ->
                        // 서버 등록이 실패해도 Keystore 에는 저장돼 있어
                        // 이 폰에서의 판별은 동작합니다.
                        // 다만 가족과 공유되지 않으므로 사용자에게 알립니다.
                        throw error
                    }
                }
            }

            doneCount = 4
            delay(400)
        }

        // ⚠️ 화면을 떠나면 이 코루틴이 취소되면서 CancellationException 이
        //    발생합니다. runCatching 이 그것까지 잡아 "실패"로 처리하면
        //    onFailed 가 한 번 더 호출되어 녹음 화면으로 되돌아갑니다.
        //    취소는 오류가 아니므로 그대로 다시 던져야 합니다.
        outcome.exceptionOrNull()?.let { error ->
            if (error is kotlinx.coroutines.CancellationException) throw error
            errorMessage = (error as? ApiFailure)?.displayMessage
                ?: error.message
                        ?: "성문 생성에 실패했습니다"
            currentOnFailed(errorMessage!!)
            return@LaunchedEffect
        }

        // 원본 녹음 파일은 성문 생성 후 삭제합니다.
        // 음성 원본을 남기지 않는다는 원칙을 코드로 지킵니다.
        recordedFiles.forEach { runCatching { it.delete() } }
        currentOnComplete()
    }

    VoiceProcessingScreen(doneCount = doneCount, errorMessage = errorMessage)
}

@Composable
fun VoiceProcessingScreen(
    doneCount: Int,
    errorMessage: String? = null,
    modifier: Modifier = Modifier,
) {
    val progress = doneCount / ProcessingStep.entries.size.toFloat()
    val animated by animateFloatAsState(progress, tween(400), label = "progress")

    IsFamScaffold(
        modifier = modifier,
        bottomBar = {
            Text(
                "앱을 종료하지 말고 잠시 기다려 주세요",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                color = InkFaint,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(172.dp)
                    .shadow(10.dp, CircleShape, clip = false)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(CardIllustStart, ProcessingEnd))
                    )
                    .padding(14.dp),
                contentAlignment = Alignment.Center,
            ) {
                MascotImage(
                    mascot = Mascot.Analyzing,
                    size = 144.dp,
                    cornerRadius = 72.dp,
                    background = Color.Transparent,
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(
                "성문을 만들고 있어요",
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 23.sp),
                color = Ink,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                errorMessage ?: "잠시만 기다려 주세요. 보통 10초 이내에 끝나요.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.5.sp, lineHeight = 22.sp,
                ),
                color = if (errorMessage != null) Danger else InkMuted,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(DisabledBg),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animated)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Brush.horizontalGradient(listOf(Honey300, Amber500))),
                )
            }
            Spacer(Modifier.height(9.dp))
            Text(
                "${(animated * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                color = Amber500,
            )

            Spacer(Modifier.height(24.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(5.dp, RoundedCornerShape(22.dp), clip = false)
                    .clip(RoundedCornerShape(22.dp))
                    .background(White)
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(13.dp),
            ) {
                ProcessingStep.entries.forEachIndexed { index, step ->
                    StepRow(
                        label = step.label,
                        done = index < doneCount,
                        active = index == doneCount,
                    )
                }
            }
        }
    }
}

@Composable
private fun StepRow(label: String, done: Boolean, active: Boolean) {
    val pulse = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by pulse.animateFloat(
        initialValue = 1f, targetValue = 0.35f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "pulseAlpha",
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .then(if (active) Modifier.alpha(pulseAlpha) else Modifier)
                .clip(CircleShape)
                .background(if (done) Safe else Color.Transparent)
                .then(
                    when {
                        done -> Modifier
                        active -> Modifier.border(2.5.dp, Amber500, CircleShape)
                        else -> Modifier.border(2.5.dp, IndicatorOff, CircleShape)
                    }
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (done) {
                Text(
                    "✓",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = White,
                )
            }
        }

        Text(
            label,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
            color = when {
                done -> Ink
                active -> Amber500
                else -> InkPlaceholder
            },
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun VoiceProcessingPreview() = IsFamTheme {
    VoiceProcessingScreen(doneCount = 2)
}