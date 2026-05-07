package com.neon.ascent.domain.model

data class TaskBank(
    val tasks: List<TaskBankTemplate>
)

data class TaskBankTemplate(
    val category: String,
    val title: String,
    val description: String,
    val baseMinutes: Int,
    val frequency: String,
    val parameter: String? = null,
    val defaultValue: Int? = null,
    val unit: String? = null
)
