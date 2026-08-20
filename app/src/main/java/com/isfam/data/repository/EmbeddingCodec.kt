package com.isfam.core.ml

import android.util.Base64
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * 임베딩 ↔ base64 변환.
 *
 * 서버는 오디오를 받지 않습니다. 앱이 ONNX 로 추출한 임베딩만 보냅니다.
 * 등록용 음성이 서버에 도달하는 순간 딥보이스 판정과 같은 법적 의무
 * (암호화 전송 · 접속기록 · 즉시 파기 · 처리방침)가 붙기 때문입니다.
 *
 * 서버 규격
 *   float32[192] = 768바이트 → base64
 *   길이가 다르거나 디코딩이 안 되면 VOICE_001(422)
 *
 * ⚠️ 바이트 순서를 맞춰야 합니다.
 *    안드로이드(ARM)는 리틀엔디안이고 서버 JVM 은 빅엔디안이 기본입니다.
 *    명시하지 않으면 같은 임베딩이 서로 다른 값으로 읽힙니다.
 */
object EmbeddingCodec {

    /** ECAPA-TDNN 출력 차원 */
    const val DIMENSION = 192

    /** 서버가 기대하는 바이트 수 */
    const val BYTE_SIZE = DIMENSION * 4

    /**
     * 서버가 대조 가능한지 판단하는 기준입니다.
     * 이 값보다 낮으면 MODEL_001(422)로 거절되고 앱은 업데이트를 유도해야 합니다.
     *
     * TODO: GET /model-info 응답으로 교체. 지금은 자산에 넣은 모델 기준입니다.
     */
    const val MODEL_VERSION = "ecapa-tdnn-v1"

    /**
     * float32[192] → base64.
     *
     * @throws IllegalArgumentException 차원이 192가 아니면
     */
    fun encode(embedding: FloatArray): String {
        require(embedding.size == DIMENSION) {
            "임베딩 차원이 맞지 않습니다: ${embedding.size} (기대 $DIMENSION)"
        }

        val buffer = ByteBuffer.allocate(BYTE_SIZE).order(ByteOrder.LITTLE_ENDIAN)
        embedding.forEach(buffer::putFloat)

        return Base64.encodeToString(buffer.array(), Base64.NO_WRAP)
    }

    /**
     * base64 → float32[192].
     *
     * GET /family/embeddings 로 받은 가족 임베딩을 푸는 데 씁니다.
     *
     * @return 실패하면 null. 서버 응답이 손상됐거나 모델 버전이 다른 경우입니다.
     */
    fun decode(encoded: String): FloatArray? = runCatching {
        val bytes = Base64.decode(encoded, Base64.NO_WRAP)
        require(bytes.size == BYTE_SIZE) {
            "임베딩 바이트 길이가 맞지 않습니다: ${bytes.size} (기대 $BYTE_SIZE)"
        }

        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        FloatArray(DIMENSION) { buffer.float }
    }.getOrNull()
}
