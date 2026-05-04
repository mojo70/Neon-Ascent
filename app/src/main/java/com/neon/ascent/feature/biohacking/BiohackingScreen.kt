package com.neon.ascent.feature.biohacking

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.neon.ascent.model.BioProtocolLog
import com.neon.ascent.model.BiohackingData
import com.neon.ascent.model.UserCharacter
import com.neon.ascent.ui.*
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.util.*

@Composable
fun BiohackingScreen(
    onBack: () -> Unit,
    viewModel: BiohackingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val characterState by viewModel.character.collectAsState()
    val logs by viewModel.logs.collectAsState()
    val isNeuralCoreThinking by viewModel.isNeuralCoreThinking.collectAsState()
    
    val activeAiType by viewModel.activeAiType.collectAsState()
    val isLocalAiAvailable = activeAiType == AiType.LOCAL
    val downloadProgress by viewModel.modelDownloadManager.downloadProgress.collectAsState()
    val isDownloading by viewModel.modelDownloadManager.isDownloading.collectAsState()
    val measurementUnit by viewModel.measurementUnit.collectAsState()

    val displayChar = characterState ?: UserCharacter(
        name = "PROTAGONIST", sex = "NON_BINARY", dob = "2077.01.01", units = measurementUnit, weight = "75", somatotype = 0.5f
    )

    var selectedSector by remember { mutableStateOf("CORE_CHASSIS") }
    var scanProgress by remember { mutableFloatStateOf(0f) }
    var showReport by remember { mutableStateOf(false) }
    var showEffectivenessLogger by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    val neonCyan = Color(0xFF00F5FF)
    val neonMagenta = Color(0xFFFF0088)
    val voidBg = Color(0xFF0A0F14)

    LaunchedEffect(isNeuralCoreThinking) {
        if (isNeuralCoreThinking) {
            animate(0f, 1f, animationSpec = tween(2000, easing = LinearOutSlowInEasing)) { value, _ ->
                scanProgress = value
            }
        } else if (scanProgress > 0f) {
            showReport = true
            animate(1f, 0f, animationSpec = tween(500)) { value, _ ->
                scanProgress = value
            }
        }
    }

    // Privacy Onboarding Overlay
    if (!uiState.hasCompletedPrivacyOnboarding) {
        PrivacyOnboarding(
            onComplete = { anon, wearable, genetic, neuralCore ->
                viewModel.updateData { it.copy(
                    consentAnonymizedUpload = anon,
                    consentWearableSync = wearable,
                    consentGeneticData = genetic,
                    enableOnDeviceNeuralCore = neuralCore,
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
                .statusBarsPadding()
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "COLLECTIVE_NET: ${if (uiState.consentAnonymizedUpload) "SYNC_ON" else "OFFLINE"}",
                            color = (if (uiState.consentAnonymizedUpload) neonCyan else neonMagenta).copy(alpha = 0.6f),
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        if (uiState.enableOnDeviceNeuralCore) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "SYNAPSE_STATUS: LOCAL // NO NETWORK",
                                color = neonCyan,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier
                                    .background(neonCyan.copy(alpha = 0.1f))
                                    .padding(horizontal = 4.dp)
                            )
                        }
                    }
                }
                NeuralLoadGauge(
                    load = if (isNeuralCoreThinking) 0.9f else displayChar.neuralLoad, 
                    modifier = Modifier.size(64.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))

            // Hero: Full Body Holographic Selector
            CyberFrame(label = "HOLOGRAPHIC_SELECTOR", borderColor = neonCyan) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp),
                    contentAlignment = Alignment.Center
                ) {
                    FullBodyHologram(
                        character = displayChar,
                        neuralLoad = displayChar.neuralLoad,
                        selectedSector = selectedSector,
                        onSectorSelected = { 
                            selectedSector = it
                            if (uiState.enableOnDeviceNeuralCore) {
                                viewModel.initiateLocalScan(it)
                            }
                        },
                        modifier = Modifier.fillMaxSize(0.9f),
                        highlightColor = if (isNeuralCoreThinking) neonMagenta else neonCyan
                    )
                    
                    SectorLabel("COGNITION", Alignment.TopEnd, selectedSector == "CRANIAL_NODE", neonCyan, neonMagenta)
                    SectorLabel("ENDOCRINE", Alignment.CenterStart, selectedSector == "CORE_CHASSIS", neonCyan, neonMagenta)
                    SectorLabel("RECOVERY", Alignment.BottomEnd, selectedSector == "MOTOR_EXTREMITIES", neonCyan, neonMagenta)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Baselines Section
            ExpandableBioSection("BASELINES_&_DEMOGRAPHICS", neonCyan) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BioReadOnlyField("AGE", calculateAge(displayChar.dob), Modifier.weight(1f), neonCyan)
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

            // Wearable Metrics
            if (uiState.isWearableSynced) {
                ExpandableBioSection("WEARABLE_METRICS", neonCyan) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BioReadOnlyField("HEART_RATE", "${uiState.currentHeartRate ?: "--"} BPM", Modifier.weight(1f), neonCyan)
                        BioReadOnlyField("STEPS_TODAY", "${uiState.currentSteps ?: "--"}", Modifier.weight(1f), neonCyan)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "LAST_SYNC: ${uiState.lastSyncTimestamp?.let { java.time.Instant.ofEpochMilli(it).toString() } ?: "NEVER"}",
                        color = Color.Gray,
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            } else {
                CyberActionButton(
                    label = "SYNC_WEARABLE_DATA",
                    color = neonCyan.copy(alpha = 0.5f),
                    onClick = { viewModel.syncWearable() }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Lifestyle Stack
            ExpandableBioSection("LIFESTYLE_STACK", neonCyan) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    BioSliderField("ENERGY_SCORE", uiState.energyScore.toFloat(), 1f..10f, neonCyan) {
                        viewModel.updateData { d -> d.copy(energyScore = it.toInt()) }
                    }
                    BioSliderField("MOOD_SCORE", uiState.moodScore.toFloat(), 1f..10f, neonCyan) {
                        viewModel.updateData { d -> d.copy(moodScore = it.toInt()) }
                    }
                    BioSliderField("FOCUS_SCORE", uiState.focusScore.toFloat(), 1f..10f, neonCyan) {
                        viewModel.updateData { d -> d.copy(focusScore = it.toInt()) }
                    }
                    BioSliderField("SLEEP_DURATION (HRS)", uiState.sleepHours ?: 7f, 4f..12f, neonCyan) {
                        viewModel.updateData { d -> d.copy(sleepHours = it) }
                    }
                    BioSliderField("STRESS_LEVEL", uiState.stressLevel.toFloat(), 1f..10f, neonCyan) {
                        viewModel.updateData { d -> d.copy(stressLevel = it.toInt()) }
                    }
                    BioInputField("SUPPLEMENT_STACK", uiState.supplements ?: "CREATINE, VIT_D, ASHWAGANDHA", neonCyan)
                    BioInputField("DIET_TYPE", uiState.dietType ?: "KETO_OMAD", neonCyan)
                }
            }

            // Goals & Constraints
            ExpandableBioSection("GOALS_&_CONSTRAINTS", neonCyan) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    BioInputField("PRIMARY_OBJECTIVE", uiState.primaryObjective ?: "LONGEVITY", neonCyan)
                    BioInputField("CONTRAINDICATIONS", uiState.contraindications ?: "NONE_DETECTED", neonCyan)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = uiState.pregnancyFlag,
                            onCheckedChange = { viewModel.updateData { d -> d.copy(pregnancyFlag = it) } },
                            colors = CheckboxDefaults.colors(checkedColor = neonMagenta)
                        )
                        Text("PREGNANCY_FLAG", color = neonMagenta, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Lab & Genetic Uploads
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                UploadCard("LAB_RESULTS_PDF", Modifier.weight(1f), neonCyan)
                UploadCard("GENETIC_RAW_DATA", Modifier.weight(1f), neonMagenta)
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (!isLocalAiAvailable) {
                CyberFrame(label = "NEURAL_ENGINE_MISSING", borderColor = neonMagenta) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Local AI capabilities require the Neural Engine core (Gemma 2B). Download now to enable secure, on-device biohacking protocols.",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        if (isDownloading) {
                            LinearProgressIndicator(
                                progress = { downloadProgress ?: 0f },
                                modifier = Modifier.fillMaxWidth().height(2.dp),
                                color = neonCyan,
                                trackColor = neonCyan.copy(alpha = 0.1f)
                            )
                            Text(
                                "DOWNLOADING_CORE...",
                                color = neonCyan,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        } else {
                            CyberActionButton(
                                label = "DOWNLOAD NEURAL_ENGINE (Gemma 4-E2B)",
                                color = neonCyan,
                                onClick = { viewModel.modelDownloadManager.startDownload() }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            CyberActionButton(
                label = if (uiState.enableOnDeviceNeuralCore) "INITIATE NEURAL_CORE_SCAN" else "INITIATE AI_DEEP_SCAN",
                color = if (isLocalAiAvailable || !uiState.enableOnDeviceNeuralCore) neonCyan else Color.Gray,
                enabled = isLocalAiAvailable || !uiState.enableOnDeviceNeuralCore,
                onClick = { viewModel.initiateLocalScan(selectedSector) }
            )

            // Dynamic Report Section
            if (showReport) {
                Spacer(modifier = Modifier.height(32.dp))
                ProtocolReport(
                    report = viewModel.latestReport.collectAsState().value,
                    magenta = neonMagenta, 
                    cyan = neonCyan
                ) {
                    showEffectivenessLogger = true
                }
            }
            
            // Effectiveness Logger Panel
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

        // Thinking Animation
        if (isNeuralCoreThinking) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f))
                    .zIndex(20f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    GlitchText(
                        text = "NEURAL_CORE THINKING...",
                        color = neonCyan,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    LinearProgressIndicator(
                        progress = { scanProgress },
                        modifier = Modifier
                            .width(240.dp)
                            .height(2.dp),
                        color = neonCyan,
                        trackColor = neonCyan.copy(alpha = 0.1f),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "UPLINKING_LOCAL_SYNAPSE",
                        color = neonMagenta,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.alpha(pulseAlpha())
                    )
                }
            }
        }

        if (scanProgress > 0f && !isNeuralCoreThinking) {
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

fun calculateAge(dob: String): String {
    return try {
        val formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")
        val birthDate = LocalDate.parse(dob, formatter)
        val currentDate = LocalDate.now()
        Period.between(birthDate, currentDate).years.toString()
    } catch (e: Exception) {
        "ERR"
    }
}

@Composable
private fun pulseAlpha(): Float {
    val infiniteTransition = rememberInfiniteTransition(label = "Pulse")
    return infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Alpha"
    ).value
}

@Composable
fun FullBodyHologram(
    character: UserCharacter?,
    neuralLoad: Float,
    selectedSector: String,
    onSectorSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    highlightColor: Color = Color(0xFF00F5FF)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "HoloPulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Pulse"
    )

    Box(modifier = modifier.pointerInput(Unit) {
        detectTapGestures { offset ->
            val yPercent = offset.y / size.height
            when {
                yPercent < 0.25f -> onSectorSelected("CRANIAL_NODE")
                yPercent < 0.65f -> onSectorSelected("CORE_CHASSIS")
                else -> onSectorSelected("MOTOR_EXTREMITIES")
            }
        }
    }) {
        AvatarImage(
            character = character,
            modifier = Modifier.fillMaxSize().alpha(0.6f),
            alpha = 0.6f
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            val magenta = Color(0xFFFF0088)
            val w = size.width
            val h = size.height
            
            // Interactive Sector Highlighting
            when (selectedSector) {
                "CRANIAL_NODE" -> {
                    drawCircle(
                        color = highlightColor.copy(alpha = 0.4f * pulse),
                        center = Offset(w * 0.5f, h * 0.12f),
                        radius = w * 0.2f,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
                "CORE_CHASSIS" -> {
                    drawRect(
                        color = highlightColor.copy(alpha = 0.3f * pulse),
                        topLeft = Offset(w * 0.25f, h * 0.25f),
                        size = Size(w * 0.5f, h * 0.35f),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
                "MOTOR_EXTREMITIES" -> {
                    drawLine(
                        color = highlightColor.copy(alpha = 0.5f * pulse),
                        start = Offset(w * 0.2f, h * 0.8f),
                        end = Offset(w * 0.8f, h * 0.8f),
                        strokeWidth = 4.dp.toPx()
                    )
                }
            }

            // Scanlines / Data Particles
            repeat(12) { i ->
                val lineY = (h * 0.05f) + (i * h * 0.08f)
                drawLine(
                    color = highlightColor.copy(alpha = 0.1f),
                    start = Offset(0f, lineY),
                    end = Offset(w, lineY),
                    strokeWidth = 1f
                )
            }
        }
    }
}

@Composable
fun BoxScope.SectorLabel(label: String, alignment: Alignment, isSelected: Boolean, cyan: Color, magenta: Color) {
    val alpha by animateFloatAsState(if (isSelected) 1f else 0.3f, label = "Alpha")
    val color by animateColorAsState(if (isSelected) magenta else cyan, label = "Color")
    
    Text(
        text = label,
        color = color,
        fontSize = 10.sp,
        fontWeight = FontWeight.Black,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier
            .align(alignment)
            .padding(12.dp)
            .alpha(alpha)
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

@Composable
fun PrivacyOnboarding(
    onComplete: (anon: Boolean, wearable: Boolean, genetic: Boolean, neuralCore: Boolean) -> Unit,
    neonCyan: Color,
    neonMagenta: Color
) {
    var anon by remember { mutableStateOf(false) }
    var wearable by remember { mutableStateOf(false) }
    var genetic by remember { mutableStateOf(false) }
    var neuralCore by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.95f)).zIndex(30f).padding(24.dp),
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
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, neonCyan.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                        .background(neonCyan.copy(alpha = 0.05f))
                        .padding(8.dp)
                ) {
                    Column {
                        PrivacyToggle("Enable ON-DEVICE NEURAL_CORE", neuralCore, neonCyan) { neuralCore = it }
                        Text(
                            "(100% private, zero data leaves device)",
                            color = neonCyan.copy(alpha = 0.7f),
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(start = 32.dp)
                        )
                    }
                }
                
                Spacer(Modifier.height(8.dp))
                
                CyberActionButton("CONFIRM_PROTOCOLS", neonCyan) {
                    onComplete(anon, wearable, genetic, neuralCore)
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
fun ProtocolReport(report: String?, magenta: Color, cyan: Color, onLogClick: () -> Unit) {
    CyberFrame(label = "AI_GENERATED_PROTOCOL", borderColor = magenta, accentColor = cyan) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (report == null) {
                Text("ANALYZING_SYNAPTIC_INPUTS...", color = Color.Gray, fontSize = 12.sp)
            } else {
                Text(
                    report,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            Box(Modifier.fillMaxWidth().background(magenta.copy(alpha = 0.1f)).padding(8.dp)) {
                Text(
                    "SAFETY_WARNING: CORE_GENERATED_ADVICE. VALIDATE WITH SYSTEM_MEDIC.",
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
