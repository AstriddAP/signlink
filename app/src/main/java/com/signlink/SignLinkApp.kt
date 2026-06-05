package com.signlink

import android.app.Application
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SignLinkApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Cargar y aplicar de inmediato el modo oscuro guardado
        val prefs = getSharedPreferences("signlink_prefs", android.content.Context.MODE_PRIVATE)
        val isDarkMode = prefs.getBoolean("dark_mode", false)
        if (isDarkMode) {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO)
        }

        try {
            // Inicializar Firebase solo si existe el archivo de configuración interno
            // Esto evita que la app se cierre si falta google-services.json
            FirebaseApp.initializeApp(this)
        } catch (e: Exception) {
            android.util.Log.w("SignLinkApp", "Firebase no se pudo inicializar (falta google-services.json)")
        }
    }
}
