package com.isfam.core.ml

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import java.io.ByteArrayOutputStream
import java.io.File
import com.isfam.core.audio.Resampler
import java.nio.ByteOrder

/**
 * m4a/wav 통화·등록 파일을 ONNX 입력용 mono 16 kHz PCM 으로 변환합니다.
 *
 * 리샘플링은 core/audio/Resampler 를 씁니다.
 *
 * ⚠️ 선형 보간을 쓰면 안 됩니다.
 *    통화 녹음은 44.1kHz 또는 48kHz 로 들어오는데,
 *    16kHz 로 줄일 때 나이퀴스트(8kHz)를 넘는 성분을 먼저 제거하지
 *    않으면 그 성분이 아래 대역으로 접혀 들어와 임베딩을 왜곡합니다.
 *    특히 44.1→16 은 2.75:1 비정수 비율이라 오차가 더 큽니다.
 *
 *    Resampler 는 windowed-sinc(Lanczos) 로 저역통과를 함께 적용합니다.
 *    실기기 검증: 12kHz 사인파 → RMS 0.0061 로 억제,
 *                1kHz 사인파  → 비율 1.000 으로 보존
 */
class AndroidAudioDecoder {
    fun decode(file: File): FloatArray {
        require(file.isFile && file.length() > 0) { "audio file is empty: ${file.name}" }
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(file.absolutePath)
            val trackIndex = (0 until extractor.trackCount).firstOrNull { index ->
                extractor.getTrackFormat(index).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: error("audio track not found: ${file.name}")
            extractor.selectTrack(trackIndex)
            val inputFormat = extractor.getTrackFormat(trackIndex)
            val mime = inputFormat.getString(MediaFormat.KEY_MIME) ?: error("audio mime is missing")
            val codec = MediaCodec.createDecoderByType(mime)
            try {
                codec.configure(inputFormat, null, null, 0)
                codec.start()
                return decodePcm(extractor, codec, inputFormat)
            } finally {
                runCatching { codec.stop() }
                codec.release()
            }
        } finally {
            extractor.release()
        }
    }

    private fun decodePcm(
        extractor: MediaExtractor,
        codec: MediaCodec,
        initialFormat: MediaFormat,
    ): FloatArray {
        val bytes = ByteArrayOutputStream()
        val info = MediaCodec.BufferInfo()
        var inputDone = false
        var outputDone = false
        var sampleRate = initialFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        var channels = initialFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)

        while (!outputDone) {
            if (!inputDone) {
                val inputIndex = codec.dequeueInputBuffer(10_000)
                if (inputIndex >= 0) {
                    val input = codec.getInputBuffer(inputIndex) ?: error("decoder input buffer missing")
                    val size = extractor.readSampleData(input, 0)
                    if (size < 0) {
                        codec.queueInputBuffer(
                            inputIndex, 0, 0, 0,
                            MediaCodec.BUFFER_FLAG_END_OF_STREAM,
                        )
                        inputDone = true
                    } else {
                        codec.queueInputBuffer(inputIndex, 0, size, extractor.sampleTime, 0)
                        extractor.advance()
                    }
                }
            }

            when (val outputIndex = codec.dequeueOutputBuffer(info, 10_000)) {
                MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    val format = codec.outputFormat
                    sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                }
                MediaCodec.INFO_TRY_AGAIN_LATER -> Unit
                else -> if (outputIndex >= 0) {
                    codec.getOutputBuffer(outputIndex)?.let { output ->
                        if (info.size > 0) {
                            output.position(info.offset)
                            output.limit(info.offset + info.size)
                            val chunk = ByteArray(info.size)
                            output.get(chunk)
                            bytes.write(chunk)
                        }
                    }
                    outputDone = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                    codec.releaseOutputBuffer(outputIndex, false)
                }
            }
        }
        val mono = pcm16ToMono(bytes.toByteArray(), channels)
        return if (sampleRate == SpeakerFbankExtractor.SAMPLE_RATE) mono
        else Resampler.resample(mono, sampleRate, SpeakerFbankExtractor.SAMPLE_RATE)
    }

    private fun pcm16ToMono(bytes: ByteArray, channels: Int): FloatArray {
        require(channels > 0) { "invalid channel count" }
        val buffer = java.nio.ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
        val frameCount = buffer.remaining() / channels
        return FloatArray(frameCount) {
            var sum = 0f
            repeat(channels) { sum += buffer.get() / 32768f }
            sum / channels
        }
    }
}