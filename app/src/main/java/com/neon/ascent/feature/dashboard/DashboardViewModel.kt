package com.neon.ascent.feature.dashboard

import android.annotation.SuppressLint
import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.neon.ascent.data.local.BiohackingDao
import com.neon.ascent.data.local.SayingsDao
import com.neon.ascent.data.repository.*
import com.neon.ascent.data.local.HabitMetricDao
import com.neon.ascent.domain.usecase.GenerateCyberLoreUseCase
import com.neon.ascent.domain.usecase.GenerateDailyTasksUseCase
import com.neon.ascent.domain.usecase.SuggestGoalsUseCase
import com.neon.ascent.feature.biohacking.AiProvider
import com.neon.ascent.core.ai.AiPersona
import com.neon.ascent.model.BiohackingData
import com.neon.ascent.model.Saying
import com.neon.ascent.model.UserCharacter
import com.neon.ascent.core.domain.goals.models.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.Locale
import javax.inject.Inject
import kotlin.random.Random

data class WeatherState(
    val isRaining: Boolean = false,
    val isNight: Boolean = false,
    val temperature: Int = 22,
    val unitSymbol: String = if (Locale.getDefault().country == "US") "F" else "C"
)

data class HealthState(
    val steps: Long = 0,
    val heartRate: Int = 0,
    val vo2Max: Double = 0.0,
    val bodyBattery: Int = 75, // Default/Simulated
    val stressLevel: Int = 20, // Default/Simulated
    val isConnected: Boolean = false,
    val lastSyncTimestamp: Long? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val characterRepository: CharacterRepository,
    private val biohackingDao: BiohackingDao,
    private val sayingsDao: SayingsDao,
    private val weatherRepository: WeatherRepository,
    private val healthRepository: HealthRepository,
    private val settingsRepository: SettingsRepository,
    private val benchmarkRepository: BenchmarkRepository,
    private val userStoryRepository: UserStoryRepository,
    private val goalRepository: GoalRepository,
    private val taskRepository: TaskRepository,
    private val habitMetricDao: HabitMetricDao,
    private val ascensionRepository: com.neon.ascent.core.domain.repository.AscensionRepository,
    private val suggestGoalsUseCase: SuggestGoalsUseCase,
    private val generateDailyTasksUseCase: GenerateDailyTasksUseCase,
    private val generateCyberLoreUseCase: GenerateCyberLoreUseCase,
    private val bioAgePredictor: BioAgePredictor,
    private val fusedLocationClient: FusedLocationProviderClient,
    private val aiProvider: AiProvider,
    private val dopamineCoordinator: com.neon.ascent.core.common.DopamineCoordinator,
    private val identityCoordinator: com.neon.ascent.core.common.IdentityCoordinator,
    private val specialRepository: com.neon.ascent.core.domain.SpecialRepository
) : ViewModel() {
    val userCharacter: StateFlow<UserCharacter?> = characterRepository.getUserCharacter()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val biohackingData: StateFlow<BiohackingData?> = biohackingDao.getBiohackingData(0)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val _weatherState = MutableStateFlow(WeatherState())
    val weatherState: StateFlow<WeatherState> = _weatherState.asStateFlow()

    private val _healthState = MutableStateFlow(HealthState())
    val healthState: StateFlow<HealthState> = _healthState.asStateFlow()

    private val _systemAdvice = MutableStateFlow("NEURAL_LINK_ESTABLISHED. SCANNING_SYSTEM...")
    val systemAdvice: StateFlow<String> = _systemAdvice.asStateFlow()

    private val _snapshotSaying = MutableStateFlow("STAY_CHROME")
    val snapshotSaying: StateFlow<String> = _snapshotSaying.asStateFlow()

    val allSayings: StateFlow<List<Saying>> = sayingsDao.getAllSayings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tickerMessages: StateFlow<List<String>> = combine(userCharacter, _weatherState) { character, weather ->
        generateTickerMessages(character, weather)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isNetrunnerMode: StateFlow<Boolean> = settingsRepository.isNetrunnerMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isReligionShortcutEnabled: StateFlow<Boolean> = settingsRepository.isReligionShortcutEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _dopamineEvent = MutableStateFlow<com.neon.ascent.core.common.DopamineEvent?>(null)

    init {
        seedSayingsIfEmpty()
        updateAtmosphereSimulated()
        fetchRealWeather()
        refreshHealthData()
        loadDashboard()

        viewModelScope.launch {
            dopamineCoordinator.events.collect { event ->
                _dopamineEvent.value = event
            }
        }
        
        // Generate initial advice once we have some data context
        viewModelScope.launch {
            delay(1000) // Give a moment for initial repo fetches
            generateSystemAdvice()
            refreshSnapshotSaying()
        }

        viewModelScope.launch {
            benchmarkRepository.populateBenchmarksFromCsv()
        }
    }

    private fun loadDashboard() {
        viewModelScope.launch {
            combine(
                userStoryRepository.getMainStory(),
                ascensionRepository.getAllDirectives(),
                ascensionRepository.getActiveMissions(),
                ascensionRepository.getAllRecurringTasks(),
                bioAgePredictor.lastResultFlow,
                habitMetricDao.getTotalCompletedDays(),
                _dopamineEvent,
                specialRepository.getAllSpecialAttributes(),
                identityCoordinator.identity,
                ascensionRepository.getCompletionsInRange(java.time.Instant.now().minus(24, java.time.temporal.ChronoUnit.HOURS))
            ) { array ->
                val story = array[0] as com.neon.ascent.domain.model.UserStory
                val directives = array[1] as List<AscensionDirective>
                val missions = array[2] as List<AscensionMission>
                val dailyTasks = array[3] as List<AscensionTask>
                val bioAge = array[4] as com.neon.ascent.model.BioAgeResult?
                val totalCompletedDays = array[5] as Int
                val dopamine = array[6] as com.neon.ascent.core.common.DopamineEvent?
                val specialAttrs = array[7] as List<com.neon.ascent.core.domain.model.SpecialAttribute>
                val identity = array[8] as com.neon.ascent.core.common.OperatorIdentity
                val recentCompletions = array[9] as List<AscensionTaskCompletion>

                // Determine top attribute for identity resonance
                val topAttr = specialAttrs.maxByOrNull { it.currentValue }?.type?.name
                identityCoordinator.updateIdentity(topAttr, totalCompletedDays)

                // Map completions to titles for micro-logs
                val recentLogMessages = recentCompletions.mapNotNull { completion ->
                    dailyTasks.find { it.id == completion.taskId }?.title?.let { "SYNC_SUCCESS // $it" }
                }.take(5)

                val lore = if (story.cyberLore.isNotBlank()) {
                    story.cyberLore
                } else if (story.bio.isNotBlank()) {
                    "Your cyber lore is being synthesized..."
                } else {
                    "Your cyber lore is still being written..."
                }

                DashboardUiState(
                    userStory = story,
                    cyberLoreSnippet = lore.take(180) + if (lore.length > 180) "..." else "",
                    activeDirectives = directives,
                    activeMissions = missions,
                    todayTasks = dailyTasks,
                    terminalFeed = emptyList(), // Now handled by BiohackingViewModel
                    bioAgeResult = bioAge,
                    totalHabitDays = totalCompletedDays,
                    isLoading = false,
                    dopamineEvent = dopamine,
                    identity = identity,
                    recentLogMessages = recentLogMessages.ifEmpty { listOf("NEURAL_LINK_STABLE", "SYNC_RATIO: 98.4%") }
                )
            }.collect { _uiState.value = it }
        }
    }

    fun suggestNewGoals() {
        viewModelScope.launch {
            val newGoals = suggestGoalsUseCase.suggestGoals()
            newGoals.forEach { goalRepository.createGoal(it) }
        }
    }

    fun generateTodaysTasks() {
        viewModelScope.launch {
            val tasks = generateDailyTasksUseCase.generateTodaysTasks()
            tasks.forEach { taskRepository.createTask(it) }
        }
    }

    fun markTaskCompleted(taskId: String) {
        viewModelScope.launch {
            val task = uiState.value.todayTasks.find { it.id == taskId }
            if (task != null) {
                ascensionRepository.completeTask(task, null, null, null)
                
                // Trigger Dopamine Menu
                if (task.type == AscensionTaskType.RECURRING) {
                    dopamineCoordinator.triggerSync(xp = task.xpValue)
                } else {
                    dopamineCoordinator.triggerSubtle(xp = task.xpValue)
                }
            }
        }
    }

    fun clearDopamineEvent() {
        _dopamineEvent.value = null
    }

    private fun seedSayingsIfEmpty() {
        viewModelScope.launch {
            val count = sayingsDao.getAllSayings().first().size
            if (count == 0) {
                val initialSayings = listOf(
                    Saying("s1", "In Night City's neon haze, know thyself before the corps rewrite your code.", "Self & Identity", 85),
                    Saying("s2", "The unexamined implant is not worth jacking in.", "Self & Identity", 92),
                    Saying("s3", "Chrome your body, but guard the analog heart.", "Self & Identity", 78),
                    Saying("s4", "I know that I know nothing—except how to ghost this ICE.", "Self & Identity", 88),
                    Saying("s5", "The reflection in your Kiroshi eyes holds more truth than the ad-screens.", "Self & Identity", 81),
                    Saying("c1", "The corpo gods demand sacrifice; the street offers redemption in rain.", "Corporate Shadows", 90),
                    Saying("c2", "Megacorps build towers; the wise build bridges in the net.", "Corporate Shadows", 82),
                    Saying("d1", "Neon enlightenment: see the light, then hack the source.", "Digital Enlightenment", 93),
                    Saying("d2", "The matrix is a cave; the wise unplug to see the forms.", "Digital Enlightenment", 95),
                    Saying("sw1", "Break on through the firewall, where the real shadows play.", "Street Wisdom", 84),
                    Saying("sw5", "Morrison howls from the beyond: the future's uncertain, but the end is always near—in pixels.", "Street Wisdom", 90),
                    Saying("sm1", "What does it profit a man to gain the whole net, yet forfeit his ghost?", "Soul in the Machine", 94),
                    Saying("r1", "Love your fellow choom as yourself, but always run a trace.", "Rebellion & Freedom", 75),
                    Saying("t1", "The sky above the port was the color of television, tuned to a dead channel.", "Truth & Illusion", 98),
                    Saying("np1", "In the sprawl, every runner is a parable waiting to be decrypted.", "Neon Parables", 73)
                )
                sayingsDao.insertSayings(initialSayings)
            }
        }
    }

    private fun generateTickerMessages(character: UserCharacter?, weather: WeatherState): List<String> {
        val messages = mutableListOf<String>()
        character?.let {
            messages.add("SUBJECT: ${it.name.uppercase()} // ARCHETYPE: ${it.archetype ?: "UNKNOWN"}")
            messages.add("LEVEL ${it.level} OPERATIVE DETECTED IN SECTOR 7")
            if (it.mbti != null) messages.add("NEURAL_PATTERN: ${it.mbti} // SYNC_RATIO: 98.4%")
            if (it.alignment != null) messages.add("MORAL_ALIGNMENT: ${it.alignment.uppercase()}")
        }
        val weatherStatus = if (weather.isRaining) "ACID_RAIN_WARNING" else "ATMOSPHERE_STABLE"
        messages.add("LOCAL_CONDITIONS: ${weather.temperature}°${weather.unitSymbol} // $weatherStatus")
        messages.add("MARKET_TICKER: \$SOL +4.2% // \$ETH -1.5% // \$EURODOLLAR STABLE")
        messages.add("MISSION_LOG: 'NEURAL_BREACH' SUCCESSFUL // REWARD: 5000 ED")
        messages.add("ALERT: ARASAKA_SECURITY_LEVEL_INCREASED_IN_WATSON")
        messages.add("STOCK: KANGA_BIOTECH (KBT) UP 12% AFTER NEURAL_LINK_BREAKTHROUGH")
        return messages
    }

    private fun generateSystemAdvice() {
        viewModelScope.launch {
            val steps = healthState.value.steps
            val hr = healthState.value.heartRate
            val weather = if (weatherState.value.isRaining) "Acid Rain" else "Clear"
            val archetype = userCharacter.value?.archetype ?: "Unknown"

            val context = """
                - STEPS: $steps
                - HR: $hr
                - ATMOSPHERE: $weather
                - ARCHETYPE: $archetype
            """.trimIndent()
            
            val prompt = AiPersona.getSocratesPrompt(context) + "\nTask: Generate one short, cryptic advice (max 12 words). OUTPUT:"
            
            val result = aiProvider.generateContent(prompt)
            if (result.startsWith("ERROR:")) {
                _systemAdvice.value = getRandomSayingFromDb()
            } else {
                // Defensive cleaning to handle small model hallucinations or prompt echoing
                val cleaned = result
                    .substringAfter("OUTPUT:")
                    .substringBefore("\n")
                    .replace("\"", "")
                    .trim()
                
                if (cleaned.isNotEmpty() && !cleaned.contains("STEPS:") && !cleaned.contains("HR:")) {
                    _systemAdvice.value = cleaned
                } else {
                    // Fallback to first line if tags weren't followed perfectly
                    val firstLine = result.split("\n").firstOrNull { it.isNotBlank() && !it.contains(":") }
                    _systemAdvice.value = firstLine?.trim() ?: getRandomSayingFromDb()
                }
            }
        }
    }

    fun refreshSnapshotSaying(flavor: String? = null) {
        viewModelScope.launch {
            val archetype = userCharacter.value?.archetype ?: "Unknown"
            val prompt = """
                Generate a short, cool cyberpunk saying (max 10 words) for a character snapshot.
                Flavor: ${flavor ?: "RANDOM_STREET_WISDOM"}
                Archetype: $archetype
                OUTPUT:
            """.trimIndent()
            
            val result = aiProvider.generateContent(prompt)
            if (result.startsWith("ERROR:")) {
                _snapshotSaying.value = getRandomSayingFromDb()
            } else {
                val cleaned = result
                    .substringAfter("OUTPUT:")
                    .substringBefore("\n")
                    .replace("\"", "")
                    .trim()
                
                _snapshotSaying.value = if (cleaned.isNotEmpty()) cleaned else result.split("\n").first().trim()
            }
        }
    }

    private suspend fun getRandomSayingFromDb(): String {
        val sayings = sayingsDao.getAllSayings().first()
        return if (sayings.isNotEmpty()) {
            val total = sayings.sumOf { it.engagementScore }
            var rand = Random.nextInt(total + 1)
            var selected = sayings.first().text
            for (saying in sayings) {
                rand -= saying.engagementScore
                if (rand <= 0) {
                    selected = saying.text
                    break
                }
            }
            selected
        } else {
            "NEURAL_LINK_STABLE // STAY_VIGILANT"
        }
    }

    @SuppressLint("MissingPermission")
    fun fetchRealWeather() {
        viewModelScope.launch {
            try {
                val location: Location? = fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null).await()
                if (location != null) {
                    val realWeather = weatherRepository.getWeatherData(location.latitude, location.longitude)
                    _weatherState.value = realWeather
                } else {
                    updateAtmosphereSimulated()
                }
            } catch (e: Exception) {
                // If real fetch fails, ensure we have at least simulated data that respects units
                updateAtmosphereSimulated()
            }
        }
    }

    fun refreshHealthData() {
        viewModelScope.launch {
            val hasPermissions = healthRepository.hasAllPermissions()
            if (hasPermissions) {
                val steps = healthRepository.getTodaySteps()
                val hr = healthRepository.getLatestHeartRate()
                val vo2 = healthRepository.getLatestVo2Max()
                val simulatedBattery = (80 - (LocalTime.now().hour * 2)).coerceIn(10, 100)
                val simulatedStress = (hr / 4).coerceIn(5, 95)

                _healthState.value = HealthState(
                    steps = steps,
                    heartRate = hr,
                    vo2Max = vo2,
                    bodyBattery = simulatedBattery,
                    stressLevel = simulatedStress,
                    isConnected = true,
                    lastSyncTimestamp = System.currentTimeMillis()
                )
                
                biohackingData.value?.let { current ->
                    biohackingDao.insertOrUpdate(current.copy(
                        isWearableSynced = true,
                        lastSyncTimestamp = System.currentTimeMillis()
                    ))
                } ?: run {
                    biohackingDao.insertOrUpdate(BiohackingData(
                        userId = 0,
                        isWearableSynced = true,
                        lastSyncTimestamp = System.currentTimeMillis()
                    ))
                }
            } else {
                _healthState.value = HealthState(isConnected = false)
            }
        }
    }

    private fun updateAtmosphereSimulated() {
        val now = LocalTime.now()
        val isNight = now.hour < 6 || now.hour > 19
        val useFahrenheit = Locale.getDefault().country == "US"
        
        // Provide more realistic defaults if fetch fails or hasn't run yet
        _weatherState.value = WeatherState(
            isRaining = false,
            isNight = isNight,
            temperature = if (useFahrenheit) {
                if (isNight) 65 else 78
            } else {
                if (isNight) 18 else 26
            }
        )
    }

    fun updateNetrunnerName(newName: String) {
        viewModelScope.launch {
            userCharacter.value?.let {
                characterRepository.updateCharacter(it.copy(netrunnerName = newName))
            }
        }
    }

    fun updateTerminalInput(text: String) {
        _uiState.update { it.copy(terminalInput = text) }
    }

    fun sendTerminalMessage() {
        val input = _uiState.value.terminalInput
        if (input.isBlank()) return

        viewModelScope.launch {
            val userMsg = TerminalMessage(input, isFromUser = true)
            _uiState.update { state ->
                state.copy(
                    terminalMessages = state.terminalMessages + userMsg,
                    terminalInput = ""
                )
            }

            val context = "Conversation history: " + _uiState.value.terminalMessages.takeLast(5).joinToString { if(it.isFromUser) "Runner: ${it.text}" else "CYBR-TES: ${it.text}" }
            val prompt = AiPersona.getSocratesPrompt(context) + "\nRunner: $input\nRespond as CYBR-TES."
            
            val response = aiProvider.generateContent(prompt)
            val aiMsg = TerminalMessage(response.substringAfter("CYBR-TES:").trim(), isFromUser = false)
            
            _uiState.update { state ->
                state.copy(terminalMessages = state.terminalMessages + aiMsg)
            }
        }
    }

    fun updateChessElo(newElo: Int) {
        viewModelScope.launch {
            characterRepository.updateChessElo(newElo)
        }
    }
}
