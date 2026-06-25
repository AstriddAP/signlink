package com.signlink.data.remote

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.signlink.R
import com.signlink.ui.main.MainActivity

class NotificationService : Service() {

    private var notificationListener: ListenerRegistration? = null
    private val channelId = "signlink_bg_notifications"
    private val serviceChannelId = "signlink_service_channel"

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            startForegroundServiceNotification()
            startFirestoreListener(currentUser.uid)
        } else {
            stopSelf()
        }
        return START_STICKY
    }

    private fun startForegroundServiceNotification() {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, serviceChannelId)
            .setContentTitle("SignLink Activo")
            .setContentText("Escuchando notificaciones en tiempo real...")
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1001, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(1001, notification)
        }
    }

    private fun startFirestoreListener(uid: String) {
        if (notificationListener != null) return

        val db = FirebaseFirestore.getInstance()
        notificationListener = db.collection("users").document(uid)
            .collection("notifications")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("NotificationService", "Error al escuchar notificaciones", error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    for (change in snapshot.documentChanges) {
                        if (change.type == DocumentChange.Type.ADDED) {
                            val doc = change.document
                            
                            // Si es un agregar contacto recíproco, procesarlo
                            val type = doc.getString("type")
                            if (type == "MUTUAL_CONTACT_ADD") {
                                val contactUid = doc.getString("uid") ?: ""
                                val contactName = doc.getString("displayName") ?: ""
                                val contactEmail = doc.getString("email") ?: ""
                                if (contactUid.isNotEmpty()) {
                                    val newContact = com.signlink.data.model.User(
                                        uid = contactUid,
                                        displayName = contactName,
                                        email = contactEmail
                                    )
                                    // Escribir el contacto recíproco directamente a través de FirebaseFirestore
                                    db.collection("users").document(uid)
                                        .collection("contacts").document(contactUid)
                                        .set(newContact)
                                }
                            }
                            
                            val title = doc.getString("title") ?: "Nuevo mensaje"
                            val body = doc.getString("body") ?: ""
                            
                            triggerLocalNotification(title, body)
                            
                            // Borramos de inmediato para no repetir
                            db.collection("users").document(uid)
                                .collection("notifications")
                                .document(doc.id)
                                .delete()
                        }
                    }
                }
            }
    }

    private fun triggerLocalNotification(title: String, messageBody: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, System.currentTimeMillis().toInt(), intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            val serviceChannel = NotificationChannel(
                serviceChannelId,
                "Canal de Servicio de SignLink",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(serviceChannel)

            val alertChannel = NotificationChannel(
                channelId,
                "Alertas de SignLink",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(alertChannel)
        }
    }

    override fun onDestroy() {
        notificationListener?.remove()
        notificationListener = null
        super.onDestroy()
    }
}
