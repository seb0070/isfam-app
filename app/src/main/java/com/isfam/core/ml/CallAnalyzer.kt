package com.isfam.core.ml

import android.util.Log
import com.isfam.BuildConfig
import com.isfam.core.audio.PreparedAudio
import com.isfam.core.call.CallerIdentity

/**
 * 통화 분석 전체 흐름.
 *
 *   ① 화자 분리 — 폰 주인 목소리 제거
 *   ② 1:N 대조  — 등록 가족 전원과 비교
 *   ③ 딥보이스  — 서버 (선택. 실패해도 판정은 진행)
 *   ④ 2축 판정
 *
 * ③이 없어도 동작합니다. 네트워크가 끊겨도 화자 검증만으로
 * 판정이 나오도록 설계했습니다.
 */
class CallAnalyzer(
    private val separator: SpeakerSeparator,
    private val store: EncryptedVoiceprintStore,
) {

    data class Analysis(
        val verdict: CallVerdict.Result,
        val separation: SpeakerSeparator.Result,
        val elapsedMs: Long,
    )

    /**
     * @param audio 16kHz 모노로 전처리된 통화 음성
     * @param identity 파일명에서 파싱한 발신자 주장
     * @param spoofScore 서버 딥보이스 점수. null 이면 미수행
     * @param spoofReliable analysis_status == "complete"
     */
    fun analyze(
        audio: PreparedAudio,
        identity: CallerIdentity,
        spoofScore: Float? = null,
        spoofReliable: Boolean = false,
    ): Analysis {
        val started = System.currentTimeMillis()

        // 폰 주인 성문이 없으면 화자 분리를 할 수 없습니다.
        // 본인 목소리 등록이 선택이 아니라 필수인 이유입니다.
        val ownerEmbedding = store.load(VoiceprintEnrollmentService.OWNER_PROFILE_ID)
            ?: return insufficientResult(
                "본인 목소리가 등록되지 않아 분석할 수 없어요",
                started,
            )

        // ① 화자 분리
        val separation = separator.separate(audio.samples, ownerEmbedding)
        log(separation.summary())

        if (!separation.usable || separation.farEndEmbedding == null) {
            return Analysis(
                verdict = CallVerdict.decide(
                    identity = identity,
                    matchedFamilyId = null,
                    matchedFamilyName = null,
                    similarity = 0f,
                    spoofScore = spoofScore,
                    spoofReliable = spoofReliable,
                    separationUsable = false,
                ),
                separation = separation,
                elapsedMs = System.currentTimeMillis() - started,
            )
        }

        // ② 1:N 대조 — 폰 주인을 제외한 가족 전원
        val family = store.loadAll()
            .filterKeys { it != VoiceprintEnrollmentService.OWNER_PROFILE_ID }

        val best = family
            .mapValues { (_, embedding) ->
                SpeakerMath.cosine(separation.farEndEmbedding, embedding)
            }
            .maxByOrNull { it.value }

        val similarity = best?.value ?: 0f

        log("1:N 대조 · 가족 ${family.size}명 · 최고 %.3f (기준 %.2f)"
            .format(similarity, CallVerdict.MATCH_THRESHOLD))

        // ④ 판정 — 유사도 구간 판단은 CallVerdict 가 합니다
        val verdict = CallVerdict.decide(
            identity = identity,
            matchedFamilyId = best?.key,
            matchedFamilyName = best?.key?.let(::displayNameOf),
            similarity = similarity,
            spoofScore = spoofScore,
            spoofReliable = spoofReliable,
            separationUsable = true,
        )

        val elapsed = System.currentTimeMillis() - started
        log("판정 ${verdict.level} · 위험도 %.0f · ${elapsed}ms".format(verdict.riskScore))

        return Analysis(verdict, separation, elapsed)
    }

    /**
     * 프로필 ID 를 표시용 이름으로.
     *
     * TODO: Repository 연결 시 서버의 display_name 으로 교체.
     *       지금은 Keystore 키를 그대로 씁니다.
     */
    private fun displayNameOf(profileId: String): String = profileId

    private fun insufficientResult(reason: String, started: Long) = Analysis(
        verdict = CallVerdict.Result(
            level = CallVerdict.Level.INSUFFICIENT,
            riskScore = 0f,
            reasons = listOf(reason),
            matchedFamilyId = null,
            similarity = 0f,
            spoofScore = null,
        ),
        separation = SpeakerSeparator.Result(
            farEndEmbedding = null,
            ownerSec = 0f, farEndSec = 0f, ambiguousSec = 0f,
            skippedSilentSec = 0f, coherence = 0f,
            processedWindows = 0, elapsedMs = 0,
        ),
        elapsedMs = System.currentTimeMillis() - started,
    )

    private fun log(message: String) {
        if (BuildConfig.DEBUG) Log.d("IsFamAnalyze", message)
    }
}
