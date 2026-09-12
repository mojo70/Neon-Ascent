package com.neon.ascent.feature.lore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neon.ascent.core.domain.chronicle.ChronicleEntry
import com.neon.ascent.core.domain.chronicle.ChronicleRepository
import com.neon.ascent.core.domain.repository.AscensionRepository
import com.neon.ascent.data.chronicle.ChronicleImportUseCase
import com.neon.ascent.data.local.entity.LoreChapter
import com.neon.ascent.data.repository.TaskRepository
import com.neon.ascent.data.repository.UserStoryRepository
import com.neon.ascent.domain.model.UserStory
import com.neon.ascent.domain.usecase.GenerateCyberLoreUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

@HiltViewModel
class LoreViewModel @Inject constructor(
    private val userStoryRepository: UserStoryRepository,
    private val taskRepository: TaskRepository,
    private val ascensionRepository: AscensionRepository,
    private val generateCyberLoreUseCase: GenerateCyberLoreUseCase,
    private val chronicleRepository: ChronicleRepository,
    private val chronicleImportUseCase: ChronicleImportUseCase
) : ViewModel() {

    val chronicleEntries: StateFlow<List<ChronicleEntry>> = chronicleRepository.observeChronicle()
        .map { list ->
            list.filter { it.wing == "CHRONICLE" }
                .sortedByDescending { it.timestamp }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userStory: StateFlow<UserStory> = userStoryRepository.getMainStory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserStory())

    init {
        checkAndTriggerWeeklyUpdate()
        viewModelScope.launch {
            chronicleImportUseCase.runImport()
        }
    }

    fun checkAndTriggerWeeklyUpdate() {
        viewModelScope.launch {
            val story = userStoryRepository.getMainStory().first()
            val today = LocalDate.now()
            
            if (today.dayOfWeek == DayOfWeek.SATURDAY) {
                val lastUpdateEpoch = story.lastWeeklyUpdate
                val lastUpdateDate = if (lastUpdateEpoch == 0L) LocalDate.MIN else 
                    LocalDate.ofEpochDay(lastUpdateEpoch / (24 * 60 * 60 * 1000))
                
                val startOfThisWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
                
                if (lastUpdateDate.isBefore(startOfThisWeek)) {
                    performWeeklyUpdate(story)
                }
            }
        }
    }

    private suspend fun performWeeklyUpdate(story: UserStory) {
        val accomplishments = mutableListOf<String>()
        val lastWeek = LocalDate.now().minusDays(7)
        
        val tasks = taskRepository.getDailyTasks().first()
        val completedThisWeek = tasks.filter { task ->
            task.completedDates.any { it.isAfter(lastWeek) }
        }
        accomplishments.addAll(completedThisWeek.map { "Completed task: ${it.title}" })
        
        val missions = ascensionRepository.getActiveMissions().first()
        val progressedMissions = missions.filter { it.createdAt.toEpochMilli() > System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000 }
        accomplishments.addAll(progressedMissions.map { "Progressed mission: ${it.title} (${(it.progress * 100).toInt()}% complete)" })
        
        if (accomplishments.isEmpty()) {
            accomplishments.add("Survived another week in the neon silence.")
        }

        val newChapterContent = generateCyberLoreUseCase.generateWeeklyUpdate(
            currentStory = story.cyberLore,
            accomplishments = accomplishments
        )
        
        val newChapter = LoreChapter(
            title = "WEEK_ENDING_${LocalDate.now()}",
            content = newChapterContent,
            timestamp = System.currentTimeMillis()
        )
        
        val updatedStory = story.copy(
            weeklyChapters = story.weeklyChapters + newChapter,
            lastWeeklyUpdate = System.currentTimeMillis()
        )
        
        userStoryRepository.saveStory(updatedStory)
    }

    fun hackChapter(index: Int, newContent: String) {
        viewModelScope.launch {
            val story = userStory.value
            val chapters = story.weeklyChapters.toMutableList()
            if (index in chapters.indices) {
                chapters[index] = chapters[index].copy(content = newContent, isHacked = true)
                userStoryRepository.saveStory(story.copy(weeklyChapters = chapters))
            }
        }
    }
    
    fun hackMainStory(newContent: String) {
        viewModelScope.launch {
            val story = userStory.value
            userStoryRepository.saveStory(story.copy(cyberLore = newContent))
        }
    }

    fun resetStory() {
        viewModelScope.launch {
            val story = userStory.value
            userStoryRepository.saveStory(story.copy(cyberLore = "", weeklyChapters = emptyList(), lastWeeklyUpdate = 0L))
        }
    }
}
