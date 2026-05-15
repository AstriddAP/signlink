package com.signlink.ui.home

import android.annotation.SuppressLint
import android.app.Application
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.signlink.R
import com.signlink.data.repository.AlertRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val application: Application,
    private val alertRepository: AlertRepository
) : ViewModel() {

    private val _panicState = MutableStateFlow<PanicState>(PanicState.Idle)
    val panicState = _panicState.asStateFlow()

    @SuppressLint("MissingPermission")
    fun triggerPanicButton() {
        _panicState.value = PanicState.Loading

        viewModelScope.launch {
            // Simulamos la obtención de ubicación y envío a Firestore
            // para evitar crashes sin google-services.json ni API Keys
            val result = alertRepository.createPanicAlert("mock_user", 0.0, 0.0)
            
            if (result.isSuccess) {
                delay(1000) // Efecto visual de carga
                showLocalNotification()
                _panicState.value = PanicState.Success
            } else {
                _panicState.value = PanicState.Error("Error en la conexión")
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun showLocalNotification() {
        val channelId = "signlink_notifications"
        val notification = NotificationCompat.Builder(application, channelId)
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle("¡Alerta de Pánico!")
            .setContentText("Tu ubicación ha sido enviada a tus contactos de confianza.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(application).notify(1, notification)
        } catch (e: Exception) {
            // Ignorar errores de notificación en emuladores sin permisos
        }
    }

    sealed class PanicState {
        object Idle : PanicState()
        object Loading : PanicState()
        object Success : PanicState()
        data class Error(val message: String) : PanicState()
    }
}
