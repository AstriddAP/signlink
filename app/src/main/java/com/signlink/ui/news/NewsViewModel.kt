package com.signlink.ui.news

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.signlink.util.GeminiManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NewsViewModel @Inject constructor(
    private val geminiManager: GeminiManager
) : ViewModel() {

    private val _newsList = MutableStateFlow<List<NewsItem>>(emptyList())
    val newsList: StateFlow<List<NewsItem>> = _newsList

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        loadMockNews()
    }

    private fun loadMockNews() {
        _newsList.value = listOf(
            NewsItem("1", "Nueva ley de accesibilidad", "El gobierno ha aprobado una nueva ley que obliga a todas las instituciones públicas a tener intérpretes de lengua de señas en tiempo real y subtitulación en todos sus videos informativos."),
            NewsItem("2", "Avance tecnológico en audífonos", "Investigadores han desarrollado una nueva tecnología de procesamiento de audio que permite a los audífonos filtrar el ruido ambiental de manera mucho más efectiva usando redes neuronales."),
            NewsItem("3", "Deportes: Triunfo nacional", "La selección nacional de deportes adaptados ha logrado obtener 5 medallas de oro en el último campeonato regional, destacando el esfuerzo y la dedicación de los atletas.")
        )
    }

    fun getExplanation(newsId: String) {
        val currentList = _newsList.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == newsId }
        
        if (index != -1 && currentList[index].explainedContent == null) {
            viewModelScope.launch {
                _isLoading.value = true
                val explanation = geminiManager.explainNews(currentList[index].rawContent)
                if (explanation != null) {
                    currentList[index] = currentList[index].copy(explainedContent = explanation)
                    _newsList.value = currentList
                }
                _isLoading.value = false
            }
        }
    }
}
