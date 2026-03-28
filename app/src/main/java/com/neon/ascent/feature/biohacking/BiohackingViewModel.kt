package com.neon.ascent.feature.biohacking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neon.ascent.data.local.BiohackingDao
import com.neon.ascent.data.local.UserCharacterDao
import com.neon.ascent.data.repository.HealthRepository
import com.neon.ascent.model.BioProtocolLog
import com.neon.ascent.model.BiohackingData
import com.neon.ascent.model.UserCharacter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BiohackingViewModel @Inject constructor(
    private val biohackingDao: BiohackingDao,
    private val userCharacterDao: UserCharacterDao,
    private val healthRepository: HealthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BiohackingData())
    val uiState: StateFlow<BiohackingData> = _uiState.asStateFlow()

    private val _character = MutableStateFlow<UserCharacter?>(null)
    val character: StateFlow<UserCharacter?> = _character.asStateFlow()

    private val _logs = MutableStateFlow<List<BioProtocolLog>>(emptyList())
    val logs: StateFlow<List<BioProtocolLog>> = _logs.asStateFlow()

    init {
        viewModelScope.launch {
            biohackingDao.getBiohackingData(0).collectLatest { data ->
                data?.let { _uiState.value = it }
            }
        }
        viewModelScope.launch {
            userCharacterDao.getUserCharacter().collectLatest { char ->
                _character.value = char
            }
        }
        viewModelScope.launch {
            biohackingDao.getProtocolLogs(0).collectLatest {
                _logs.value = it
            }
        }
    }

    fun updateData(update: (BiohackingData) -> BiohackingData) {
        val newData = update(_uiState.value)
        _uiState.value = newData
        viewModelScope.launch {
            biohackingDao.insertOrUpdate(newData)
        }
    }

    fun syncWearable() {
        viewModelScope.launch {
            if (healthRepository.hasAllPermissions()) {
                updateData { it.copy(
                    isWearableSynced = true, 
                    lastSyncTimestamp = System.currentTimeMillis()
                ) }
            }
        }
    }

    fun logProtocolEffectiveness(
        energy: Int,
        sleep: Int,
        mood: Int,
        focus: Int,
        sideEffects: String?,
        notes: String?,
        protocolId: String
    ) {
        viewModelScope.launch {
            val log = BioProtocolLog(
                userId = 0,
                energyScore = energy,
                sleepQuality = sleep,
                moodScore = mood,
                focusScore = focus,
                sideEffects = sideEffects,
                notes = notes,
                protocolId = protocolId
            )
            biohackingDao.insertProtocolLog(log)
        }
    }
}
