package com.signlink

import android.app.Application
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SignLinkApp : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            // Inicializar Firebase solo si existe el archivo de configuración interno
            // Esto evita que la app se cierre si falta google-services.json
            FirebaseApp.initializeApp(this)
        } catch (e: Exception) {
            android.util.Log.w("SignLinkApp", "Firebase no se pudo inicializar (falta google-services.json)")
        }
    }
}
