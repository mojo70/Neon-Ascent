package com.neon.ascent.feature.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neon.ascent.data.local.*
import com.neon.ascent.data.repository.JournalRepository
import com.neon.ascent.feature.biohacking.AiProvider
import com.neon.ascent.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
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

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    val entries: StateFlow<List<JournalEntry>> = combine(
        journalRepository.allEntries,
        _searchQuery
    ) { entries, query ->
        if (query.isBlank()) entries
        else entries.filter { it.text.contains(query, ignoreCase = true) || it.category.contains(query, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bioProtocolLogs: StateFlow<List<BioProtocolLog>> = combine(
        biohackingDao.getAllProtocolLogs(),
        _searchQuery
    ) { logs, query ->
        if (query.isBlank()) logs
        else logs.filter { 
            it.protocolId.contains(query, ignoreCase = true) || 
            (it.notes?.contains(query, ignoreCase = true) == true) ||
            (it.sideEffects?.contains(query, ignoreCase = true) == true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
            taskDao.updateTaskCompletion(task.id, isCompleted)
        }
    }

    fun setSystemDatabaseHacked(hacked: Boolean) {
        _isSystemDatabaseHacked.value = hacked
    }

    fun getTasksForQuest(questId: String): Flow<List<Task>> = taskDao.getTasksForQuest(questId)

    fun decryptShard(shard: DataShard) {
        viewModelScope.launch {
            loreDao.updateShardDecrypted(shard.id, true)
        }
    }

    fun breakDownTask(task: Task) {
        // AI Logic to break down task would go here
    }
}
