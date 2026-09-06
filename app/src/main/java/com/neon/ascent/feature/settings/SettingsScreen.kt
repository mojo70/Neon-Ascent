package com.neon.ascent.feature.settings

import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import com.neon.ascent.ui.*
import com.neon.ascent.core.common.*
import com.neon.ascent.core.domain.backup.models.RestoreMode
import com.neon.ascent.util.BiometricAuthManager
import com.neon.ascent.util.findFragmentActivity
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

@Composable
fun MatrixRainBackground() {
    val theme = LocalNeonTheme.current
    val columns = 15
    val rainState = remember { Array(columns) { Random.nextFloat() * 40f } }
    val infiniteTransition = rememberInfiniteTransition(label = "MatrixRain")
    
    val frame by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(100, easing = LinearEasing)),
        label = "frame"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        for (i in 0 until columns) {
            rainState[i] += 0.2f
            if (rainState[i] > 50f) rainState[i] = -Random.nextFloat() * 20f
            
            val x = (size.width / columns) * i
            for (j in 0 until 15) {
                val y = (rainState[i] + j) * (size.height / 40)
                val alpha = if (theme.mode == VisualMode.CYBER) {
                    (1f - (j / 15f)) * 0.08f
                } else {
                    0.04f // Steve: gray ticks @ 4%
                }
                
                if (y > 0 && y < size.height) {
                    drawRect(
                        color = theme.ink.copy(alpha = alpha),
                        topLeft = Offset(x, y),
                        size = Size(2.dp.toPx(), 8.dp.toPx())
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onResetComplete: () -> Unit,
    onDeepNodeUnlock: () -> Unit,
    onNavigateToHealthPreferences: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val theme = LocalNeonTheme.current
    
    // Theme Mode State
    val visualMode by viewModel.visualMode.collectAsState()

    // Notifications State
    val isNeuralBriefEnabled by viewModel.isNeuralBriefEnabled.collectAsState()
    val quietHoursStart by viewModel.quietHoursStart.collectAsState()
    val quietHoursEnd by viewModel.quietHoursEnd.collectAsState()
    val briefFrequency by viewModel.briefFrequency.collectAsState()

    // Neon Guide State
    val guideVerbosity by viewModel.guideVerbosity.collectAsState()
    val cloudFallbackEnabled by viewModel.cloudFallbackEnabled.collectAsState()
    val expertWeighting by viewModel.expertWeighting.collectAsState()

    // Biometrics & Sync State
    val isHealthGranted by viewModel.isHealthConnectGranted.collectAsState()
    val biometricLockEnabled by viewModel.isBiometricLockEnabled.collectAsState()
    
    // Appearance State
    val neonIntensity by viewModel.neonIntensity.collectAsState()
    val isDopamineMenuVisible by viewModel.isDopamineMenuVisible.collectAsState()
    val isSelfMapVisible by viewModel.isSelfMapVisible.collectAsState()

    // Privacy State
    val isShardVaultEnabled by viewModel.isShardVaultEnabled.collectAsState()
    val localAiOnly by viewModel.isLocalAiOnly.collectAsState()

    // Backup & Cloud State
    val backupFrequency by viewModel.backupFrequency.collectAsState()
    val backupScopeWorkout by viewModel.backupScopeWorkout.collectAsState()
    val backupScopeBiometrics by viewModel.backupScopeBiometrics.collectAsState()
    val backupScopeCodex by viewModel.backupScopeCodex.collectAsState()
    val backupScopeJournal by viewModel.backupScopeJournal.collectAsState()
    val backupScopeCharacter by viewModel.backupScopeCharacter.collectAsState()
    val backupWifiOnly by viewModel.backupWifiOnly.collectAsState()
    val backupRequireCharging by viewModel.backupRequireCharging.collectAsState()
    val lastBackupTimestamp by viewModel.lastBackupTimestamp.collectAsState()
    val backupToastMessage by viewModel.backupToastMessage.collectAsState()
    val pendingRestoreJson by viewModel.pendingRestoreJson.collectAsState()

    val biometricAuthManager = remember { BiometricAuthManager(context) }
    val healthPermissionsLauncher = rememberLauncherForActivityResult(
        androidx.health.connect.client.PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        viewModel.checkHealthConnectStatus()
    }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { contentUri ->
            viewModel.backupExportEvent.replayCache.lastOrNull()?.let { json ->
                context.contentResolver.openOutputStream(contentUri)?.use { out ->
                    out.write(json.toByteArray())
                }
                Toast.makeText(context, "Saved to Cloud Storage", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { contentUri ->
            val jsonContent = context.contentResolver.openInputStream(contentUri)?.use { input ->
                input.bufferedReader().use { it.readText() }
            }
            jsonContent?.let { json ->
                viewModel.onRestoreFileSelected(json)
            }
        }
    }
    
    var showResetDialog by remember { mutableStateOf(false) }
    var buildHashClickCount by remember { mutableStateOf(0) }
    var showPasswordDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.exportEvent.collect { logContent ->
            val sendIntent = android.content.Intent().apply {
                action = android.content.Intent.ACTION_SEND
                putExtra(android.content.Intent.EXTRA_TEXT, logContent)
                type = "text/markdown"
            }
            val shareIntent = android.content.Intent.createChooser(sendIntent, "EXPORT NEURAL LOG")
            context.startActivity(shareIntent)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.backupExportEvent.collect { jsonContent ->
            createDocumentLauncher.launch("neon_ascent_backup_${System.currentTimeMillis()}.json")
        }
    }

    LaunchedEffect(backupToastMessage) {
        backupToastMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            viewModel.clearBackupToast()
        }
    }
    
    Box(modifier = Modifier.fillMaxSize().background(theme.canvas)) {
        MatrixRainBackground()
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    drawContent()
                    val scanlineColor = theme.ink.copy(alpha = if (theme.mode == VisualMode.CYBER) 0.1f else 0.04f)
                    for (i in 0 until size.height.toInt() step 6) {
                        drawLine(scanlineColor, Offset(0f, i.toFloat()), Offset(size.width, i.toFloat()), strokeWidth = 1f)
                    }
                }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onBack()
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = theme.ink)
                }
                Text(
                    "//SYS_SETTINGS_CONSOLIDATED",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        color = theme.ink,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 1. Notifications
            SettingsSection(label = "NEURAL_PINGS", icon = Icons.Default.Notifications) {
                ToggleSetting("Neural Brief Protocol", isNeuralBriefEnabled) {
                    viewModel.setNeuralBriefEnabled(it)
                }
                
                AnimatedVisibility(visible = isNeuralBriefEnabled) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("QUIET HOURS", color = theme.ink.copy(alpha = 0.6f), fontSize = 10.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TimeField(label = "START", time = quietHoursStart, modifier = Modifier.weight(1f), onTimeSelected = {
                                viewModel.setQuietHoursStart(it)
                            })
                            TimeField(label = "END", time = quietHoursEnd, modifier = Modifier.weight(1f), onTimeSelected = {
                                viewModel.setQuietHoursEnd(it)
                            })
                        }
                        
                        Text("FREQUENCY", color = theme.ink.copy(alpha = 0.6f), fontSize = 10.sp)
                        Row(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("DAILY", "BI_DAILY", "WEEKLY").forEach { freq ->
                                CyberTabButton(
                                    selected = briefFrequency == freq,
                                    onClick = { viewModel.setBriefFrequency(freq) },
                                    label = freq,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        
                        Button(
                            onClick = { viewModel.debugTriggerTestBrief() },
                            modifier = Modifier.fillMaxWidth().height(44.dp).border(1.dp, theme.ink.copy(alpha = 0.5f), CyberButtonShape),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                        ) {
                            Text("TEST NEURAL BRIEF", color = theme.ink, fontSize = 11.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Neon Guide
            SettingsSection(label = "NEON_GUIDE_CORE", icon = Icons.Default.Psychology) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("VERBOSITY", color = theme.ink.copy(alpha = 0.6f), fontSize = 10.sp)
                    Row(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("MINIMAL", "STANDARD", "VERBOSE").forEach { verb ->
                            CyberTabButton(
                                selected = guideVerbosity == verb,
                                onClick = { viewModel.setGuideVerbosity(verb) },
                                label = verb,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    ToggleSetting("Cloud Fallback Support", cloudFallbackEnabled) {
                        viewModel.setCloudFallbackEnabled(it)
                    }

                    Text("EXPERT WEIGHTING", color = theme.ink.copy(alpha = 0.6f), fontSize = 10.sp)
                    Row(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("LOGIC", "BALANCED", "CREATIVE").forEach { weight ->
                            CyberTabButton(
                                selected = expertWeighting == weight,
                                onClick = { viewModel.setExpertWeighting(weight) },
                                label = weight,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Biometrics & Sync
            SettingsSection(label = "UPLINK_STATUS", icon = Icons.Default.Sync) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (!isHealthGranted) {
                        Button(
                            onClick = { healthPermissionsLauncher.launch(viewModel.getHealthPermissions()) },
                            modifier = Modifier.fillMaxWidth().height(48.dp).border(1.dp, theme.ink, CyberButtonShape),
                            colors = ButtonDefaults.buttonColors(containerColor = theme.surfaceRaised)
                        ) {
                            Text("+ JACK IN HEALTH CONNECT", color = theme.ink, fontSize = 12.sp)
                        }
                    } else {
                        DeviceStatusCard("HEALTH_CONNECT_API", "OPTIMAL", "SYNC_ACTIVE")
                    }
                    
                    SettingsItem("SYNC PREFERENCES") {
                        onNavigateToHealthPreferences()
                    }
                    
                    Button(
                        onClick = { viewModel.checkHealthConnectStatus() },
                        modifier = Modifier.fillMaxWidth().height(44.dp).border(1.dp, theme.ink.copy(alpha = 0.5f), CyberButtonShape),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                    ) {
                        Text("FORCE MANUAL UPLINK", color = theme.ink, fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Data & Cloud Backup
            SettingsSection(label = "DATA_AND_CLOUD_BACKUP", icon = Icons.Default.CloudUpload) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Status Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(theme.surface)
                            .border(1.dp, theme.ink.copy(alpha = 0.2f))
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("SYSTEM AUTO-BACKUP", color = theme.ink, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("ACTIVE", color = Color(0xFF00FF66), fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                            Text("MANAGED BY ANDROID OS // GOOGLE DRIVE", color = theme.ink.copy(alpha = 0.7f), fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(top = 2.dp))
                            
                            val lastBackupDateStr = if (lastBackupTimestamp > 0L) {
                                SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(
                                    Date(lastBackupTimestamp)
                                )
                            } else "AUTOMATIC (OVERNIGHT ON WI-FI)"
                            Text("LAST MANUAL EXPORT: $lastBackupDateStr", color = theme.ink.copy(alpha = 0.5f), fontSize = 9.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(top = 2.dp))
                        }
                    }

                    Text("AUTOMATIC BACKUP SCHEDULE", color = theme.ink.copy(alpha = 0.6f), fontSize = 10.sp)
                    Row(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("DAILY", "WEEKLY", "POST_WORKOUT", "MANUAL").forEach { freq ->
                            val isSelected = backupFrequency == freq || (freq == "POST_WORKOUT" && backupFrequency == "AFTER_WORKOUT") || (freq == "MANUAL" && backupFrequency == "MANUAL_ONLY")
                            CyberTabButton(
                                selected = isSelected,
                                onClick = {
                                    val mapped = when (freq) {
                                        "POST_WORKOUT" -> "AFTER_WORKOUT"
                                        "MANUAL" -> "MANUAL_ONLY"
                                        else -> freq
                                    }
                                    viewModel.setBackupFrequency(mapped)
                                },
                                label = freq,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Text("BACKUP SCOPE", color = theme.ink.copy(alpha = 0.6f), fontSize = 10.sp)
                    ToggleSetting("Workout Logs & History", backupScopeWorkout) {
                        viewModel.setBackupScopeWorkout(it)
                    }
                    ToggleSetting("Biometrics & Health Vitals", backupScopeBiometrics) {
                        viewModel.setBackupScopeBiometrics(it)
                    }
                    ToggleSetting("Codex & Knowledge Vault", backupScopeCodex) {
                        viewModel.setBackupScopeCodex(it)
                    }
                    ToggleSetting("Journal & Reflections", backupScopeJournal) {
                        viewModel.setBackupScopeJournal(it)
                    }
                    ToggleSetting("Character & Progression", backupScopeCharacter) {
                        viewModel.setBackupScopeCharacter(it)
                    }

                    Text("NETWORK CONSTRAINTS", color = theme.ink.copy(alpha = 0.6f), fontSize = 10.sp)
                    ToggleSetting("Wi-Fi Only Sync", backupWifiOnly) {
                        viewModel.setBackupWifiOnly(it)
                    }
                    ToggleSetting("Require Charging", backupRequireCharging) {
                        viewModel.setBackupRequireCharging(it)
                    }

                    HorizontalDivider(color = theme.ink.copy(alpha = 0.1f))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { viewModel.triggerManualBackup() },
                            modifier = Modifier.weight(1f).height(44.dp).border(1.dp, theme.ink, CyberButtonShape),
                            colors = ButtonDefaults.buttonColors(containerColor = theme.surfaceRaised)
                        ) {
                            Text("BACKUP NOW", color = theme.ink, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { openDocumentLauncher.launch("application/json") },
                            modifier = Modifier.weight(1f).height(44.dp).border(1.dp, theme.ink.copy(alpha = 0.5f), CyberButtonShape),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                        ) {
                            Text("RESTORE (.JSON)", color = theme.ink, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Dopamine Menu & Self Map
            SettingsSection(label = "NEURAL_INTERFACE", icon = Icons.Default.Dashboard) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    ToggleSetting("Show Dopamine Menu", isDopamineMenuVisible) {
                        viewModel.setDopamineMenuVisible(it)
                    }
                    ToggleSetting("Show Self Map Grid", isSelfMapVisible) {
                        viewModel.setSelfMapVisible(it)
                    }
                    SettingsItem("EDIT INTERFACE LAYOUT") {
                        // TODO: Navigate to layout editor
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 5. Appearance & Theme
            SettingsSection(label = "VISUAL_STIMULI", icon = Icons.Default.Palette) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("MODE", color = theme.ink.copy(alpha = 0.6f), fontSize = 10.sp)
                    Row(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        VisualMode.entries.forEach { mode ->
                            val previewPalette = when(mode) {
                                VisualMode.CYBER -> NeonThemeData.cyber(neonIntensity)
                                VisualMode.STEVE -> NeonThemeData.steve(neonIntensity)
                            }
                            CyberTabButton(
                                selected = visualMode == mode,
                                onClick = { viewModel.setVisualMode(mode) },
                                label = mode.name,
                                modifier = Modifier.weight(1f),
                                palette = previewPalette
                            )
                        }
                    }
                    Text(
                        if (visualMode == VisualMode.CYBER) "NIGHT HUD // NEON ON VOID" else "DAY HUD // INK ON PAPER",
                        color = theme.ink.copy(alpha = 0.4f),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text("NEON INTENSITY", color = theme.ink.copy(alpha = 0.6f), fontSize = 10.sp)
                    Slider(
                        value = neonIntensity,
                        onValueChange = { viewModel.setNeonIntensity(it) },
                        colors = SliderDefaults.colors(thumbColor = theme.ink, activeTrackColor = theme.ink)
                    )
                    Text(
                        if (visualMode == VisualMode.CYBER) "NIGHT HUD // GLOW GAIN" else "DAY HUD // INK WEIGHT",
                        color = theme.ink.copy(alpha = 0.4f),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    
                    SettingsItem("CUSTOM AVATAR MODULE") {
                        // TODO: Open avatar selection
                    }
                    
                    SettingsItem(if (visualMode == VisualMode.CYBER) "COLOR SCHEME: MATRIX_EMERALD" else "COLOR SCHEME: STEVE_HI-CON") {
                        // TODO: Theme picker
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 6. Privacy & Data
            SettingsSection(label = "SHARD_VAULT", icon = Icons.Default.Security) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    ToggleSetting("Shard Vault Encryption", isShardVaultEnabled) {
                        viewModel.setShardVaultEnabled(it)
                    }
                    
                    ToggleSetting("Local AI Core Only", localAiOnly) {
                        viewModel.setLocalAiOnly(it)
                    }

                    ToggleSetting("Biometric Node Lock", biometricLockEnabled) { enabled ->
                        val activity = context.findFragmentActivity()
                        if (activity != null) {
                            if (enabled) {
                                biometricAuthManager.authenticate(
                                    activity,
                                    "ENABLE LOCK",
                                    "Confirm identity",
                                    onSuccess = { viewModel.setBiometricLockEnabled(true) },
                                    onError = {}
                                )
                            } else {
                                biometricAuthManager.authenticate(
                                    activity,
                                    "DISABLE LOCK",
                                    "Confirm identity",
                                    onSuccess = { viewModel.setBiometricLockEnabled(false) },
                                    onError = {}
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = theme.ink.copy(alpha = 0.05f))

                    SettingsItem("EXPORT NEURAL LOG [.MD]") {
                        viewModel.exportNeuralLog()
                    }

                    SettingsItem("WIPE NEURAL PROFILE", color = theme.secondary) {
                        showResetDialog = true
                    }

                    HorizontalDivider(color = theme.ink.copy(alpha = 0.05f))

                    Button(
                        onClick = { viewModel.initializeWorkoutLibrary() },
                        modifier = Modifier.fillMaxWidth().height(44.dp).border(1.dp, Color(0xFF00CCFF).copy(alpha = 0.5f), CyberButtonShape),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                    ) {
                        Text("INITIALIZE WORKOUT LIBRARY", color = Color(0xFF00CCFF), fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 7. Credits & Attribution
            SettingsSection(label = "SYSTEM_CREDITS", icon = Icons.Default.Info) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "EXTERNAL_ASSETS //",
                        color = theme.ink.copy(alpha = 0.6f),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        "gong.wav by reinsamba -- https://freesound.org/s/46062/ -- License: Attribution 4.0",
                        color = theme.ink.copy(alpha = 0.8f),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 16.sp
                    )
                    
                    Spacer(Modifier.height(16.dp))

                    Text(
                        "TRAINING_ORIGINS //",
                        color = theme.ink.copy(alpha = 0.6f),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        "CyberCrapp is based on the DoggCrapp (DC) training system created by Dante Trudel. Originally posted on the IntenseMuscle forums.",
                        color = theme.ink.copy(alpha = 0.8f),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 16.sp
                    )

                    Text(
                        "https://www.intensemuscle.com/forum/main-forums/the-dogg-pound/20286-updated-dc-training-newbies-read-this-first-and-then-ask-questions-later",
                        color = theme.ink.copy(alpha = 0.5f),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 14.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    
                    Text(
                        "CORE_OS // NEON_ASCENT_LABS",
                        color = theme.ink.copy(alpha = 0.4f),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Footer
            Column(
                horizontalAlignment = Alignment.CenterHorizontally, 
                modifier = Modifier.fillMaxWidth().padding(bottom = 40.dp)
            ) {
                Text(
                    text = "BUILD_HASH: 7F2A91X_CONSOLIDATED", 
                    color = theme.inkMuted, 
                    fontSize = 10.sp, 
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .clickable {
                            buildHashClickCount++
                            if (buildHashClickCount >= 7) {
                                showPasswordDialog = true
                                buildHashClickCount = 0
                            }
                        }
                        .padding(16.dp)
                )
                Text("JACK OUT", color = theme.secondary, fontWeight = FontWeight.Black, letterSpacing = 4.sp, modifier = Modifier.clickable { onBack() })
            }
        }

        if (showResetDialog) {
            CyberConfirmDialog(
                title = "WIPE PROFILE?",
                description = "Permanent deletion of all neural progress. Type 'JACKIN' to confirm.",
                confirmText = "JACKIN",
                onConfirm = { 
                    viewModel.resetProfile { 
                        showResetDialog = false
                        onResetComplete()
                    }
                },
                onDismiss = { showResetDialog = false }
            )
        }

        if (showPasswordDialog) {
            SecretPasswordDialog(
                onCorrectPassword = {
                    showPasswordDialog = false
                    onDeepNodeUnlock()
                },
                onDismiss = { showPasswordDialog = false }
            )
        }

        pendingRestoreJson?.let { json ->
            Dialog(onDismissRequest = { viewModel.dismissRestoreDialog() }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(CyberButtonShape)
                        .background(theme.surface)
                        .border(2.dp, theme.ink, CyberButtonShape)
                        .padding(24.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("UPLINK_RESTORE_STRATEGY", color = theme.ink, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Select how to merge or replace local data with the imported backup JSON file:",
                            color = theme.ink.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(20.dp))

                        Button(
                            onClick = { viewModel.confirmRestore(RestoreMode.MERGE) },
                            modifier = Modifier.fillMaxWidth().height(48.dp).border(1.dp, theme.ink, CyberButtonShape),
                            colors = ButtonDefaults.buttonColors(containerColor = theme.surfaceRaised)
                        ) {
                            Text("MERGE (COMBINE WITH LOCAL)", color = theme.ink, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(Modifier.height(12.dp))

                        Button(
                            onClick = { viewModel.confirmRestore(RestoreMode.REPLACE) },
                            modifier = Modifier.fillMaxWidth().height(48.dp).border(1.dp, theme.secondary, CyberButtonShape),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                        ) {
                            Text("REPLACE (WIPE & OVERWRITE)", color = theme.secondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(Modifier.height(12.dp))

                        TextButton(onClick = { viewModel.dismissRestoreDialog() }) {
                            Text("CANCEL", color = theme.inkMuted)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsSection(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val theme = LocalNeonTheme.current
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, theme.ink.copy(alpha = if (expanded) 0.5f else 0.1f), CyberButtonShape)
            .background(theme.surface.copy(alpha = 0.5f), CyberButtonShape)
            .clip(CyberButtonShape)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = theme.ink, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Text(
                    label,
                    color = theme.ink,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
            Icon(
                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = theme.inkMuted
            )
        }
        
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Box(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                content()
            }
        }
    }
}

@Composable
fun TimeField(label: String, time: String, modifier: Modifier = Modifier, onTimeSelected: (String) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    var tempTime by remember { mutableStateOf(time) }
    val theme = LocalNeonTheme.current

    Column(modifier = modifier) {
        Text(label, color = theme.inkMuted, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .background(theme.surfaceRaised)
                .border(1.dp, theme.ink.copy(alpha = 0.2f))
                .clickable { showDialog = true }
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(time, color = theme.ink, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
        }
    }

    if (showDialog) {
        Dialog(onDismissRequest = { showDialog = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(theme.surface)
                    .border(1.dp, theme.ink)
                    .padding(24.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("MANUAL_TIME_ENTRY", color = theme.ink, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = tempTime,
                        onValueChange = { tempTime = it },
                        placeholder = { Text("HH:mm", color = theme.inkMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = theme.ink,
                            unfocusedBorderColor = theme.inkMuted,
                            focusedTextColor = theme.ink
                        )
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = {
                            onTimeSelected(tempTime)
                            showDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = theme.ink)
                    ) {
                        Text("SET_TIME", color = theme.canvas)
                    }
                }
            }
        }
    }
}

@Composable
fun ToggleSetting(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val theme = LocalNeonTheme.current
    Row(
        modifier = Modifier.fillMaxWidth().height(48.dp), 
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = theme.ink, fontSize = 13.sp)
        Switch(
            checked = checked, 
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = theme.ink,
                checkedTrackColor = theme.ink.copy(alpha = 0.3f),
                uncheckedThumbColor = theme.inkMuted
            )
        )
    }
}

@Composable
fun SettingsItem(label: String, color: Color? = null, onClick: () -> Unit) {
    val theme = LocalNeonTheme.current
    val finalColor = color ?: theme.ink
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp)
    ) {
        Text(label, color = finalColor, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, fontSize = 13.sp)
    }
}

@Composable
fun DeviceStatusCard(name: String, signal: String, status: String) {
    val theme = LocalNeonTheme.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(theme.surface)
            .border(1.dp, theme.ink.copy(alpha = 0.2f))
            .padding(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(name, color = theme.ink, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text("SIGNAL: $signal", color = theme.ink.copy(alpha = 0.6f), fontSize = 9.sp)
            }
            Text(status, color = theme.ink, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SecretPasswordDialog(onCorrectPassword: () -> Unit, onDismiss: () -> Unit) {
    var password by remember { mutableStateOf("") }
    val haptic = LocalHapticFeedback.current
    val theme = LocalNeonTheme.current
    
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .clip(CyberButtonShape)
                .background(theme.surface)
                .border(2.dp, theme.secondary, CyberButtonShape)
                .padding(24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(">>> ACCESS_RESTRICTED <<<", color = theme.secondary, fontWeight = FontWeight.Black, fontSize = 18.sp)
                Spacer(Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = { Text("ENTER PASSWORD", color = theme.inkMuted) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = theme.secondary,
                        unfocusedBorderColor = theme.inkMuted,
                        focusedTextColor = theme.ink
                    )
                )
                
                Spacer(Modifier.height(24.dp))
                
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (password == "!c3Br3@k3r") {
                            onCorrectPassword()
                        } else {
                            password = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp).clip(CyberButtonShape),
                    colors = ButtonDefaults.buttonColors(containerColor = theme.secondary)
                ) {
                    Text("BREAK ICE", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun CyberConfirmDialog(
    title: String,
    description: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    var input by remember { mutableStateOf("") }
    var countdown by remember { mutableStateOf(3) }
    val haptic = LocalHapticFeedback.current
    val theme = LocalNeonTheme.current
    
    LaunchedEffect(Unit) {
        while(countdown > 0) {
            delay(1000)
            countdown--
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .clip(CyberButtonShape)
                .background(theme.surface)
                .border(2.dp, theme.secondary, CyberButtonShape)
                .padding(24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(title, color = theme.secondary, fontWeight = FontWeight.Black, fontSize = 20.sp)
                Spacer(Modifier.height(8.dp))
                Text(description, color = theme.ink, fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it.uppercase() },
                    placeholder = { Text("TYPE TO CONFIRM", color = theme.inkMuted) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = theme.secondary,
                        unfocusedBorderColor = theme.inkMuted,
                        focusedTextColor = theme.ink
                    )
                )
                
                Spacer(Modifier.height(16.dp))
                
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onConfirm()
                    },
                    enabled = input == confirmText && countdown == 0,
                    modifier = Modifier.fillMaxWidth().height(50.dp).clip(CyberButtonShape),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = theme.secondary,
                        disabledContainerColor = theme.inkMuted
                    )
                ) {
                    Text(if (countdown > 0) "WAITING... $countdown" else "EXECUTE OVERRIDE", color = Color.White)
                }
            }
        }
    }
}
