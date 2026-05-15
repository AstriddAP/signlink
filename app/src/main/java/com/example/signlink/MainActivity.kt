package com.example.signlink

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Redirigir a la MainActivity correcta en com.signlink
        val intent = android.content.Intent(this, com.signlink.ui.main.MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
