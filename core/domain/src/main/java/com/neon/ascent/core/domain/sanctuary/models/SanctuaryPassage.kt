package com.neon.ascent.core.domain.sanctuary.models

data class SanctuaryPassage(
    val id: String,
    val book: String,
    val chapter: Int,
    val verses: String,
    val reference: String,
    val translation: String = "ESV",
    val title: String? = null,
    val text: String,
    val themes: List<String>,
    val tags: List<String>,
    val sourceNote: String? = null,
    val sortOrder: Int,
    val isFavoriteDefault: Boolean = false
)
