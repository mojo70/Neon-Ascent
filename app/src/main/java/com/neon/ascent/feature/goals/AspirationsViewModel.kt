package com.neon.ascent.feature.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neon.ascent.data.repository.UserStoryRepository
import com.neon.ascent.data.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AspirationsViewModel @Inject constructor(
    private val userStoryRepository: UserStoryRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _aspirations = MutableStateFlow<List<String>>(emptyList())
    val aspirations: StateFlow<List<String>> = _aspirations.asStateFlow()

    val yearlyReviewEnabled = userPreferencesRepository.isYearlyReviewEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    init {
        viewModelScope.launch {
            userStoryRepository.getMainStory().collect { story ->
                _aspirations.value = story.grandAspirations
            }
        }
    }

    fun addAspiration(aspiration: String) {
        if (aspiration.isNotBlank() && !_aspirations.value.contains(aspiration)) {
            val newList = _aspirations.value + aspiration
            updateAspirations(newList)
        }
    }

    fun removeAspiration(aspiration: String) {
        val newList = _aspirations.value - aspiration
        updateAspirations(newList)
    }

    private fun updateAspirations(newList: List<String>) {
        viewModelScope.launch {
            userStoryRepository.updateAspirations(newList)
        }
    }

    fun toggleYearlyReview(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setYearlyReviewEnabled(enabled)
        }
    }
}
