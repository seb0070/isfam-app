package com.isfam.core.audio

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/**
 * 리샘플러 자체 검증.
 *
 * 골든 테스트 벡터(ML 담당 제공)가 오기 전까지 쓰는 임시 검증입니다.
 * 앨리어싱이 실제로 억제되는지 확인합니다.
 *
 * 검증 원리
 *   48kHz 에서 12kHz 사인파를 만들어 16kHz 로 다운샘플링합니다.
 *   12kHz 는 16kHz 의 나이퀴스트(8kHz)를 넘으므로 제거되어야 합니다.
 *
 *   저역통과 필터가 있으면  → 거의 무음 (RMS 낮음)
 *   선형 보간이면           → 4kHz 로 접혀 들어옴 (RMS 높음)
 *
 * 사용법 — 디버그 빌드에서 한 번 호출해 로그를 확인하세요.
 *   AudioSelfTest.run()?.let { Log.d("IsFam", it) }
 */
object AudioSelfTest {

    fun run(): String = buildString {
        appendLine("── 리샘플러 검증 ──")
        appendLine(aliasingTest())
        appendLine(passbandTest())
        appendLine(lengthTest())
    }

    /** 나이퀴스트를 넘는 성분이 제거되는가 */
    private fun aliasingTest(): String {
        val input = sineWave(freqHz = 12_000, sampleRate = 48_000, seconds = 0.5f)
        val output = Resampler.resample(input, 48_000, 16_000)
        val rms = AudioPipeline.rms(output)

        // 제대로 필터링되면 원본 대비 크게 줄어듭니다
        val passed = rms < 0.05f
        return "12kHz 억제: RMS %.4f  %s".format(rms, if (passed) "통과" else "실패")
    }

    /** 통과 대역 신호는 보존되는가 */
    private fun passbandTest(): String {
        val input = sineWave(freqHz = 1_000, sampleRate = 48_000, seconds = 0.5f)
        val output = Resampler.resample(input, 48_000, 16_000)
        val inRms = AudioPipeline.rms(input)
        val outRms = AudioPipeline.rms(output)

        val ratio = if (inRms > 0) outRms / inRms else 0f
        val passed = abs(ratio - 1f) < 0.1f
        return "1kHz 보존: 비율 %.3f  %s".format(ratio, if (passed) "통과" else "실패")
    }

    /** 출력 길이가 비율에 맞는가 */
    private fun lengthTest(): String {
        val input = FloatArray(48_000)
        val output = Resampler.resample(input, 48_000, 16_000)
        val expected = 16_000
        val passed = abs(output.size - expected) <= 2
        return "길이: ${output.size} (기대 $expected)  ${if (passed) "통과" else "실패"}"
    }

    private fun sineWave(freqHz: Int, sampleRate: Int, seconds: Float): FloatArray {
        val n = (sampleRate * seconds).toInt()
        return FloatArray(n) { i ->
            sin(2.0 * PI * freqHz * i / sampleRate).toFloat() * 0.8f
        }
    }
}
