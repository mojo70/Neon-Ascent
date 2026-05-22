package com.neon.ascent.feature.goals.ui.ascension

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neon.ascent.core.domain.goals.models.*
import com.neon.ascent.core.domain.repository.AscensionRepository
import com.neon.ascent.feature.goals.domain.usecases.NeonMentorUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AscensionReviewUiState(
    val directive: AscensionDirective? = null,
    val reviewText: String = "INITIALIZING_DIALECTIC_ANALYSIS...",
    val isLoading: Boolean = true
)

@HiltViewModel
class AscensionReviewViewModel @Inject constructor(
    private val repository: AscensionRepository,
    private val mentorUseCase: NeonMentorUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AscensionReviewUiState())
    val uiState = _uiState.asStateFlow()

    fun loadReview(directiveId: String) {
        viewModelScope.launch {
            val directive = repository.getAllDirectives().first().find { it.id == directiveId } ?: return@launch
            val missions = repository.getMissionsForDirective(directiveId).first()
            val tasks = repository.getTasksForParent(directiveId).first() // Simplified for now
            
            _uiState.update { it.copy(directive = directive, isLoading = true) }
            
            val review = mentorUseCase.getReview(directive, missions, tasks)
            
            _uiState.update { it.copy(reviewText = review, isLoading = false) }
        }
    }
}
