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
            require(samples.size >= SpeakerFbankExtractor.SAMPLE_RATE * 2) {
                "${file.name}: 2초 이상의 음성이 필요합니다."
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
