package com.signlink.ui.dictionary

data class SignLanguageItem(
    val id: Int,
    val word: String,
    val gifUrl: String, // Puede ser una URL o un recurso local
    val category: String = "General"
)
