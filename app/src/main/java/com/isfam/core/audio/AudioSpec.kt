package com.isfam.core.audio

/**
 * 오디오 전처리 규격.
 *
 * ⚠️ ML 담당과 반드시 동일해야 하는 값들입니다.
 *
 * Python(torchaudio/librosa) 과 Kotlin(MediaCodec) 은 서로 다른 구현이라
 * 하나라도 어긋나면 같은 오디오에서 다른 임베딩이 나옵니다.
 * 서버에서 잘 되던 모델이 폰에서 정확도가 떨어지는 가장 흔한 원인입니다.
 *
 * 검증 방법 — 골든 테스트 벡터
 *   ① ML 담당이 고정 wav 5개를 Python 파이프라인에 통과시켜 임베딩 저장
 *   ② 같은 파일을 이 파이프라인에 통과
 *   ③ 코사인 유사도 > 0.99 면 통과
 */
object AudioSpec {

    const val SAMPLE_RATE = 16_000
    const val CHANNELS = 1

    /**
     * 분석 길이 상한.
     *
     * 실기기에서 19분짜리 통화가 관측되었습니다. 전체를 처리하면
     * 디코딩만 약 50초, 2초 윈도우 570개에 추론이 필요해 감당할 수 없습니다.
     *
     * 가족 사칭은 통화 시작에 일어납니다. "엄마 나야"는 첫 10초 안에
     * 나오지 19분째에 나오지 않습니다.
     *
     * 부수 효과 — 통화 전체를 훑지 않는다는 점은 프라이버시 논거로도
     * 활용할 수 있습니다.
     */
    const val MAX_ANALYSIS_SEC = 90

    /** 화자 분리 윈도우 */
    const val WINDOW_SEC = 2.0f
    const val HOP_SEC = 1.0f

    /** ECAPA 는 1.5초 미만에서 임베딩이 불안정해집니다 */
    const val MIN_WINDOW_SEC = 1.5f

    // ── 음질 게이트 (서버 구현과 동일한 값) ────────────────────
    const val MIN_DURATION_SEC = 2.0f
    const val MIN_RMS = 0.005f
    const val MIN_SPEECH_RATIO = 0.25f

    /** 발화 비율 계산 프레임 크기 */
    const val VAD_FRAME_MS = 20
    const val VAD_THRESHOLD = 0.01f

    /**
     * 정규화 방식.
     *
     * ⚠️ ML 담당 회신 대기 중입니다.
     *    peak / RMS / 안 함 중 무엇인지 확정되면 바꿔야 합니다.
     *    지금은 통화 녹음이 대체로 조용해 peak 정규화를 기본으로 둡니다.
     */
    val NORMALIZATION = Normalization.Peak

    enum class Normalization {
        /** 최대 진폭을 TARGET_PEAK 에 맞춤 */
        Peak,
        /** RMS 를 TARGET_RMS 에 맞춤 */
        Rms,
        None,
    }

    const val TARGET_PEAK = 0.95f
    const val TARGET_RMS = 0.06f
}
