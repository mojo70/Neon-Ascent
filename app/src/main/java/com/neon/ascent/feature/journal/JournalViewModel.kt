package com.neon.ascent.feature.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neon.ascent.data.repository.JournalRepository
import com.neon.ascent.model.JournalEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class JournalViewModel @Inject constructor(
    private val repository: JournalRepository
) : ViewModel() {

    val entries: StateFlow<List<JournalEntry>> = repository.allEntries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleHeart(entry: JournalEntry) {
        viewModelScope.launch {
            repository.toggleHeart(entry.id, !entry.isHearted)
        }
    }

    fun deleteEntry(entry: JournalEntry) {
        viewModelScope.launch {
            repository.removeFromJournal(entry)
        }
    }
}
