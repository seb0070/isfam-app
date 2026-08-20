package com.isfam.core.ml

import kotlin.math.abs

/**
 * 화자 분리 — 제외법(speaker exclusion)
 *
 * 삼성 통화 녹음은 모노 1채널이라 폰 주인과 상대방 목소리가 섞여 있습니다.
 * 그대로 임베딩을 뽑으면 두 사람의 평균이 나와 대조가 무의미해집니다.
 *
 * 일반적인 화자 분리(diarization) 모델을 쓰지 않는 이유는
 * 한쪽이 누구인지 이미 알기 때문입니다.
 *
 *   ❌ 어려운 문제: 두 화자를 찾아서 나눠라
 *   ✅ 쉬운 문제:   폰 주인 목소리를 지워라. 남은 게 상대방이다.
 *
 * 폰 주인의 성문은 목소리 등록 때 Keystore 에 저장됩니다.
 * 그래서 본인 목소리 등록이 선택이 아니라 필수입니다.
 */
class SpeakerSeparator(
    private val verifier: OnDeviceSpeakerVerifier,
) {

    companion object {
        /** 이 값 이상이면 폰 주인으로 보고 버립니다 */
        const val OWNER_HIGH = 0.55f

        /** 이 값 이하면 상대방으로 봅니다 */
        const val OWNER_LOW = 0.35f

        /** 분석 윈도우 */
        const val WINDOW_SEC = 2.0f
        const val HOP_SEC = 1.0f

        /** ECAPA 는 1.5초 미만에서 임베딩이 불안정합니다 */
        const val MIN_WINDOW_SEC = 1.5f

        /** 상대방 발화가 이보다 적으면 결과를 신뢰할 수 없습니다 */
        const val MIN_FAR_END_SEC = 3.0f

        /** 무음 구간은 건너뜁니다. ONNX 호출을 줄여 속도를 크게 높입니다 */
        private const val SILENCE_RMS = 0.008f

        private const val SAMPLE_RATE = 16_000
    }

    data class Result(
        /** 상대방 임베딩. 분리에 실패하면 null */
        val farEndEmbedding: FloatArray?,
        val ownerSec: Float,
        val farEndSec: Float,
        val ambiguousSec: Float,
        val skippedSilentSec: Float,
        /** 남은 조각들이 서로 얼마나 일관적인가. 낮으면 3자 통화 의심 */
        val coherence: Float,
        val processedWindows: Int,
        val elapsedMs: Long,
    ) {
        val usable: Boolean
            get() = farEndEmbedding != null &&
                    farEndSec >= MIN_FAR_END_SEC &&
                    coherence >= 0.6f

        fun summary(): String = buildString {
            append("주인 %.1f초 · 상대 %.1f초 · 애매 %.1f초 · 무음 %.1f초"
                .format(ownerSec, farEndSec, ambiguousSec, skippedSilentSec))
            append("\n윈도우 ${processedWindows}개 · ${elapsedMs}ms")
            append(" · 일관성 %.2f".format(coherence))
            append(if (usable) " · 사용 가능" else " · 신뢰 불가")
        }
    }

    /**
     * @param samples 16kHz 모노 float32
     * @param ownerEmbedding 폰 주인의 등록 성문
     */
    fun separate(samples: FloatArray, ownerEmbedding: FloatArray): Result {
        val started = System.currentTimeMillis()

        val win = (WINDOW_SEC * SAMPLE_RATE).toInt()
        val hop = (HOP_SEC * SAMPLE_RATE).toInt()
        val minWin = (MIN_WINDOW_SEC * SAMPLE_RATE).toInt()

        val farEndEmbeddings = ArrayList<FloatArray>()
        var ownerSec = 0f
        var farEndSec = 0f
        var ambiguousSec = 0f
        var silentSec = 0f
        var processed = 0

        var start = 0
        while (start + minWin <= samples.size) {
            val end = minOf(start + win, samples.size)
            val chunk = samples.copyOfRange(start, end)
            val chunkSec = chunk.size.toFloat() / SAMPLE_RATE

            // 무음이면 ONNX 를 돌리지 않습니다.
            // 통화의 30~50%가 무음이라 이 한 줄이 속도를 크게 좌우합니다.
            if (rms(chunk) < SILENCE_RMS) {
                silentSec += chunkSec
                start += hop
                continue
            }

            val embedding = verifier.createEmbedding(chunk)
            processed++

            when (val similarity = SpeakerMath.cosine(embedding, ownerEmbedding)) {
                in OWNER_HIGH..1f -> ownerSec += chunkSec
                in -1f..OWNER_LOW -> {
                    farEndEmbeddings += embedding
                    farEndSec += chunkSec
                }
                // 임계값 사이는 두 사람이 겹쳐 말한 구간일 가능성이 높아 버립니다.
                // 하나의 임계값만 쓰면 이런 구간이 잘못 섞여 들어갑니다.
                else -> {
                    ambiguousSec += chunkSec
                    @Suppress("UNUSED_EXPRESSION") similarity
                }
            }

            start += hop
        }

        if (farEndEmbeddings.isEmpty()) {
            return Result(
                farEndEmbedding = null,
                ownerSec = ownerSec, farEndSec = 0f,
                ambiguousSec = ambiguousSec, skippedSilentSec = silentSec,
                coherence = 0f, processedWindows = processed,
                elapsedMs = System.currentTimeMillis() - started,
            )
        }

        val mean = SpeakerMath.averageAndNormalize(farEndEmbeddings)

        // 남은 조각들이 정말 한 사람인지 확인합니다.
        // 서로 안 닮은 조각이 섞여 있으면 3자 통화이거나 분리가 실패한 것입니다.
        val coherence = farEndEmbeddings
            .map { SpeakerMath.cosine(it, mean) }
            .average()
            .toFloat()

        return Result(
            farEndEmbedding = mean,
            ownerSec = ownerSec,
            farEndSec = farEndSec,
            ambiguousSec = ambiguousSec,
            skippedSilentSec = silentSec,
            coherence = coherence,
            processedWindows = processed,
            elapsedMs = System.currentTimeMillis() - started,
        )
    }

    private fun rms(x: FloatArray): Float {
        if (x.isEmpty()) return 0f
        var acc = 0.0
        for (v in x) acc += v.toDouble() * v
        return kotlin.math.sqrt(acc / x.size).toFloat()
    }
}