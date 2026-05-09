package com.neon.ascent.feature.health.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.neon.ascent.core.data.datastore.HealthPreferencesDataStore
import com.neon.ascent.core.domain.special.usecases.UpdateSpecialFromHealthUseCase
import com.neon.ascent.feature.health.data.HealthConnectManager
import com.neon.ascent.feature.health.data.workers.HealthSyncWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class HealthViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    val healthManager: HealthConnectManager,
    private val updateSpecialFromHealthUseCase: UpdateSpecialFromHealthUseCase,
    private val healthPrefs: HealthPreferencesDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(HealthUiState())
    val uiState: StateFlow<HealthUiState> = _uiState.asStateFlow()

    // Backward compatibility for existing screen
    val hasPermissions: StateFlow<Boolean> = _uiState
        .map { it.hasPermissions }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    init {
        observeSyncStatus()
        checkInitialHealthStatus()
    }

    private fun checkInitialHealthStatus() {
        viewModelScope.launch {
            val hasPermissions = healthManager.isAvailableAndHasPermissions()
            val lastSync = healthPrefs.lastSyncTime.first()
            _uiState.update { it.copy(
                isAvailable = true,
                hasPermissions = hasPermissions,
                lastSyncTime = lastSync
            ) }
        }
    }

    fun checkPermissions() {
        checkInitialHealthStatus()
    }

    private fun observeSyncStatus() {
        WorkManager.getInstance(context)
            .getWorkInfosForUniqueWorkLiveData("neon_ascent_health_sync")
            .asFlow()
            .map { infos ->
                infos.firstOrNull()?.let { info ->
                    when (info.state) {
                        WorkInfo.State.RUNNING -> SyncStatus.Syncing
                        WorkInfo.State.SUCCEEDED -> SyncStatus.Success
                        WorkInfo.State.FAILED -> SyncStatus.Failed
                        else -> SyncStatus.Idle
                    }
                } ?: SyncStatus.Idle
            }
            .onEach { status ->
                _uiState.update { it.copy(syncStatus = status) }
            }
            .launchIn(viewModelScope)
    }

    /** Full permission flow orchestration */
    fun requestHealthPermissions() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val rationale = healthManager.getPermissionRationale()

            // Show rationale first (UI will observe this)
            _uiState.update { it.copy(showRationale = true, rationale = rationale) }

            // Note: The actual permission request needs to be triggered from the Activity/Compose layer
            // using the Activity Result Contract. We handle the result via checkPermissions() 
            // after the launcher returns.
        }
    }

    fun triggerImmediateSync() {
        viewModelScope.launch {
            _uiState.update { it.copy(syncStatus = SyncStatus.Syncing) }

            try {
                // One-time manual sync
                HealthSyncWorker.triggerImmediateSync(context)

                // Also run immediate UseCase for instant feedback
                updateSpecialFromHealthUseCase()

                val now = Instant.now()
                healthPrefs.updateLastSyncTime(now)
                
                _uiState.update { it.copy(
                    lastSyncTime = now,
                    syncStatus = SyncStatus.Success
                ) }
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    syncStatus = SyncStatus.Failed,
                    error = "Sync failed. Retrying in background..."
                ) }
            }
        }
    }

    fun dismissRationale() {
        _uiState.update { it.copy(showRationale = false) }
    }

    val autoSyncEnabled = healthPrefs.autoSyncEnabled.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), true
    )

    val syncIntervalHours = healthPrefs.syncIntervalHours.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), 8
    )

    fun setAutoSyncEnabled(enabled: Boolean) {
        viewModelScope.launch {
            healthPrefs.setAutoSyncEnabled(enabled)
        }
    }
}

// ==================== UI STATE ====================

data class HealthUiState(
    val isAvailable: Boolean = false,
    val hasPermissions: Boolean = false,
    val isLoading: Boolean = false,
    val syncStatus: SyncStatus = SyncStatus.Idle,
    val lastSyncTime: Instant? = null,
    val showRationale: Boolean = false,
    val rationale: Map<String, String> = emptyMap(),
    val error: String? = null
)

enum class SyncStatus {
    Idle, Syncing, Success, Failed
}
