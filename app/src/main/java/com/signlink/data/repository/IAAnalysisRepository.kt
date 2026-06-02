package com.signlink.data.repository

import com.signlink.data.remote.AnalysisResponse
import com.signlink.data.remote.ApiService
import com.signlink.data.remote.SummaryRequest
import com.signlink.data.remote.SummaryResponse
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IAAnalysisRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun analyzeImage(imageFile: File): Result<AnalysisResponse> {
        return try {
            if (!imageFile.exists() || imageFile.length() == 0L) {
                return Result.failure(Exception("El archivo de imagen está vacío o no existe"))
            }
            val requestFile = imageFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("file", "image.jpg", requestFile)
            val response = apiService.analizarImagen(body)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMsg = if (response.code() == 429 || response.code() == 500) "LIMITE_ALCANZADO" else "ERROR_CONEXION"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            if (imageFile.exists()) imageFile.delete()
        }
    }

    suspend fun resumirTexto(texto: String): Result<SummaryResponse> {
        return try {
            val response = apiService.resumirTexto(SummaryRequest(texto))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Error al resumir texto"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resumirArchivo(file: File): Result<SummaryResponse> {
        return try {
            // Detectar el tipo de contenido basado en la extensión
            val mimeType = when (file.extension.lowercase()) {
                "pdf" -> "application/pdf"
                "png" -> "image/png"
                "jpg", "jpeg" -> "image/jpeg"
                "txt" -> "text/plain"
                else -> "application/octet-stream"
            }
            
            val requestFile = file.asRequestBody(mimeType.toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
            val response = apiService.resumirArchivo(body)
            
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Error en servidor: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
