package com.neon.ascent.feature.terminal.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neon.ascent.core.domain.SpecialRepository
import com.neon.ascent.core.domain.model.BenchmarkTest
import com.neon.ascent.core.domain.model.SpecialAttribute
import com.neon.ascent.core.domain.model.SpecialType
import com.neon.ascent.feature.health.HealthSyncUseCase
import com.patrykandpatrick.vico.core.entry.FloatEntry
import com.patrykandpatrick.vico.core.entry.entryOf
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AttributeHistoryViewModel @Inject constructor(
    private val specialRepository: SpecialRepository,
    private val healthSyncUseCase: HealthSyncUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val attributeTypeName: String = savedStateHandle["attributeType"] ?: SpecialType.INTELLIGENCE.name
    private val attributeType: SpecialType = SpecialType.valueOf(attributeTypeName)

    val currentAttribute: StateFlow<SpecialAttribute?> = specialRepository
        .getSpecialAttribute(attributeType)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val benchmarkHistory: StateFlow<List<BenchmarkTest>> = specialRepository
        .getBenchmarkHistory(attributeType)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val percentileTimeSeries: StateFlow<List<FloatEntry>> = benchmarkHistory.map { tests ->
        tests.sortedBy { it.timestamp }
            .map { entryOf(it.timestamp.toEpochMilli().toFloat(), it.percentile?.toFloat() ?: 50f) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun runReTest(type: SpecialType) {
        // Trigger re-test logic
    }

    fun triggerHealthSyncForAttribute(type: SpecialType) {
        viewModelScope.launch {
            try {
                healthSyncUseCase()
            } catch (e: Exception) {
                // Log error or handle failure
            }
        }
    }
}
