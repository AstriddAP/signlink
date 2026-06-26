package com.signlink.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface FcmApi {
    @POST("fcm/send")
    suspend fun sendNotification(
        @Header("Authorization") authorizationHeader: String,
        @Body payload: FcmPayload
    ): Response<FcmResponse>
}

data class FcmPayload(
    val to: String,
    val priority: String = "high",
    val data: Map<String, String>
)

data class FcmResponse(
    val multicast_id: Long = 0,
    val success: Int = 0,
    val failure: Int = 0,
    val canonical_ids: Int = 0,
    val results: List<FcmResult>? = null
)

data class FcmResult(
    val message_id: String? = null,
    val error: String? = null
)
