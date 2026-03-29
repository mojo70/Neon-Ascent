package com.neon.ascent.feature.cyberdeck

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neon.ascent.feature.biohacking.AiProvider
import com.neon.ascent.feature.biohacking.AiType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class CyberdeckViewModel @Inject constructor(
    private val aiProvider: AiProvider
) : ViewModel() {
    val aiType: StateFlow<AiType> = aiProvider.activeAiType
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AiType.NONE)
}
