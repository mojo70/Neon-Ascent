package com.neon.ascent.feature.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neon.ascent.core.data.mapper.QUEST_IMPORTED_DIRECTIVE_ID
import com.neon.ascent.core.domain.goals.models.AscensionMission
import com.neon.ascent.core.domain.goals.models.AscensionTask
import com.neon.ascent.core.domain.repository.AscensionRepository
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
    private val aiProvider: AiProvider,
    private val ascensionRepository: AscensionRepository
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

    // Binds getMissionsForDirective(imported_quests), falling back to questDao if empty
    val quests: StateFlow<List<Quest>> = questDao.getAllQuests()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val importedQuestMissions: StateFlow<List<AscensionMission>> = ascensionRepository
        .getMissionsForDirective(QUEST_IMPORTED_DIRECTIVE_ID)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dailyTasks: StateFlow<List<Task>> = taskDao.getDailyTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recurringAscensionTasks: StateFlow<List<AscensionTask>> = ascensionRepository
        .getAllRecurringTasks()
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
            // First complete or update in Ascension V3 if mapped, with fallback to legacy taskDao
            try {
                val v3Task = ascensionRepository.getTaskById(task.id).firstOrNull()
                    ?: ascensionRepository.getTaskById("qtask:${task.id}").firstOrNull()

                if (v3Task != null && isCompleted) {
                    ascensionRepository.completeTask(v3Task, null, null, null)
                }
            } catch (_: Exception) {}
            taskDao.updateTaskCompletion(task.id, isCompleted)
        }
    }

    fun completeAscensionTask(task: AscensionTask) {
        viewModelScope.launch {
            ascensionRepository.completeTask(task, null, null, null)
        }
    }

    fun setSystemDatabaseHacked(hacked: Boolean) {
        _isSystemDatabaseHacked.value = hacked
    }

    fun getTasksForQuest(questId: String): Flow<List<Task>> = taskDao.getTasksForQuest(questId)

    fun getTasksForAscensionMission(missionId: String): Flow<List<AscensionTask>> =
        ascensionRepository.getTasksForParent(missionId)

    fun decryptShard(shard: DataShard) {
        viewModelScope.launch {
            loreDao.updateShardDecrypted(shard.id, true)
        }
    }

    fun breakDownTask(task: Task) {
        // AI Logic to break down task would go here
    }
}
