package com.isfam.core.ml

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.sqrt

class SpeakerMathTest {
    @Test
    fun `cosine handles same opposite and orthogonal embeddings`() {
        assertEquals(1f, SpeakerMath.cosine(floatArrayOf(1f, 2f), floatArrayOf(1f, 2f)), 1e-6f)
        assertEquals(-1f, SpeakerMath.cosine(floatArrayOf(1f, 0f), floatArrayOf(-1f, 0f)), 1e-6f)
        assertEquals(0f, SpeakerMath.cosine(floatArrayOf(1f, 0f), floatArrayOf(0f, 1f)), 1e-6f)
    }

    @Test
    fun `averaged voiceprint is unit normalized`() {
        val result = SpeakerMath.averageAndNormalize(
            listOf(floatArrayOf(1f, 0f), floatArrayOf(0f, 1f))
        )
        val expected = (1.0 / sqrt(2.0)).toFloat()
        assertEquals(expected, result[0], 1e-6f)
        assertEquals(expected, result[1], 1e-6f)
    }
}
