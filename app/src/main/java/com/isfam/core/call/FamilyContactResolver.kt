package com.isfam.core.call

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.content.ContextCompat

/**
 * 등록된 가족이 이 폰의 연락처에 어떤 이름으로 저장되어 있는지 찾습니다.
 *
 * 왜 필요한가
 *   통화 녹음 파일명에는 연락처에 저장된 이름이 그대로 들어갑니다.
 *     통화 녹음 큰딸_260817_130530.m4a
 *   우리 시스템의 display_name("김서연")과 다르기 때문에,
 *   서버 정보만으로는 이 통화가 등록 가족인지 알 수 없습니다.
 *
 * 어떻게
 *   서버에서 받은 가족 전화번호로 연락처를 역조회합니다.
 *   PhoneLookup URI 를 쓰므로 연락처 전체를 훑지 않습니다.
 *
 * 프라이버시
 *   조회 결과는 단말에만 보관하고 서버로 보내지 않습니다.
 *   연락처 이름은 이 폰 사용자의 개인정보이며, 우리가 수집할 이유가
 *   없습니다. 게이트 판정과 화면 표시에만 씁니다.
 */
class FamilyContactResolver(private val context: Context) {

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * 전화번호로 연락처 이름을 찾습니다.
     *
     * @return 저장된 이름. 연락처에 없거나 권한이 없으면 null
     */
    fun resolveName(phoneNumber: String): String? {
        if (!hasPermission()) return null
        if (phoneNumber.isBlank()) return null

        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(phoneNumber),
        )

        return runCatching {
            context.contentResolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
                null, null, null,
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        }.getOrNull()
    }

    /**
     * 가족 전원의 연락처 이름을 모읍니다. 게이트에 넘길 집합입니다.
     *
     * @param familyPhoneNumbers 서버에서 받은 가족 전화번호 목록
     */
    fun resolveAll(familyPhoneNumbers: List<String>): Set<String> =
        familyPhoneNumbers.mapNotNull(::resolveName).toSet()
}
