package com.signlink.ui.captioning

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.signlink.data.repository.SpeechRecognitionRepository
import com.signlink.util.GeminiManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AudioTranscriptionViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: SpeechRecognitionRepository,
    private val geminiManager: GeminiManager
) : ViewModel() {

    private val _transcription = MutableStateFlow("")
    val transcription: StateFlow<String> = _transcription

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _translationTime = MutableStateFlow<String?>(null)
    val translationTime: StateFlow<String?> = _translationTime

    fun transcribeAudioUri(uri: Uri) {
        viewModelScope.launch {
            _transcription.value = ""
            _isLoading.value = true
            _translationTime.value = null
            val startTime = System.currentTimeMillis()
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes() ?: return@launch
                
                repository.transcribeAudioFile(bytes)
                    .catch { e -> _transcription.value = "Error: ${e.message}" }
                    .collect { result ->
                        _transcription.value = result.transcript
                        val duration = (System.currentTimeMillis() - startTime) / 1000.0
                        _translationTime.value = String.format(java.util.Locale.US, "Tiempo de traducción: %.2fs", duration)
                    }
            } catch (e: Exception) {
                _transcription.value = "Error al leer el archivo: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun simplifyWithIA() {
        val currentText = _transcription.value
        if (currentText.isBlank()) return

        viewModelScope.launch {
            _isLoading.value = true
            _translationTime.value = null
            val startTime = System.currentTimeMillis()
            val simplified = geminiManager.simplifyMessage(currentText)
            if (simplified != null) {
                _transcription.value = simplified
                val duration = (System.currentTimeMillis() - startTime) / 1000.0
                _translationTime.value = String.format(java.util.Locale.US, "Tiempo de simplificación: %.2fs", duration)
            }
            _isLoading.value = false
        }
    }

    fun startLiveTranscription() {
        viewModelScope.launch {
            if (_transcription.value.startsWith("Error")) {
                _transcription.value = ""
            }
            _isLoading.value = true
            _translationTime.value = null
            repository.startStreamingRecognition()
                .catch { e -> _transcription.value = "Error: ${e.message}" }
                .collect { result ->
                    if (result.isFinal) {
                        if (_transcription.value.startsWith("Error")) {
                            _transcription.value = ""
                        }
                        val startTime = System.currentTimeMillis()
                        // Procesamos el segmento de voz finalizado con Gemini para puntuarlo en segundo plano
                        val punctuatedText = geminiManager.correctMessage(result.transcript)
                        val textToAppend = punctuatedText ?: result.transcript
                        _transcription.value += textToAppend.trim() + " "
                        
                        val duration = (System.currentTimeMillis() - startTime) / 1000.0
                        _translationTime.value = String.format(java.util.Locale.US, "Tiempo de procesamiento: %.2fs", duration)
                    }
                }
            _isLoading.value = false
        }
    }
}
