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
 * 실측 예시 (기기·One UI 버전에 따라 접두어가 다릅니다)
 *   통화 녹음 01000000000_260817_130530.m4a → PhoneNumber("01000000000")
 *   통화 막내딸_260809_133433.m4a           → ContactName("막내딸")
 *   Call 01012345678_260817_130530.m4a      → PhoneNumber("01012345678")
 *
 * 접두어를 고정하지 않고 "뒤쪽 타임스탬프"를 기준으로 자릅니다.
 * 접두어 목록에 없는 문구가 나와도 동작하게 하기 위해서입니다.
 */
object CallRecordingNameParser {

    /**
     * 파일명 "끝"의 YYMMDD_HHMMSS 를 앵커로 잡고 그 앞을 정체로 봅니다.
     *
     * ⚠️ 반드시 끝($)에 고정해야 합니다.
     *    앞에서부터 찾으면 01000000000 같은 11자리 번호의 뒷부분을
     *    타임스탬프로 잘못 읽습니다. (실제로 겪은 버그)
     *
     * 구분자가 _ 인지 공백인지도 기기마다 달라 [_ ]* 로 받습니다.
     *   실측: "통화 녹음 01000000000_260817 _130530"
     */
    private val TIMESTAMP_ANCHOR =
        Regex("""^(.*?)[_ ]*(\d{6})[_ ]*(\d{6})\s*$""")

    /**
     * 알려진 접두어. 정체 앞에 붙은 것만 제거합니다.
     * 긴 것부터 시도해야 "통화 녹음"이 "통화"로 잘리지 않습니다.
     */
    private val PREFIXES = listOf(
        "통화 녹음", "전화 녹음", "통화녹음", "음성 녹음",
        "Call recording", "Voice call", "통화", "Call",
    )

    private val KR_MOBILE = Regex("""01[016789][-_. ]?\d{3,4}[-_. ]?\d{4}""")
    private val KR_LANDLINE = Regex("""0\d{1,2}[-_. ]?\d{3,4}[-_. ]?\d{4}""")

    private val TIMESTAMP_FORMAT =
        DateTimeFormatter.ofPattern("yyMMdd_HHmmss", Locale.KOREA)

    fun parse(fileName: String): Pair<CallerIdentity, LocalDateTime?> {
        val base = fileName.substringBeforeLast('.')
        val match = TIMESTAMP_ANCHOR.find(base)
            ?: return extractNumberOnly(base) to null

        val identity = toIdentity(stripPrefix(match.groupValues[1]))

        val startedAt = runCatching {
            LocalDateTime.parse(
                "${match.groupValues[2]}_${match.groupValues[3]}",
                TIMESTAMP_FORMAT,
            )
        }.getOrNull()

        return identity to startedAt
    }

    /** 접두어를 제거합니다. 긴 것부터 확인해 부분 일치를 피합니다. */
    private fun stripPrefix(raw: String): String {
        var text = raw.trim()
        for (prefix in PREFIXES.sortedByDescending { it.length }) {
            if (text.startsWith(prefix, ignoreCase = true)) {
                text = text.removePrefix(text.take(prefix.length))
                break
            }
        }
        return text.trim().trim('_', '-', ' ')
    }

    private fun toIdentity(raw: String): CallerIdentity = when {
        raw.isBlank() -> CallerIdentity.Unknown
        isPhoneNumber(raw) -> CallerIdentity.PhoneNumber(raw)
        // 접두어 목록에 없는 문구가 남았지만 안에 번호가 있는 경우
        containsPhoneNumber(raw) -> CallerIdentity.PhoneNumber(extractNumber(raw)!!)
        else -> CallerIdentity.ContactName(raw)
    }

    private fun extractNumberOnly(text: String): CallerIdentity =
        extractNumber(text)?.let { CallerIdentity.PhoneNumber(it) } ?: CallerIdentity.Unknown

    private fun extractNumber(text: String): String? =
        KR_MOBILE.find(text)?.value ?: KR_LANDLINE.find(text)?.value

    private fun containsPhoneNumber(text: String): Boolean = extractNumber(text) != null

    private fun isPhoneNumber(text: String): Boolean =
        KR_MOBILE.matches(text) || KR_LANDLINE.matches(text)
}