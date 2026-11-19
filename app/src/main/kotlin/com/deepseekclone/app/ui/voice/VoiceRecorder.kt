package com.deepseekclone.app.ui.voice

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import java.io.ByteArrayOutputStream
import kotlin.concurrent.thread

/**
 * 录音：16kHz 单声道 16bit PCM，停止时封成 WAV，供 ASR 接口使用。
 */
class VoiceRecorder {

    private var audioRecord: AudioRecord? = null
    private var worker: Thread? = null
    @Volatile private var running = false
    private val pcm = ByteArrayOutputStream()
    var isRecording: Boolean = false
        private set

    fun start(): Boolean {
        if (isRecording) return true
        val minBuffer = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL, ENCODING)
        if (minBuffer <= 0) return false
        val bufferSize = maxOf(minBuffer * 2, 4096)
        val record = try {
            AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL,
                ENCODING,
                bufferSize,
            )
        } catch (e: Exception) {
            return false
        }
        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            return false
        }
        pcm.reset()
        audioRecord = record
        running = true
        isRecording = true
        record.startRecording()
        worker = thread(name = "voice-recorder") {
            val buffer = ByteArray(bufferSize)
            while (running) {
                val read = record.read(buffer, 0, buffer.size)
                if (read > 0) pcm.write(buffer, 0, read)
            }
        }
        return true
    }

    /** 停止录音并返回 WAV 字节；无有效音频时返回 null。 */
    fun stop(): ByteArray? {
        if (!isRecording) return null
        isRecording = false
        running = false
        runCatching { worker?.join(300) }
        worker = null
        runCatching { audioRecord?.stop() }
        runCatching { audioRecord?.release() }
        audioRecord = null
        val raw = pcm.toByteArray()
        pcm.reset()
        if (raw.size < SAMPLE_RATE / 2) return null // 少于 0.5 秒视为无效
        return wrapWav(raw)
    }

    private fun wrapWav(data: ByteArray): ByteArray {
        val dataSize = data.size
        val byteRate = SAMPLE_RATE * 2
        val header = ByteArrayOutputStream().apply {
            write("RIFF".toByteArray())
            writeLe32(36 + dataSize)
            write("WAVE".toByteArray())
            write("fmt ".toByteArray())
            writeLe32(16)
            writeLe16(1) // PCM
            writeLe16(1) // mono
            writeLe32(SAMPLE_RATE)
            writeLe32(byteRate)
            writeLe16(2) // block align
            writeLe16(16) // bits per sample
            write("data".toByteArray())
            writeLe32(dataSize)
        }.toByteArray()
        return header + data
    }

    private fun ByteArrayOutputStream.writeLe32(value: Int) {
        write(value and 0xFF)
        write((value ushr 8) and 0xFF)
        write((value ushr 16) and 0xFF)
        write((value ushr 24) and 0xFF)
    }

    private fun ByteArrayOutputStream.writeLe16(value: Int) {
        write(value and 0xFF)
        write((value ushr 8) and 0xFF)
    }

    companion object {
        const val SAMPLE_RATE = 16_000
        private const val CHANNEL = AudioFormat.CHANNEL_IN_MONO
        private const val ENCODING = AudioFormat.ENCODING_PCM_16BIT
    }
}
