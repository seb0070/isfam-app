package com.isfam.core.audio

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/**
 * 저역통과 필터가 적용된 리샘플러.
 *
 * ⚠️ 선형 보간을 쓰면 안 됩니다.
 *
 * 48kHz → 16kHz 는 3:1 다운샘플링입니다. 나이퀴스트 주파수가
 * 24kHz 에서 8kHz 로 낮아지므로, 8kHz 를 넘는 성분을 먼저 제거하지 않으면
 * 그 성분이 8kHz 아래로 "접혀 들어옵니다"(앨리어싱).
 *
 * 접혀 들어온 성분은 원래 신호와 구분할 수 없어 되돌릴 수 없고,
 * 화자 검증 임베딩을 왜곡시킵니다. 이 상태로 모델에 넣으면
 * 정확도가 낮게 나오는데 원인을 모델로 오해하기 쉽습니다.
 *
 * 구현: windowed-sinc 보간 (Lanczos)
 *   sinc 함수가 이상적인 저역통과 필터이고,
 *   Lanczos 창으로 잘라 유한 길이로 만듭니다.
 *   다운샘플링일 때는 cutoff 를 낮춰 필터와 보간을 동시에 수행합니다.
 */
object Resampler {

    /**
     * 커널 반폭(lobe 수).
     * 클수록 정확하지만 느립니다. 3이면 음성에 충분합니다.
     */
    private const val LOBES = 3

    /**
     * @param input  float32 PCM, 범위 [-1.0, 1.0]
     * @param fromHz 원본 샘플레이트
     * @param toHz   목표 샘플레이트
     */
    fun resample(input: FloatArray, fromHz: Int, toHz: Int): FloatArray {
        if (input.isEmpty()) return FloatArray(0)
        if (fromHz == toHz) return input.copyOf()

        val ratio = toHz.toDouble() / fromHz
        val outputSize = (input.size * ratio).toInt()
        if (outputSize <= 0) return FloatArray(0)

        val output = FloatArray(outputSize)

        // 다운샘플링이면 목표 나이퀴스트에 맞춰 cutoff 를 낮춥니다.
        // 이것이 저역통과 필터 역할을 합니다.
        val cutoff = if (ratio < 1.0) ratio else 1.0

        // 커널 폭도 cutoff 에 반비례해 넓어집니다
        val filterScale = 1.0 / cutoff
        val halfWidth = LOBES * filterScale

        for (i in 0 until outputSize) {
            // 출력 i 에 대응하는 입력 위치 (실수)
            val center = i / ratio

            val start = kotlin.math.ceil(center - halfWidth).toInt()
            val end = kotlin.math.floor(center + halfWidth).toInt()

            var sum = 0.0
            var weightSum = 0.0

            for (j in start..end) {
                if (j < 0 || j >= input.size) continue

                val distance = (center - j) * cutoff
                val weight = lanczosWeight(distance)
                if (weight == 0.0) continue

                sum += input[j] * weight
                weightSum += weight
            }

            output[i] = if (weightSum > 0) (sum / weightSum).toFloat() else 0f
        }

        return output
    }

    /** Lanczos 창을 씌운 sinc. |x| >= LOBES 이면 0 */
    private fun lanczosWeight(x: Double): Double {
        val ax = abs(x)
        if (ax >= LOBES) return 0.0
        if (ax < 1e-9) return 1.0
        return sinc(x) * sinc(x / LOBES)
    }

    /** 정규화 sinc — sin(πx) / (πx) */
    private fun sinc(x: Double): Double {
        val px = PI * x
        return sin(px) / px
    }
}
