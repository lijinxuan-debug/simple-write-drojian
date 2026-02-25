package com.example.accounting.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.accounting.utils.PcmToWavUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class VoiceViewModel(application: Application) : AndroidViewModel(application) {

    private var isRecording = false
    private val context = getApplication<Application>()

    // 录音逻辑
    @SuppressLint("MissingPermission") // 权限由 Activity 检查
    fun startRecording() {
        isRecording = true
        viewModelScope.launch(Dispatchers.IO) {
            val bufferSize = AudioRecord.getMinBufferSize(16000, 
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
            val audioRecord = AudioRecord(MediaRecorder.AudioSource.MIC, 
                16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize)

            val pcmFile = File(context.cacheDir, "temp.pcm")
            audioRecord.startRecording()
            
            pcmFile.outputStream().use { fos ->
                val data = ByteArray(bufferSize)
                while (isRecording) {
                    val read = audioRecord.read(data, 0, bufferSize)
                    if (read > 0) fos.write(data, 0, read)
                }
            }
            audioRecord.stop()
            audioRecord.release()
            
            // 录音停了，开始转码上传
            processAndUpload(pcmFile)
        }
    }

    fun stopRecording() {
        isRecording = false
    }

    private suspend fun processAndUpload(pcmFile: File) = withContext(Dispatchers.IO) {
        val wavFile = File(context.cacheDir, "record.wav")
        PcmToWavUtil.encodePcmToWav(pcmFile, wavFile)
        
        // 这里写你的 OkHttp 上传逻辑，发给你之前写的 Spring Boot 接口
        // uploadToYourServer(wavFile)
    }
}