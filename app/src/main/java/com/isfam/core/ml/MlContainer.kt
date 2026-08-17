package com.isfam.core.ml

import android.content.Context

/**
 * ML 레이어 접근점.
 *
 * ONNX 세션은 생성 비용이 크고(모델 21MB) 스레드 안전하므로
 * 앱 전체에서 하나만 만들어 재사용합니다.
 * 화면마다 새로 만들면 메모리와 시간이 낭비됩니다.
 *
 * AppContainer 에서 한 번 만들고 필요한 곳에 넘깁니다.
 */
class MlContainer(private val context: Context) {

    /** ONNX 세션. 최초 접근 시 모델을 로드합니다. */
    val speakerVerifier: OnDeviceSpeakerVerifier by lazy {
        OnDeviceSpeakerVerifier(context)
    }

    val voiceprintStore: EncryptedVoiceprintStore by lazy {
        EncryptedVoiceprintStore(context)
    }

    val audioDecoder: AndroidAudioDecoder by lazy {
        AndroidAudioDecoder()
    }

    val enrollmentService: VoiceprintEnrollmentService by lazy {
        VoiceprintEnrollmentService(audioDecoder, speakerVerifier, voiceprintStore)
    }

    val speakerSeparator: SpeakerSeparator by lazy {
        SpeakerSeparator(speakerVerifier)
    }

    /** 앱 종료 시 ONNX 세션 해제 */
    fun close() {
        runCatching { speakerVerifier.close() }
    }
}
