package com.isfam.core.call

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.isfam.core.audio.AudioPipeline

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

        val durationSec = (endedAt - startedAt) / 1000
        CallLog.d("통화 종료 · ${if (isIncoming) "수신" else "발신"} · ${durationSec}초")

        // TODO: CallEventDao 주입 후 이벤트 저장
        // val eventId = callEventDao.insert(
        //     CallEventEntity(
        //         startedAtMillis = startedAt,
        //         endedAtMillis = endedAt,
        //         isIncoming = isIncoming,
        //     )
        // )

        // 발신 통화는 분석하지 않습니다
        if (!isIncoming) {
            CallLog.d("발신 통화 — 분석하지 않음")
            return Result.success()
        }

        val source = CallRecordingSource(applicationContext)

        // 녹음 파일이 나타날 때까지 대기
        CallLog.d("녹음 파일 대기 시작…")
        val found = source.awaitNewRecording(since = endedAt)
        if (found == null) {
            // 자동녹음이 꺼져 있거나 지원하지 않는 기기일 수 있습니다
            CallLog.w("90초 내 녹음 파일 없음 — 자동녹음 설정 확인 필요")
            return Result.success()
        }
        CallLog.d(
            "파일 발견 (+%.1f초) · %s".format(
                (System.currentTimeMillis() - endedAt) / 1000f,
                CallLog.mask(found.identity),
            )
        )

        // 다이얼러가 파일을 다 쓸 때까지 대기
        val stable = source.awaitStable(found)
        if (stable == null) {
            CallLog.w("파일 쓰기가 끝나지 않음 — 재시도")
            return Result.retry()
        }
        CallLog.d(
            "쓰기 완료 (총 +%.1f초) · %dKB · %d초".format(
                (System.currentTimeMillis() - endedAt) / 1000f,
                stable.sizeBytes / 1024,
                stable.durationSec,
            )
        )

        // 분석 대상인지 판정
        // TODO: 등록 가족 이름을 Repository 에서 가져옵니다
        val registeredNames = emptySet<String>()
        val decision = CallGate.decide(stable, isIncoming, registeredNames)

        if (decision is CallGate.Decision.Skip) {
            CallLog.d("게이트: 건너뜀 — ${decision.reason.label}")
            // TODO: callEventDao.markAnalyzed(eventId)
            return Result.success()
        }
        CallLog.d("게이트: 분석 진행")

        // 오디오 전처리
        val prepared = AudioPipeline(applicationContext).prepare(stable.uri)

        when (prepared) {
            is AudioPipeline.Result.Failed -> {
                CallLog.w("전처리 실패: ${prepared.message}")
                return Result.success()
            }

            is AudioPipeline.Result.QualityFailed -> {
                // 불충분한 음성으로 안전·위험을 단정하지 않습니다
                CallLog.w("음질 미달: ${prepared.reason.serverCode}")
                CallLog.d(prepared.audio.summary())
                // TODO: 판정 보류(insufficient)로 기록 저장
                return Result.success()
            }

            is AudioPipeline.Result.Success -> {
                CallLog.d("전처리 완료\n${prepared.audio.summary()}")
                CallLog.d("윈도우 ${prepared.audio.windows().size}개")
                // TODO: #16 화자 분리 → 1:N 대조 → 결과 저장
                // TODO: #10 위험이면 알림 발송
            }
        }

        return Result.success()
    }

    companion object {
        const val KEY_STARTED_AT = "started_at"
        const val KEY_ENDED_AT = "ended_at"
        const val KEY_IS_INCOMING = "is_incoming"
    }
}