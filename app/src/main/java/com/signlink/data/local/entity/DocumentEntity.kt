package com.signlink.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val type: String, // "DNI", "CONADIS", "CV", "OTROS"
    val frontImagePath: String? = null,
    val backImagePath: String? = null,
    val filePath: String? = null, // Para PDFs u otros archivos
    val description: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
