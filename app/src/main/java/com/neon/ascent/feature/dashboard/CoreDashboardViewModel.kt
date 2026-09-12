package com.neon.ascent.feature.dashboard

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neon.ascent.core.domain.character.models.UserCharacter
import com.neon.ascent.core.domain.character.repository.CharacterRepository
import com.neon.ascent.data.local.JournalDao
import com.neon.ascent.data.repository.SayingsRepository
import com.neon.ascent.data.repository.SettingsRepository
import com.neon.ascent.feature.biohacking.AiProvider
import com.neon.ascent.core.data.local.dao.NeuralMemoryDao
import com.neon.ascent.core.data.local.entity.NeuralMemory
import com.neon.ascent.feature.notifications.data.SmartPingScheduler
import com.neon.ascent.model.Saying
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
class CoreDashboardViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val characterRepository: CharacterRepository,
    private val sayingsRepository: SayingsRepository,
    private val journalDao: JournalDao,
    private val settingsRepository: SettingsRepository,
    private val aiProvider: AiProvider,
    private val neuralMemoryDao: NeuralMemoryDao,
    private val notificationScheduler: SmartPingScheduler
) : ViewModel() {

    val userCharacter: StateFlow<UserCharacter?> = characterRepository.getUserCharacter()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val neuralMemories: StateFlow<List<NeuralMemory>> = neuralMemoryDao.getAllMemories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val nanoTemperature = settingsRepository.nanoTemperature
    val cloudFallbackThreshold = settingsRepository.cloudFallbackThreshold
    val philosophySeed = settingsRepository.philosophySeed
    val isNetrunnerMode = settingsRepository.isNetrunnerMode

    val allSayings: StateFlow<List<Saying>> = sayingsRepository.getAllSayings()
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

    private val _firstEntryMessage = MutableStateFlow<String?>(null)
    val firstEntryMessage = _firstEntryMessage.asStateFlow()

    private var aiCoreRegenJob: Job? = null
    private var databankRegenJob: Job? = null

    private val REGEN_TIME_MS = 3 * 60 * 1000L // 3 minutes

    init {
        checkFirstEntry()
        generateDummyLogs()
    }

    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /**
     * DEBUG: Triggers the Neural Brief immediately for testing.
     */
    fun debugTriggerNeuralBrief() {
        viewModelScope.launch {
            notificationScheduler.enqueueDailyNeuralBrief(isTestRequest = true)
        }
    }

    private fun checkFirstEntry() {
        viewModelScope.launch {
            sayingsRepository.syncUserSayingsFromDatabase()

            delay(300) // Brief pause to allow UI to settle

            if (settingsRepository.isFirstAiCoreEntry.value) {
                // Fetch random saying from "Soul in the Machine"
                val soulSayings = sayingsRepository.getSayingsByCategory("Soul in the Machine").first()
                val randomSaying = if (soulSayings.isNotEmpty()) {
                    soulSayings[Random.nextInt(soulSayings.size)].text
                } else {
                    "Ghost in the shell detected."
                }

                val messageText = "Neural Core Online. Fragment found: \"$randomSaying\""
                
                val welcomeLog = HackEvent(
                    type = "WELCOME_INITIALIZATION",
                    details = messageText,
                    timestamp = System.currentTimeMillis(),
                    bounty = 0
                )
                
                _hackHistory.value = listOf(welcomeLog) + _hackHistory.value
                _firstEntryMessage.value = messageText
                
                settingsRepository.setFirstAiCoreEntry(false)
            }
        }
    }

    fun dismissFirstEntryMessage() {
        _firstEntryMessage.value = null
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
            sayingsRepository.addCustomSaying(text)
        }
    }

    fun toggleSayingEnabled(saying: Saying) {
        viewModelScope.launch {
            sayingsRepository.toggleSayingEnabled(saying)
        }
    }

    fun deleteSaying(saying: Saying) {
        viewModelScope.launch {
            if (saying.category == "Custom") {
                sayingsRepository.deleteSaying(saying)
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
                characterRepository.saveCharacter(char.copy(eddies = char.eddies + event.bounty))
                // Remove from history after claiming
                _hackHistory.value = _hackHistory.value.filter { it != event }
            }
        }
    }

    fun quickUnlock(target: String) {
        viewModelScope.launch {
            val char = userCharacter.value ?: return@launch
            if (char.eddies >= 20) {
                characterRepository.saveCharacter(char.copy(eddies = char.eddies - 20))
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
                characterRepository.saveCharacter(
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
