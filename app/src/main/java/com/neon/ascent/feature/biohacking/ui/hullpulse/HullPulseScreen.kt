package com.neon.ascent.feature.biohacking.ui.hullpulse

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neon.ascent.ui.SoftGridBackground
import com.neon.ascent.core.common.Vignette
import com.neon.ascent.ui.CyberCutShape

@Composable
fun HullPulseScreen(
    onBack: () -> Unit,
    viewModel: HullPulseViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val systemColor = Color(0xFF00FF9C)
    val accentColor = Color(0xFF00CCFF)

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF010101))) {
        SoftGridBackground()
        Vignette()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(16.dp)
        ) {

            // Technical Info Frame
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "HULL PULSE",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 2.sp
                        )
                        Box(
                            modifier = Modifier
                                .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                "+30 XP",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Text(
                        text = "Pelvic floor. Same as any other lift: contract, hold 4s, release 4s. 10 cycles. Sit or stand. Breathe.",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(top = 12.dp),
                        lineHeight = 18.sp
                    )

                    Spacer(Modifier.height(48.dp))

                    // Animated Pulse Core
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        val infiniteTransition = rememberInfiniteTransition(label = "Pulse")
                        val pulseScale by infiniteTransition.animateFloat(
                            initialValue = 1f,
                            targetValue = 1.05f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1000, easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "GlowPulse"
                        )

                        val intensityScale by animateFloatAsState(
                            targetValue = if (state.phase == PulsePhase.HOLD) 0.9f else 0.3f,
                            animationSpec = tween(
                                durationMillis = 4000, // Sync with 4s phase
                                easing = if (state.phase == PulsePhase.HOLD) FastOutSlowInEasing else LinearOutSlowInEasing
                            ),
                            label = "IntensityScale"
                        )

                        Canvas(modifier = Modifier.size(220.dp)) {
                            val center = Offset(size.width / 2, size.height / 2)
                            val baseRadius = size.width / 2
                            
                            // 1. Technical Outer Ring
                            drawCircle(
                                color = Color.White.copy(alpha = 0.05f),
                                radius = baseRadius,
                                style = Stroke(width = 1.dp.toPx())
                            )
                            
                            // 2. The Shaded "Intensity" Core
                            val coreRadius = baseRadius * intensityScale
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        systemColor.copy(alpha = 0.4f * pulseScale),
                                        systemColor.copy(alpha = 0.1f),
                                        Color.Transparent
                                    ),
                                    center = center,
                                    radius = coreRadius
                                ),
                                radius = coreRadius
                            )

                            // 3. Precision Tracking Ring
                            drawArc(
                                color = if (state.phase == PulsePhase.HOLD) systemColor else Color.White.copy(alpha = 0.1f),
                                startAngle = -90f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = Stroke(
                                    width = (if (state.phase == PulsePhase.HOLD) 4.dp else 1.dp).toPx(),
                                    cap = StrokeCap.Round
                                )
                            )
                            
                            // 4. Glowing Edge (only when holding)
                            if (state.phase == PulsePhase.HOLD) {
                                drawCircle(
                                    color = systemColor.copy(alpha = 0.2f),
                                    radius = coreRadius,
                                    style = Stroke(width = 2.dp.toPx())
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (state.phase == PulsePhase.HOLD) "HOLD" else if (state.phase == PulsePhase.RELEASE) "RELEASE" else "READY",
                                color = if (state.phase == PulsePhase.HOLD) systemColor else Color.White,
                                fontSize = 36.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp
                            )
                            Spacer(Modifier.height(4.dp))
                            if (state.isRunning) {
                                Text(
                                    text = "${state.phaseTimeRemaining}s",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 18.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = if (state.isRunning) "${state.currentCycle}/${state.totalCycles} CYCLES" else "INITIATE_UPLINK",
                                color = Color.White.copy(alpha = 0.4f),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(Modifier.height(48.dp))

                    // Linear Progress
                    Column {
                        LinearProgressIndicator(
                            progress = { state.totalProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = systemColor,
                            trackColor = Color.White.copy(alpha = 0.05f)
                        )
                        
                        Spacer(Modifier.height(24.dp))

                        // Controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (state.isRunning) {
                                Button(
                                    onClick = { viewModel.stopPulse() },
                                    modifier = Modifier
                                        .height(44.dp)
                                        .width(100.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White.copy(alpha = 0.1f),
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                                ) {
                                    Text("STOP", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Button(
                                    onClick = { viewModel.startPulse() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = systemColor,
                                        contentColor = Color.Black
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        "BEGIN HULL_PULSE",
                                        fontSize = 16.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Close Button
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(end = 16.dp)
        ) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
        }
    }
}
