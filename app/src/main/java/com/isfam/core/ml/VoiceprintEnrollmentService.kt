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

    suspend fun enroll(
        familyId: String,
        audioFiles: List<File>,
        onProgress: (completed: Int, total: Int) -> Unit = { _, _ -> },
    ): FloatArray {
        require(audioFiles.isNotEmpty()) { "등록 음성이 없습니다." }
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
            verifier.createEmbedding(samples).also { onProgress(index + 1, audioFiles.size) }
        }
        val voiceprint = SpeakerMath.averageAndNormalize(embeddings)
        store.save(familyId, voiceprint)
        return voiceprint
    }
}