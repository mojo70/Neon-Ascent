package com.neon.ascent.feature.terminal.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neon.ascent.core.domain.SpecialRepository
import com.neon.ascent.core.domain.model.CognitiveTestSession
import com.neon.ascent.core.domain.model.SpecialAttribute
import com.neon.ascent.core.domain.model.SpecialType
import com.neon.ascent.core.domain.special.CognitiveTestEngine
import com.neon.ascent.core.domain.special.usecases.UpdateSpecialFromCognitiveTestUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DiagnosticsViewModel @Inject constructor(
    private val updateSpecialUseCase: UpdateSpecialFromCognitiveTestUseCase,
    private val cognitiveTestEngine: CognitiveTestEngine,
    private val specialRepository: SpecialRepository
) : ViewModel() {

    private val _specialState = MutableStateFlow<Map<SpecialType, SpecialAttribute>>(emptyMap())
    val specialState: StateFlow<Map<SpecialType, SpecialAttribute>> = _specialState.asStateFlow()

    private val _isRunningTest = MutableStateFlow(false)
    val isRunningTest = _isRunningTest.asStateFlow()

    private val _lastTestResult = MutableStateFlow<CognitiveTestSession?>(null)
    val lastTestResult = _lastTestResult.asStateFlow()

    init {
        viewModelScope.launch {
            specialRepository.getAllSpecialAttributes().collect { attributes ->
                _specialState.value = attributes.associateBy { it.type }
            }
        }
    }

    fun runCognitiveDiagnostic() {
        viewModelScope.launch {
            _isRunningTest.value = true
            try {
                val (session, _) = cognitiveTestEngine.runAdaptiveSession()
                // Update Intelligence + persist
                val updatedAttribute = updateSpecialUseCase(session)
                _lastTestResult.value = session
            } finally {
                _isRunningTest.value = false
            }
        }
    }
}
