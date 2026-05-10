package com.neon.ascent.feature.terminal.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neon.ascent.core.domain.model.CognitiveItem
import com.neon.ascent.core.domain.model.CognitiveTestSession
import com.neon.ascent.core.domain.special.CognitiveTestEngine
import com.neon.ascent.core.domain.special.usecases.UpdateSpecialFromCognitiveTestUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CognitiveTestViewModel @Inject constructor(
    private val engine: CognitiveTestEngine,
    private val updateUseCase: UpdateSpecialFromCognitiveTestUseCase
) : ViewModel() {

    private val _testState = MutableStateFlow<TestState>(TestState.NotStarted)
    val testState: StateFlow<TestState> = _testState.asStateFlow()

    private var currentSession: CognitiveTestSession? = null

    fun startTest() {
        currentSession = engine.startNewSession()
        loadNextQuestion()
    }

    private fun loadNextQuestion() {
        if (engine.shouldStop()) {
            finalizeTest()
            return
        }

        val nextItem = engine.selectNextItem()
        _testState.value = TestState.InProgress(
            currentItem = nextItem,
            progress = engine.responseCount.toFloat() / 20f
        )
    }

    fun recordAnswer(selectedAnswer: String) {
        val current = (_testState.value as? TestState.InProgress)?.currentItem ?: return
        val correct = selectedAnswer == current.correctAnswer

        engine.recordResponse(current, correct)
        loadNextQuestion()
    }

    private fun finalizeTest() {
        val session = engine.finalizeSession()
        currentSession = session
        _testState.value = TestState.Complete(session)

        // Update S.P.E.C.I.A.L.
        viewModelScope.launch {
            updateUseCase(session)
        }
    }
}

sealed class TestState {
    object NotStarted : TestState()
    data class InProgress(val currentItem: CognitiveItem, val progress: Float) : TestState()
    data class Complete(val session: CognitiveTestSession) : TestState()
}
