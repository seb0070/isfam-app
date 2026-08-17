package com.isfam.core.ml

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteOrder
import kotlin.math.floor

/** m4a/wav 통화·등록 파일을 ONNX 입력용 mono 16 kHz PCM으로 변환합니다. */
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
        else linearResample(mono, sampleRate, SpeakerFbankExtractor.SAMPLE_RATE)
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

    internal fun linearResample(input: FloatArray, sourceRate: Int, targetRate: Int): FloatArray {
        require(sourceRate > 0 && targetRate > 0)
        if (input.isEmpty() || sourceRate == targetRate) return input.copyOf()
        val outputSize = (input.size.toLong() * targetRate / sourceRate).toInt().coerceAtLeast(1)
        return FloatArray(outputSize) { outputIndex ->
            val position = outputIndex.toDouble() * sourceRate / targetRate
            val left = floor(position).toInt().coerceIn(input.indices)
            val right = (left + 1).coerceAtMost(input.lastIndex)
            val fraction = (position - left).toFloat()
            input[left] * (1f - fraction) + input[right] * fraction
        }
    }
}
