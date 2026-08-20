package com.isfam.core.ml

import com.isfam.core.call.CallerIdentity

/**
 * 통화 위험도 판정.
 *
 * 서버 risk_scoring_service 의 정책을 온디바이스로 옮긴 것입니다.
 * 두 경로의 판정이 갈리면 사용자가 혼란스러우므로 기준을 맞춥니다.
 *
 * 서버 정책 (README 참고)
 *   등록 가족 similarity >= 기준값      → 안전 후보
 *   기준값에 근접                        → 확인 필요
 *   기준값보다 크게 낮음                 → AI 여부와 무관하게 위험
 *
 * 여기에 IsFam 앱만의 축을 하나 더 씁니다.
 *
 *   발신자가 누구라고 주장하는가 (통화 녹음 파일명에서 파싱)
 *
 * 서버는 오디오만 받으므로 이 정보를 쓸 수 없습니다.
 * 발신번호 변작 사칭은 이 축이 있어야 잡힙니다.
 */
object CallVerdict {

    enum class Level { SAFE, CAUTION, DANGER, INSUFFICIENT }

    // ── 임계값 ────────────────────────────────────────────────
    //
    // 서버 실측 기준 (등록 가족 음성 10개, 화자 2명)
    //   같은 화자 최소 유사도  0.6676
    //   다른 화자 최대 유사도  0.5021
    //   분리 여유도            0.1655
    //   threshold 0.65 → 정확도 100%, FAR 0%, FRR 0%
    //
    // ⚠️ 하나의 임계값으로 자르면 0.649와 0.651이 정반대 판정이 됩니다.
    //    경계선 구간을 따로 두어 "확인 필요"로 보냅니다.

    /** 이 값 이상이면 등록 가족으로 인정 (ISFAM_SPEAKER_THRESHOLD) */
    const val MATCH_THRESHOLD = 0.65f

    /** 이 값과 MATCH_THRESHOLD 사이는 판단하기 애매한 구간 */
    const val NEAR_THRESHOLD = 0.55f

    /** 딥보이스 의심 기준 (ISFAM_ANTI_SPOOFING_THRESHOLD) */
    const val SPOOF_THRESHOLD = 0.50f

    /** 이 값을 넘으면 다른 신호와 무관하게 위험 */
    const val STRONG_SPOOF = 0.80f

    /** 유사도 구간 */
    private enum class Match { Matched, Near, Mismatched }

    private fun matchOf(similarity: Float): Match = when {
        similarity >= MATCH_THRESHOLD -> Match.Matched
        similarity >= NEAR_THRESHOLD -> Match.Near
        else -> Match.Mismatched
    }

    data class Result(
        val level: Level,
        /** 0~100 */
        val riskScore: Float,
        /** 판단 근거. 화면과 알림에 그대로 씁니다 */
        val reasons: List<String>,
        /** ⚠️ 서버로 보내면 안 됩니다 — 발신자 식별 결과 */
        val matchedFamilyId: String?,
        val similarity: Float,
        val spoofScore: Float?,
    ) {
        /**
         * 서버로 보낼 수 있는 값만 추립니다.
         *
         * similarity 와 matchedFamilyId 는 통화 상대방의 생체정보
         * 처리 결과입니다. 동의를 받을 방법이 없어 로컬에만 둡니다.
         */
        fun toServerPayload(): Map<String, Any> = mapOf(
            "risk_level" to riskScore,
            "final_decision" to when (level) {
                Level.SAFE -> "normal"
                Level.CAUTION, Level.INSUFFICIENT -> "needs_check"
                Level.DANGER -> "danger"
            },
        )
    }

    fun decide(
        identity: CallerIdentity,
        matchedFamilyId: String?,
        matchedFamilyName: String?,
        similarity: Float,
        spoofScore: Float?,
        spoofReliable: Boolean,
        separationUsable: Boolean,
    ): Result {
        // 화자 분리에 실패하면 어떤 판정도 신뢰할 수 없습니다.
        // 불충분한 근거로 안전이나 위험을 단정하지 않습니다.
        if (!separationUsable) {
            return Result(
                level = Level.INSUFFICIENT,
                riskScore = 0f,
                reasons = listOf("상대방 목소리가 충분히 담기지 않아 판단을 보류했어요"),
                matchedFamilyId = null,
                similarity = similarity,
                spoofScore = spoofScore,
            )
        }

        val reasons = mutableListOf<String>()
        val match = matchOf(similarity)

        val spoof = if (spoofReliable) spoofScore else null
        val spoofSuspicious = spoof != null && spoof >= SPOOF_THRESHOLD
        val spoofStrong = spoof != null && spoof >= STRONG_SPOOF

        if (spoofScore != null && !spoofReliable) {
            reasons += "음질이 불안정해 합성 여부는 참고만 했어요"
        }

        val level = when (identity) {
            is CallerIdentity.ContactName ->
                decideForContact(identity.name, match, spoofSuspicious, spoofStrong, reasons)

            is CallerIdentity.PhoneNumber ->
                decideForUnknownNumber(match, matchedFamilyName, spoofSuspicious, spoofStrong, reasons)

            CallerIdentity.Unknown ->
                decideForUnidentified(match, spoofSuspicious, reasons)
        }

        return Result(
            level = level,
            riskScore = riskScore(level, similarity, spoof),
            reasons = reasons,
            matchedFamilyId = matchedFamilyId.takeIf { match == Match.Matched },
            similarity = similarity,
            spoofScore = spoofScore,
        )
    }

    /**
     * 연락처에 저장된 이름으로 걸려온 경우.
     *
     * 저장된 번호로 왔는데 목소리가 다르면 발신번호 변작 사칭입니다.
     * 정상적인 설명이 거의 없어 위험으로 봅니다.
     */
    private fun decideForContact(
        name: String,
        match: Match,
        spoofSuspicious: Boolean,
        spoofStrong: Boolean,
        reasons: MutableList<String>,
    ): Level = when (match) {

        Match.Matched -> {
            reasons += "${name}님의 목소리와 일치해요"
            if (spoofSuspicious) {
                // 목소리는 맞는데 합성 흔적이 있다 = 복제 의심
                reasons += "다만 합성 음성 흔적이 감지됐어요"
                if (spoofStrong) Level.DANGER else Level.CAUTION
            } else {
                Level.SAFE
            }
        }

        Match.Near -> {
            // 경계 구간. 단정하지 않고 사용자에게 넘깁니다.
            reasons += "${name}님의 목소리와 비슷하지만 확실하지 않아요"
            if (spoofSuspicious) {
                reasons += "합성 음성 흔적도 함께 감지됐어요"
                Level.DANGER
            } else {
                Level.CAUTION
            }
        }

        Match.Mismatched -> {
            reasons += "${name}님 번호로 왔지만 목소리가 달라요"
            reasons += "발신번호를 바꿔 거는 수법일 수 있어요"
            Level.DANGER
        }
    }

    /**
     * 저장되지 않은 번호로 걸려온 경우.
     *
     * ⚠️ "모르는 번호 + 가족 목소리"를 곧바로 위험으로 보면 안 됩니다.
     *    공중전화, 회사 전화, 새로 바꾼 번호, 해외 통화 등
     *    정상적인 경우가 많습니다. 확인 필요로 두고 판단은 사용자에게
     *    넘기되, 합성 흔적이 있으면 위험으로 올립니다.
     */
    private fun decideForUnknownNumber(
        match: Match,
        matchedFamilyName: String?,
        spoofSuspicious: Boolean,
        spoofStrong: Boolean,
        reasons: MutableList<String>,
    ): Level = when (match) {

        Match.Matched -> {
            val who = matchedFamilyName ?: "가족"
            reasons += "모르는 번호인데 ${who}님 목소리가 들려요"
            if (spoofSuspicious) {
                reasons += "합성 음성으로 흉내 냈을 가능성이 있어요"
                Level.DANGER
            } else {
                reasons += "번호를 바꿨을 수도 있으니 직접 확인해 보세요"
                Level.CAUTION
            }
        }

        Match.Near -> {
            reasons += "가족 목소리와 비슷하지만 확실하지 않아요"
            if (spoofSuspicious) {
                reasons += "합성 음성 흔적이 감지됐어요"
                Level.DANGER
            } else {
                Level.CAUTION
            }
        }

        Match.Mismatched -> {
            reasons += "등록된 가족이 아닌 목소리예요"
            if (spoofSuspicious) {
                reasons += "합성 음성으로 만들어진 목소리로 보여요"
                Level.DANGER
            } else {
                // 모르는 사람의 실제 목소리.
                // 육성 사칭은 음성만으로 탐지할 수 없어 여기서 멈춥니다.
                Level.CAUTION
            }
        }
    }

    /** 파일명을 읽지 못해 발신자를 알 수 없는 경우 */
    private fun decideForUnidentified(
        match: Match,
        spoofSuspicious: Boolean,
        reasons: MutableList<String>,
    ): Level {
        reasons += "발신자 정보를 확인할 수 없었어요"
        return when {
            spoofSuspicious -> {
                reasons += "합성 음성으로 만들어진 목소리로 보여요"
                Level.DANGER
            }
            match == Match.Matched -> {
                reasons += "등록된 가족의 목소리와 일치해요"
                Level.SAFE
            }
            else -> Level.CAUTION
        }
    }

    /**
     * 0~100 위험도.
     *
     * 등급이 주(主)이고 점수는 등급 안에서의 정도입니다.
     * 점수로 등급을 정하지 않습니다. 임계값 하나로 갈리면
     * 경계선에서 판정이 뒤집혀 신뢰를 잃습니다.
     */
    private fun riskScore(level: Level, similarity: Float, spoof: Float?): Float {
        val s = spoof ?: 0f
        return when (level) {
            Level.SAFE -> ((1f - similarity) * 30f).coerceIn(0f, 30f)
            Level.CAUTION -> (35f + (1f - similarity) * 20f + s * 15f).coerceIn(30f, 69f)
            Level.DANGER -> (70f + (1f - similarity) * 15f + s * 15f).coerceIn(70f, 100f)
            Level.INSUFFICIENT -> 0f
        }
    }
}