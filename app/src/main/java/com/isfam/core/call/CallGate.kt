package com.isfam.core.call

/**
 * 분석 대상 판정 게이트.
 *
 * 모든 통화를 분석하지 않습니다. 저장된 지인과의 통화를 걸러내는 것이
 * 알림 피로도 문제의 해결책입니다. 부모님 통화 대부분이 여기서 제외됩니다.
 */
object CallGate {

    sealed interface Decision {
        data object Analyze : Decision
        data class Skip(val reason: SkipReason) : Decision
    }

    enum class SkipReason(val label: String) {
        Outgoing("발신 통화"),
        KnownContact("등록 가족이 아닌 저장된 연락처"),
        TooShort("통화가 너무 짧음"),
    }

    /** 이보다 짧으면 분석해도 의미 있는 결과가 나오지 않습니다 */
    private const val MIN_DURATION_SEC = 5

    /**
     * @param registeredFamilyNames 부모님 연락처에 저장된 가족 이름들.
     *        등록 시점에 사용자가 직접 지정합니다.
     *        (예: 우리 시스템의 "김서연"이 부모님 폰에는 "큰딸"로 저장됨)
     */
    fun decide(
        recording: CallRecording,
        isIncoming: Boolean,
        registeredFamilyNames: Set<String>,
    ): Decision {
        if (!isIncoming) return Decision.Skip(SkipReason.Outgoing)
        if (recording.durationSec < MIN_DURATION_SEC) {
            return Decision.Skip(SkipReason.TooShort)
        }

        return when (val identity = recording.identity) {
            // 모르는 번호 — 보이스피싱의 주 경로
            is CallerIdentity.PhoneNumber -> Decision.Analyze

            // 저장된 연락처 — 등록 가족이면 분석, 아니면 건너뜀
            is CallerIdentity.ContactName ->
                if (identity.name in registeredFamilyNames) Decision.Analyze
                else Decision.Skip(SkipReason.KnownContact)

            // 파일명을 못 읽은 경우는 보수적으로 분석
            CallerIdentity.Unknown -> Decision.Analyze
        }
    }
}
