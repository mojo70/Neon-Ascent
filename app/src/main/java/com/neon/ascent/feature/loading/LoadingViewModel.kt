package com.neon.ascent.feature.loading

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neon.ascent.feature.biohacking.AiProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoadingViewModel @Inject constructor(
    private val aiProvider: AiProvider
) : ViewModel() {
    val activeAiType = aiProvider.activeAiType

    fun initializeAi() {
        viewModelScope.launch {
            aiProvider.initialize()
        }
    }
}
