package com.isfam.core.ml

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** 가족 voiceprint를 Android Keystore AES-GCM 키로 암호화해 저장합니다. */
class EncryptedVoiceprintStore(context: Context) {
    companion object {
        private const val KEY_ALIAS = "isfam_voiceprint_aes_v1"
        private const val PREFS_NAME = "isfam_encrypted_voiceprints"
        private const val PREFIX = "voiceprint."
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    @Synchronized
    fun save(familyId: String, embedding: FloatArray) {
        require(familyId.isNotBlank())
        require(embedding.size == 192) { "voiceprint must have 192 values" }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        cipher.updateAAD(familyId.toByteArray(Charsets.UTF_8))
        val encrypted = cipher.doFinal(floatArrayToBytes(embedding))
        val payload = Base64.encodeToString(cipher.iv, Base64.NO_WRAP) + ":" +
            Base64.encodeToString(encrypted, Base64.NO_WRAP)
        prefs.edit().putString(PREFIX + familyId, payload).apply()
    }

    @Synchronized
    fun load(familyId: String): FloatArray? {
        val payload = prefs.getString(PREFIX + familyId, null) ?: return null
        val parts = payload.split(':', limit = 2)
        require(parts.size == 2) { "invalid encrypted voiceprint" }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.DECRYPT_MODE,
            getOrCreateKey(),
            GCMParameterSpec(128, Base64.decode(parts[0], Base64.NO_WRAP)),
        )
        cipher.updateAAD(familyId.toByteArray(Charsets.UTF_8))
        return bytesToFloatArray(cipher.doFinal(Base64.decode(parts[1], Base64.NO_WRAP)))
    }

    fun loadAll(): Map<String, FloatArray> = prefs.all.keys
        .asSequence()
        .filter { it.startsWith(PREFIX) }
        .associate { key -> key.removePrefix(PREFIX) to requireNotNull(load(key.removePrefix(PREFIX))) }

    fun delete(familyId: String) = prefs.edit().remove(PREFIX + familyId).apply()

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
            )
            generateKey()
        }
    }

    private fun floatArrayToBytes(values: FloatArray): ByteArray =
        ByteBuffer.allocate(values.size * 4).order(ByteOrder.LITTLE_ENDIAN).apply {
            values.forEach(::putFloat)
        }.array()

    private fun bytesToFloatArray(bytes: ByteArray): FloatArray {
        require(bytes.size == 192 * 4) { "invalid voiceprint byte length" }
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        return FloatArray(192) { buffer.float }
    }
}
