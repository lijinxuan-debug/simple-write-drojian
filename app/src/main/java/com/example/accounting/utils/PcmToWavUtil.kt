package com.example.accounting.utils

import java.io.File

object PcmToWavUtil {
    fun encodePcmToWav(pcmFile: File, wavFile: File) {
        val sampleRate = 16000L
        val channels = 1
        val byteRate = 16 * sampleRate * channels / 8
        val data = pcmFile.readBytes()
        val totalAudioLen = data.size.toLong()
        val totalDataLen = totalAudioLen + 36

        wavFile.outputStream().use { out ->
            out.write("RIFF".toByteArray())
            out.write(intToBytes(totalDataLen.toInt()))
            out.write("WAVE".toByteArray())
            out.write("fmt ".toByteArray())
            out.write(intToBytes(16))
            out.write(shortToBytes(1.toShort()))
            out.write(shortToBytes(channels.toShort()))
            out.write(intToBytes(sampleRate.toInt()))
            out.write(intToBytes(byteRate.toInt()))
            out.write(shortToBytes((channels * 16 / 8).toShort()))
            out.write(shortToBytes(16.toShort()))
            out.write("data".toByteArray())
            out.write(intToBytes(totalAudioLen.toInt()))
            out.write(data)
        }
    }

    private fun intToBytes(value: Int) = byteArrayOf(
        value.toByte(), (value shr 8).toByte(), (value shr 16).toByte(), (value shr 24).toByte()
    )

    private fun shortToBytes(value: Short) = byteArrayOf(
        value.toByte(), (value.toInt() shr 8).toByte()
    )
}