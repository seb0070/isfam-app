package com.isfam.core.call

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.OnConflictStrategy

/**
 * 통화 이벤트 기록.
 *
 * PHONE_STATE 브로드캐스트는 앱이 종료돼 있어도 시스템이 깨워주므로
 * 상시 실행 서비스 없이도 수신/발신을 기록할 수 있습니다.
 *
 * 나중에 녹음 파일을 발견하면 파일명의 통화 시각과 대조해
 * 어느 이벤트인지 찾습니다.
 */
@Entity(tableName = "call_events")
data class CallEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startedAtMillis: Long,
    val endedAtMillis: Long,
    val isIncoming: Boolean,
    /** 서버에 등록된 call_event_id. 미전송이면 null */
    val serverId: Int? = null,
    val analyzed: Boolean = false,
) {
    val durationSec: Int get() = ((endedAtMillis - startedAtMillis) / 1000).toInt()
    val direction: String get() = if (isIncoming) "수신" else "발신"
}

@Dao
interface CallEventDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: CallEventEntity): Long

    /**
     * 녹음 파일의 통화 시각으로 이벤트를 찾습니다.
     * 파일명 시각과 실제 기록 시각에 약간의 오차가 있어 범위로 검색합니다.
     */
    @Query(
        """
        SELECT * FROM call_events
        WHERE ABS(startedAtMillis - :startedAtMillis) < :windowMs
        ORDER BY ABS(startedAtMillis - :startedAtMillis) ASC
        LIMIT 1
        """
    )
    suspend fun findNear(startedAtMillis: Long, windowMs: Long = 90_000): CallEventEntity?

    @Query("SELECT * FROM call_events ORDER BY endedAtMillis DESC LIMIT :limit")
    suspend fun recent(limit: Int = 50): List<CallEventEntity>

    @Query("UPDATE call_events SET analyzed = 1 WHERE id = :id")
    suspend fun markAnalyzed(id: Long)

    @Query("UPDATE call_events SET serverId = :serverId WHERE id = :id")
    suspend fun attachServerId(id: Long, serverId: Int)

    /** 오래된 기록 정리 */
    @Query("DELETE FROM call_events WHERE endedAtMillis < :beforeMillis")
    suspend fun deleteOlderThan(beforeMillis: Long)
}
