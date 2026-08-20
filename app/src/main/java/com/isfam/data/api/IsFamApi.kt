package com.isfam.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PUT
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * ══════════════════════════════════════════════════════════════
 *  IsFam 서버 API — 백엔드 구현(isfam-be, Kotlin/Spring) 기준
 *
 *  경로에 접두어가 없습니다. Spring 서버에 context-path 설정이 없어
 *  BASE_URL 뒤에 바로 붙습니다.
 *  대부분의 엔드포인트가 인증(Bearer)을 요구하며,
 *  일부는 X-Device-Id 헤더도 필수입니다. 둘 다 인터셉터가 자동 주입합니다.
 * ══════════════════════════════════════════════════════════════
 */
interface IsFamApi {

    @GET("health")
    suspend fun health(): HealthResponse

    // ══ 인증 ══════════════════════════════════════════════════
    //
    // 휴대폰 인증은 2단계입니다.
    //   ① phone/send   → verification_id 발급
    //   ② phone/verify → phone_verification_token 발급 (5분 · 1회용)
    //   ③ signup       → 위 토큰으로 가입
    //
    // 데모 환경에서는 실제 SMS 를 보내지 않고 코드가 123456 으로 고정입니다.

    @POST("auth/phone/send")
    suspend fun sendVerificationCode(@Body body: PhoneSendRequest): PhoneSendResponse

    @POST("auth/phone/verify")
    suspend fun verifyPhoneCode(@Body body: PhoneVerifyRequest): PhoneVerifyResponse

    @POST("auth/signup")
    suspend fun signup(@Body body: SignupRequest): SignupResponse

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): LoginResponse

    @POST("auth/logout")
    suspend fun logout()

    @POST("auth/refresh")
    suspend fun refresh(@Body body: RefreshRequest): TokenResponse

    @GET("auth/me")
    suspend fun me(): MeResponse

    /** 닉네임 수정 */
    @PUT("auth/me")
    suspend fun updateDisplayName(@Body body: UpdateDisplayNameRequest): UpdateDisplayNameResponse

    /** 회원 탈퇴 — PUT 입니다 (DELETE 아님) */
    @PUT("auth/me")
    suspend fun withdraw(@Body body: WithdrawRequest)

    // ══ 가족 공간 ═════════════════════════════════════════════

    @POST("family")
    suspend fun createFamily(@Body body: CreateFamilyRequest): FamilyResponse

    @GET("family")
    suspend fun getFamily(): FamilyResponse

    @PUT("family")
    suspend fun renameFamily(@Body body: CreateFamilyRequest): RenameFamilyResponse

    /** 공간 삭제. owner 만 가능하고 다른 멤버가 남아 있으면 409 */
    @DELETE("family")
    suspend fun deleteFamily()

    /** 나가기(본인) 또는 내보내기(owner) */
    @DELETE("family/members/{userId}")
    suspend fun removeMember(@Path("userId") userId: Int)

    // ══ 목소리 등록 ═══════════════════════════════════════════
    //
    // 문장 3개를 각각 업로드합니다. 서버는 임베딩만 저장하고
    // 원본 파일은 즉시 삭제합니다.

    /**
     * 목소리 등록 · 재등록.
     *
     * ⚠️ 오디오를 보내지 않습니다. 앱이 ONNX 로 추출한 임베딩만 보냅니다.
     *    서버가 consumes = application/json 으로 막아두었고,
     *    multipart 로 보내면 415(COMMON_007)입니다.
     *
     *    등록용 음성이 서버에 도달하는 순간 딥보이스 판정과 같은
     *    법적 의무(암호화 전송·접속기록·즉시 파기·처리방침)가 붙기 때문입니다.
     *
     * 여러 번 호출하면 샘플이 누적되고, 같은 sentence_id 면 교체됩니다.
     */
    @POST("me/voiceprint")
    suspend fun registerVoiceprint(
        @Body body: VoiceprintRegisterRequest,
    ): VoiceprintRegisterResponse

    /** 미등록이면 404 가 아니라 voiceprint_registered=false 인 200 입니다 */
    @GET("me/voiceprint")
    suspend fun getVoiceprintStatus(): VoiceprintStatusResponse

    /** 부모 폰에서 가족 구성원 임베딩 다운로드 */
    @GET("family/embeddings")
    suspend fun getFamilyEmbeddings(
        @Query("since") since: String? = null,
    ): FamilyEmbeddingsResponse

    @PUT("devices/me/sync-status")
    suspend fun updateSyncStatus(@Body body: SyncStatusRequest): SyncStatusResponse

    // ══ 초대 ══════════════════════════════════════════════════

    @POST("family/invite-code")
    suspend fun createInviteCode(): InviteCodeResponse

    /** 코드가 없으면 200 + 전 필드 null */
    @GET("family/invite-code")
    suspend fun getInviteCode(): InviteCodeResponse

    @DELETE("family/invite-code")
    suspend fun deactivateInviteCode()

    /** 딥링크 진입 직후 호출. 인증 불필요 */
    @GET("invitations/{inviteCode}")
    suspend fun previewInvitation(
        @Path("inviteCode") inviteCode: String,
    ): InvitePreviewResponse

    @POST("invitations/{inviteCode}/accept")
    suspend fun acceptInvitation(
        @Path("inviteCode") inviteCode: String,
        @Body body: AcceptInvitationRequest,
    ): AcceptInvitationResponse

    // ══ 단말 ══════════════════════════════════════════════════

    @POST("devices")
    suspend fun registerDevice(@Body body: RegisterDeviceRequest): RegisterDeviceResponse

    @GET("devices/me/capability")
    suspend fun getCapability(): CapabilityResponse

    @PUT("devices/me/capability")
    suspend fun updateCapability(@Body body: UpdateCapabilityRequest): UpdateCapabilityResponse

    @PUT("devices/me/push-token")
    suspend fun updatePushToken(@Body body: PushTokenRequest): PushTokenResponse

    @GET("model-info")
    suspend fun getModelInfo(): List<ModelInfoResponse>

    // ══ 통화 이벤트 · 분석 ════════════════════════════════════

    @POST("call-events")
    suspend fun createCallEvent(@Body body: CallEventRequest): CallEventResponse

    @PUT("call-events/{callEventId}")
    suspend fun updateCallEvent(
        @Path("callEventId") callEventId: Int,
        @Body body: UpdateCallEventRequest,
    ): CallEventResponse

    /** 온디바이스 분석 결과 제출 */
    @POST("voice-analyses")
    suspend fun submitAnalysis(@Body body: SubmitAnalysisRequest): AnalysisResponse

    @GET("voice-analyses/{analysisId}")
    suspend fun getAnalysis(@Path("analysisId") analysisId: Long): AnalysisResponse

    @GET("voice-analyses")
    suspend fun getAnalyses(
        @Query("date_from") dateFrom: String? = null,
        @Query("date_to") dateTo: String? = null,
        @Query("final_decision") finalDecision: String? = null,
        @Query("min_risk_level") minRiskLevel: Float? = null,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20,
    ): AnalysisListResponse

    @DELETE("voice-analyses/{analysisId}")
    suspend fun deleteAnalysis(@Path("analysisId") analysisId: Long)

    // ══ 알림 ══════════════════════════════════════════════════

    @GET("notifications")
    suspend fun getNotifications(
        @Query("unread_only") unreadOnly: Boolean? = null,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20,
    ): NotificationListResponse

    @GET("notifications/unread-count")
    suspend fun getUnreadCount(): UnreadCountResponse

    @PUT("notifications/{notificationId}/read")
    suspend fun markNotificationRead(
        @Path("notificationId") notificationId: Long,
    ): MarkReadResponse

    // ══ 딥보이스 탐지 ═════════════════════════════════════════
    //
    // 화자 검증은 온디바이스(ONNX)에서, 딥보이스는 서버에서 합니다.
    // 통화 음성이 서버로 올라가는 유일한 경로입니다.
    //
    // ⚠️ 아직 서버에 구현되지 않았습니다 (Phase 8).
    //    경로는 POST /call-events/{id}/spoof-check 로 예정되어 있습니다.
    //    업로드 상한 5MB — 초과하면 COMMON_006 입니다.

    @Multipart
    @POST("call-events/{callEventId}/spoof-check")
    suspend fun detectSpoofing(
        @Path("callEventId") callEventId: Int,
        @Part audioFile: MultipartBody.Part,
    ): AntiSpoofingResponse

    // ══ 데모 ══════════════════════════════════════════════════

    @POST("demo-sessions")
    suspend fun submitDemo(@Body body: DemoRequest): DemoResponse

    @GET("demo-sessions")
    suspend fun getDemoSessions(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20,
    ): DemoListResponse

    // ══ 설정 ══════════════════════════════════════════════════

    @GET("settings")
    suspend fun getSettings(): SettingsResponse

    @PUT("settings")
    suspend fun updateSettings(@Body body: UpdateSettingsRequest): SettingsResponse

    /** OS 권한 상태를 서버에 동기화 */
    @PUT("settings/permissions")
    suspend fun syncPermissions(@Body body: SyncPermissionsRequest): SettingsResponse
}

// ══════════════════════════════════════════════════════════════
//  DTO
// ══════════════════════════════════════════════════════════════

@Serializable
data class HealthResponse(val status: String)

// ─── 인증 ─────────────────────────────────────────────────────

@Serializable
data class PhoneSendRequest(
    @SerialName("phone_number") val phoneNumber: String,
)

@Serializable
data class PhoneSendResponse(
    @SerialName("verification_id") val verificationId: String,
    /** 유효시간 3분 */
    @SerialName("expires_at") val expiresAt: String,
)

@Serializable
data class PhoneVerifyRequest(
    @SerialName("verification_id") val verificationId: String,
    /** 데모 환경 고정값 "123456" */
    @SerialName("verification_code") val verificationCode: String,
)

@Serializable
data class PhoneVerifyResponse(
    /**
     * 가입·비밀번호 재설정에 사용하는 1회용 토큰.
     * 5분간 유효하며 소비되면 즉시 폐기됩니다.
     */
    @SerialName("phone_verification_token") val phoneVerificationToken: String,
    @SerialName("expires_at") val expiresAt: String,
)

/**
 * 회원가입.
 *
 * 권한 필드는 가입 시점의 스냅샷입니다.
 * 사용자가 OS 설정에서 언제든 바꿀 수 있으므로 이후에는
 * PUT /settings/permissions 로 계속 동기화해야 합니다.
 */
@Serializable
data class SignupRequest(
    @SerialName("phone_number") val phoneNumber: String,
    @SerialName("phone_verification_token") val phoneVerificationToken: String,
    val password: String,
    /** 실명 */
    @SerialName("user_name") val userName: String,
    /** 앱 내 호칭 */
    @SerialName("display_name") val displayName: String,

    @SerialName("terms_agreed") val termsAgreed: Boolean,
    @SerialName("voice_print_agreed") val voicePrintAgreed: Boolean,
    @SerialName("marketing_agreed") val marketingAgreed: Boolean = false,

    @SerialName("notification_permission") val notificationPermission: Boolean = false,
    @SerialName("microphone_permission") val microphonePermission: Boolean = false,
    @SerialName("file_permission") val filePermission: Boolean = false,
    /**
     * ⚠️ 앱이 확인할 수 없는 값입니다.
     * 삼성 전화 앱 설정이라 읽기·제어가 불가능합니다 (실기기 검증 완료).
     * 사용자가 "설정했어요"를 누른 자기 신고값이며, 실제 동작 여부는
     * 첫 통화 후 녹음 파일이 생겨야 확인됩니다.
     */
    @SerialName("call_recording_enabled") val callRecordingEnabled: Boolean = false,
)

@Serializable
data class SignupResponse(
    @SerialName("user_id") val userId: Int,
    @SerialName("display_name") val displayName: String,
    @SerialName("device_id") val deviceId: Int? = null,
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
)

@Serializable
data class LoginRequest(
    @SerialName("phone_number") val phoneNumber: String,
    val password: String,
)

@Serializable
data class LoginResponse(
    @SerialName("user_id") val userId: Int,
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
)

@Serializable
data class RefreshRequest(
    @SerialName("refresh_token") val refreshToken: String,
)

@Serializable
data class TokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
)

@Serializable
data class MeResponse(
    @SerialName("user_id") val userId: Int,
    @SerialName("display_name") val displayName: String,
    /** 마스킹된 형태로 옵니다 — "010****1234" */
    @SerialName("phone_number") val phoneNumber: String,
    @SerialName("created_at") val createdAt: String,
)

@Serializable
data class UpdateDisplayNameRequest(
    @SerialName("display_name") val displayName: String,
)

@Serializable
data class UpdateDisplayNameResponse(
    @SerialName("user_id") val userId: Int,
    @SerialName("display_name") val displayName: String,
    @SerialName("updated_at") val updatedAt: String,
)

@Serializable
data class WithdrawRequest(val password: String)

// ─── 가족 ─────────────────────────────────────────────────────

@Serializable
data class CreateFamilyRequest(val name: String)

@Serializable
data class FamilyResponse(
    @SerialName("family_id") val familyId: Int,
    val name: String,
    /** "owner" 또는 "member" */
    @SerialName("my_member_role") val myMemberRole: String,
    val members: List<FamilyMemberDto> = emptyList(),
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class FamilyMemberDto(
    @SerialName("user_id") val userId: Int,
    @SerialName("display_name") val displayName: String,
    @SerialName("member_role") val memberRole: String = "member",
    @SerialName("voiceprint_registered") val voiceprintRegistered: Boolean,
    @SerialName("protection_enabled") val protectionEnabled: Boolean,
    @SerialName("joined_at") val joinedAt: String,
)

@Serializable
data class RenameFamilyResponse(
    @SerialName("family_id") val familyId: Int,
    val name: String,
    @SerialName("updated_at") val updatedAt: String,
)

// ─── 목소리 등록 ──────────────────────────────────────────────

/**
 * 목소리 등록 요청.
 *
 * embedding 은 base64 로 감싼 float32[192](= 768바이트)입니다.
 * 길이가 다르거나 디코딩이 안 되면 VOICE_001(422)입니다.
 */
@Serializable
data class VoiceprintRegisterRequest(
    @SerialName("sentence_id") val sentenceId: Int,
    /** EmbeddingCodec.encode() 결과 */
    val embedding: String,
    /** EmbeddingCodec.MODEL_VERSION. 낮으면 MODEL_001(422) */
    @SerialName("embedding_model_version") val embeddingModelVersion: String,
    @SerialName("audio_quality") val audioQuality: AudioQualityRequest,
)

/**
 * 앱이 계산한 음질 지표.
 *
 * is_analyzable 만 필수입니다. 나머지는 앱 버전에 따라 빠질 수 있어
 * 서버가 null 을 허용하고 없는 지표는 판정에서 중립으로 취급합니다.
 */
@Serializable
data class AudioQualityRequest(
    @SerialName("is_analyzable") val isAnalyzable: Boolean,
    @SerialName("duration_seconds") val durationSeconds: Double? = null,
    @SerialName("rms_energy") val rmsEnergy: Double? = null,
    @SerialName("peak_amplitude") val peakAmplitude: Double? = null,
    @SerialName("speech_ratio") val speechRatio: Double? = null,
)

@Serializable
data class VoiceprintRegisterResponse(
    @SerialName("voiceprint_registered") val voiceprintRegistered: Boolean,
    @SerialName("embedding_model_version") val embeddingModelVersion: String? = null,
    @SerialName("sample_count") val sampleCount: Int = 0,
    @SerialName("registered_at") val registeredAt: String? = null,
)

@Serializable
data class VoiceprintStatusResponse(
    @SerialName("voiceprint_registered") val voiceprintRegistered: Boolean,
    @SerialName("embedding_model_version") val embeddingModelVersion: String? = null,
    @SerialName("sample_count") val sampleCount: Int = 0,
    val samples: List<VoiceprintSampleDto> = emptyList(),
    @SerialName("registered_at") val registeredAt: String? = null,
)

/** quality 가 "review" 면 앱이 재녹음을 유도합니다 */
@Serializable
data class VoiceprintSampleDto(
    @SerialName("sentence_id") val sentenceId: Int,
    val quality: String,
)

@Serializable
data class FamilyEmbeddingsResponse(
    @SerialName("embedding_model_version") val embeddingModelVersion: String,
    @SerialName("synced_at") val syncedAt: String,
    val members: List<EmbeddingMemberDto> = emptyList(),
    /** since 이후 탈퇴·강퇴·음성삭제된 멤버 ID */
    @SerialName("removed_user_ids") val removedUserIds: List<Int> = emptyList(),
)

@Serializable
data class EmbeddingMemberDto(
    @SerialName("user_id") val userId: Int,
    @SerialName("display_name") val displayName: String,
    /** base64 로 인코딩된 암호화 벡터. Keystore 에 저장해야 합니다. */
    val embedding: String,
    @SerialName("updated_at") val updatedAt: String,
)

@Serializable
data class SyncStatusRequest(
    @SerialName("synced_at") val syncedAt: String,
    /** "push" | "joined" | "periodic" | "foreground" */
    @SerialName("sync_trigger") val syncTrigger: String,
)

@Serializable
data class SyncStatusResponse(
    @SerialName("device_id") val deviceId: Int,
    @SerialName("embedding_synced_at") val embeddingSyncedAt: String,
    @SerialName("sync_trigger") val syncTrigger: String,
)

// ─── 초대 ─────────────────────────────────────────────────────

@Serializable
data class InviteCodeResponse(
    @SerialName("invite_id") val inviteId: Int? = null,
    /** 6자리 영숫자 — "AB12CD" */
    @SerialName("invite_code") val inviteCode: String? = null,
    @SerialName("invite_link") val inviteLink: String? = null,
    /** 서버가 QR 이미지를 만들어 줍니다. 앱에서 생성할 필요 없습니다. */
    @SerialName("qr_code_url") val qrCodeUrl: String? = null,
    /** 만료 72시간 */
    @SerialName("expires_at") val expiresAt: String? = null,
    @SerialName("rotated_at") val rotatedAt: String? = null,
) {
    val hasActiveCode: Boolean get() = inviteCode != null
}

@Serializable
data class InvitePreviewResponse(
    @SerialName("invite_code") val inviteCode: String,
    @SerialName("family_name") val familyName: String,
    @SerialName("member_count") val memberCount: Int,
    @SerialName("expires_at") val expiresAt: String,
)

@Serializable
data class AcceptInvitationRequest(
    @SerialName("voice_sharing_consent") val voiceSharingConsent: Boolean,
)

@Serializable
data class AcceptInvitationResponse(
    @SerialName("family_id") val familyId: Int,
    @SerialName("family_name") val familyName: String,
    @SerialName("user_id") val userId: Int,
    @SerialName("display_name") val displayName: String,
    @SerialName("member_role") val memberRole: String,
    val status: String,
    @SerialName("joined_at") val joinedAt: String,
)

// ─── 단말 ─────────────────────────────────────────────────────

@Serializable
data class RegisterDeviceRequest(
    /** "android" 또는 "ios" */
    val platform: String,
    val manufacturer: String,
    @SerialName("device_model") val deviceModel: String,
    @SerialName("os_version") val osVersion: String,
    @SerialName("push_token") val pushToken: String,
)

@Serializable
data class RegisterDeviceResponse(
    @SerialName("device_id") val deviceId: Int,
    @SerialName("call_recording_supported") val callRecordingSupported: Boolean,
)

@Serializable
data class CapabilityResponse(
    @SerialName("call_recording_supported") val callRecordingSupported: Boolean,
    @SerialName("guidance_required") val guidanceRequired: Boolean,
    @SerialName("guidance_url") val guidanceUrl: String? = null,
)

@Serializable
data class UpdateCapabilityRequest(
    @SerialName("call_recording_supported") val callRecordingSupported: Boolean,
)

@Serializable
data class UpdateCapabilityResponse(
    @SerialName("device_id") val deviceId: Int,
    @SerialName("call_recording_supported") val callRecordingSupported: Boolean,
)

@Serializable
data class PushTokenRequest(
    @SerialName("push_token") val pushToken: String,
)

@Serializable
data class PushTokenResponse(
    @SerialName("device_id") val deviceId: Int,
    @SerialName("push_token") val pushToken: String,
)

@Serializable
data class ModelInfoResponse(
    @SerialName("latest_model_version") val latestModelVersion: String,
    @SerialName("min_supported_version") val minSupportedVersion: String,
    @SerialName("update_required") val updateRequired: Boolean,
    @SerialName("download_url") val downloadUrl: String? = null,
)

// ─── 통화 이벤트 ──────────────────────────────────────────────

@Serializable
data class CallEventRequest(
    @SerialName("detected_at") val detectedAt: String,
)

@Serializable
data class CallEventResponse(
    @SerialName("call_event_id") val callEventId: Int,
    /** "detected" | "analyzed_on_device" | "skipped" | "failed" */
    @SerialName("local_analysis_status") val localAnalysisStatus: String,
    @SerialName("voice_analysis_id") val voiceAnalysisId: Long? = null,
)

@Serializable
data class UpdateCallEventRequest(
    @SerialName("local_analysis_status") val localAnalysisStatus: String,
    @SerialName("voice_analysis_id") val voiceAnalysisId: Long? = null,
)

// ─── 분석 결과 ────────────────────────────────────────────────

/**
 * 온디바이스 분석 결과 제출.
 *
 * ⚠️ 현재 앱은 온디바이스 추론이 없습니다.
 *    서버 분석 경로(오디오 업로드)가 명세에 없으므로
 *    백엔드와 협의가 필요합니다.
 */
@Serializable
data class SubmitAnalysisRequest(
    @SerialName("call_event_id") val callEventId: Int,
    @SerialName("matched_family_id") val matchedFamilyId: Int? = null,
    @SerialName("similarity_score") val similarityScore: Float,
    @SerialName("spoof_score") val spoofScore: Float,
    /** 0~100 */
    @SerialName("risk_level") val riskLevel: Float,
    /** "normal" | "needs_check" | "danger" */
    @SerialName("final_decision") val finalDecision: String,
    @SerialName("on_device_model_version") val onDeviceModelVersion: String,
    @SerialName("analyzed_at") val analyzedAt: String,
)

@Serializable
data class AnalysisResponse(
    @SerialName("analysis_id") val analysisId: Long,
    @SerialName("matched_family") val matchedFamily: MatchedFamilyDto? = null,
    @SerialName("matched_member") val matchedMember: MatchedFamilyDto? = null,
    @SerialName("similarity_score") val similarityScore: Float,
    @SerialName("spoof_score") val spoofScore: Float,
    /** 0~100 */
    @SerialName("risk_level") val riskLevel: Float,
    @SerialName("final_decision") val finalDecision: String,
    @SerialName("submitted_at") val submittedAt: String,
)

@Serializable
data class MatchedFamilyDto(
    @SerialName("family_id") val familyId: Int? = null,
    @SerialName("user_id") val userId: Int? = null,
    val name: String? = null,
    @SerialName("display_name") val displayName: String? = null,
) {
    val label: String get() = displayName ?: name ?: "알 수 없음"
}

@Serializable
data class AnalysisListResponse(
    val items: List<AnalysisResponse> = emptyList(),
    val page: Int = 1,
    @SerialName("page_size") val pageSize: Int = 20,
    val total: Int = 0,
)

// ─── 딥보이스 탐지 ────────────────────────────────────────────

@Serializable
data class AntiSpoofingResponse(
    /** "complete" 또는 "more_voice_required" */
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
data class LabelScoreDto(val label: String, val score: Double)

/** 서버가 판정한 음질. 앱의 AudioPipeline 검사와 별개입니다. */
@Serializable
data class AudioQualityDto(
    @SerialName("is_analyzable") val isAnalyzable: Boolean,
    val message: String,
    @SerialName("duration_seconds") val durationSeconds: Double,
    @SerialName("rms_energy") val rmsEnergy: Double,
    @SerialName("peak_amplitude") val peakAmplitude: Double,
    @SerialName("speech_ratio") val speechRatio: Double,
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

// ─── 알림 ─────────────────────────────────────────────────────

@Serializable
data class NotificationListResponse(
    val items: List<NotificationDto> = emptyList(),
    @SerialName("unread_count") val unreadCount: Int = 0,
    val page: Int = 1,
    @SerialName("page_size") val pageSize: Int = 20,
    val total: Int = 0,
)

@Serializable
data class NotificationDto(
    @SerialName("notification_id") val notificationId: Long,
    /** "danger_call" 등 */
    val type: String,
    val title: String,
    val body: String,
    /** "voice_analysis" | "none" — none 이면 탭해도 이동하지 않습니다 */
    @SerialName("target_type") val targetType: String,
    @SerialName("target_id") val targetId: Long? = null,
    @SerialName("read_at") val readAt: String? = null,
    @SerialName("created_at") val createdAt: String,
) {
    val isRead: Boolean get() = readAt != null
    val isNavigable: Boolean get() = targetType != "none" && targetId != null
}

@Serializable
data class UnreadCountResponse(
    @SerialName("unread_count") val unreadCount: Int,
)

@Serializable
data class MarkReadResponse(
    @SerialName("notification_id") val notificationId: Long,
    @SerialName("read_at") val readAt: String,
)

// ─── 데모 ─────────────────────────────────────────────────────

@Serializable
data class DemoRequest(
    /** "real" 또는 "fake" */
    @SerialName("user_guess") val userGuess: String,
    @SerialName("ai_result") val aiResult: String,
)

@Serializable
data class DemoResponse(
    @SerialName("demo_session_id") val demoSessionId: Long,
    @SerialName("is_correct") val isCorrect: Boolean,
)

@Serializable
data class DemoListResponse(
    val items: List<DemoItemDto> = emptyList(),
    val page: Int = 1,
    @SerialName("page_size") val pageSize: Int = 20,
    val total: Int = 0,
)

@Serializable
data class DemoItemDto(
    @SerialName("demo_session_id") val demoSessionId: Long,
    @SerialName("user_guess") val userGuess: String,
    @SerialName("ai_result") val aiResult: String,
    @SerialName("is_correct") val isCorrect: Boolean,
    @SerialName("created_at") val createdAt: String,
)

// ─── 설정 ─────────────────────────────────────────────────────

@Serializable
data class SettingsResponse(
    @SerialName("notification_enabled") val notificationEnabled: Boolean,
    val permissions: PermissionStatusDto,
)

@Serializable
data class PermissionStatusDto(
    @SerialName("notification_permission") val notificationPermission: Boolean,
    @SerialName("microphone_permission") val microphonePermission: Boolean,
    @SerialName("file_permission") val filePermission: Boolean,
)

@Serializable
data class UpdateSettingsRequest(
    @SerialName("notification_enabled") val notificationEnabled: Boolean,
)

@Serializable
data class SyncPermissionsRequest(
    @SerialName("notification_permission") val notificationPermission: Boolean,
    @SerialName("microphone_permission") val microphonePermission: Boolean,
    @SerialName("file_permission") val filePermission: Boolean,
    @SerialName("battery_optimization_ignored") val batteryOptimizationIgnored: Boolean,
)

// ══════════════════════════════════════════════════════════════
//  에러 코드
// ══════════════════════════════════════════════════════════════

@Serializable
data class ApiErrorResponse(
    @SerialName("error_code") val errorCode: String? = null,
    val message: String? = null,
)

/**
 * 서버 error_code → 사용자 문구.
 *
 * 문구는 백엔드 ErrorCode 와 동일하게 맞췄습니다.
 * 같은 상황에서 앱과 서버가 다른 말을 하면 사용자가 혼란스럽습니다.
 */
enum class ApiError(val code: String, val message: String) {
    // ── 인증 ──────────────────────────────────────────────────
    AuthNotRegistered("AUTH_001", "가입되지 않은 번호예요"),
    AuthTokenExpired("AUTH_002", "로그인이 만료됐어요. 다시 로그인해 주세요"),
    AuthDuplicatePhone("AUTH_003", "이미 가입된 번호예요"),
    AuthWrongCode("AUTH_004", "인증번호가 맞지 않아요"),
    AuthResendLimit("AUTH_005", "인증번호 발송 횟수를 초과했어요"),
    AuthVerifyExpired("AUTH_006", "인증이 만료됐어요. 처음부터 다시 진행해 주세요"),
    AuthTermsRequired("AUTH_007", "필수 약관에 동의해 주세요"),
    AuthWrongPassword("AUTH_008", "비밀번호가 맞지 않아요"),
    AuthLoginRequired("AUTH_009", "로그인이 필요해요"),

    // ── 가족 ──────────────────────────────────────────────────
    FamilyMemberNotFound("FAMILY_001", "해당 구성원을 찾을 수 없어요"),
    FamilyNoSpace("FAMILY_002", "소속된 가족 공간이 없어요"),
    FamilyNotOwner("FAMILY_003", "가족 공간 관리자만 할 수 있어요"),
    FamilyAlreadyJoined("FAMILY_004", "이미 다른 가족 공간에 참여하고 있어요"),
    FamilyOwnerCannotLeave("FAMILY_005", "관리자는 공간을 삭제한 뒤에 나갈 수 있어요"),
    FamilyFull("FAMILY_006", "가족 공간 인원이 가득 찼어요"),
    FamilyHasMembers("FAMILY_007", "남아 있는 구성원이 있어 공간을 삭제할 수 없어요"),

    // ── 초대 ──────────────────────────────────────────────────
    InviteInvalid("INVITE_001", "만료되었거나 존재하지 않는 초대예요"),
    InviteAlreadyMember("INVITE_002", "이미 이 가족 공간의 구성원이에요"),
    InviteConsentRequired("INVITE_003", "음성 공유 동의가 필요해요"),

    // ── 음성 · 단말 · 모델 ────────────────────────────────────
    VoiceQuality("VOICE_001", "음질이 부족해요. 조용한 곳에서 다시 녹음해 주세요"),
    DeviceUnsupported("DEVICE_001", "통화 자동녹음을 지원하지 않는 기기예요"),
    DeviceRequired("DEVICE_002", "단말 정보가 필요해요"),
    ModelOutdated("MODEL_001", "앱을 최신 버전으로 업데이트해 주세요"),

    // ── 공통 ──────────────────────────────────────────────────
    BadRequest("COMMON_001", "요청 형식이 올바르지 않아요"),
    InvalidInput("COMMON_002", "입력값을 확인해 주세요"),
    Forbidden("COMMON_003", "접근 권한이 없어요"),
    NotFound("COMMON_004", "요청한 정보를 찾을 수 없어요"),
    MethodNotAllowed("COMMON_005", "지원하지 않는 요청 방식이에요"),
    PayloadTooLarge("COMMON_006", "파일 크기가 너무 커요"),
    UnsupportedMediaType("COMMON_007", "지원하지 않는 형식이에요"),
    ServerError("COMMON_008", "일시적인 오류가 발생했어요"),
    AnalysisServerError("COMMON_009", "분석 서버와 통신하지 못했어요"),

    Unknown("", "잠시 후 다시 시도해 주세요");

    companion object {
        fun from(code: String?): ApiError =
            entries.firstOrNull { it.code == code } ?: Unknown
    }
}