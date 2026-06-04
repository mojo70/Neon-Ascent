package com.neon.ascent.feature.dashboard

import com.neon.ascent.domain.model.UserStory
import com.neon.ascent.model.BioAgeResult
import com.neon.ascent.model.TerminalEvent
import com.neon.ascent.core.domain.goals.models.*
import com.neon.ascent.core.common.DopamineEvent
import com.neon.ascent.core.common.OperatorIdentity

data class DashboardUiState(
    val userStory: UserStory = UserStory(),
    val cyberLoreSnippet: String = "",
    val activeDirectives: List<AscensionDirective> = emptyList(),
    val activeMissions: List<AscensionMission> = emptyList(),
    val todayTasks: List<AscensionTask> = emptyList(),
    val terminalFeed: List<TerminalEvent> = emptyList(),
    val bioAgeResult: BioAgeResult? = null,
    val totalHabitDays: Int = 0,
    val totalXpThisQuarter: Long = 0,
    val isLoading: Boolean = true,
    val dopamineEvent: DopamineEvent? = null,
    val identity: OperatorIdentity = OperatorIdentity(),
    val recentLogMessages: List<String> = emptyList(),
    val terminalMessages: List<TerminalMessage> = listOf(
        TerminalMessage("DECK_OS_STABLE", isFromUser = false),
        TerminalMessage("WAITING_FOR_INPUT...", isFromUser = false)
    ),
    val terminalInput: String = ""
)

data class TerminalMessage(
    val text: String,
    val isFromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
