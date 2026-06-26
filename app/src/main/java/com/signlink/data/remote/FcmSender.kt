package com.signlink.data.remote

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.signlink.BuildConfig
import kotlinx.coroutines.tasks.await
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object FcmSender {
    private const val TAG = "FcmSender"
    private const val BASE_URL = "https://fcm.googleapis.com/"

    private val fcmApi: FcmApi by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FcmApi::class.java)
    }

    /**
     * Obtiene la clave de FCM Server. Intenta primero desde Firestore en /config/fcm (serverKey).
     * Si no existe o falla, recurre a BuildConfig.FCM_SERVER_KEY.
     */
    private suspend fun getFcmServerKey(): String {
        try {
            val doc = FirebaseFirestore.getInstance()
                .collection("config")
                .document("fcm")
                .get()
                .await()
            val serverKey = doc.getString("serverKey")
            if (!serverKey.isNullOrEmpty()) {
                Log.d(TAG, "Clave FCM recuperada desde Firestore con éxito")
                return serverKey
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error recuperando clave FCM desde Firestore, se usará fallback de local.properties", e)
        }
        return BuildConfig.FCM_SERVER_KEY
    }

    suspend fun sendNotification(toToken: String, data: Map<String, String>) {
        if (toToken.isEmpty()) {
            Log.w(TAG, "Token de destino vacío, omitiendo notificación")
            return
        }

        val serverKey = getFcmServerKey()
        if (serverKey.isEmpty()) {
            Log.e(TAG, "No se encontró FCM Server Key ni en Firestore ni en BuildConfig. No se puede enviar la notificación.")
            return
        }

        val authHeader = "key=$serverKey"
        val payload = FcmPayload(to = toToken, data = data)

        try {
            val response = fcmApi.sendNotification(authHeader, payload)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Log.d(TAG, "Notificación FCM enviada con éxito. Success: ${body.success}, Failure: ${body.failure}")
                }
            } else {
                Log.e(TAG, "Error en la respuesta de FCM: ${response.code()} - ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Excepción al enviar notificación FCM", e)
        }
    }
}
