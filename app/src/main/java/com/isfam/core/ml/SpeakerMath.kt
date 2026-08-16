package com.isfam.core.ml

import kotlin.math.sqrt

object SpeakerMath {
    fun cosine(left: FloatArray, right: FloatArray): Float {
        require(left.size == right.size && left.isNotEmpty()) { "embedding dimensions differ" }
        var dot = 0.0
        var leftNorm = 0.0
        var rightNorm = 0.0
        for (index in left.indices) {
            dot += left[index] * right[index]
            leftNorm += left[index] * left[index]
            rightNorm += right[index] * right[index]
        }
        val denominator = sqrt(leftNorm) * sqrt(rightNorm)
        return if (denominator == 0.0) 0f else (dot / denominator).toFloat().coerceIn(-1f, 1f)
    }

    fun averageAndNormalize(embeddings: List<FloatArray>): FloatArray {
        require(embeddings.isNotEmpty())
        val size = embeddings.first().size
        require(size > 0 && embeddings.all { it.size == size })
        val average = FloatArray(size)
        embeddings.forEach { embedding ->
            for (index in average.indices) average[index] += embedding[index] / embeddings.size
        }
        val norm = sqrt(average.fold(0.0) { acc, value -> acc + value * value }).toFloat()
        require(norm > 0f) { "voiceprint norm is zero" }
        for (index in average.indices) average[index] /= norm
        return average
    }
}
