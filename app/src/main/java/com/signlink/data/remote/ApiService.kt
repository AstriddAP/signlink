package com.signlink.data.remote

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ApiService {
    @retrofit2.http.GET("/")
    suspend fun wakeUp(): Response<Unit>

    @Multipart
    @POST("analizar-imagen/")
    suspend fun analizarImagen(
        @Part file: MultipartBody.Part
    ): Response<AnalysisResponse>

    @POST("resumir-texto/")
    suspend fun resumirTexto(
        @Body request: SummaryRequest
    ): Response<SummaryResponse>

    @Multipart
    @POST("resumir-archivo/")
    suspend fun resumirArchivo(
        @Part file: MultipartBody.Part
    ): Response<SummaryResponse>
}

data class SummaryRequest(
    val texto: String
)

data class SummaryResponse(
    val resumen: String
)
