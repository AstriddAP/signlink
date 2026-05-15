package com.signlink.data.repository

import android.content.Context
import com.google.api.gax.core.FixedCredentialsProvider
import com.google.api.gax.rpc.ApiStreamObserver
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.speech.v1.*
import com.google.protobuf.ByteString
import com.signlink.R
import com.signlink.data.audio.AudioRecorder
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpeechRecognitionRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val audioRecorder: AudioRecorder
) {

    data class TranscriptionResult(
        val transcript: String,
        val speakerTag: Int = 0,
        val isFinal: Boolean = false
    )

    private var speechClient: SpeechClient? = null

    private fun getSpeechClient(): SpeechClient {
        if (speechClient == null) {
            val credentials = GoogleCredentials.fromStream(
                context.resources.openRawResource(R.raw.google_credentials)
            )
            val settings = SpeechSettings.newBuilder()
                .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                .build()
            speechClient = SpeechClient.create(settings)
        }
        return speechClient!!
    }

    fun startStreamingRecognition(): Flow<TranscriptionResult> = callbackFlow {
        val client = getSpeechClient()
        val responseObserver = object : ApiStreamObserver<StreamingRecognizeResponse> {
            override fun onNext(response: StreamingRecognizeResponse?) {
                response?.resultsList?.forEach { result ->
                    val alternative = result.alternativesList.firstOrNull()
                    val transcript = alternative?.transcript
                    
                    @Suppress("DEPRECATION")
                    val speakerTag = alternative?.wordsList?.firstOrNull()?.speakerTag ?: 0
                    
                    if (transcript != null) {
                        trySend(TranscriptionResult(transcript, speakerTag, result.isFinal))
                    }
                }
            }

            override fun onError(t: Throwable?) {
                close(t)
            }

            override fun onCompleted() {
                close()
            }
        }

        val callable = client.streamingRecognizeCallable()
        
        @Suppress("DEPRECATION")
        val requestObserver = callable.bidiStreamingCall(responseObserver)

        val config = RecognitionConfig.newBuilder()
            .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
            .setSampleRateHertz(16000)
            .setLanguageCode("es-ES")
            .setDiarizationConfig(SpeakerDiarizationConfig.newBuilder()
                .setEnableSpeakerDiarization(true)
                .setMinSpeakerCount(1)
                .setMaxSpeakerCount(5)
                .build())
            .build()

        val streamingConfig = StreamingRecognitionConfig.newBuilder()
            .setConfig(config)
            .setInterimResults(true)
            .build()

        requestObserver.onNext(
            StreamingRecognizeRequest.newBuilder()
                .setStreamingConfig(streamingConfig)
                .build()
        )

        val audioJob = launch(Dispatchers.IO) {
            try {
                audioRecorder.startRecording().collect { audioData ->
                    if (isActive) {
                        val request = StreamingRecognizeRequest.newBuilder()
                            .setAudioContent(ByteString.copyFrom(audioData))
                            .build()
                        requestObserver.onNext(request)
                    }
                }
            } catch (e: Exception) {
                if (isActive) requestObserver.onError(e)
            }
        }

        awaitClose {
            audioJob.cancel()
            audioRecorder.stopRecording()
            requestObserver.onCompleted()
        }
    }.flowOn(Dispatchers.IO)

    fun transcribeAudioFile(audioBytes: ByteArray): Flow<TranscriptionResult> = callbackFlow {
        val client = getSpeechClient()
        
        val config = RecognitionConfig.newBuilder()
            .setEncoding(RecognitionConfig.AudioEncoding.ENCODING_UNSPECIFIED)
            .setLanguageCode("es-ES")
            .setEnableAutomaticPunctuation(true)
            .build()
            
        val audio = RecognitionAudio.newBuilder()
            .setContent(ByteString.copyFrom(audioBytes))
            .build()
            
        try {
            val response = withContext(Dispatchers.IO) {
                client.recognize(config, audio)
            }
            
            response.resultsList.forEach { result ->
                val alternative = result.alternativesList.firstOrNull()
                val transcript = alternative?.transcript
                if (transcript != null) {
                    trySend(TranscriptionResult(transcript, isFinal = true))
                }
            }
            close()
        } catch (e: Exception) {
            close(e)
        }
        
        awaitClose { }
    }.flowOn(Dispatchers.IO)
}
