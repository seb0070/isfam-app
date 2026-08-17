package com.isfam.core.call

import android.util.Log
import com.isfam.BuildConfig

/**
 * 통화 파이프라인 디버그 로그.
 *
 * 백그라운드에서 돌아가는 흐름이라 화면으로는 확인할 방법이 없어
 * Logcat 으로 각 단계를 찍습니다.
 *
 * ⚠️ 릴리즈 빌드에서는 아무것도 출력하지 않습니다.
 *    발신자 이름·번호가 포함되므로 운영 환경에 남으면 안 됩니다.
 *
 * 사용법 — Logcat 검색창에 IsFamCall 입력
 */
internal object CallLog {

    private const val TAG = "IsFamCall"

    fun d(message: String) {
        if (BuildConfig.DEBUG) Log.d(TAG, message)
    }

    fun w(message: String) {
        if (BuildConfig.DEBUG) Log.w(TAG, message)
    }

    /**
     * 발신자 정보를 가립니다.
     * 디버그 로그라도 이름·번호를 그대로 남기지 않는 편이 안전합니다.
     *
     *   막내딸        → 막**
     *   01076352857  → 010****2857
     */
    fun mask(identity: CallerIdentity): String = when (identity) {
        is CallerIdentity.ContactName ->
            // 이름이 길어도 별표를 3개로 고정합니다. 길이 자체도 정보이므로
            identity.name.take(1) + "***"

        is CallerIdentity.PhoneNumber -> {
            val digits = identity.number.filter(Char::isDigit)
            if (digits.length >= 7) "${digits.take(3)}****${digits.takeLast(4)}"
            else "***"
        }

        CallerIdentity.Unknown -> "알 수 없음"
    }
}