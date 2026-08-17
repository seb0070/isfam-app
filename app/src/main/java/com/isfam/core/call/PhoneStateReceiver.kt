package com.isfam.core.call

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf

/**
 * 통화 상태 수신기.
 *
 * PHONE_STATE 는 안드로이드가 앱을 깨워주는 브로드캐스트라
 * 포그라운드 서비스가 죽어 있어도 수신/발신 정보를 기록할 수 있습니다.
 * (실기기 검증 완료)
 *
 * 여기서는 상태 전이만 판별하고 무거운 작업은 WorkManager 로 넘깁니다.
 * 브로드캐스트 리시버는 실행 시간이 짧아야 하고,
 * Android 12+ 에서는 백그라운드 서비스 시작도 제한됩니다.
 *
 * 상태 전이
 *   RINGING → OFFHOOK → IDLE  = 수신 (받음)
 *   RINGING → IDLE            = 수신 (거절·부재중, 녹음 없음)
 *           → OFFHOOK → IDLE  = 발신
 */
class PhoneStateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return

        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val lastState = prefs.getString(KEY_LAST_STATE, TelephonyManager.EXTRA_STATE_IDLE)
        val now = System.currentTimeMillis()

        // EXTRA_INCOMING_NUMBER 는 READ_CALL_LOG 가 있어야 채워집니다.
        // Play 정책상 기본 전화앱에만 허용되므로 사용하지 않습니다.
        // 발신자 정보는 녹음 파일명에서 얻습니다.

        when (state) {
            TelephonyManager.EXTRA_STATE_RINGING -> {
                prefs.edit()
                    .putBoolean(KEY_SAW_RINGING, true)
                    .putString(KEY_LAST_STATE, state)
                    .apply()
            }

            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                prefs.edit()
                    .putLong(KEY_OFFHOOK_AT, now)
                    .putString(KEY_LAST_STATE, state)
                    .apply()
            }

            TelephonyManager.EXTRA_STATE_IDLE -> {
                if (lastState == TelephonyManager.EXTRA_STATE_OFFHOOK) {
                    val isIncoming = prefs.getBoolean(KEY_SAW_RINGING, false)
                    val startedAt = prefs.getLong(KEY_OFFHOOK_AT, now)
                    enqueueCallEnded(context, startedAt, now, isIncoming)
                }
                prefs.edit()
                    .putBoolean(KEY_SAW_RINGING, false)
                    .putString(KEY_LAST_STATE, state)
                    .apply()
            }
        }
    }

    /**
     * 통화 종료 처리를 WorkManager 에 위임합니다.
     *
     * 녹음 파일이 나타나기까지 10~30초가 걸려 리시버 안에서 기다릴 수 없습니다.
     * WorkManager 는 앱이 죽어도 작업을 이어갑니다.
     */
    private fun enqueueCallEnded(
        context: Context,
        startedAt: Long,
        endedAt: Long,
        isIncoming: Boolean,
    ) {
        val request = OneTimeWorkRequestBuilder<CallAnalysisWorker>()
            .setInputData(
                workDataOf(
                    CallAnalysisWorker.KEY_STARTED_AT to startedAt,
                    CallAnalysisWorker.KEY_ENDED_AT to endedAt,
                    CallAnalysisWorker.KEY_IS_INCOMING to isIncoming,
                )
            )
            .build()

        WorkManager.getInstance(context).enqueue(request)
    }

    companion object {
        private const val PREFS = "isfam_call_state"
        private const val KEY_LAST_STATE = "last_state"
        private const val KEY_SAW_RINGING = "saw_ringing"
        private const val KEY_OFFHOOK_AT = "offhook_at"
    }
}
