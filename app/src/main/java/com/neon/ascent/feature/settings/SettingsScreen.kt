package com.neon.ascent.feature.settings

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.neon.ascent.feature.charactercreation.CyberButtonShape
import com.neon.ascent.feature.charactercreation.CyberFrame
import com.neon.ascent.feature.charactercreation.CyberGridBackground
import com.neon.ascent.feature.charactercreation.GlitchOverlay
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
    onResetComplete: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val biometricLockEnabled by viewModel.isBiometricLockEnabled.collectAsState()
    val biometricAuthManager = remember { BiometricAuthManager(context) }
    
    var showResetDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    
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
                        if (biometricLockEnabled) {
                            biometricAuthManager.authenticate(
                                context as FragmentActivity,
                                "RESET PROFILE",
                                "Confirm biometric signature to wipe profile",
                                onSuccess = { showResetDialog = true },
                                onError = { /* Show error */ }
                            )
                        } else {
                            showResetDialog = true
                        }
                    })
                    SettingsItem("EXPORT_CHARACTER [.JSON]", onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (biometricLockEnabled) {
                            biometricAuthManager.authenticate(
                                context as FragmentActivity,
                                "EXPORT DATA",
                                "Confirm biometric signature to download character data",
                                onSuccess = { /* Proceed with export */ },
                                onError = { /* Show error */ }
                            )
                        } else {
                            // Proceed with export
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
                    Button(
                        onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clip(CyberButtonShape)
                            .border(1.dp, Color(0xFF00FF9C), CyberButtonShape),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1A1A))
                    ) {
                        Text("+ JACK IN DEVICE", color = Color(0xFF00FF9C), fontWeight = FontWeight.Black)
                    }
                    
                    DeviceStatusCard("NEURAL_LINK_V4", "98%", "LAST_SYNC: 2m ago")
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = true, 
                            onCheckedChange = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) },
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
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            CyberFrame(label = "SECURITY & LOGS") {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SettingsItem("DATA EXPORT [.LOG]", onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
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
                Text("BUILD_HASH: 7F2A91X_STABLE", color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
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
