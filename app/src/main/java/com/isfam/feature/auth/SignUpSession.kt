package com.isfam.feature.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * 회원가입 세션.
 *
 * 가입 정보와 권한 상태를 여러 화면에 걸쳐 모았다가
 * 마지막(08 권한 화면)에서 한 번에 서버로 보냅니다.
 *
 *   06 회원가입  → 약관·이름·닉네임·번호·인증·비밀번호 수집
 *   08 권한      → 권한 상태 수집 후 POST /auth/signup 호출
 *
 * 권한 화면이 가입 API 앞에 오는 이유는 signup 요청 본문에
 * notification_permission 등 권한 필드가 포함되기 때문입니다.
 */
class SignUpSession {

    // ── 06에서 수집 ───────────────────────────────────────────
    var userName by mutableStateOf("")          // 이름
    var displayName by mutableStateOf("")       // 닉네임
    var phoneNumber by mutableStateOf("")
    var password by mutableStateOf("")
    /** phone/send 응답. verify 요청에 사용합니다. */
    var verificationId by mutableStateOf("")

    /**
     * phone/verify 응답으로 받는 1회용 토큰.
     * 5분간 유효하며 signup 에서 소비되면 폐기됩니다.
     * 인증번호 자체가 아니라 이 토큰을 가입 요청에 보냅니다.
     */
    var phoneVerificationToken by mutableStateOf("")

    var termsAgreed by mutableStateOf(false)
    var voicePrintAgreed by mutableStateOf(false)
    var marketingAgreed by mutableStateOf(false)

    // ── 08에서 수집 ───────────────────────────────────────────
    var notificationPermission by mutableStateOf(false)
    var microphonePermission by mutableStateOf(false)
    var filePermission by mutableStateOf(false)

    /**
     * ⚠️ 앱이 확인할 수 없는 값입니다.
     *
     * 삼성 전화 앱의 자동 통화녹음 설정은 다른 앱의 설정이라
     * 읽을 수도, 켜줄 수도 없습니다. (실기기 검증 완료)
     * 사용자가 "설정했어요"를 누른 자기 신고값일 뿐이고,
     * 실제 동작 여부는 첫 통화 후 녹음 파일이 생겨야 확인됩니다.
     */
    var callRecordingEnabled by mutableStateOf(false)

    // ── 서버 전송용 ───────────────────────────────────────────

    /** 06 화면을 통과했는지 */
    val infoCollected: Boolean
        get() = userName.isNotBlank() && phoneNumber.isNotBlank() &&
                password.isNotBlank() && phoneVerificationToken.isNotBlank() &&
                termsAgreed && voicePrintAgreed

    fun applyForm(form: SignUpForm) {
        userName = form.name
        displayName = form.displayName.ifBlank { form.name }
        phoneNumber = form.phone
        password = form.password
        termsAgreed = form.terms
        voicePrintAgreed = form.voiceData
        marketingAgreed = form.marketing
    }

    fun applyPermissions(
        notification: Boolean,
        microphone: Boolean,
        file: Boolean,
        callRecording: Boolean,
    ) {
        notificationPermission = notification
        microphonePermission = microphone
        filePermission = file
        callRecordingEnabled = callRecording
    }

    /**
     * 서버 요청 형태로 변환합니다.
     *
     * 인증번호가 아니라 phoneVerificationToken 을 보냅니다.
     * 토큰은 5분간만 유효하므로, 인증 후 가입까지 오래 걸리면
     * AUTH_006 이 납니다. 그때는 인증부터 다시 해야 합니다.
     */
    fun toSignUpParams() = com.isfam.data.repository.SignUpParams(
        phoneNumber = phoneNumber.filter(Char::isDigit),
        phoneVerificationToken = phoneVerificationToken,
        password = password,
        userName = userName,
        displayName = displayName,
        termsAgreed = termsAgreed,
        voicePrintAgreed = voicePrintAgreed,
        marketingAgreed = marketingAgreed,
        notificationPermission = notificationPermission,
        microphonePermission = microphonePermission,
        filePermission = filePermission,
        callRecordingEnabled = callRecordingEnabled,
    )

    fun reset() {
        userName = ""; displayName = ""; phoneNumber = ""
        password = ""; verificationId = ""; phoneVerificationToken = ""
        termsAgreed = false; voicePrintAgreed = false; marketingAgreed = false
        notificationPermission = false; microphonePermission = false
        filePermission = false; callRecordingEnabled = false
    }
}