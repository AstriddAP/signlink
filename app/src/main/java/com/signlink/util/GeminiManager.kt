package com.signlink.util

import android.content.Context
import com.signlink.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val apiKey = BuildConfig.GEMINI_API_KEY

    private val model by lazy {
        GenerativeModel(
            modelName = "gemini-2.5-flash",
            apiKey = apiKey
        )
    }

    private fun getSummaryMode(): String {
        val prefs = context.getSharedPreferences("signlink_prefs", Context.MODE_PRIVATE)
        return prefs.getString("summary_mode", "sencillo") ?: "sencillo"
    }

    /**
     * Simplifica un texto complejo a lenguaje sencillo con emojis para mejor comprensión.
     * Soporta modos: sencillo, estandar y tecnico.
     */
    suspend fun simplifyMessage(complexText: String, mode: String = getSummaryMode()): String? = withContext(Dispatchers.IO) {
        try {
            val prompt = when (mode) {
                "sencillo" -> """
                    Eres un asistente para personas con discapacidad auditiva.
                    Tu tarea es simplificar el siguiente mensaje para que sea extremadamente fácil de entender para alguien que se comunica principalmente con lengua de señas (ya que no suelen usar palabras muy técnicas).
                    - Usa lenguaje muy sencillo, claro y directo.
                    - Evita tecnicismos, palabras complejas, formalismos excesivos o redundancias.
                    - Usa oraciones cortas e ideas simples de asimilar.
                    - Añade emojis pertinentes para apoyar la comprensión visual.
                    
                    Mensaje complejo:
                    $complexText
                    
                    Resumen simplificado:
                """.trimIndent()
                "tecnico" -> """
                    Actúa como un asistente profesional y preciso.
                    Tu tarea es realizar un resumen técnico y detallado del siguiente mensaje.
                    - Conserva la terminología técnica, términos especializados, cifras, nombres propios y datos clave.
                    - Mantén una redacción formal, estructurada y profesional.
                    - Resume los puntos principales de forma concisa pero completa, sin omitir detalles técnicos relevantes.
                    
                    Mensaje complejo:
                    $complexText
                    
                    Resumen técnico:
                """.trimIndent()
                else -> """
                    Actúa como un asistente útil.
                    Tu tarea es realizar un resumen estándar, claro y equilibrado del siguiente mensaje.
                    - Identifica y resume las ideas y puntos principales de manera objetiva.
                    - Mantén un tono neutro y accesible.
                    - Usa una estructura bien organizada y fácil de seguir.
                    
                    Mensaje complejo:
                    $complexText
                    
                    Resumen estándar:
                """.trimIndent()
            }

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
                Actúa como un corrector ortográfico y gramatical experto en español.
                Toma el siguiente texto de voz transcrita (que no tiene signos de puntuación) y:
                1. Añade los signos de puntuación adecuados (puntos, comas, signos de interrogación/exclamación).
                2. Corrige cualquier error de ortografía o gramática.
                3. Respeta las mayúsculas al inicio de las oraciones.
                
                Devuelve únicamente el texto corregido, sin ningún tipo de introducción, explicación ni formato extra.
                
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

    /**
     * Identifica el hablante (Hablante 1 o Hablante 2) para el nuevo segmento de texto,
     * basándose en el historial de la conversación.
     */
    suspend fun diarizeSpeechSegment(
        historyText: String,
        newSegmentText: String
    ): DiarizationResult? = withContext(Dispatchers.IO) {
        try {
            val prompt = """
                Eres un asistente de transcripción y diarización de diálogos en español en tiempo real.
                Analiza el flujo de la conversación e identifica quién es el autor del nuevo segmento transcrito de entre todos los participantes.

                Instrucciones:
                1. Identifica al autor del nuevo segmento. Si el contexto revela el nombre real del hablante (por ejemplo, si le saludan por su nombre o se presenta), usa ese nombre real (ej. "Astrid", "Efraín", "Profesor"). Si no es posible deducir su nombre real, asígnale una etiqueta numerada genérica del tipo "Hablante 1", "Hablante 2", "Hablante 3", etc., manteniendo la coherencia con los mensajes del historial.
                2. Si es el primer mensaje de la conversación y no se menciona ningún nombre, asígnale "Hablante 1".
                3. Corrige la ortografía y gramática del nuevo segmento y añade los signos de puntuación adecuados (comas, puntos, signos de interrogación/exclamación).
                4. Devuelve el resultado ESTRICTAMENTE en formato JSON con los campos:
                   - "speaker": El nombre deducido del hablante (nombre real o "Hablante X").
                   - "text": El texto corregido y puntuado.
                5. Devuelve únicamente el objeto JSON, sin envolverlo en bloques de código de markdown como ```json.

                Historial de conversación:
                $historyText

                Nuevo segmento transcrito:
                $newSegmentText
            """.trimIndent()

            val response = model.generateContent(prompt)
            val jsonText = response.text?.trim() ?: return@withContext null
            val cleanJson = jsonText.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            com.google.gson.Gson().fromJson(cleanJson, DiarizationResult::class.java)
        } catch (e: Exception) {
            null
        }
    }
}

data class DiarizationResult(
    val speaker: String,
    val text: String
)

