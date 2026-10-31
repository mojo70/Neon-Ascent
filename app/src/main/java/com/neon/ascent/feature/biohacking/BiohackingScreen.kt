@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.neon.ascent.feature.biohacking

import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.health.connect.client.PermissionController
import androidx.hilt.navigation.compose.hiltViewModel
import com.neon.ascent.core.common.LocalNeonTheme
import com.neon.ascent.core.common.LocalVisualMode
import com.neon.ascent.core.common.VisualMode
import com.neon.ascent.core.data.local.entity.BodySampleEntity
import com.neon.ascent.core.domain.character.models.UserCharacter
import com.neon.ascent.core.domain.model.SpecialType
import com.neon.ascent.feature.biohacking.ui.labs.LabsAnalyzeSection
import com.neon.ascent.feature.biohacking.ui.labs.LabsCheckInSection
import com.neon.ascent.feature.biohacking.ui.labs.LabsSection
import com.neon.ascent.feature.biohacking.ui.labs.LabsTodaySection
import com.neon.ascent.feature.biohacking.ui.labs.labsSectionForFocus
import com.neon.ascent.feature.biohacking.ui.labs.showsPregnancyCheck
import com.neon.ascent.feature.biohacking.ui.labs.waistDisplayValue
import com.neon.ascent.feature.biohacking.ui.sheet.BodyLogMode
import com.neon.ascent.feature.biohacking.ui.sheet.BodyLogSheet
import com.neon.ascent.feature.health.data.services.LiveBiometricService
import com.neon.ascent.feature.health.domain.uplink.UplinkProvider
import com.neon.ascent.feature.health.domain.uplink.UplinkStatus
import com.neon.ascent.feature.health.domain.uplink.UplinkSyncStatus
import com.neon.ascent.model.BioProtocolLog
import com.neon.ascent.model.BiohackingData
import com.neon.ascent.model.HealthTrend
import com.neon.ascent.ui.CyberActionButton
import com.neon.ascent.ui.CyberCutShape
import com.neon.ascent.ui.CyberFrame
import com.neon.ascent.core.common.CyberGridBackground
import com.neon.ascent.ui.GlitchText
import com.neon.ascent.ui.NeuralLoadGauge
import com.neon.ascent.ui.components.ChargeInspectSheet
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@Composable
fun BiohackingScreen(
    onBack: () -> Unit,
    focus: String? = null,
    onNavigateToForge: (SpecialType, String?, String?, String?) -> Unit = { _, _, _, _ -> },
    onNavigateToGuide: (String?) -> Unit = {},
    onNavigateToDopamineMenu: () -> Unit = {},
    onNavigateToReconstitutionCalculator: () -> Unit = {},
    onNavigateToHalfLifeSketch: () -> Unit = {},
    onNavigateToUnitConverter: () -> Unit = {},
    onNavigateToOps: () -> Unit = {},
    onRelink: (UplinkProvider) -> Unit = {},
    onNavigateToSelfMap: () -> Unit = {},
    onNavigateToChronicle: (() -> Unit)? = null,
    viewModel: BiohackingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val trends by viewModel.trends.collectAsState()
    val characterState by viewModel.character.collectAsState()
    val logs by viewModel.logs.collectAsState()
    val isNeuralCoreThinking by viewModel.isNeuralCoreThinking.collectAsState()
    val latestReport by viewModel.latestReport.collectAsState()
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
    val activeDiet by viewModel.activeDiet.collectAsState()
    val activeSupplementStack by viewModel.activeSupplementStack.collectAsState()
    val supplementOverrides by viewModel.supplementOverrides.collectAsState()
    val activeProtocols by viewModel.activeProtocols.collectAsState()
    val protocolOverrides by viewModel.protocolOverrides.collectAsState()
    val isHigherCarbDay by viewModel.isHigherCarbDay.collectAsState()
    val bodySamples by viewModel.recentBodySamples.collectAsState()
    val neonRowsCount by viewModel.neonBodyRowsCount.collectAsState()

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val theme = LocalNeonTheme.current
    val visualMode = LocalVisualMode.current
    val neonCyan = if (visualMode == VisualMode.STEVE) theme.ink else if (visualMode == VisualMode.PILOT) theme.accent else Color(0xFF00F5FF)
    val neonMagenta = if (visualMode == VisualMode.STEVE) theme.secondary else if (visualMode == VisualMode.PILOT) theme.accentDanger else Color(0xFFFF0088)

    val displayChar = characterState ?: UserCharacter(
        name = "PROTAGONIST", sex = "NON_BINARY", dob = "2077.01.01", units = measurementUnit, weight = "75", somatotype = 0.5f
    )

    val focusedSection = labsSectionForFocus(focus)
    var toolsExpanded by remember(focus) { mutableStateOf(focusedSection == LabsSection.Tools) }
    var connectExpanded by remember(focus) { mutableStateOf(focusedSection == LabsSection.Connect) }
    var checkInExpanded by remember(focus) { mutableStateOf(focusedSection == LabsSection.CheckIn) }
    var recordsExpanded by remember(focus) { mutableStateOf(focusedSection == LabsSection.Records) }
    var analyzeExpanded by remember(focus) { mutableStateOf(focusedSection == LabsSection.Analyze) }

    var selectedSector by remember { mutableStateOf("CORE_CHASSIS") }
    var showEffectivenessLogger by remember { mutableStateOf(false) }
    var showBodyLogSheet by remember { mutableStateOf(false) }
    var bodyLogMode by remember { mutableStateOf<BodyLogMode?>(null) }
    var showChargeSheet by remember { mutableStateOf(false) }
    var showDailyOriginSheet by remember { mutableStateOf(false) }
    var showPrivacySheet by remember { mutableStateOf(false) }
    var scanWasRunning by remember { mutableStateOf(false) }

    val todayRequester = remember { BringIntoViewRequester() }
    val logRequester = remember { BringIntoViewRequester() }
    val toolsRequester = remember { BringIntoViewRequester() }
    val connectRequester = remember { BringIntoViewRequester() }
    val checkInRequester = remember { BringIntoViewRequester() }
    val recordsRequester = remember { BringIntoViewRequester() }
    val analyzeRequester = remember { BringIntoViewRequester() }

    LaunchedEffect(isSyncing) {
        if (isSyncing) snackbarHostState.showSnackbar("Neural Uplink: Manual sync initiated...")
    }
    LaunchedEffect(uiState.isWearableSynced) {
        if (uiState.isWearableSynced) snackbarHostState.showSnackbar("Neural Link Established: Synchronizing telemetry.")
    }
    LaunchedEffect(focus) {
        if (focus == null) return@LaunchedEffect
        when (labsSectionForFocus(focus)) {
            LabsSection.Today -> todayRequester.bringIntoView()
            LabsSection.Log -> logRequester.bringIntoView()
            LabsSection.Tools -> toolsRequester.bringIntoView()
            LabsSection.Connect -> connectRequester.bringIntoView()
            LabsSection.CheckIn -> checkInRequester.bringIntoView()
            LabsSection.Records -> recordsRequester.bringIntoView()
            LabsSection.Analyze -> analyzeRequester.bringIntoView()
        }
    }
    LaunchedEffect(isNeuralCoreThinking) {
        if (isNeuralCoreThinking) {
            scanWasRunning = true
        } else if (scanWasRunning) {
            scanWasRunning = false
            if (latestReport != null) {
                analyzeExpanded = true
                analyzeRequester.bringIntoView()
            }
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.processLabResults(it) }
    }
    val permissionsLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) {
        viewModel.syncWearable()
        viewModel.checkAndTriggerBodyBackfill()
    }
    LaunchedEffect(Unit) { viewModel.checkAndTriggerBodyBackfill() }

    fun openBodyLog(mode: BodyLogMode) {
        bodyLogMode = mode
        showBodyLogSheet = true
    }

    if (showBodyLogSheet) {
        BodyLogSheet(
            onDismiss = { showBodyLogSheet = false },
            onSave = { state ->
                viewModel.saveBodyLog(state)
                showBodyLogSheet = false
            },
            measurementUnit = measurementUnit,
            mode = bodyLogMode ?: BodyLogMode.WEIGHT,
            samples = bodySamples,
            currentEnergy = uiState.energyScore,
            currentMood = uiState.moodScore,
            currentFocus = uiState.focusScore,
            currentStress = uiState.stressLevel,
            currentObjective = uiState.primaryObjective,
            currentContraindications = uiState.contraindications,
            onSaveHowIFeel = { energy, mood, focus, stress, objective, contraindications ->
                viewModel.updateData {
                    it.copy(
                        energyScore = energy,
                        moodScore = mood,
                        focusScore = focus,
                        stressLevel = stress,
                        primaryObjective = objective,
                        contraindications = contraindications
                    )
                }
                showBodyLogSheet = false
            },
            showShareToHcFooter = neonRowsCount > 0,
            onRequestHcBodyPerms = {
                scope.launch {
                    val permissions = viewModel.getPermissionsToRequest()
                    if (permissions.isNotEmpty()) permissionsLauncher.launch(permissions)
                }
            }
        )
    }
    if (showDailyOriginSheet) {
        com.neon.ascent.feature.biohacking.ui.sheet.DailyOriginSheet(
            onDismiss = { showDailyOriginSheet = false },
            onOpenChronicleCompose = { _, _ ->
                showDailyOriginSheet = false
                onNavigateToChronicle?.invoke()
            },
            viewModel = viewModel
        )
    }
    if (showPermissionRationale) {
        com.neon.ascent.feature.health.ui.PermissionRationaleDialog(
            rationale = permissionsRationale,
            onConfirm = {
                viewModel.dismissRationale()
                scope.launch {
                    val permissions = viewModel.getPermissionsToRequest()
                    if (permissions.isEmpty()) viewModel.syncWearable() else permissionsLauncher.launch(permissions)
                }
            },
            onDismiss = { viewModel.dismissRationale() }
        )
    }
    if (showChargeSheet && neonCharge != null) {
        ChargeInspectSheet(
            charge = neonCharge,
            onDismiss = { showChargeSheet = false },
            isLabsView = true
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(theme.canvas)) {
        if (visualMode == VisualMode.CYBER) {
            CyberGridBackground()
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
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                LabsHeader(
                    collectiveOn = uiState.consentAnonymizedUpload,
                    onDevice = uiState.enableOnDeviceNeuralCore,
                    neuralLoad = if (isNeuralCoreThinking) 0.9f else displayChar.neuralLoad,
                    accent = neonCyan,
                    danger = neonMagenta,
                    onStatusClick = {
                        if (uiState.hasCompletedPrivacyOnboarding) showPrivacySheet = true
                    },
                    onDopamine = onNavigateToDopamineMenu,
                    onGuide = { onNavigateToGuide(null) }
                )

                LabsTodaySection(
                    modifier = Modifier.bringIntoViewRequester(todayRequester),
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
                    activeDiet = activeDiet,
                    isHigherCarbDay = isHigherCarbDay,
                    activeSupplementStack = activeSupplementStack,
                    supplementOverrides = supplementOverrides,
                    activeProtocols = activeProtocols,
                    protocolOverrides = protocolOverrides,
                    onOpenDayCard = { showDailyOriginSheet = true },
                    onOpenSelfMap = onNavigateToSelfMap,
                    onNavigateToCodexDiet = { onNavigateToGuide("Codex Diet Panel") },
                    onNavigateToCodexSupplements = { onNavigateToGuide("Codex Supplement Panel") },
                    onNavigateToCodexProtocols = { onNavigateToGuide("Codex Protocol Panel") },
                    onNavigateToOps = onNavigateToOps,
                    onRequestNutritionPermission = { viewModel.relink(UplinkProvider.HEALTH_CONNECT) },
                    onChargeClick = {
                        if (neonCharge == null) {
                            connectExpanded = true
                            scope.launch { connectRequester.bringIntoView() }
                        } else {
                            showChargeSheet = true
                        }
                    }
                )

                Column(
                    modifier = Modifier.bringIntoViewRequester(logRequester).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "LOG",
                        color = neonCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                    LabsTextButton("Log body", neonCyan) { openBodyLog(BodyLogMode.WEIGHT) }
                    LabsTextButton("Log blood pressure", neonCyan) { openBodyLog(BodyLogMode.BLOOD_PRESSURE) }
                    LabsTextButton("Log how I feel", neonCyan) { openBodyLog(BodyLogMode.HOW_I_FEEL) }
                    FeelingCompactCard(
                        energy = uiState.energyScore,
                        mood = uiState.moodScore,
                        focus = uiState.focusScore,
                        stress = uiState.stressLevel,
                        neonCyan = neonCyan,
                        onClick = { openBodyLog(BodyLogMode.HOW_I_FEEL) }
                    )
                    BodyLogCard(
                        samples = bodySamples,
                        measurementUnit = measurementUnit,
                        neonCyan = neonCyan,
                        onWeightClick = { openBodyLog(BodyLogMode.WEIGHT) },
                        onBodyFatClick = { openBodyLog(BodyLogMode.BODY_FAT) },
                        onWaistClick = { openBodyLog(BodyLogMode.WAIST) },
                        onBloodPressureClick = { openBodyLog(BodyLogMode.BLOOD_PRESSURE) }
                    )
                }

                LabsCollapsedSection(
                    label = "TOOLS",
                    expanded = toolsExpanded,
                    onToggle = { toolsExpanded = !toolsExpanded },
                    borderColor = neonCyan,
                    modifier = Modifier.bringIntoViewRequester(toolsRequester)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        CyberActionButton("LAUNCH // RECONSTITUTION_CALCULATOR", neonCyan, onClick = onNavigateToReconstitutionCalculator)
                        CyberActionButton("HALF-LIFE SKETCH", neonCyan, onClick = onNavigateToHalfLifeSketch)
                        CyberActionButton("UNIT CONVERTER", neonCyan, onClick = onNavigateToUnitConverter)
                    }
                }

                LabsCollapsedSection(
                    label = "CONNECT",
                    expanded = connectExpanded,
                    onToggle = { connectExpanded = !connectExpanded },
                    borderColor = neonCyan,
                    modifier = Modifier.bringIntoViewRequester(connectRequester)
                ) {
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
                            if (enabled) LiveBiometricService.start(context) else LiveBiometricService.stop(context)
                        }
                    )
                }

                LabsCollapsedSection(
                    label = "CHECK-IN",
                    expanded = checkInExpanded,
                    onToggle = { checkInExpanded = !checkInExpanded },
                    borderColor = neonCyan,
                    modifier = Modifier.bringIntoViewRequester(checkInRequester)
                ) {
                    LabsCheckInSection(
                        energy = uiState.energyScore,
                        mood = uiState.moodScore,
                        focus = uiState.focusScore,
                        stress = uiState.stressLevel,
                        objective = uiState.primaryObjective,
                        contraindications = uiState.contraindications,
                        pregnancyFlag = uiState.pregnancyFlag,
                        showPregnancy = showsPregnancyCheck(characterState?.sex),
                        onEnergyChange = { score -> viewModel.updateData { it.copy(energyScore = score) } },
                        onMoodChange = { score -> viewModel.updateData { it.copy(moodScore = score) } },
                        onFocusChange = { score -> viewModel.updateData { it.copy(focusScore = score) } },
                        onStressChange = { score -> viewModel.updateData { it.copy(stressLevel = score) } },
                        onObjectiveCommit = { value -> viewModel.updateData { it.copy(primaryObjective = value) } },
                        onContraindicationsCommit = { value -> viewModel.updateData { it.copy(contraindications = value) } },
                        onPregnancyChange = { checked -> viewModel.updateData { it.copy(pregnancyFlag = checked) } },
                        neonCyan = neonCyan,
                        neonMagenta = neonMagenta
                    )
                }

                LabsCollapsedSection(
                    label = "RECORDS",
                    expanded = recordsExpanded,
                    onToggle = { recordsExpanded = !recordsExpanded },
                    borderColor = neonCyan,
                    modifier = Modifier.bringIntoViewRequester(recordsRequester)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "BASELINES",
                            color = neonCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            BioReadOnlyField("Age", calculateAge(displayChar.dob), Modifier.weight(1f), neonCyan)
                            BioReadOnlyField("Sex", displayChar.sex, Modifier.weight(1f), neonCyan)
                        }
                        BioReadOnlyField("Height", displayChar.formattedHeight(measurementUnit), Modifier.fillMaxWidth(), neonCyan)
                        if (uiState.calculatedBioAge != null) {
                            BioAgeCard(
                                bioAge = uiState.calculatedBioAge!!,
                                calendarAge = uiState.calendarAgeAtCalculation ?: calculateAge(displayChar.dob).toIntOrNull() ?: 0,
                                neonCyan = neonCyan,
                                neonMagenta = neonMagenta
                            )
                        }
                        BiometricTrendsSection(
                            trends = trends,
                            selectedRange = selectedTimeRange,
                            onRangeSelected = { viewModel.setTimeRange(it) },
                            neonCyan = neonCyan,
                            neonMagenta = neonMagenta,
                            onNavigateToGuide = onNavigateToGuide,
                            onConnectWearable = { viewModel.triggerManualSync() }
                        )
                    }
                }

                LabsCollapsedSection(
                    label = "ANALYZE",
                    expanded = analyzeExpanded,
                    onToggle = { analyzeExpanded = !analyzeExpanded },
                    borderColor = neonCyan,
                    modifier = Modifier.bringIntoViewRequester(analyzeRequester)
                ) {
                    LabsAnalyzeSection(
                        selectedSector = selectedSector,
                        onSectorSelected = { selectedSector = it },
                        scanEnabled = isLocalAiAvailable || !uiState.enableOnDeviceNeuralCore,
                        onRunScan = { viewModel.initiateLocalScan(selectedSector) },
                        isLocalAiAvailable = isLocalAiAvailable,
                        isDownloading = isDownloading,
                        downloadProgress = downloadProgress,
                        onDownloadModel = { viewModel.modelDownloadManager.startDownload() },
                        onUploadLab = { filePickerLauncher.launch("*/*") },
                        report = latestReport,
                        showEffectivenessForm = showEffectivenessLogger,
                        onLogHowThisFelt = { showEffectivenessLogger = true },
                        onTurnIntoDirective = { report ->
                            onNavigateToForge(
                                SpecialType.ENDURANCE,
                                "Bio Protocol: ${report.take(15)}...",
                                report,
                                "Bio Age: ${uiState.calculatedBioAge ?: "Unknown"}"
                            )
                        },
                        onLogEffectiveness = { energy, sleep, mood, focusScore, sideEffects, notes ->
                            val protocolId = (uiState.reportTimestamp ?: System.currentTimeMillis()).toString()
                            viewModel.logProtocolEffectiveness(
                                energy,
                                sleep,
                                mood,
                                focusScore,
                                sideEffects.ifBlank { null },
                                notes.ifBlank { null },
                                protocolId
                            )
                            showEffectivenessLogger = false
                        },
                        logs = logs,
                        neuralInsights = neuralInsights,
                        onForgeInsight = { insight ->
                            onNavigateToForge(
                                SpecialType.INTELLIGENCE,
                                "Neural Insight: ${insight.content.take(15)}...",
                                insight.content,
                                "Neural Load: ${((displayChar.neuralLoad) * 100).toInt()}%"
                            )
                        },
                        neonCyan = neonCyan,
                        neonMagenta = neonMagenta
                    )
                }
                Spacer(modifier = Modifier.height(48.dp))
            }
        }

        if (isNeuralCoreThinking) {
            ThinkingOverlay(
                accent = neonCyan,
                danger = neonMagenta,
                onCancel = { viewModel.cancelScan() }
            )
        }

        if (!uiState.hasCompletedPrivacyOnboarding) {
            PrivacyOnboarding(
                onComplete = { anon, wearable, genetic, neuralCore ->
                    viewModel.updateData {
                        it.copy(
                            consentAnonymizedUpload = anon,
                            consentWearableSync = wearable,
                            consentGeneticData = genetic,
                            enableOnDeviceNeuralCore = neuralCore,
                            hasCompletedPrivacyOnboarding = true,
                            hasConsentedToDataProcessing = true
                        )
                    }
                },
                neonCyan = neonCyan,
                neonMagenta = neonMagenta,
                overlayColor = theme.overlay.copy(alpha = 0.95f)
            )
        } else if (showPrivacySheet) {
            PrivacyChooserSheet(
                uiState = uiState,
                neonCyan = neonCyan,
                neonMagenta = neonMagenta,
                onDismiss = { showPrivacySheet = false },
                onConfirm = { anon, wearable, genetic, neuralCore ->
                    viewModel.updateData {
                        it.copy(
                            consentAnonymizedUpload = anon,
                            consentWearableSync = wearable,
                            consentGeneticData = genetic,
                            enableOnDeviceNeuralCore = neuralCore
                        )
                    }
                    showPrivacySheet = false
                }
            )
        }
    }
}

@Composable
private fun LabsHeader(
    collectiveOn: Boolean,
    onDevice: Boolean,
    neuralLoad: Float,
    accent: Color,
    danger: Color,
    onStatusClick: () -> Unit,
    onDopamine: () -> Unit,
    onGuide: () -> Unit
) {
    val theme = LocalNeonTheme.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "LABS",
                color = accent,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "${if (collectiveOn) "Collective net on" else "Collective net off"} · ${if (onDevice) "On-device" else "On-device off"}",
                color = theme.ink,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.clickable(onClickLabel = "Privacy and sync settings", onClick = onStatusClick)
            )
        }
        IconButtonNoRipple(onDopamine, "Dopamine Menu", Icons.Default.Bolt, danger)
        IconButtonNoRipple(onGuide, "Neon Guide", Icons.Default.ChatBubble, accent)
        NeuralLoadGauge(load = neuralLoad, modifier = Modifier.size(64.dp))
    }
}

@Composable
private fun IconButtonNoRipple(onClick: () -> Unit, description: String, icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color) {
    androidx.compose.material3.IconButton(onClick = onClick) {
        Icon(imageVector = icon, contentDescription = description, tint = tint)
    }
}

@Composable
private fun LabsTextButton(label: String, accent: Color, onClick: () -> Unit) {
    val theme = LocalNeonTheme.current
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        border = BorderStroke(1.dp, accent)
    ) {
        Text(label, color = theme.ink, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun LabsCollapsedSection(
    label: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    borderColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val action = if (expanded) "Collapse $label" else "Expand $label"
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clickable(onClickLabel = action, onClick = onToggle),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = borderColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = action,
                tint = borderColor
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.padding(bottom = 12.dp)) { content() }
        }
        HorizontalDivider(color = borderColor.copy(alpha = 0.2f))
    }
}

@Composable
private fun ThinkingOverlay(accent: Color, danger: Color, onCancel: () -> Unit) {
    val theme = LocalNeonTheme.current
    val motionScale = animatorDurationScale()
    Box(
        modifier = Modifier.fillMaxSize().background(theme.overlay).zIndex(20f),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            GlitchText(text = "NEURAL_CORE THINKING...", color = accent, fontSize = 24.sp, fontWeight = FontWeight.Black)
            Spacer(modifier = Modifier.height(16.dp))
            if (motionScale == 0f) {
                LinearProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier.width(240.dp).height(2.dp),
                    color = accent,
                    trackColor = accent.copy(alpha = 0.1f)
                )
            } else {
                LinearProgressIndicator(
                    modifier = Modifier.width(240.dp).height(2.dp),
                    color = accent,
                    trackColor = accent.copy(alpha = 0.1f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "UPLINKING_LOCAL_SYNAPSE",
                color = danger,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.alpha(pulseAlpha())
            )
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = onCancel) {
                Text("Cancel", color = theme.ink, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun animatorDurationScale(): Float {
    val context = LocalContext.current
    return Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
}

@Composable
private fun pulseAlpha(): Float {
    val scale = animatorDurationScale()
    val infiniteTransition = rememberInfiniteTransition(label = "Pulse")
    val animated by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Alpha"
    )
    return if (scale == 0f) 1f else animated
}

fun calculateAge(dob: String): String {
    return try {
        val birthDate = LocalDate.parse(dob, DateTimeFormatter.ofPattern("yyyy.MM.dd"))
        Period.between(birthDate, LocalDate.now()).years.toString()
    } catch (_: Exception) {
        "ERR"
    }
}

@Composable
fun PrivacyOnboarding(
    onComplete: (anon: Boolean, wearable: Boolean, genetic: Boolean, neuralCore: Boolean) -> Unit,
    neonCyan: Color,
    neonMagenta: Color,
    overlayColor: Color,
    initialAnon: Boolean = false,
    initialWearable: Boolean = false,
    initialGenetic: Boolean = false,
    initialNeuralCore: Boolean = true
) {
    Box(
        modifier = Modifier.fillMaxSize().background(overlayColor).zIndex(30f).padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        CyberFrame(label = "PRIVACY_ENCRYPTION_INIT", borderColor = neonCyan) {
            PrivacyChooserContent(
                initialAnon = initialAnon,
                initialWearable = initialWearable,
                initialGenetic = initialGenetic,
                initialNeuralCore = initialNeuralCore,
                neonCyan = neonCyan,
                neonMagenta = neonMagenta,
                confirmLabel = "Confirm",
                onComplete = onComplete
            )
        }
    }
}

@Composable
fun PrivacyChooserSheet(
    uiState: BiohackingData,
    neonCyan: Color,
    neonMagenta: Color,
    onDismiss: () -> Unit,
    onConfirm: (Boolean, Boolean, Boolean, Boolean) -> Unit
) {
    val theme = LocalNeonTheme.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = theme.canvas,
        contentColor = theme.ink
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            PrivacyChooserContent(
                initialAnon = uiState.consentAnonymizedUpload,
                initialWearable = uiState.consentWearableSync,
                initialGenetic = uiState.consentGeneticData,
                initialNeuralCore = uiState.enableOnDeviceNeuralCore,
                neonCyan = neonCyan,
                neonMagenta = neonMagenta,
                confirmLabel = "Confirm",
                onComplete = onConfirm
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PrivacyChooserContent(
    initialAnon: Boolean,
    initialWearable: Boolean,
    initialGenetic: Boolean,
    initialNeuralCore: Boolean,
    neonCyan: Color,
    neonMagenta: Color,
    confirmLabel: String,
    onComplete: (Boolean, Boolean, Boolean, Boolean) -> Unit
) {
    val theme = LocalNeonTheme.current
    var anon by remember { mutableStateOf(initialAnon) }
    var wearable by remember { mutableStateOf(initialWearable) }
    var genetic by remember { mutableStateOf(initialGenetic) }
    var neuralCore by remember { mutableStateOf(initialNeuralCore) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Select your data sharing protocols. All data is end-to-end encrypted on-device.",
            color = theme.ink,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace
        )
        PrivacyToggle("Collective net", "Anonymized stats can leave this phone.", anon, neonCyan) { anon = it }
        PrivacyToggle("Wearable auto-sync", "Health Connect can write readings into Labs.", wearable, neonCyan) { wearable = it }
        PrivacyToggle("Genetic personalization", "Lab and genetic files can be used in scans.", genetic, neonMagenta) { genetic = it }
        PrivacyToggle("On-device core", "The scan runs on this phone.", neuralCore, neonCyan) { neuralCore = it }
        CyberActionButton(confirmLabel, neonCyan) { onComplete(anon, wearable, genetic, neuralCore) }
    }
}

@Composable
fun PrivacyToggle(label: String, detail: String, checked: Boolean, color: Color, onCheckedChange: (Boolean) -> Unit) {
    val theme = LocalNeonTheme.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(checkedColor = color, uncheckedColor = color.copy(alpha = 0.3f))
        )
        Column {
            Text(label, color = theme.ink, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            Text(detail, color = theme.inkMuted, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
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
        Text("Log ${log.timestamp}", color = cyan, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        Text("Energy ${log.energyScore}", color = theme.ink, fontSize = 12.sp)
    }
}

@Composable
fun BioReadOnlyField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    color: Color,
    onClick: (() -> Unit)? = null
) {
    val theme = LocalNeonTheme.current
    val clickableModifier = if (onClick != null) modifier.clickable { onClick() } else modifier
    Column(modifier = clickableModifier) {
        Text(label, color = color.copy(alpha = 0.6f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text(value, color = theme.ink, fontSize = 14.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
    }
}

@Composable
fun BioSliderField(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    color: Color,
    onValueChange: (Float) -> Unit
) {
    val theme = LocalNeonTheme.current
    var sliderValue by remember(value) { mutableFloatStateOf(value) }
    val steps = ((range.endInclusive - range.start).roundToInt() - 1).coerceAtLeast(0)
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = color.copy(alpha = 0.8f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(
                "${sliderValue.roundToInt()} / ${range.endInclusive.roundToInt()}",
                color = theme.ink,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black
            )
        }
        Slider(
            value = sliderValue,
            onValueChange = { sliderValue = it },
            onValueChangeFinished = {
                val snapped = sliderValue.roundToInt().toFloat().coerceIn(range.start, range.endInclusive)
                sliderValue = snapped
                onValueChange(snapped)
            },
            valueRange = range,
            steps = steps,
            colors = SliderDefaults.colors(thumbColor = color, activeTrackColor = color, inactiveTrackColor = color.copy(alpha = 0.2f))
        )
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
                    Text("CALENDAR", color = theme.inkMuted, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    Text("$calendarAge", color = theme.ink, fontSize = 24.sp, fontWeight = FontWeight.Black)
                }
                Text(" VS ", color = color, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("BIOLOGICAL", color = color, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
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
            .clickable(onClickLabel = label) { onClick() }
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.Add, contentDescription = label, tint = color, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(8.dp))
        Text(label, color = color, fontSize = 12.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
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
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Live monitoring", color = theme.ink, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            Switch(
                checked = isLiveMonitoringEnabled,
                onCheckedChange = onLiveMonitoringToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = neonCyan,
                    checkedTrackColor = neonCyan.copy(alpha = 0.5f)
                )
            )
        }
        HorizontalDivider(color = neonCyan.copy(alpha = 0.2f), thickness = 0.5.dp)
        if (syncStatuses.isEmpty()) {
            Text("No active uplinks", color = theme.inkMuted, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        }
        syncStatuses.forEach { status -> UplinkStatusItem(status, neonCyan, neonMagenta, onRelink) }
        Button(
            onClick = onManualSyncClick,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            enabled = !isSyncing,
            colors = ButtonDefaults.buttonColors(
                containerColor = neonCyan.copy(alpha = 0.1f),
                disabledContainerColor = neonCyan.copy(alpha = 0.05f)
            ),
            border = BorderStroke(1.dp, neonCyan.copy(alpha = if (isSyncing) 0.2f else 0.5f)),
            shape = RoundedCornerShape(4.dp)
        ) {
            if (isSyncing) {
                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = neonCyan)
                Spacer(Modifier.width(8.dp))
                Text("Sync in progress", color = neonCyan, fontSize = 12.sp)
            } else {
                Icon(Icons.Default.Refresh, contentDescription = "Sync wearables", tint = neonCyan, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Sync wearables", color = neonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
        is UplinkStatus.Error, is UplinkStatus.PermissionRequired, is UplinkStatus.NeedsReAuth -> neonMagenta
        is UplinkStatus.Syncing -> Color.Yellow
        else -> theme.inkMuted
    }
    val statusText = when (val status = syncStatus.currentStatus) {
        is UplinkStatus.Connected -> "Connected"
        is UplinkStatus.Error -> "Error: ${status.message}"
        is UplinkStatus.PermissionRequired -> "Permission needed"
        is UplinkStatus.NeedsReAuth -> "Sign in again"
        is UplinkStatus.Syncing -> "Syncing (${(status.progress * 100).toInt()}%)"
        is UplinkStatus.Authenticating -> "Signing in"
        is UplinkStatus.Disconnected -> "Disconnected"
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(syncStatus.provider.name.replace("_", " "), color = theme.ink, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            Text(statusText, color = statusColor, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            syncStatus.lastSuccessfulSync?.let { lastSync ->
                Text("Last sync ${formatTimeAgo(lastSync)}", color = theme.inkMuted, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            }
        }
        if (syncStatus.currentStatus is UplinkStatus.Error ||
            syncStatus.currentStatus is UplinkStatus.PermissionRequired ||
            syncStatus.currentStatus is UplinkStatus.Disconnected ||
            syncStatus.currentStatus is UplinkStatus.NeedsReAuth
        ) {
            TextButton(onClick = { onRelink(syncStatus.provider) }) {
                Text("Re-link", color = neonCyan, fontSize = 12.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

fun formatTimeAgo(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 60_000 -> "Just now"
        diff < 3_600_000 -> "${diff / 60_000}m ago"
        diff < 86_400_000 -> "${diff / 3_600_000}h ago"
        else -> "${diff / 86_400_000}d ago"
    }
}

@Composable
fun BiometricTrendsSection(
    trends: List<HealthTrend>,
    selectedRange: Int,
    onRangeSelected: (Int) -> Unit,
    neonCyan: Color,
    neonMagenta: Color,
    onNavigateToGuide: (String?) -> Unit,
    onConnectWearable: () -> Unit
) {
    val theme = LocalNeonTheme.current
    val ranges = mapOf(7 to "7D", 30 to "30D", 90 to "90D")
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("TRENDS", color = neonCyan, fontSize = 12.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ranges.forEach { (days, label) ->
                    Text(
                        text = label,
                        color = if (selectedRange == days) neonCyan else theme.inkMuted,
                        fontSize = 12.sp,
                        fontWeight = if (selectedRange == days) FontWeight.Bold else FontWeight.Normal,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .clickable { onRangeSelected(days) }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
        }
        if (trends.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.Analytics, contentDescription = "No trend data", tint = theme.inkMuted, modifier = Modifier.size(32.dp))
                Spacer(Modifier.height(8.dp))
                Text("No wearable readings yet", color = theme.ink, fontSize = 12.sp, fontFamily = FontFamily.Monospace, textAlign = TextAlign.Center)
                TextButton(onClick = onConnectWearable) {
                    Text("Connect a wearable", color = neonCyan, fontSize = 12.sp)
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                trends.forEach { trend ->
                    TrendCard(trend, neonCyan, neonMagenta, onNavigateToGuide, Modifier.width(220.dp))
                }
            }
        }
    }
}

@Composable
fun TrendCard(
    trend: HealthTrend,
    neonCyan: Color,
    neonMagenta: Color,
    onNavigateToGuide: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = LocalNeonTheme.current
    var isExpanded by remember { mutableStateOf(false) }
    val statusColor = when (trend.label) {
        "SLEEP" -> if ((trend.currentValue.toFloatOrNull() ?: 8f) < 6.5f) neonMagenta else neonCyan
        "HRV" -> if ((trend.currentValue.toFloatOrNull() ?: 35f) < 35f) neonMagenta else neonCyan
        "STEPS" -> if ((trend.currentValue.toFloatOrNull() ?: 10000f) < 5000f) neonMagenta else neonCyan
        else -> neonCyan
    }
    val footnote = when (trend.label) {
        "SLEEP" -> "Sleep under 6.5 h"
        "HRV" -> "HRV under 35 ms"
        "STEPS" -> "Steps under 5,000"
        else -> null
    }
    CyberFrame(
        label = trend.label,
        borderColor = statusColor.copy(alpha = 0.6f),
        modifier = modifier.clickable { isExpanded = !isExpanded }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Text(trend.currentValue, color = theme.ink, fontSize = 24.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                Text(trend.unit, color = statusColor, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
            if (trend.label == "STEPS") {
                BarChart(trend.dataPoints, statusColor, Modifier.fillMaxWidth().height(60.dp))
            } else {
                Sparkline(trend.dataPoints, statusColor, Modifier.fillMaxWidth().height(60.dp))
            }
            if (footnote != null) {
                Text(footnote, color = theme.inkMuted, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            }
            AnimatedVisibility(visible = isExpanded) {
                Column {
                    trend.insight?.let { insight ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(insight, color = theme.ink, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { onNavigateToGuide("Analyze my ${trend.label} trend (${trend.currentValue} ${trend.unit}). How does this correlate with my active Directives?") },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = statusColor.copy(alpha = 0.1f)),
                        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(2.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(Icons.Default.Psychology, contentDescription = "Ask Neon Guide", modifier = Modifier.size(16.dp), tint = statusColor)
                        Spacer(Modifier.width(6.dp))
                        Text("Ask Neon Guide", color = statusColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun FeelingCompactCard(
    energy: Int,
    mood: Int,
    focus: Int,
    stress: Int,
    neonCyan: Color,
    onClick: () -> Unit
) {
    val theme = LocalNeonTheme.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(2.dp))
            .border(1.dp, neonCyan.copy(alpha = 0.2f), RoundedCornerShape(2.dp))
            .background(neonCyan.copy(alpha = 0.04f))
            .clickable(onClickLabel = "Log how I feel", onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        Text(
            text = "HOW I FEEL (TODAY)",
            color = neonCyan,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("ENERGY $energy/10", color = theme.ink, fontSize = 11.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
            Text("MOOD $mood/10", color = theme.ink, fontSize = 11.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
            Text("FOCUS $focus/10", color = theme.ink, fontSize = 11.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
            Text("STRESS $stress/10", color = theme.ink, fontSize = 11.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
fun BodyLogCard(
    samples: List<BodySampleEntity>,
    measurementUnit: String = "Metric",
    neonCyan: Color,
    onWeightClick: () -> Unit,
    onBodyFatClick: () -> Unit,
    onWaistClick: () -> Unit,
    onBloodPressureClick: () -> Unit
) {
    val isImperial = measurementUnit.equals("Imperial", ignoreCase = true)
    val lastWeight = samples.filter { it.metric == "WEIGHT" }.maxByOrNull { it.loggedAt }
    val lastBf = samples.filter { it.metric == "BF_PCT" }.maxByOrNull { it.loggedAt }
    val lastWaist = samples.filter { it.metric == "TAPE" && it.site?.contains("WAIST") == true }.maxByOrNull { it.loggedAt }
    val lastBpSys = samples.filter { it.metric == "BP_SYS" }.maxByOrNull { it.loggedAt }
    val lastBpDia = samples.filter { it.metric == "BP_DIA" }.maxByOrNull { it.loggedAt }
    val weightStr = lastWeight?.let {
        if (isImperial) String.format(Locale.US, "%.1f lb", it.value * 2.20462262)
        else String.format(Locale.US, "%.1f kg", it.value)
    } ?: "Not logged"
    val bfStr = lastBf?.let {
        val methodSuffix = it.method?.let { method -> " ($method)" } ?: ""
        String.format(Locale.US, "%.1f%%%s", it.value, methodSuffix)
    } ?: "Not logged"
    val waistStr = lastWaist?.let {
        val shown = waistDisplayValue(it.value, isImperial)
        if (isImperial) String.format(Locale.US, "%.1f in", shown) else String.format(Locale.US, "%.1f cm", shown)
    } ?: "Not logged"
    val bpStr = if (lastBpSys != null && lastBpDia != null) {
        val pos = lastBpSys.position?.lowercase() ?: ""
        "${lastBpSys.value.toInt()}/${lastBpDia.value.toInt()} $pos".trim()
    } else {
        "Not logged"
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BodyCompactField("Weight", weightStr, Modifier.weight(1f), neonCyan, "Log weight", onWeightClick)
            BodyCompactField("Body fat", bfStr, Modifier.weight(1f), neonCyan, "Log body fat", onBodyFatClick)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BodyCompactField("Waist", waistStr, Modifier.weight(1f), neonCyan, "Log waist", onWaistClick)
            BodyCompactField("Blood pressure", bpStr, Modifier.weight(1f), neonCyan, "Log blood pressure", onBloodPressureClick)
        }
    }
}

@Composable
private fun BodyCompactField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    color: Color,
    clickLabel: String,
    onClick: () -> Unit
) {
    val theme = LocalNeonTheme.current
    Column(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(2.dp))
            .border(1.dp, color.copy(alpha = 0.2f), RoundedCornerShape(2.dp))
            .background(color.copy(alpha = 0.04f))
            .clickable(onClickLabel = clickLabel, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(label, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        Text(value, color = theme.ink, fontSize = 14.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, maxLines = 1)
    }
}

@Composable
fun BarChart(data: List<Float>, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        if (data.isEmpty()) return@Canvas
        val max = data.maxOrNull()?.coerceAtLeast(1f) ?: 1f
        val barWidth = (size.width / data.size) * 0.8f
        val spacing = (size.width / data.size) * 0.2f
        data.forEachIndexed { index, value ->
            val barHeight = (value / max) * size.height
            drawRect(
                color = color.copy(alpha = 0.6f),
                topLeft = Offset(index * (barWidth + spacing), size.height - barHeight),
                size = Size(barWidth, barHeight)
            )
        }
    }
}

@Composable
fun Sparkline(data: List<Float>, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        if (data.size < 2) return@Canvas
        val min = data.minOrNull() ?: 0f
        val max = data.maxOrNull() ?: 1f
        val range = (max - min).coerceAtLeast(1f)
        val stepX = size.width / (data.size - 1)
        val points = data.mapIndexed { index, value ->
            Offset(index * stepX, size.height - ((value - min) / range * size.height))
        }
        val path = Path().apply {
            moveTo(points.first().x, points.first().y)
            for (index in 1 until points.size) lineTo(points[index].x, points[index].y)
        }
        drawPath(path, color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        val fillPath = Path().apply {
            addPath(path)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(fillPath, brush = Brush.verticalGradient(listOf(color.copy(alpha = 0.3f), Color.Transparent)))
    }
}
