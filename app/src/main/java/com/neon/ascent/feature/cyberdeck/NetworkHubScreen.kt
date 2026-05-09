package com.neon.ascent.feature.cyberdeck

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neon.ascent.model.ChatSession
import com.neon.ascent.model.CorpoNode
import com.neon.ascent.model.DifficultyTier
import com.neon.ascent.model.InvestorSlide
import com.neon.ascent.model.SkillType
import com.neon.ascent.ui.CyberFrame
import com.neon.ascent.ui.components.CeoContactCard
import com.neon.ascent.ui.theme.LocalNeonTheme
import java.util.Locale

@Composable
fun NetworkHubScreen(
    onBack: () -> Unit,
    onChessClick: () -> Unit,
    viewModel: CyberdeckViewModel,
    chatViewModel: ChatViewModel = hiltViewModel(),
    hubViewModel: NetworkHubViewModel = hiltViewModel(),
    stockViewModel: StockViewModel = hiltViewModel(),
    netWorthViewModel: NetWorthViewModel = hiltViewModel()
) {
    var activeTab by remember { mutableStateOf(NetworkTab.CHATS) }
    var selectedContact by remember { mutableStateOf<String?>(null) }
    var selectedCorpo by remember { mutableStateOf<CorpoNode?>(null) }
    var selectedDossierCorpId by remember { mutableStateOf<String?>(null) }
    var showingInvestorDeck by remember { mutableStateOf(false) }
    
    val currentChallenge by viewModel.currentChallenge.collectAsState()
    val lastReward by viewModel.lastReward.collectAsState()
    val bypassed by viewModel.bypassedBlackIce.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        if (selectedContact != null) {
            ChatDetailScreen(
                contactName = selectedContact!!,
                onBack = { selectedContact = null },
                viewModel = chatViewModel
            )
        } else if (selectedDossierCorpId != null) {
            MegacorpDossierScreen(
                corpId = selectedDossierCorpId!!,
                onBack = { selectedDossierCorpId = null }
            )
        } else if (selectedCorpo != null) {
            val tier = if (selectedCorpo!!.name in bypassed && selectedCorpo!!.securityTier == DifficultyTier.BLACK_ICE) {
                DifficultyTier.GHOST
            } else {
                selectedCorpo!!.securityTier
            }
            CorpoDetailView(
                corpo = selectedCorpo!!,
                currentTier = tier,
                onBack = { 
                    selectedCorpo = null
                    showingInvestorDeck = false
                },
                onAttack = { viewModel.startPcapChallenge(tier, SkillType.ANALYSIS) },
                onViewDeck = { showingInvestorDeck = true },
                onViewDossier = { selectedDossierCorpId = selectedCorpo?.name?.lowercase() ?: "" },
                onBypass = { user, pwd -> viewModel.bypassBlackIce(selectedCorpo!!.name, user, pwd) }
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF020508))
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // Top Header
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF00FF9F))
                    }
                    Text(
                        "NET // V3.0",
                        color = Color(0xFF00FF9F),
                        fontSize = 20.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Tab Bar
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    NetworkTabItem("CHATS", activeTab == NetworkTab.CHATS, Modifier.weight(1f)) { activeTab = NetworkTab.CHATS }
                    NetworkTabItem("SCAN", activeTab == NetworkTab.SCAN, Modifier.weight(1f)) { activeTab = NetworkTab.SCAN }
                    NetworkTabItem("BIZ", activeTab == NetworkTab.BIZ, Modifier.weight(1f)) { activeTab = NetworkTab.BIZ }
                    NetworkTabItem("NODES", activeTab == NetworkTab.NODES, Modifier.weight(1f)) { activeTab = NetworkTab.NODES }
                    NetworkTabItem("GAMES", activeTab == NetworkTab.GAMES, Modifier.weight(1f)) { activeTab = NetworkTab.GAMES }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Content Area
                Box(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
                    when (activeTab) {
                        NetworkTab.CHATS -> ChatListArea(chatViewModel, { selectedContact = it }, { selectedDossierCorpId = it })
                        NetworkTab.SCAN -> NetScanArea(hubViewModel, chatViewModel)
                        NetworkTab.BIZ -> BizScreen(stockViewModel, netWorthViewModel)
                        NetworkTab.NODES -> CorpoNodesArea(viewModel, bypassed) { selectedCorpo = it }
                        NetworkTab.GAMES -> GamesArea(onChessClick)
                    }
                }
            }
        }

        // Overlays
        currentChallenge?.let { challenge ->
            PcapChallengeScreen(
                challenge = challenge,
                onFlagSubmitted = { viewModel.submitFlag(it) },
                onFilterApplied = { /* VM logic */ },
                onTimeout = { /* timeout logic */ }
            )
        }

        lastReward?.let { reward ->
            HackingRewardDialog(
                reward = reward,
                onDismiss = { viewModel.clearReward() }
            )
        }

        if (showingInvestorDeck && selectedCorpo?.investorDeck != null) {
            InvestorDeckOverlay(
                slides = selectedCorpo!!.investorDeck!!,
                onDismiss = { showingInvestorDeck = false }
            )
        }
    }
}

@Composable
fun ChatListArea(
    viewModel: ChatViewModel,
    onContactClick: (String) -> Unit,
    onDossierClick: (String) -> Unit
) {
    val sessions by viewModel.chatSessions.collectAsState()
    val megacorps by viewModel.megacorps.collectAsState()
    val trustMap by viewModel.executiveTrust.collectAsState()
    var lookupText by remember { mutableStateOf("") }

    Column {
        // Netrunner Lookup
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = lookupText,
                onValueChange = { lookupText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("NETRUNNER_LOOKUP...", color = Color.Gray, fontSize = 12.sp) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White.copy(alpha = 0.05f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            )
            IconButton(onClick = { 
                if (lookupText.isNotBlank()) {
                    viewModel.addContact(lookupText)
                    lookupText = ""
                }
            }) {
                Icon(Icons.Default.Search, contentDescription = "Add", tint = Color(0xFF00FF9F))
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (megacorps.isNotEmpty()) {
                item {
                    Text(
                        "// EXECUTIVE_CONTACTS",
                        color = Color(0xFFFF006E),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                items(megacorps) { corp ->
                    CeoContactCard(
                        megacorp = corp,
                        trustLevel = trustMap[corp.id] ?: 0f,
                        onMessageClick = { 
                            val handle = corp.ceo.netHandle ?: "UNKNOWN"
                            viewModel.addContact(handle, isFixer = true)
                            onContactClick(handle)
                        },
                        onProfileClick = { onDossierClick(corp.id) }
                    )
                }
            }

            item {
                Text(
                    "// ACTIVE_SESSIONS",
                    color = Color(0xFF00FF9F),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            items(sessions) { session ->
                ChatSessionItem(session) { onContactClick(session.contactName) }
            }
        }
    }
}

@Composable
fun ChatSessionItem(session: ChatSession, onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Alpha"
    )

    CyberFrame(
        label = session.contactName,
        borderColor = if (session.isFixer) Color(0xFFFF0088) else Color(0xFF00FF9F),
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Unread Indicator (Red Light)
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(
                        if (session.isUnread) Color.Red.copy(alpha = alpha) else Color.Transparent
                    )
                    .then(if (session.isUnread) Modifier.border(1.dp, Color.Red, CircleShape) else Modifier)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column {
                Text(
                    session.lastMessage,
                    color = Color.White,
                    fontSize = 14.sp,
                    maxLines = 1
                )
                Text(
                    java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(session.lastTimestamp),
                    color = Color.Gray,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
fun NetScanArea(viewModel: NetworkHubViewModel, chatViewModel: ChatViewModel) {
    val state by viewModel.scanningState.collectAsState()
    
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize()) {
        CyberFrame(label = "PROXIMITY_SCANNER") {
            Box(modifier = Modifier.fillMaxWidth().height(250.dp), contentAlignment = Alignment.Center) {
                when (val s = state) {
                    is ScanningState.Idle -> {
                        Text("INITIATE_PROXIMITY_TRACE", color = Color.Gray)
                    }
                    is ScanningState.Scanning -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Color(0xFF00FF9F))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("ACQUIRING_TARGET...", color = Color(0xFF00FF9F), fontSize = 12.sp)
                        }
                    }
                    is ScanningState.Tracing -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("TARGET_LOCKED", color = Color.Red, fontWeight = FontWeight.Bold)
                            Text("DISTANCE: ${String.format(Locale.getDefault(), "%.1f", s.distanceMeters)}m", color = Color.White, fontSize = 24.sp, fontFamily = FontFamily.Monospace)
                            Text("PHYSICALLY_MOVE_TOWARDS_SIGNAL", color = Color.Gray, fontSize = 10.sp)
                        }
                    }
                    is ScanningState.Found -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("SIGNAL_CAPTURED!", color = Color(0xFF00FF9F), fontWeight = FontWeight.Bold)
                            Text(s.name, color = Color.White, fontSize = 20.sp)
                            Button(onClick = { 
                                chatViewModel.addContact(s.name)
                                viewModel.stopScan()
                            }) {
                                Text("ADD_TO_CONTACTS")
                            }
                        }
                    }
                    is ScanningState.Error -> {
                        Text("ERROR: ${s.message}", color = Color.Red)
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = { 
                if (state == ScanningState.Idle) viewModel.startScan() else viewModel.stopScan()
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF9F))
        ) {
            Text(if (state == ScanningState.Idle) "START_TRACE" else "ABORT_TRACE", color = Color.Black)
        }
    }
}

@Composable
fun NetworkTabItem(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier.clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            label,
            color = if (selected) Color(0xFF00FF9F) else Color.Gray,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(if (selected) Color(0xFF00FF9F) else Color.Transparent)
        )
    }
}

@Composable
fun CorpoNodesArea(viewModel: CyberdeckViewModel, bypassed: Set<String>, onCorpoClick: (CorpoNode) -> Unit) {
    val corposByVM by viewModel.corpoNodes.collectAsState()
    
    val hardcodedCorpos = remember {
        listOf(
            CorpoNode(
                name = "AetherX",
                ticker = "AETX",
                slogan = "Our Heavy Starship doesn’t pull out. Uranus knows.",
                profile = "Founded in 2065 by ex-SpaceX renegades, AetherX is the premier megacorp for aggressive deep-space expansion. We deliver conquest. No safe orbits. No gentle landings. Just raw, unrelenting thrust.",
                securityTier = DifficultyTier.BLACK_ICE,
                stockPrice = 387.42,
                stockChange = 24.67,
                stockChangePercent = 6.8,
                marketCap = "§1.84 Trillion",
                ceo = "Dr. Vance 'Thrust' Calder",
                highlights = listOf(
                    "2065 - First successful crewed slingshot around Uranus.",
                    "2067 - Deployment of the Heavy Starship-class fleet.",
                    "2068 - Establishment of New Elysium Outpost on Titania.",
                    "2069 - Record 47 consecutive days of continuous orbital pounding."
                ),
                earningsReport = """
                    AETHERX (AETX) Q3 2071 Earnings Transmission
                    Net Revenue: §487.3 Billion ▲ +38% YoY
                    Adjusted Thrust Profit: §214.6 Billion ▲ +51%
                    EPS: §4.82 | Market Reaction: +19% After-Hours
                    
                    [DIVISIONAL PERFORMANCE]
                    • Orbital Dominance: §289.4B (+62%)
                    • Planetary Acquisition: §98.7B (+27%)
                    • Deep Thrust R&D: §67.2B (+81%)
                    • Colony & Ent.: §32.0B (+194%)
                    
                    [CEO STATEMENT - VANCE "THRUST" CALDER]
                    "This quarter we grabbed the stars and pinned them down. Uranus is no longer virgin territory. We’re not slowing down. We’re not pulling out. We’re going deeper, harder, and into more holes than ever before."
                    
                    [GUIDANCE]
                    Targeting full colonization of Oberon and Umbriel. 3-for-1 Stock Split projected. Dividend: §0.69 (Nice).
                """.trimIndent(),
                investorDeck = listOf(
                    InvestorSlide("AETHERX", "Conquering the Outer System", "Presented by Dr. Vance 'Thrust' Calder – CEO & Chief Penetration Officer\nOctober 2071"),
                    InvestorSlide("THE PROBLEM", "The inner system is crowded, regulated, and boring.", "Mars is full of tourists and eco-hippies\nEarth orbit is clogged with debris and lawyers\nGovernments still pretend space belongs to 'humanity'\n\nWe fix that."),
                    InvestorSlide("OUR SOLUTION", "Heavy Starship-class Fleet", "Zero-pullout propulsion\nMassive payload capacity (planets feel it)\nProven on Uranus (multiple times)\n\nCore Offering: Planetary Acquisition as a Service (PAaaS)"),
                    InvestorSlide("TRACTION", "We Don't Just Talk – We Pound", "14 successful Uranus deep-impact missions in Q3 alone\nTitania Pleasure Outpost – 94% occupancy\n47-day continuous orbital record\nAcquired AstroThrust Dynamics in a very hostile takeover"),
                    InvestorSlide("MARKET OPPORTUNITY", "Total Addressable Market: §4.7 Trillion by 2080", "Deep-space conquest: §1.9T\nOrbital 'entertainment' services: §890B\nResource extraction: §1.2T\nGovernment black-budget contracts: §700B"),
                    InvestorSlide("TECHNOLOGY", "Next-Gen 'No-Pullout' Propulsion", "Deep Thrust AI – Optimizes angle, duration, and intensity in real time\nPleasure Dome Hab Modules – Because crews perform better after release"),
                    InvestorSlide("FINANCIAL HIGHLIGHTS", "Revenue: §487.3 Billion (+38% YoY)", "Adjusted Thrust Profit: §214.6 Billion (+51%)\nEPS: §4.82\n\nGuidance Q4: §620B+"),
                    InvestorSlide("TEAM", "Vance 'Thrust' Calder – CEO", "Dr. Nadia 'Velvet' Voss – CTO\nMarcus 'Iron' Kane – Head of Security & Acquisitions\n12,400 highly motivated professionals"),
                    InvestorSlide("ROADMAP", "What's Next", "Q4 2071 – Heavy Starship-8 launch\n2072 – Full Neptune teasing operations\n2073 – First crewed 'extended contact' mission to Pluto"),
                    InvestorSlide("THE ASK", "Raising §450 Billion", "60% More Heavy Starships\n25% Expanded pleasure colonies\n10% Marketing (Uranus memes)\n\nJoin the thrust."),
                    InvestorSlide("AETHERX", "Reach for the stars. Then reach deeper.", "Thank you.\nQuestions? Fire away.")
                )
            ),
            CorpoNode(
                name = "Panopticon",
                ticker = "PANO",
                slogan = "Do Evil.",
                profile = "The megacorp that doesn’t just watch you — it knows you better than you know yourself. While others sell you products, Panopticon sells omniscience. Your search history, your dreams, your biometric fluctuations, your late-night Grid scrolls… all fuel the machine.",
                securityTier = DifficultyTier.GHOST,
                stockPrice = 1284.70,
                stockChange = 41.33,
                stockChangePercent = 3.2,
                marketCap = "§5.2 Trillion",
                ceo = "Dr. Elias Voss",
                highlights = listOf(
                    "2051 - Panopticon Index Launch: 8.4 quadrillion queries/sec.",
                    "2059 - Predictive Behavior Mesh: 94% accuracy 11 days early.",
                    "2064 - Project Veil: Global telemetry integration.",
                    "2068 - Consolidation is Caring: Acquired remaining social networks.",
                    "2070 - Echo Chamber 2.0: Perfectly custom-tailored reality."
                ),
                earningsReport = """
                    PANOPTICON (PANO) Q3 2071 Earnings Transmission
                    Net Revenue: §1.24 Trillion ▲ +47% YoY
                    Adjusted Omniscience Profit: §892 Billion ▲ +62%
                    EPS: §9.87 | Market Reaction: +31% After-Hours
                    
                    [DIVISIONAL PERFORMANCE]
                    • OmniSearch: §612B (+39%)
                    • ShadowNet: §341B (+71%)
                    • EchoForge: §198B (+112%)
                    • NexusID: §67B (+28%)
                    • BlackLens: §22B (official)
                    
                    [CEO STATEMENT - DR. ELIAS VOSS]
                    "Interesting. You still believe quarterly reports matter. We don’t sell data. We don’t sell predictions. We sell certainty. And this quarter, certainty was extremely profitable. …I already knew you would applaud."
                """.trimIndent(),
                investorDeck = listOf(
                    InvestorSlide("PANOPTICON", "We See What You Will Become", "Presented by Dr. Elias Voss – The Eye\nOctober 2071"),
                    InvestorSlide("THE PROBLEM", "The world is drowning in noise.", "Humans lie. Humans forget. Humans hide. We fix that. Panopticon removes uncertainty from existence."),
                    InvestorSlide("OUR SOLUTION", "The Panopticon Mesh", "One single intelligence layer that knows every citizen better than they know themselves.\n\nCore Product: Omniscience as a Service (OaaS)"),
                    InvestorSlide("TRACTION", "We Predicted This", "3.8 billion active Echo Chamber users\n1,247 pre-empted incidents this quarter\n98% global neural implant penetration\nShadow profiles now 400% more detailed than user self-knowledge"),
                    InvestorSlide("MARKET OPPORTUNITY", "Total Addressable Market: §9.3 Trillion by 2080", "Surveillance state contracts: §4.1T\nPersonalized reality licensing: §2.8T\nPre-crime intelligence sales: §1.7T\nBlackLens classified work: §700B"),
                    InvestorSlide("TECHNOLOGY", "DeepMind Succession Protocol", "The AI that replaced the ethics board Dr. Voss personally dissolved in 2048.\n\nVoss Neural Signature: Every decision is logged with his private biometric key."),
                    InvestorSlide("FINANCIAL HIGHLIGHTS", "Revenue: §1.24 Trillion (+47% YoY)", "Adjusted Omniscience Profit: §892 Billion (+62%)\nEPS: §9.87\n\nGuidance Q4: §1.6T+"),
                    InvestorSlide("TEAM", "Dr. Elias Voss – CEO", "The Oversight Committee\nDeepMind AI Core\nBlackLens Intelligence Division"),
                    InvestorSlide("ROADMAP", "What's Next", "Q4 2071 – Pre-Crime Reality Layer launch\n2072 – Full neural thought indexing\n2073 – Interplanetary Mesh extension"),
                    InvestorSlide("THE ASK", "Raising §750 Billion", "55% Expanded Mesh infrastructure\n25% BlackLens wetwork upgrades\n15% EchoForge tuning\n\nJoin the only megacorp that already knows you’ll say yes."),
                    InvestorSlide("PANOPTICON", "Do Evil.", "Thank you. I already knew you would invest.")
                )
            ),
            CorpoNode(
                name = "Obsidian Veil",
                ticker = "OBSV",
                slogan = "We see what you will become.",
                profile = "The premier megacorp for human (re)design. Where others sell cyberware, Obsidian Veil sells evolution. They don’t augment your body — they rewrite what it means to be human. Sleek, elegant, and terrifyingly precise.",
                securityTier = DifficultyTier.BLACK_ICE,
                stockPrice = 1467.80,
                stockChange = 52.40,
                stockChangePercent = 3.7,
                marketCap = "§2.4 Trillion",
                ceo = "Dr. Seraphine Voss",
                highlights = listOf(
                    "2057 - First successful full-body Obsidian Conversion.",
                    "2062 - Launched the Veil Lattice for real-time DNA rewriting.",
                    "2066 - Project Eternity achieved biological age 147.",
                    "2069 - Released luxury Chromedream augmentation line.",
                    "2070 - Integrated Panopticon telemetry into new augs."
                ),
                earningsReport = """
                    OBSIDIAN VEIL (OBSV) Q3 2071 Earnings Transmission
                    Net Revenue: §743 Billion ▲ +41% YoY
                    Adjusted Evolution Profit: §519 Billion ▲ +58%
                    EPS: §8.91 | CEO: Veilwalker
                    
                    [DIVISIONAL PERFORMANCE]
                    • Veil Lattice: §327B (+48%)
                    • Obsidian Aesthetics: §230B (+35%)
                    • Eternity Labs: §111B (+12%)
                    • Shadowform: §52B (+22%)
                    • Ascension Clinics: §23B (+95%)
                    
                    [CEO STATEMENT - VEILWALKER]
                    "Another cycle completed. Another step closer to perfection. You still cling to that fragile meat. How… endearing."
                """.trimIndent()
            ),
            CorpoNode(
                name = "Microhard",
                ticker = "MHRD",
                slogan = "Your data, our dominion.",
                profile = "The operating system of the solar system. If Panopticon watches everything, Microhard runs everything — your implants, your hab, your rocket, your neural firewall (that they quietly bypass).",
                securityTier = DifficultyTier.OPERATIVE,
                stockPrice = 892.40,
                stockChange = 37.25,
                stockChangePercent = 4.3,
                marketCap = "§3.1 Trillion",
                ceo = "Gideon 'Gid' Bates",
                highlights = listOf(
                    "2051 - Forced global adoption of Microhard Helix OS.",
                    "2059 - Launched Always-On Telemetry — 'for your safety.'",
                    "2063 - Acquired every major competitor in 'hostile but legal' fashion.",
                    "2067 - Project Dominion - Every consumer device phones home by default.",
                    "2070 - Released HoloWindows 2077 with 400% more forced updates."
                ),
                earningsReport = """
                    MICROHARD (MHRD) Q3 2071 Earnings Transmission
                    Net Revenue: §978 Billion ▲ +29% YoY
                    Adjusted Dominion Profit: §614 Billion ▲ +44%
                    EPS: §7.42 | CEO: Dominus
                    
                    [DIVISIONAL PERFORMANCE]
                    • Helix OS: §469B (+15%)
                    • Dominion Cloud: §283B (+44%)
                    • Surface Armaments: §117B (+8%)
                    • Enforcer Updates: §78B (+52%)
                    • Legacy Tax: §31B (+5%)
                    
                    [CEO STATEMENT - DOMINUS]
                    "Another fantastic quarter. Users continue to voluntarily give us more control than ever before. We appreciate your continued trust… you don’t really have another option anyway."
                """.trimIndent()
            ),
            CorpoNode(
                name = "OmniSight",
                ticker = "OMNI",
                slogan = "All eyes, one truth",
                profile = "Global news and perception management. If we didn't report it, it didn't happen. Truth is what we broadcast.",
                securityTier = DifficultyTier.OPERATIVE,
                stockPrice = 189.45,
                stockChange = 12.30,
                stockChangePercent = 6.9,
                marketCap = "§1.5 Trillion"
            ),
            CorpoNode(
                name = "HelixSpace",
                ticker = "HLXS",
                slogan = "DNA is just a launchpad",
                profile = "Bio-integrated space travel. We grow our ships in orbit and our pilots in vats. The final frontier is within.",
                securityTier = DifficultyTier.GHOST,
                stockPrice = 521.00,
                stockChange = -2.40,
                stockChangePercent = -0.4,
                marketCap = "§2.1 Trillion"
            ),
            CorpoNode(
                name = "Kagami Heavy Industries",
                ticker = "KAGM",
                slogan = "Honor is obsolete",
                profile = "Manufacturers of high-end combat robotics and urban pacification drones. Precision engineering for a world that needs order.",
                securityTier = DifficultyTier.NETRUNNER,
                stockPrice = 612.55,
                stockChange = -8.30,
                stockChangePercent = -1.3,
                marketCap = "§4.8 Trillion"
            ),
            CorpoNode(
                name = "Aegis Armaments",
                ticker = "AEGS",
                slogan = "Total Protection through Superior Firepower",
                profile = "Personal and corporate defense solutions. If it doesn't fire 6000 rounds per minute, it's not Aegis. Protecting your assets with hot lead.",
                securityTier = DifficultyTier.OPERATIVE,
                stockPrice = 275.90,
                stockChange = 12.45,
                stockChangePercent = 4.7,
                marketCap = "§1.2 Trillion"
            ),
            CorpoNode(
                name = "NeoBank",
                ticker = "NEOB",
                slogan = "Your wealth, digitized and weaponized",
                profile = "The only bank that offers combat-ready asset protection and orbital safety deposits.",
                securityTier = DifficultyTier.NOVICE,
                stockPrice = 112.30,
                stockChange = 0.50,
                stockChangePercent = 0.4,
                marketCap = "§800 Billion"
            ),
            CorpoNode(
                name = "SpectraMedia",
                ticker = "SPEC",
                slogan = "Reality is a Choice",
                profile = "VR and Braindance dominance. Why live in the slums when you can be a god in our server farms?",
                securityTier = DifficultyTier.NOVICE,
                stockPrice = 98.20,
                stockChange = 4.10,
                stockChangePercent = 4.4,
                marketCap = "§650 Billion"
            ),
            CorpoNode(
                name = "VitaSynth",
                ticker = "VITA",
                slogan = "Synthesizing a Better You",
                profile = "Genetic modifications and synthetic organ replacement. Dying is now a subscription service.",
                securityTier = DifficultyTier.GHOST,
                stockPrice = 432.15,
                stockChange = 21.60,
                stockChangePercent = 5.3,
                marketCap = "§1.9 Trillion"
            ),
            CorpoNode(
                name = "SecuraCorp",
                ticker = "SECU",
                slogan = "Safe. Secure. Silent.",
                profile = "High-end private security and 'cleanup' operations. We don't just protect; we erase the problem.",
                securityTier = DifficultyTier.NETRUNNER,
                stockPrice = 754.30,
                stockChange = -2.10,
                stockChangePercent = -0.3,
                marketCap = "§2.8 Trillion"
            )
        )
    }

    val displayCorpos = if (corposByVM.isNotEmpty()) {
        (corposByVM + hardcodedCorpos).distinctBy { it.name }
    } else {
        hardcodedCorpos
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(displayCorpos) { corpo ->
            val isBypassed = corpo.name in bypassed
            val displayTier = if (isBypassed && corpo.securityTier == DifficultyTier.BLACK_ICE) DifficultyTier.GHOST else corpo.securityTier
            
            CyberFrame(
                label = corpo.name,
                modifier = Modifier.clickable { onCorpoClick(corpo) }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(corpo.slogan, color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        Text("Security: $displayTier", color = when(displayTier) {
                            DifficultyTier.NOVICE -> Color.Green
                            DifficultyTier.OPERATIVE -> Color.Yellow
                            DifficultyTier.BLACK_ICE -> Color.Magenta
                            else -> Color.Red
                        }, fontSize = 10.sp)
                    }
                    Button(
                        onClick = { viewModel.startPcapChallenge(displayTier, SkillType.ANALYSIS) },
                        enabled = displayTier != DifficultyTier.BLACK_ICE,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Red.copy(alpha = 0.2f),
                            disabledContainerColor = Color.Gray.copy(alpha = 0.1f)
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (displayTier == DifficultyTier.BLACK_ICE) Color.Gray else Color.Red)
                    ) {
                        Text(if (displayTier == DifficultyTier.BLACK_ICE) "LOCKED" else "ATTACK", color = if (displayTier == DifficultyTier.BLACK_ICE) Color.Gray else Color.Red, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun CorpoDetailView(
    corpo: CorpoNode,
    currentTier: DifficultyTier,
    onBack: () -> Unit,
    onAttack: () -> Unit,
    onViewDeck: () -> Unit,
    onViewDossier: () -> Unit,
    onBypass: (String, String) -> Boolean
) {
    var showLoginDialog by remember { mutableStateOf(false) }

    if (showLoginDialog) {
        CorpoLoginDialog(
            corpoName = corpo.name,
            onDismiss = { showLoginDialog = false },
            onLogin = { user, pwd -> 
                if (onBypass(user, pwd)) {
                    showLoginDialog = false
                    true
                } else false
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF020508))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF00FF9F))
            }
            Text(
                "CORPO_PROFILE // ${corpo.name}",
                color = Color(0xFF00FF9F),
                fontSize = 18.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (corpo.investorDeck != null) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CyberFrame(
                    label = "INVESTOR_NET",
                    borderColor = Color(0xFF00CCFF),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(8.dp).clickable { onViewDeck() },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("INVESTOR_DECK", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                            Text("Q3 2071 DATA", color = Color(0xFF00CCFF), fontSize = 8.sp)
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color(0xFF00CCFF), modifier = Modifier.rotate(180f).size(16.dp))
                    }
                }
                
                CyberFrame(
                    label = "SHADOW_NET",
                    borderColor = Color.Red,
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(8.dp).clickable { onViewDossier() },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("DOSSIER_ACCESS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                            Text("CLASSIFIED", color = Color.Red, fontSize = 8.sp)
                        }
                        Icon(Icons.Default.Terminal, contentDescription = null, tint = Color.Red, modifier = Modifier.size(16.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        CyberFrame(label = "MARKET_DATA") {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(corpo.ticker, color = Color.Gray, fontSize = 12.sp)
                        Text("§${corpo.stockPrice}", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        val color = if (corpo.stockChange >= 0) Color(0xFF00FF9F) else Color.Red
                        Text("${if (corpo.stockChange >= 0) "+" else ""}${corpo.stockChange}", color = color)
                        Text("${String.format(Locale.getDefault(), "%.2f", corpo.stockChangePercent)}%", color = color)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("MARKET_CAP: ${corpo.marketCap}", color = Color.Gray, fontSize = 10.sp)
                
                Spacer(modifier = Modifier.height(16.dp))
                // Fake Chart
                FakeCorpoChart(modifier = Modifier.fillMaxWidth().height(150.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        CyberFrame(label = "SECURITY_STATUS") {
            Column(modifier = Modifier.padding(12.dp)) {
                val color = when(currentTier) {
                    DifficultyTier.NOVICE -> Color.Green
                    DifficultyTier.OPERATIVE -> Color.Yellow
                    DifficultyTier.BLACK_ICE -> Color.Magenta
                    else -> Color.Red
                }
                Text("TIER: ${currentTier.name}", color = color, fontWeight = FontWeight.Bold)
                
                if (currentTier == DifficultyTier.BLACK_ICE) {
                    Text("STATUS: UNCRACKABLE. ENCRYPTION EXCEEDS HARDWARE LIMITS.", color = Color.Red, fontSize = 10.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("AUTHORIZATION_REQUIRED", color = Color.Gray, fontSize = 10.sp)
                } else {
                    Text("STATUS: VULNERABLE. BREACH_VECTOR_IDENTIFIED.", color = Color(0xFF00FF9F), fontSize = 10.sp)
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Button(
                    onClick = { showLoginDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00CCFF).copy(alpha = 0.2f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00CCFF))
                ) {
                    Text("SYSTEM_LOGIN", color = Color(0xFF00CCFF))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        CyberFrame(label = "ABOUT") {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(corpo.slogan, color = Color(0xFF00FF9F), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(corpo.profile, color = Color.White, fontSize = 12.sp)
                if (corpo.ceo.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("CEO: ${corpo.ceo}", color = Color(0xFF00CCFF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (corpo.highlights.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            CyberFrame(label = "TIMELINE") {
                Column(modifier = Modifier.padding(16.dp)) {
                    corpo.highlights.forEach { highlight ->
                        Text("• $highlight", color = Color.Gray, fontSize = 11.sp, modifier = Modifier.padding(bottom = 4.dp))
                    }
                }
            }
        }

        corpo.earningsReport?.let { report ->
            Spacer(modifier = Modifier.height(16.dp))
            CyberFrame(label = "QUARTERLY_EARNINGS") {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        report,
                        color = Color(0xFF00FF9F).copy(alpha = 0.8f),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        if (currentTier != DifficultyTier.BLACK_ICE) {
            Button(
                onClick = onAttack,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.2f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red)
            ) {
                Text("INITIATE_BREACH", color = Color.Red)
            }
        }
        
        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun FakeCorpoChart(modifier: Modifier) {
    Canvas(modifier = modifier) {
        val points = listOf(0.4f, 0.5f, 0.45f, 0.6f, 0.55f, 0.8f, 0.75f, 0.9f, 0.85f, 1f)
        val path = Path()
        val step = size.width / (points.size - 1)
        
        points.forEachIndexed { index, value ->
            val x = index * step
            val y = size.height - (value * size.height)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        
        drawPath(
            path = path,
            color = Color(0xFF00FF9F),
            style = Stroke(width = 2.dp.toPx())
        )
        
        // Fill area under
        val fillPath = android.graphics.Path(path.asAndroidPath())
        fillPath.lineTo(size.width, size.height)
        fillPath.lineTo(0f, size.height)
        fillPath.close()
        
        drawPath(
            path = fillPath.asComposePath(),
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF00FF9F).copy(alpha = 0.3f), Color.Transparent)
            )
        )
    }
}

@Composable
fun GamesArea(onChessClick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        CyberFrame(label = "CYBER_CHESS_V2") {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp).clickable { onChessClick() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Casino, contentDescription = null, tint = Color(0xFF00FF9F))
                Spacer(modifier = Modifier.width(16.dp))
                Text("ENTER_MATCHMAKING", color = Color.White)
            }
        }
    }
}

@Composable
fun CorpoLoginDialog(
    corpoName: String,
    onDismiss: () -> Unit,
    onLogin: (String, String) -> Boolean
) {
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        CyberFrame(label = "SECURE_LOGIN // $corpoName") {
            Column(
                modifier = Modifier
                    .background(Color(0xFF020508))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "AUTHORIZED_PERSONNEL_ONLY",
                    color = Color.Red,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                
                Spacer(modifier = Modifier.height(24.dp))

                TextField(
                    value = user,
                    onValueChange = { user = it; error = false },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("USERNAME", color = Color.Gray) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White.copy(alpha = 0.05f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                TextField(
                    value = pass,
                    onValueChange = { pass = it; error = false },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("EXECUTIVE_KEY", color = Color.Gray) },
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White.copy(alpha = 0.05f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                if (error) {
                    Text(
                        "INVALID_CREDENTIALS",
                        color = Color.Red,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray.copy(alpha = 0.2f))
                    ) {
                        Text("CANCEL")
                    }
                    Button(
                        onClick = {
                            if (!onLogin(user, pass)) {
                                error = true
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF9F).copy(alpha = 0.2f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00FF9F))
                    ) {
                        Text("LOGIN", color = Color(0xFF00FF9F))
                    }
                }
            }
        }
    }
}

@Composable
fun InvestorDeckOverlay(
    slides: List<com.neon.ascent.model.InvestorSlide>,
    onDismiss: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { slides.size })

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.9f))
            .clickable { onDismiss() } // Dimmed background click closes
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .clickable(enabled = false) { } // Prevent clicks through to background
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "INVESTOR_DECK // SLIDE ${pagerState.currentPage + 1}/${slides.size}",
                    color = Color(0xFF00CCFF),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close", tint = Color.Red)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { index ->
                val slide = slides[index]
                CyberFrame(
                    label = slide.title,
                    borderColor = Color(0xFF00CCFF).copy(alpha = 0.5f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            slide.title,
                            color = Color(0xFF00CCFF),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        slide.subtitle?.let {
                            Text(
                                it,
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            slide.body,
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 14.sp,
                            lineHeight = 22.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            // Indicator dots
            Row(
                Modifier
                    .height(8.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(slides.size) { iteration ->
                    val color = if (pagerState.currentPage == iteration) Color(0xFF00CCFF) else Color.Gray.copy(alpha = 0.5f)
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .clip(CircleShape)
                            .background(color)
                            .size(8.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

enum class NetworkTab { CHATS, SCAN, BIZ, NODES, GAMES }
