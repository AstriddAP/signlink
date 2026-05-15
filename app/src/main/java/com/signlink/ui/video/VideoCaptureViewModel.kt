package com.signlink.ui.video

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.signlink.data.repository.SpeechRecognitionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VideoCaptureViewModel @Inject constructor(
    private val speechRepository: SpeechRecognitionRepository
) : ViewModel() {

    private val _realtimeCaptions = MutableStateFlow("")
    val realtimeCaptions = _realtimeCaptions.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording = _isRecording.asStateFlow()

    fun startCaptions() {
        viewModelScope.launch {
            speechRepository.startStreamingRecognition().collect { result ->
                _realtimeCaptions.value = result.transcript
            }
        }
    }

    fun setIsRecording(recording: Boolean) {
        _isRecording.value = recording
    }
}
