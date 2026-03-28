package com.neon.ascent.feature.biohacking

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.neon.ascent.model.BioProtocolLog
import com.neon.ascent.model.BiohackingData
import com.neon.ascent.model.UserCharacter
import com.neon.ascent.ui.*
import java.util.*

@Composable
fun BiohackingScreen(
    onBack: () -> Unit,
    viewModel: BiohackingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val characterState by viewModel.character.collectAsState()
    val logs by viewModel.logs.collectAsState()
    
    val displayChar = characterState ?: UserCharacter(
        name = "PROTAGONIST", sex = "NON_BINARY", dob = "2077", units = "METRIC", weight = "75", somatotype = 0.5f
    )

    var scanProgress by remember { mutableFloatStateOf(0f) }
    var isGenerating by remember { mutableStateOf(false) }
    var showReport by remember { mutableStateOf(false) }
    var showEffectivenessLogger by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    val neonCyan = Color(0xFF00F5FF)
    val neonMagenta = Color(0xFFFF0088)
    val voidBg = Color(0xFF0A0F14)

    LaunchedEffect(isGenerating) {
        if (isGenerating) {
            try {
                animate(0f, 1f, animationSpec = tween(1500, easing = LinearOutSlowInEasing)) { value, _ ->
                    scanProgress = value
                }
                showReport = true
                animate(1f, 0f, animationSpec = tween(500)) { value, _ ->
                    scanProgress = value
                }
            } finally {
                scanProgress = 0f
                isGenerating = false
            }
        }
    }

    // Privacy Onboarding Overlay
    if (!uiState.hasCompletedPrivacyOnboarding) {
        PrivacyOnboarding(
            onComplete = { anon, wearable, genetic ->
                viewModel.updateData { it.copy(
                    consentAnonymizedUpload = anon,
                    consentWearableSync = wearable,
                    consentGeneticData = genetic,
                    hasCompletedPrivacyOnboarding = true,
                    hasConsentedToDataProcessing = true
                ) }
            },
            neonCyan = neonCyan,
            neonMagenta = neonMagenta
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(voidBg)
            .cyberGlitch(intensity = scanProgress * 0.75f)
    ) {
        CyberGridBackground()
        FloatingParticles(intensity = displayChar.neuralLoad + (scanProgress * 0.5f))
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(scrollState)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "BIOHACKING_INTERFACE // V.2.3",
                        color = neonCyan,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        "COLLECTIVE_NET: ${if (uiState.consentAnonymizedUpload) "SYNC_ON" else "OFFLINE"}",
                        color = (if (uiState.consentAnonymizedUpload) neonCyan else neonMagenta).copy(alpha = 0.6f),
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                NeuralLoadGauge(load = displayChar.neuralLoad, modifier = Modifier.size(64.dp))
            }
            
            Spacer(modifier = Modifier.height(24.dp))

            // Main intake sections
            ExpandableBioSection("BASELINES_&_DEMOGRAPHICS", neonCyan) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BioReadOnlyField("AGE", "28", Modifier.weight(1f), neonCyan)
                        BioReadOnlyField("SEX", displayChar.sex, Modifier.weight(1f), neonCyan)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BioReadOnlyField("HEIGHT", "${displayChar.heightFeet ?: "0"}'${displayChar.heightInches ?: "0"}\"", Modifier.weight(1f), neonCyan)
                        BioReadOnlyField("WEIGHT", "${displayChar.weight}kg", Modifier.weight(1f), neonCyan)
                    }
                    BioSliderField("BODY_FAT_%", uiState.bodyFatPercentage ?: 15f, 5f..40f, neonCyan) {
                        viewModel.updateData { d -> d.copy(bodyFatPercentage = it) }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            CyberActionButton(
                label = "INITIATE AI_DEEP_SCAN",
                color = neonCyan,
                onClick = { if (!isGenerating) isGenerating = true }
            )

            // Dynamic Report Section
            if (showReport) {
                Spacer(modifier = Modifier.height(32.dp))
                ProtocolReport(neonMagenta, neonCyan) {
                    showEffectivenessLogger = true
                }
            }
            
            // Effectiveness Logger Overlay/Panel
            if (showEffectivenessLogger) {
                Spacer(modifier = Modifier.height(32.dp))
                EffectivenessLoggerPanel(
                    onLog = { e, s, m, f, se, n ->
                        viewModel.logProtocolEffectiveness(e, s, m, f, se, n, "PROTOCOL_X_77")
                        showEffectivenessLogger = false
                    },
                    neonCyan = neonCyan
                )
            }
            
            // Log History
            if (logs.isNotEmpty()) {
                Spacer(modifier = Modifier.height(32.dp))
                CyberFrame(label = "PROTOCOL_LOG_HISTORY", borderColor = neonCyan.copy(alpha = 0.5f)) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        logs.take(5).forEach { log ->
                            EffectivenessLogItem(log, neonCyan)
                        }
                    }
                }
            }
            
            // Stats Heatmap Mock
            if (uiState.consentAnonymizedUpload) {
                Spacer(modifier = Modifier.height(32.dp))
                CommunityStatsHeatmap(neonCyan)
            }

            Spacer(modifier = Modifier.height(64.dp))
        }

        if (scanProgress > 0f) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color.White, neonCyan.copy(alpha = 0.5f), Color.Transparent),
                        center = center,
                        radius = scanProgress * size.maxDimension
                    ),
                    radius = scanProgress * size.maxDimension,
                    alpha = (1f - scanProgress).coerceAtLeast(0f)
                )
            }
        }
    }
}

@Composable
fun PrivacyOnboarding(
    onComplete: (anon: Boolean, wearable: Boolean, genetic: Boolean) -> Unit,
    neonCyan: Color,
    neonMagenta: Color
) {
    var anon by remember { mutableStateOf(false) }
    var wearable by remember { mutableStateOf(false) }
    var genetic by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.95f)).zIndex(10f).padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        CyberFrame(label = "PRIVACY_ENCRYPTION_INIT", borderColor = neonCyan) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "Select your data sharing protocols. All data is end-to-end encrypted on-device.",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
                
                PrivacyToggle("Collective Neural Net (Anonymized Stats)", anon, neonCyan) { anon = it }
                PrivacyToggle("Wearable Auto-Sync", wearable, neonCyan) { wearable = it }
                PrivacyToggle("Genetic Data Personalization", genetic, neonMagenta) { genetic = it }
                
                Spacer(Modifier.height(8.dp))
                
                CyberActionButton("CONFIRM_PROTOCOLS", neonCyan) {
                    onComplete(anon, wearable, genetic)
                }
            }
        }
    }
}

@Composable
fun PrivacyToggle(label: String, checked: Boolean, color: Color, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(
            checked = checked, 
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(checkedColor = color, uncheckedColor = color.copy(alpha = 0.3f))
        )
        Text(label, color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
    }
}

@Composable
fun ProtocolReport(magenta: Color, cyan: Color, onLogClick: () -> Unit) {
    CyberFrame(label = "AI_GENERATED_PROTOCOL", borderColor = magenta, accentColor = cyan) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ReportCard("Nootropic: Bromantane + Tyrosine", "100mg / 500mg", "Dopamine synthesis up-regulation.", magenta, cyan)
            
            Box(Modifier.fillMaxWidth().background(magenta.copy(alpha = 0.1f)).padding(8.dp)) {
                Text(
                    "SAFETY_WARNING: Do not combine with SSRIs. Consult physician if heart rate exceeds 110bpm at rest.",
                    color = magenta,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Button(
                onClick = onLogClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = cyan.copy(alpha = 0.1f)),
                border = BorderStroke(1.dp, cyan)
            ) {
                Text("INITIATE EFFECTIVENESS_LOG", color = cyan, fontSize = 10.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
fun EffectivenessLoggerPanel(onLog: (Int, Int, Int, Int, String, String) -> Unit, neonCyan: Color) {
    var energy by remember { mutableFloatStateOf(5f) }
    var sleep by remember { mutableFloatStateOf(5f) }
    var notes by remember { mutableStateOf("") }

    CyberFrame(label = "LOG_RESULTS // PHASE_1", borderColor = neonCyan) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            BioSliderField("CURRENT_ENERGY", energy, 1f..10f, neonCyan) { energy = it }
            BioSliderField("SLEEP_QUALITY", sleep, 1f..10f, neonCyan) { sleep = it }
            
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("NOTES_FROM_THE_MATRIX", color = neonCyan.copy(alpha = 0.5f), fontSize = 10.sp) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodySmall.copy(color = Color.White, fontFamily = FontFamily.Monospace),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent, 
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = neonCyan,
                    unfocusedIndicatorColor = neonCyan.copy(alpha = 0.3f)
                )
            )
            
            CyberActionButton("LOG_TO_NEURAL_NET", neonCyan) {
                onLog(energy.toInt(), sleep.toInt(), 5, 5, "NONE", notes)
            }
        }
    }
}

@Composable
fun CommunityStatsHeatmap(cyan: Color) {
    CyberFrame(label = "COMMUNITY_INSIGHTS", borderColor = cyan.copy(alpha = 0.4f)) {
        Column {
            Text(
                "82% of similar users reported +15% Focus on this stack.",
                color = Color.White,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
            Spacer(Modifier.height(8.dp))
            // Mock Heatmap
            Row(Modifier.fillMaxWidth().height(4.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                repeat(20) { i ->
                    Box(Modifier.weight(1f).fillMaxHeight().background(if (i < 16) cyan else Color.Gray.copy(alpha = 0.2f)))
                }
            }
        }
    }
}

@Composable
fun EffectivenessLogItem(log: BioProtocolLog, cyan: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).background(Color(0xFF080808)).padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("LOG_${log.timestamp}", color = cyan, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
        Text("ENERGY: ${log.energyScore}", color = Color.White, fontSize = 9.sp)
    }
}

@Composable
fun ExpandableBioSection(label: String, color: Color, content: @Composable () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = color, fontSize = 12.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
            Icon(
                if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
        }
        
        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.padding(bottom = 16.dp)) {
                content()
            }
        }
        
        HorizontalDivider(color = color.copy(alpha = 0.2f), thickness = 0.5.dp)
    }
}

@Composable
fun BioReadOnlyField(label: String, value: String, modifier: Modifier = Modifier, color: Color) {
    Column(modifier = modifier) {
        Text(label, color = color.copy(alpha = 0.6f), fontSize = 8.sp, fontWeight = FontWeight.Bold)
        Text(value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
    }
}

@Composable
fun BioSliderField(label: String, value: Float, range: ClosedFloatingPointRange<Float>, color: Color, onValueChange: (Float) -> Unit) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = color.copy(alpha = 0.6f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(value.toInt().toString(), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(thumbColor = color, activeTrackColor = color, inactiveTrackColor = color.copy(alpha = 0.2f))
        )
    }
}

@Composable
fun BioInputField(label: String, value: String, color: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF050505))
            .border(0.5.dp, color.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
            .padding(12.dp)
    ) {
        Column {
            Text(label, color = color.copy(alpha = 0.6f), fontSize = 8.sp, fontWeight = FontWeight.Bold)
            Text(value, color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
fun UploadCard(label: String, modifier: Modifier = Modifier, color: Color) {
    Column(
        modifier = modifier
            .background(Color(0xFF080808))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.Add, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(8.dp))
        Text(label, color = color, fontSize = 10.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
    }
}

@Composable
fun ReportCard(title: String, detail: String, description: String, magenta: Color, cyan: Color) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF080808))
            .neonBorder(magenta.copy(alpha = 0.4f), width = 1.dp, cornerRadius = 8.dp)
            .padding(12.dp)
    ) {
        Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
        Text(detail, color = magenta, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        Spacer(Modifier.height(6.dp))
        Text(description, color = Color.Gray, fontSize = 11.sp, fontFamily = FontFamily.Monospace, lineHeight = 14.sp)
        
        Spacer(Modifier.height(14.dp))
        Button(
            onClick = {},
            modifier = Modifier.height(34.dp).align(Alignment.End),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF151515)),
            contentPadding = PaddingValues(horizontal = 14.dp),
            shape = RoundedCornerShape(4.dp)
        ) {
            Text("LOG EFFECTIVENESS", fontSize = 10.sp, color = cyan, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
        }
    }
}
