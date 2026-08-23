package com.neon.ascent.feature.biohacking.ui.hullpulse

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neon.ascent.core.common.NeonCyan
import com.neon.ascent.core.common.NeonGreen
import com.neon.ascent.core.common.NeonPink
import com.neon.ascent.core.common.Scanlines
import com.neon.ascent.core.common.Vignette
import com.neon.ascent.core.common.neonBorder
import com.neon.ascent.ui.CyberActionButton
import com.neon.ascent.ui.CyberCutShape
import com.neon.ascent.ui.GlitchText
import com.neon.ascent.ui.HudCornerAccents
import com.neon.ascent.ui.SoftGridBackground

private enum class HullIntensity(val label: String) {
    LOW("LOW"),
    MID("MID"),
    HIGH("HIGH"),
    OVERCLOCK("OVERCLOCK")
}

private enum class HullProtocol(val label: String, val iconType: String) {
    FLICK("FLICK", "LIGHTNING"),
    HOLD("HOLD", "TARGET"),
    LADDER("LADDER", "STAIRS"),
    REVERSE("REVERSE", "SYNC"),
    SEED("SEED", "BURST")
}

@Composable
fun HullPulseScreen(
    onBack: () -> Unit,
    viewModel: HullPulseViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    var selectedIntensity by remember { mutableStateOf(HullIntensity.MID) }
    var selectedProtocol by remember { mutableStateOf(HullProtocol.HOLD) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF010403))
    ) {
        // Multi-layered Cyber Background
        SoftGridBackground()
        Vignette()
        Scanlines(intensity = 0.1f)

        // HUD Corner Accents
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            HudCornerAccents(color = NeonGreen.copy(alpha = 0.35f))
        }

        // Main Scrollable Area
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Status Bar: Version & Sub-node + XP / Shard Chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "// HULL_PULSE v0.8.4",
                        color = NeonCyan.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "LOBBY // LIFE-RPG NODE",
                        color = NeonCyan.copy(alpha = 0.5f),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    )
                }

                // XP / Core Chip
                Box(
                    modifier = Modifier
                        .clip(CyberCutShape)
                        .background(Color(0xFF04120C).copy(alpha = 0.8f))
                        .neonBorder(NeonGreen.copy(alpha = 0.6f), width = 1.dp, cornerRadius = 0.dp)
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ChipIcon(color = NeonGreen)
                        Spacer(Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "+30 XP",
                                color = NeonGreen,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "/ CORE",
                                color = NeonGreen.copy(alpha = 0.7f),
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Title
            GlitchText(
                text = "HULL PULSE",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black
            )

            Spacer(Modifier.height(6.dp))

            // Status Bar Ticker
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "FLOOR: ",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "ONLINE",
                    color = NeonGreen,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "  //  SEED: ",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = selectedProtocol.name,
                    color = NeonCyan,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "  //  LOAD: ",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = selectedIntensity.name,
                    color = if (selectedIntensity == HullIntensity.OVERCLOCK) NeonPink else NeonGreen,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black
                )
            }

            Spacer(Modifier.height(16.dp))

            // ==================== LOBBY MODE ====================
            AnimatedVisibility(
                visible = !state.isRunning,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Intensity Selection Rail
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HullIntensity.entries.forEach { intensity ->
                            val isSelected = selectedIntensity == intensity
                            val chipAccent = if (intensity == HullIntensity.OVERCLOCK) NeonPink else NeonGreen
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .clip(CyberCutShape)
                                    .background(
                                        if (isSelected) chipAccent else Color(0xFF06100B).copy(alpha = 0.85f)
                                    )
                                    .neonBorder(
                                        color = if (isSelected) chipAccent else chipAccent.copy(alpha = 0.35f),
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        glowIntensity = if (isSelected) 0.8f else 0.15f,
                                        cornerRadius = 0.dp
                                    )
                                    .clickable { selectedIntensity = intensity },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = intensity.label,
                                    color = if (isSelected) Color.Black else Color.White.copy(alpha = 0.85f),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Protocol Grid (Row 1: FLICK, HOLD, LADDER)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ProtocolTile(
                            protocol = HullProtocol.FLICK,
                            isSelected = selectedProtocol == HullProtocol.FLICK,
                            onClick = { selectedProtocol = HullProtocol.FLICK },
                            modifier = Modifier.weight(1f)
                        )
                        ProtocolTile(
                            protocol = HullProtocol.HOLD,
                            isSelected = selectedProtocol == HullProtocol.HOLD,
                            onClick = { selectedProtocol = HullProtocol.HOLD },
                            modifier = Modifier.weight(1f)
                        )
                        ProtocolTile(
                            protocol = HullProtocol.LADDER,
                            isSelected = selectedProtocol == HullProtocol.LADDER,
                            onClick = { selectedProtocol = HullProtocol.LADDER },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    // Protocol Grid (Row 2: REVERSE, SEED)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ProtocolTile(
                            protocol = HullProtocol.REVERSE,
                            isSelected = selectedProtocol == HullProtocol.REVERSE,
                            accentColor = NeonPink,
                            onClick = { selectedProtocol = HullProtocol.REVERSE },
                            modifier = Modifier.weight(1f)
                        )
                        ProtocolTile(
                            protocol = HullProtocol.SEED,
                            isSelected = selectedProtocol == HullProtocol.SEED,
                            onClick = { selectedProtocol = HullProtocol.SEED },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    // Phase Preview Frame
                    PhasePreviewSection()

                    Spacer(Modifier.height(16.dp))

                    // Microcopy
                    Text(
                        text = "CONTRACT THE FLOOR. NOT THE GLUTES.",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = "BREATHE ON RELEASE.",
                        color = NeonCyan,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(24.dp))

                    // Primary Action (Jack Hull Pulse)
                    CyberActionButton(
                        label = "JACK HULL_PULSE",
                        color = NeonPink,
                        onClick = { viewModel.startPulse() }
                    )
                }
            }

            // ==================== SESSION MODE ====================
            AnimatedVisibility(
                visible = state.isRunning,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.height(16.dp))

                    // Giant Phase Core
                    PulseCore(
                        state = state,
                        modifier = Modifier.size(260.dp)
                    )

                    Spacer(Modifier.height(28.dp))

                    // Cyber Cliped Progress Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CyberCutShape)
                            .background(Color(0xFF0A1810))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction = state.totalProgress.coerceIn(0f, 1f))
                                .height(6.dp)
                                .background(NeonGreen)
                                .neonBorder(NeonGreen, width = 1.dp, cornerRadius = 0.dp)
                        )
                    }

                    Spacer(Modifier.height(32.dp))

                    // Session Abort Button
                    CyberActionButton(
                        label = "ABORT HULL",
                        color = NeonPink,
                        onClick = { viewModel.stopPulse() }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }

        // Close Button in top right
        IconButton(
            onClick = {
                viewModel.stopPulse()
                onBack()
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close Hull Pulse",
                tint = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun ProtocolTile(
    protocol: HullProtocol,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    accentColor: Color = NeonCyan,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) accentColor else Color.White.copy(alpha = 0.15f)
    val glowIntensity = if (isSelected) 0.6f else 0.05f

    Box(
        modifier = modifier
            .height(84.dp)
            .clip(CyberCutShape)
            .background(
                if (isSelected) Color(0xFF031418).copy(alpha = 0.85f) else Color(0xFF06090A).copy(alpha = 0.7f)
            )
            .neonBorder(
                color = borderColor,
                width = if (isSelected) 1.5.dp else 1.dp,
                glowIntensity = glowIntensity,
                cornerRadius = 0.dp
            )
            .clickable { onClick() }
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            ProtocolIcon(
                iconType = protocol.iconType,
                color = if (isSelected) (if (protocol == HullProtocol.HOLD) NeonGreen else accentColor) else Color.White.copy(alpha = 0.5f)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = protocol.label,
                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.5f),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
private fun ProtocolIcon(iconType: String, color: Color) {
    Canvas(modifier = Modifier.size(24.dp)) {
        when (iconType) {
            "LIGHTNING" -> {
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(size.width * 0.55f, 0f)
                    lineTo(size.width * 0.2f, size.height * 0.55f)
                    lineTo(size.width * 0.5f, size.height * 0.55f)
                    lineTo(size.width * 0.45f, size.height)
                    lineTo(size.width * 0.8f, size.height * 0.45f)
                    lineTo(size.width * 0.5f, size.height * 0.45f)
                    close()
                }
                drawPath(path, color = color)
            }
            "TARGET" -> {
                drawCircle(color, radius = size.width * 0.4f, style = Stroke(width = 2.dp.toPx()))
                drawCircle(color, radius = size.width * 0.15f, style = Stroke(width = 2.dp.toPx()))
                drawLine(color, Offset(size.width / 2, 0f), Offset(size.width / 2, size.height), 1.5.dp.toPx())
                drawLine(color, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), 1.5.dp.toPx())
            }
            "STAIRS" -> {
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(size.width * 0.15f, size.height * 0.85f)
                    lineTo(size.width * 0.4f, size.height * 0.85f)
                    lineTo(size.width * 0.4f, size.height * 0.55f)
                    lineTo(size.width * 0.65f, size.height * 0.55f)
                    lineTo(size.width * 0.65f, size.height * 0.25f)
                    lineTo(size.width * 0.9f, size.height * 0.25f)
                }
                drawPath(path, color = color, style = Stroke(width = 2.dp.toPx()))
            }
            "SYNC" -> {
                drawArc(
                    color = color,
                    startAngle = 45f,
                    sweepAngle = 270f,
                    useCenter = false,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            "BURST" -> {
                val center = Offset(size.width / 2, size.height / 2)
                drawCircle(color, radius = 2.5.dp.toPx(), center = center)
                for (i in 0..5) {
                    val angle = (i * 60f) * (Math.PI / 180f)
                    val r = size.width * 0.38f
                    val px = center.x + (r * Math.cos(angle)).toFloat()
                    val py = center.y + (r * Math.sin(angle)).toFloat()
                    drawCircle(color, radius = 2.dp.toPx(), center = Offset(px, py))
                }
            }
        }
    }
}

@Composable
private fun PhasePreviewSection() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "// PHASE PREVIEW",
            color = NeonPink,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(CyberCutShape)
                .background(Color(0xFF040809).copy(alpha = 0.85f))
                .neonBorder(NeonCyan.copy(alpha = 0.4f), width = 1.dp, cornerRadius = 0.dp)
                .padding(vertical = 12.dp, horizontal = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PhasePreviewItem(color = NeonGreen, label = "HOLD", duration = "6s")
                Text(">", color = NeonCyan.copy(alpha = 0.5f), fontSize = 14.sp, fontFamily = FontFamily.Monospace)
                PhasePreviewItem(color = NeonCyan, label = "REL", duration = "4s")
                Text(">", color = NeonCyan.copy(alpha = 0.5f), fontSize = 14.sp, fontFamily = FontFamily.Monospace)
                PhasePreviewItem(color = NeonPink, label = "FLICK", duration = "x8")
                Text(">", color = NeonCyan.copy(alpha = 0.5f), fontSize = 14.sp, fontFamily = FontFamily.Monospace)
                PhasePreviewItem(color = NeonGreen, label = "HOLD", duration = "8s")
                Text(">", color = NeonCyan.copy(alpha = 0.5f), fontSize = 14.sp, fontFamily = FontFamily.Monospace)
                PhasePreviewItem(color = NeonCyan, label = "DROP", duration = "5s")
            }
        }
    }
}

@Composable
private fun PhasePreviewItem(color: Color, label: String, duration: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Grid dot matrix icon
        Canvas(modifier = Modifier.size(20.dp)) {
            val step = size.width / 3f
            for (r in 0..2) {
                for (c in 0..2) {
                    drawRect(
                        color = color,
                        topLeft = Offset(c * step + 1.dp.toPx(), r * step + 1.dp.toPx()),
                        size = Size(step - 2.dp.toPx(), step - 2.dp.toPx())
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row {
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.width(3.dp))
            Text(
                text = duration,
                color = color,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
private fun PulseCore(
    state: HullPulseUiState,
    modifier: Modifier = Modifier
) {
    val isHolding = state.phase == PulsePhase.HOLD || state.phase == PulsePhase.CONTRACT
    val isReleasing = state.phase == PulsePhase.RELEASE

    // Dynamic inner radial core scale: HOLD swells toward 0.9, RELEASE collapses toward 0.25
    val intensityScale by animateFloatAsState(
        targetValue = when {
            isHolding -> 0.9f
            isReleasing -> 0.25f
            else -> 0.4f
        },
        animationSpec = tween(
            durationMillis = 4000,
            easing = if (isHolding) FastOutSlowInEasing else LinearOutSlowInEasing
        ),
        label = "PulseCoreScale"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "PulseGlow")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "GlowAlpha"
    )

    val coreColor = if (isHolding) NeonGreen else if (isReleasing) NeonCyan else Color.White.copy(alpha = 0.5f)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val strokeWidth = 4.dp.toPx()
            val outerRadius = (size.minDimension - strokeWidth * 6) / 2f

            // 1. Outer Track Ring
            drawCircle(
                color = Color.White.copy(alpha = 0.08f),
                radius = outerRadius,
                style = Stroke(width = strokeWidth)
            )

            // 2. Set Progress Arc (Outer Ring = totalProgress)
            val sweepAngle = state.totalProgress.coerceIn(0f, 1f) * 360f
            if (sweepAngle > 0f) {
                for (i in 0..4) {
                    val f = i.toFloat()
                    val glowAlpha = (0.25f - f * 0.04f).coerceAtLeast(0f)
                    drawArc(
                        color = NeonGreen.copy(alpha = glowAlpha),
                        startAngle = -90f,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = strokeWidth + f * 6f, cap = StrokeCap.Round)
                    )
                }

                drawArc(
                    color = NeonGreen,
                    startAngle = -90f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            // 3. Inner Radial Core (Holding swell / Release collapse)
            val coreRadius = outerRadius * intensityScale
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        coreColor.copy(alpha = 0.35f * pulseAlpha),
                        coreColor.copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = coreRadius
                ),
                radius = coreRadius
            )

            // Dynamic Inner Boundary Ring
            drawCircle(
                color = coreColor.copy(alpha = if (isHolding) 0.6f else 0.2f),
                radius = coreRadius,
                style = Stroke(width = if (isHolding) 2.5.dp.toPx() else 1.dp.toPx())
            )
        }

        // Center Content Text
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val phaseLabel = when (state.phase) {
                PulsePhase.HOLD -> "HOLD"
                PulsePhase.CONTRACT -> "CONTRACT"
                PulsePhase.RELEASE -> "RELEASE"
                PulsePhase.IDLE -> "READY"
            }

            Text(
                text = phaseLabel,
                color = coreColor,
                fontSize = 36.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black,
                letterSpacing = 3.sp
            )

            if (state.isRunning) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${state.phaseTimeRemaining}s",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = "${state.currentCycle}/${state.totalCycles} CYCLES",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
private fun ChipIcon(color: Color) {
    Canvas(modifier = Modifier.size(16.dp)) {
        // Microchip square
        drawRect(
            color = color,
            topLeft = Offset(size.width * 0.2f, size.height * 0.2f),
            size = Size(size.width * 0.6f, size.height * 0.6f),
            style = Stroke(width = 1.5.dp.toPx())
        )
        // Chip pins
        val stroke = 1.dp.toPx()
        drawLine(color, Offset(size.width * 0.35f, 0f), Offset(size.width * 0.35f, size.height * 0.2f), stroke)
        drawLine(color, Offset(size.width * 0.65f, 0f), Offset(size.width * 0.65f, size.height * 0.2f), stroke)
        drawLine(color, Offset(size.width * 0.35f, size.height * 0.8f), Offset(size.width * 0.35f, size.height), stroke)
        drawLine(color, Offset(size.width * 0.65f, size.height * 0.8f), Offset(size.width * 0.65f, size.height), stroke)
        drawLine(color, Offset(0f, size.height * 0.35f), Offset(size.width * 0.2f, size.height * 0.35f), stroke)
        drawLine(color, Offset(0f, size.height * 0.65f), Offset(size.width * 0.2f, size.height * 0.65f), stroke)
        drawLine(color, Offset(size.width * 0.8f, size.height * 0.35f), Offset(size.width, size.height * 0.35f), stroke)
        drawLine(color, Offset(size.width * 0.8f, size.height * 0.65f), Offset(size.width, size.height * 0.65f), stroke)
    }
}
