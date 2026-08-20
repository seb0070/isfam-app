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

            // 서버는 sentence_id 를 1부터 셉니다
            val sentenceId = index + 1
            perSentence[sentenceId] = embedding
            quality[sentenceId] = SampleQuality(
                durationSeconds = samples.size.toDouble() / SpeakerFbankExtractor.SAMPLE_RATE,
                rmsEnergy = rms,
                peakAmplitude = samples.maxOf { kotlin.math.abs(it) }.toDouble(),
            )

            onProgress(sentenceId, audioFiles.size)
            embedding
        }

        val voiceprint = SpeakerMath.averageAndNormalize(embeddings)
        store.save(familyId, voiceprint)

        return Result(voiceprint, perSentence, quality)
    }
}