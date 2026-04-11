package com.neon.ascent.feature.library

data class EBook(
    val id: String,
    val title: String,
    val author: String,
    val chapters: List<Chapter>,
    val language: String // "en", "he", "grc"
)

data class Chapter(
    val title: String,
    val content: String // XHTML content
)
