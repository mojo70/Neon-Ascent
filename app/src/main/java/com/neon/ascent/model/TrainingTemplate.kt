package com.neon.ascent.model

data class TrainingTemplate(
    val id: String,
    val name: String,
    val description: String,
    val somatotype: String,
    val strength: Int,
    val agility: Int,
    val endurance: Int,
    val intelligence: Int,
    val perception: Int,
    val charisma: Int,
    val luck: Int
)
