package com.signlink.ui.ai

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.signlink.data.repository.IAAnalysisRepository
import com.signlink.util.GeminiManager
import com.signlink.util.TTSManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class AiExplanationViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val geminiManager: GeminiManager,
    private val iaRepository: IAAnalysisRepository,
    private val ttsManager: TTSManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<AiUiState>(AiUiState.Idle)
    val uiState: StateFlow<AiUiState> = _uiState.asStateFlow()

    fun summarizeText(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            _uiState.value = AiUiState.Loading
            iaRepository.resumirTexto(text).onSuccess { response ->
                val prefs = context.getSharedPreferences("signlink_prefs", Context.MODE_PRIVATE)
                val mode = prefs.getString("summary_mode", "sencillo") ?: "sencillo"

                val finalSummary = if (mode == "estandar") {
                    response.resumen
                } else {
                    geminiManager.simplifyMessage(response.resumen, mode) ?: response.resumen
                }

                _uiState.value = AiUiState.Success(finalSummary)
                ttsManager.speak(finalSummary)
            }.onFailure {
                _uiState.value = AiUiState.Error("Error al resumir el mensaje.")
            }
        }
    }

    fun summarizeFile(file: File) {
        viewModelScope.launch {
            _uiState.value = AiUiState.Loading
            iaRepository.resumirArchivo(file).onSuccess { response ->
                val prefs = context.getSharedPreferences("signlink_prefs", Context.MODE_PRIVATE)
                val mode = prefs.getString("summary_mode", "sencillo") ?: "sencillo"

                val finalSummary = if (mode == "estandar") {
                    response.resumen
                } else {
                    geminiManager.simplifyMessage(response.resumen, mode) ?: response.resumen
                }

                _uiState.value = AiUiState.Success(finalSummary)
                ttsManager.speak(finalSummary)
            }.onFailure {
                _uiState.value = AiUiState.Error("Error al resumir el archivo.")
            }
        }
    }


    fun simplifyText(text: String) {
        if (text.isBlank()) return

        viewModelScope.launch {
            _uiState.value = AiUiState.Loading
            val result = geminiManager.simplifyMessage(text)
            if (result != null) {
                _uiState.value = AiUiState.Success(result)
            } else {
                _uiState.value = AiUiState.Error("No se pudo simplificar el texto. Intenta de nuevo.")
            }
        }
    }

    fun correctText(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            _uiState.value = AiUiState.Loading
            val result = geminiManager.correctMessage(text)
            if (result != null) {
                _uiState.value = AiUiState.Success(result)
            } else {
                _uiState.value = AiUiState.Error("No se pudo corregir el texto.")
            }
        }
    }

    fun defineWord(word: String) {
        if (word.isBlank()) return
        viewModelScope.launch {
            _uiState.value = AiUiState.Loading
            val result = geminiManager.defineWord(word)
            if (result != null) {
                _uiState.value = AiUiState.Success(result)
            } else {
                _uiState.value = AiUiState.Error("No se pudo encontrar la definición.")
            }
        }
    }

    fun speakText(text: String) {
        ttsManager.speak(text)
    }

    fun stopTts() {
        ttsManager.stop()
    }

    sealed class AiUiState {
        object Idle : AiUiState()
        object Loading : AiUiState()
        data class Success(val result: String) : AiUiState()
        data class Error(val message: String) : AiUiState()
    }
}
