package com.neon.ascent.feature.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neon.ascent.data.local.BiohackingDao
import com.neon.ascent.data.local.LoreDao
import com.neon.ascent.data.local.QuestDao
import com.neon.ascent.data.local.TaskDao
import com.neon.ascent.data.repository.JournalRepository
import com.neon.ascent.feature.biohacking.AiProvider
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
    private val taskDao: TaskDao,
    private val loreDao: LoreDao,
    private val aiProvider: AiProvider
) : ViewModel() {

    val entries: StateFlow<List<JournalEntry>> = journalRepository.allEntries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bioProtocolLogs: StateFlow<List<BioProtocolLog>> = biohackingDao.getAllProtocolLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val quests: StateFlow<List<Quest>> = questDao.getAllQuests()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dailyTasks: StateFlow<List<Task>> = taskDao.getDailyTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val shards: StateFlow<List<DataShard>> = loreDao.getAllDataShards()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val memories: StateFlow<List<MemoryFragment>> = loreDao.getAllMemoryFragments()
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

    fun decryptShard(shard: DataShard) {
        viewModelScope.launch {
            loreDao.updateDataShard(shard.copy(isDecrypted = true))
        }
    }

    fun breakDownTask(task: Task) {
        viewModelScope.launch {
            val prompt = "Break down this task into 3 actionable cyberpunk steps: ${task.description}"
            val result = aiProvider.generateContent(prompt)
            taskDao.updateTask(task.copy(aiBreakdownNotes = result))
        }
    }
}
