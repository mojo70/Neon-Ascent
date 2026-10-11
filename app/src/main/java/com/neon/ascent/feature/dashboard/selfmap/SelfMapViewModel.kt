package com.neon.ascent.feature.dashboard.selfmap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neon.ascent.core.data.local.entity.EoEEntryEntity
import com.neon.ascent.core.data.local.entity.SelfItemEntity
import com.neon.ascent.core.data.repository.SelfMapRepository
import com.neon.ascent.core.domain.selfmap.SelfMapCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SelfMapUiState(
    val isOnboardingSeen: Boolean = false,
    val isLoading: Boolean = true,
    val activeItems: List<SelfItemEntity> = emptyList(),
    val coreAttachmentLoad: Float = 100f,
    val coreCount: Int = 0,
    val avgImportance: Float = 0f,
    val biometricLockEnabled: Boolean = false,
    val eoeEntries: List<EoEEntryEntity> = emptyList()
)

@HiltViewModel
class SelfMapViewModel @Inject constructor(
    private val repository: SelfMapRepository
) : ViewModel() {

    @Suppress("UNCHECKED_CAST")
    val uiState: StateFlow<SelfMapUiState> = combine(
        repository.onboardingCompleted,
        repository.activeItems,
        repository.coreAttachmentLoadPercentage,
        repository.coreCount,
        repository.averageImportance,
        repository.biometricLockEnabled,
        repository.getAllEoEEntries()
    ) { flows: Array<Any?> ->
        val onboardingSeen = flows[0] as Boolean
        val items = flows[1] as List<SelfItemEntity>
        val loadPct = flows[2] as Float
        val cores = flows[3] as Int
        val avgImp = flows[4] as Float
        val bioLock = flows[5] as Boolean
        val entries = flows[6] as List<EoEEntryEntity>

        SelfMapUiState(
            isOnboardingSeen = onboardingSeen,
            isLoading = false,
            activeItems = items,
            coreAttachmentLoad = loadPct,
            coreCount = cores,
            avgImportance = avgImp,
            biometricLockEnabled = bioLock,
            eoeEntries = entries
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SelfMapUiState()
    )

    init {
        viewModelScope.launch {
            repository.preseedDefaultItemsIfEmpty()
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            repository.setOnboardingCompleted(true)
            repository.preseedDefaultItemsIfEmpty()
        }
    }

    fun skipOnboarding() {
        viewModelScope.launch {
            repository.setOnboardingCompleted(true)
            repository.preseedDefaultItemsIfEmpty()
        }
    }

    fun setBiometricLockEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setBiometricLockEnabled(enabled)
        }
    }

    fun addItem(name: String, category: SelfMapCategory, importance: Float, notes: String?) {
        viewModelScope.launch {
            repository.insertSelfItem(
                SelfItemEntity(
                    name = name,
                    category = category,
                    importance = importance.coerceIn(0.0f, 1.0f),
                    notes = notes
                )
            )
        }
    }

    fun updateItem(item: SelfItemEntity) {
        viewModelScope.launch {
            repository.updateSelfItem(item)
        }
    }

    fun archiveItem(id: Long) {
        viewModelScope.launch {
            repository.archiveSelfItem(id)
        }
    }

    fun deleteItem(id: Long) {
        viewModelScope.launch {
            repository.deleteSelfItem(id)
        }
    }

    fun logEoEReaction(
        selfItemId: Long,
        emotionLabel: String,
        epText: String,
        perceptionText: String,
        appraisal: String?,
        intensity: Int?,
        canChangeNotes: String?
    ) {
        viewModelScope.launch {
            repository.insertEoEEntry(
                EoEEntryEntity(
                    selfItemId = selfItemId,
                    emotionLabel = emotionLabel,
                    epText = epText,
                    perceptionText = perceptionText,
                    appraisal = appraisal,
                    intensity = intensity,
                    canChangeNotes = canChangeNotes
                )
            )
        }
    }

    suspend fun exportDataAsJson(): String {
        return repository.exportDataAsJson()
    }

    fun deleteAllData() {
        viewModelScope.launch {
            repository.deleteAllData()
        }
    }
}
