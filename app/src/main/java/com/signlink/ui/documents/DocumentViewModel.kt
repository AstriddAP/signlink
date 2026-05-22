package com.signlink.ui.documents

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.signlink.data.local.entity.DocumentEntity
import com.signlink.data.repository.DocumentRepository
import com.signlink.util.GeminiManager
import com.signlink.util.TTSManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DocumentViewModel @Inject constructor(
    private val repository: DocumentRepository,
    private val geminiManager: GeminiManager,
    private val ttsManager: TTSManager
) : ViewModel() {

    private val _documents = MutableStateFlow<List<DocumentEntity>>(emptyList())
    val documents: StateFlow<List<DocumentEntity>> = _documents.asStateFlow()

    private val _selectedDocument = MutableStateFlow<DocumentEntity?>(null)
    val selectedDocument: StateFlow<DocumentEntity?> = _selectedDocument.asStateFlow()

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private val _analysisResult = MutableStateFlow<String?>(null)
    val analysisResult: StateFlow<String?> = _analysisResult.asStateFlow()

    init {
        loadDocuments()
    }

    private fun loadDocuments() {
        viewModelScope.launch {
            repository.getAllDocuments().collect {
                _documents.value = it
            }
        }
    }

    fun getDocumentById(id: Int) {
        viewModelScope.launch {
            _selectedDocument.value = repository.getDocumentById(id)
        }
    }

    fun addDocument(document: DocumentEntity) {
        viewModelScope.launch {
            repository.insertDocument(document)
        }
    }

    fun deleteDocument(document: DocumentEntity) {
        viewModelScope.launch {
            repository.deleteDocument(document)
        }
    }

    fun analyzeAndSpeakDocument(bitmap: Bitmap) {
        viewModelScope.launch {
            _isAnalyzing.value = true
            _analysisResult.value = "La IA está analizando tu documento..."
            ttsManager.speak("Analizando documento, por favor espera un momento.")

            val result = geminiManager.analyzeDocument(bitmap)
            _analysisResult.value = result
            _isAnalyzing.value = false

            result?.let {
                ttsManager.speak(it)
            }
        }
    }

    fun speakCurrentAnalysis() {
        _analysisResult.value?.let { text ->
            if (text != "La IA está analizando tu documento...") {
                ttsManager.speak(text)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        ttsManager.stop()
    }
}
