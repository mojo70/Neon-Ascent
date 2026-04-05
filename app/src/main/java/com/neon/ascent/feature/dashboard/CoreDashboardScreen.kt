package com.neon.ascent.feature.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.neon.ascent.ui.CyberFrame
import com.neon.ascent.ui.Scanlines

@Composable
fun CoreDashboardScreen(
    onBack: () -> Unit,
    onTriggerHack: (String) -> Unit,
    viewModel: CoreDashboardViewModel = hiltViewModel(),
    unlockedSectionFromResult: String? = null,
    onUnlockConsumed: () -> Unit = {}
) {
    var activeTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("VAULT", "LOGS", "AI_CORE", "DATABANK")
    val userCharacter by viewModel.userCharacter.collectAsState()
    val isRooted = userCharacter?.isSystemDatabaseUnlocked == true
    
    val aiSessionUnlocked by viewModel.aiCoreSessionUnlocked.collectAsState()
    val dataSessionUnlocked by viewModel.databankSessionUnlocked.collectAsState()

    // Handle session unlock from navigation result
    LaunchedEffect(unlockedSectionFromResult) {
        unlockedSectionFromResult?.let { section ->
            viewModel.sessionUnlock(section)
            // Automatically switch to the tab that was just unlocked
            val index = tabs.indexOf(section)
            if (index != -1) {
                activeTab = index
            }
            onUnlockConsumed()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Scanlines()
        
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "LOCAL_AI_CORE v3.0",
                    color = Color(0xFF00FF9C),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    "DISCONNECT",
                    color = Color.Red,
                    modifier = Modifier.clickable { onBack() },
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
            }

            Spacer(Modifier.height(16.dp))

            // Tab Bar
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                tabs.forEachIndexed { index, title ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .background(if (activeTab == index) Color(0xFF00FF9C).copy(alpha = 0.2f) else Color.Transparent)
                            .border(1.dp, if (activeTab == index) Color(0xFF00FF9C) else Color.Gray.copy(alpha = 0.3f))
                            .clickable { activeTab = index },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            title,
                            color = if (activeTab == index) Color(0xFF00FF9C) else Color.Gray,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Content Area
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                val currentTabName = tabs[activeTab]
                
                when (currentTabName) {
                    "VAULT" -> VaultHub(viewModel)
                    "LOGS" -> DiagnosticsLogs(viewModel)
                    "AI_CORE" -> {
                        if (!isRooted) {
                            BlackIceOverlay { onTriggerHack("ROOT") }
                        } else if (!aiSessionUnlocked) {
                            LightIceOverlay(
                                section = "AI_CORE",
                                onQuickHack = { viewModel.quickUnlock("AI_CORE") },
                                onManualHack = { onTriggerHack("AI_CORE") }
                            )
                        } else {
                            AiParametersPanel(viewModel)
                        }
                    }
                    "DATABANK" -> {
                        if (!isRooted) {
                            BlackIceOverlay { onTriggerHack("ROOT") }
                        } else if (!dataSessionUnlocked) {
                            LightIceOverlay(
                                section = "DATABANK",
                                onQuickHack = { viewModel.quickUnlock("DATABANK") },
                                onManualHack = { onTriggerHack("DATABANK") }
                            )
                        } else {
                            DatabankManager(viewModel)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BlackIceOverlay(onTriggerHack: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().border(2.dp, Color.Red.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
            Icon(Icons.Default.Lock, contentDescription = null, tint = Color.Red, modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(16.dp))
            Text(
                "BLACK_ICE_DETECTED",
                color = Color.Red,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 20.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Access restricted. Arasaka-grade ICE detected. Root breach protocol required for decryption.",
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onTriggerHack,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) {
                Text("INITIATE ROOT BREACH", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun LightIceOverlay(section: String, onQuickHack: () -> Unit, onManualHack: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().border(1.dp, Color(0xFF00CCFF).copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
            Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF00CCFF), modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(16.dp))
            Text(
                "LIGHT_ICE_ACTIVE",
                color = Color(0xFF00CCFF),
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 18.sp
            )
            Text(
                "Section: $section",
                color = Color.Gray,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onManualHack,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00CCFF))
            ) {
                Text("BREACH NODE", color = Color.Black)
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onQuickHack,
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, Color(0xFF00FF9C))
            ) {
                Text("QUICKHACK (20 ED)", color = Color(0xFF00FF9C))
            }
        }
    }
}

@Composable
fun VaultHub(viewModel: CoreDashboardViewModel) {
    val char by viewModel.userCharacter.collectAsState()
    val isNetrunnerOn by viewModel.isNetrunnerMode.collectAsState()
    var showTransferDialog by remember { mutableStateOf(false) }
    
    if (showTransferDialog) {
        TransferToSecureDialog(
            maxAmount = char?.eddies ?: 0,
            onDismiss = { showTransferDialog = false },
            onConfirm = { amount ->
                viewModel.transferToSecure(amount)
                showTransferDialog = false
            }
        )
    }

    Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // NETRUNNER TOGGLE
        CyberFrame(label = "SYSTEM_STATE") {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text("NETRUNNER MODE", color = if (isNetrunnerOn) Color.Red else Color.Gray, fontWeight = FontWeight.Bold)
                        Text("Exposes kernel to subnet", color = Color.Gray, fontSize = 10.sp)
                    }
                    Switch(
                        checked = isNetrunnerOn, 
                        onCheckedChange = { viewModel.toggleNetrunnerMode(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.Red, checkedTrackColor = Color.Red.copy(alpha = 0.3f))
                    )
                }
                if (isNetrunnerOn) {
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Red, modifier = Modifier.size(12.dp))
                        Text("RISK LEVEL: CRITICAL - HIGH TRACE PROBABILITY", color = Color.Red, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }

        // XP & EDDIES
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            CyberFrame(label = "XP", modifier = Modifier.weight(1f)) {
                Column {
                    Text("LVL ${char?.level ?: 1}", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    LinearProgressIndicator(
                        progress = { (char?.experience?.toFloat() ?: 0f) / 5000f },
                        modifier = Modifier.fillMaxWidth().height(4.dp),
                        color = Color(0xFF00FF9C),
                        trackColor = Color.Gray.copy(alpha = 0.2f),
                    )
                }
            }
            CyberFrame(label = "VAULT", modifier = Modifier.weight(1.5f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text("€${char?.eddies ?: 0}", color = Color(0xFFFFCC00), fontSize = 18.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                        Text(if (char?.walletConnected == true) "SOLANA: LINKED" else "UNLINKED", color = Color.Gray, fontSize = 9.sp)
                    }
                    if (char?.walletConnected == true) {
                        IconButton(onClick = { showTransferDialog = true }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Shield, contentDescription = "Transfer to Secure", tint = Color(0xFF00FF9C), modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }

        // ICE CONFIG
        val alpha = if (isNetrunnerOn) 1f else 0.3f
        CyberFrame(label = "ICE_CONFIG", modifier = Modifier.alpha(alpha)) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                IceOption("WHITE_NOISE_PULSE", "Standard defensive layer", true, isNetrunnerOn)
                IceOption("SYNAPTIC_FEEDBACK", "Counter-hack payload", false, isNetrunnerOn)
                IceOption("GHOST_PROTOCOL", "Reduces trace speed", false, isNetrunnerOn)
            }
        }

        // DEAD DROP
        CyberFrame(label = "DEAD_DROP_MESSAGES", modifier = Modifier.alpha(alpha)) {
            Column {
                var message by remember { mutableStateOf("") }
                BasicTextField(
                    value = message,
                    onValueChange = { if (it.length <= 140) message = it },
                    enabled = isNetrunnerOn,
                    textStyle = TextStyle(color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                    modifier = Modifier.fillMaxWidth().height(60.dp).background(Color.White.copy(alpha = 0.05f)).padding(8.dp),
                    decorationBox = { innerTextField ->
                        if (message.isEmpty()) Text("Leave graffiti for other runners...", color = Color.Gray, fontSize = 11.sp)
                        innerTextField()
                    }
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${message.length}/140", color = Color.Gray, fontSize = 9.sp)
                    Text("UPLOAD", color = if (isNetrunnerOn) Color(0xFF00FF9C) else Color.Gray, fontSize = 10.sp, modifier = Modifier.clickable(enabled = isNetrunnerOn) {})
                }
            }
        }
    }
}

@Composable
fun TransferToSecureDialog(maxAmount: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var amountText by remember { mutableStateOf("") }
    val amount = amountText.toIntOrNull() ?: 0
    val isValid = amount in 1..maxAmount

    Dialog(onDismissRequest = onDismiss) {
        CyberFrame(label = "SECURE_TRANSFER_PROTOCOL") {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Transfer Eddies to Solana Secure Wallet?", color = Color.White, fontSize = 14.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                Text("AVAILABLE: €$maxAmount", color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                Spacer(Modifier.height(8.dp))
                BasicTextField(
                    value = amountText,
                    onValueChange = { if (it.all { char -> char.isDigit() }) amountText = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = TextStyle(color = Color(0xFFFFCC00), fontFamily = FontFamily.Monospace, fontSize = 18.sp, textAlign = TextAlign.Center),
                    cursorBrush = SolidColor(Color(0xFF00FF9C)),
                    modifier = Modifier.fillMaxWidth().background(Color.White.copy(alpha = 0.05f)).padding(12.dp),
                    decorationBox = { innerTextField ->
                        if (amountText.isEmpty()) Text("ENTER_AMOUNT", color = Color.Gray.copy(alpha = 0.5f), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontSize = 14.sp)
                        innerTextField()
                    }
                )
                Spacer(Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                    ) {
                        Text("CANCEL", color = Color.White, fontSize = 10.sp)
                    }
                    Button(
                        onClick = { onConfirm(amount) },
                        enabled = isValid,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF9C))
                    ) {
                        Text("TRANSFER", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun IceOption(name: String, desc: String, enabled: Boolean, systemActive: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(name, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(desc, color = Color.Gray, fontSize = 9.sp)
        }
        Checkbox(
            checked = enabled, 
            onCheckedChange = {}, 
            enabled = systemActive,
            colors = CheckboxDefaults.colors(checkedColor = Color(0xFF00FF9C), uncheckedColor = Color.Gray)
        )
    }
}

@Composable
fun AiParametersPanel(viewModel: CoreDashboardViewModel) {
    val temp by viewModel.nanoTemperature.collectAsState()
    val fallback by viewModel.cloudFallbackThreshold.collectAsState()
    val seed by viewModel.philosophySeed.collectAsState()
    
    var model by remember { mutableStateOf("Gemini Nano") }
    val philosophyOptions = listOf("PLATO", "SOCRATES", "MARCUS_AURELIUS", "NIETZSCHE", "BUDDHA")
    
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        CyberFrame(label = "MODEL_SELECTOR") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ModelOption("Gemini Nano (Local)", "Private, offline, zero-latency ghost", model == "Gemini Nano") { model = "Gemini Nano" }
                ModelOption("Hybrid Cloud (Vertex)", "Overclocked for deeper runs", model == "Hybrid Cloud") { model = "Hybrid Cloud" }
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        CyberFrame(label = "SYNAPTIC_CONTROLS") {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                LabeledSlider("NANO_TEMPERATURE", temp, 0f..2f) { viewModel.updateNanoTemperature(it) }
                LabeledSlider("CLOUD_FALLBACK_THRESHOLD", fallback, 0f..100f, suffix = "%") { viewModel.updateCloudFallback(it) }
                
                Text("PHILOSOPHY_SEED", color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    philosophyOptions.forEach { option ->
                        val isSelected = seed == option
                        Box(
                            modifier = Modifier
                                .border(1.dp, if (isSelected) Color(0xFF00FF9C) else Color.Gray.copy(alpha = 0.3f))
                                .background(if (isSelected) Color(0xFF00FF9C).copy(alpha = 0.2f) else Color.Transparent)
                                .clickable { viewModel.updatePhilosophySeed(option) }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(option, color = if (isSelected) Color(0xFF00FF9C) else Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        CyberFrame(label = "LIVE_TEST_PROMPT") {
            Column {
                var testPrompt by remember { mutableStateOf("") }
                BasicTextField(
                    value = testPrompt,
                    onValueChange = { testPrompt = it },
                    textStyle = TextStyle(color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                    cursorBrush = SolidColor(Color(0xFF00FF9C)),
                    modifier = Modifier.fillMaxWidth().height(80.dp).background(Color.White.copy(alpha = 0.05f)).padding(8.dp),
                    decorationBox = { innerTextField ->
                        if (testPrompt.isEmpty()) Text("Type neural probe here...", color = Color.Gray, fontSize = 12.sp)
                        innerTextField()
                    }
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {}, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF9C))) {
                        Text("RUN_NANO", color = Color.Black, fontSize = 10.sp)
                    }
                    Button(onClick = {}, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00CCFF))) {
                        Text("RUN_CLOUD", color = Color.Black, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ModelOption(title: String, desc: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(8.dp)
            .border(1.dp, if (selected) Color(0xFF00FF9C) else Color.Transparent)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick, colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF00FF9C)))
        Column {
            Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(desc, color = Color.Gray, fontSize = 10.sp)
        }
    }
}

@Composable
fun LabeledSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, suffix: String = "", onValueChange: (Float) -> Unit) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            Text("%.2f".format(value) + suffix, color = Color(0xFF00FF9C), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(thumbColor = Color(0xFF00FF9C), activeTrackColor = Color(0xFF00FF9C))
        )
    }
}

@Composable
fun DatabankManager(viewModel: CoreDashboardViewModel) {
    val sayings by viewModel.allSayings.collectAsState()
    var newSayingText by remember { mutableStateOf("") }
    
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        CyberFrame(label = "CUSTOM_ENTRY") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BasicTextField(
                    value = newSayingText,
                    onValueChange = { newSayingText = it },
                    textStyle = TextStyle(color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                    modifier = Modifier.weight(1f).background(Color.White.copy(alpha = 0.05f)).padding(8.dp),
                    decorationBox = { innerTextField ->
                        if (newSayingText.isEmpty()) Text("Add custom wisdom...", color = Color.Gray, fontSize = 12.sp)
                        innerTextField()
                    }
                )
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { if (newSayingText.isNotBlank()) { viewModel.addCustomSaying(newSayingText); newSayingText = "" } },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF9C))
                ) {
                    Text("+", color = Color.Black)
                }
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        CyberFrame(label = "SAYINGS_ARCHIVE") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                sayings.forEach { saying ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "> \"${saying.text}\"", 
                            color = if (saying.category == "Custom") Color(0xFF00FF9C) else Color.Gray, 
                            fontSize = 11.sp, 
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { viewModel.deleteSaying(saying) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        Button(
            onClick = { viewModel.seedNano() }, 
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFCC00))
        ) {
            Text("SEED NANO CORE", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun DiagnosticsLogs(viewModel: CoreDashboardViewModel) {
    val hackHistory by viewModel.hackHistory.collectAsState()
    val isNetrunnerOn by viewModel.isNetrunnerMode.collectAsState()

    val systemLogs = listOf(
        "Nano: 4.2 tokens/sec | Cloud latency: 180ms",
        "Memory Load: 42% | Temperature: 38°C",
        "Neural Link: Stable (98.4%)",
        "Uptime: 04:22:15"
    )
    
    Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        CyberFrame(label = "SYSTEM_DIAGNOSTICS") {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                systemLogs.forEach { log ->
                    Text("> $log", color = Color(0xFF00FF9C), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }

        CyberFrame(label = "HACK_HISTORY_L60M") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (hackHistory.isEmpty()) {
                    Text("NO RECENT ACTIVITY", color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                } else {
                    hackHistory.forEach { event ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, if (isNetrunnerOn) Color.Red.copy(alpha = 0.3f) else Color.Gray.copy(alpha = 0.1f))
                                .clickable(enabled = isNetrunnerOn) { viewModel.claimBounty(event) }
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(event.type, color = if (isNetrunnerOn) Color.Red else Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text(event.details, color = Color.Gray, fontSize = 9.sp)
                            }
                            if (isNetrunnerOn) {
                                Text("CLAIM €${event.bounty}", color = Color(0xFFFFCC00), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
