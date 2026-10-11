package com.neon.ascent.feature.codex.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neon.ascent.core.data.local.entity.EoEEntryEntity
import com.neon.ascent.core.data.local.entity.SelfItemEntity
import com.neon.ascent.core.data.repository.SelfMapRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CodexSelfMapUiState(
    val activeItems: List<SelfItemEntity> = emptyList(),
    val eoeEntries: List<EoEEntryEntity> = emptyList(),
    val coreAttachmentLoad: Float = 100f,
    val currentMass: Float = 0f,
    val baselineMass: Float? = null,
    val avgImportance: Float = 0f,
    val coreCount: Int = 0,
    val biometricLockEnabled: Boolean = false
)

@HiltViewModel
class CodexSelfMapViewModel @Inject constructor(
    private val selfMapRepository: SelfMapRepository
) : ViewModel() {

    @Suppress("UNCHECKED_CAST")
    val uiState: StateFlow<CodexSelfMapUiState> = combine(
        selfMapRepository.activeItems,
        selfMapRepository.getAllEoEEntries(),
        selfMapRepository.coreAttachmentLoadPercentage,
        selfMapRepository.currentMass,
        selfMapRepository.baselineMass,
        selfMapRepository.averageImportance,
        selfMapRepository.coreCount,
        selfMapRepository.biometricLockEnabled
    ) { flows: Array<Any?> ->
        CodexSelfMapUiState(
            activeItems = flows[0] as List<SelfItemEntity>,
            eoeEntries = flows[1] as List<EoEEntryEntity>,
            coreAttachmentLoad = flows[2] as Float,
            currentMass = flows[3] as Float,
            baselineMass = flows[4] as Float?,
            avgImportance = flows[5] as Float,
            coreCount = flows[6] as Int,
            biometricLockEnabled = flows[7] as Boolean
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CodexSelfMapUiState()
    )

    suspend fun exportDataAsJson(): String {
        return selfMapRepository.exportDataAsJson()
    }

    fun deleteAllData() {
        viewModelScope.launch {
            selfMapRepository.deleteAllData()
        }
    }
}
