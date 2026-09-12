package com.neon.ascent.feature.goals.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neon.ascent.core.domain.goals.models.AscensionDirective
import com.neon.ascent.core.domain.goals.models.DirectiveStatus
import com.neon.ascent.core.domain.model.SpecialType
import com.neon.ascent.core.domain.repository.AscensionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AspirationCreationViewModel @Inject constructor(
    private val ascensionRepository: AscensionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AspirationCreationUiState())
    val uiState: StateFlow<AspirationCreationUiState> = _uiState.asStateFlow()

    fun updateTitle(title: String) { _uiState.update { it.copy(title = title) } }
    fun updateDescription(desc: String) { _uiState.update { it.copy(description = desc) } }
    fun toggleTargetDate(enabled: Boolean) { _uiState.update { it.copy(hasTargetDate = enabled) } }
    fun updateLinkedAttributes(attrs: List<SpecialType>) { _uiState.update { it.copy(linkedAttributes = attrs) } }

    fun saveAspiration() {
        val state = _uiState.value
        if (!state.isValid) return

        val directive = AscensionDirective(
            id = UUID.randomUUID().toString(),
            title = state.title,
            description = state.description,
            targetEndDate = if (state.hasTargetDate) LocalDate.now().plusMonths(6) else null,
            linkedAttributes = state.linkedAttributes,
            status = DirectiveStatus.ACTIVE
        )

        viewModelScope.launch {
            ascensionRepository.insertDirective(directive)
        }
    }
}

data class AspirationCreationUiState(
    val title: String = "",
    val description: String = "",
    val hasTargetDate: Boolean = false,
    val targetDate: LocalDate? = null,
    val linkedAttributes: List<SpecialType> = emptyList()
) {
    val isValid: Boolean = title.length >= 5 && linkedAttributes.isNotEmpty()
}
