package com.isfam.core.ml

import android.content.Context
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class LocalSpeakerVerificationResult(
    val isRegisteredFamily: Boolean,
    val bestFamilyId: String?,
    val similarity: Float,
    val threshold: Float,
    val modelVersion: String,
)

/** ONNX Runtime CPU로 가족 화자 임베딩을 앱 내부에서 생성·비교합니다. */
class OnDeviceSpeakerVerifier(
    context: Context,
    private val threshold: Float = 0.65f,
) : AutoCloseable {
    companion object {
        const val MODEL_ASSET = "ecapa_tdnn_voiceprint.onnx"
        const val MODEL_VERSION = "ecapa-onnx-fp32-v1"
        private const val INPUT_NAME = "features"
    }

    private val environment = OrtEnvironment.getEnvironment()
    private val sessionOptions = OrtSession.SessionOptions().apply {
        setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
        setIntraOpNumThreads(2)
    }
    private val session = context.assets.open(MODEL_ASSET).use { input ->
        environment.createSession(input.readBytes(), sessionOptions)
    }
    private val fbank = SpeakerFbankExtractor()

    fun createEmbedding(samples: FloatArray): FloatArray {
        val features = fbank.extract(samples)
        val buffer = ByteBuffer.allocateDirect(features.size * SpeakerFbankExtractor.MEL_BINS * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        features.forEach { frame -> buffer.put(frame) }
        buffer.rewind()

        OnnxTensor.createTensor(
            environment,
            buffer,
            longArrayOf(1, features.size.toLong(), SpeakerFbankExtractor.MEL_BINS.toLong()),
        ).use { tensor ->
            session.run(mapOf(INPUT_NAME to tensor)).use { result ->
                val batch = result[0].value as Array<*>
                val embedding = (batch[0] as FloatArray).copyOf()
                require(embedding.size == 192) { "unexpected embedding size: ${embedding.size}" }
                return embedding
            }
        }
    }

    fun verify(
        samples: FloatArray,
        enrolledVoiceprints: Map<String, FloatArray>,
    ): LocalSpeakerVerificationResult {
        if (enrolledVoiceprints.isEmpty()) {
            return LocalSpeakerVerificationResult(false, null, 0f, threshold, MODEL_VERSION)
        }
        val candidate = createEmbedding(samples)
        val best = enrolledVoiceprints.maxByOrNull { (_, enrolled) -> SpeakerMath.cosine(candidate, enrolled) }
        val similarity = best?.let { SpeakerMath.cosine(candidate, it.value) } ?: 0f
        return LocalSpeakerVerificationResult(
            isRegisteredFamily = similarity >= threshold,
            bestFamilyId = best?.key,
            similarity = similarity,
            threshold = threshold,
            modelVersion = MODEL_VERSION,
        )
    }

    override fun close() {
        session.close()
        sessionOptions.close()
    }

}
