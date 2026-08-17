package com.isfam.feature.voice

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.File

/**
 * 목소리 등록용 녹음기.
 *
 * 서버는 wav / m4a 를 받습니다. AAC(m4a) 로 녹음하는 이유는
 * 실제 통화 녹음도 m4a 라서 도메인이 맞기 때문입니다.
 * 등록 음성만 무손실로 받으면 오히려 통화 음성과 특성이 달라집니다.
 *
 * 파형은 MediaRecorder.maxAmplitude 를 주기적으로 읽어 그립니다.
 * 실제 파형(PCM 샘플)이 아니라 구간별 최대 진폭이지만
 * 사용자에게 "녹음되고 있다"를 보여주는 목적에는 충분합니다.
 */
class VoiceRecorder(private val context: Context) {

    companion object {
        const val SAMPLE_RATE = 16_000
        /**
         * 문장당 최소 길이.
         *
         * ECAPA 는 발화가 길수록 임베딩이 안정적입니다.
         * 특히 이 성문은 화자 분리에서 "주인 목소리 제거" 기준으로도
         * 쓰이므로 부정확하면 상대방 음성까지 잘못 걸러집니다.
         *
         * VoiceprintEnrollmentService 의 검사값과 반드시 같아야 합니다.
         */
        const val MIN_DURATION_MS = 5_000L
        const val MAX_DURATION_MS = 15_000L
        /** 파형 막대 개수. UI 키트 기준 40개 */
        const val WAVE_BARS = 40
        /** 진폭 샘플링 주기 */
        const val TICK_MS = 60L

        /**
         * 제출 가능한 최소 품질.
         * 이 값 미만이면 목소리가 작거나 주변이 시끄러워 성문을 만들 수 없습니다.
         */
        const val MIN_QUALITY = 0.55f

        /** 이 비율 미만으로만 말했으면 문장을 끝까지 읽지 않은 것입니다 */
        const val MIN_SPEECH_RATIO = 0.45f

        /**
         * 만점 기준 발화 비율.
         *
         * 문장을 자연스럽게 읽으면 쉼표·마침표 때문에 100% 는 안 나옵니다.
         * 70% 면 충분히 또박또박 읽은 것으로 봅니다.
         */
        private const val TARGET_SPEECH_RATIO = 0.70f

        /**
         * 발화로 인정할 진폭 하한 (maxAmplitude 원시값, 0~32767).
         *
         * ⚠️ 로그 스케일 값으로 판정하면 안 됩니다.
         *    실측에서 무음이 0.42, 말소리가 0.78 로 나왔습니다.
         *    원시값으로는 75 vs 3196 인데 로그를 씌우면 차이가
         *    거의 사라져 분별력이 없어집니다.
         *
         * 실측 (SM-S937N, 조용한 실내)
         *   무음·배경잡음  75 ~ 130
         *   정상 발화     1200 ~ 6000
         */
        private const val SPEECH_RAW = 600

        /** 목소리가 또렷하다고 볼 수 있는 평균 진폭 */
        private const val GOOD_RAW = 2500
    }

    enum class State { Idle, Recording, Stopped, Error }

    var state by mutableStateOf(State.Idle)
        private set

    var elapsedMs by mutableIntStateOf(0)
        private set

    /**
     * 파형 표시용 진폭. 최근 WAVE_BARS 개만 유지합니다.
     *
     * ⚠️ 품질 계산에 이걸 쓰면 안 됩니다.
     *    40개 × 60ms = 2.4초치라, 앞부분에서 말하고 뒤에서 쉬면
     *    발화비율이 0에 가깝게 나옵니다.
     *    전체 통계는 아래 누적 카운터를 씁니다.
     */
    val amplitudes = mutableStateListOf<Float>()

    // ── 녹음 전체 구간 누적 (품질 계산용) ──────────────────────
    private var totalFrames = 0
    private var voicedFrames = 0
    private var voicedLevelSum = 0.0

    /** 0.0 ~ 1.0. 길이·음량·발화비율을 종합한 품질 */
    var quality by mutableFloatStateOf(0f)
        private set

    /** 전체 구간 중 실제로 말한 비율 */
    var speechRatio by mutableFloatStateOf(0f)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null

    val recordedFile: File? get() = outputFile?.takeIf { it.exists() && it.length() > 0 }

    // ── 제어 ──────────────────────────────────────────────────

    fun start(sentenceIndex: Int) {
        if (state == State.Recording) return
        reset()

        val file = File(context.cacheDir, "voiceprint_$sentenceIndex.m4a")
        if (file.exists()) file.delete()
        outputFile = file

        runCatching {
            val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION") MediaRecorder()
            }
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioSamplingRate(SAMPLE_RATE)
            r.setAudioChannels(1)
            r.setAudioEncodingBitRate(64_000)
            r.setOutputFile(file.absolutePath)
            r.prepare()
            r.start()
            recorder = r
            state = State.Recording
        }.onFailure {
            errorMessage = "녹음을 시작할 수 없어요: ${it.message}"
            state = State.Error
        }
    }

    fun stop() {
        if (state != State.Recording) return
        runCatching {
            recorder?.stop()
        }.onFailure {
            // 너무 짧으면 stop() 이 실패합니다. 파일도 쓸 수 없는 상태입니다.
            errorMessage = "녹음이 너무 짧아요. 문장을 끝까지 읽어주세요."
            outputFile?.delete()
            outputFile = null
        }
        release()
        state = if (errorMessage == null) State.Stopped else State.Error

        if (com.isfam.BuildConfig.DEBUG) {
            android.util.Log.d(
                "IsFamRecord",
                "정지 · %.1f초 · 발화비율 %.0f%% · 품질 %.0f%% · %s"
                    .format(
                        elapsedMs / 1000f,
                        speechRatio * 100,
                        quality * 100,
                        failureReason?.name ?: "통과",
                    ),
            )
        }
    }

    fun reset() {
        release()
        outputFile?.delete()
        outputFile = null
        amplitudes.clear()
        totalFrames = 0
        voicedFrames = 0
        voicedLevelSum = 0.0
        elapsedMs = 0
        quality = 0f
        speechRatio = 0f
        errorMessage = null
        state = State.Idle
    }

    private fun release() {
        runCatching { recorder?.reset(); recorder?.release() }
        recorder = null
    }

    // ── 화면에서 주기적으로 호출 ──────────────────────────────

    /**
     * TICK_MS 마다 호출해 진폭을 수집합니다.
     * @return 최대 길이에 도달했으면 true (자동 정지 필요)
     */
    fun tick(): Boolean {
        if (state != State.Recording) return false

        val raw = runCatching { recorder?.maxAmplitude ?: 0 }.getOrDefault(0)
        // maxAmplitude 는 0~32767. 로그 스케일이 사람 귀에 더 맞습니다.
        val normalized = if (raw <= 0) 0f
        else (kotlin.math.ln(1f + raw) / kotlin.math.ln(32768f)).coerceIn(0f, 1f)

        // 파형 표시용 (최근 구간만)
        amplitudes.add(normalized)
        if (amplitudes.size > WAVE_BARS) amplitudes.removeAt(0)

        // 품질 계산용 (전체 구간 누적).
        // 판정은 정규화 값이 아니라 원시 진폭으로 합니다.
        totalFrames++
        if (raw > SPEECH_RAW) {
            voicedFrames++
            voicedLevelSum += raw.toDouble()
        }

        elapsedMs += TICK_MS.toInt()
        speechRatio = if (totalFrames == 0) 0f
        else voicedFrames.toFloat() / totalFrames
        quality = estimateQuality()

        // maxAmplitude 가 계속 0 이면 마이크 입력이 없는 것입니다.
        // 에뮬레이터이거나 다른 앱이 마이크를 점유한 경우입니다.
        if (com.isfam.BuildConfig.DEBUG && totalFrames % 16 == 0) {
            android.util.Log.d(
                "IsFamRecord",
                "raw=%5d  발화 %d/%d (%.0f%%)  평균 %.0f  품질 %.0f%%".format(
                    raw, voicedFrames, totalFrames, speechRatio * 100,
                    if (voicedFrames > 0) voicedLevelSum / voicedFrames else 0.0,
                    quality * 100,
                ),
            )
        }

        return elapsedMs >= MAX_DURATION_MS
    }

    /**
     * 음성 품질 점수.
     *
     * ⚠️ 길이는 품질에 포함하지 않습니다.
     *    가만히 있어도 시간은 흐르므로, 길이를 섞으면 아무 말 없이
     *    5초를 채웠을 때 점수가 부풀려집니다. 길이는 canSubmit 에서
     *    별도 조건으로 검사합니다.
     *
     * 두 가지만 봅니다.
     *   발화 비율 — 전체 구간 중 실제로 말한 비율
     *   발화 음량 — 말한 구간에서의 평균 크기 (무음 구간은 제외)
     *
     * 말한 구간만 평균을 내는 이유는, 전체 평균을 쓰면
     * 문장 사이의 자연스러운 쉼표까지 감점되기 때문입니다.
     */
    private fun estimateQuality(): Float {
        // 한 번도 말하지 않았으면 0점입니다
        if (voicedFrames == 0) return 0f

        // 얼마나 자주 말했는가.
        // 최소치(45%)가 아니라 목표치(70%)를 기준으로 나눕니다.
        // 최소치로 나누면 45%만 넘어도 만점이 되어버립니다.
        val ratioScore = (speechRatio / TARGET_SPEECH_RATIO).coerceIn(0f, 1f)

        // 말할 때 충분히 크게 말했는가 (무음 구간은 평균에서 제외)
        val level = (voicedLevelSum / voicedFrames).toFloat()
        val levelScore = ((level - SPEECH_RAW) / (GOOD_RAW - SPEECH_RAW))
            .coerceIn(0f, 1f)

        return (ratioScore * 0.6f + levelScore * 0.4f).coerceIn(0f, 1f)
    }

    /**
     * 제출 가능 여부.
     *
     * 길이만 보면 안 됩니다. 아무 말 없이 5초를 채워도 통과해버리고,
     * 그 음성으로 만든 성문은 화자 분리 기준으로 쓰일 수 없습니다.
     */
    val canSubmit: Boolean
        get() = failureReason == null

    /** 제출할 수 없는 이유. null 이면 통과 */
    val failureReason: FailureReason?
        get() = when {
            state != State.Stopped -> FailureReason.NotRecorded
            recordedFile == null -> FailureReason.NotRecorded
            elapsedMs < MIN_DURATION_MS -> FailureReason.TooShort
            speechRatio < MIN_SPEECH_RATIO -> FailureReason.NoSpeech
            quality < MIN_QUALITY -> FailureReason.LowQuality
            else -> null
        }

    enum class FailureReason(val message: String) {
        NotRecorded("녹음을 먼저 진행해 주세요"),
        TooShort("녹음이 너무 짧아요. 문장을 끝까지 읽어주세요"),
        NoSpeech("목소리가 거의 들리지 않아요. 다시 읽어주세요"),
        LowQuality("소리가 작거나 주변이 시끄러워요. 조용한 곳에서 다시 해주세요"),
    }
}

/**
 * 등록용 문장 3개.
 *
 * 실제 가족 통화에서 나오는 말로 골랐습니다.
 * 등록 음성과 통화 음성의 말투가 다르면 같은 사람이어도
 * 임베딩이 달라져 오탐률이 올라갑니다.
 *
 * 1번은 보이스피싱이 흉내내는 첫마디("나야")를 포함합니다.
 * 실제 사칭 상황과 같은 발화를 등록해 두는 효과가 있습니다.
 *
 * 각 문장은 자연스럽게 읽으면 5~6초, 합쳐서 약 16초입니다.
 */
val EnrollmentSentences = listOf(
    "어, 나야. 지금 통화 괜찮아? 별일 없지? 밥은 먹었고?",
    "요즘 어떻게 지내? 별일 없으면 됐고, 무슨 일 있으면 바로 연락해.",
    "알았어, 조심히 들어가. 도착하면 연락하고 시간 될 때 얼굴 한번 보자.",
)