package com.isfam.core.call

/**
 * 분석 대상 판정 게이트.
 *
 * 모든 통화를 분석하지 않습니다. 등록된 가족과 모르는 번호만 봅니다.
 * 저장된 지인(친구·직장 등)과의 통화를 걸러내는 것이
 * 알림 피로도와 배터리 문제의 해결책입니다.
 *
 * 판정 매트릭스 — 분석 통과 후 어떻게 판단하는가
 *   가족 이름 + 목소리 일치   → 안전
 *   가족 이름 + 목소리 불일치 → 위험 (번호 변작 사칭)
 *   모르는 번호 + 가족 목소리 → 위험 (목소리 복제 의심)
 *   모르는 번호 + 불일치      → 판정 보류
 *
 * 저장된 지인을 건너뛰어도 위 네 경우는 모두 잡힙니다.
 * 사기범은 가족을 사칭하거나 모르는 번호로 걸기 때문입니다.
 */
object CallGate {

    sealed interface Decision {
        data object Analyze : Decision
        data class Skip(val reason: SkipReason) : Decision
    }

    enum class SkipReason(val label: String) {
        Outgoing("발신 통화"),
        TooShort("통화가 너무 짧음"),
        KnownContact("등록 가족이 아닌 저장된 연락처"),
        UserExcluded("사용자가 자동 분석을 끈 가족"),
    }

    /** 이보다 짧으면 분석해도 의미 있는 결과가 나오지 않습니다 */
    private const val MIN_DURATION_SEC = 5

    /**
     * @param registeredFamilyNames 등록된 가족이 이 폰의 연락처에
     *        저장되어 있는 이름들.
     *
     *        ⚠️ 우리 시스템의 display_name 이 아닙니다.
     *           "김서연"이 부모님 폰에는 "큰딸"로 저장되어 있고,
     *           통화 녹음 파일명에 나오는 것은 후자입니다.
     *           서버에서 받은 가족 전화번호를 연락처에서 조회해
     *           채웁니다. (FamilyContactResolver)
     *
     * @param excludedContactNames 22번 프로필 시트에서
     *        "이 가족 통화 자동 분석"을 끈 상대
     */
    fun decide(
        recording: CallRecording,
        isIncoming: Boolean,
        registeredFamilyNames: Set<String>,
        excludedContactNames: Set<String> = emptySet(),
    ): Decision {
        // 보이스피싱은 항상 수신 전화입니다
        if (!isIncoming) return Decision.Skip(SkipReason.Outgoing)

        if (recording.durationSec < MIN_DURATION_SEC) {
            return Decision.Skip(SkipReason.TooShort)
        }

        return when (val identity = recording.identity) {
            // 모르는 번호 — 보이스피싱의 주 경로
            is CallerIdentity.PhoneNumber -> Decision.Analyze

            is CallerIdentity.ContactName -> when {
                identity.name in excludedContactNames ->
                    Decision.Skip(SkipReason.UserExcluded)

                // 등록 가족이면 분석합니다.
                // 가족 이름으로 왔는데 목소리가 다른 경우가
                // 번호 변작 사칭이므로 반드시 봐야 합니다.
                identity.name in registeredFamilyNames -> Decision.Analyze

                // 친구·직장 등 — 사칭 대상이 아니므로 건너뜁니다
                else -> Decision.Skip(SkipReason.KnownContact)
            }

            // 파일명을 읽지 못한 경우는 보수적으로 분석합니다
            CallerIdentity.Unknown -> Decision.Analyze
        }
    }
}