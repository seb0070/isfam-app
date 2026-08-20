package com.isfam.core.permission

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * IsFAM 이 필요로 하는 권한 전체를 모델링합니다.
 *
 * 실기기 검증(SM-S937N / Android 16) 결과를 반영했습니다.
 * 권한은 세 종류로 성격이 완전히 다릅니다.
 *
 *   1. 런타임 권한   — 팝업으로 요청. 거부/영구거부 처리 필요
 *   2. 특수 권한     — 설정 화면으로 이동해야 함 (배터리 최적화)
 *   3. 외부 앱 설정  — 앱이 확인조차 할 수 없음 (삼성 자동 통화녹음)
 *
 * 3번이 특히 중요합니다. 다른 앱의 설정이라 프로그래밍적으로 켤 수도,
 * 켜졌는지 확인할 수도 없습니다. 실제로 통화가 한 번 일어나서
 * 녹음 파일이 생겨야만 확인됩니다.
 */

enum class RuntimePermission(
    val manifestKey: String,
    val title: String,
    val reason: String,
    val minSdk: Int = 1,
    val maxSdk: Int = Int.MAX_VALUE,
) {
    /** 통화녹음 파일 읽기. 이 서비스의 핵심 권한. */
    MediaAudio(
        manifestKey = Manifest.permission.READ_MEDIA_AUDIO,
        title = "통화 녹음 파일 접근",
        reason = "통화가 끝난 뒤 저장된 녹음 파일을 읽어 분석합니다. 파일은 분석 직후 삭제됩니다.",
        minSdk = 33,
    ),

    /** Android 12 이하 대체 권한 */
    ExternalStorage(
        manifestKey = Manifest.permission.READ_EXTERNAL_STORAGE,
        title = "통화 녹음 파일 접근",
        reason = "통화가 끝난 뒤 저장된 녹음 파일을 읽어 분석합니다. 파일은 분석 직후 삭제됩니다.",
        maxSdk = 32,
    ),

    /** 통화 종료 감지 + 수신/발신 구분 */
    PhoneState(
        manifestKey = Manifest.permission.READ_PHONE_STATE,
        title = "통화 상태 확인",
        reason = "통화가 끝난 시점을 알아야 분석을 시작할 수 있습니다. 통화 내용은 읽지 않습니다.",
    ),

    /** 위험 알림 */
    Notification(
        manifestKey = Manifest.permission.POST_NOTIFICATIONS,
        title = "알림",
        reason = "위험이 감지되면 즉시 알려드립니다.",
        minSdk = 33,
    ),

    /**
     * 등록된 가족이 이 폰에 어떤 이름으로 저장돼 있는지 조회합니다.
     * 통화 녹음 파일명이 그 이름으로 나오기 때문에 필요합니다.
     */
    Contacts(
        manifestKey = Manifest.permission.READ_CONTACTS,
        title = "연락처",
        reason = "가족이 저장된 이름을 확인해 그 통화만 분석합니다. 연락처를 밖으로 보내지 않습니다.",
    ),

    /** 목소리 등록 시에만 사용 */
    Microphone(
        manifestKey = Manifest.permission.RECORD_AUDIO,
        title = "마이크",
        reason = "가족 목소리를 등록할 때만 사용합니다. 평소에는 마이크를 켜지 않습니다.",
    ),
    ;

    fun isApplicable(): Boolean =
        Build.VERSION.SDK_INT in minSdk..maxSdk

    companion object {
        /** 현재 OS 버전에서 실제로 요청해야 하는 권한만 */
        fun applicable(): List<RuntimePermission> = entries.filter { it.isApplicable() }

        fun required(): List<RuntimePermission> =
            applicable().filter { it != Microphone }   // 마이크는 등록 화면에서 별도 요청

        /**
         * 없어도 앱이 동작하는 권한.
         *
         * 연락처가 없으면 저장된 가족 이름을 알 수 없어
         * 모르는 번호 통화만 분석하게 됩니다. 보호 범위가 좁아지지만
         * 서비스 자체는 돌아갑니다.
         */
        fun optional(): List<RuntimePermission> = listOf(Contacts)
    }
}

/** 개별 권한의 현재 상태 */
enum class PermissionStatus {
    Granted,
    /** 아직 요청한 적 없음 */
    NotRequested,
    /** 거부했지만 다시 물어볼 수 있음 */
    Denied,
    /** 영구 거부 — 팝업이 더 이상 뜨지 않음. 설정 앱으로 보내야 함 */
    PermanentlyDenied,
}

/**
 * 삼성 자동 통화녹음 상태.
 *
 * ⚠️ 앱이 직접 확인할 수 없습니다.
 * 통화 후 녹음 파일이 실제로 생기는지로만 판단 가능합니다.
 * 그래서 온보딩 시점에는 반드시 Unknown 입니다.
 */
enum class AutoRecordingStatus {
    /** 아직 확인 불가 — 첫 통화 전 */
    Unknown,
    /** 녹음 파일 발견 — 정상 동작 확인됨 */
    Confirmed,
    /** 통화 후에도 파일 없음 — 미지원 기기이거나 설정이 꺼져 있음 */
    NotWorking,
}

data class PermissionUiState(
    val runtime: Map<RuntimePermission, PermissionStatus> = emptyMap(),
    val batteryOptimizationIgnored: Boolean = false,
    val autoRecording: AutoRecordingStatus = AutoRecordingStatus.Unknown,
) {
    val allRequiredGranted: Boolean
        get() = RuntimePermission.required()
            .all { runtime[it] == PermissionStatus.Granted }

    val hasPermanentDenial: Boolean
        get() = runtime.values.any { it == PermissionStatus.PermanentlyDenied }

    /** 온보딩을 통과할 수 있는 최소 조건 */
    val canProceed: Boolean
        get() = allRequiredGranted

    fun statusOf(p: RuntimePermission): PermissionStatus =
        runtime[p] ?: PermissionStatus.NotRequested
}

/**
 * 권한 상태를 읽고 판정합니다.
 *
 * 영구 거부 판정에 주의가 필요합니다.
 * shouldShowRequestPermissionRationale 는 두 경우 모두 false 입니다.
 *   (a) 아직 한 번도 요청하지 않음
 *   (b) 사용자가 "다시 묻지 않음"으로 거부함
 * 구분하려면 "요청한 적 있는지"를 우리가 직접 기록해야 합니다.
 */
class PermissionChecker(private val context: Context) {

    private val prefs = context.getSharedPreferences("isfam_permission", Context.MODE_PRIVATE)

    fun markRequested(p: RuntimePermission) {
        prefs.edit().putBoolean("asked_${p.name}", true).apply()
    }

    private fun wasRequested(p: RuntimePermission): Boolean =
        prefs.getBoolean("asked_${p.name}", false)

    fun isGranted(p: RuntimePermission): Boolean =
        ContextCompat.checkSelfPermission(context, p.manifestKey) ==
                PackageManager.PERMISSION_GRANTED

    fun statusOf(activity: Activity, p: RuntimePermission): PermissionStatus = when {
        isGranted(p) -> PermissionStatus.Granted
        !wasRequested(p) -> PermissionStatus.NotRequested
        ActivityCompat.shouldShowRequestPermissionRationale(activity, p.manifestKey) ->
            PermissionStatus.Denied
        else -> PermissionStatus.PermanentlyDenied
    }

    fun snapshot(activity: Activity): PermissionUiState = PermissionUiState(
        runtime = RuntimePermission.applicable().associateWith { statusOf(activity, it) },
        batteryOptimizationIgnored = isBatteryOptimizationIgnored(),
        autoRecording = readAutoRecordingStatus(),
    )

    // ── 특수 권한 ──────────────────────────────────────────────

    fun isBatteryOptimizationIgnored(): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    // ── 외부 앱 설정 ───────────────────────────────────────────

    fun readAutoRecordingStatus(): AutoRecordingStatus =
        AutoRecordingStatus.valueOf(
            prefs.getString("auto_recording", AutoRecordingStatus.Unknown.name)!!
        )

    fun writeAutoRecordingStatus(status: AutoRecordingStatus) {
        prefs.edit().putString("auto_recording", status.name).apply()
    }
}

/**
 * 설정 화면으로 보내는 인텐트 모음.
 *
 * 기기마다 인텐트가 막혀 있는 경우가 있어 전부 폴백을 둡니다.
 * 인텐트 실패로 앱이 죽는 것이 가장 나쁜 시나리오입니다.
 */
object SettingsIntents {

    /** 앱 설정 — 영구 거부된 권한을 사용자가 직접 켜야 할 때 */
    fun appDetails(context: Context): Intent =
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null),
        )

    /** 배터리 최적화 예외 요청 */
    @Suppress("BatteryLife")
    fun batteryOptimization(context: Context): Intent = Intent(
        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
        Uri.parse("package:${context.packageName}"),
    )

    fun batteryOptimizationFallback(): Intent =
        Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)

    /**
     * 전화 앱 실행.
     *
     * 자동 통화녹음은 시스템 설정이 아니라 삼성 전화 앱 안에 있습니다.
     *
     *   전화 앱 → ⋮ → 설정 → 통화 녹음 → 통화 자동 녹음
     *
     * 해당 화면으로 직접 보내는 공식 딥링크는 없습니다.
     * 삼성이 exported="false" 로 막아두었고, 비공식 컴포넌트 이름을
     * 직접 지정하는 방법은 One UI 버전마다 깨지므로 쓰지 않습니다.
     *
     * 전화 앱을 띄우는 것까지만 하고, 나머지는 화면에서 단계로 안내합니다.
     */
    fun dialerApp(context: Context): Intent? {
        val pm = context.packageManager
        // 삼성 전화 앱 → 기본 전화 앱 순으로 시도
        return runCatching { pm.getLaunchIntentForPackage(SAMSUNG_DIALER) }.getOrNull()
            ?: runCatching {
                Intent(Intent.ACTION_DIAL).takeIf { it.resolveActivity(pm) != null }
            }.getOrNull()
    }

    /** 삼성 기기인지 (자동녹음 지원 가능성 판단용) */
    fun isSamsungDevice(): Boolean =
        android.os.Build.MANUFACTURER.equals("samsung", ignoreCase = true)

    /** 전화 앱이 설치되어 있는지 */
    fun hasSamsungDialer(context: Context): Boolean = runCatching {
        context.packageManager.getLaunchIntentForPackage(SAMSUNG_DIALER) != null
    }.getOrDefault(false)

    private const val SAMSUNG_DIALER = "com.samsung.android.dialer"

    /** 인텐트를 안전하게 실행. 실패해도 앱이 죽지 않게 합니다. */
    fun safeStart(context: Context, vararg candidates: Intent?): Boolean {
        for (intent in candidates) {
            if (intent == null) continue
            val ok = runCatching {
                context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }.isSuccess
            if (ok) return true
        }
        return false
    }
}