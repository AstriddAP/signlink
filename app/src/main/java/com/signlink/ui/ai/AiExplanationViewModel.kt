package com.signlink.ui.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.signlink.util.GeminiManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AiExplanationViewModel @Inject constructor(
    private val geminiManager: GeminiManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<AiUiState>(AiUiState.Idle)
    val uiState: StateFlow<AiUiState> = _uiState.asStateFlow()

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
