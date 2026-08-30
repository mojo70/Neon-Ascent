package com.neon.ascent.feature.notifications.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neon.ascent.core.domain.notifications.BriefService
import com.neon.ascent.core.domain.repository.AscensionRepository
import com.neon.ascent.feature.notifications.data.NeuralPingManager
import com.neon.ascent.feature.notifications.data.SmartPingScheduler
import com.neon.ascent.core.data.datastore.BriefPreferencesDataStore
import com.neon.ascent.core.data.notifications.BriefFactsBuilder
import com.neon.ascent.core.domain.notifications.brief.BriefStanceResolver
import com.neon.ascent.core.domain.notifications.brief.TemplateCopyWriter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject

@HiltViewModel
class NotificationPreferencesViewModel @Inject constructor(
    private val neuralPingManager: NeuralPingManager,
    private val briefService: BriefService,
    private val smartPingScheduler: SmartPingScheduler,
    private val repository: AscensionRepository,
    private val briefPrefs: BriefPreferencesDataStore,
    private val factsBuilder: BriefFactsBuilder
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationPreferencesUiState())
    val uiState: StateFlow<NotificationPreferencesUiState> = _uiState.asStateFlow()

    init {
        checkBurnoutStatus()
    }

    private fun checkBurnoutStatus() {
        viewModelScope.launch {
            try {
                // Calculate 7-day completion rate from repository
                val sevenDaysAgo = Instant.now().minus(7, ChronoUnit.DAYS)
                val completions = repository.getCompletionsInRange(sevenDaysAgo).first()
                val tasks = repository.getAllRecurringTasks().first()
                
                // If tasks exist, calculate rate
                if (tasks.isNotEmpty()) {
                    val completedTasksCount = completions.map { it.taskId }.distinct().size
                    val completionRate = completedTasksCount.toFloat() / tasks.size
                    val burnoutActive = completionRate < 0.4f
                    
                    _uiState.update { 
                        it.copy(
                            burnoutFatigueActive = burnoutActive,
                            completionRate7Day = (completionRate * 100).toInt()
                        ) 
                    }
                }
            } catch (e: Exception) {
                // Fallback
            }
        }
    }

    fun toggleMaster(enabled: Boolean) {
        _uiState.update { it.copy(masterEnabled = enabled) }
        if (enabled) {
            viewModelScope.launch {
                smartPingScheduler.scheduleSmartPings()
            }
        }
    }

    fun setFrequency(hours: Int) { 
        _uiState.update { it.copy(frequencyHours = hours) } 
    }
    
    fun setPingBudget(budget: String) { 
        _uiState.update { it.copy(pingBudget = budget) } 
    }
    
    fun toggleAdaptiveWake(enabled: Boolean) { 
        viewModelScope.launch {
            briefPrefs.setAdaptiveWakeEnabled(enabled)
            smartPingScheduler.scheduleNextAdaptiveBrief()
            _uiState.update { it.copy(adaptiveWakeDefault = enabled) } 
        }
    }

    fun toggleMissionPings(enabled: Boolean) { _uiState.update { it.copy(missionPingsEnabled = enabled) } }
    fun toggleStreakPings(enabled: Boolean) { _uiState.update { it.copy(streakPingsEnabled = enabled) } }
    fun toggleSystemPings(enabled: Boolean) { _uiState.update { it.copy(systemPingsEnabled = enabled) } }

    fun sendTestPing() {
        neuralPingManager.sendNeuralPing(
            title = "SINGLE TRANSMISSION // PROTOCOL_READY",
            message = "Operator. Hydration protocol window is open. 16oz awaits. +10 XP on breach.",
            taskId = "test_id"
        )
    }

    fun sendTestBrief() {
        viewModelScope.launch {
            val facts = factsBuilder.build()
            val stance = com.neon.ascent.core.domain.notifications.brief.BriefStanceResolver.resolve(facts)
            val copy = com.neon.ascent.core.domain.notifications.brief.TemplateCopyWriter.write(facts, stance)
            
            briefService.showNeuralBrief(
                title = copy.headline + " // TEST",
                content = copy.body,
                actions = listOf(
                    BriefService.BriefAction(
                        label = "OPEN DECK",
                        actionName = BriefService.ACTION_OPEN_DECK,
                        type = "DASHBOARD"
                    )
                )
            )
        }
    }

    fun resetToDefaults() {
        _uiState.update { NotificationPreferencesUiState() }
    }
}

data class NotificationPreferencesUiState(
    val masterEnabled: Boolean = true,
    val frequencyHours: Int = 4,
    val quietStartHour: Int = 22,
    val quietEndHour: Int = 8,
    val pingBudget: String = "MEDIUM", // "LOW", "MEDIUM", "HIGH"
    val adaptiveWakeDefault: Boolean = false,
    val burnoutFatigueActive: Boolean = false,
    val completionRate7Day: Int = 100,
    val missionPingsEnabled: Boolean = true,
    val streakPingsEnabled: Boolean = true,
    val systemPingsEnabled: Boolean = true
)
