package com.neon.ascent.feature.notifications.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neon.ascent.core.data.datastore.BriefPreferencesDataStore
import com.neon.ascent.core.data.notifications.BriefFactsBuilder
import com.neon.ascent.core.domain.notifications.BriefService
import com.neon.ascent.core.domain.notifications.brief.AmTemplateWriter
import com.neon.ascent.core.domain.notifications.brief.BriefStanceResolver
import com.neon.ascent.core.domain.notifications.brief.PmTemplateWriter
import com.neon.ascent.core.domain.notifications.models.BriefSlot
import com.neon.ascent.core.domain.repository.AscensionRepository
import com.neon.ascent.feature.notifications.data.NeuralPingManager
import com.neon.ascent.feature.notifications.data.SmartPingScheduler
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
        observePreferences()
    }

    private fun observePreferences() {
        viewModelScope.launch {
            briefPrefs.targetWakeWd.collect { wd ->
                _uiState.update { it.copy(targetWakeWd = wd ?: "07:00 (USUAL)") }
            }
        }
        viewModelScope.launch {
            briefPrefs.targetWakeWe.collect { we ->
                _uiState.update { it.copy(targetWakeWe = we ?: "08:00 (USUAL)") }
            }
        }
        viewModelScope.launch {
            briefPrefs.pulsePmMode.collect { mode ->
                _uiState.update { it.copy(pulsePmMode = mode) }
            }
        }
        viewModelScope.launch {
            briefPrefs.pulsePmCustomDays.collect { days ->
                _uiState.update { it.copy(pulsePmCustomDays = days) }
            }
        }
        viewModelScope.launch {
            briefPrefs.pulsePmCustomTime.collect { time ->
                _uiState.update { it.copy(pulsePmCustomTime = time) }
            }
        }
    }

    private fun checkBurnoutStatus() {
        viewModelScope.launch {
            try {
                val sevenDaysAgo = Instant.now().minus(7, ChronoUnit.DAYS)
                val completions = repository.getCompletionsInRange(sevenDaysAgo).first()
                val tasks = repository.getAllRecurringTasks().first()
                
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
            } catch (_: Exception) {}
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

    fun setTargetWakeWd(time: String?) {
        viewModelScope.launch {
            briefPrefs.setTargetWakeWd(time?.takeIf { it.isNotBlank() && !it.contains("USUAL") })
            smartPingScheduler.scheduleNextAdaptiveBrief()
        }
    }

    fun setTargetWakeWe(time: String?) {
        viewModelScope.launch {
            briefPrefs.setTargetWakeWe(time?.takeIf { it.isNotBlank() && !it.contains("USUAL") })
            smartPingScheduler.scheduleNextAdaptiveBrief()
        }
    }

    fun setPulsePmMode(mode: String) {
        viewModelScope.launch {
            briefPrefs.setPulsePmMode(mode)
            smartPingScheduler.scheduleNextAdaptiveBrief()
        }
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

    fun sendTestAmBrief() {
        viewModelScope.launch {
            val facts = factsBuilder.build(BriefSlot.AM)
            val stance = BriefStanceResolver.resolve(facts)
            val copy = AmTemplateWriter.write(facts, stance)
            
            briefService.showNeuralBrief(
                title = copy.shadeHeadline,
                content = copy.shadeBody,
                actions = copy.actions.map {
                    BriefService.BriefAction(it.label, it.actionName, it.type)
                },
                notificationId = BriefService.BRIEF_NOTIFICATION_ID_AM
            )
        }
    }

    fun sendTestPmBrief() {
        viewModelScope.launch {
            val facts = factsBuilder.build(BriefSlot.PM)
            val stance = BriefStanceResolver.resolve(facts)
            val copy = PmTemplateWriter.write(facts, stance)
            
            briefService.showNeuralBrief(
                title = copy.shadeHeadline,
                content = copy.shadeBody,
                actions = copy.actions.map {
                    BriefService.BriefAction(it.label, it.actionName, it.type)
                },
                notificationId = BriefService.BRIEF_NOTIFICATION_ID_PM
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
    val systemPingsEnabled: Boolean = true,
    val targetWakeWd: String = "07:00 (USUAL)",
    val targetWakeWe: String = "08:00 (USUAL)",
    val pulsePmMode: String = "NEED_ONLY",
    val pulsePmCustomDays: String = "MON,TUE,WED,THU,FRI",
    val pulsePmCustomTime: String = "20:30"
)
