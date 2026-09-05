package com.neon.ascent.feature.biohacking

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.draw.scale
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
import androidx.health.connect.client.PermissionController
import com.neon.ascent.core.common.*
import com.neon.ascent.core.domain.model.SpecialType
import com.neon.ascent.feature.health.domain.uplink.*
import com.neon.ascent.feature.health.data.workers.HealthSyncWorker
import com.neon.ascent.feature.health.data.services.LiveBiometricService
import com.neon.ascent.core.domain.character.models.UserCharacter
import com.neon.ascent.model.BioProtocolLog
import com.neon.ascent.model.BiohackingData
import com.neon.ascent.ui.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.util.*

import androidx.compose.material.icons.automirrored.filled.LibraryBooks

@Composable
fun BiohackingScreen(
    onBack: () -> Unit,
    focus: String? = null,
    onNavigateToForge: (SpecialType, String?, String?, String?) -> Unit = { _, _, _, _ -> },
    onNavigateToGuide: (String?) -> Unit = {},
    onNavigateToDopamineMenu: () -> Unit = {},
    onNavigateToOps: () -> Unit = {},
    onRelink: (UplinkProvider) -> Unit = {},
    viewModel: BiohackingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val trends by viewModel.trends.collectAsState()
    val characterState by viewModel.character.collectAsState()
    val logs by viewModel.logs.collectAsState()
    val isNeuralCoreThinking by viewModel.isNeuralCoreThinking.collectAsState()
    
    val activeAiType by viewModel.activeAiType.collectAsState()
    val isLocalAiAvailable = activeAiType == AiType.LOCAL
    val downloadProgress by viewModel.modelDownloadManager.downloadProgress.collectAsState()
    val isDownloading by viewModel.modelDownloadManager.isDownloading.collectAsState()
    val measurementUnit by viewModel.measurementUnit.collectAsState()
    val neuralInsights by viewModel.neuralInsights.collectAsState()
    val uplinkSyncStatuses by viewModel.uplinkSyncStatuses.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val showPermissionRationale by viewModel.showPermissionRationale.collectAsState()
    val permissionsRationale by viewModel.permissionsRationale.collectAsState()
    val isLiveMonitoringEnabled by viewModel.liveMonitoringEnabled.collectAsState()
    val macros by viewModel.macros.collectAsState()
    val vitalsSnapshot by viewModel.vitalsSnapshot.collectAsState()
    val rhrSeries by viewModel.rhrSeries.collectAsState()
    val hrvSeries by viewModel.hrvSeries.collectAsState()
    val recoveryScore by viewModel.recoveryScore.collectAsState()
    val neonCharge by viewModel.neonCharge.collectAsState()
    val hasNutritionPermission by viewModel.hasNutritionPermission.collectAsState()
    val completedSessions by viewModel.completedSessionsThisWeek.collectAsState()
    val scheduledSessions by viewModel.scheduledSessionsThisWeek.collectAsState()
    val selectedTimeRange by viewModel.selectedTimeRange.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(isSyncing) {
        if (isSyncing) {
            snackbarHostState.showSnackbar("Neural Uplink: Manual sync initiated...")
        }
    }

    LaunchedEffect(uiState.isWearableSynced) {
        if (uiState.isWearableSynced) {
            snackbarHostState.showSnackbar("Neural Link Established: Synchronizing telemetry.")
        }
    }

    val displayChar = characterState ?: UserCharacter(
        name = "PROTAGONIST", sex = "NON_BINARY", dob = "2077.01.01", units = measurementUnit, weight = "75", somatotype = 0.5f
    )

    var selectedSector by remember { mutableStateOf("CORE_CHASSIS") }
    var scanProgress by remember { mutableFloatStateOf(0f) }
    var showReport by remember { mutableStateOf(false) }
    var showEffectivenessLogger by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let { viewModel.processLabResults(it) }
    }

    val permissionsLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        viewModel.syncWearable()
    }

    val scope = rememberCoroutineScope()
    val theme = LocalNeonTheme.current
    val visualMode = LocalVisualMode.current
    val neonCyan = if (visualMode == VisualMode.STEVE) theme.ink else Color(0xFF00F5FF)
    val neonMagenta = if (visualMode == VisualMode.STEVE) theme.secondary else Color(0xFFFF0088)
    val voidBg = theme.canvas

    if (showPermissionRationale) {
        com.neon.ascent.feature.health.ui.PermissionRationaleDialog(
            rationale = permissionsRationale,
            onConfirm = {
                viewModel.dismissRationale()
                scope.launch {
                    val permissions = viewModel.getPermissionsToRequest()
                    if (permissions.isEmpty()) {
                        // All permissions already granted, just sync
                        viewModel.syncWearable()
                    } else {
                        permissionsLauncher.launch(permissions)
                    }
                }
            },
            onDismiss = { viewModel.dismissRationale() }
        )
    }

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
            neonMagenta = neonMagenta,
            overlayColor = theme.overlay.copy(alpha = 0.95f)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(voidBg)
            .cyberGlitch(intensity = if (visualMode == VisualMode.STEVE) 0f else scanProgress * 0.75f)
    ) {
        if (visualMode == VisualMode.CYBER) {
            CyberGridBackground()
            FloatingParticles(intensity = displayChar.neuralLoad + (scanProgress * 0.5f))
        }
        
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { 
                SnackbarHost(snackbarHostState) { data ->
                    Surface(
                        modifier = Modifier.padding(16.dp),
                        shape = CyberCutShape,
                        color = theme.overlay,
                        border = BorderStroke(1.dp, neonCyan)
                    ) {
                        Text(
                            text = data.visuals.message,
                            color = neonCyan,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp)
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onNavigateToDopamineMenu) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = "Dopamine Menu",
                                tint = neonMagenta,
                                modifier = Modifier.cyberGlitch(0.1f)
                            )
                        }
                        IconButton(onClick = { onNavigateToGuide(null) }) {
                            Icon(
                                imageVector = Icons.Default.ChatBubble,
                                contentDescription = "Neon Guide",
                                tint = neonCyan,
                                modifier = Modifier.cyberGlitch(0.1f)
                            )
                        }
                        NeuralLoadGauge(
                            load = if (isNeuralCoreThinking) 0.9f else displayChar.neuralLoad, 
                            modifier = Modifier.size(64.dp)
                        )
                    }
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

                Spacer(modifier = Modifier.height(16.dp))

                CyberActionButton(
                    label = "DOPAMINE_MENU // PROTOCOL_BYPASS",
                    color = neonMagenta,
                    onClick = onNavigateToDopamineMenu
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Uplink Status Section
                UplinkStatusPanel(
                    syncStatuses = uplinkSyncStatuses,
                    isSyncing = isSyncing,
                    neonCyan = neonCyan,
                    neonMagenta = neonMagenta,
                    onManualSyncClick = { viewModel.triggerManualSync() },
                    onRelink = { provider -> 
                        viewModel.relink(provider)
                        onRelink(provider)
                    },
                    isLiveMonitoringEnabled = isLiveMonitoringEnabled,
                    onLiveMonitoringToggle = { enabled ->
                        viewModel.toggleLiveMonitoring(enabled)
                        if (enabled) LiveBiometricService.start(context)
                        else LiveBiometricService.stop(context)
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Nutrition Macros Uplink
                macros?.let {
                    NutritionMacrosCard(
                        macros = it,
                        neonCyan = neonCyan,
                        neonMagenta = neonMagenta
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // VITALS Stack (P0)
                VitalsStack(
                    snapshot = vitalsSnapshot,
                    rhrSeries = rhrSeries,
                    hrvSeries = hrvSeries,
                    completedSessions = completedSessions,
                    scheduledSessions = scheduledSessions,
                    recoveryScore = recoveryScore,
                    neonCharge = neonCharge,
                    macros = macros,
                    hasNutritionPermission = hasNutritionPermission,
                    neonCyan = neonCyan,
                    neonMagenta = neonMagenta,
                    onNavigateToOps = onNavigateToOps,
                    onRequestNutritionPermission = { viewModel.relink(UplinkProvider.HEALTH_CONNECT) }
                )
                
                Spacer(modifier = Modifier.height(24.dp))

                // Biometric Trends Section
                BiometricTrendsSection(
                    trends = trends,
                    selectedRange = selectedTimeRange,
                    onRangeSelected = { viewModel.setTimeRange(it) },
                    neonCyan = neonCyan,
                    neonMagenta = neonMagenta,
                    onNavigateToGuide = onNavigateToGuide,
                    onNavigateToDopamineMenu = onNavigateToDopamineMenu
                )
                Spacer(modifier = Modifier.height(24.dp))

                // Baselines Section
                ExpandableBioSection("BASELINES_&_DEMOGRAPHICS", neonCyan, initiallyExpanded = focus == "baselines") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            BioReadOnlyField("AGE", calculateAge(displayChar.dob), Modifier.weight(1f), neonCyan)
                            BioReadOnlyField("SEX", displayChar.sex, Modifier.weight(1f), neonCyan)
                        }
                        if (uiState.calculatedBioAge != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            BioAgeCard(
                                bioAge = uiState.calculatedBioAge!!,
                                calendarAge = uiState.calendarAgeAtCalculation ?: calculateAge(displayChar.dob).toIntOrNull() ?: 0,
                                neonCyan = neonCyan,
                                neonMagenta = neonMagenta
                            )
                            Spacer(modifier = Modifier.height(8.dp))
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
                    ExpandableBioSection("WEARABLE_METRICS", neonCyan, initiallyExpanded = focus == "hrv" || focus == "biometrics") {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            BioReadOnlyField("HEART_RATE", "${uiState.currentHeartRate ?: "--"} BPM", Modifier.weight(1f), neonCyan)
                            BioReadOnlyField("STEPS_TODAY", "${uiState.currentSteps ?: "--"}", Modifier.weight(1f), neonCyan)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "LAST_SYNC: ${uiState.lastSyncTimestamp?.let { java.time.Instant.ofEpochMilli(it).toString() } ?: "NEVER"}",
                            color = theme.inkMuted,
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
                ExpandableBioSection("LIFESTYLE_STACK", neonCyan, initiallyExpanded = focus == "recovery" || focus == "lifestyle") {
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
                ExpandableBioSection("GOALS_&_CONSTRAINTS", neonCyan, initiallyExpanded = focus == "goals") {
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
                    UploadCard("LAB_RESULTS_PDF/JSON", Modifier.weight(1f), neonCyan) {
                        filePickerLauncher.launch("*/*")
                    }
                    UploadCard("GENETIC_RAW_DATA", Modifier.weight(1f), neonMagenta) {
                        // Placeholder for genetic data
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                if (!isLocalAiAvailable) {
                    CyberFrame(label = "NEURAL_ENGINE_MISSING", borderColor = neonMagenta) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "Local AI capabilities require the Neural Engine core (Gemma 2B). Download now to enable secure, on-device biohacking protocols.",
                                color = theme.ink,
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
                    color = if (isLocalAiAvailable || !uiState.enableOnDeviceNeuralCore) neonCyan else theme.inkMuted,
                    enabled = isLocalAiAvailable || !uiState.enableOnDeviceNeuralCore,
                    onClick = { viewModel.initiateLocalScan(selectedSector) }
                )

                // Neural Insights from Memory Palace
                if (neuralInsights.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(32.dp))
                    CyberFrame(label = "NEURAL_INSIGHTS // MEMORY_PALACE", borderColor = neonCyan) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            neuralInsights.take(3).forEach { insight ->
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = insight.content,
                                            color = theme.ink,
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(
                                            onClick = { 
                                                onNavigateToForge(
                                                    SpecialType.INTELLIGENCE, 
                                                    "Neural Insight: ${insight.content.take(15)}...", 
                                                    insight.content,
                                                    "Neural Load: ${((displayChar.neuralLoad) * 100).toInt()}%"
                                                ) 
                                            }
                                        ) {
                                            val isHighValue = insight.content.contains("Forge", ignoreCase = true) || 
                                                              insight.content.contains("Sync", ignoreCase = true) ||
                                                              insight.content.length > 40
                                            Icon(
                                                imageVector = if (isHighValue) Icons.Default.Bolt else Icons.Default.Add, 
                                                "Forge Directive", 
                                                tint = if (isHighValue) neonMagenta else neonCyan,
                                                modifier = if (isHighValue) Modifier.cyberGlitch(0.3f) else Modifier
                                            )
                                        }
                                    }
                                    Text(
                                        text = "LOGGED: ${java.time.Instant.ofEpochMilli(insight.timestamp)}",
                                        color = neonCyan.copy(alpha = 0.5f),
                                        fontSize = 8.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                if (insight != neuralInsights.take(3).last()) {
                                    HorizontalDivider(color = neonCyan.copy(alpha = 0.2f))
                                }
                            }
                        }
                    }
                }

                // Dynamic Report Section
                if (showReport) {
                    Spacer(modifier = Modifier.height(32.dp))
                    ProtocolReport(
                        report = viewModel.latestReport.collectAsState().value,
                        magenta = neonMagenta, 
                        cyan = neonCyan,
                        onForgeClick = { report ->
                            onNavigateToForge(
                                SpecialType.ENDURANCE, 
                                "Bio Protocol: ${report.take(15)}...", 
                                report,
                                "Bio Age: ${uiState.calculatedBioAge ?: "Unknown"}"
                            )
                        }
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
        }

        // Thinking Animation
        if (isNeuralCoreThinking) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(theme.overlay)
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
                        listOf(theme.ink, neonCyan.copy(alpha = 0.5f), Color.Transparent),
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
    neonMagenta: Color,
    overlayColor: Color
) {
    val theme = LocalNeonTheme.current
    var anon by remember { mutableStateOf(false) }
    var wearable by remember { mutableStateOf(false) }
    var genetic by remember { mutableStateOf(false) }
    var neuralCore by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier.fillMaxSize().background(overlayColor).zIndex(30f).padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        CyberFrame(label = "PRIVACY_ENCRYPTION_INIT", borderColor = neonCyan) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "Select your data sharing protocols. All data is end-to-end encrypted on-device.",
                    color = theme.ink,
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
    val theme = LocalNeonTheme.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(
            checked = checked, 
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(checkedColor = color, uncheckedColor = color.copy(alpha = 0.3f))
        )
        Text(label, color = theme.ink, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
    }
}

@Composable
fun ProtocolReport(
    report: String?, 
    magenta: Color, 
    cyan: Color, 
    onForgeClick: (String) -> Unit = {},
    onLogClick: () -> Unit
) {
    val theme = LocalNeonTheme.current
    CyberFrame(label = "AI_GENERATED_PROTOCOL", borderColor = magenta, accentColor = cyan) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (report == null) {
                Text("ANALYZING_SYNAPTIC_INPUTS...", color = theme.inkMuted, fontSize = 12.sp)
            } else {
                Text(
                    report,
                    color = theme.ink,
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
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onLogClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = cyan.copy(alpha = 0.1f)),
                    border = BorderStroke(1.dp, cyan)
                ) {
                    Text("LOG_EFF", color = cyan, fontSize = 10.sp, fontWeight = FontWeight.Black)
                }
                
                Button(
                    onClick = { report?.let { onForgeClick(it) } },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = magenta.copy(alpha = 0.1f)),
                    border = BorderStroke(1.dp, magenta)
                ) {
                    Text("FORGE_DIRECTIVE", color = magenta, fontSize = 10.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
fun EffectivenessLoggerPanel(onLog: (Int, Int, Int, Int, String, String) -> Unit, neonCyan: Color) {
    val theme = LocalNeonTheme.current
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
                textStyle = MaterialTheme.typography.bodySmall.copy(color = theme.ink, fontFamily = FontFamily.Monospace),
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
    val theme = LocalNeonTheme.current
    CyberFrame(label = "COMMUNITY_INSIGHTS", borderColor = cyan.copy(alpha = 0.4f)) {
        Column {
            Text(
                "82% of similar users reported +15% Focus on this stack.",
                color = theme.ink,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
            Spacer(Modifier.height(8.dp))
            // Mock Heatmap
            Row(Modifier.fillMaxWidth().height(4.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                repeat(20) { i ->
                    val color: Color = if (i < 16) cyan else theme.inkMuted.copy(alpha = 0.2f)
                    Box(Modifier.weight(1f).fillMaxHeight().background(color))
                }
            }
        }
    }
}

@Composable
fun EffectivenessLogItem(log: BioProtocolLog, cyan: Color) {
    val theme = LocalNeonTheme.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).background(theme.surfaceRaised).padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("LOG_${log.timestamp}", color = cyan, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
        Text("ENERGY: ${log.energyScore}", color = theme.ink, fontSize = 9.sp)
    }
}

@Composable
fun ExpandableBioSection(
    label: String, 
    color: Color, 
    initiallyExpanded: Boolean = false,
    content: @Composable () -> Unit
) {
    var expanded by remember(initiallyExpanded) { mutableStateOf(initiallyExpanded) }
    
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
    val theme = LocalNeonTheme.current
    Column(modifier = modifier) {
        Text(label, color = color.copy(alpha = 0.6f), fontSize = 8.sp, fontWeight = FontWeight.Bold)
        Text(value, color = theme.ink, fontSize = 14.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
    }
}

@Composable
fun BioSliderField(label: String, value: Float, range: ClosedFloatingPointRange<Float>, color: Color, onValueChange: (Float) -> Unit) {
    val theme = LocalNeonTheme.current
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = color.copy(alpha = 0.6f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(value.toInt().toString(), color = theme.ink, fontSize = 10.sp, fontWeight = FontWeight.Black)
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
    val theme = LocalNeonTheme.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(theme.surface)
            .border(0.5.dp, color.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
            .padding(12.dp)
    ) {
        Column {
            Text(label, color = color.copy(alpha = 0.6f), fontSize = 8.sp, fontWeight = FontWeight.Bold)
            Text(value, color = theme.ink, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
fun BioAgeCard(bioAge: Float, calendarAge: Int, neonCyan: Color, neonMagenta: Color) {
    val theme = LocalNeonTheme.current
    val delta = bioAge - calendarAge
    val color = if (delta <= 0) neonCyan else neonMagenta
    
    CyberFrame(label = "BIOLOGICAL_AGE_ANALYSIS", borderColor = color) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("CALENDAR", color = theme.inkMuted, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                    Text("$calendarAge", color = theme.ink, fontSize = 24.sp, fontWeight = FontWeight.Black)
                }
                Text(" VS ", color = color, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("BIOLOGICAL", color = color, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                    Text(String.format(Locale.US, "%.1f", bioAge), color = color, fontSize = 24.sp, fontWeight = FontWeight.Black)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (delta <= 0) "SYSTEM_OPTIMIZED: -${String.format(Locale.US, "%.1f", -delta)} YEARS" 
                       else "DEGRADATION_DETECTED: +${String.format(Locale.US, "%.1f", delta)} YEARS",
                color = color,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun UploadCard(label: String, modifier: Modifier = Modifier, color: Color, onClick: () -> Unit = {}) {
    val theme = LocalNeonTheme.current
    Column(
        modifier = modifier
            .background(theme.surfaceRaised)
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .clickable { onClick() }
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
    val theme = LocalNeonTheme.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(theme.surfaceRaised)
            .neonBorder(magenta.copy(alpha = 0.4f), width = 1.dp, cornerRadius = 8.dp)
            .padding(12.dp)
    ) {
        Text(title, color = theme.ink, fontWeight = FontWeight.Bold, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
        Text(detail, color = magenta, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        Spacer(Modifier.height(6.dp))
        Text(description, color = theme.inkMuted, fontSize = 11.sp, fontFamily = FontFamily.Monospace, lineHeight = 14.sp)
        
        Spacer(Modifier.height(14.dp))
        Button(
            onClick = {},
            modifier = Modifier.height(34.dp).align(Alignment.End),
            colors = ButtonDefaults.buttonColors(containerColor = theme.surface),
            contentPadding = PaddingValues(horizontal = 14.dp),
            shape = RoundedCornerShape(4.dp)
        ) {
            Text("LOG EFFECTIVENESS", fontSize = 10.sp, color = cyan, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
fun UplinkStatusPanel(
    syncStatuses: List<UplinkSyncStatus>,
    isSyncing: Boolean,
    neonCyan: Color,
    neonMagenta: Color,
    onManualSyncClick: () -> Unit,
    onRelink: (UplinkProvider) -> Unit,
    isLiveMonitoringEnabled: Boolean,
    onLiveMonitoringToggle: (Boolean) -> Unit
) {
    val theme = LocalNeonTheme.current
    CyberFrame(label = "NEURAL_UPLINK_STATUS", borderColor = neonCyan.copy(alpha = 0.6f)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "LIVE_MONITORING (BLE)",
                    color = theme.ink,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
                Switch(
                    checked = isLiveMonitoringEnabled,
                    onCheckedChange = onLiveMonitoringToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = neonCyan,
                        checkedTrackColor = neonCyan.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.scale(0.7f)
                )
            }

            HorizontalDivider(color = neonCyan.copy(alpha = 0.2f), thickness = 0.5.dp)

            if (syncStatuses.isEmpty()) {
                Text(
                    "NO_ACTIVE_UPLINKS_FOUND",
                    color = theme.inkMuted,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            syncStatuses.forEach { status ->
                UplinkStatusItem(status, neonCyan, neonMagenta, onRelink)
            }

            Spacer(modifier = Modifier.height(4.dp))
            
            Button(
                onClick = onManualSyncClick,
                modifier = Modifier.fillMaxWidth().height(32.dp),
                enabled = !isSyncing,
                colors = ButtonDefaults.buttonColors(
                    containerColor = neonCyan.copy(alpha = 0.1f),
                    disabledContainerColor = neonCyan.copy(alpha = 0.05f)
                ),
                border = BorderStroke(1.dp, if (isSyncing) neonCyan.copy(alpha = 0.2f) else neonCyan.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(4.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                if (isSyncing) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = neonCyan)
                    Spacer(Modifier.width(8.dp))
                    Text("SYNC_IN_PROGRESS...", color = neonCyan.copy(alpha = 0.5f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = neonCyan, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("MANUAL_DEEP_SYNC", color = neonCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun UplinkStatusItem(
    syncStatus: UplinkSyncStatus,
    neonCyan: Color,
    neonMagenta: Color,
    onRelink: (UplinkProvider) -> Unit
) {
    val theme = LocalNeonTheme.current
    val statusColor = when (syncStatus.currentStatus) {
        is UplinkStatus.Connected -> neonCyan
        is UplinkStatus.Error -> neonMagenta
        is UplinkStatus.PermissionRequired -> neonMagenta
        is UplinkStatus.NeedsReAuth -> neonMagenta
        is UplinkStatus.Syncing -> Color.Yellow
        else -> theme.inkMuted
    }

    val statusText = when (val s = syncStatus.currentStatus) {
        is UplinkStatus.Connected -> "CONNECTED"
        is UplinkStatus.Error -> "ERROR: ${s.message}"
        is UplinkStatus.PermissionRequired -> "NO_PERMISSION"
        is UplinkStatus.NeedsReAuth -> "RE-AUTH_REQUIRED"
        is UplinkStatus.Syncing -> "SYNCING (${(s.progress * 100).toInt()}%)"
        is UplinkStatus.Authenticating -> "AUTHENTICATING..."
        is UplinkStatus.Disconnected -> "DISCONNECTED"
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = syncStatus.provider.name.replace("_", " "),
                color = theme.ink,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = statusText,
                color = statusColor,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = 1
            )
            syncStatus.lastSuccessfulSync?.let { lastSync ->
                val timeAgo = formatTimeAgo(lastSync)
                Text(
                    text = "LAST_SYNC: $timeAgo",
                    color = theme.inkMuted,
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            if (syncStatus.lastError != null && syncStatus.currentStatus !is UplinkStatus.Error) {
                Text(
                    text = "LAST_ERR: ${syncStatus.lastError}",
                    color = neonMagenta.copy(alpha = 0.7f),
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1
                )
            }
        }
        
        if (syncStatus.currentStatus is UplinkStatus.Error || syncStatus.currentStatus is UplinkStatus.PermissionRequired || syncStatus.currentStatus is UplinkStatus.Disconnected || syncStatus.currentStatus is UplinkStatus.NeedsReAuth) {
            TextButton(
                onClick = { onRelink(syncStatus.provider) },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.height(24.dp)
            ) {
                Text("RE-LINK", color = neonCyan, fontSize = 10.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

fun formatTimeAgo(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 60000 -> "Just now"
        diff < 3600000 -> "${diff / 60000}m ago"
        diff < 86400000 -> "${diff / 3600000}h ago"
        else -> "${diff / 86400000}d ago"
    }
}

@Composable
fun BiometricTrendsSection(
    trends: List<com.neon.ascent.model.HealthTrend>,
    selectedRange: Int,
    onRangeSelected: (Int) -> Unit,
    neonCyan: Color,
    neonMagenta: Color,
    onNavigateToGuide: (String?) -> Unit,
    onNavigateToDopamineMenu: () -> Unit
) {
    val theme = LocalNeonTheme.current
    val ranges = mapOf(7 to "7D", 30 to "30D", 90 to "90D")

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "BIOMETRIC_TRENDS",
                color = neonCyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp
            )
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ranges.forEach { (days, label) ->
                    Text(
                        text = label,
                        color = if (selectedRange == days) neonCyan else theme.inkMuted,
                        fontSize = 9.sp,
                        fontWeight = if (selectedRange == days) FontWeight.Bold else FontWeight.Normal,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .clickable { onRangeSelected(days) }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                            .background(if (selectedRange == days) neonCyan.copy(alpha = 0.1f) else Color.Transparent)
                    )
                }
            }
        }
        
        if (trends.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                    .border(1.dp, theme.inkMuted.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                    .clickable { onNavigateToDopamineMenu() },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                    Icon(Icons.Default.Analytics, contentDescription = null, tint = theme.inkMuted.copy(alpha = 0.5f), modifier = Modifier.size(32.dp))
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "NO_UPLINK_DATA_DETECTED",
                        color = theme.inkMuted,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        "Initialize your neural link. Tap here for a Dopamine Menu reset while you wait for sync.",
                        color = neonMagenta.copy(alpha = 0.7f),
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                trends.forEach { trend ->
                    TrendCard(
                        trend = trend,
                        neonCyan = neonCyan,
                        neonMagenta = neonMagenta,
                        onNavigateToGuide = onNavigateToGuide,
                        modifier = Modifier.width(220.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun TrendCard(
    trend: com.neon.ascent.model.HealthTrend,
    neonCyan: Color,
    neonMagenta: Color,
    onNavigateToGuide: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = LocalNeonTheme.current
    var isExpanded by remember { mutableStateOf(false) }
    
    // Threshold-based color coding
    val statusColor = when (trend.label) {
        "SLEEP" -> if ((trend.currentValue.toFloatOrNull() ?: 8f) < 6.5f) neonMagenta else neonCyan
        "HRV" -> if ((trend.currentValue.toFloatOrNull() ?: 35f) < 35f) neonMagenta else neonCyan
        "STEPS" -> if ((trend.currentValue.toFloatOrNull() ?: 10000f) < 5000f) neonMagenta else neonCyan
        else -> neonCyan
    }
    
    CyberFrame(
        label = trend.label,
        borderColor = statusColor.copy(alpha = 0.6f),
        modifier = modifier.clickable { isExpanded = !isExpanded }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = trend.currentValue,
                    color = theme.ink,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = trend.unit,
                    color = statusColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            
            if (trend.label == "STEPS") {
                BarChart(
                    data = trend.dataPoints,
                    color = statusColor,
                    modifier = Modifier.fillMaxWidth().height(60.dp)
                )
            } else {
                Sparkline(
                    data = trend.dataPoints,
                    color = statusColor,
                    modifier = Modifier.fillMaxWidth().height(60.dp)
                )
            }
            
            if (trend.insight != null) {
                Text(
                    text = trend.insight,
                    color = theme.ink.copy(alpha = 0.8f),
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 12.sp,
                    modifier = Modifier.background(statusColor.copy(alpha = 0.1f)).padding(4.dp)
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = statusColor.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        "ANALYSIS: Correlation detected between ${trend.label} and recent Mission adherence. Suggesting neural load optimization.",
                        color = theme.inkMuted,
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Button(
                        onClick = { onNavigateToGuide("Analyze my ${trend.label} trend (${trend.currentValue} ${trend.unit}). How does this correlate with my active Directives?") },
                        modifier = Modifier.fillMaxWidth().height(32.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = statusColor.copy(alpha = 0.1f)),
                        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(2.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(14.dp), tint = statusColor)
                            Spacer(Modifier.width(6.dp))
                            Text("ASK_NEON_GUIDE", color = statusColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NutritionMacrosCard(macros: com.neon.ascent.core.domain.workout.rules.Macros, neonCyan: Color, neonMagenta: Color) {
    val theme = LocalNeonTheme.current
    CyberFrame(label = "BIOMETRIC_NUTRITION_UPLINK", borderColor = neonCyan) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("DAILY_TARGET", color = theme.inkMuted, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                    Text("${macros.calories}", color = theme.ink, fontSize = 28.sp, fontWeight = FontWeight.Black)
                    Text("KCAL", color = neonCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    MacroSubStat("PRO", "${macros.protein}g", neonCyan)
                    MacroSubStat("CHO", "${macros.carbs}g", theme.ink)
                    MacroSubStat("FAT", "${macros.fat}g", neonMagenta)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Visual breakdown bar
            Row(modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp))) {
                val total = (macros.protein * 4 + macros.carbs * 4 + macros.fat * 9).toFloat()
                Box(Modifier.weight(macros.protein * 4 / total).fillMaxHeight().background(neonCyan))
                Box(Modifier.weight(macros.carbs * 4 / total).fillMaxHeight().background(theme.ink.copy(alpha = 0.6f)))
                Box(Modifier.weight(macros.fat * 9 / total).fillMaxHeight().background(neonMagenta))
            }
        }
    }
}

@Composable
fun MacroSubStat(label: String, value: String, color: Color) {
    val theme = LocalNeonTheme.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = color, fontSize = 8.sp, fontWeight = FontWeight.Black)
        Text(value, color = theme.ink, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}

@Composable
fun BarChart(
    data: List<Float>,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        if (data.isEmpty()) return@Canvas
        
        val max = data.maxOrNull()?.coerceAtLeast(1f) ?: 1f
        val width = size.width
        val height = size.height
        val barWidth = (width / data.size) * 0.8f
        val spacing = (width / data.size) * 0.2f
        
        data.forEachIndexed { index, value ->
            val barHeight = (value / max) * height
            drawRect(
                color = color.copy(alpha = 0.6f),
                topLeft = Offset(x = index * (barWidth + spacing), y = height - barHeight),
                size = Size(width = barWidth, height = barHeight)
            )
            // Cap
            drawRect(
                color = color,
                topLeft = Offset(x = index * (barWidth + spacing), y = height - barHeight),
                size = Size(width = barWidth, height = 2.dp.toPx())
            )
        }
    }
}

@Composable
fun Sparkline(
    data: List<Float>,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        if (data.size < 2) return@Canvas
        
        val min = data.minOrNull() ?: 0f
        val max = data.maxOrNull() ?: 1f
        val range = (max - min).coerceAtLeast(1f)
        
        val width = size.width
        val height = size.height
        val stepX = width / (data.size - 1)
        
        val points = data.mapIndexed { index, value ->
            Offset(
                x = index * stepX,
                y = height - ((value - min) / range * height)
            )
        }
        
        val path = Path().apply {
            moveTo(points.first().x, points.first().y)
            for (i in 1 until points.size) {
                lineTo(points[i].x, points[i].y)
            }
        }
        
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        
        // Fill area under sparkline
        val fillPath = Path().apply {
            addPath(path)
            lineTo(width, height)
            lineTo(0f, height)
            close()
        }
        
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(color.copy(alpha = 0.3f), Color.Transparent)
            )
        )
        
        // Draw grid lines
        repeat(4) { i ->
            val y = (height / 3) * i
            drawLine(
                color = color.copy(alpha = 0.05f),
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1f
            )
        }
    }
}
