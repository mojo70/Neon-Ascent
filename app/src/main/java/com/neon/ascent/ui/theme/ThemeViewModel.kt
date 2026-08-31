package com.neon.ascent.ui.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neon.ascent.data.repository.UserPreferencesRepository
import com.neon.ascent.core.common.VisualMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val visualMode: StateFlow<VisualMode> = userPreferencesRepository.themeMode
        .map { mode: String ->
            try {
                VisualMode.valueOf(mode.uppercase())
            } catch (e: Exception) {
                VisualMode.CYBER
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = VisualMode.CYBER
        )

    val neonIntensity: StateFlow<Float> = userPreferencesRepository.neonIntensity
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.8f
        )
}
