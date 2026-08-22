package com.neon.ascent.feature.loading

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neon.ascent.data.local.SayingsDao
import com.neon.ascent.data.repository.JournalRepository
import com.neon.ascent.feature.biohacking.AiProvider
import com.neon.ascent.model.JournalEntry
import com.neon.ascent.model.Saying
import com.neon.ascent.data.AppSessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
class LoadingViewModel @Inject constructor(
    private val aiProvider: AiProvider,
    private val sayingsDao: SayingsDao,
    private val journalRepository: JournalRepository,
    private val appSessionManager: AppSessionManager
) : ViewModel() {
    val activeAiType = aiProvider.activeAiType
    val isAppLoaded = appSessionManager.isAppLoaded

    private val _randomSaying = MutableStateFlow<Saying?>(null)
    val randomSaying: StateFlow<Saying?> = _randomSaying.asStateFlow()

    private val _isSaved = MutableStateFlow(false)
    val isSaved: StateFlow<Boolean> = _isSaved.asStateFlow()

    init {
        loadRandomSaying()
    }

    fun initializeAi() {
        viewModelScope.launch {
            aiProvider.initialize()
            appSessionManager.markAppLoaded()
        }
    }

    private fun loadRandomSaying() {
        viewModelScope.launch {
            val sayings = sayingsDao.getAllSayings().first()
            if (sayings.isNotEmpty()) {
                val selected = sayings[Random.nextInt(sayings.size)]
                _randomSaying.value = selected
                _isSaved.value = journalRepository.isAlreadySaved(selected.id)
            }
        }
    }

    fun saveToJournal() {
        val saying = _randomSaying.value ?: return
        viewModelScope.launch {
            journalRepository.saveToJournal(
                JournalEntry(
                    id = saying.id,
                    text = saying.text,
                    category = saying.category
                )
            )
            _isSaved.value = true
        }
    }
}
