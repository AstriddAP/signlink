package com.signlink.ui.captioning

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.signlink.data.repository.SpeechRecognitionRepository
import com.signlink.util.GeminiManager
import com.signlink.util.TTSManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LiveCaptioningViewModel @Inject constructor(
    private val speechRepository: SpeechRecognitionRepository,
    private val geminiManager: GeminiManager,
    private val ttsManager: TTSManager
) : ViewModel() {

    private val _captions = MutableStateFlow<List<Caption>>(emptyList())
    val captions = _captions.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording = _isRecording.asStateFlow()

    private var recordingJob: kotlinx.coroutines.Job? = null

    fun toggleRecording() {
        if (_isRecording.value) {
            stopRecording()
        } else {
            startRecording()
        }
    }

    fun startRecording() {
        if (_isRecording.value) return
        _isRecording.value = true
        recordingJob?.cancel()
        recordingJob = viewModelScope.launch {
            while (_isRecording.value) {
                try {
                    speechRepository.startStreamingRecognition()
                        .catch { error ->
                            Log.e("LiveCaptioning", "Recognition error: ${error.message}")
                            if (error.message?.contains("micrófono") == true || error.message?.contains("Permiso") == true) {
                                _isRecording.value = false
                                val errorCaption = Caption(
                                    text = error.message ?: "Error al acceder al micrófono",
                                    speaker = "Sistema",
                                    color = "#FF5252",
                                    isProcessing = false
                                )
                                _captions.value = _captions.value.takeLast(29) + errorCaption
                            }
                            delay(500)
                        }
                        .collect { result ->
                            val transcriptText = result.transcript
                            if (transcriptText.isBlank()) return@collect

                            val currentList = _captions.value.toMutableList()

                            if (!result.isFinal) {
                                // Mientras se habla, mostramos una burbuja provisional marcada como "Escuchando..."
                                val tempCaption = Caption(
                                    text = transcriptText,
                                    speaker = "Escuchando...",
                                    color = "#FFEB3B", // Amarillo
                                    isProcessing = true
                                )
                                if (currentList.isNotEmpty() && currentList.last().isProcessing) {
                                    currentList[currentList.size - 1] = tempCaption
                                } else {
                                    currentList.add(tempCaption)
                                }
                                _captions.value = currentList.takeLast(30)
                            } else {
                                // En cuanto se termina la frase, quitamos la provisional
                                if (currentList.isNotEmpty() && currentList.last().isProcessing) {
                                    currentList.removeAt(currentList.size - 1)
                                }

                                // Creamos una burbuja temporal "Procesando..." para indicar que la IA está diarizando
                                val finalCaptionProvisional = Caption(
                                    text = transcriptText,
                                    speaker = "Procesando...",
                                    color = "#CCCCCC", // Gris
                                    isProcessing = false
                                )
                                currentList.add(finalCaptionProvisional)
                                _captions.value = currentList.takeLast(30)

                                // Ejecutamos la diarización contextual con Gemini asíncronamente
                                viewModelScope.launch {
                                    val historyText = _captions.value
                                        .filter { it.speaker != "Procesando..." && it.speaker != "Escuchando..." }
                                        .takeLast(10)
                                        .joinToString("\n") { "${it.speaker}: ${it.text}" }

                                    val diarized = geminiManager.diarizeSpeechSegment(historyText, transcriptText)
                                    val updatedList = _captions.value.toMutableList()
                                    val index = updatedList.indexOf(finalCaptionProvisional)
                                    
                                    if (index != -1) {
                                        if (diarized != null) {
                                            val colors = listOf(
                                                "#FF5252", // Rojo/Coral
                                                "#448AFF", // Azul
                                                "#4CAF50", // Verde
                                                "#9C27B0", // Morado
                                                "#FF9800", // Naranja
                                                "#E91E63", // Rosa
                                                "#00BCD4"  // Celeste
                                            )
                                            val colorIndex = java.lang.Math.abs(diarized.speaker.hashCode()) % colors.size
                                            val speakerColor = colors[colorIndex]
                                            updatedList[index] = Caption(
                                                text = diarized.text,
                                                speaker = diarized.speaker,
                                                color = speakerColor,
                                                isProcessing = false
                                            )
                                        } else {
                                            // Fallback: usar Hablante 1 si hay un fallo
                                            updatedList[index] = Caption(
                                                text = transcriptText,
                                                speaker = "Hablante 1",
                                                color = "#FF5252",
                                                isProcessing = false
                                            )
                                        }
                                        _captions.value = updatedList
                                    }
                                }
                            }
                        }
                } catch (e: Exception) {
                    Log.e("LiveCaptioning", "Loop collection exception: ${e.message}")
                    delay(500)
                }
                delay(300) // Pequeño respiro entre reinicios automáticos
            }
        }
    }

    fun stopRecording() {
        _isRecording.value = false
        recordingJob?.cancel()
        recordingJob = null
    }

    fun stopTts() {
        ttsManager.stop()
    }

    data class Caption(
        val text: String,
        val speaker: String,
        val color: String,
        val isProcessing: Boolean = false
    )
}
