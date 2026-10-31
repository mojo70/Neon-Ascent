package com.neon.ascent.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.neon.ascent.core.domain.sanctuary.models.SanctuaryPassage

@Entity(tableName = "sanctuary_passages")
data class SanctuaryPassageEntity(
    @PrimaryKey val id: String,
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

fun SanctuaryPassageEntity.toDomain() = SanctuaryPassage(
    id = id,
    book = book,
    chapter = chapter,
    verses = verses,
    reference = reference,
    translation = translation,
    title = title,
    text = text,
    themes = themes,
    tags = tags,
    sourceNote = sourceNote,
    sortOrder = sortOrder,
    isFavoriteDefault = isFavoriteDefault
)

fun SanctuaryPassage.toEntity() = SanctuaryPassageEntity(
    id = id,
    book = book,
    chapter = chapter,
    verses = verses,
    reference = reference,
    translation = translation,
    title = title,
    text = text,
    themes = themes,
    tags = tags,
    sourceNote = sourceNote,
    sortOrder = sortOrder,
    isFavoriteDefault = isFavoriteDefault
)
