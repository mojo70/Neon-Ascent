package com.neon.ascent.feature.dashboard

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neon.ascent.model.Saying
import com.neon.ascent.ui.CyberFrame
import com.neon.ascent.ui.Scanlines

@Composable
fun CoreDashboardScreen(
    onBack: () -> Unit,
    viewModel: CoreDashboardViewModel = hiltViewModel()
) {
    var activeTab by remember { mutableStateOf(0) }
    val tabs = listOf("AI_CORE", "DATABANK", "ECONOMY", "LOGS")

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
                    "CORE_OS_DASHBOARD v2.0",
                    color = Color(0xFF00FF9C),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    "EXIT",
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
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Content Area
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (activeTab) {
                    0 -> AiParametersPanel(viewModel)
                    1 -> DatabankManager(viewModel)
                    2 -> EconomyHub(viewModel)
                    3 -> DiagnosticsLogs()
                }
            }
        }
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
            Text("%.2f$suffix".format(value), color = Color(0xFF00FF9C), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
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
fun EconomyHub(viewModel: CoreDashboardViewModel) {
    val char by viewModel.userCharacter.collectAsState()
    
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        CyberFrame(label = "XP_PROGRESSION") {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(modifier = Modifier.size(60.dp).border(2.dp, Color(0xFF00FF9C), RoundedCornerShape(30.dp)), contentAlignment = Alignment.Center) {
                    Text("${char?.level ?: 1}", color = Color.White, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("LEVEL ${char?.level ?: 1} OPERATIVE", color = Color.White, fontSize = 14.sp)
                    Text("XP: ${char?.experience ?: 0} / 5000", color = Color.Gray, fontSize = 10.sp)
                }
            }
        }
        
        CyberFrame(label = "CURRENCY_RESERVES") {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("€${char?.eddies ?: 0}", color = Color(0xFFFFCC00), fontSize = 32.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black)
                    if (char?.walletConnected == true) {
                        Text("(Solana ✓)", color = Color(0xFF00FF9C), fontSize = 10.sp)
                    } else {
                        Button(onClick = {}, colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)) {
                            Text("CONNECT PHANTOM", fontSize = 10.sp)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Button(onClick = {}, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF9C))) {
                    Text("CLAIM REWARDS", color = Color.Black)
                }
            }
        }

        CyberFrame(label = "QUICK_HACK_CONFIG") {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("AUTO-GHOST ICE (COST: 20 ED)", color = Color.White, fontSize = 12.sp)
                Switch(checked = true, onCheckedChange = {}, colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00FF9C)))
            }
        }
    }
}

@Composable
fun DiagnosticsLogs() {
    val logs = listOf(
        "Nano: 4.2 tokens/sec | Cloud latency: 180ms",
        "Memory Load: 42% | Temperature: 38°C",
        "Neural Link: Stable (98.4%)",
        "Connection: Encrypted (AES-256)",
        "Uptime: 04:22:15"
    )
    CyberFrame(label = "SYSTEM_DIAGNOSTICS") {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            logs.forEach { log ->
                Text("> $log", color = Color(0xFF00FF9C), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
        }
    }
}
