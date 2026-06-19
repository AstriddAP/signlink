package com.signlink.util

import android.content.Context
import android.speech.tts.TextToSpeech
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TTSManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var tts: TextToSpeech? = null
    private var isReady = false

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("es", "ES")
                isReady = true
            }
        }
    }

    fun speak(text: String) {
        if (isReady) {
            val prefs = context.getSharedPreferences("signlink_prefs", Context.MODE_PRIVATE)
            val pitch = prefs.getFloat("voice_pitch", 1.0f)
            val speed = prefs.getFloat("voice_speed", 1.0f)
            val voiceName = prefs.getString("voice_name", null)

            tts?.setPitch(pitch)
            tts?.setSpeechRate(speed)

            if (!voiceName.isNullOrEmpty()) {
                val selectedVoice = tts?.voices?.find { it.name == voiceName }
                if (selectedVoice != null) {
                    tts?.voice = selectedVoice
                }
            } else {
                // Si no hay voz configurada explícitamente, seleccionamos una voz natural que se adecúe al tono deseado
                if (pitch < 1.0f) {
                    val maleVoice = tts?.voices?.find { voice ->
                        val name = voice.name.lowercase()
                        val localeStr = voice.locale?.toString()?.lowercase() ?: ""
                        val lang = voice.locale?.language?.lowercase() ?: ""
                        (lang == "es" || lang == "spa") && (
                            name.contains("male") || name.contains("masc") ||
                            name.contains("eed") || name.contains("hco") ||
                            name.contains("gft") || name.contains("fnd") ||
                            name.contains("jcd") || name.contains("old") ||
                            (name.contains("dfg") && !name.contains("es-es")) ||
                            (name.contains("sfg") && (localeStr.contains("mx") || localeStr.contains("us")))
                        )
                    }
                    if (maleVoice != null) {
                        tts?.voice = maleVoice
                    }
                } else if (pitch > 1.0f) {
                    val femaleVoice = tts?.voices?.find { voice ->
                        val name = voice.name.lowercase()
                        val localeStr = voice.locale?.toString()?.lowercase() ?: ""
                        val lang = voice.locale?.language?.lowercase() ?: ""
                        (lang == "es" || lang == "spa") && (
                            name.contains("female") || name.contains("feme") ||
                            name.contains("ana") || name.contains("eea") ||
                            name.contains("prt") || name.contains("aod") ||
                            name.contains("esc") || name.contains("nmi") ||
                            (name.contains("sfg") && localeStr.contains("es-es"))
                        )
                    }
                    if (femaleVoice != null) {
                        tts?.voice = femaleVoice
                    }
                }
            }

            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    fun stop() {
        tts?.stop()
    }
}

