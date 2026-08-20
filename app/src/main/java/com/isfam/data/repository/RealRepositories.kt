package com.isfam.data.repository

import com.isfam.core.TokenStore
import com.isfam.core.ml.EmbeddingCodec
import com.isfam.data.api.AcceptInvitationRequest
import com.isfam.data.api.ApiError
import com.isfam.data.api.ApiErrorResponse
import com.isfam.data.api.AudioQualityRequest
import com.isfam.data.api.CreateFamilyRequest
import com.isfam.data.api.IsFamApi
import com.isfam.data.api.LoginRequest
import com.isfam.data.api.PhoneSendRequest
import com.isfam.data.api.PhoneVerifyRequest
import com.isfam.data.api.PushTokenRequest
import com.isfam.data.api.RegisterDeviceRequest
import com.isfam.data.api.SignupRequest
import com.isfam.data.api.SyncPermissionsRequest
import com.isfam.data.api.SyncStatusRequest
import com.isfam.data.api.UpdateCapabilityRequest
import com.isfam.data.api.UpdateSettingsRequest
import com.isfam.data.api.VoiceprintRegisterRequest
import kotlinx.serialization.json.Json
import retrofit2.HttpException

/**
 * 서버 호출 구현.
 *
 * 모든 호출을 apiCall 로 감싸 예외를 Result 로 바꿉니다.
 * 화면이 try-catch 를 쓰지 않아도 되고, 빠뜨려서 앱이 죽는 일이 없습니다.
 */

private val errorJson = Json { ignoreUnknownKeys = true }

/**
 * 서버 예외를 ApiFailure 로 변환합니다.
 *
 * HttpException 의 body 에 error_code 가 들어 있고,
 * 그걸 사용자 문구로 바꾸는 게 ApiError 입니다.
 */
internal suspend fun <T> apiCall(block: suspend () -> T): Result<T> =
    runCatching { block() }.recoverCatching { throwable ->
        throw when (throwable) {
            is HttpException -> {
                val body = throwable.response()?.errorBody()?.string()
                val code = body?.let {
                    runCatching {
                        errorJson.decodeFromString<ApiErrorResponse>(it).errorCode
                    }.getOrNull()
                }
                ApiFailure(ApiError.from(code), throwable.code())
            }
            // 네트워크 자체가 안 되는 경우
            is java.io.IOException -> ApiFailure(ApiError.ServerError)
            else -> throwable
        }
    }

// ══════════════════════════════════════════════════════════════

class RealAuthRepository(
    private val api: IsFamApi,
    private val tokenStore: TokenStore,
) : AuthRepository {

    override suspend fun sendVerificationCode(phoneNumber: String) = apiCall {
        val res = api.sendVerificationCode(PhoneSendRequest(phoneNumber))
        PhoneVerification(res.verificationId, res.expiresAt)
    }

    override suspend fun verifyPhoneCode(verificationId: String, code: String) = apiCall {
        api.verifyPhoneCode(PhoneVerifyRequest(verificationId, code))
            .phoneVerificationToken
    }

    override suspend fun signUp(request: SignUpParams) = apiCall {
        val res = api.signup(
            SignupRequest(
                phoneNumber = request.phoneNumber,
                phoneVerificationToken = request.phoneVerificationToken,
                password = request.password,
                userName = request.userName,
                displayName = request.displayName,
                termsAgreed = request.termsAgreed,
                voicePrintAgreed = request.voicePrintAgreed,
                marketingAgreed = request.marketingAgreed,
                notificationPermission = request.notificationPermission,
                microphonePermission = request.microphonePermission,
                filePermission = request.filePermission,
                callRecordingEnabled = request.callRecordingEnabled,
            )
        )
        tokenStore.save(res.accessToken, res.refreshToken)
        AuthSession(res.userId.toLong(), res.displayName)
    }

    override suspend fun login(phoneNumber: String, password: String) = apiCall {
        val res = api.login(LoginRequest(phoneNumber, password))
        tokenStore.save(res.accessToken, res.refreshToken)
        AuthSession(res.userId.toLong(), "")
    }

    override suspend fun logout() = apiCall {
        // 서버 호출이 실패해도 로컬 토큰은 지웁니다.
        // 사용자가 로그아웃을 눌렀는데 네트워크 때문에 남아 있으면 안 됩니다.
        runCatching { api.logout() }
        tokenStore.clear()
    }

    override suspend fun getMe() = apiCall {
        val res = api.me()
        UserProfile(res.userId.toLong(), res.displayName, res.phoneNumber, res.createdAt)
    }

    override fun hasSession(): Boolean = tokenStore.accessToken != null
}

// ══════════════════════════════════════════════════════════════

class RealFamilyRepository(private val api: IsFamApi) : FamilyRepository {

    override suspend fun createFamily(name: String) = apiCall {
        api.createFamily(CreateFamilyRequest(name)).toDomain()
    }

    override suspend fun getFamily() = apiCall { api.getFamily().toDomain() }

    override suspend fun renameFamily(name: String) = apiCall {
        api.renameFamily(CreateFamilyRequest(name))
        Unit
    }

    override suspend fun leaveOrRemove(userId: Long) = apiCall {
        api.removeMember(userId.toInt())
    }

    override suspend fun createInviteCode() = apiCall {
        api.createInviteCode().toDomain()
    }

    override suspend fun getInviteCode() = apiCall {
        api.getInviteCode().toDomain()
    }

    override suspend fun deactivateInviteCode() = apiCall {
        api.deactivateInviteCode()
    }

    override suspend fun previewInvitation(inviteCode: String) = apiCall {
        val res = api.previewInvitation(inviteCode)
        InvitePreview(res.inviteCode, res.familyName, res.memberCount, res.expiresAt)
    }

    override suspend fun acceptInvitation(
        inviteCode: String,
        voiceSharingConsent: Boolean,
    ) = apiCall {
        api.acceptInvitation(inviteCode, AcceptInvitationRequest(voiceSharingConsent))
        api.getFamily().toDomain()
    }
}

private fun com.isfam.data.api.FamilyResponse.toDomain() = Family(
    familyId = familyId.toLong(),
    name = name,
    isOwner = myMemberRole == "owner",
    members = members.map {
        FamilyMember(
            userId = it.userId.toLong(),
            displayName = it.displayName,
            isOwner = it.memberRole == "owner",
            voiceprintRegistered = it.voiceprintRegistered,
            protectionEnabled = it.protectionEnabled,
            joinedAt = it.joinedAt,
        )
    },
)

private fun com.isfam.data.api.InviteCodeResponse.toDomain() = InviteCode(
    code = inviteCode,
    link = inviteLink,
    qrCodeUrl = qrCodeUrl,
    expiresAt = expiresAt,
)

// ══════════════════════════════════════════════════════════════

class RealVoiceprintRepository(private val api: IsFamApi) : VoiceprintRepository {

    override suspend fun registerVoiceprint(
        sentenceId: Int,
        embedding: FloatArray,
        quality: VoiceQuality,
    ) = apiCall {
        api.registerVoiceprint(
            VoiceprintRegisterRequest(
                sentenceId = sentenceId,
                embedding = EmbeddingCodec.encode(embedding),
                embeddingModelVersion = EmbeddingCodec.MODEL_VERSION,
                audioQuality = AudioQualityRequest(
                    isAnalyzable = quality.isAnalyzable,
                    durationSeconds = quality.durationSeconds,
                    rmsEnergy = quality.rmsEnergy,
                    peakAmplitude = quality.peakAmplitude,
                    speechRatio = quality.speechRatio,
                ),
            )
        )
        // 등록 직후 상태를 다시 읽어 샘플 품질까지 받아옵니다
        api.getVoiceprintStatus().toDomain()
    }

    override suspend fun getStatus() = apiCall {
        api.getVoiceprintStatus().toDomain()
    }

    override suspend fun syncFamilyEmbeddings(since: String?) = apiCall {
        val res = api.getFamilyEmbeddings(since)
        EmbeddingSync(
            modelVersion = res.embeddingModelVersion,
            syncedAt = res.syncedAt,
            members = res.members.mapNotNull { member ->
                // 디코딩 실패한 임베딩은 버립니다.
                // 잘못된 벡터로 대조하면 엉뚱한 판정이 나옵니다.
                EmbeddingCodec.decode(member.embedding)?.let {
                    MemberEmbedding(
                        userId = member.userId.toLong(),
                        displayName = member.displayName,
                        embedding = it,
                        updatedAt = member.updatedAt,
                    )
                }
            },
            removedUserIds = res.removedUserIds.map(Int::toLong),
        )
    }
}

private fun com.isfam.data.api.VoiceprintStatusResponse.toDomain() = VoiceprintStatus(
    registered = voiceprintRegistered,
    modelVersion = embeddingModelVersion,
    sampleCount = sampleCount,
    samples = samples.map { VoiceprintSample(it.sentenceId, it.quality) },
)

// ══════════════════════════════════════════════════════════════

class RealDeviceRepository(private val api: IsFamApi) : DeviceRepository {

    override suspend fun registerDevice(pushToken: String) = apiCall {
        val res = api.registerDevice(
            RegisterDeviceRequest(
                platform = "android",
                manufacturer = android.os.Build.MANUFACTURER,
                deviceModel = android.os.Build.MODEL,
                osVersion = android.os.Build.VERSION.RELEASE,
                pushToken = pushToken,
            )
        )
        DeviceInfo(res.deviceId.toLong(), res.callRecordingSupported)
    }

    override suspend fun getCapability() = apiCall {
        val res = api.getCapability()
        DeviceCapability(
            res.callRecordingSupported,
            res.guidanceRequired,
            res.guidanceUrl,
        )
    }

    override suspend fun updateCapability(supported: Boolean) = apiCall {
        api.updateCapability(UpdateCapabilityRequest(supported))
        Unit
    }

    override suspend fun updatePushToken(token: String) = apiCall {
        api.updatePushToken(PushTokenRequest(token))
        Unit
    }

    override suspend fun updateSyncStatus(syncedAt: String, trigger: String) = apiCall {
        api.updateSyncStatus(SyncStatusRequest(syncedAt, trigger))
        Unit
    }
}

class RealSettingsRepository(private val api: IsFamApi) : SettingsRepository {

    override suspend fun getSettings() = apiCall {
        val res = api.getSettings()
        AppSettings(
            notificationEnabled = res.notificationEnabled,
            permissions = PermissionSnapshot(
                notification = res.permissions.notificationPermission,
                microphone = res.permissions.microphonePermission,
                file = res.permissions.filePermission,
            ),
        )
    }

    override suspend fun updateNotificationEnabled(enabled: Boolean) = apiCall {
        api.updateSettings(UpdateSettingsRequest(enabled))
        Unit
    }

    override suspend fun syncPermissions(permissions: PermissionSnapshot) = apiCall {
        api.syncPermissions(
            SyncPermissionsRequest(
                notificationPermission = permissions.notification,
                microphonePermission = permissions.microphone,
                filePermission = permissions.file,
                batteryOptimizationIgnored = permissions.batteryOptimizationIgnored,
            )
        )
        Unit
    }
}

class RealNotificationRepository(private val api: IsFamApi) : NotificationRepository {

    override suspend fun getNotifications(unreadOnly: Boolean?, page: Int) = apiCall {
        val res = api.getNotifications(unreadOnly, page)
        NotificationPage(
            items = res.items.map {
                AppNotification(
                    notificationId = it.notificationId,
                    type = it.type,
                    title = it.title,
                    body = it.body,
                    targetType = it.targetType,
                    targetId = it.targetId,
                    isRead = it.isRead,
                    createdAt = it.createdAt,
                )
            },
            unreadCount = res.unreadCount,
            total = res.total,
        )
    }

    override suspend fun getUnreadCount() = apiCall {
        api.getUnreadCount().unreadCount
    }

    override suspend fun markAsRead(notificationId: Long) = apiCall {
        api.markNotificationRead(notificationId)
        Unit
    }
}
