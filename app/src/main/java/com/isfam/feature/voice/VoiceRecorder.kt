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
        const val MIN_DURATION_MS = 3_000L
        const val MAX_DURATION_MS = 15_000L
        /** 파형 막대 개수. UI 키트 기준 40개 */
        const val WAVE_BARS = 40
        /** 진폭 샘플링 주기 */
        const val TICK_MS = 60L
    }

    enum class State { Idle, Recording, Stopped, Error }

    var state by mutableStateOf(State.Idle)
        private set

    var elapsedMs by mutableIntStateOf(0)
        private set

    /** 0.0 ~ 1.0 로 정규화한 진폭 목록 */
    val amplitudes = mutableStateListOf<Float>()

    /** 0.0 ~ 1.0. 길이·음량·변화량을 종합한 대략적 품질 */
    var quality by mutableFloatStateOf(0f)
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
            errorMessage = "녹음이 너무 짧아요. 3초 이상 말씀해 주세요."
            outputFile?.delete()
            outputFile = null
        }
        release()
        state = if (errorMessage == null) State.Stopped else State.Error
    }

    fun reset() {
        release()
        outputFile?.delete()
        outputFile = null
        amplitudes.clear()
        elapsedMs = 0
        quality = 0f
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

        amplitudes.add(normalized)
        if (amplitudes.size > WAVE_BARS) amplitudes.removeAt(0)

        elapsedMs += TICK_MS.toInt()
        quality = estimateQuality()

        return elapsedMs >= MAX_DURATION_MS
    }

    /**
     * 대략적인 품질 점수.
     *
     * ⚠️ 서버 판정을 대체하지 않습니다. 사용자에게 즉각적인 피드백을
     *    주기 위한 화면용 지표이고, 최종 판단은 업로드 후 서버가 합니다.
     *
     * 길이 · 평균 음량 · 무음 비율 세 가지를 섞습니다.
     */
    private fun estimateQuality(): Float {
        if (amplitudes.isEmpty()) return 0f

        val durationScore = (elapsedMs / MIN_DURATION_MS.toFloat()).coerceIn(0f, 1f)

        val mean = amplitudes.average().toFloat()
        // 통화·마이크 녹음에서 0.35 근처면 충분히 또렷합니다
        val volumeScore = (mean / 0.35f).coerceIn(0f, 1f)

        val voiced = amplitudes.count { it > 0.12f }
        val speechScore = (voiced.toFloat() / amplitudes.size / 0.5f).coerceIn(0f, 1f)

        return (durationScore * 0.35f + volumeScore * 0.35f + speechScore * 0.3f)
            .coerceIn(0f, 1f)
    }

    val canSubmit: Boolean
        get() = state == State.Stopped &&
                elapsedMs >= MIN_DURATION_MS &&
                recordedFile != null
}

/** 등록용 문장 3개 */
val EnrollmentSentences = listOf(
    "엄마, 지금 통화 괜찮아? 나 도착하면 다시 전화할게.",
    "오늘 저녁은 집에서 먹을 거야. 걱정하지 말고 기다려.",
    "무슨 일 있으면 꼭 나한테 먼저 전화해. 알겠지?",
)