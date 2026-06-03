package com.signlink.data.repository

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.signlink.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpeechRecognitionRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SpeechRecognitionRepository {

    override fun startStreamingRecognition(): Flow<SpeechRecognitionRepository.TranscriptionResult> = callbackFlow {
        // El SpeechRecognizer nativo de Android debe ser creado y utilizado en el hilo principal (Main thread).
        val speechRecognizer = withContext(Dispatchers.Main) {
            SpeechRecognizer.createSpeechRecognizer(context)
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }

        var lastPartialText = ""

        val listener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            
            override fun onError(error: Int) {
                val exception = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH,
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                        // Si se detectó algo antes del silencio, lo enviamos como final para no perderlo
                        if (lastPartialText.isNotEmpty()) {
                            trySend(SpeechRecognitionRepository.TranscriptionResult(lastPartialText, isFinal = true))
                        }
                        null
                    }
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                        Exception("Permiso de micrófono denegado. Por favor, actívalo en los ajustes de tu dispositivo.")
                    }
                    SpeechRecognizer.ERROR_NETWORK,
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> {
                        Exception("Error de red. Asegúrate de tener conexión a internet.")
                    }
                    SpeechRecognizer.ERROR_AUDIO -> {
                        Exception("Error al acceder al micrófono. Cierra otras aplicaciones que lo estén usando.")
                    }
                    else -> null // Para otros códigos cerramos normalmente sin arrojar un error técnico al usuario
                }

                if (exception != null) {
                    close(exception)
                } else {
                    close()
                }
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull() ?: ""
                if (text.isNotEmpty()) {
                    trySend(SpeechRecognitionRepository.TranscriptionResult(text, isFinal = true))
                }
                close()
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull() ?: ""
                if (text.isNotEmpty()) {
                    lastPartialText = text
                    trySend(SpeechRecognitionRepository.TranscriptionResult(text, isFinal = false))
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }

        withContext(Dispatchers.Main) {
            speechRecognizer.setRecognitionListener(listener)
            speechRecognizer.startListening(intent)
        }

        awaitClose {
            // Debe detenerse y destruirse en el hilo principal
            launch(Dispatchers.Main) {
                speechRecognizer.stopListening()
                speechRecognizer.destroy()
            }
        }
    }.flowOn(Dispatchers.Main)

    override fun transcribeAudioFile(audioBytes: ByteArray): Flow<SpeechRecognitionRepository.TranscriptionResult> = flow {
        try {
            val model = GenerativeModel(
                modelName = "gemini-2.5-flash",
                apiKey = BuildConfig.GEMINI_API_KEY
            )
            // Solicitamos a Gemini 1.5 Flash transcribir el archivo de audio
            val response = model.generateContent(
                content {
                    // Usamos un tipo MIME de audio común; Gemini identifica automáticamente el formato interno (WAV, MP3, OPUS, etc.)
                    blob("audio/ogg", audioBytes)
                    text("Transcribe este audio palabra por palabra en español. Devuelve únicamente la transcripción exacta sin añadir ningún comentario, introducción o formato extra.")
                }
            )
            val text = response.text ?: ""
            emit(SpeechRecognitionRepository.TranscriptionResult(text.trim(), isFinal = true))
        } catch (e: Exception) {
            throw e
        }
    }.flowOn(Dispatchers.IO)
}
