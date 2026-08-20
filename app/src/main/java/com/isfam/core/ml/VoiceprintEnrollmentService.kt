package com.isfam.core.ml

import java.io.File
import kotlin.math.sqrt

class VoiceprintEnrollmentService(
    private val decoder: AndroidAudioDecoder,
    private val verifier: OnDeviceSpeakerVerifier,
    private val store: EncryptedVoiceprintStore,
) {
    companion object {
        const val OWNER_PROFILE_ID = "owner"

        /** 프레임 크기. 서버 audio_quality.py 와 동일합니다 */
        private const val FRAME_MS = 20

        /**
         * 발화로 인정할 프레임 RMS.
         * 서버 VoiceQualityPolicy 의 MIN_RMS_ENERGY 와 같은 값입니다.
         */
        private const val SPEECH_RMS = 0.02

        /**
         * 배경으로 볼 프레임 RMS 상한.
         *
         * ⚠️ 발화 기준과 반드시 분리해야 합니다.
         *    하나의 임계값으로 나누면 배경 소음까지 "발화"로 세어져
         *    무음 구간에 거의 0에 가까운 프레임만 남습니다.
         *    그러면 SNR 분모가 극단적으로 작아져 80dB 같은
         *    현실에 없는 값이 나옵니다. (실제로 겪은 문제)
         *
         *    두 값 사이 구간은 애매하므로 양쪽 어디에도 넣지 않습니다.
         */
        private const val SILENCE_RMS = 0.01

        /**
         * SNR 을 신뢰하려면 무음 구간이 최소 이만큼은 있어야 합니다.
         * 20ms × 10 = 0.2초. 이보다 적으면 표본이 부족해 값이 튑니다.
         */
        private const val MIN_SILENT_FRAMES = 10
    }

    /**
     * 등록 결과.
     *
     * 서버는 문장별로 sentence_id 와 함께 받으므로 개별 임베딩이 필요하고,
     * 온디바이스 대조는 평균 성문을 씁니다. 둘 다 돌려줍니다.
     */
    data class Result(
        /** Keystore 에 저장된 대표 성문 */
        val voiceprint: FloatArray,
        /** 문장 번호(1부터) → 임베딩. 서버 전송용 */
        val perSentence: Map<Int, FloatArray>,
        /** 문장별 음질 지표. 서버 audio_quality 로 보냅니다 */
        val quality: Map<Int, SampleQuality>,
    )

    /** 서버 audio_quality 와 같은 항목 */
    data class SampleQuality(
        val durationSeconds: Double,
        val rmsEnergy: Double,
        val peakAmplitude: Double,
        /**
         * 발화 구간 비율.
         *
         * ⚠️ 이 값을 안 보내면 서버 VoiceQualityPolicy 가
         *    null 을 통과로 처리해 품질 검사가 절반만 돕니다.
         *    (구버전 앱을 막지 않으려는 서버 설계)
         */
        val speechRatio: Double,
        /** 배경으로 분류된 프레임 수. SNR 신뢰도 판단용 */
        val silentFrames: Int,
        /**
         * 신호 대 잡음비(dB).
         *
         * 서버 스펙에는 없어 전송하지 않고 로그로만 남깁니다.
         * rms 와 speech_ratio 는 "소리가 났는가"만 알려줄 뿐,
         * 그게 목소리인지 주변 소음인지 구분하지 못합니다.
         *
         *   20dB 이상  깨끗함
         *   10dB 미만  잡음이 많음
         */
        val snrDb: Double,
    )

    suspend fun enroll(
        familyId: String,
        audioFiles: List<File>,
        onProgress: (completed: Int, total: Int) -> Unit = { _, _ -> },
    ): Result {
        require(audioFiles.isNotEmpty()) { "등록 음성이 없습니다." }

        val perSentence = LinkedHashMap<Int, FloatArray>()
        val quality = LinkedHashMap<Int, SampleQuality>()

        val embeddings = audioFiles.mapIndexed { index, file ->
            val samples = decoder.decode(file)

            // VoiceRecorder.MIN_DURATION_MS(5초) 와 반드시 같은 값이어야 합니다.
            // 이 성문은 화자 분리에서 "주인 목소리 제거" 기준으로도 쓰이므로
            // 짧으면 상대방 음성까지 잘못 걸러집니다.
            require(samples.size >= SpeakerFbankExtractor.SAMPLE_RATE * 5) {
                "${file.name}: 5초 이상의 음성이 필요합니다. 문장을 끝까지 읽어주세요."
            }

            val rms = sqrt(samples.fold(0.0) { acc, value -> acc + value * value } / samples.size)
            require(rms >= PreprocessSpec.MIN_RMS) { "${file.name}: 목소리가 너무 작습니다." }

            val embedding = verifier.createEmbedding(samples)
            val analysis = analyzeFrames(samples)

            // 서버는 sentence_id 를 1부터 셉니다
            val sentenceId = index + 1
            perSentence[sentenceId] = embedding
            quality[sentenceId] = SampleQuality(
                durationSeconds = samples.size.toDouble() / SpeakerFbankExtractor.SAMPLE_RATE,
                rmsEnergy = rms,
                peakAmplitude = samples.maxOf { kotlin.math.abs(it) }.toDouble(),
                speechRatio = analysis.speechRatio,
                silentFrames = analysis.silentFrames,
                snrDb = analysis.snrDb,
            )

            onProgress(sentenceId, audioFiles.size)
            embedding
        }

        val voiceprint = SpeakerMath.averageAndNormalize(embeddings)
        store.save(familyId, voiceprint)

        return Result(voiceprint, perSentence, quality)
    }

    // ── 프레임 분석 ───────────────────────────────────────────

    private data class FrameAnalysis(
        val speechRatio: Double,
        val snrDb: Double,
        /** SNR 을 구할 수 있었는지 판단하는 근거 */
        val silentFrames: Int,
    )

    /**
     * 20ms 프레임 단위로 발화 비율과 SNR 을 구합니다.
     *
     * 프레임 크기와 임계값은 서버 audio_quality.py 와 같습니다.
     * 두 곳의 계산이 다르면 앱이 통과시킨 값을 서버가 거절하게 됩니다.
     */
    private fun analyzeFrames(samples: FloatArray): FrameAnalysis {
        val frameSize = SpeakerFbankExtractor.SAMPLE_RATE * FRAME_MS / 1000
        if (samples.size < frameSize) return FrameAnalysis(0.0, 0.0, 0)

        var voicedCount = 0
        var silentCount = 0
        var totalCount = 0
        var voicedEnergy = 0.0
        var silentEnergy = 0.0

        var start = 0
        while (start + frameSize <= samples.size) {
            var acc = 0.0
            for (i in start until start + frameSize) {
                acc += samples[i].toDouble() * samples[i]
            }
            val frameRms = sqrt(acc / frameSize)

            totalCount++
            when {
                frameRms >= SPEECH_RMS -> {
                    voicedCount++
                    voicedEnergy += frameRms
                }
                frameRms < SILENCE_RMS -> {
                    silentCount++
                    silentEnergy += frameRms
                }
                // 두 기준 사이는 판단을 보류합니다
            }
            start += frameSize
        }

        val speechRatio = if (totalCount == 0) 0.0
        else voicedCount.toDouble() / totalCount

        val voicedAvg = if (voicedCount > 0) voicedEnergy / voicedCount else 0.0
        val silentAvg = if (silentCount > 0) silentEnergy / silentCount else 0.0

        // 무음 구간이 없으면 계속 소리가 났다는 뜻이라 SNR 을 구할 수 없습니다.
        // 잡음일 수도, 쉼 없이 말했을 수도 있어 0 으로 두고 판단하지 않습니다.
        //
        // 상한을 두는 이유는 무음 구간이 몇 프레임뿐일 때
        // 분모가 우연히 작아져 값이 튀기 때문입니다.
        // 실제 마이크 녹음에서 40dB 를 넘는 SNR 은 나오지 않습니다.
        val snrDb = if (silentCount >= MIN_SILENT_FRAMES &&
            silentAvg > 1e-9 && voicedAvg > 1e-9
        ) {
            (20.0 * kotlin.math.log10(voicedAvg / silentAvg)).coerceIn(0.0, 40.0)
        } else {
            0.0
        }

        return FrameAnalysis(speechRatio, snrDb, silentCount)
    }

}