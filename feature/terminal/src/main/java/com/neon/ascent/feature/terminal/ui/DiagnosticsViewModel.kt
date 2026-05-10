package com.neon.ascent.feature.terminal.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neon.ascent.core.domain.SpecialRepository
import com.neon.ascent.core.domain.model.SpecialAttribute
import com.neon.ascent.core.domain.model.SpecialType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DiagnosticsViewModel @Inject constructor(
    private val specialRepository: SpecialRepository
) : ViewModel() {

    private val _specialState = MutableStateFlow<Map<SpecialType, SpecialAttribute>>(emptyMap())
    val specialState: StateFlow<Map<SpecialType, SpecialAttribute>> = _specialState.asStateFlow()

    init {
        viewModelScope.launch {
            specialRepository.getAllSpecialAttributes().collect { attributes ->
                _specialState.value = attributes.associateBy { it.type }
            }
        }
    }
}
