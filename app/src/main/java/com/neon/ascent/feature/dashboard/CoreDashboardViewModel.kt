package com.neon.ascent.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neon.ascent.data.local.JournalDao
import com.neon.ascent.data.local.SayingsDao
import com.neon.ascent.data.local.UserCharacterDao
import com.neon.ascent.data.repository.SettingsRepository
import com.neon.ascent.feature.biohacking.AiProvider
import com.neon.ascent.model.Saying
import com.neon.ascent.model.UserCharacter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CoreDashboardViewModel @Inject constructor(
    private val userCharacterDao: UserCharacterDao,
    private val sayingsDao: SayingsDao,
    private val journalDao: JournalDao,
    private val settingsRepository: SettingsRepository,
    private val aiProvider: AiProvider
) : ViewModel() {

    val userCharacter: StateFlow<UserCharacter?> = userCharacterDao.getUserCharacter()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val nanoTemperature = settingsRepository.nanoTemperature
    val cloudFallbackThreshold = settingsRepository.cloudFallbackThreshold
    val philosophySeed = settingsRepository.philosophySeed

    val allSayings: StateFlow<List<Saying>> = sayingsDao.getAllSayings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateNanoTemperature(temp: Float) = settingsRepository.setNanoTemperature(temp)
    fun updateCloudFallback(threshold: Float) = settingsRepository.setCloudFallbackThreshold(threshold)
    fun updatePhilosophySeed(seed: String) = settingsRepository.setPhilosophySeed(seed)

    fun addCustomSaying(text: String) {
        viewModelScope.launch {
            val id = "custom_" + System.currentTimeMillis()
            sayingsDao.insertSaying(Saying(id, text, "Custom", 100))
        }
    }

    fun deleteSaying(saying: Saying) {
        viewModelScope.launch {
            sayingsDao.deleteSaying(saying)
        }
    }

    fun seedNano() {
        viewModelScope.launch {
            val sayings = allSayings.value.take(10).joinToString("\n") { it.text }
            val entries = journalDao.getAllEntries().first().take(3).joinToString("\n") { it.text }
            val seed = philosophySeed.value
            
            val prompt = """
                SYSTEM_UPDATE: SEEDING_NANO_CORE
                PHILOSOPHY_MODE: $seed
                CONTEXT_DATA:
                $sayings
                $entries
                
                NANO_CORE_INITIALIZED. STANDBY.
            """.trimIndent()
            
            aiProvider.generateContent(prompt, forceLocal = true)
        }
    }
}
