package com.isfam.data.repository

import com.isfam.data.api.ApiError

/**
 * Repository 계약.
 *
 * 화면은 이 인터페이스만 알고 구현체는 모릅니다.
 * 서버가 준비되면 AppContainer 의 useFakeData 를 false 로 바꾸는 것만으로
 * Fake → Real 전환이 끝납니다. 화면 코드는 한 줄도 바뀌지 않습니다.
 *
 * 반환 타입이 Result 인 이유
 *   네트워크 호출은 실패가 정상 경로의 일부입니다.
 *   예외를 던지면 호출부마다 try-catch 를 써야 하고,
 *   빠뜨리면 앱이 죽습니다. Result 로 강제하면 컴파일러가 잡아줍니다.
 */

/**
 * 서버 오류를 화면이 그대로 쓸 수 있는 형태로 감쌉니다.
 *
 * fieldMessage 를 우선 쓰세요. 서버가 어느 필드가 왜 틀렸는지
 * 알려주는데, 일반 문구("입력값을 확인해 주세요")만 보여주면
 * 사용자가 무엇을 고쳐야 할지 알 수 없습니다.
 */
data class ApiFailure(
    val error: ApiError,
    val statusCode: Int? = null,
    /** field → reason. 예) password → "영문과 숫자를 포함해 8자 이상이어야 해요" */
    val fieldErrors: Map<String, String> = emptyMap(),
) : Exception(error.message) {

    /** 필드 오류가 있으면 그것을, 없으면 일반 문구를 돌려줍니다 */
    val displayMessage: String
        get() = fieldErrors.values.firstOrNull() ?: error.message

    fun reasonFor(field: String): String? = fieldErrors[field]
}

// ══════════════════════════════════════════════════════════════
//  인증
// ══════════════════════════════════════════════════════════════

interface AuthRepository {

    /**
     * 휴대폰 인증 1단계 — 인증번호 발송.
     * @return verification_id. verify 요청에 필요합니다.
     */
    suspend fun sendVerificationCode(phoneNumber: String): Result<PhoneVerification>

    /**
     * 휴대폰 인증 2단계 — 코드 확인.
     * @return 1회용 토큰. 5분간 유효하며 signup 에서 소비됩니다.
     */
    suspend fun verifyPhoneCode(
        verificationId: String,
        code: String,
    ): Result<String>

    suspend fun signUp(request: SignUpParams): Result<AuthSession>

    suspend fun login(phoneNumber: String, password: String): Result<AuthSession>

    suspend fun logout(): Result<Unit>

    suspend fun getMe(): Result<UserProfile>

    /** 저장된 토큰이 있는지. 스플래시에서 분기에 씁니다. */
    fun hasSession(): Boolean
}

data class PhoneVerification(
    val verificationId: String,
    val expiresAt: String,
)

data class SignUpParams(
    val phoneNumber: String,
    val phoneVerificationToken: String,
    val password: String,
    /** 실명 */
    val userName: String,
    /** 앱 내 호칭 */
    val displayName: String,
    val termsAgreed: Boolean,
    val voicePrintAgreed: Boolean,
    val marketingAgreed: Boolean,
    val notificationPermission: Boolean,
    val microphonePermission: Boolean,
    val filePermission: Boolean,
    /** ⚠️ 앱이 확인할 수 없는 값. 사용자 자기 신고입니다. */
    val callRecordingEnabled: Boolean,
)

data class AuthSession(
    val userId: Long,
    val displayName: String,
)

data class UserProfile(
    val userId: Long,
    val displayName: String,
    /** 마스킹된 형태로 옵니다 — "010****1234" */
    val phoneNumber: String,
    val createdAt: String,
)

// ══════════════════════════════════════════════════════════════
//  가족 · 초대
// ══════════════════════════════════════════════════════════════

interface FamilyRepository {

    suspend fun createFamily(name: String): Result<Family>

    /** 소속된 공간이 없으면 FAMILY_002 */
    suspend fun getFamily(): Result<Family>

    suspend fun renameFamily(name: String): Result<Unit>

    suspend fun leaveOrRemove(userId: Long): Result<Unit>

    // ── 초대 ──────────────────────────────────────────────────

    /** 기존 코드가 있으면 무효화하고 새로 발급합니다 */
    suspend fun createInviteCode(): Result<InviteCode>

    /** 코드가 없으면 hasActiveCode = false 인 응답 */
    suspend fun getInviteCode(): Result<InviteCode>

    suspend fun deactivateInviteCode(): Result<Unit>

    /** 딥링크 진입 직후. 인증 불필요 */
    suspend fun previewInvitation(inviteCode: String): Result<InvitePreview>

    suspend fun acceptInvitation(
        inviteCode: String,
        voiceSharingConsent: Boolean,
    ): Result<Family>
}

data class Family(
    val familyId: Long,
    val name: String,
    val isOwner: Boolean,
    val members: List<FamilyMember>,
)

data class FamilyMember(
    val userId: Long,
    val displayName: String,
    val isOwner: Boolean,
    val voiceprintRegistered: Boolean,
    val protectionEnabled: Boolean,
    val joinedAt: String,
)

data class InviteCode(
    /** 6자리 영숫자 — "AB12CD". 없으면 null */
    val code: String?,
    /**
     * ⚠️ 딥링크는 도메인 설정이 끝나지 않아 아직 동작하지 않습니다.
     *    탭해도 앱이 열리지 않으므로 공유 기능에 쓰면 안 됩니다.
     *    (assetlinks.json 등록 후 사용 가능)
     */
    val link: String?,
    /**
     * ⚠️ 사용하지 마세요.
     *    서버가 QR 이미지를 생성하지 않아 이 주소에는 아무것도 없습니다.
     *    QR 은 code 값을 앱에서 직접 인코딩해 그립니다(QrCodeImage).
     *    필드는 서버 정리 예정이라 곧 사라지거나 null 이 됩니다.
     */
    val qrCodeUrl: String?,
    val expiresAt: String?,
) {
    val hasActiveCode: Boolean get() = code != null
}

data class InvitePreview(
    val inviteCode: String,
    val familyName: String,
    val memberCount: Int,
    val expiresAt: String,
)

// ══════════════════════════════════════════════════════════════
//  목소리 · 임베딩
// ══════════════════════════════════════════════════════════════

interface VoiceprintRepository {

    /**
     * 목소리 등록.
     *
     * ⚠️ 오디오를 보내지 않습니다. 온디바이스에서 추출한 임베딩만 보냅니다.
     *    서버가 오디오를 받는 순간 법적 의무가 붙기 때문에
     *    application/json 으로 제한되어 있습니다.
     */
    suspend fun registerVoiceprint(
        sentenceId: Int,
        embedding: FloatArray,
        quality: VoiceQuality,
    ): Result<VoiceprintStatus>

    suspend fun getStatus(): Result<VoiceprintStatus>

    /**
     * 가족 구성원 임베딩 동기화.
     *
     * 이게 있어야 1:N 대조가 가능합니다.
     * 받은 임베딩은 Keystore 에 암호화 저장합니다.
     */
    suspend fun syncFamilyEmbeddings(since: String? = null): Result<EmbeddingSync>
}

data class VoiceQuality(
    val isAnalyzable: Boolean,
    val durationSeconds: Double? = null,
    val rmsEnergy: Double? = null,
    val peakAmplitude: Double? = null,
    val speechRatio: Double? = null,
)

data class VoiceprintStatus(
    val registered: Boolean,
    val modelVersion: String?,
    val sampleCount: Int,
    /** quality 가 "review" 인 샘플은 재녹음을 유도합니다 */
    val samples: List<VoiceprintSample>,
)

data class VoiceprintSample(
    val sentenceId: Int,
    val quality: String,
)

data class EmbeddingSync(
    val modelVersion: String,
    val syncedAt: String,
    val members: List<MemberEmbedding>,
    /** 탈퇴·강퇴·음성삭제된 멤버. 로컬에서 지워야 합니다. */
    val removedUserIds: List<Long>,
)

data class MemberEmbedding(
    val userId: Long,
    val displayName: String,
    val embedding: FloatArray,
    val updatedAt: String,
)

// ══════════════════════════════════════════════════════════════
//  단말 · 설정 · 알림
// ══════════════════════════════════════════════════════════════

interface DeviceRepository {

    suspend fun registerDevice(pushToken: String): Result<DeviceInfo>

    /** 통화 자동녹음 지원 여부 조회 */
    suspend fun getCapability(): Result<DeviceCapability>

    /**
     * 자동녹음 동작 여부 보고.
     *
     * ⚠️ 앱이 직접 확인할 수 없는 값입니다.
     *    첫 통화 후 녹음 파일이 생겼는지로만 판단합니다.
     */
    suspend fun updateCapability(supported: Boolean): Result<Unit>

    suspend fun updatePushToken(token: String): Result<Unit>

    suspend fun updateSyncStatus(syncedAt: String, trigger: String): Result<Unit>
}

data class DeviceInfo(
    val deviceId: Long,
    val callRecordingSupported: Boolean,
)

data class DeviceCapability(
    val callRecordingSupported: Boolean,
    val guidanceRequired: Boolean,
    val guidanceUrl: String?,
)

interface SettingsRepository {

    suspend fun getSettings(): Result<AppSettings>

    suspend fun updateNotificationEnabled(enabled: Boolean): Result<Unit>

    /** OS 권한 상태를 서버에 동기화. 권한이 바뀔 때마다 호출합니다. */
    suspend fun syncPermissions(permissions: PermissionSnapshot): Result<Unit>
}

data class AppSettings(
    val notificationEnabled: Boolean,
    val permissions: PermissionSnapshot,
)

data class PermissionSnapshot(
    val notification: Boolean,
    val microphone: Boolean,
    val file: Boolean,
    val batteryOptimizationIgnored: Boolean = false,
)

interface NotificationRepository {

    suspend fun getNotifications(
        unreadOnly: Boolean? = null,
        page: Int = 1,
    ): Result<NotificationPage>

    suspend fun getUnreadCount(): Result<Int>

    suspend fun markAsRead(notificationId: Long): Result<Unit>
}

data class NotificationPage(
    val items: List<AppNotification>,
    val unreadCount: Int,
    val total: Int,
)

data class AppNotification(
    val notificationId: Long,
    val type: String,
    val title: String,
    val body: String,
    /** "voice_analysis" | "none". none 이면 탭해도 이동하지 않습니다 */
    val targetType: String,
    val targetId: Long?,
    val isRead: Boolean,
    val createdAt: String,
) {
    val isNavigable: Boolean get() = targetType != "none" && targetId != null
}