package com.signlink.ui.news

data class NewsItem(
    val id: String,
    val title: String,
    val rawContent: String,
    var explainedContent: String? = null
)
