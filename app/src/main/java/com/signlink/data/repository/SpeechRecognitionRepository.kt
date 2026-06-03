package com.signlink.data.repository

import kotlinx.coroutines.flow.Flow

interface SpeechRecognitionRepository {
    data class TranscriptionResult(
        val transcript: String,
        val speakerTag: Int = 0,
        val isFinal: Boolean = false
    )

    fun startStreamingRecognition(): Flow<TranscriptionResult>
    fun transcribeAudioFile(audioBytes: ByteArray): Flow<TranscriptionResult>
}
