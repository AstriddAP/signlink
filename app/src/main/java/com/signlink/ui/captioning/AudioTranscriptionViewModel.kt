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

    fun transcribeAudioUri(uri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes() ?: return@launch
                
                repository.transcribeAudioFile(bytes)
                    .catch { e -> _transcription.value = "Error: ${e.message}" }
                    .collect { result ->
                        _transcription.value += result.transcript + " "
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
            val simplified = geminiManager.simplifyMessage(currentText)
            if (simplified != null) {
                _transcription.value = simplified
            }
            _isLoading.value = false
        }
    }

    fun startLiveTranscription() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.startStreamingRecognition()
                .catch { e -> _transcription.value = "Error: ${e.message}" }
                .collect { result ->
                    if (result.isFinal) {
                        _transcription.value += result.transcript + " "
                    }
                }
            _isLoading.value = false
        }
    }
}
