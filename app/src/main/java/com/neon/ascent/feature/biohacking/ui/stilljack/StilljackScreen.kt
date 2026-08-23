package com.neon.ascent.feature.biohacking.ui.stilljack

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
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
import com.neon.ascent.core.common.FloatingParticles
import com.neon.ascent.core.common.NeonCyan
import com.neon.ascent.core.common.NeonGreen
import com.neon.ascent.core.common.NeonPink
import com.neon.ascent.core.common.Scanlines
import com.neon.ascent.core.common.StaticNoise
import com.neon.ascent.core.common.Vignette
import com.neon.ascent.core.common.neonBorder
import com.neon.ascent.ui.CyberActionButton
import com.neon.ascent.ui.CyberCutShape
import com.neon.ascent.ui.GlitchText
import com.neon.ascent.ui.HudCornerAccents
import com.neon.ascent.ui.SoftGridBackground
import java.util.Locale

@Composable
fun StilljackScreen(
    onBack: () -> Unit,
    viewModel: StilljackViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF010403))
    ) {
        // Multi-layered cyber background
        SoftGridBackground()
        Vignette()
        Scanlines(intensity = 0.12f)
        FloatingParticles(intensity = 0.08f)

        if (state.noiseEnabled && state.isRunning) {
            StaticNoise(intensity = 0.06f)
        }

        // HUD Corner Accents
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            HudCornerAccents(color = NeonGreen.copy(alpha = 0.35f))
        }

        // Main Screen Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header: Title & Glitch Effect
            GlitchText(
                text = "STILLJACK",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black
            )

            Spacer(Modifier.height(6.dp))

            // Subtitle / Status line
            Text(
                text = "FEED: MUTED  //  ICE: STANDBY  //  NO_WORD  //  NO_COLLECT  //  HOLD",
                color = NeonCyan.copy(alpha = 0.75f),
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(10.dp))

            // XP / Citadel Chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Box(
                    modifier = Modifier
                        .clip(CyberCutShape)
                        .background(Color(0xFF04120C).copy(alpha = 0.8f))
                        .neonBorder(NeonGreen.copy(alpha = 0.6f), width = 1.dp, cornerRadius = 0.dp)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "+40 XP / CITADEL",
                        color = NeonGreen,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Circular Hold Ring
            HoldRing(
                totalSeconds = state.totalSeconds,
                remainingSeconds = state.remainingSeconds,
                isRunning = state.isRunning,
                modifier = Modifier.size(240.dp)
            )

            Spacer(Modifier.height(20.dp))

            // Configuration Controls (Duration + Toggles + Footer hint)
            AnimatedVisibility(
                visible = !state.isRunning,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Duration Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(5, 10, 15, 20).forEach { mins ->
                            val isSelected = state.totalSeconds == mins * 60
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .clip(CyberCutShape)
                                    .background(
                                        if (isSelected) NeonGreen else Color(0xFF06100B).copy(alpha = 0.85f)
                                    )
                                    .neonBorder(
                                        color = if (isSelected) NeonGreen else NeonGreen.copy(alpha = 0.35f),
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        glowIntensity = if (isSelected) 0.8f else 0.2f,
                                        cornerRadius = 0.dp
                                    )
                                    .clickable { viewModel.setDuration(mins) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${mins}M",
                                    color = if (isSelected) Color.Black else Color.White.copy(alpha = 0.9f),
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Audio Toggles
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StilljackAudioToggleRow(
                            label = "NOISE",
                            enabled = state.noiseEnabled,
                            onClick = viewModel::toggleNoise
                        )
                        StilljackAudioToggleRow(
                            label = "START GONG",
                            enabled = state.startGongEnabled,
                            onClick = viewModel::toggleStartGong
                        )
                        StilljackAudioToggleRow(
                            label = "MID GONG",
                            enabled = state.midGongEnabled,
                            onClick = viewModel::toggleMidGong
                        )
                        StilljackAudioToggleRow(
                            label = "END GONG",
                            enabled = state.endGongEnabled,
                            onClick = viewModel::toggleEndGong
                        )
                    }

                    Spacer(Modifier.height(14.dp))

                    Text(
                        text = "WHITE_HISS + BOWL. ALL FOUR CAN GO SILENT.",
                        color = Color.White.copy(alpha = 0.35f),
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // Primary CTA Button
            CyberActionButton(
                label = if (state.isRunning) "ABORT HOLD" else "BEGIN STILLJACK",
                color = if (state.isRunning) NeonPink else NeonGreen,
                onClick = {
                    if (state.isRunning) {
                        viewModel.stopStilljack()
                    } else {
                        viewModel.startStilljack()
                    }
                }
            )

            Spacer(Modifier.height(16.dp))
        }

        // Close Button placed in front of content
        IconButton(
            onClick = {
                viewModel.stopStilljack()
                onBack()
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close Stilljack",
                tint = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun HoldRing(
    totalSeconds: Int,
    remainingSeconds: Int,
    isRunning: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "StilljackHoldRing")

    // Slow 4-6s scale breath pulse
    val breathScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BreathPulse"
    )

    val idleGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "IdleGlow"
    )

    val progress = if (totalSeconds > 0 && isRunning) {
        ((totalSeconds - remainingSeconds).toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 4.dp.toPx()
            val center = Offset(size.width / 2f, size.height / 2f)
            val outerRadius = (size.minDimension - strokeWidth * 6) / 2f
            val innerRadius = outerRadius * 0.78f

            // 1. Subtle inner breath pulse circle
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        NeonGreen.copy(alpha = 0.08f * (breathScale - 0.9f) * 6f),
                        NeonGreen.copy(alpha = 0.02f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = innerRadius * breathScale
                ),
                radius = innerRadius * breathScale
            )

            drawCircle(
                color = NeonCyan.copy(alpha = 0.12f),
                radius = innerRadius * breathScale,
                style = Stroke(width = 1.5.dp.toPx())
            )

            // 2. Base track (faint circular track)
            drawCircle(
                color = NeonGreen.copy(alpha = 0.15f),
                radius = outerRadius,
                style = Stroke(width = strokeWidth)
            )

            // Inner concentric guide ring
            drawCircle(
                color = NeonGreen.copy(alpha = 0.08f),
                radius = outerRadius - 12.dp.toPx(),
                style = Stroke(width = 1.dp.toPx())
            )

            // 3. Active Hold Arc / Full Glow
            val sweepAngle = if (isRunning) progress * 360f else 360f
            val baseColor = NeonGreen
            val glowMult = if (isRunning) 1f else idleGlowAlpha

            for (i in 0..5) {
                val f = i.toFloat()
                val glowAlpha = ((0.22f - f * 0.035f).coerceAtLeast(0f) * glowMult).coerceIn(0f, 1f)
                drawArc(
                    color = baseColor.copy(alpha = glowAlpha),
                    startAngle = -90f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(
                        width = strokeWidth + f * 7f,
                        cap = StrokeCap.Round
                    )
                )
            }

            drawArc(
                color = baseColor,
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Outer subtle boundary ring
            drawCircle(
                color = NeonGreen.copy(alpha = 0.2f),
                radius = outerRadius + 8.dp.toPx(),
                style = Stroke(width = 1.dp.toPx())
            )
        }

        // Centered Timer Inside Ring
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = formatSeconds(remainingSeconds),
                color = Color.White,
                fontSize = 38.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "/ ${formatSeconds(totalSeconds)}",
                color = Color.White.copy(alpha = 0.45f),
                fontSize = 18.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
        }
    }
}

@Composable
private fun StilljackAudioToggleRow(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(CyberCutShape)
            .background(
                if (enabled) Color(0xFF04180F).copy(alpha = 0.9f) else Color(0xFF050807).copy(alpha = 0.7f)
            )
            .neonBorder(
                color = if (enabled) NeonGreen.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.15f),
                width = 1.dp,
                glowIntensity = if (enabled) 0.5f else 0.05f,
                cornerRadius = 0.dp
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = if (enabled) Color.White else Color.White.copy(alpha = 0.45f),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
            Text(
                text = if (enabled) "ON" else "OFF",
                color = if (enabled) NeonGreen else Color.White.copy(alpha = 0.35f),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
        }
    }
}

private fun formatSeconds(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return String.format(Locale.US, "%02d:%02d", m, s)
}
