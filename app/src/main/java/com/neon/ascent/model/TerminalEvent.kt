package com.neon.ascent.model

data class TerminalEvent(
    val id: String,
    val title: String,
    val type: String, // MISSION, TASK, PROTOCOL, LOG
    val status: String, // PENDING, COMPLETED, ADDED, ACTIVE, LOGGED
    val timestamp: Long
)
