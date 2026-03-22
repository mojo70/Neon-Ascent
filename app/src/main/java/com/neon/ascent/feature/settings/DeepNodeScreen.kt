package com.neon.ascent.feature.settings

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neon.ascent.feature.charactercreation.CyberFrame
import com.neon.ascent.feature.charactercreation.CyberGridBackground
import com.neon.ascent.feature.charactercreation.GlitchOverlay

@Composable
fun DeepNodeScreen(onBack: () -> Unit) {
    var currentSubScreen by remember { mutableStateOf("ROOT") }

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
                            // More stub text
                            Spacer(Modifier.height(100.dp))
                            Text("[SYSTEM_END_OF_PAGE]", color = Color.DarkGray, fontSize = 10.sp)
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
