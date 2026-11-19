package com.deepseekclone.app.ui.voice

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack

/** 播放 TTS 返回的裸 PCM（24kHz 单声道 16bit 小端）。 */
object PcmPlayer {

    private var track: AudioTrack? = null

    @Synchronized
    fun play(pcm: ByteArray) {
        stop()
        if (pcm.isEmpty()) return
        val minBuffer = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL, ENCODING)
        val bufferSize = maxOf(minBuffer, pcm.size * 2)
        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(ENCODING)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(CHANNEL)
                    .build(),
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        audioTrack.write(pcm, 0, pcm.size)
        audioTrack.play()
        track = audioTrack
    }

    @Synchronized
    fun stop() {
        runCatching { track?.stop() }
        runCatching { track?.release() }
        track = null
    }

    const val SAMPLE_RATE = 24_000
    private const val CHANNEL = AudioFormat.CHANNEL_OUT_MONO
    private const val ENCODING = AudioFormat.ENCODING_PCM_16BIT
}
