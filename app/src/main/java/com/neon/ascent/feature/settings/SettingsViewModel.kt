package com.neon.ascent.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neon.ascent.data.local.UserCharacterDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userCharacterDao: UserCharacterDao
) : ViewModel() {

    fun resetProfile(onComplete: () -> Unit) {
        viewModelScope.launch {
            userCharacterDao.resetCharacter()
            onComplete()
        }
    }
}
