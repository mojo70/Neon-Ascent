package com.neon.ascent.feature.habits.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neon.ascent.core.domain.goals.models.AscensionMission
import com.neon.ascent.core.domain.goals.models.AscensionTask
import com.neon.ascent.core.domain.goals.models.AscensionTaskType
import com.neon.ascent.core.domain.model.SpecialType
import com.neon.ascent.core.domain.repository.AscensionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class HabitsViewModel @Inject constructor(
    private val ascensionRepository: AscensionRepository
) : ViewModel() {

    val recurringTasks: StateFlow<List<AscensionTask>> = ascensionRepository.getAllRecurringTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeMissions: StateFlow<List<AscensionMission>> = ascensionRepository.getActiveMissions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayProgress: StateFlow<String> = combine(recurringTasks, activeMissions) { tasks, missions ->
        val completedTasks = tasks.count { it.lastCompleted != null }
        val totalTasks = tasks.size
        val missionProgress = if (missions.isEmpty()) 0 else (missions.sumOf { it.progress.toDouble() } / missions.size * 100).toInt()
        "${completedTasks}/${totalTasks} tasks • ${missionProgress}% missions"
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Loading...")

    fun completeTask(task: AscensionTask) {
        viewModelScope.launch {
            ascensionRepository.completeTask(task, null, null, null)
        }
    }

    fun createQuickHabit(title: String, linkedAttributes: List<SpecialType>) {
        viewModelScope.launch {
            val task = AscensionTask(
                id = UUID.randomUUID().toString(),
                parentId = null,
                title = title,
                description = "Quick recurring task",
                type = AscensionTaskType.RECURRING,
                linkedAttributes = linkedAttributes
            )
            ascensionRepository.insertTask(task)
        }
    }
}
