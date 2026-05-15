package com.signlink.data.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class AudioRecorder @Inject constructor() {

    private var audioRecord: AudioRecord? = null
    private var isRecording = false

    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

    @SuppressLint("MissingPermission")
    fun startRecording(): Flow<ByteArray> = flow {
        try {
            if (minBufferSize == AudioRecord.ERROR || minBufferSize == AudioRecord.ERROR_BAD_VALUE) {
                Log.e("AudioRecorder", "Invalid buffer size")
                return@flow
            }

            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                minBufferSize.coerceAtLeast(sampleRate * 2)
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e("AudioRecorder", "AudioRecord initialization failed")
                return@flow
            }

            val bufferSize = minBufferSize.coerceAtLeast(sampleRate * 2)
            val buffer = ByteArray(bufferSize)
            
            try {
                audioRecord?.startRecording()
            } catch (e: IllegalStateException) {
                Log.e("AudioRecorder", "startRecording failed: ${e.message}")
                return@flow
            }
            
            isRecording = true

            while (isRecording) {
                val record = audioRecord // local reference for safety
                if (record == null || record.state != AudioRecord.STATE_INITIALIZED) break
                
                val read = record.read(buffer, 0, buffer.size)
                if (read > 0) {
                    emit(buffer.copyOf(read))
                } else if (read < 0) {
                    Log.e("AudioRecorder", "Error reading audio: $read")
                    break
                }
                delay(50)
            }
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Recording exception: ${e.message}")
        } finally {
            stopRecording()
        }
    }.flowOn(Dispatchers.IO)

    fun stopRecording() {
        isRecording = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }
}
