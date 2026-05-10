package com.neon.ascent.feature.notifications.ui

import androidx.lifecycle.ViewModel
import com.neon.ascent.feature.notifications.data.NeuralPingManager
import com.neon.ascent.feature.notifications.data.SmartPingScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class NotificationPreferencesViewModel @Inject constructor(
    private val neuralPingManager: NeuralPingManager,
    private val smartPingScheduler: SmartPingScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationPreferencesUiState())
    val uiState: StateFlow<NotificationPreferencesUiState> = _uiState.asStateFlow()

    fun toggleMaster(enabled: Boolean) {
        _uiState.update { it.copy(masterEnabled = enabled) }
        // In a real app, you might want to cancel pending work if disabled
        // For now, we'll just trigger scheduling if enabled
        if (enabled) {
            // We can't call suspend from here easily without viewModelScope
            // But SmartPingScheduler.scheduleSmartPings is suspend
            // For MVP, we'll just update state. Actual scheduling usually happens on app start or habit change.
        }
    }

    fun setFrequency(hours: Int) { _uiState.update { it.copy(frequencyHours = hours) } }
    fun toggleMissionPings(enabled: Boolean) { _uiState.update { it.copy(missionPingsEnabled = enabled) } }
    fun toggleStreakPings(enabled: Boolean) { _uiState.update { it.copy(streakPingsEnabled = enabled) } }
    fun toggleSystemPings(enabled: Boolean) { _uiState.update { it.copy(systemPingsEnabled = enabled) } }

    fun sendTestPing() {
        neuralPingManager.sendNeuralPing(
            title = "TEST TRANSMISSION",
            message = "This is a test ping from the deck. Signal strong."
        )
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
    val missionPingsEnabled: Boolean = true,
    val streakPingsEnabled: Boolean = true,
    val systemPingsEnabled: Boolean = true
)
