package com.signlink.util

import com.signlink.BuildConfig
import android.graphics.Bitmap
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiManager @Inject constructor() {

    private val apiKey = BuildConfig.GEMINI_API_KEY

    private val model by lazy {
        GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = apiKey
        )
    }

    suspend fun simplifyMessage(complexText: String): String? = withContext(Dispatchers.IO) {
        try {
            val prompt = """
                Eres un asistente para personas con discapacidad auditiva. 
                Tu tarea es simplificar el siguiente mensaje para que sea fácil de entender.
                Usa lenguaje sencillo, frases cortas y añade emojis pertinentes.
                
                Mensaje complejo:
                ${complexText}
                
                Resumen simplificado:
            """.trimIndent()

            val response = model.generateContent(prompt)
            response.text
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun correctMessage(text: String): String? = withContext(Dispatchers.IO) {
        try {
            val prompt = """
                Actúa como un corrector ortográfico y gramatical experto.
                Corrige el siguiente texto manteniendo su sentido original pero mejorando la redacción y ortografía.
                Si el texto ya es correcto, devuélvelo tal cual.
                
                Texto a corregir:
                $text
                
                Texto corregido:
            """.trimIndent()

            val response = model.generateContent(prompt)
            response.text
        } catch (e: Exception) {
            null
        }
    }

    suspend fun defineWord(word: String): String? = withContext(Dispatchers.IO) {
        try {
            val prompt = """
                Explica el significado de la palabra o concepto "$word" de una manera extremadamente sencilla, 
                como si se lo explicaras a un niño de 8 años o a una persona con dificultades de comprensión.
                Usa ejemplos cotidianos y emojis.
                
                Palabra: $word
                Explicación simplificada:
            """.trimIndent()

            val response = model.generateContent(prompt)
            response.text
        } catch (e: Exception) {
            null
        }
    }

    suspend fun explainNews(newsContent: String): String? = withContext(Dispatchers.IO) {
        try {
            val prompt = """
                Explica la siguiente noticia de forma sencilla y resumida para una persona con discapacidad auditiva.
                Tu explicación DEBE tener estos puntos:
                - 📰 ¿Qué pasó?
                - 📅 ¿Cuándo ocurrió?
                - 👥 ¿Quiénes participaron?
                - 💡 Resumen fácil de entender
                
                Usa emojis, frases cortas y lenguaje muy simple.
                
                Noticia:
                $newsContent
            """.trimIndent()

            val response = model.generateContent(prompt)
            response.text
        } catch (e: Exception) {
            null
        }
    }

    suspend fun analyzeDocument(bitmap: Bitmap): String? = withContext(Dispatchers.IO) {
        try {
            val prompt = """
                Analiza esta imagen de un documento (DNI, carnet, etc.).
                1. Identifica qué documento es.
                2. Extrae los datos principales (Nombre, fecha de vencimiento).
                3. Explica de forma muy breve y sencilla qué contiene para que una persona pueda escucharlo.
                Usa un tono amable y pausado.
            """.trimIndent()

            val inputContent = content {
                image(bitmap)
                text(prompt)
            }

            val response = model.generateContent(inputContent)
            response.text
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun formatNumbersInText(text: String): String? = withContext(Dispatchers.IO) {
        try {
            val prompt = """
                Identifica números, precios, fechas o cantidades en el texto.
                Devuelve el texto resaltando los números entre asteriscos (ej: *S/ 50.00*).
                Texto: $text
            """.trimIndent()

            val response = model.generateContent(prompt)
            response.text // Esto es String?
        } catch (e: Exception) {
            null
        }
    }

    suspend fun extractDniData(bitmap: Bitmap): String? = withContext(Dispatchers.IO) {
        try {
            val prompt = """
                Analiza esta imagen de un DNI peruano y extrae la información en este formato JSON exacto:
                {
                  "nombres": "...",
                  "apellidos": "...",
                  "dni": "...",
                  "direccion": "...",
                  "fecha_nacimiento": "..."
                }
                Si no puedes leer un campo, pon "No detectado". No incluyas nada más que el JSON.
            """.trimIndent()

            val inputContent = content {
                image(bitmap)
                text(prompt)
            }

            val response = model.generateContent(inputContent)
            response.text?.replace("```json", "")?.replace("```", "")?.trim()
        } catch (e: Exception) {
            null
        }
    }
}
