package com.isfam.core.ml

import com.isfam.data.api.AntiSpoofingResponse
import com.isfam.data.api.IsFamApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

data class HybridVoiceResult(
    val decision: Decision,
    val family: LocalSpeakerVerificationResult,
    val deepVoice: AntiSpoofingResponse,
    val reason: String,
)

/**
 * 가족 유사도는 폰에서, 딥보이스 탐지는 FastAPI에서 동시에 수행한 뒤 합칩니다.
 * Java 서버가 프록시를 구현하면 IsFamApi의 BASE_URL만 Java 서버로 두면 됩니다.
 */
class HybridVoiceVerifier(
    private val decoder: AndroidAudioDecoder,
    private val speakerVerifier: OnDeviceSpeakerVerifier,
    private val voiceprintStore: EncryptedVoiceprintStore,
    private val api: IsFamApi,
) {
    suspend fun verify(callAudio: File): HybridVoiceResult = coroutineScope {
        val local = async(Dispatchers.Default) {
            val samples = decoder.decode(callAudio)
            speakerVerifier.verify(samples, voiceprintStore.loadAll())
        }
        val remote = async { detectDeepVoice(callAudio) }
        combine(local.await(), remote.await())
    }

    private suspend fun detectDeepVoice(file: File): AntiSpoofingResponse {
        val mediaType = when (file.extension.lowercase()) {
            "wav" -> "audio/wav"
            "mp3" -> "audio/mpeg"
            "m4a" -> "audio/mp4"
            else -> "application/octet-stream"
        }.toMediaTypeOrNull()
        val part = MultipartBody.Part.createFormData(
            "audio_file",
            file.name,
            file.asRequestBody(mediaType),
        )
        return api.detectSpoofing(part)
    }

    internal fun combine(
        family: LocalSpeakerVerificationResult,
        deepVoice: AntiSpoofingResponse,
    ): HybridVoiceResult {
        val status = deepVoice.analysisStatus ?: "complete"
        val (decision, reason) = when {
            status == "more_voice_required" ->
                Decision.CAUTION to "통화 음질이 불안정해 추가 확인이 필요합니다."
            deepVoice.isSpoofed ->
                Decision.DANGER to "합성 음성 가능성이 감지되었습니다."
            family.isRegisteredFamily ->
                Decision.SAFE to "등록된 가족 목소리와 일치하고 합성 음성 징후가 낮습니다."
            else ->
                Decision.CAUTION to "등록된 가족 목소리와 일치하지 않아 확인이 필요합니다."
        }
        return HybridVoiceResult(decision, family, deepVoice, reason)
    }
}
