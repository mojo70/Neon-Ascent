package com.neon.ascent.feature.cyberdeck

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neon.ascent.data.local.InventoryDao
import com.neon.ascent.data.local.UserCharacterDao
import com.neon.ascent.feature.biohacking.AiProvider
import com.neon.ascent.feature.biohacking.AiType
import com.neon.ascent.model.*
import com.neon.ascent.util.PcapGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
class CyberdeckViewModel @Inject constructor(
    private val aiProvider: AiProvider,
    private val inventoryDao: InventoryDao,
    private val userCharacterDao: UserCharacterDao
) : ViewModel() {
    val aiType: StateFlow<AiType> = aiProvider.activeAiType
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AiType.NONE)

    private val _netWatchAlerts = MutableStateFlow<List<NetWatchAlert>>(emptyList())
    val netWatchAlerts: StateFlow<List<NetWatchAlert>> = _netWatchAlerts.asStateFlow()

    private val _externalFeeds = MutableStateFlow<List<String>>(emptyList())
    val combinedFeeds: StateFlow<List<String>> = combine(_netWatchAlerts, _externalFeeds) { alerts, external ->
        val alertStrings = alerts.map { "[${it.severity}] ${it.title}: ${it.description}" }
        (external + alertStrings).takeLast(15)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val pcapGenerator = PcapGenerator()

    private val _currentChallenge = MutableStateFlow<Challenge?>(null)
    val currentChallenge: StateFlow<Challenge?> = _currentChallenge.asStateFlow()

    private val _lastReward = MutableStateFlow<HackingReward?>(null)
    val lastReward: StateFlow<HackingReward?> = _lastReward.asStateFlow()

    val components = inventoryDao.getComponents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isCraftingAvailable: StateFlow<Boolean> = components.map { list ->
        list.any { it.quantity >= 10 }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val userCharacter = userCharacterDao.getUserCharacter()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val quickHacks = inventoryDao.getQuickHacks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val equippedQuickHacks: StateFlow<List<QuickHack>> = combine(userCharacter, quickHacks) { user, hacks ->
        val equippedIds = user?.getQuickhackList() ?: emptyList()
        hacks.filter { it.id in equippedIds }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val inventoryQuickHacks: StateFlow<List<QuickHack>> = combine(userCharacter, quickHacks) { user, hacks ->
        val equippedIds = user?.getQuickhackList() ?: emptyList()
        hacks.filter { it.id !in equippedIds }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun useQuickHack(quickHack: QuickHack) {
        viewModelScope.launch {
            inventoryDao.insertQuickHack(quickHack.copy(lastUsedTimestamp = System.currentTimeMillis()))
        }
    }

    init {
        generateAlerts()
    }

    fun startPcapChallenge(tier: DifficultyTier, skill: SkillType) {
        viewModelScope.launch {
            _currentChallenge.value = pcapGenerator.generate(tier, skill)
            _lastReward.value = null
        }
    }

    fun submitFlag(flag: String) {
        val challenge = _currentChallenge.value ?: return
        if (flag.trim().equals(challenge.correctFlag, ignoreCase = true)) {
            processWin(challenge)
        } else {
            // Handle wrong flag
        }
    }

    fun clearReward() {
        _lastReward.value = null
    }

    private fun processWin(challenge: Challenge) {
        viewModelScope.launch {
            val reward = calculateRewards(challenge)
            
            // Update User Stats
            val char = userCharacterDao.getUserCharacter().first()
            char?.let { 
                userCharacterDao.updateUserCharacter(it.copy(
                    experience = it.experience + reward.xp,
                    eddies = it.eddies + reward.eddies
                ))
            }

            // Update Inventory
            reward.components.forEach { (rarity, amount) ->
                inventoryDao.addComponents(rarity, amount)
            }

            _lastReward.value = reward
            _currentChallenge.value = null

            // AI Feedback
            val prompt = "Generate a short educational cyberpunk praise for successfully filtering ${challenge.metadata["recommendedFilter"]} to find a flag in a ${challenge.tier} tier challenge."
            val feedback = aiProvider.generateContent(prompt)
            setExternalFeeds(listOf(feedback))
        }
    }

    private fun calculateRewards(challenge: Challenge): HackingReward {
        val multiplier = when (challenge.tier) {
            DifficultyTier.NOVICE -> 1
            DifficultyTier.OPERATIVE -> 2
            DifficultyTier.GHOST -> 5
            DifficultyTier.NETRUNNER -> 10
        }

        val components = mutableListOf<Pair<Rarity, Int>>()
        when (challenge.tier) {
            DifficultyTier.NOVICE -> components.add(Rarity.COMMON to Random.nextInt(1, 3))
            DifficultyTier.OPERATIVE -> components.add(Rarity.COMMON to Random.nextInt(3, 6))
            DifficultyTier.GHOST -> {
                components.add(Rarity.COMMON to Random.nextInt(5, 10))
                components.add(Rarity.RARE to Random.nextInt(1, 2))
            }
            DifficultyTier.NETRUNNER -> {
                components.add(Rarity.RARE to Random.nextInt(2, 4))
                components.add(Rarity.EPIC to Random.nextInt(1, 2))
            }
        }

        return HackingReward(
            xp = 100L * multiplier,
            eddies = 50 * multiplier,
            fragments = 5 * multiplier,
            components = components
        )
    }

    fun craftQuickHack(rarity: Rarity) {
        viewModelScope.launch {
            val component = inventoryDao.getComponentByRarity(rarity)
            if (component != null && component.quantity >= 10) {
                inventoryDao.insertOrUpdateComponent(component.copy(quantity = component.quantity - 10))
                
                val roll = Random.nextInt(100)
                val quickHackType = when (rarity) {
                    Rarity.COMMON -> if (roll < 75) QuickHackType.ICE_BREAKER else null
                    Rarity.RARE -> when {
                        roll < 65 -> QuickHackType.ICE_BREAKER
                        roll < 95 -> QuickHackType.GHOST_PULSE
                        else -> QuickHackType.NEURAL_OVERRIDE
                    }
                    Rarity.EPIC -> when {
                        roll < 45 -> QuickHackType.GHOST_PULSE
                        roll < 90 -> QuickHackType.NEURAL_OVERRIDE
                        else -> QuickHackType.SYSTEM_BURN
                    }
                    Rarity.LEGENDARY -> QuickHackType.SYSTEM_BURN // Just in case
                }

                if (quickHackType != null) {
                    val qh = createQuickHack(quickHackType)
                    inventoryDao.insertQuickHack(qh)
                }
            }
        }
    }

    private fun createQuickHack(type: QuickHackType): QuickHack {
        return when (type) {
            QuickHackType.ICE_BREAKER -> QuickHack(
                id = "ICE_BREAKER_${System.currentTimeMillis()}",
                name = "ICE Breaker",
                type = QuickHackType.ICE_BREAKER,
                rarity = Rarity.COMMON,
                description = "Skips one small firewall mini-game.",
                cooldownHours = 4
            )
            QuickHackType.GHOST_PULSE -> QuickHack(
                id = "GHOST_PULSE_${System.currentTimeMillis()}",
                name = "Ghost Pulse",
                type = QuickHackType.GHOST_PULSE,
                rarity = Rarity.RARE,
                description = "Skips any firewall for 24 hours.",
                cooldownHours = 12
            )
            QuickHackType.NEURAL_OVERRIDE -> QuickHack(
                id = "NEURAL_OVERRIDE_${System.currentTimeMillis()}",
                name = "Neural Override",
                type = QuickHackType.NEURAL_OVERRIDE,
                rarity = Rarity.EPIC,
                description = "Skips ALL firewalls in one feature for the entire day.",
                cooldownHours = 24
            )
            QuickHackType.SYSTEM_BURN -> QuickHack(
                id = "SYSTEM_BURN_${System.currentTimeMillis()}",
                name = "System Burn",
                type = QuickHackType.SYSTEM_BURN,
                rarity = Rarity.LEGENDARY,
                description = "Permanently removes one specific firewall from the game.",
                cooldownHours = 0,
                isOneTimeUse = true
            )
        }
    }

    fun setExternalFeeds(feeds: List<String>) {
        _externalFeeds.value = feeds
    }

    private fun generateAlerts() {
        viewModelScope.launch {
            val templates = listOf(
                "TRACE_DETECTED" to "Unauthorized access attempt in Subnet 7.",
                "BOUNTY_POSTED" to "Target: 'Ghost_Dog'. Reward: 50,000 Eddies.",
                "ICE_UPDATE" to "Arasaka updating firewall protocols in Watson.",
                "NET_WATCH_ADVISORY" to "High level of packet loss in the combat zone.",
                "SYSTEM_WARNING" to "Neural link instability detected in rank 3 operatives."
            )
            
            while (true) {
                val (title, desc) = templates.random()
                val severity = listOf("LOW", "MEDIUM", "CRITICAL").random()
                val newAlert = NetWatchAlert(title = title, description = desc, severity = severity)
                
                _netWatchAlerts.update { (it + newAlert).takeLast(10) }
                delay(Random.nextLong(10000, 30000)) // New alert every 10-30 seconds
            }
        }
    }
}
