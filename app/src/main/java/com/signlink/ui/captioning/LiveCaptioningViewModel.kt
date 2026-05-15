package com.signlink.ui.captioning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.signlink.data.repository.SpeechRecognitionRepository
import com.signlink.util.GeminiManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LiveCaptioningViewModel @Inject constructor(
    private val speechRepository: SpeechRecognitionRepository,
    private val geminiManager: GeminiManager
) : ViewModel() {

    private val _captions = MutableStateFlow<List<Caption>>(emptyList())
    val captions = _captions.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording = _isRecording.asStateFlow()

    fun toggleRecording() {
        if (_isRecording.value) {
            stopRecording()
        } else {
            startRecording()
        }
    }

    fun onVisualDetection(text: String) {
        val visualCaption = Caption(
            text = text,
            speaker = "LENS",
            color = "#00E5FF"
        )
        if (_captions.value.lastOrNull()?.text != text) {
            _captions.value = _captions.value.takeLast(20) + visualCaption
        }
    }

    private fun startRecording() {
        _isRecording.value = true
        viewModelScope.launch {
            speechRepository.startStreamingRecognition().collect { result ->
                val speakerColor = when (result.speakerTag) {
                    1 -> "#FF5252" // Rojo
                    2 -> "#448AFF" // Azul
                    3 -> "#4CAF50" // Verde
                    else -> "#FFEB3B" // Amarillo
                }

                val transcriptText = result.transcript

                val newCaption = Caption(
                    text = transcriptText,
                    speaker = if (result.speakerTag > 0) "Hablante ${result.speakerTag}" else "Voz detectada",
                    color = speakerColor,
                    isProcessing = !result.isFinal
                )

                val currentList = _captions.value.toMutableList()
                
                // Si el resultado es final, aplicamos el reconocimiento inteligente de números (Req. 2)
                if (result.isFinal) {
                    val finalCaption = newCaption.copy(isProcessing = false)
                    currentList.add(finalCaption)
                    _captions.value = currentList.takeLast(30)

                    viewModelScope.launch {
                        val formattedText = geminiManager.formatNumbersInText(transcriptText)
                        if (formattedText != null) {
                            // Reemplazamos el último mensaje por el formateado por IA
                            val updatedList = _captions.value.toMutableList()
                            val index = updatedList.indexOf(finalCaption)
                            if (index != -1) {
                                updatedList[index] = finalCaption.copy(text = formattedText)
                                _captions.value = updatedList
                            }
                        }
                    }
                    return@collect
                }

                if (currentList.isNotEmpty() && !result.isFinal && currentList.last().speaker == newCaption.speaker) {
                    currentList[currentList.size - 1] = newCaption
                } else {
                    currentList.add(newCaption)
                }
                
                _captions.value = currentList.takeLast(30)
            }
        }
    }

    private fun updateFinalCaption(updatedCaption: Caption) {
        val currentList = _captions.value.toMutableList()
        val index = currentList.indexOfLast { it.speaker == updatedCaption.speaker && it.text.contains(updatedCaption.text.take(5)) }
        if (index != -1) {
            currentList[index] = updatedCaption
            _captions.value = currentList
        }
    }

    private fun stopRecording() {
        _isRecording.value = false
    }

    data class Caption(
        val text: String,
        val speaker: String,
        val color: String,
        val isProcessing: Boolean = false
    )
}
