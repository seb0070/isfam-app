package com.isfam.core.audio

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * 통화 녹음 파일을 모델 입력으로 변환합니다.
 *
 *   ① MediaCodec 디코딩 (m4a → PCM16)
 *   ② 모노 다운믹스
 *   ③ 앞 90초 절단
 *   ④ 리샘플링 48kHz → 16kHz (저역통과 필터 적용)
 *   ⑤ 정규화
 *   ⑥ 음질 검사
 *
 * 안드로이드에는 torchaudio 도 ffmpeg 도 없어 MediaCodec 으로 직접 합니다.
 * (ffmpeg-kit 은 2025년 프로젝트가 종료되어 신규 채택이 어렵습니다)
 *
 * 실측 기준 (SM-S937N)
 *   원본 audio/mp4a-latm · 48kHz · 모노 1채널
 *   디코딩 속도 약 40ms / 오디오 1초
 */
class AudioPipeline(private val context: Context) {

    sealed interface Result {
        data class Success(val audio: PreparedAudio) : Result
        data class QualityFailed(val audio: PreparedAudio, val reason: QualityIssue) : Result
        data class Failed(val message: String) : Result
    }

    enum class QualityIssue(val serverCode: String, val userMessage: String) {
        TooShort("too_short_for_chunk_analysis", "통화가 너무 짧아 분석할 수 없어요"),
        LowEnergy("low_energy_or_silence", "소리가 거의 없어 분석할 수 없어요"),
        TooLittleSpeech("too_little_speech", "말소리가 충분하지 않아요"),
    }

    suspend fun prepare(uri: Uri): Result = withContext(Dispatchers.IO) {
        val decoded = decode(uri) ?: return@withContext Result.Failed("디코딩 실패")

        // 앞 90초만 사용
        val truncated = truncate(decoded.samples, decoded.sampleRate)

        val resampled = Resampler.resample(
            input = truncated,
            fromHz = decoded.sampleRate,
            toHz = AudioSpec.SAMPLE_RATE,
        )

        val normalized = normalize(resampled)

        val audio = PreparedAudio(
            samples = normalized,
            sampleRate = AudioSpec.SAMPLE_RATE,
            sourceSampleRate = decoded.sampleRate,
            sourceChannels = decoded.channels,
            sourceMime = decoded.mime,
            truncated = truncated.size < decoded.samples.size,
        )

        val issue = checkQuality(audio)
        if (issue != null) Result.QualityFailed(audio, issue) else Result.Success(audio)
    }

    // ── ① 디코딩 ──────────────────────────────────────────────

    private data class Decoded(
        val samples: FloatArray,
        val sampleRate: Int,
        val channels: Int,
        val mime: String,
    )

    private fun decode(uri: Uri): Decoded? {
        var extractor: MediaExtractor? = null
        var codec: MediaCodec? = null
        val pfd = runCatching {
            context.contentResolver.openFileDescriptor(uri, "r")
        }.getOrNull() ?: return null

        try {
            extractor = MediaExtractor().apply { setDataSource(pfd.fileDescriptor) }

            var trackIndex = -1
            var format: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val f = extractor.getTrackFormat(i)
                if (f.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) {
                    trackIndex = i; format = f; break
                }
            }
            if (trackIndex < 0 || format == null) return null

            val mime = format.getString(MediaFormat.KEY_MIME)!!
            val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            val channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)

            extractor.selectTrack(trackIndex)
            codec = MediaCodec.createDecoderByType(mime).apply {
                configure(format, null, null, 0)
                start()
            }

            // 필요한 만큼만 디코딩하면 긴 통화에서 시간을 크게 아낍니다
            val maxSamples = AudioSpec.MAX_ANALYSIS_SEC * sampleRate * channels
            val pcm = ArrayList<Short>(minOf(maxSamples, sampleRate * channels * 30))

            val info = MediaCodec.BufferInfo()
            var sawInputEos = false
            var sawOutputEos = false

            while (!sawOutputEos && pcm.size < maxSamples) {
                if (!sawInputEos) {
                    val inIdx = codec.dequeueInputBuffer(10_000)
                    if (inIdx >= 0) {
                        val buf = codec.getInputBuffer(inIdx)!!
                        val n = extractor.readSampleData(buf, 0)
                        if (n < 0) {
                            codec.queueInputBuffer(
                                inIdx, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM
                            )
                            sawInputEos = true
                        } else {
                            codec.queueInputBuffer(inIdx, 0, n, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }

                val outIdx = codec.dequeueOutputBuffer(info, 10_000)
                if (outIdx >= 0) {
                    if (info.size > 0) {
                        val out = codec.getOutputBuffer(outIdx)!!
                        out.position(info.offset)
                        out.limit(info.offset + info.size)
                        val shorts = out.order(ByteOrder.nativeOrder()).asShortBuffer()
                        while (shorts.hasRemaining()) pcm.add(shorts.get())
                    }
                    codec.releaseOutputBuffer(outIdx, false)
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        sawOutputEos = true
                    }
                }
            }

            if (pcm.isEmpty()) return null

            return Decoded(
                samples = downmix(pcm, channels),
                sampleRate = sampleRate,
                channels = channels,
                mime = mime,
            )
        } catch (e: Exception) {
            return null
        } finally {
            runCatching { codec?.stop(); codec?.release() }
            runCatching { extractor?.release() }
            runCatching { pfd.close() }
        }
    }

    // ── ② 다운믹스 ────────────────────────────────────────────

    /**
     * 실측 결과 통화 녹음은 모노 1채널입니다.
     * 부모님과 상대방 목소리가 한 트랙에 섞여 있어,
     * 채널 분리로는 화자를 나눌 수 없습니다.
     * 화자 분리는 폰 주인 임베딩으로 제거하는 방식(제외법)을 씁니다.
     */
    private fun downmix(pcm: List<Short>, channels: Int): FloatArray {
        if (channels <= 1) {
            return FloatArray(pcm.size) { pcm[it] / 32768f }
        }
        val frames = pcm.size / channels
        return FloatArray(frames) { i ->
            var sum = 0f
            for (c in 0 until channels) sum += pcm[i * channels + c] / 32768f
            sum / channels
        }
    }

    // ── ③ 절단 ────────────────────────────────────────────────

    private fun truncate(samples: FloatArray, sampleRate: Int): FloatArray {
        val limit = AudioSpec.MAX_ANALYSIS_SEC * sampleRate
        return if (samples.size <= limit) samples else samples.copyOfRange(0, limit)
    }

    // ── ⑤ 정규화 ──────────────────────────────────────────────

    private fun normalize(samples: FloatArray): FloatArray {
        if (samples.isEmpty()) return samples

        return when (AudioSpec.NORMALIZATION) {
            AudioSpec.Normalization.None -> samples

            AudioSpec.Normalization.Peak -> {
                var peak = 0f
                for (v in samples) peak = maxOf(peak, abs(v))
                if (peak < 1e-6f) return samples
                val gain = AudioSpec.TARGET_PEAK / peak
                FloatArray(samples.size) { (samples[it] * gain).coerceIn(-1f, 1f) }
            }

            AudioSpec.Normalization.Rms -> {
                val rms = rms(samples)
                if (rms < 1e-6f) return samples
                val gain = AudioSpec.TARGET_RMS / rms
                FloatArray(samples.size) { (samples[it] * gain).coerceIn(-1f, 1f) }
            }
        }
    }

    // ── ⑥ 음질 검사 ───────────────────────────────────────────

    /**
     * 불충분한 음성으로 "안전" 또는 "위험"을 단정하지 않기 위한 안전장치입니다.
     * 미달이면 분석은 하되 결과를 신뢰하지 않고 판정 보류로 처리합니다.
     */
    private fun checkQuality(audio: PreparedAudio): QualityIssue? = when {
        audio.durationSec < AudioSpec.MIN_DURATION_SEC -> QualityIssue.TooShort
        audio.rms < AudioSpec.MIN_RMS -> QualityIssue.LowEnergy
        audio.speechRatio < AudioSpec.MIN_SPEECH_RATIO -> QualityIssue.TooLittleSpeech
        else -> null
    }

    companion object {
        internal fun rms(x: FloatArray): Float {
            if (x.isEmpty()) return 0f
            var acc = 0.0
            for (v in x) acc += v.toDouble() * v
            return sqrt(acc / x.size).toFloat()
        }

        /** 20ms 프레임 단위 간이 VAD. 서버 구현과 같은 방식입니다. */
        internal fun speechRatio(x: FloatArray, sampleRate: Int): Float {
            val frame = sampleRate * AudioSpec.VAD_FRAME_MS / 1000
            if (x.size < frame) return 0f

            var voiced = 0
            var total = 0
            var i = 0
            while (i + frame <= x.size) {
                var peak = 0f
                for (j in i until i + frame) peak = maxOf(peak, abs(x[j]))
                if (peak > AudioSpec.VAD_THRESHOLD) voiced++
                total++
                i += frame
            }
            return if (total == 0) 0f else voiced.toFloat() / total
        }
    }
}

/** 전처리가 끝난 오디오 */
data class PreparedAudio(
    /** float32, 16kHz, 모노, 범위 [-1.0, 1.0] */
    val samples: FloatArray,
    val sampleRate: Int,
    val sourceSampleRate: Int,
    val sourceChannels: Int,
    val sourceMime: String,
    /** 90초 상한에 걸려 잘렸는지 */
    val truncated: Boolean,
) {
    val durationSec: Float get() = samples.size.toFloat() / sampleRate
    val rms: Float by lazy { AudioPipeline.rms(samples) }
    val speechRatio: Float by lazy { AudioPipeline.speechRatio(samples, sampleRate) }

    /** ONNX 입력용 버퍼 */
    fun toFloatBuffer(): java.nio.ByteBuffer =
        java.nio.ByteBuffer
            .allocateDirect(samples.size * 4)
            .order(ByteOrder.nativeOrder())
            .apply { asFloatBuffer().put(samples); rewind() }

    /** 2초 윈도우 / 1초 hop 으로 자릅니다. 화자 분리에 사용합니다. */
    fun windows(): List<FloatArray> {
        val win = (AudioSpec.WINDOW_SEC * sampleRate).toInt()
        val hop = (AudioSpec.HOP_SEC * sampleRate).toInt()
        val minWin = (AudioSpec.MIN_WINDOW_SEC * sampleRate).toInt()

        val result = ArrayList<FloatArray>()
        var start = 0
        while (start + minWin <= samples.size) {
            val end = minOf(start + win, samples.size)
            result += samples.copyOfRange(start, end)
            start += hop
        }
        return result
    }

    fun summary(): String = buildString {
        append("$sourceMime · ${sourceSampleRate}Hz · ${sourceChannels}ch")
        append(" → ${sampleRate}Hz 모노 · %.1f초".format(durationSec))
        if (truncated) append(" (앞 ${AudioSpec.MAX_ANALYSIS_SEC}초)")
        append("\nRMS %.4f · 발화비율 %.0f%%".format(rms, speechRatio * 100))
    }
}
