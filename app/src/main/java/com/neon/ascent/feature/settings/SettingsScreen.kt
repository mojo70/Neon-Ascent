package com.neon.ascent.feature.settings

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
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
    onNavigateToHealthPreferences: () -> Unit,
    onNavigateToNotificationPermission: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val biometricLockEnabled by viewModel.isBiometricLockEnabled.collectAsState()
    val localAiOnly by viewModel.isLocalAiOnly.collectAsState()
    val measurementUnit by viewModel.measurementUnit.collectAsState()
    val isHealthGranted by viewModel.isHealthConnectGranted.collectAsState()
    val biometricAuthManager = remember { BiometricAuthManager(context) }
    
    val healthPermissionsLauncher = rememberLauncherForActivityResult(
        androidx.health.connect.client.PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        viewModel.checkHealthConnectStatus()
    }
    
    var showResetDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    var pinAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    // Secret screen states
    var buildHashClickCount by remember { mutableStateOf(0) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    
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
                    "//SYS_SETTINGS v0.8.4",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        color = Color(0xFF00FF9C),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            CyberFrame(label = "CHARACTER CORE") {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SettingsItem("EDIT AVATAR / BIO", onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    })
                    SettingsItem("RESET NEURAL PROFILE", color = Color(0xFFFF006E), onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val action = { showResetDialog = true }
                        if (biometricLockEnabled) {
                            if (biometricAuthManager.canAuthenticate()) {
                                biometricAuthManager.authenticate(
                                    context as FragmentActivity,
                                    "RESET PROFILE",
                                    "Confirm biometric signature to wipe profile",
                                    onSuccess = action,
                                    onError = { 
                                        pinAction = action
                                        showPinDialog = true 
                                    }
                                )
                            } else {
                                pinAction = action
                                showPinDialog = true
                            }
                        } else {
                            action()
                        }
                    })
                    SettingsItem("EXPORT_CHARACTER [.JSON]", onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val action = { /* Proceed with export */ }
                        if (biometricLockEnabled) {
                            if (biometricAuthManager.canAuthenticate()) {
                                biometricAuthManager.authenticate(
                                    context as FragmentActivity,
                                    "EXPORT DATA",
                                    "Confirm biometric signature to download character data",
                                    onSuccess = action,
                                    onError = { 
                                        pinAction = action
                                        showPinDialog = true 
                                    }
                                )
                            } else {
                                pinAction = action
                                showPinDialog = true
                            }
                        } else {
                            action()
                        }
                    })
                    SettingsItem("IMPORT_CHARACTER [.JSON]", onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    })
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            CyberFrame(label = "WEARABLE LINKS") {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (!isHealthGranted) {
                        Button(
                            onClick = { 
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                healthPermissionsLauncher.launch(viewModel.getHealthPermissions())
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .clip(CyberButtonShape)
                                .border(1.dp, Color(0xFF00FF9C), CyberButtonShape),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1A1A))
                        ) {
                            Text("+ JACK IN HEALTH CONNECT", color = Color(0xFF00FF9C), fontWeight = FontWeight.Black)
                        }
                    } else {
                        DeviceStatusCard("HEALTH_CONNECT_API", "OPTIMAL", "CONNECTED // SYNCED")
                    }

                    SettingsItem("HEALTH & BIOMETRICS CONFIG", onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onNavigateToHealthPreferences()
                    })

                    SettingsItem("NEURAL PING PROTOCOLS", onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onNavigateToNotificationPermission()
                    })
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = isHealthGranted, 
                            onCheckedChange = { 
                                if (!isHealthGranted) {
                                    healthPermissionsLauncher.launch(viewModel.getHealthPermissions())
                                }
                            },
                            colors = CheckboxDefaults.colors(checkedColor = Color(0xFF00FF9C), uncheckedColor = Color.DarkGray)
                        )
                        Text("SHARE BIOMETRICS TO BOOST STATS", color = Color.White, fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            CyberFrame(label = "INTERFACE PROTOCOLS") {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column {
                        Text("NEON INTENSITY", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                        Slider(
                            value = 0.8f, 
                            onValueChange = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) }, 
                            colors = SliderDefaults.colors(thumbColor = Color(0xFF00FF9C), activeTrackColor = Color(0xFF00FF9C))
                        )
                    }
                    
                    ToggleSetting("TERMINAL BEEPS", true, onCheckedChange = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) })
                    ToggleSetting("RAIN NOISE", false, onCheckedChange = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) })
                    ToggleSetting("ICE ALERTS", true, onCheckedChange = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) })
                    ToggleSetting("RUNNER BOUNTIES", true, onCheckedChange = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) })
                    
                    ToggleSetting(
                        label = "LOCAL AI CORE ONLY", 
                        checked = localAiOnly,
                        onCheckedChange = { 
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.setLocalAiOnly(it)
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text("SYSTEM_UNITS", color = Color(0xFF00FF9C).copy(alpha = 0.6f), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        Modifier.fillMaxWidth().selectableGroup(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CyberTabButton(
                            selected = measurementUnit == "Imperial", 
                            onClick = { 
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.setMeasurementUnit("Imperial") 
                            }, 
                            label = "IMPERIAL", 
                            modifier = Modifier.weight(1f)
                        )
                        CyberTabButton(
                            selected = measurementUnit == "Metric", 
                            onClick = { 
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.setMeasurementUnit("Metric") 
                            }, 
                            label = "METRIC", 
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            CyberFrame(label = "SECURITY & LOGS") {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SettingsItem("DATA EXPORT [.LOG]", onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val action = { /* Export logs */ }
                        if (biometricLockEnabled) {
                            if (biometricAuthManager.canAuthenticate()) {
                                biometricAuthManager.authenticate(
                                    context as FragmentActivity,
                                    "EXPORT LOGS",
                                    "Confirm biometric signature to export system logs",
                                    onSuccess = action,
                                    onError = { 
                                        pinAction = action
                                        showPinDialog = true 
                                    }
                                )
                            } else {
                                pinAction = action
                                showPinDialog = true
                            }
                        } else {
                            action()
                        }
                    })
                    SettingsItem("DELETE ACCOUNT", color = Color(0xFFFF006E), onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showDeleteDialog = true
                    })
                    ToggleSetting(
                        label = "BIOMETRIC LOCK", 
                        checked = biometricLockEnabled,
                        onCheckedChange = { enabled ->
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (enabled) {
                                if (biometricAuthManager.canAuthenticate()) {
                                    biometricAuthManager.authenticate(
                                        context as FragmentActivity,
                                        "ENABLE BIOMETRIC LOCK",
                                        "Confirm identity to secure system nodes",
                                        onSuccess = { viewModel.setBiometricLockEnabled(true) },
                                        onError = { /* Error handling */ }
                                    )
                                }
                            } else {
                                biometricAuthManager.authenticate(
                                    context as FragmentActivity,
                                    "DISABLE BIOMETRIC LOCK",
                                    "Confirm identity to unlock system nodes",
                                    onSuccess = { viewModel.setBiometricLockEnabled(false) },
                                    onError = { /* Error handling */ }
                                )
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally, 
                modifier = Modifier.fillMaxWidth().padding(bottom = 40.dp)
            ) {
                Text(
                    text = "BUILD_HASH: 7F2A91X_STABLE", 
                    color = Color.Gray, 
                    fontSize = 10.sp, 
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            buildHashClickCount++
                            Log.d("SettingsScreen", "Build hash clicked: $buildHashClickCount")
                            if (buildHashClickCount >= 7) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showPasswordDialog = true
                                buildHashClickCount = 0
                            }
                        }
                        .padding(16.dp) // Increase touch target size
                )
                Text("MEET THE DECKERS WHO BUILT THIS", color = Color(0xFF00FF9C).copy(alpha = 0.5f), fontSize = 10.sp, modifier = Modifier.clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                })
                Text("REPORT BUG", color = Color(0xFF00FF9C).copy(alpha = 0.5f), fontSize = 10.sp, modifier = Modifier.clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                })
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "JACK OUT", 
                    color = Color(0xFFFF006E), 
                    fontWeight = FontWeight.Black, 
                    letterSpacing = 4.sp,
                    modifier = Modifier.clickable { 
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                )
            }
        }

        if (showResetDialog) {
            CyberConfirmDialog(
                title = "RESET NEURAL PROFILE?",
                description = "This will wipe all progress. Enter 'JACKIN' to proceed.",
                confirmText = "JACKIN",
                onConfirm = { 
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.resetProfile { 
                        showResetDialog = false
                        onResetComplete()
                    }
                },
                onDismiss = { showResetDialog = false }
            )
        }

        if (showDeleteDialog) {
            CyberConfirmDialog(
                title = "TERMINATE ACCOUNT?",
                description = "Permanent data deletion. 30-day grace period applies.",
                confirmText = "TERMINATE",
                onConfirm = { 
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    showDeleteDialog = false 
                },
                onDismiss = { showDeleteDialog = false }
            )
        }

        if (showPinDialog) {
            CyberPinDialog(
                onConfirm = { 
                    showPinDialog = false
                    pinAction?.invoke()
                },
                onDismiss = { showPinDialog = false }
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
fun CyberPinDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .clip(CyberButtonShape)
                .background(Color(0xFF0F0F0F))
                .border(2.dp, Color(0xFF00FF9C), CyberButtonShape)
                .padding(24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("ENTER_PIN_CODE", color = Color(0xFF00FF9C), fontWeight = FontWeight.Black, fontSize = 20.sp)
                Spacer(Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 4) pin = it },
                    placeholder = { Text("####", color = Color.Gray) },
                    modifier = Modifier.width(120.dp),
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00FF9C),
                        unfocusedBorderColor = Color.DarkGray,
                        focusedTextColor = Color.White
                    )
                )
                
                Spacer(Modifier.height(24.dp))
                
                Button(
                    onClick = onConfirm,
                    enabled = pin.length == 4, // Assuming a 4-digit PIN for now
                    modifier = Modifier.fillMaxWidth().height(50.dp).clip(CyberButtonShape),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF9C))
                ) {
                    Text("VALIDATE", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ToggleSetting(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(), 
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.White, fontSize = 14.sp)
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
fun CyberConfirmDialog(
    title: String,
    description: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    var input by remember { mutableStateOf("") }
    var countdown by remember { mutableStateOf(3) }
    
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
                    onClick = onConfirm,
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

@Composable
fun SettingsItem(label: String, color: Color = Color(0xFF00FF9C), onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp)
    ) {
        Text(label, color = color, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, fontSize = 14.sp)
    }
}

@Composable
fun DeviceStatusCard(name: String, signal: String, lastSync: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0F0F0F))
            .border(1.dp, Color(0xFF00FF9C).copy(alpha = 0.2f))
            .padding(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(name, color = Color.White, fontWeight = FontWeight.Bold)
                Text("SIGNAL: $signal", color = Color(0xFF00FF9C), fontSize = 10.sp)
                Text(lastSync, color = Color.Gray, fontSize = 10.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("CONNECTED", color = Color(0xFF00FF9C), fontSize = 10.sp)
                Text("RE-SYNC", color = Color(0xFF00FF9C), fontSize = 10.sp, modifier = Modifier.clickable { }.padding(top = 4.dp))
            }
        }
    }
}
