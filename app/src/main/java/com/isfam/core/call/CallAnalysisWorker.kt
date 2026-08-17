package com.isfam.core.call

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * 통화 종료 후 분석 파이프라인.
 *
 * WorkManager 로 실행하는 이유는 녹음 파일이 나타나기까지 10~30초가
 * 걸리기 때문입니다. 브로드캐스트 리시버 안에서는 기다릴 수 없고,
 * Android 12+ 에서는 백그라운드 서비스 시작도 제한됩니다.
 * WorkManager 는 앱이 죽어도 작업을 이어갑니다.
 *
 * 흐름
 *   통화 이벤트 저장
 *     → 발신이면 종료
 *     → 녹음 파일 대기 (최대 90초)
 *     → 쓰기 완료 대기
 *     → 게이트 판정
 *     → 오디오 전처리 · 분석  (#14 에서 연결)
 *     → 결과 저장 · 위험이면 알림  (#10 에서 연결)
 */
class CallAnalysisWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val startedAt = inputData.getLong(KEY_STARTED_AT, 0L)
        val endedAt = inputData.getLong(KEY_ENDED_AT, 0L)
        val isIncoming = inputData.getBoolean(KEY_IS_INCOMING, false)

        if (startedAt == 0L || endedAt == 0L) return Result.failure()

        // TODO: CallEventDao 주입 후 이벤트 저장
        // val eventId = callEventDao.insert(
        //     CallEventEntity(
        //         startedAtMillis = startedAt,
        //         endedAtMillis = endedAt,
        //         isIncoming = isIncoming,
        //     )
        // )

        // 발신 통화는 분석하지 않습니다
        if (!isIncoming) return Result.success()

        val source = CallRecordingSource(applicationContext)

        // 녹음 파일이 나타날 때까지 대기
        val found = source.awaitNewRecording(since = endedAt)
            ?: return Result.success()   // 자동녹음이 꺼져 있을 수 있습니다

        // 다이얼러가 파일을 다 쓸 때까지 대기
        val stable = source.awaitStable(found)
            ?: return Result.retry()

        // 분석 대상인지 판정
        // TODO: 등록 가족 이름을 Repository 에서 가져옵니다
        val registeredNames = emptySet<String>()
        val decision = CallGate.decide(stable, isIncoming, registeredNames)

        if (decision is CallGate.Decision.Skip) {
            // TODO: callEventDao.markAnalyzed(eventId)
            return Result.success()
        }

        // TODO: #14 오디오 전처리 → 분석 → 결과 저장
        // val audio = AudioPipeline(applicationContext).prepare(stable.uri)
        // val result = analyzer.analyze(audio)
        // TODO: #10 위험이면 알림 발송

        return Result.success()
    }

    companion object {
        const val KEY_STARTED_AT = "started_at"
        const val KEY_ENDED_AT = "ended_at"
        const val KEY_IS_INCOMING = "is_incoming"
    }
}
