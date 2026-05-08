package com.neon.ascent.feature.cyberdeck

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.neon.ascent.model.SkillType
import com.neon.ascent.ui.CyberFrame
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
    
    val currentChallenge by viewModel.currentChallenge.collectAsState()
    val lastReward by viewModel.lastReward.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        if (selectedContact != null) {
            ChatDetailScreen(
                contactName = selectedContact!!,
                onBack = { selectedContact = null },
                viewModel = chatViewModel
            )
        } else if (selectedCorpo != null) {
            CorpoDetailView(
                corpo = selectedCorpo!!,
                onBack = { selectedCorpo = null },
                onAttack = { viewModel.startPcapChallenge(selectedCorpo!!.securityTier, SkillType.ANALYSIS) }
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
                        NetworkTab.CHATS -> ChatListArea(chatViewModel) { selectedContact = it }
                        NetworkTab.SCAN -> NetScanArea(hubViewModel, chatViewModel)
                        NetworkTab.BIZ -> BizScreen(stockViewModel, netWorthViewModel)
                        NetworkTab.NODES -> CorpoNodesArea(viewModel) { selectedCorpo = it }
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
    }
}

@Composable
fun ChatListArea(viewModel: ChatViewModel, onContactClick: (String) -> Unit) {
    val sessions by viewModel.chatSessions.collectAsState()
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
fun CorpoNodesArea(viewModel: CyberdeckViewModel, onCorpoClick: (CorpoNode) -> Unit) {
    val corpos = remember {
        listOf(
            CorpoNode(
                name = "AetherX",
                ticker = "AETX",
                slogan = "Our Heavy Starship doesn’t pull out. Uranus knows.",
                profile = "Founded in 2065 by ex-SpaceX renegades, AetherX is the premier megacorp for aggressive deep-space expansion. We deliver conquest. No safe orbits. No gentle landings. Just raw, unrelenting thrust.",
                securityTier = DifficultyTier.GHOST,
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
                """.trimIndent()
            ),
            CorpoNode(
                name = "Panopticon",
                ticker = "PNOT",
                slogan = "Do Evil",
                profile = "The world's largest surveillance and data collection agency. We don't just see everything; we predict your next thought before you have it. Privacy is a legacy bug we've patched.",
                securityTier = DifficultyTier.NETRUNNER,
                stockPrice = 1240.89,
                stockChange = -15.20,
                stockChangePercent = -1.2,
                marketCap = "§5.2 Trillion",
                ceo = "The Oversight Committee",
                highlights = listOf("2062 - Global Neural Integration", "2066 - Predicative Crime Unit launch")
            ),
            CorpoNode(
                name = "Microhard",
                ticker = "MHRD",
                slogan = "Your data, our dominion",
                profile = "Legacy tech giant turned digital overlord. Our operating systems run on 99% of all neural links. Resistance is a blue screen away.",
                securityTier = DifficultyTier.OPERATIVE,
                stockPrice = 456.12,
                stockChange = 5.43,
                stockChangePercent = 1.1,
                marketCap = "§3.1 Trillion",
                ceo = "Satya 'System' 2.0",
                highlights = listOf("2060 - Cloud Mind acquisition", "2070 - NeuralOS v14 forced update")
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

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(corpos) { corpo ->
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
                        Text("Security: ${corpo.securityTier}", color = when(corpo.securityTier) {
                            DifficultyTier.NOVICE -> Color.Green
                            DifficultyTier.OPERATIVE -> Color.Yellow
                            else -> Color.Red
                        }, fontSize = 10.sp)
                    }
                    Button(
                        onClick = { viewModel.startPcapChallenge(corpo.securityTier, SkillType.ANALYSIS) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.2f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red)
                    ) {
                        Text("ATTACK", color = Color.Red, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun CorpoDetailView(corpo: CorpoNode, onBack: () -> Unit, onAttack: () -> Unit) {
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
                        Text("${corpo.stockChangePercent}%", color = color)
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
        
        Button(
            onClick = onAttack,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.2f)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red)
        ) {
            Text("INITIATE_BREACH", color = Color.Red)
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

enum class NetworkTab { CHATS, SCAN, BIZ, NODES, GAMES }
