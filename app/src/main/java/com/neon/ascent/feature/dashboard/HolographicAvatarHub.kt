package com.neon.ascent.feature.dashboard

import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun HolographicAvatarHub(
    viewModel: DashboardViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onUpgradeClick: (String) -> Unit,
    onHacksClick: () -> Unit,
    onAttributeScanClick: () -> Unit,
    onStoryClick: () -> Unit,
    onGoalSettingClick: () -> Unit
) {
    val userCharacter by viewModel.userCharacter.collectAsState()
    val healthState by viewModel.healthState.collectAsState()
    var selectedBodyPart by remember { mutableStateOf<String?>(null) }
    var isEditingName by remember { mutableStateOf(false) }
    var editedName by remember { mutableStateOf("") }

    val neuralLoad = userCharacter?.neuralLoad ?: 0.2f

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
        GlitchOverlay(intensity = neuralLoad)

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
                        .weight(1.2f)
                        .fillMaxHeight()
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // Energy Bar (Body Battery)
                        EnergyBar(label = "NEURAL_ENERGY", value = healthState.bodyBattery / 100f)
                        
                        Spacer(Modifier.height(8.dp))
                        
                        Box(contentAlignment = Alignment.Center) {
                            HologramDisplay(userCharacter) { part ->
                                selectedBodyPart = part
                                systemLogs.add(0, "[LOG] SECTOR_ACCESS: $part")
                            }
                        }
                    }
                }

                // Right Column: HUD & Logs
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // HUD Info
                    CyberFrame(label = "BIOMETRIC_HUD") {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selectedBodyPart ?: "TOTAL_SYNC",
                                    color = Color(0xFFFF006E),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                
                                if (healthState.isConnected) {
                                    HeartbeatLine(bpm = healthState.heartRate)
                                }
                            }
                            
                            Spacer(Modifier.height(8.dp))
                            
                            AttributeRadarChart(
                                stats = mapOf(
                                    "STR" to (userCharacter?.strength ?: 0),
                                    "AGI" to (userCharacter?.agility ?: 0),
                                    "END" to (userCharacter?.endurance ?: 0),
                                    "PER" to (userCharacter?.perception ?: 0),
                                    "CHA" to (userCharacter?.charisma ?: 0),
                                    "LUC" to (userCharacter?.luck ?: 0)
                                ),
                                modifier = Modifier.size(160.dp)
                            )

                            Spacer(Modifier.height(12.dp))
                            
                            // Health Metrics Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                MetricSmall(label = "VO2_MAX", value = String.format("%.1f", healthState.vo2Max), unit = "mL/kg/min")
                                MetricSmall(label = "STRESS", value = healthState.stressLevel.toString(), unit = "LVL")
                            }

                            Spacer(Modifier.height(12.dp))
                            
                            val stats = when(selectedBodyPart) {
                                "HEAD" -> {
                                    val rank = userCharacter?.getChessRank() ?: "GHOST_IN_SHELL"
                                    val title = userCharacter?.getChessTitle() ?: "UNRANKED"
                                    "PERCEPTION: ${userCharacter?.perception ?: "??"}\nRANK: $rank [$title]\nELO: ${userCharacter?.chessElo ?: 1000}\nNEURAL_SYNC: 98%"
                                }
                                "TORSO" -> "ENDURANCE: ${userCharacter?.endurance ?: "??"}\nHEART_RATE: ${healthState.heartRate} BPM\nRESP_RATE: 14"
                                "ARMS" -> "STRENGTH: ${userCharacter?.strength ?: "??"}\nLOAD_CAP: 85%\nGRIP_PSI: 120"
                                "LEGS" -> "AGILITY: ${userCharacter?.agility ?: "??"}\nREFLEX: ACTIVATED\nGAIT_SYNC: OK"
                                else -> "SYSTEM_WIDE BIOMETRICS DETECTED\nALL_NODES: OPERATIONAL"
                            }
                            Text(
                                stats, 
                                color = Color.White, 
                                fontSize = 10.sp, 
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.align(Alignment.Start)
                            )
                            
                            if (selectedBodyPart != null) {
                                Spacer(Modifier.height(12.dp))
                                Button(
                                    onClick = { onUpgradeClick(selectedBodyPart!!) },
                                    modifier = Modifier.fillMaxWidth().height(32.dp).clip(CyberButtonShape),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF9C)),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("UPGRADE", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Primary Navigation Node
                    CyberFrame(label = "SYSTEM_CONTROLS") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            DashboardButtonSmall("ATTRIBUTE SCAN", Color(0xFF00FF9C), onAttributeScanClick)
                            DashboardButtonSmall("YOUR STORY", Color(0xFFFF006E), onStoryClick)
                            DashboardButtonSmall("GOAL SETTING", Color.White, onGoalSettingClick)
                            DashboardButtonSmall("BIOHACKS", Color(0xFF00FFFF), onHacksClick)
                        }
                    }

                    // System Logs
                    CyberFrame(label = "SYSTEM_LOGS") {
                        LazyColumn(modifier = Modifier.height(120.dp)) {
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
fun EnergyBar(label: String, value: Float) {
    Column(modifier = Modifier.width(180.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = Color(0xFF00FFFF), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text("${(value * 100).toInt()}%", color = Color(0xFF00FFFF), fontSize = 10.sp)
        }
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(Color.Black)
                .border(1.dp, Color(0xFF00FFFF).copy(alpha = 0.3f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(value)
                    .fillMaxHeight()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xFF00FFFF).copy(alpha = 0.5f), Color(0xFF00FFFF))
                        )
                    )
            )
        }
    }
}

@Composable
fun MetricSmall(label: String, value: String, unit: String) {
    Column {
        Text(label, color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.width(2.dp))
            Text(unit, color = Color(0xFF00FF9C), fontSize = 8.sp)
        }
    }
}

@Composable
fun HeartbeatLine(bpm: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "Heartbeat")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 60000 / bpm.coerceAtLeast(1), easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Phase"
    )

    Canvas(modifier = Modifier.size(60.dp, 20.dp)) {
        val path = Path()
        val width = size.width
        val height = size.height
        val centerY = height / 2

        path.moveTo(0f, centerY)
        path.lineTo(width * 0.3f, centerY)
        path.lineTo(width * 0.4f, centerY - height * 0.4f)
        path.lineTo(width * 0.5f, centerY + height * 0.4f)
        path.lineTo(width * 0.6f, centerY)
        path.lineTo(width, centerY)

        drawPath(
            path = path,
            color = Color(0xFFFF006E).copy(alpha = 0.2f),
            style = Stroke(width = 2.dp.toPx())
        )

        // The "moving" pulse
        val progress = phase
        val drawPath = Path()
        drawPath.moveTo(0f, centerY)
        if (progress > 0.3f) drawPath.lineTo(width * 0.3f, centerY)
        if (progress > 0.4f) drawPath.lineTo(width * 0.4f, centerY - height * 0.4f)
        if (progress > 0.5f) drawPath.lineTo(width * 0.5f, centerY + height * 0.4f)
        if (progress > 0.6f) drawPath.lineTo(width * 0.6f, centerY)
        if (progress > 0.9f) drawPath.lineTo(width, centerY)

        drawPath(
            path = drawPath,
            color = Color(0xFFFF006E),
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

@Composable
fun DashboardButtonSmall(label: String, color: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .clip(CyberButtonShape)
            .border(1.dp, color, CyberButtonShape),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F0F0F)),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(label, color = color, fontSize = 10.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
fun AttributeRadarChart(
    stats: Map<String, Int>,
    modifier: Modifier = Modifier,
    maxValue: Int = 10
) {
    val labels = listOf("STR", "AGI", "END", "PER", "CHA", "LUC")
    val values = listOf(
        stats["STR"] ?: 0,
        stats["AGI"] ?: 0,
        stats["END"] ?: 0,
        stats["PER"] ?: 0,
        stats["CHA"] ?: 0,
        stats["LUC"] ?: 0
    )

    Canvas(modifier = modifier) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val radius = size.minDimension / 2.5f

        // Draw background hexagons
        for (i in 1..5) {
            val currentRadius = radius * (i / 5f)
            val path = Path()
            for (j in 0 until 6) {
                val angle = (j * 60f - 90f) * (Math.PI / 180f).toFloat()
                val x = centerX + currentRadius * cos(angle)
                val y = centerY + currentRadius * sin(angle)
                if (j == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            drawPath(
                path = path,
                color = Color(0xFF00FF9C).copy(alpha = 0.1f),
                style = Stroke(width = 1.dp.toPx())
            )
        }

        // Draw axes
        for (i in 0 until 6) {
            val angle = (i * 60f - 90f) * (Math.PI / 180f).toFloat()
            val x = centerX + radius * cos(angle)
            val y = centerY + radius * sin(angle)
            drawLine(
                color = Color(0xFF00FF9C).copy(alpha = 0.1f),
                start = Offset(centerX, centerY),
                end = Offset(x, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        // Draw data polygon
        val dataPath = Path()
        for (i in 0 until 6) {
            val angle = (i * 60f - 90f) * (Math.PI / 180f).toFloat()
            val value = values[i].coerceIn(0, maxValue)
            val currentRadius = radius * (value.toFloat() / maxValue)
            val x = centerX + currentRadius * cos(angle)
            val y = centerY + currentRadius * sin(angle)
            if (i == 0) dataPath.moveTo(x, y) else dataPath.lineTo(x, y)
        }
        dataPath.close()
        drawPath(
            path = dataPath,
            color = Color(0xFF00FF9C).copy(alpha = 0.3f)
        )
        drawPath(
            path = dataPath,
            color = Color(0xFF00FF9C),
            style = Stroke(width = 2.dp.toPx())
        )
        
        // Draw labels
        val textPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 8.sp.toPx()
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.MONOSPACE
        }
        
        for (i in 0 until 6) {
            val angle = (i * 60f - 90f) * (Math.PI / 180f).toFloat()
            val x = centerX + (radius + 12.dp.toPx()) * cos(angle)
            val y = centerY + (radius + 12.dp.toPx()) * sin(angle)
            drawContext.canvas.nativeCanvas.drawText(
                labels[i],
                x,
                y + 4.dp.toPx(),
                textPaint
            )
        }
    }
}

@Composable
fun HologramDisplay(character: UserCharacter?, onPartClick: (String) -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "HologramAnim")
    
    // Scanning line animation
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
            // Load Bitmap or Pixelated Fallback
            val context = LocalContext.current
            val avatarBitmap = remember(character?.avatarPath) {
                if (character?.avatarPath != null && character.avatarPath != "internal_storage_placeholder") {
                    try {
                        BitmapFactory.decodeFile(character.avatarPath)
                    } catch (e: Exception) { null }
                } else { null }
            }

            if (avatarBitmap != null) {
                Image(
                    bitmap = avatarBitmap.asImageBitmap(),
                    contentDescription = "Avatar",
                    modifier = Modifier.fillMaxSize().alpha(0.7f),
                    contentScale = ContentScale.Fit
                )
            } else {
                PixelatedSilhouette(modifier = Modifier.fillMaxSize(0.7f))
            }
            
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

@Composable
fun PixelatedSilhouette(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val color = Color(0xFF00FF9C).copy(alpha = 0.3f)
        val pixelSize = 10.dp.toPx()
        
        // Simple pixelated head
        drawRect(color, Offset(center.x - pixelSize*2, pixelSize*2), Size(pixelSize*4, pixelSize*4))
        // Neck
        drawRect(color, Offset(center.x - pixelSize, pixelSize*6), Size(pixelSize*2, pixelSize*2))
        // Torso
        drawRect(color, Offset(center.x - pixelSize*4, pixelSize*8), Size(pixelSize*8, pixelSize*12))
        // Arms
        drawRect(color, Offset(center.x - pixelSize*6, pixelSize*8), Size(pixelSize*2, pixelSize*10))
        drawRect(color, Offset(center.x + pixelSize*4, pixelSize*8), Size(pixelSize*2, pixelSize*10))
        // Legs
        drawRect(color, Offset(center.x - pixelSize*3, pixelSize*20), Size(pixelSize*2, pixelSize*12))
        drawRect(color, Offset(center.x + pixelSize, pixelSize*20), Size(pixelSize*2, pixelSize*12))
    }
}
