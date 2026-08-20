package com.isfam.core.call

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.time.ZoneId

/**
 * 통화 녹음 파일 탐색.
 *
 * MediaStore 로 자동 탐색합니다. 사용자가 폴더를 지정할 필요가 없습니다.
 * (실기기 검증 완료 — READ_MEDIA_AUDIO 권한만으로 동작)
 *
 * Android 12(API 31)부터 Recordings/ 가 정식 미디어 컬렉션이 되고
 * IS_RECORDING 컬럼이 추가되어 통화 녹음만 정확히 골라낼 수 있습니다.
 */
class CallRecordingSource(private val context: Context) {

    private val collection
        get() = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)

    private val hasIsRecordingColumn = Build.VERSION.SDK_INT >= 31

    /**
     * 최근 통화 녹음 목록. 최신순.
     *
     * @param limit 가져올 최대 개수
     * @param modifiedAfterMillis 이 시각 이후 파일만
     */
    suspend fun findRecent(
        limit: Int = 20,
        modifiedAfterMillis: Long? = null,
    ): List<CallRecording> = withContext(Dispatchers.IO) {
        val projection = buildList {
            add(MediaStore.Audio.Media._ID)
            add(MediaStore.Audio.Media.DISPLAY_NAME)
            add(MediaStore.Audio.Media.SIZE)
            add(MediaStore.Audio.Media.DURATION)
            add(MediaStore.Audio.Media.DATE_MODIFIED)
            add(MediaStore.Audio.Media.RELATIVE_PATH)
            if (hasIsRecordingColumn) add(MediaStore.Audio.Media.IS_RECORDING)
        }.toTypedArray()

        val selection = modifiedAfterMillis?.let {
            "${MediaStore.Audio.Media.DATE_MODIFIED} >= ?"
        }
        val args = modifiedAfterMillis?.let { arrayOf((it / 1000).toString()) }

        val cursor = runCatching {
            context.contentResolver.query(
                collection, projection, selection, args,
                "${MediaStore.Audio.Media.DATE_MODIFIED} DESC",
            )
        }.getOrNull() ?: return@withContext emptyList()

        cursor.use {
            val idCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val nameCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val sizeCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val durCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dateCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)
            val pathCol = it.getColumnIndex(MediaStore.Audio.Media.RELATIVE_PATH)
            val recCol = if (hasIsRecordingColumn)
                it.getColumnIndex(MediaStore.Audio.Media.IS_RECORDING) else -1

            val result = ArrayList<CallRecording>()
            while (it.moveToNext() && result.size < limit) {
                val name = it.getString(nameCol) ?: continue
                val path = if (pathCol >= 0) it.getString(pathCol) else null
                val isRecording = if (recCol >= 0) it.getInt(recCol) == 1 else false

                if (!looksLikeCallRecording(name, path, isRecording)) continue

                val (identity, startedAt) = CallRecordingNameParser.parse(name)
                result += CallRecording(
                    uri = ContentUris.withAppendedId(collection, it.getLong(idCol)),
                    fileName = name,
                    sizeBytes = it.getLong(sizeCol),
                    durationMs = it.getLong(durCol),
                    modifiedAtMillis = it.getLong(dateCol) * 1000,
                    identity = identity,
                    startedAt = startedAt,
                )
            }
            result
        }
    }

    /** 통화 녹음으로 보이는가 */
    private fun looksLikeCallRecording(
        name: String,
        relativePath: String?,
        isRecordingFlag: Boolean,
    ): Boolean {
        if (isRecordingFlag) return true
        val path = relativePath?.lowercase().orEmpty()
        if (path.contains("call")) return true
        val lower = name.lowercase()
        return lower.startsWith("통화") || lower.startsWith("call")
    }

    /**
     * 이번 통화의 녹음 파일이 나타날 때까지 기다립니다.
     *
     * ⚠️ "종료 시각 이후에 생긴 파일"만으로 찾으면 안 됩니다.
     *
     *    통화가 연달아 오면 앞 통화의 Worker 가 기다리는 사이
     *    뒤 통화의 녹음 파일이 생기고, 앞 Worker 가 그것을
     *    집어갑니다. (실제로 겪은 버그 — +66.6초 만에 다른 통화
     *    파일을 가져감)
     *
     *    파일명에 통화 시작 시각이 들어 있으므로 그것으로
     *    대조해 이번 통화의 파일인지 확인합니다.
     *
     * 실측 기준 파일 등장까지 0.1~15초입니다.
     * 30초를 넘기면 자동녹음이 꺼져 있다고 보는 편이 낫습니다.
     *
     * @param callStartedAt 통화 시작 시각 (OFFHOOK)
     * @param callEndedAt 통화 종료 시각 (IDLE)
     * @return 이번 통화의 파일. 못 찾으면 null
     */
    suspend fun awaitRecordingFor(
        callStartedAt: Long,
        callEndedAt: Long,
        timeoutMs: Long = 30_000,
        pollIntervalMs: Long = 1_000,
    ): CallRecording? {
        val deadline = System.currentTimeMillis() + timeoutMs

        while (System.currentTimeMillis() < deadline) {
            val candidates = findRecent(limit = 5, modifiedAfterMillis = callStartedAt - 5_000)
            val matched = candidates.firstOrNull { it.matchesCall(callStartedAt, callEndedAt) }
            if (matched != null) return matched

            // 후보는 있는데 이번 통화 것이 아니면 다른 통화의 파일입니다
            if (candidates.isNotEmpty()) {
                CallLog.d("후보 ${candidates.size}개 있으나 이번 통화와 불일치 — 계속 대기")
            }
            delay(pollIntervalMs)
        }
        return null
    }

    /**
     * 이 녹음이 해당 통화의 것인가.
     *
     * 파일명의 시각이 통화 구간 안에 있고, 녹음 길이가
     * 통화 길이와 크게 다르지 않아야 합니다.
     */
    private fun CallRecording.matchesCall(
        callStartedAt: Long,
        callEndedAt: Long,
    ): Boolean {
        val fileStart = startedAtMillis() ?: return false

        // 파일명 시각은 초 단위라 오차가 있습니다.
        // 통화 시작 전후 10초 범위를 인정합니다.
        val withinWindow = fileStart in (callStartedAt - 10_000)..(callEndedAt + 10_000)
        if (!withinWindow) return false

        // 녹음 길이가 통화 길이와 비슷한지.
        // 녹음은 연결된 뒤 시작하므로 통화보다 조금 짧습니다.
        val callSec = ((callEndedAt - callStartedAt) / 1000).toInt()
        val diff = kotlin.math.abs(durationSec - callSec)
        return diff <= 15
    }

    /**
     * 파일 쓰기가 끝날 때까지 기다립니다.
     *
     * 다이얼러가 아직 쓰는 중인 파일을 읽으면 깨진 오디오가 나옵니다.
     * 크기가 [stableForMs] 동안 변하지 않으면 완료로 봅니다.
     * 실측 결과 약 2.5초가 걸립니다.
     *
     * @return 안정화된 파일. 타임아웃이면 null.
     */
    suspend fun awaitStable(
        recording: CallRecording,
        stableForMs: Long = 2_000,
        timeoutMs: Long = 60_000,
        pollIntervalMs: Long = 500,
    ): CallRecording? = withContext(Dispatchers.IO) {
        val deadline = System.currentTimeMillis() + timeoutMs
        var lastSize = -1L
        var stableSince = 0L

        while (System.currentTimeMillis() < deadline) {
            val size = currentSize(recording) ?: -1L

            if (size > 0 && size == lastSize) {
                if (stableSince == 0L) stableSince = System.currentTimeMillis()
                if (System.currentTimeMillis() - stableSince >= stableForMs) {
                    return@withContext recording.copy(sizeBytes = size)
                }
            } else {
                stableSince = 0L
                lastSize = size
            }
            delay(pollIntervalMs)
        }
        null
    }

    private fun currentSize(recording: CallRecording): Long? = runCatching {
        context.contentResolver
            .query(recording.uri, arrayOf(MediaStore.Audio.Media.SIZE), null, null, null)
            ?.use { if (it.moveToFirst()) it.getLong(0) else null }
    }.getOrNull()

    /**
     * 자동 통화녹음이 실제로 동작하는지 확인합니다.
     *
     * ⚠️ 삼성 전화 앱의 설정을 직접 읽을 방법이 없습니다.
     *    녹음 파일이 생기는지로만 간접 확인할 수 있어,
     *    첫 통화 전까지는 Unknown 입니다.
     */
    suspend fun checkCapability(): RecordingCapability {
        val recent = findRecent(limit = 1)
        return if (recent.isNotEmpty()) RecordingCapability.Confirmed
        else RecordingCapability.Unknown
    }
}

enum class RecordingCapability {
    /** 첫 통화 전 — 확인 불가 */
    Unknown,
    /** 녹음 파일 발견 — 정상 동작 확인 */
    Confirmed,
    /** 통화 후에도 파일이 없음 — 미지원 기기이거나 설정이 꺼져 있음 */
    NotWorking,
}

/** 파일 시각을 로컬 시간대 epoch millis 로 */
internal fun CallRecording.startedAtMillis(): Long? =
    startedAt?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()