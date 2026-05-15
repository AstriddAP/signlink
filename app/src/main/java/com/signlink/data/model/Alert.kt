package com.signlink.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint

data class Alert(
    val alertId: String = "",
    val userId: String = "",
    val type: String = "emergency", // 'emergency' | 'geofence' | 'custom'
    val location: GeoPoint? = null,
    val address: String = "",
    val notifiedContacts: List<String> = emptyList(),
    val status: String = "active", // 'active' | 'resolved'
    val timestamp: Timestamp? = null
)
