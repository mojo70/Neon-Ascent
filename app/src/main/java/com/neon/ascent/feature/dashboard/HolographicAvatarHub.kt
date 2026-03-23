package com.neon.ascent.feature.dashboard

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neon.ascent.feature.charactercreation.CyberButtonShape
import com.neon.ascent.feature.charactercreation.CyberFrame
import com.neon.ascent.feature.charactercreation.CyberGridBackground
import com.neon.ascent.feature.charactercreation.GlitchOverlay
import com.neon.ascent.model.UserCharacter

@Composable
fun HolographicAvatarHub(
    viewModel: DashboardViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val userCharacter by viewModel.userCharacter.collectAsState()
    var selectedBodyPart by remember { mutableStateOf<String?>(null) }
    var isEditingName by remember { mutableStateOf(false) }
    var editedName by remember { mutableStateOf("") }

    val systemLogs = remember {
        mutableStateListOf(
            "INITIALIZING NEURAL_LINK...",
            "AVATAR_HOLOGRAPH_STABLE",
            "BIOMETRIC_DATA_LOADED"
        )
    }

    LaunchedEffect(userCharacter?.netrunnerName) {
        editedName = userCharacter?.netrunnerName ?: "RUNNER_UNKNOWN"
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF020202))) {
        CyberGridBackground()
        GlitchOverlay()

        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            // Header with AI Name
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF00FF9C))
                }
                
                if (isEditingName) {
                    OutlinedTextField(
                        value = editedName,
                        onValueChange = { editedName = it },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00FF9C),
                            unfocusedBorderColor = Color(0xFF00FF9C).copy(alpha = 0.5f),
                            focusedTextColor = Color.White
                        ),
                        trailingIcon = {
                            TextButton(onClick = { 
                                viewModel.updateNetrunnerName(editedName)
                                isEditingName = false 
                            }) {
                                Text("SYNC", color = Color(0xFF00FF9C))
                            }
                        }
                    )
                } else {
                    Text(
                        text = "//${userCharacter?.netrunnerName ?: "GENERATING..."}",
                        modifier = Modifier.clickable { isEditingName = true }.weight(1f),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            color = Color(0xFF00FF9C),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxSize()) {
                // Left Column: Avatar Hologram
                Box(
                    modifier = Modifier
                        .weight(1.5f)
                        .fillMaxHeight()
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    HologramDisplay(userCharacter) { part ->
                        selectedBodyPart = part
                        systemLogs.add(0, "[LOG] SECTOR_ACCESS: $part")
                    }
                }

                // Right Column: HUD & Logs
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CyberFrame(label = "BIOMETRIC_HUD") {
                        Column {
                            Text(
                                text = selectedBodyPart ?: "SELECT_SECTOR",
                                color = Color(0xFFFF006E),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(Modifier.height(8.dp))
                            val stats = when(selectedBodyPart) {
                                "HEAD" -> "PERCEPTION: ${userCharacter?.perception ?: "??"}\nFOCUS: STABLE"
                                "TORSO" -> "ENDURANCE: ${userCharacter?.endurance ?: "??"}\nHEART_RATE: 72 BPM"
                                "ARMS" -> "STRENGTH: ${userCharacter?.strength ?: "??"}\nLOAD_CAP: 85%"
                                "LEGS" -> "AGILITY: ${userCharacter?.agility ?: "??"}\nREFLEX: ACTIVATED"
                                else -> "AWAITING_INPUT..."
                            }
                            Text(stats, color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        }
                    }

                    CyberFrame(label = "PERKS_AND_EFFECTS") {
                        Column {
                            if (userCharacter?.holyGhost != null) {
                                Text("HOLY_GHOST [LVL ${userCharacter?.holyGhost}]", color = Color(0xFF00FF9C), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("Aura: Active", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                            } else {
                                Text("NO_ACTIVE_PERKS", color = Color.Gray, fontSize = 12.sp)
                            }
                        }
                    }

                    CyberFrame(label = "SYSTEM_LOGS") {
                        LazyColumn(modifier = Modifier.height(200.dp)) {
                            items(systemLogs) { log ->
                                Text(
                                    text = log,
                                    color = Color(0xFF00FF9C).copy(alpha = 0.7f),
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HologramDisplay(character: UserCharacter?, onPartClick: (String) -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "HologramAnim")
    
    val scanY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ScanLine"
    )

    val flickerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(100, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Flicker"
    )

    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        if (character?.holyGhost != null) {
            HolyGhostAura()
        }

        Canvas(modifier = Modifier
            .fillMaxSize()
            .graphicsLayer(alpha = flickerAlpha)
        ) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF00FF9C).copy(alpha = 0.1f), Color.Transparent),
                    center = center,
                    radius = size.minDimension / 1.5f
                )
            )

            val y = size.height * scanY
            drawLine(
                color = Color(0xFF00FF9C).copy(alpha = 0.5f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 2.dp.toPx()
            )
        }

        Box(
            modifier = Modifier
                .fillMaxHeight(0.8f)
                .fillMaxWidth()
                .border(1.dp, Color(0xFF00FF9C).copy(alpha = 0.2f), CyberButtonShape),
            contentAlignment = Alignment.Center
        ) {
            Text("FULL_BODY_HOLOGRAPH", color = Color(0xFF00FF9C).copy(alpha = 0.5f))
            
            Column(modifier = Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f).fillMaxWidth().clickable { onPartClick("HEAD") })
                Box(Modifier.weight(2f).fillMaxWidth().clickable { onPartClick("TORSO") })
                Row(Modifier.weight(1f).fillMaxWidth()) {
                    Box(Modifier.weight(1f).fillMaxHeight().clickable { onPartClick("ARMS") })
                    Box(Modifier.weight(1f).fillMaxHeight().clickable { onPartClick("ARMS") })
                }
                Box(Modifier.weight(2f).fillMaxWidth().clickable { onPartClick("LEGS") })
            }
        }
    }
}
