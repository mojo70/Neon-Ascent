package com.neon.ascent.domain.model

data class AspirationDatabank(
    val aspirations: List<AspirationTemplate>
)

data class AspirationTemplate(
    val keyword: String,
    val title: String,
    val suggestedGoals: List<GoalTemplate>
)

data class GoalTemplate(
    val title: String,
    val description: String,
    val targetValue: Float,
    val unit: String,
    val milestones: List<Int>? = null
)
