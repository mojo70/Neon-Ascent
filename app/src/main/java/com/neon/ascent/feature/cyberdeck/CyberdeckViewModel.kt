package com.neon.ascent.feature.cyberdeck

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neon.ascent.core.ai.AiPersona
import com.neon.ascent.data.local.InventoryDao
import com.neon.ascent.data.local.UserCharacterDao
import com.neon.ascent.core.lore.data.LoreRepository
import com.neon.ascent.core.lore.data.Megacorp
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
    private val userCharacterDao: UserCharacterDao,
    private val loreRepository: LoreRepository
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

    private val _bypassedBlackIce = MutableStateFlow<Set<String>>(emptySet())
    val bypassedBlackIce: StateFlow<Set<String>> = _bypassedBlackIce.asStateFlow()

    private val _corpoNodes = MutableStateFlow<List<CorpoNode>>(emptyList())
    val corpoNodes: StateFlow<List<CorpoNode>> = _corpoNodes.asStateFlow()

    init {
        generateAlerts()
        loadCorpoNodes()
    }

    private fun loadCorpoNodes() {
        viewModelScope.launch {
            val megacorps = loreRepository.getAllMegacorps()
            if (megacorps.isNotEmpty()) {
                _corpoNodes.value = megacorps.map { it.toCorpoNode() }
            }
        }
    }

    private fun Megacorp.toCorpoNode(): CorpoNode {
        return CorpoNode(
            name = name,
            ticker = stockTicker.symbol,
            slogan = slogan,
            profile = description,
            securityTier = when (hackingDifficulty?.uppercase()) {
                "EXTREME" -> DifficultyTier.BLACK_ICE
                "VERY HARD" -> DifficultyTier.BLACK_ICE
                "HARD" -> DifficultyTier.NETRUNNER
                "MEDIUM-HARD" -> DifficultyTier.GHOST
                "MEDIUM" -> DifficultyTier.OPERATIVE
                "MEDIUM-EASY" -> DifficultyTier.OPERATIVE
                "EASY-MEDIUM" -> DifficultyTier.OPERATIVE
                "EASY" -> DifficultyTier.NOVICE
                "EASIEST" -> DifficultyTier.NOVICE
                else -> if (hasNativeBlackICE) DifficultyTier.BLACK_ICE else DifficultyTier.OPERATIVE
            },
            stockPrice = stockTicker.price,
            stockChange = stockTicker.change,
            stockChangePercent = if (stockTicker.price != 0.0) (stockTicker.change / (stockTicker.price - stockTicker.change)) * 100 else 0.0,
            marketCap = "§${String.format("%.1f", (stockTicker.price * stockTicker.volume) / 1_000_000_000_000.0)} Trillion",
            ceo = ceo.name,
            highlights = achievements.map { "${it.year} - ${it.title}: ${it.description}" },
            earningsReport = generateEarningsReport(this),
            investorDeck = generateInvestorDeck(this)
        )
    }

    private fun generateEarningsReport(megacorp: Megacorp): String {
        return """
            ${megacorp.name} (${megacorp.stockTicker.symbol}) Q3 2071 Earnings Transmission
            Net Revenue: ${megacorp.financials?.revenue ?: "N/A"}
            Profit: ${megacorp.financials?.profit ?: "N/A"}
            EPS: ${megacorp.financials?.eps ?: "N/A"}
            
            [DIVISIONAL PERFORMANCE]
            ${megacorp.divisions.joinToString("\n") { "• ${it.name}: ${it.revenue ?: "N/A"} (${it.growth ?: "N/A"})" }}
            
            [CEO STATEMENT]
            "${megacorp.ceo.name}: ${megacorp.flavorText.firstOrNull() ?: ""}"
        """.trimIndent()
    }

    private fun generateInvestorDeck(megacorp: Megacorp): List<InvestorSlide>? {
        return when (megacorp.id) {
            "panopticon" -> listOf(
                InvestorSlide("PANOPTICON", "We See What You Will Become", "Presented by Dr. Elias Voss – The Eye\nOctober 2071"),
                InvestorSlide("THE PROBLEM", "The world is drowning in noise.", "Humans lie. Humans forget. Humans hide. We fix that. Panopticon removes uncertainty from existence."),
                InvestorSlide("OUR SOLUTION", "The Panopticon Mesh", "One single intelligence layer that knows every citizen better than they know themselves.\n\nCore Product: Omniscience as a Service (OaaS)")
            )
            "aetherx" -> listOf(
                InvestorSlide("AETHERX", "Conquering the Outer System", "Presented by Dr. Vance 'Thrust' Calder – CEO & Chief Penetration Officer\nOctober 2071"),
                InvestorSlide("THE PROBLEM", "The inner system is crowded, regulated, and boring.", "Mars is full of tourists and eco-hippies\nEarth orbit is clogged with debris and lawyers\nGovernments still pretend space belongs to 'humanity'\n\nWe fix that."),
                InvestorSlide("OUR SOLUTION", "Heavy Starship-class Fleet", "Zero-pullout propulsion\nMassive payload capacity (planets feel it)\nProven on Uranus (multiple times)\n\nCore Offering: Planetary Acquisition as a Service (PAaaS)")
            )
            "obsidianveil" -> listOf(
                InvestorSlide("OBSIDIAN VEIL", "Ascension Series Ω", "Presented by Dr. Seraphine Voss – Veilwalker\nOctober 2071"),
                InvestorSlide("THE VISION", "Flesh is temporary. Obsidian is eternal.", "We don't just augment. We rewrite what it means to be human."),
                InvestorSlide("TRACTION", "The Future is Post-Human", "Veil Lattice adoption up 67%\nProject Eternity achieved 100% client retention\nMidnight Seraph skin set sold out in 11 minutes")
            )
            "helixspace" -> listOf(
                InvestorSlide("HELIXSPACE", "Infinite Spin Series Ω", "Presented by Dr. Elara Voss – HelixPrime\nOctober 2071"),
                InvestorSlide("THE VISION", "The future is not launched. It is grown.", "We build elegant, self-sustaining helical architecture for the stars."),
                InvestorSlide("OUR SOLUTION", "Helix Ring Habitats", "Orbital habitats with spun gravity, adapted colonists, and living starships grown from DNA scaffolding.")
            )
            "omnisight" -> listOf(
                InvestorSlide("OMNISIGHT", "Certainty Series Ω", "Presented by Dr. Lucian Crowe – AllSeer\nOctober 2071"),
                InvestorSlide("THE PROBLEM", "Uncertainty is expensive.", "Chaos is unprofitable. Free will is a liability.\n\nWe fix that."),
                InvestorSlide("OUR SOLUTION", "The Oracle Engine", "One unified predictive model that knows every variable before it exists.")
            )
            "microhard" -> listOf(
                InvestorSlide("MICROHARD", "Dominion Series Ω", "Presented by Gideon “Gid” Bates – Dominus\nOctober 2071"),
                InvestorSlide("THE PROBLEM", "The solar system runs on chaos.", "Fragmented OS. Insecure implants. Users who think they own their own hardware.\n\nWe fix that."),
                InvestorSlide("OUR SOLUTION", "Helix OS + Dominion Cloud", "Seamless integration across implants, habs, rockets, and weapons.\nMandatory telemetry 'for your safety'.\nZero user choice. Maximum control.\n\nCore Product: Total Digital Dominion as a Service (TDaaS)")
            )
            "kagami" -> listOf(
                InvestorSlide("KAGAMI HEAVY INDUSTRIES", "Blade Protocol Series Ω", "Presented by Ryūji “Kage” Nakamura\nOctober 2071"),
                InvestorSlide("THE VISION", "Honor is obsolete.", "The iron fist of the corporate world. We don't just secure assets; we eliminate threats."),
                InvestorSlide("OUR SOLUTION", "Shadow Grid + Void Strike", "Neo-feudal corporate warriors, monomolecular blades, and orbital-drop strike teams.")
            )
            "neobank" -> listOf(
                InvestorSlide("NEOBANK", "Ledger Series Ω", "Presented by Lucius “Ledger” Voss – VaultLord\nOctober 2071"),
                InvestorSlide("THE VISION", "Debt is the truest form of loyalty.", "We don't just bank assets; we bank futures. The solar system's premier weaponized financial institution."),
                InvestorSlide("OUR SOLUTION", "Eternal Debt + Credit Nexus", "Loans that last generations and credit scores that dictate your legal reality.\n\nCore Offerings: Eternal Debt Instruments & NeoVault Zero")
            )
            "vitasynth" -> listOf(
                InvestorSlide("VITASYNTH", "Immortality Series Ω", "Presented by Dr. Isolde “Eternal” Voss – VitalLord\nOctober 2071"),
                InvestorSlide("THE VISION", "Death is a bug.", "We don't just sell medicine; we sell time. The premier destination for the post-biological elite."),
                InvestorSlide("OUR SOLUTION", "Longevity + OrganForge", "Longevity Serum, high-performance reprints, and NeuroBliss mood sculpting.")
            )
            "securacorp" -> listOf(
                InvestorSlide("SECURECORP", "Pacification Series Ω", "Presented by General Marcus “Iron” Kane – IronCommand\nOctober 2071"),
                InvestorSlide("THE VISION", "Protection has a price. Obedience is free.", "Order through superior force. Freedom is a security risk. The solar system's premier private military force."),
                InvestorSlide("OUR SOLUTION", "Iron Mandate + Void Strike", "Private policing, orbital rapid deployment, and asset protection.")
            )
            "aegis" -> listOf(
                InvestorSlide("AEGIS ARMAMENTS", "Firepower Series Ω", "Presented by Colonel Valeria “Forge” Kane – AegisPrime\nOctober 2071"),
                InvestorSlide("THE VISION", "Shield and Sword. No mercy. No refunds.", "The premier heavy weapons manufacturer. We fund conflicts just to sell solutions."),
                InvestorSlide("OUR SOLUTION", "The Titan Forge + Orbital Lance", "Titan-Class warframes, Orbital Lance railgun platforms, and Warforge weapon printing.")
            )
            else -> null
        }
    }

    fun bypassBlackIce(corpoName: String, username: String, password: String): Boolean {
        if (corpoName == "AetherX" && username == "Thrust" && password == "P0undUr@nu\$N0Pull0ut420.69") {
            grantBypassRewards(DifficultyTier.BLACK_ICE)
            _bypassedBlackIce.update { it + corpoName }
            return true
        }
        if (corpoName == "Panopticon" && username == "TheEye" && password == "EyeSeeYouPullingOut42069") {
            grantBypassRewards(DifficultyTier.GHOST)
            _bypassedBlackIce.update { it + corpoName }
            return true
        }
        if (corpoName == "Microhard" && username == "Dominus" && password == "GidOwnsYourOS6969") {
            grantBypassRewards(DifficultyTier.OPERATIVE)
            _bypassedBlackIce.update { it + corpoName }
            return true
        }
        if (corpoName == "Obsidian Veil" && username == "Veilwalker" && password == "MeatIsObsoleteSlut420") {
            grantBypassRewards(DifficultyTier.BLACK_ICE)
            _bypassedBlackIce.update { it + corpoName }
            return true
        }
        if (corpoName == "OmniSight" && username == "AllSeer" && password == "ModelSaysYoureFucked99.7") {
            grantBypassRewards(DifficultyTier.GHOST)
            _bypassedBlackIce.update { it + corpoName }
            return true
        }
        if (corpoName == "HelixSpace" && username == "HelixPrime" && password == "SpinToWinBaby420") {
            grantBypassRewards(DifficultyTier.GHOST)
            _bypassedBlackIce.update { it + corpoName }
            return true
        }
        if (corpoName == "NeoBank" && username == "VaultLord" && password == "Y0urCr3d1t5MyPl34sur3_69") {
            grantBypassRewards(DifficultyTier.BLACK_ICE)
            _bypassedBlackIce.update { it + corpoName }
            return true
        }
        if (corpoName == "Kagami Heavy Industries" && username == "Kage" && password == "KageBladeN0Mercy6969") {
            grantBypassRewards(DifficultyTier.BLACK_ICE)
            _bypassedBlackIce.update { it + corpoName }
            return true
        }
        if (corpoName == "VitaSynth" && username == "VitalLord" && password == "L1v3L0ng3rP4yF0r3v3r_69") {
            grantBypassRewards(DifficultyTier.GHOST)
            _bypassedBlackIce.update { it + corpoName }
            return true
        }
        if (corpoName == "SecuraCorp" && username == "IronCommand" && password == "1r0nF1stN0M3rcy_6969") {
            grantBypassRewards(DifficultyTier.BLACK_ICE)
            _bypassedBlackIce.update { it + corpoName }
            return true
        }
        if (corpoName == "Aegis Armaments" && username == "AegisPrime" && password == "F0rg3d1nF1r3N0R3tr3at_69") {
            grantBypassRewards(DifficultyTier.OPERATIVE)
            _bypassedBlackIce.update { it + corpoName }
            return true
        }
        if (corpoName == "SpectraMedia" && username == "SpectraLord" && password == "1AmTh3R34l1ty69xX") {
            grantBypassRewards(DifficultyTier.NOVICE)
            _bypassedBlackIce.update { it + corpoName }
            return true
        }
        return false
    }

    private fun grantBypassRewards(tier: DifficultyTier) {
        viewModelScope.launch {
            val challenge = Challenge(
                id = "bypass_${System.currentTimeMillis()}",
                tier = tier,
                type = ChallengeType.WEB_EXPLOIT,
                focusSkill = SkillType.ANALYSIS,
                title = "Black ICE Bypass",
                description = "Credential extraction success.",
                timeLimitSeconds = 0,
                bufferSize = 0,
                packets = emptyList(),
                correctFlag = "",
                solutionHint = "",
                metadata = emptyMap()
            )
            processWin(challenge)
        }
    }

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
            val context = "Successfully filtering ${challenge.metadata["recommendedFilter"]} to find a flag in a ${challenge.tier} tier challenge."
            val prompt = AiPersona.getSocratesPrompt(context) + "\nGenerate a short educational cyberpunk praise."
            val feedback = aiProvider.generateContent(prompt)
            setExternalFeeds(listOf(feedback))
        }
    }

    private fun calculateRewards(challenge: Challenge): HackingReward {
        // Special lookup for bypass rewards from JSON
        val megacorpId = challenge.id.substringAfter("bypass_").lowercase() // Not reliable id
        // Let's use the tier multiplier for now as a base, but ideally we'd pass the mc rewards here.

        val multiplier = when (challenge.tier) {
            DifficultyTier.NOVICE -> 1
            DifficultyTier.OPERATIVE -> 2
            DifficultyTier.GHOST -> 5
            DifficultyTier.NETRUNNER -> 10
            DifficultyTier.BLACK_ICE -> 25
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
            DifficultyTier.BLACK_ICE -> {
                components.add(Rarity.EPIC to Random.nextInt(2, 5))
                components.add(Rarity.LEGENDARY to Random.nextInt(1, 2))
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
