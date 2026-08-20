package com.isfam.data.repository

import kotlinx.coroutines.delay

/**
 * 서버 없이 화면을 돌리기 위한 구현.
 *
 * 단순히 고정값을 반환하지 않고 상태를 들고 있습니다.
 * 가족을 만들면 실제로 목록에 반영되고, 초대 코드를 발급하면
 * 그 코드로 조회가 됩니다. 그래야 화면 흐름을 제대로 확인할 수 있습니다.
 *
 * 네트워크 지연도 흉내 냅니다. 즉시 반환하면 로딩 상태를 만들 수 없어
 * 서버를 붙였을 때 처음 보는 문제가 생깁니다.
 */

private const val FAKE_DELAY_MS = 400L

class FakeAuthRepository : AuthRepository {

    private var session: AuthSession? = null
    private var lastVerificationId: String? = null

    override suspend fun sendVerificationCode(phoneNumber: String): Result<PhoneVerification> {
        delay(FAKE_DELAY_MS)
        val id = "fake-verification-${System.currentTimeMillis()}"
        lastVerificationId = id
        return Result.success(PhoneVerification(id, "2026-12-31T23:59:59Z"))
    }

    override suspend fun verifyPhoneCode(
        verificationId: String,
        code: String,
    ): Result<String> {
        delay(FAKE_DELAY_MS)
        // 서버 데모 환경과 같은 고정 코드입니다
        return if (code == "123456") {
            Result.success("fake-phone-token")
        } else {
            Result.failure(ApiFailure(com.isfam.data.api.ApiError.AuthWrongCode))
        }
    }

    override suspend fun signUp(request: SignUpParams): Result<AuthSession> {
        delay(FAKE_DELAY_MS)
        val created = AuthSession(userId = 1L, displayName = request.displayName)
        session = created
        return Result.success(created)
    }

    override suspend fun login(phoneNumber: String, password: String): Result<AuthSession> {
        delay(FAKE_DELAY_MS)
        val created = AuthSession(userId = 1L, displayName = "서연")
        session = created
        return Result.success(created)
    }

    override suspend fun logout(): Result<Unit> {
        session = null
        return Result.success(Unit)
    }

    override suspend fun getMe(): Result<UserProfile> {
        delay(FAKE_DELAY_MS)
        return Result.success(
            UserProfile(1L, session?.displayName ?: "서연", "010****1123", "2026-08-01T09:00:00Z")
        )
    }

    override fun hasSession(): Boolean = session != null
}

// ══════════════════════════════════════════════════════════════

class FakeFamilyRepository : FamilyRepository {

    private var family: Family? = Family(
        familyId = 1L,
        name = "우리 가족",
        isOwner = true,
        members = listOf(
            FamilyMember(1L, "김서연", true, true, true, "2026-08-01T09:00:00Z"),
            FamilyMember(2L, "김상호", false, true, true, "2026-08-02T10:00:00Z"),
            FamilyMember(3L, "이정영", false, true, true, "2026-08-02T11:00:00Z"),
            FamilyMember(4L, "김도현", false, false, true, "2026-08-05T14:00:00Z"),
        ),
    )

    private var inviteCode: InviteCode = InviteCode(null, null, null, null)

    override suspend fun createFamily(name: String): Result<Family> {
        delay(FAKE_DELAY_MS)
        val created = Family(
            familyId = 1L,
            name = name,
            isOwner = true,
            members = listOf(
                FamilyMember(1L, "김서연", true, true, true, "2026-08-20T09:00:00Z"),
            ),
        )
        family = created
        return Result.success(created)
    }

    override suspend fun getFamily(): Result<Family> {
        delay(FAKE_DELAY_MS)
        return family?.let { Result.success(it) }
            ?: Result.failure(ApiFailure(com.isfam.data.api.ApiError.FamilyNoSpace))
    }

    override suspend fun renameFamily(name: String): Result<Unit> {
        delay(FAKE_DELAY_MS)
        family = family?.copy(name = name)
        return Result.success(Unit)
    }

    override suspend fun leaveOrRemove(userId: Long): Result<Unit> {
        delay(FAKE_DELAY_MS)
        family = family?.let { f -> f.copy(members = f.members.filterNot { it.userId == userId }) }
        return Result.success(Unit)
    }

    override suspend fun createInviteCode(): Result<InviteCode> {
        delay(FAKE_DELAY_MS)
        val code = (1..6).map { "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".random() }.joinToString("")
        inviteCode = InviteCode(
            code = code,
            link = "https://isfam.app/i/$code",
            qrCodeUrl = "https://isfam.app/qr/$code.png",
            expiresAt = "2026-08-23T18:00:00Z",
        )
        return Result.success(inviteCode)
    }

    override suspend fun getInviteCode(): Result<InviteCode> {
        delay(FAKE_DELAY_MS)
        return Result.success(inviteCode)
    }

    override suspend fun deactivateInviteCode(): Result<Unit> {
        inviteCode = InviteCode(null, null, null, null)
        return Result.success(Unit)
    }

    override suspend fun previewInvitation(inviteCode: String): Result<InvitePreview> {
        delay(FAKE_DELAY_MS)
        return Result.success(
            InvitePreview(inviteCode, "우리 가족", 3, "2026-08-23T18:00:00Z")
        )
    }

    override suspend fun acceptInvitation(
        inviteCode: String,
        voiceSharingConsent: Boolean,
    ): Result<Family> {
        delay(FAKE_DELAY_MS)
        if (!voiceSharingConsent) {
            return Result.failure(ApiFailure(com.isfam.data.api.ApiError.InviteConsentRequired))
        }
        return getFamily()
    }
}

// ══════════════════════════════════════════════════════════════

class FakeVoiceprintRepository : VoiceprintRepository {

    private val registered = mutableMapOf<Int, String>()

    override suspend fun registerVoiceprint(
        sentenceId: Int,
        embedding: FloatArray,
        quality: VoiceQuality,
    ): Result<VoiceprintStatus> {
        delay(FAKE_DELAY_MS)
        if (!quality.isAnalyzable) {
            return Result.failure(ApiFailure(com.isfam.data.api.ApiError.VoiceQuality))
        }
        registered[sentenceId] = "good"
        return getStatus()
    }

    override suspend fun getStatus(): Result<VoiceprintStatus> {
        delay(FAKE_DELAY_MS)
        return Result.success(
            VoiceprintStatus(
                registered = registered.isNotEmpty(),
                modelVersion = com.isfam.core.ml.EmbeddingCodec.MODEL_VERSION,
                sampleCount = registered.size,
                samples = registered.map { VoiceprintSample(it.key, it.value) },
            )
        )
    }

    override suspend fun syncFamilyEmbeddings(since: String?): Result<EmbeddingSync> {
        delay(FAKE_DELAY_MS)
        // 서버 없이는 진짜 가족 임베딩을 만들 수 없습니다.
        // 무작위 벡터를 주면 대조 결과가 무의미해지므로 빈 목록을 돌려줍니다.
        return Result.success(
            EmbeddingSync(
                modelVersion = com.isfam.core.ml.EmbeddingCodec.MODEL_VERSION,
                syncedAt = "2026-08-20T18:00:00Z",
                members = emptyList(),
                removedUserIds = emptyList(),
            )
        )
    }
}

// ══════════════════════════════════════════════════════════════

class FakeDeviceRepository : DeviceRepository {

    override suspend fun registerDevice(pushToken: String): Result<DeviceInfo> {
        delay(FAKE_DELAY_MS)
        return Result.success(DeviceInfo(1L, callRecordingSupported = true))
    }

    override suspend fun getCapability(): Result<DeviceCapability> {
        delay(FAKE_DELAY_MS)
        return Result.success(DeviceCapability(true, guidanceRequired = false, guidanceUrl = null))
    }

    override suspend fun updateCapability(supported: Boolean) = Result.success(Unit)
    override suspend fun updatePushToken(token: String) = Result.success(Unit)
    override suspend fun updateSyncStatus(syncedAt: String, trigger: String) = Result.success(Unit)
}

class FakeSettingsRepository : SettingsRepository {

    private var notificationEnabled = true

    override suspend fun getSettings(): Result<AppSettings> {
        delay(FAKE_DELAY_MS)
        return Result.success(
            AppSettings(
                notificationEnabled = notificationEnabled,
                permissions = PermissionSnapshot(
                    notification = true, microphone = true,
                    file = true, batteryOptimizationIgnored = false,
                ),
            )
        )
    }

    override suspend fun updateNotificationEnabled(enabled: Boolean): Result<Unit> {
        notificationEnabled = enabled
        return Result.success(Unit)
    }

    override suspend fun syncPermissions(permissions: PermissionSnapshot) = Result.success(Unit)
}

class FakeNotificationRepository : NotificationRepository {

    private val items = mutableListOf(
        AppNotification(
            notificationId = 1L,
            type = "danger_call",
            title = "위험한 통화가 감지됐어요",
            body = "010-4482-9917 통화에서 AI 합성 음성이 감지되었습니다",
            targetType = "voice_analysis",
            targetId = 3L,
            isRead = false,
            createdAt = "2026-08-20T12:03:00Z",
        ),
    )

    override suspend fun getNotifications(unreadOnly: Boolean?, page: Int): Result<NotificationPage> {
        delay(FAKE_DELAY_MS)
        val filtered = if (unreadOnly == true) items.filterNot { it.isRead } else items
        return Result.success(
            NotificationPage(filtered, items.count { !it.isRead }, items.size)
        )
    }

    override suspend fun getUnreadCount(): Result<Int> =
        Result.success(items.count { !it.isRead })

    override suspend fun markAsRead(notificationId: Long): Result<Unit> {
        val index = items.indexOfFirst { it.notificationId == notificationId }
        if (index >= 0) items[index] = items[index].copy(isRead = true)
        return Result.success(Unit)
    }
}
