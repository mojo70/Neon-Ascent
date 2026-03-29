package com.neon.ascent.feature.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neon.ascent.data.local.BiohackingDao
import com.neon.ascent.data.local.QuestDao
import com.neon.ascent.data.local.TaskDao
import com.neon.ascent.data.repository.JournalRepository
import com.neon.ascent.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class JournalViewModel @Inject constructor(
    private val journalRepository: JournalRepository,
    private val biohackingDao: BiohackingDao,
    private val questDao: QuestDao,
    private val taskDao: TaskDao
) : ViewModel() {

    val entries: StateFlow<List<JournalEntry>> = journalRepository.allEntries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bioProtocolLogs: StateFlow<List<BioProtocolLog>> = biohackingDao.getAllProtocolLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val quests: StateFlow<List<Quest>> = questDao.getAllQuests()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dailyTasks: StateFlow<List<Task>> = taskDao.getDailyTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isSystemDatabaseHacked = MutableStateFlow(false)
    val isSystemDatabaseHacked = _isSystemDatabaseHacked.asStateFlow()

    fun toggleHeart(entry: JournalEntry) {
        viewModelScope.launch {
            journalRepository.toggleHeart(entry.id, !entry.isHearted)
        }
    }

    fun deleteEntry(entry: JournalEntry) {
        viewModelScope.launch {
            journalRepository.removeFromJournal(entry)
        }
    }

    fun updateTaskCompletion(task: Task, isCompleted: Boolean) {
        viewModelScope.launch {
            taskDao.updateTask(task.copy(isCompleted = isCompleted))
        }
    }

    fun setSystemDatabaseHacked(hacked: Boolean) {
        _isSystemDatabaseHacked.value = hacked
    }
    
    fun getTasksForQuest(questId: String): Flow<List<Task>> {
        return taskDao.getTasksForQuest(questId)
    }
}
