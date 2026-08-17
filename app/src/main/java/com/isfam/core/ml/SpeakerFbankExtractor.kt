package com.isfam.core.ml

import org.jtransforms.fft.FloatFFT_1D
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * SpeechBrain spkrec-ecapa-voxceleb의 FBank 전처리를 Kotlin으로 재현합니다.
 * 모델 입력은 [1, frames, 80]이고 각 mel bin은 발화 단위 평균 정규화됩니다.
 */
class SpeakerFbankExtractor {
    companion object {
        const val SAMPLE_RATE = 16_000
        const val FFT_SIZE = 400
        const val HOP_SIZE = 160
        const val MEL_BINS = 80
        private const val TOP_DB = 80.0
        private const val MIN_POWER = 1e-10
    }

    private val fft = FloatFFT_1D(FFT_SIZE.toLong())
    private val window = FloatArray(FFT_SIZE) { index ->
        // torch.hamming_window(400, periodic=true)
        (0.54 - 0.46 * cos(2.0 * PI * index / FFT_SIZE)).toFloat()
    }
    private val filters = createMelFilters()

    /** 입력은 mono 16 kHz, [-1, 1] 범위 PCM입니다. */
    @Synchronized
    fun extract(samples: FloatArray): Array<FloatArray> {
        require(samples.isNotEmpty()) { "audio samples must not be empty" }

        // torch.stft(center=true): 양 끝에 FFT_SIZE/2 만큼 zero padding.
        val frameCount = samples.size / HOP_SIZE + 1
        val features = Array(frameCount) { FloatArray(MEL_BINS) }
        val fftBuffer = FloatArray(FFT_SIZE * 2)
        var globalMaxDb = Float.NEGATIVE_INFINITY

        for (frame in 0 until frameCount) {
            fftBuffer.fill(0f)
            val start = frame * HOP_SIZE - FFT_SIZE / 2
            for (index in 0 until FFT_SIZE) {
                val sourceIndex = start + index
                if (sourceIndex in samples.indices) {
                    fftBuffer[index] = samples[sourceIndex] * window[index]
                }
            }
            fft.realForwardFull(fftBuffer)

            for (mel in 0 until MEL_BINS) {
                var melPower = 0.0
                for (bin in 0..FFT_SIZE / 2) {
                    val real = fftBuffer[bin * 2].toDouble()
                    val imaginary = fftBuffer[bin * 2 + 1].toDouble()
                    melPower += (real * real + imaginary * imaginary) * filters[bin][mel]
                }
                val db = (10.0 * log10(max(melPower, MIN_POWER))).toFloat()
                features[frame][mel] = db
                if (db > globalMaxDb) globalMaxDb = db
            }
        }

        val floorDb = globalMaxDb - TOP_DB.toFloat()
        for (frame in features.indices) {
            for (mel in 0 until MEL_BINS) {
                features[frame][mel] = max(features[frame][mel], floorDb)
            }
        }

        // SpeechBrain InputNormalization(norm_type="sentence", std_norm=false)
        for (mel in 0 until MEL_BINS) {
            var sum = 0.0
            for (frame in features.indices) sum += features[frame][mel]
            val mean = (sum / frameCount).toFloat()
            for (frame in features.indices) features[frame][mel] -= mean
        }
        return features
    }

    private fun createMelFilters(): Array<FloatArray> {
        val minMel = hzToMel(0.0)
        val maxMel = hzToMel(SAMPLE_RATE / 2.0)
        val hzPoints = DoubleArray(MEL_BINS + 2) { index ->
            val mel = minMel + (maxMel - minMel) * index / (MEL_BINS + 1)
            melToHz(mel)
        }
        val centers = DoubleArray(MEL_BINS) { hzPoints[it + 1] }
        val bands = DoubleArray(MEL_BINS) { hzPoints[it + 1] - hzPoints[it] }

        return Array(FFT_SIZE / 2 + 1) { bin ->
            val frequency = SAMPLE_RATE.toDouble() * bin / FFT_SIZE
            FloatArray(MEL_BINS) { mel ->
                val slope = (frequency - centers[mel]) / bands[mel]
                max(0.0, min(slope + 1.0, -slope + 1.0)).toFloat()
            }
        }
    }

    private fun hzToMel(hz: Double): Double = 2595.0 * log10(1.0 + hz / 700.0)
    private fun melToHz(mel: Double): Double = 700.0 * (10.0.pow(mel / 2595.0) - 1.0)
}
