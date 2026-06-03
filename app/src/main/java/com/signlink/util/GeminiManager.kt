package com.signlink.util

import com.signlink.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
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

    /**
     * Simplifica un texto complejo a lenguaje sencillo con emojis para mejor comprensión.
     */
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
            null
        }
    }

    /**
     * Corrige la gramática y ortografía de un texto.
     */
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

    /**
     * Define un concepto de forma extremadamente sencilla (para nivel de 8 años).
     */
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

    /**
     * Resalta números y cantidades en un texto para facilitar la localización visual.
     */
    suspend fun formatNumbersInText(text: String): String? = withContext(Dispatchers.IO) {
        try {
            val prompt = """
                Identifica números, precios, fechas o cantidades en el texto.
                Devuelve el texto resaltando los números entre asteriscos (ej: *S/ 50.00*).
                Texto: $text
            """.trimIndent()

            val response = model.generateContent(prompt)
            response.text
        } catch (e: Exception) {
            null
        }
    }
}
