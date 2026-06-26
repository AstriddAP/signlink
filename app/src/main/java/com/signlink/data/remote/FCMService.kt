package com.signlink.data.remote

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.signlink.R
import com.signlink.data.model.User
import com.signlink.data.repository.UserRepository
import com.signlink.ui.contacts.ChatFragment
import com.signlink.ui.main.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class FCMService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO)

    @Inject
    lateinit var userRepository: UserRepository

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d("FCMService", "Mensaje FCM recibido. Data payload: ${remoteMessage.data}")

        // 1. Procesar datos (data-only messages)
        val data = remoteMessage.data
        if (data.isNotEmpty()) {
            val title = data["title"] ?: "SignLink Alerta"
            val body = data["body"] ?: ""
            val type = data["type"]
            val notificationId = data["notificationId"]

            val currentUser = FirebaseAuth.getInstance().currentUser
            val currentUid = currentUser?.uid

            // Si es un mensaje de chat
            if (type == "CHAT_MESSAGE") {
                val senderId = data["senderId"] ?: ""
                val senderName = data["senderName"] ?: "Contacto"
                
                // Calcular chatId ordenado alfabéticamente
                val chatId = if (currentUid != null && senderId.isNotEmpty()) {
                    if (currentUid < senderId) "${currentUid}_${senderId}" else "${senderId}_${currentUid}"
                } else {
                    ""
                }

                // Si el usuario está viendo actualmente este chat, NO mostrar la notificación en pantalla
                if (chatId.isNotEmpty() && chatId == ChatFragment.activeChatId) {
                    Log.d("FCMService", "Usuario en chat activo ($chatId). Omitiendo banner de notificación.")
                } else {
                    sendNotification(title, body, "chat", senderId, senderName)
                }
            } 
            // Si es la solicitud de contacto recíproco
            else if (type == "MUTUAL_CONTACT_ADD") {
                val contactUid = data["uid"] ?: ""
                val contactName = data["displayName"] ?: ""
                val contactEmail = data["email"] ?: ""

                if (currentUid != null && contactUid.isNotEmpty()) {
                    val newContact = User(
                        uid = contactUid,
                        displayName = contactName,
                        email = contactEmail
                    )
                    serviceScope.launch {
                        try {
                            userRepository.addContact(currentUid, newContact)
                            Log.d("FCMService", "Contacto recíproco agregado exitosamente en segundo plano: $contactName")
                        } catch (e: Exception) {
                            Log.e("FCMService", "Error agregando contacto recíproco en segundo plano", e)
                        }
                    }
                }
                sendNotification(title, body, "chat", contactUid, contactName)
            } else {
                // Caso por defecto
                sendNotification(title, body)
            }

            // 2. Limpieza en Firestore para evitar notificaciones duplicadas en MainActivity
            if (currentUid != null && !notificationId.isNullOrEmpty()) {
                FirebaseFirestore.getInstance().collection("users").document(currentUid)
                    .collection("notifications").document(notificationId)
                    .delete()
                    .addOnSuccessListener {
                        Log.d("FCMService", "Documento de notificación temporal $notificationId eliminado de Firestore")
                    }
                    .addOnFailureListener { e ->
                        Log.e("FCMService", "Error eliminando documento de notificación temporal $notificationId de Firestore", e)
                    }
            }
        }

        // Si por alguna razón envían un formato de notificación estándar
        remoteMessage.notification?.let {
            sendNotification(it.title ?: "SignLink Alerta", it.body ?: "")
        }
    }

    override fun onNewToken(token: String) {
        Log.d("FCMService", "Nuevo token de FCM generado: $token")
        // Cuando se genera un nuevo token, si hay usuario autenticado, guardarlo en Firestore
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUid != null) {
            serviceScope.launch {
                try {
                    userRepository.updateFcmToken(currentUid, token)
                    Log.d("FCMService", "Token FCM actualizado en Firestore con éxito")
                } catch (e: Exception) {
                    Log.e("FCMService", "Error guardando token de FCM en Firestore", e)
                }
            }
        }
    }

    private fun sendNotification(
        title: String, 
        messageBody: String, 
        navigateTo: String? = null, 
        contactUid: String? = null, 
        contactName: String? = null
    ) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            if (navigateTo != null) {
                putExtra("navigate_to", navigateTo)
                putExtra("contact_uid", contactUid)
                putExtra("contact_name", contactName)
            }
        }
        val pendingIntent = PendingIntent.getActivity(
            this, System.currentTimeMillis().toInt(), intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val channelId = "signlink_notifications"
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Alertas de SignLink",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }
}
