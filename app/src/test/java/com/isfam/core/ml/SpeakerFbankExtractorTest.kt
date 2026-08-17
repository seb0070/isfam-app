package com.isfam.core.ml

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class SpeakerFbankExtractorTest {
    @Test
    fun `matches SpeechBrain golden features for 440hz fixture`() {
        val samples = FloatArray(48_000) { index ->
            (0.30 * sin(2.0 * PI * 440.0 * index / 16_000.0)).toFloat()
        }
        val features = SpeakerFbankExtractor().extract(samples)

        assertEquals(301, features.size)
        assertEquals(80, features.first().size)
        assertEquals(47.7872f, features[0][0], 0.03f)
        assertEquals(58.7223f, features[0][10], 0.03f)
        assertEquals(34.9047f, features[0][40], 0.03f)
        assertEquals(21.4622f, features[0][79], 0.03f)
        assertEquals(-0.6511f, features[100][20], 0.03f)

        listOf(0, 10, 40, 79).forEach { mel ->
            assertEquals(0f, features.map { it[mel] }.average().toFloat(), 0.001f)
        }
    }
}
