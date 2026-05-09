package com.neon.ascent.feature.terminal.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neon.ascent.core.domain.SpecialRepository
import com.neon.ascent.core.domain.model.SpecialAttribute
import com.neon.ascent.core.domain.model.SpecialType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class TerminalViewModel @Inject constructor(
    private val specialRepository: SpecialRepository
) : ViewModel() {

    val specialState: StateFlow<Map<SpecialType, SpecialAttribute>> = specialRepository.getAllSpecialAttributes()
        .map { attributes -> attributes.associateBy { it.type } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyMap())
}
