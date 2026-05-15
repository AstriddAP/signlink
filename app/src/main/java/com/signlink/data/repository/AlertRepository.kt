package com.signlink.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.signlink.data.model.Alert
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

interface AlertRepository {
    suspend fun createPanicAlert(userId: String, latitude: Double, longitude: Double): Result<String>
}

@Singleton
class AlertRepositoryImpl @Inject constructor(
    // Firestore inyectado pero no usado para evitar crash sin google-services.json
) : AlertRepository {

    override suspend fun createPanicAlert(userId: String, latitude: Double, longitude: Double): Result<String> {
        return try {
            // SIMULACIÓN MOCKUP (PRO)
            delay(1000) // Simular latencia de red
            Result.success("mock_alert_id")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
