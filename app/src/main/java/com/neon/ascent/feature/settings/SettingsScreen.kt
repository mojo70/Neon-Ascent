package com.neon.ascent.feature.settings

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import com.neon.ascent.ui.*
import com.neon.ascent.util.BiometricAuthManager
import com.neon.ascent.util.findFragmentActivity
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun MatrixRainBackground() {
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
                val alpha = (1f - (j / 15f)) * 0.08f
                if (y > 0 && y < size.height) {
                    drawRect(
                        color = Color(0xFF00FF9C).copy(alpha = alpha),
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

    val biometricAuthManager = remember { BiometricAuthManager(context) }
    val healthPermissionsLauncher = rememberLauncherForActivityResult(
        androidx.health.connect.client.PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        viewModel.checkHealthConnectStatus()
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
    
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0A0A0A))) {
        MatrixRainBackground()
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    drawContent()
                    val scanlineColor = Color.Black.copy(alpha = 0.1f)
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
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF00FF9C))
                }
                Text(
                    "//SYS_SETTINGS_CONSOLIDATED",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        color = Color(0xFF00FF9C),
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
                        Text("QUIET HOURS", color = Color(0xFF00FF9C).copy(alpha = 0.6f), fontSize = 10.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TimeField(label = "START", time = quietHoursStart, modifier = Modifier.weight(1f), onTimeSelected = {
                                viewModel.setQuietHoursStart(it)
                            })
                            TimeField(label = "END", time = quietHoursEnd, modifier = Modifier.weight(1f), onTimeSelected = {
                                viewModel.setQuietHoursEnd(it)
                            })
                        }
                        
                        Text("FREQUENCY", color = Color(0xFF00FF9C).copy(alpha = 0.6f), fontSize = 10.sp)
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
                            modifier = Modifier.fillMaxWidth().height(44.dp).border(1.dp, Color(0xFF00FF9C).copy(alpha = 0.5f), CyberButtonShape),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                        ) {
                            Text("TEST NEURAL BRIEF", color = Color(0xFF00FF9C), fontSize = 11.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Neon Guide
            SettingsSection(label = "NEON_GUIDE_CORE", icon = Icons.Default.Psychology) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("VERBOSITY", color = Color(0xFF00FF9C).copy(alpha = 0.6f), fontSize = 10.sp)
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

                    Text("EXPERT WEIGHTING", color = Color(0xFF00FF9C).copy(alpha = 0.6f), fontSize = 10.sp)
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
                            modifier = Modifier.fillMaxWidth().height(48.dp).border(1.dp, Color(0xFF00FF9C), CyberButtonShape),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1A1A))
                        ) {
                            Text("+ JACK IN HEALTH CONNECT", color = Color(0xFF00FF9C), fontSize = 12.sp)
                        }
                    } else {
                        DeviceStatusCard("HEALTH_CONNECT_API", "OPTIMAL", "SYNC_ACTIVE")
                    }
                    
                    SettingsItem("SYNC PREFERENCES") {
                        onNavigateToHealthPreferences()
                    }
                    
                    Button(
                        onClick = { viewModel.checkHealthConnectStatus() },
                        modifier = Modifier.fillMaxWidth().height(44.dp).border(1.dp, Color(0xFF00FF9C).copy(alpha = 0.5f), CyberButtonShape),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                    ) {
                        Text("FORCE MANUAL UPLINK", color = Color(0xFF00FF9C), fontSize = 11.sp)
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
                    Text("NEON INTENSITY", color = Color(0xFF00FF9C).copy(alpha = 0.6f), fontSize = 10.sp)
                    Slider(
                        value = neonIntensity,
                        onValueChange = { viewModel.setNeonIntensity(it) },
                        colors = SliderDefaults.colors(thumbColor = Color(0xFF00FF9C), activeTrackColor = Color(0xFF00FF9C))
                    )
                    
                    SettingsItem("CUSTOM AVATAR MODULE") {
                        // TODO: Open avatar selection
                    }
                    
                    SettingsItem("COLOR SCHEME: MATRIX_EMERALD") {
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

                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))

                    SettingsItem("EXPORT NEURAL LOG [.MD]") {
                        viewModel.exportNeuralLog()
                    }

                    SettingsItem("WIPE NEURAL PROFILE", color = Color(0xFFFF006E)) {
                        showResetDialog = true
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))

                    Button(
                        onClick = { viewModel.initializeWorkoutLibrary() },
                        modifier = Modifier.fillMaxWidth().height(44.dp).border(1.dp, Color(0xFF00CCFF).copy(alpha = 0.5f), CyberButtonShape),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                    ) {
                        Text("INITIALIZE WORKOUT LIBRARY", color = Color(0xFF00CCFF), fontSize = 11.sp)
                    }
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
                    color = Color.Gray, 
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
                Text("JACK OUT", color = Color(0xFFFF006E), fontWeight = FontWeight.Black, letterSpacing = 4.sp, modifier = Modifier.clickable { onBack() })
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
    }
}

@Composable
fun SettingsSection(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF00FF9C).copy(alpha = if (expanded) 0.5f else 0.1f), CyberButtonShape)
            .background(Color(0xFF111111).copy(alpha = 0.5f), CyberButtonShape)
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
                Icon(icon, contentDescription = null, tint = Color(0xFF00FF9C), modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Text(
                    label,
                    color = Color(0xFF00FF9C),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
            Icon(
                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = Color.Gray
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

    Column(modifier = modifier) {
        Text(label, color = Color.Gray, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .background(Color(0xFF1A1A1A))
                .border(1.dp, Color(0xFF00FF9C).copy(alpha = 0.2f))
                .clickable { showDialog = true }
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(time, color = Color.White, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
        }
    }

    if (showDialog) {
        Dialog(onDismissRequest = { showDialog = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F0F0F))
                    .border(1.dp, Color(0xFF00FF9C))
                    .padding(24.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("MANUAL_TIME_ENTRY", color = Color(0xFF00FF9C), fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = tempTime,
                        onValueChange = { tempTime = it },
                        placeholder = { Text("HH:mm", color = Color.Gray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00FF9C),
                            unfocusedBorderColor = Color.DarkGray,
                            focusedTextColor = Color.White
                        )
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = {
                            onTimeSelected(tempTime)
                            showDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF9C))
                    ) {
                        Text("SET_TIME", color = Color.Black)
                    }
                }
            }
        }
    }
}

@Composable
fun ToggleSetting(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(48.dp), 
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.White, fontSize = 13.sp)
        Switch(
            checked = checked, 
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF00FF9C),
                checkedTrackColor = Color(0xFF00FF9C).copy(alpha = 0.3f),
                uncheckedThumbColor = Color.DarkGray
            )
        )
    }
}

@Composable
fun SettingsItem(label: String, color: Color = Color(0xFF00FF9C), onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp)
    ) {
        Text(label, color = color, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, fontSize = 13.sp)
    }
}

@Composable
fun DeviceStatusCard(name: String, signal: String, status: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0F0F0F))
            .border(1.dp, Color(0xFF00FF9C).copy(alpha = 0.2f))
            .padding(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text("SIGNAL: $signal", color = Color(0xFF00FF9C), fontSize = 9.sp)
            }
            Text(status, color = Color(0xFF00FF9C), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SecretPasswordDialog(onCorrectPassword: () -> Unit, onDismiss: () -> Unit) {
    var password by remember { mutableStateOf("") }
    val haptic = LocalHapticFeedback.current
    
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .clip(CyberButtonShape)
                .background(Color(0xFF0F0F0F))
                .border(2.dp, Color(0xFFFF006E), CyberButtonShape)
                .padding(24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(">>> ACCESS_RESTRICTED <<<", color = Color(0xFFFF006E), fontWeight = FontWeight.Black, fontSize = 18.sp)
                Spacer(Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = { Text("ENTER PASSWORD", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFFF006E),
                        unfocusedBorderColor = Color.DarkGray,
                        focusedTextColor = Color.White
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
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF006E))
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
                .background(Color(0xFF0F0F0F))
                .border(2.dp, Color(0xFFFF006E), CyberButtonShape)
                .padding(24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(title, color = Color(0xFFFF006E), fontWeight = FontWeight.Black, fontSize = 20.sp)
                Spacer(Modifier.height(8.dp))
                Text(description, color = Color.White, fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it.uppercase() },
                    placeholder = { Text("TYPE TO CONFIRM", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFFF006E),
                        unfocusedBorderColor = Color.DarkGray,
                        focusedTextColor = Color.White
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
                        containerColor = Color(0xFFFF006E),
                        disabledContainerColor = Color.DarkGray
                    )
                ) {
                    Text(if (countdown > 0) "WAITING... $countdown" else "EXECUTE OVERRIDE", color = Color.White)
                }
            }
        }
    }
}
