package com.signlink.ui.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.signlink.data.repository.IAAnalysisRepository
import com.signlink.util.GeminiManager
import com.signlink.util.TTSManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class AiExplanationViewModel @Inject constructor(
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
                _uiState.value = AiUiState.Success(response.resumen)
                ttsManager.speak(response.resumen)
            }.onFailure {
                _uiState.value = AiUiState.Error("Error al resumir el mensaje.")
            }
        }
    }

    fun summarizeFile(file: File) {
        viewModelScope.launch {
            _uiState.value = AiUiState.Loading
            iaRepository.resumirArchivo(file).onSuccess { response ->
                _uiState.value = AiUiState.Success(response.resumen)
                ttsManager.speak(response.resumen)
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

    sealed class AiUiState {
        object Idle : AiUiState()
        object Loading : AiUiState()
        data class Success(val result: String) : AiUiState()
        data class Error(val message: String) : AiUiState()
    }
}
