package com.signlink.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val content: String, // Este contenido se guardará encriptado o el acceso estará protegido
    val type: String, // "PASSWORD", "EMAIL", "PIN", "GENERAL"
    val createdAt: Long = System.currentTimeMillis()
)
