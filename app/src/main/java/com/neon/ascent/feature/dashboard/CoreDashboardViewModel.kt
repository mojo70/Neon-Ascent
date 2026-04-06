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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

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
    val isNetrunnerMode = settingsRepository.isNetrunnerMode

    val allSayings: StateFlow<List<Saying>> = sayingsDao.getAllSayings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _hackHistory = MutableStateFlow<List<HackEvent>>(emptyList())
    val hackHistory = _hackHistory.asStateFlow()

    private val _aiCoreSessionUnlocked = MutableStateFlow(false)
    val aiCoreSessionUnlocked = _aiCoreSessionUnlocked.asStateFlow()

    private val _databankSessionUnlocked = MutableStateFlow(false)
    val databankSessionUnlocked = _databankSessionUnlocked.asStateFlow()

    // ICE Regeneration Progress (0.0 to 1.0)
    private val _aiCoreIceRegen = MutableStateFlow(0f)
    val aiCoreIceRegen = _aiCoreIceRegen.asStateFlow()

    private val _databankIceRegen = MutableStateFlow(0f)
    val databankIceRegen = _databankIceRegen.asStateFlow()

    private var aiCoreRegenJob: Job? = null
    private var databankRegenJob: Job? = null

    private val REGEN_TIME_MS = 3 * 60 * 1000L // 3 minutes

    init {
        checkFirstEntry()
        generateDummyLogs()
    }

    private fun checkFirstEntry() {
        viewModelScope.launch {
            if (settingsRepository.isFirstAiCoreEntry.value) {
                // Fetch random saying from "Soul in the Machine"
                val soulSayings = sayingsDao.getSayingsByCategory("Soul in the Machine")
                val randomSaying = if (soulSayings.isNotEmpty()) {
                    soulSayings[Random.nextInt(soulSayings.size)].text
                } else {
                    "Ghost in the shell detected."
                }

                val welcomeLog = HackEvent(
                    type = "WELCOME_INITIALIZATION",
                    details = "Neural Core Online. Fragment found: \"$randomSaying\"",
                    timestamp = System.currentTimeMillis(),
                    bounty = 0
                )
                
                _hackHistory.value = listOf(welcomeLog) + _hackHistory.value
                settingsRepository.setFirstAiCoreEntry(false)
            }
        }
    }

    private fun generateDummyLogs() {
        val existing = _hackHistory.value
        _hackHistory.value = existing + listOf(
            HackEvent("CORE_ACCESS_ATTEMPT", "IP: 192.168.1.42", System.currentTimeMillis() - 15 * 60000, 50),
            HackEvent("PACKET_SNIFF_DETECTED", "PORT: 8080", System.currentTimeMillis() - 45 * 60000, 120),
            HackEvent("MALWARE_INJECTION", "TRACED: NightCity_Subnet", System.currentTimeMillis() - 75 * 60000, 300)
        )
    }

    fun updateNanoTemperature(temp: Float) = settingsRepository.setNanoTemperature(temp)
    fun updateCloudFallback(threshold: Float) = settingsRepository.setCloudFallbackThreshold(threshold)
    fun updatePhilosophySeed(seed: String) = settingsRepository.setPhilosophySeed(seed)
    fun toggleNetrunnerMode(enabled: Boolean) = settingsRepository.setNetrunnerMode(enabled)

    fun addCustomSaying(text: String) {
        viewModelScope.launch {
            val id = "custom_" + System.currentTimeMillis()
            sayingsDao.insertSaying(Saying(id, text, "Custom", 100))
        }
    }

    fun toggleSayingEnabled(saying: Saying) {
        viewModelScope.launch {
            sayingsDao.insertSaying(saying.copy(isEnabled = !saying.isEnabled))
        }
    }

    fun deleteSaying(saying: Saying) {
        viewModelScope.launch {
            if (saying.category == "Custom") {
                sayingsDao.deleteSaying(saying)
            }
        }
    }

    fun seedNano() {
        viewModelScope.launch {
            val sayings = allSayings.value.filter { it.isEnabled }.take(10).joinToString("\n") { it.text }
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

    fun claimBounty(event: HackEvent) {
        viewModelScope.launch {
            userCharacter.value?.let { char ->
                userCharacterDao.updateUserCharacter(char.copy(eddies = char.eddies + event.bounty))
                // Remove from history after claiming
                _hackHistory.value = _hackHistory.value.filter { it != event }
            }
        }
    }

    fun quickUnlock(target: String) {
        viewModelScope.launch {
            val char = userCharacter.value ?: return@launch
            if (char.eddies >= 20) {
                userCharacterDao.updateEddies(char.eddies - 20)
                sessionUnlock(target)
            }
        }
    }

    fun sessionUnlock(target: String) {
        if (target == "AI_CORE") {
            _aiCoreSessionUnlocked.value = true
            startRegen("AI_CORE")
        }
        if (target == "DATABANK") {
            _databankSessionUnlocked.value = true
            startRegen("DATABANK")
        }
    }

    private fun startRegen(target: String) {
        if (target == "AI_CORE") {
            aiCoreRegenJob?.cancel()
            aiCoreRegenJob = viewModelScope.launch {
                val startTime = System.currentTimeMillis()
                while (System.currentTimeMillis() - startTime < REGEN_TIME_MS) {
                    val progress = (System.currentTimeMillis() - startTime).toFloat() / REGEN_TIME_MS
                    _aiCoreIceRegen.value = progress
                    delay(1000)
                }
                _aiCoreIceRegen.value = 1f
            }
        } else if (target == "DATABANK") {
            databankRegenJob?.cancel()
            databankRegenJob = viewModelScope.launch {
                val startTime = System.currentTimeMillis()
                while (System.currentTimeMillis() - startTime < REGEN_TIME_MS) {
                    val progress = (System.currentTimeMillis() - startTime).toFloat() / REGEN_TIME_MS
                    _databankIceRegen.value = progress
                    delay(1000)
                }
                _databankIceRegen.value = 1f
            }
        }
    }

    fun checkIce(currentTab: String) {
        if (currentTab != "AI_CORE" && _aiCoreIceRegen.value >= 1f) {
            _aiCoreSessionUnlocked.value = false
            _aiCoreIceRegen.value = 0f
        }
        if (currentTab != "DATABANK" && _databankIceRegen.value >= 1f) {
            _databankSessionUnlocked.value = false
            _databankIceRegen.value = 0f
        }
    }

    fun transferToSecure(amount: Int) {
        viewModelScope.launch {
            val char = userCharacter.value ?: return@launch
            if (amount > 0 && char.eddies >= amount) {
                userCharacterDao.updateUserCharacter(
                    char.copy(
                        eddies = char.eddies - amount,
                        secureEddies = char.secureEddies + amount
                    )
                )
            }
        }
    }
}

data class HackEvent(
    val type: String,
    val details: String,
    val timestamp: Long,
    val bounty: Int
)
