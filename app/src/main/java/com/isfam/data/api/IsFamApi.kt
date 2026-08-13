package com.isfam.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

/**
 * ══════════════════════════════════════════════════════════════
 *  ⚠️ 이 파일은 API 명세 PDF 가 아니라 실제 서버 코드(IsFAM-main)를
 *     읽고 작성했습니다.
 *
 *  PDF 에 있던 다음 엔드포인트는 구현되어 있지 않습니다:
 *    /auth/phone/send, /auth/phone/verify, /me/voiceprint,
 *    /family/embeddings, /devices, /invitations,
 *    /call-events, /voice-analyses, /settings, /model-info
 *
 *  base URL 뒤에 /api/v1 이 붙습니다.
 *  인증이 필요한 것은 auth 라우트뿐이고, family/voice/demo 는
 *  현재 토큰 없이 호출됩니다.
 * ══════════════════════════════════════════════════════════════
 */
interface IsFamApi {

    // ── 헬스체크 (prefix 없음) ─────────────────────────────────
    @GET("health")
    suspend fun health(): HealthResponse

    // ── 인증 ─────────────────────────────────────────────────
    //
    // SMS 발송이 없습니다. verification_code 는 "123456" 고정입니다.
    // (서버 설정 auth_fixed_verification_code)
    // 데모용 정책이라 프론트에서는 인증번호 입력 화면을 그대로 두되
    // 기본값을 채워두면 시연이 매끄럽습니다.

    @POST("api/v1/auth/signup")
    suspend fun signup(@Body body: SignupRequest): SignupResponse

    @POST("api/v1/auth/login")
    suspend fun login(@Body body: LoginRequest): LoginResponse

    @POST("api/v1/auth/refresh")
    suspend fun refresh(@Body body: RefreshRequest): TokenResponse

    /** 204 No Content. Authorization 헤더 필요 */
    @POST("api/v1/auth/logout")
    suspend fun logout()

    @GET("api/v1/auth/me")
    suspend fun me(): MeResponse

    // ── 가족 성문 등록 ────────────────────────────────────────
    //
    // PDF 의 /me/voiceprint 가 아니라 /family/register 입니다.
    // sentence_id 개념이 없고, 이름·관계·오디오 파일을 함께 보냅니다.
    // 같은 이름으로 여러 번 등록하면 샘플이 누적되는 구조입니다.

    @Multipart
    @POST("api/v1/family/register")
    suspend fun registerFamilyVoice(
        @Part("name") name: RequestBody,
        @Part("relation") relation: RequestBody,
        @Part audioFile: MultipartBody.Part,
    ): FamilyRegisterResponse

    @GET("api/v1/family")
    suspend fun listFamily(): FamilyListResponse

    @GET("api/v1/family/{familyId}")
    suspend fun getFamilyMember(@Path("familyId") familyId: Int): FamilyMemberResponse

    @DELETE("api/v1/family/{familyId}")
    suspend fun deleteFamilyMember(@Path("familyId") familyId: Int): FamilyDeleteResponse

    // ── ★ 통화 분석 — 이 앱의 핵심 엔드포인트 ──────────────────
    //
    // 통화 녹음 파일을 올리면
    //   ① 등록된 가족 전원과 1:N 대조
    //   ② 딥보이스 탐지
    //   ③ 위험도 점수 + 등급 + 사유 문장
    // 을 한 번에 돌려줍니다.

    @Multipart
    @POST("api/v1/voice/verify")
    suspend fun verifyVoice(
        @Part audioFile: MultipartBody.Part,
    ): SecureVoiceVerificationResponse

    /** 딥보이스 탐지 없이 가족 대조만 (더 빠름) */
    @Multipart
    @POST("api/v1/voice/verify-family")
    suspend fun verifyFamily(
        @Part audioFile: MultipartBody.Part,
    ): VerifyFamilyResponse

    /** 두 음성 1:1 비교 — 디버깅·테스트용 */
    @Multipart
    @POST("api/v1/voice/compare")
    suspend fun compareVoice(
        @Part audioFile1: MultipartBody.Part,
        @Part audioFile2: MultipartBody.Part,
    ): VoiceCompareResponse

    // ── 딥보이스 단독 ─────────────────────────────────────────

    @GET("api/v1/anti-spoofing/model-info")
    suspend fun antiSpoofingModelInfo(): AntiSpoofingModelInfoResponse

    @Multipart
    @POST("api/v1/anti-spoofing/detect")
    suspend fun detectSpoofing(
        @Part audioFile: MultipartBody.Part,
    ): AntiSpoofingResponse

    // ── 데모 (real/fake 맞추기) ───────────────────────────────

    @POST("api/v1/demo/start")
    suspend fun startDemo(): DemoStartResponse

    @POST("api/v1/demo/{sessionId}/answer")
    suspend fun answerDemo(
        @Path("sessionId") sessionId: String,
        @Body body: DemoAnswerRequest,
    ): DemoAnswerResponse
}

// ══════════════════════════════════════════════════════════════
//  DTO — 서버 Pydantic 스키마와 1:1 대응
// ══════════════════════════════════════════════════════════════

@Serializable
data class HealthResponse(val status: String)

// ─── 인증 ─────────────────────────────────────────────────────

@Serializable
data class SignupRequest(
    @SerialName("phone_number") val phoneNumber: String,
    @SerialName("display_name") val displayName: String,
    /** 서버 고정값 "123456" */
    @SerialName("verification_code") val verificationCode: String = "123456",
    /** "child" 또는 "parent" */
    val role: String,
)

@Serializable
data class LoginRequest(
    @SerialName("phone_number") val phoneNumber: String,
    @SerialName("verification_code") val verificationCode: String = "123456",
)

@Serializable
data class RefreshRequest(
    @SerialName("refresh_token") val refreshToken: String,
)

@Serializable
open class TokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
)

@Serializable
data class SignupResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("user_id") val userId: Int,
    @SerialName("display_name") val displayName: String,
    val role: String,
)

@Serializable
data class LoginResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("user_id") val userId: Int,
    val role: String,
)

@Serializable
data class MeResponse(
    @SerialName("user_id") val userId: Int,
    @SerialName("display_name") val displayName: String,
    @SerialName("phone_number") val phoneNumber: String,
    val role: String,
    @SerialName("created_at") val createdAt: String,
)

// ─── 가족 ─────────────────────────────────────────────────────

@Serializable
data class FamilyRegisterResponse(
    @SerialName("family_id") val familyId: Int,
    val name: String,
    val relation: String,
    @SerialName("model_name") val modelName: String,
    val message: String,
)

@Serializable
data class FamilyMemberResponse(
    @SerialName("family_id") val familyId: Int,
    val name: String,
    val relation: String,
    @SerialName("model_name") val modelName: String,
)

@Serializable
data class FamilyListResponse(val members: List<FamilyMemberResponse>)

@Serializable
data class FamilyDeleteResponse(
    @SerialName("family_id") val familyId: Int,
    val message: String,
)

// ─── 음질 ─────────────────────────────────────────────────────

@Serializable
data class AudioQualityDto(
    @SerialName("is_analyzable") val isAnalyzable: Boolean,
    val message: String,
    @SerialName("duration_seconds") val durationSeconds: Double,
    @SerialName("rms_energy") val rmsEnergy: Double,
    @SerialName("peak_amplitude") val peakAmplitude: Double,
    @SerialName("speech_ratio") val speechRatio: Double,
)

// ─── 가족 대조 (1:N) ──────────────────────────────────────────

@Serializable
data class FamilyCandidateDto(
    @SerialName("family_id") val familyId: Int,
    val name: String,
    val relation: String,
    val similarity: Double,
    @SerialName("sample_count") val sampleCount: Int = 1,
    @SerialName("max_similarity") val maxSimilarity: Double? = null,
    @SerialName("mean_similarity") val meanSimilarity: Double? = null,
    @SerialName("median_similarity") val medianSimilarity: Double? = null,
    @SerialName("weighted_mean_similarity") val weightedMeanSimilarity: Double? = null,
    @SerialName("weighted_median_similarity") val weightedMedianSimilarity: Double? = null,
    @SerialName("profile_threshold") val profileThreshold: Double? = null,
    @SerialName("confidence_score") val confidenceScore: Double? = null,
    @SerialName("sample_quality") val sampleQuality: String? = null,
    @SerialName("low_quality_sample_count") val lowQualitySampleCount: Int = 0,
)

@Serializable
data class VerifyFamilyResponse(
    @SerialName("is_registered_family") val isRegisteredFamily: Boolean,
    /** 등록된 가족이 없으면 null */
    @SerialName("best_match") val bestMatch: FamilyCandidateDto? = null,
    val threshold: Double,
    /** 전원과의 유사도. 1:N 대조 결과 전체 */
    val candidates: List<FamilyCandidateDto> = emptyList(),
    val message: String,
    @SerialName("model_name") val modelName: String,
)

@Serializable
data class VoiceCompareResponse(
    val similarity: Double,
    val threshold: Double,
    @SerialName("is_same_speaker") val isSameSpeaker: Boolean,
    val message: String,
    @SerialName("model_name") val modelName: String,
)

// ─── 딥보이스 ─────────────────────────────────────────────────

@Serializable
data class LabelScoreDto(val label: String, val score: Double)

@Serializable
data class AntiSpoofingResponse(
    @SerialName("analysis_status") val analysisStatus: String? = null,
    @SerialName("processing_time_ms") val processingTimeMs: Double? = null,
    @SerialName("is_spoofed") val isSpoofed: Boolean,
    @SerialName("spoof_score") val spoofScore: Double,
    val threshold: Double,
    @SerialName("predicted_label") val predictedLabel: String,
    @SerialName("predicted_score") val predictedScore: Double,
    val message: String,
    @SerialName("model_name") val modelName: String,
    @SerialName("analyzed_segments") val analyzedSegments: Int = 0,
    @SerialName("max_spoof_segment_index") val maxSpoofSegmentIndex: Int = 0,
    @SerialName("segment_seconds") val segmentSeconds: Double = 0.0,
    @SerialName("label_scores") val labelScores: List<LabelScoreDto> = emptyList(),
    @SerialName("audio_quality") val audioQuality: AudioQualityDto? = null,
)

@Serializable
data class AntiSpoofingModelInfoResponse(
    val status: String,
    @SerialName("model_name") val modelName: String,
    @SerialName("model_version") val modelVersion: String,
    val device: String,
    val threshold: Double,
    @SerialName("sample_rate") val sampleRate: Int,
    @SerialName("max_audio_seconds") val maxAudioSeconds: Double,
    @SerialName("window_seconds") val windowSeconds: Double,
    @SerialName("hop_seconds") val hopSeconds: Double,
    @SerialName("batch_size") val batchSize: Int,
    @SerialName("max_concurrency") val maxConcurrency: Int,
    @SerialName("warmed_up") val warmedUp: Boolean,
)

// ─── ★ 통합 분석 결과 ─────────────────────────────────────────

@Serializable
data class SecureVoiceVerificationResponse(
    /** "complete" 또는 "more_voice_required" */
    @SerialName("analysis_status") val analysisStatus: String,
    @SerialName("is_trusted") val isTrusted: Boolean,
    /** "safe" | "caution" | "danger" */
    @SerialName("risk_level") val riskLevel: String,
    /** ⚠️ 0~100 이 아니라 0.0~1.0 입니다 */
    @SerialName("risk_score") val riskScore: Double,
    @SerialName("family_confidence") val familyConfidence: Double,
    @SerialName("mismatch_confidence") val mismatchConfidence: Double,
    /** 6가지 값. Decision.from() 참고 */
    @SerialName("final_decision") val finalDecision: String,
    /** 사용자에게 보여줄 판정 사유. 한국어 문장 */
    @SerialName("decision_reasons") val decisionReasons: List<String> = emptyList(),
    @SerialName("processing_time_ms") val processingTimeMs: Double,
    @SerialName("family_model_time_ms") val familyModelTimeMs: Double,
    @SerialName("anti_spoofing_model_time_ms") val antiSpoofingModelTimeMs: Double,
    @SerialName("audio_quality") val audioQuality: AudioQualityDto,
    @SerialName("family_verification") val familyVerification: VerifyFamilyResponse,
    @SerialName("anti_spoofing") val antiSpoofing: AntiSpoofingResponse,
)

// ─── 데모 ─────────────────────────────────────────────────────

@Serializable
data class DemoStartResponse(
    @SerialName("session_id") val sessionId: String,
    @SerialName("audio_url") val audioUrl: String,
    @SerialName("playback_seconds") val playbackSeconds: Int,
    val message: String,
)

@Serializable
data class DemoAnswerRequest(
    /** "real" 또는 "fake" */
    @SerialName("user_guess") val userGuess: String,
)

@Serializable
data class DemoAnswerResponse(
    @SerialName("session_id") val sessionId: String,
    @SerialName("user_guess") val userGuess: String,
    @SerialName("actual_label") val actualLabel: String,
    @SerialName("is_user_correct") val isUserCorrect: Boolean,
    @SerialName("ai_guess") val aiGuess: String,
    @SerialName("is_ai_correct") val isAiCorrect: Boolean,
    @SerialName("anti_spoofing") val antiSpoofing: AntiSpoofingResponse,
    val message: String,
)

// ══════════════════════════════════════════════════════════════
//  UI 모델 변환
//
//  화면에서 문자열 리터럴("danger")을 직접 비교하지 마세요.
//  서버가 값을 바꾸면 33개 화면을 다 뒤져야 합니다.
// ══════════════════════════════════════════════════════════════

enum class RiskLevel { SAFE, CAUTION, DANGER, INSUFFICIENT;
    companion object {
        fun from(riskLevel: String, analysisStatus: String): RiskLevel =
            when {
                analysisStatus != "complete" -> INSUFFICIENT
                riskLevel == "safe" -> SAFE
                riskLevel == "caution" -> CAUTION
                riskLevel == "danger" -> DANGER
                else -> INSUFFICIENT
            }
    }
}

/** 서버 final_decision 6종 */
enum class Decision(val serverValue: String, val userMessage: String) {
    TrustedFamily("trusted_family_voice", "등록된 가족의 목소리가 맞아요"),
    SpoofedFamilyLike("spoofed_family_like_voice", "가족 목소리를 흉내 낸 AI 합성음으로 보여요"),
    SpoofedUnknown("spoofed_unknown_voice", "AI로 만들어진 목소리로 보여요"),
    UnregisteredDetected("unregistered_voice_detected", "등록된 가족이 아닌 목소리예요"),
    NeedsConfirmation("family_voice_needs_confirmation", "확인이 필요해요"),
    UnknownReal("unknown_real_voice", "등록되지 않은 실제 사람의 목소리예요"),
    Unmapped("", "판정할 수 없어요");

    companion object {
        fun from(value: String): Decision =
            entries.firstOrNull { it.serverValue == value } ?: Unmapped
    }
}

/** 화면이 쓰는 최종 모델 */
data class AnalysisUiModel(
    val riskLevel: RiskLevel,
    val decision: Decision,
    /** 0~100 으로 변환된 값 (서버는 0.0~1.0) */
    val riskPercent: Int,
    val matchedName: String?,
    val similarity: Double,
    val spoofScore: Double,
    val reasons: List<String>,
    val isAnalyzable: Boolean,
    val elapsedMs: Long,
) {
    companion object {
        fun from(res: SecureVoiceVerificationResponse) = AnalysisUiModel(
            riskLevel = RiskLevel.from(res.riskLevel, res.analysisStatus),
            decision = Decision.from(res.finalDecision),
            riskPercent = (res.riskScore * 100).toInt().coerceIn(0, 100),
            matchedName = res.familyVerification.bestMatch?.name,
            similarity = res.familyVerification.bestMatch?.similarity ?: 0.0,
            spoofScore = res.antiSpoofing.spoofScore,
            reasons = res.decisionReasons,
            isAnalyzable = res.analysisStatus == "complete",
            elapsedMs = res.processingTimeMs.toLong(),
        )
    }
}
