package com.isfam.core.call

import android.net.Uri
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 통화 녹음 파일 한 건.
 *
 * 실기기 검증 결과 (SM-S937N / Android 16)
 *   경로   Recordings/Call/
 *   포맷   .m4a · 48kHz · 모노 1채널
 *   식별   MediaStore.Audio.IS_RECORDING = 1
 *   파일명 통화 {연락처명 또는 번호}_{YYMMDD}_{HHMMSS}.m4a
 */
data class CallRecording(
    val uri: Uri,
    val fileName: String,
    val sizeBytes: Long,
    val durationMs: Long,
    val modifiedAtMillis: Long,
    val identity: CallerIdentity,
    val startedAt: LocalDateTime?,
) {
    val durationSec: Int get() = (durationMs / 1000).toInt()
}

/**
 * 통화 상대가 스스로 주장하는 정체.
 *
 * 파일명에서 얻으므로 READ_CALL_LOG 권한이 필요 없습니다.
 * (Play 정책상 기본 전화앱에만 허용되는 권한이라 중요한 이점입니다)
 */
sealed interface CallerIdentity {

    /**
     * 연락처에 저장된 상대. 번호를 알 수 없습니다.
     *
     * ⚠️ 이 경우 차단·번호 공유를 하면 안 됩니다.
     *    발신번호 변작 공격에서는 표시된 번호가 곧 진짜 가족의 번호라
     *    차단하면 실제 가족이 차단됩니다.
     */
    data class ContactName(val name: String) : CallerIdentity

    /** 저장되지 않은 번호. 번호가 나온다는 것 자체가 "모르는 번호"라는 증거입니다. */
    data class PhoneNumber(val number: String) : CallerIdentity

    data object Unknown : CallerIdentity

    val label: String
        get() = when (this) {
            is ContactName -> name
            is PhoneNumber -> number
            Unknown -> "알 수 없음"
        }

    /** 차단·공유 기능을 노출해도 되는가 */
    val isBlockable: Boolean get() = this is PhoneNumber
}

/**
 * 통화 녹음 파일명 파서.
 *
 * 실측 예시
 *   통화 막내딸_260809_133433.m4a      → ContactName("막내딸")
 *   통화 01076352857_260809_135255.m4a → PhoneNumber("01076352857")
 */
object CallRecordingNameParser {

    /** "통화 {정체}_{YYMMDD}_{HHMMSS}" — 로케일에 따라 접두어가 다를 수 있습니다 */
    private val SAMSUNG_FORMAT = Regex("""^(?:통화|Call)\s+(.+?)_(\d{6})_(\d{6})""")

    /** 접두어가 다른 기기용 폴백 — 뒤쪽 타임스탬프만 잡습니다 */
    private val TIMESTAMP_ONLY = Regex("""(.+?)_(\d{6})_(\d{6})""")

    private val KR_MOBILE = Regex("""01[016789][-_. ]?\d{3,4}[-_. ]?\d{4}""")
    private val KR_LANDLINE = Regex("""0\d{1,2}[-_. ]?\d{3,4}[-_. ]?\d{4}""")

    private val TIMESTAMP_FORMAT =
        DateTimeFormatter.ofPattern("yyMMdd_HHmmss", Locale.KOREA)

    fun parse(fileName: String): Pair<CallerIdentity, LocalDateTime?> {
        val match = SAMSUNG_FORMAT.find(fileName)
            ?: TIMESTAMP_ONLY.find(fileName)
            ?: return extractNumberOnly(fileName) to null

        val raw = match.groupValues[1]
            .removePrefix("통화").removePrefix("Call").trim()

        val identity = when {
            raw.isBlank() -> CallerIdentity.Unknown
            isPhoneNumber(raw) -> CallerIdentity.PhoneNumber(raw)
            else -> CallerIdentity.ContactName(raw)
        }

        val startedAt = runCatching {
            LocalDateTime.parse(
                "${match.groupValues[2]}_${match.groupValues[3]}",
                TIMESTAMP_FORMAT,
            )
        }.getOrNull()

        return identity to startedAt
    }

    private fun extractNumberOnly(fileName: String): CallerIdentity {
        val number = KR_MOBILE.find(fileName)?.value
            ?: KR_LANDLINE.find(fileName)?.value
        return if (number != null) CallerIdentity.PhoneNumber(number)
        else CallerIdentity.Unknown
    }

    private fun isPhoneNumber(text: String): Boolean =
        KR_MOBILE.matches(text) || KR_LANDLINE.matches(text)
}
