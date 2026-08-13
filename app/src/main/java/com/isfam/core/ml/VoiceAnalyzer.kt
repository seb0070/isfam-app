package com.isfam.core.ml

/**
 * ══════════════════════════════════════════════════════════════
 *  ML 레이어 계약
 *
 *  안드로이드 UI 코드는 이 인터페이스만 압니다.
 *  서버에서 분석하든 폰에서 분석하든 화면 코드는 바뀌지 않습니다.
 *
 *    P1 (지금)  ServerVoiceAnalyzer   — 오디오 업로드 → 서버 응답
 *    P3 (나중)  OnDeviceVoiceAnalyzer — ONNX 추론
 *
 *  ML 담당은 P3 시점에 OnDeviceVoiceAnalyzer 만 구현하면 됩니다.
 * ══════════════════════════════════════════════════════════════
 */
interface VoiceAnalyzer {
    suspend fun analyze(request: AnalysisRequest): AnalysisResult
}

// ─── 입력 ─────────────────────────────────────────────────────

data class AnalysisRequest(
    /** 전처리가 끝난 오디오. 규격은 PreprocessSpec 참고. */
    val audio: AudioSample,

    /**
     * 폰 주인(부모님)의 등록 임베딩.
     * 화자 분리에 필수입니다 — 주인 목소리를 제거해야 상대방이 남습니다.
     * null 이면 화자 분리를 할 수 없으므로 분석을 진행하면 안 됩니다.
     */
    val ownerEmbedding: FloatArray?,

    /** 등록된 가족 전원. 1:N 대조 대상 (주인 본인 제외). */
    val familyEmbeddings: List<FamilyEmbedding>,

    /** 파일명에서 파싱한 발신자 정보 */
    val claimedIdentity: ClaimedIdentity,
)

data class AudioSample(
    /** float32, 범위 [-1.0, 1.0] */
    val samples: FloatArray,
    val sampleRate: Int = 16_000,
    val channels: Int = 1,
) {
    val durationSec: Float get() = samples.size.toFloat() / sampleRate
}

data class FamilyEmbedding(
    val familyMemberId: Int,
    val displayName: String,
    /** ECAPA-TDNN 출력. 차원은 모델에 따름 (192 또는 256) */
    val vector: FloatArray,
    /**
     * 이 임베딩이 코덱 열화(AMR-WB)를 거친 버전인지.
     * 통화 음성은 코덱을 거쳐 왜곡되므로, 깨끗한 등록 음성과
     * 직접 비교하면 같은 사람도 점수가 낮게 나옵니다.
     * 두 버전을 모두 두고 높은 쪽을 채택합니다.
     */
    val isCodecSimulated: Boolean = false,
)

/**
 * 파일명이 알려주는 "이 전화가 누구라고 주장하는가".
 *
 * 실측 결과:
 *   통화 막내딸_260809_133433.m4a       → ContactName("막내딸")
 *   통화 01076352857_260809_135255.m4a  → PhoneNumber("01076352857")
 */
sealed interface ClaimedIdentity {
    /** 연락처에 저장된 상대. 번호를 알 수 없음 → 차단 기능 비활성 */
    data class ContactName(val name: String) : ClaimedIdentity

    /** 저장되지 않은 번호 = 모르는 번호 확정 → 차단 가능 */
    data class PhoneNumber(val number: String) : ClaimedIdentity

    data object Unknown : ClaimedIdentity
}

// ─── 출력 ─────────────────────────────────────────────────────

data class AnalysisResult(
    val decision: Decision,
    /** 0~100. UI 표시용 */
    val riskScore: Int,
    /** 사용자에게 보여줄 판정 사유 (한 문장) */
    val reason: String,

    /** 가장 닮은 가족. 아무와도 안 닮으면 null */
    val matchedFamilyId: Int?,
    val similarityScore: Float,
    /** 딥보이스 의심도. 미측정이면 null */
    val spoofScore: Float?,

    /** 화자 분리 진단 — 결과를 신뢰할 수 있는지 판단하는 근거 */
    val separation: SeparationInfo,

    val modelVersion: String,
    val elapsedMs: Long,
)

enum class Decision {
    /** 🟢 등록된 가족의 실제 목소리 */
    SAFE,

    /** 🟡 확인 필요 */
    CAUTION,

    /** 🔴 위험 — 알림 발송 */
    DANGER,

    /**
     * ⚪ 분석했으나 신뢰 불가.
     * 음질 미달, 상대 발화 부족, 화자 분리 실패 등.
     * 불충분한 근거로 "안전" 또는 "위험"을 단정하지 않기 위한 값입니다.
     */
    INSUFFICIENT,

    /** ⚪ 게이트에서 걸러짐 — 저장된 지인 전화. 분석 자체를 하지 않음 */
    SKIPPED,
}

data class SeparationInfo(
    val ownerSpeechSec: Float,
    val farEndSpeechSec: Float,
    val ambiguousSec: Float,
    val succeeded: Boolean,
) {
    /** 상대방 발화가 3초 미만이면 임베딩을 신뢰할 수 없습니다 */
    val hasEnoughFarEndSpeech: Boolean get() = farEndSpeechSec >= 3.0f
}

// ─── 게이트 ───────────────────────────────────────────────────

/**
 * 분석 전에 "이 통화를 볼 것인가"를 정합니다.
 * 이 판단은 안드로이드에서 하며, ML 은 관여하지 않습니다.
 *
 * 저장된 지인 전화를 걸러내는 것이 알림 피로도 해결책입니다.
 */
object AnalysisGate {
    fun shouldAnalyze(
        identity: ClaimedIdentity,
        registeredFamilyNames: Set<String>,
        isIncoming: Boolean,
    ): Boolean {
        if (!isIncoming) return false           // 발신 통화는 분석 안 함
        return when (identity) {
            is ClaimedIdentity.PhoneNumber -> true                       // 모르는 번호
            is ClaimedIdentity.ContactName -> identity.name in registeredFamilyNames
            ClaimedIdentity.Unknown -> true
        }
    }
}

// ─── 전처리 규격 ──────────────────────────────────────────────

/**
 * ⚠️ ML 담당과 반드시 동일해야 하는 값들.
 *
 * Python(torchaudio) 과 Kotlin(MediaCodec) 은 서로 다른 구현이므로,
 * 이 값이 하나라도 어긋나면 같은 오디오에서 다른 임베딩이 나옵니다.
 * 서버에서 잘 되던 모델이 폰에서 정확도가 떨어지는 가장 흔한 원인입니다.
 */
object PreprocessSpec {
    const val SAMPLE_RATE = 16_000
    const val CHANNELS = 1

    /** 앞부분만 분석. 19분 통화를 전부 처리할 수 없습니다. */
    const val MAX_ANALYSIS_SEC = 90

    /** 화자 분리 윈도우 */
    const val WINDOW_SEC = 2.0f
    const val HOP_SEC = 1.0f

    /** 이보다 짧은 조각은 임베딩이 불안정해 버립니다 */
    const val MIN_WINDOW_SEC = 1.5f

    /** 음질 게이트 (서버 구현과 동일) */
    const val MIN_DURATION_SEC = 2.0f
    const val MIN_RMS = 0.005f
    const val MIN_SPEECH_RATIO = 0.25f

    /** 화자 분리 임계값 — 서버에서 튜닝 후 확정할 것 */
    const val OWNER_HIGH = 0.55f    // 이상이면 주인 목소리로 보고 제거
    const val OWNER_LOW = 0.35f     // 이하면 상대방. 사이는 애매 → 버림
}

// ─── P1 구현 (서버) ───────────────────────────────────────────

/**
 * 지금 사용할 구현.
 * 오디오를 서버로 보내고 결과를 받습니다.
 *
 * 이 방식은 임시가 아니라 정식 폴백 경로로 유지합니다.
 * 저사양 기기에서 온디바이스 성능이 부족하면 그대로 씁니다.
 */
// class ServerVoiceAnalyzer(private val api: IsFamApi) : VoiceAnalyzer { ... }

// ─── P3 구현 (온디바이스) ─────────────────────────────────────

/**
 * ML 담당이 나중에 구현할 부분.
 * ONNX Runtime Mobile 로 ECAPA-TDNN 을 실행합니다.
 *
 * 필요한 것:
 *   - ECAPA-TDNN ONNX 파일 (int8 양자화)
 *   - 입력 텐서 규격 (shape, dtype)
 *   - 출력 임베딩 차원
 *   - Python 전처리와 일치함을 증명하는 골든 테스트 벡터
 */
// class OnDeviceVoiceAnalyzer(private val session: OrtSession) : VoiceAnalyzer { ... }
