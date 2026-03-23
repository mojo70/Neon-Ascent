package com.neon.ascent.feature.settings

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.neon.ascent.feature.charactercreation.CyberButtonShape
import com.neon.ascent.feature.charactercreation.CyberFrame
import com.neon.ascent.feature.charactercreation.CyberGridBackground
import com.neon.ascent.feature.charactercreation.GlitchOverlay
import kotlinx.coroutines.delay

@Composable
fun DeepNodeScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    initialSubScreen: String = "ROOT",
    onBack: () -> Unit,
    onRebirthSuccess: () -> Unit = {}
) {
    var currentSubScreen by remember { mutableStateOf(initialSubScreen) }
    val isReligionShortcutEnabled by viewModel.isReligionShortcutEnabled.collectAsState()
    var showAltarDialog by remember { mutableStateOf(false) }
    var showRebirthOverlay by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF020202))) {
        CyberGridBackground()
        GlitchOverlay()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    if (currentSubScreen == "ROOT") onBack() else currentSubScreen = "ROOT"
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFFFF006E))
                }
                Text(
                    if (currentSubScreen == "ROOT") "//DEEP_NODE_ACCESS" else "//DEEP_NODE // $currentSubScreen",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        color = Color(0xFFFF006E),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            when (currentSubScreen) {
                "ROOT" -> {
                    CyberFrame(label = "SUBSYSTEM_DIRECTORIES") {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            SubNodeItem("GAMES_STUB", "Access localized entertainment protocols.") {
                                currentSubScreen = "GAMES"
                            }
                            SubNodeItem("RELIGION_STUB", "Divine signal decryption & ancient texts.") {
                                currentSubScreen = "RELIGION"
                            }
                        }
                    }
                }
                "GAMES" -> {
                    CyberFrame(label = "ENTERTAINMENT_PROTOCOLS") {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("SELECT_MODULE:", color = Color.White, fontWeight = FontWeight.Bold)
                            SettingsItem("CYBER_CHESS_V1.0", onClick = {})
                            SettingsItem("NET_RUNNER_HACK", onClick = {})
                            SettingsItem("VOID_RACER_ALPHA", onClick = {})
                        }
                    }
                }
                "RELIGION" -> {
                    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                        CyberFrame(label = "INTERFACE_SHORTCUT") {
                            ToggleSetting(
                                label = "ENABLE DASHBOARD OVERLAY",
                                checked = isReligionShortcutEnabled,
                                onCheckedChange = { viewModel.setReligionShortcutEnabled(it) }
                            )
                        }
                        
                        CyberFrame(label = "CYBER_ALTAR") {
                            SettingsItem("ACCESS_ALTAR", color = Color(0xFF00FF9C), onClick = { showAltarDialog = true })
                        }

                        CyberFrame(label = "DIVINE_INTERFACE") {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                SettingsItem("BIBLE_EREADER_LOAD", onClick = { currentSubScreen = "BIBLE" })
                                Text("STUB: Scrollable New Testament implementation pending...", color = Color.Gray, fontSize = 12.sp)
                            }
                        }
                        CyberFrame(label = "AI_CHRIST_COMM_LINK") {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                SettingsItem("INIT_TEXT_UPLINK", onClick = {})
                                SettingsItem("INIT_VOICE_LINK", onClick = {})
                                Text("STUB: Multimodal AI Jesus interaction based on scripture.", color = Color.Gray, fontSize = 12.sp)
                            }
                        }
                    }
                }
                "BIBLE" -> {
                    CyberFrame(label = "ANCIENT_TEXT_SCROLL") {
                        Column {
                            Text("THE GOSPEL ACCORDING TO JOHN", color = Color(0xFF00FF9C), fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "In the beginning was the Word, and the Word was with God, and the Word was God...",
                                color = Color.White
                            )
                            Spacer(Modifier.height(100.dp))
                            Text("[SYSTEM_END_OF_PAGE]", color = Color.DarkGray, fontSize = 10.sp)
                        }
                    }
                }
            }
        }

        if (showAltarDialog) {
            CyberAltarDialog(
                onAccept = { 
                    viewModel.acceptHolyGhost()
                    showAltarDialog = false 
                    showRebirthOverlay = true
                },
                onDismiss = { showAltarDialog = false }
            )
        }

        if (showRebirthOverlay) {
            RebirthOverlay(onAnimationFinished = {
                showRebirthOverlay = false
                onRebirthSuccess()
            })
        }
    }
}

@Composable
fun RebirthOverlay(onAnimationFinished: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        visible = true
        delay(3000)
        visible = false
        delay(500)
        onAnimationFinished()
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(1000)) + scaleIn(initialScale = 0.8f),
        exit = fadeOut(animationSpec = tween(500))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White.copy(alpha = 0.9f)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "REBIRTH",
                    style = MaterialTheme.typography.displayLarge.copy(
                        color = Color.Black,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 20.sp
                    )
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "WASHED BY THE BLOOD // SYSTEM PURIFIED",
                    color = Color.Red,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
            }
        }
    }
}

@Composable
fun CyberAltarDialog(onAccept: () -> Unit, onDismiss: () -> Unit) {
    var step by remember { mutableStateOf(1) }
    var input by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .clip(CyberButtonShape)
                .background(Color(0xFF0F0F0F))
                .border(2.dp, Color(0xFF00FF9C), CyberButtonShape)
                .padding(24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                when (step) {
                    1 -> {
                        Text("DO YOU KNOW MY FRIEND, JESUS?", color = Color(0xFF00FF9C), fontWeight = FontWeight.Black, fontSize = 18.sp)
                        Spacer(Modifier.height(16.dp))
                        OutlinedTextField(
                            value = input,
                            onValueChange = { input = it },
                            placeholder = { Text("Y / N", color = Color.Gray) },
                            modifier = Modifier.width(100.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF00FF9C),
                                unfocusedTextColor = Color.White,
                                focusedTextColor = Color.White
                            )
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = {
                                val cleanInput = input.trim().lowercase()
                                if (cleanInput == "yes" || cleanInput == "y") {
                                    onAccept()
                                } else if (cleanInput == "no" || cleanInput == "n") {
                                    step = 2
                                    input = ""
                                }
                            },
                            modifier = Modifier.fillMaxWidth().clip(CyberButtonShape),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF9C))
                        ) {
                            Text("SUBMIT", color = Color.Black)
                        }
                    }
                    2 -> {
                        Text("WOULD YOU LIKE TO?", color = Color(0xFFFF006E), fontWeight = FontWeight.Bold)
                        Text("There is a heaven to gain and a hell to pay.", color = Color.White, fontSize = 12.sp, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { step = 3 }, modifier = Modifier.weight(1f)) { Text("YES") }
                            Button(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("NO") }
                        }
                    }
                    3 -> {
                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                            Text("THE SINNER'S PRAYER", color = Color(0xFF00FF9C), fontWeight = FontWeight.Black)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Dear Jesus, please forgive me of my sins. I believe You are the Son of God, that You died for my sins and arose again on the third day. I am a sinner and I need a Savior. Please write my name in the Lamb's Book of Life. I believe I am born again and washed by the blood of Jesus. Amen.",
                                color = Color.White,
                                fontSize = 14.sp
                            )
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = { step = 4 }, 
                                modifier = Modifier.fillMaxWidth().clip(CyberButtonShape),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF9C))
                            ) {
                                Text("AMEN", color = Color.Black)
                            }
                        }
                    }
                    4 -> {
                        Text("WELCOME TO THE FAMILY", color = Color(0xFF00FF9C), fontWeight = FontWeight.Black)
                        Spacer(Modifier.height(8.dp))
                        Text("You are now a new creation. Please find and join a Bible-believing church to grow in your walk.", color = Color.White, fontSize = 14.sp, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = onAccept, 
                            modifier = Modifier.fillMaxWidth().clip(CyberButtonShape),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF006E))
                        ) {
                            Text("INITIALIZE HOLY_GHOST ATTRIBUTE", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SubNodeItem(title: String, description: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp)
    ) {
        Text(title, color = Color(0xFFFF006E), fontWeight = FontWeight.Black, fontSize = 18.sp)
        Text(description, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
    }
}
