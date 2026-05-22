package com.neon.ascent.feature.goals.ui.ascension

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neon.ascent.core.domain.goals.models.*
import com.neon.ascent.core.domain.repository.AscensionRepository
import com.neon.ascent.feature.goals.domain.usecases.ExportNeuralLogUseCase
import com.neon.ascent.feature.goals.domain.usecases.NeonMentorUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.IsoFields
import java.util.UUID
import javax.inject.Inject

data class TerminalRitualUiState(
    val completionHistory: List<AscensionTaskCompletion> = emptyList(),
    val ritualAnalysis: String? = null,
    val heatmapData: Map<LocalDate, Int> = emptyMap(),
    val isLoading: Boolean = true,
    val currentQuarter: Int = 0,
    val currentYear: Int = 0,
    val directives: List<AscensionDirective> = emptyList(),
    val missions: List<AscensionMission> = emptyList(),
    val tasks: List<AscensionTask> = emptyList()
)

@HiltViewModel
class TerminalRitualViewModel @Inject constructor(
    private val repository: AscensionRepository,
    private val mentorUseCase: NeonMentorUseCase,
    private val exportNeuralLogUseCase: ExportNeuralLogUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(TerminalRitualUiState())
    val uiState = _uiState.asStateFlow()

    private val _exportEvent = MutableSharedFlow<String>()
    val exportEvent = _exportEvent.asSharedFlow()

    init {
        loadRitualData()
    }

    private fun loadRitualData() {
        val now = LocalDate.now()
        val quarter = now.get(IsoFields.QUARTER_OF_YEAR)
        val year = now.year
        
        // Start of quarter
        val startOfQuarter = now.with(IsoFields.DAY_OF_QUARTER, 1L)
            .atStartOfDay(ZoneId.systemDefault()).toInstant()

        viewModelScope.launch {
            combine(
                repository.getCompletionsInRange(startOfQuarter),
                repository.getAllDirectives(),
                repository.getActiveMissions(),
                repository.getAllRecurringTasks()
            ) { history, dirs, mis, tsk ->
                val heatmap = history.groupBy { 
                    it.timestamp.atZone(ZoneId.systemDefault()).toLocalDate() 
                }.mapValues { it.value.size }

                TerminalRitualUiState(
                    completionHistory = history,
                    heatmapData = heatmap,
                    currentQuarter = quarter,
                    currentYear = year,
                    directives = dirs,
                    missions = mis,
                    tasks = tsk,
                    isLoading = false
                )
            }.collect { updatedState ->
                _uiState.update { current ->
                    updatedState.copy(
                        ritualAnalysis = current.ritualAnalysis ?: updatedState.ritualAnalysis
                    )
                }

                if (updatedState.completionHistory.isNotEmpty() && _uiState.value.ritualAnalysis == null) {
                    val analysis = mentorUseCase.getTerminalRitualAnalysis(updatedState.completionHistory)
                    repository.insertNeuralLog(
                        title = "TERMINAL_RITUAL_Q${quarter}_$year",
                        content = analysis,
                        type = "RITUAL_SYNTHESIS"
                    )
                    _uiState.update { it.copy(ritualAnalysis = analysis) }
                }
            }
        }
    }

    fun exportNeuralLog() {
        viewModelScope.launch {
            val logContent = exportNeuralLogUseCase()
            _exportEvent.emit(logContent)
        }
    }

    fun createProposedDirective(title: String, description: String, visionStatement: String?) {
        viewModelScope.launch {
            val directive = AscensionDirective(
                id = UUID.randomUUID().toString(),
                title = title,
                description = description,
                visionStatement = visionStatement,
                isQuarterly = true,
                targetEndDate = LocalDate.now().plusMonths(3)
            )
            repository.insertDirective(directive)
            repository.insertNeuralLog(
                title = "New Quarterly Directive Deployed",
                content = "Directive deployed in planning review: $title",
                type = "DIRECTIVE_DEPLOY"
            )
        }
    }
}
