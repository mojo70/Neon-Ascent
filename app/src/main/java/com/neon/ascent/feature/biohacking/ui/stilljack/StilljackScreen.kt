package com.neon.ascent.feature.biohacking.ui.stilljack

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neon.ascent.ui.CyberCutShape
import com.neon.ascent.ui.SoftGridBackground
import com.neon.ascent.core.common.Vignette
import java.util.Locale

@Composable
fun StilljackScreen(
    onBack: () -> Unit,
    viewModel: StilljackViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val systemColor = Color(0xFF00FF9C)

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF010101))) {
        SoftGridBackground()
        Vignette()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "STILLJACK",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "Sit. Breathe. Empty the feed. Not sanctuary – no Word, no collect. Just hold.",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        "+40 XP",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            // Timer Display
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = formatSeconds(state.remainingSeconds),
                    color = Color.White,
                    fontSize = 48.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = " / ${formatSeconds(state.totalSeconds)}",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 24.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 8.dp, start = 8.dp)
                )
            }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f), thickness = 1.dp)
            Spacer(Modifier.height(24.dp))

            // Duration Pickers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                listOf(5, 10, 15, 20).forEach { mins ->
                    val isSelected = state.totalSeconds == mins * 60
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .background(
                                if (isSelected) systemColor else Color.Black.copy(alpha = 0.2f),
                                RoundedCornerShape(8.dp)
                            )
                            .border(
                                1.dp,
                                if (isSelected) systemColor else Color.White.copy(alpha = 0.2f),
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { viewModel.setDuration(mins) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "${mins}M",
                            color = if (isSelected) Color.Black else Color.White,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Audio Toggles
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AudioToggle(
                        label = "NOISE",
                        enabled = state.noiseEnabled,
                        onClick = viewModel::toggleNoise,
                        modifier = Modifier.weight(1f)
                    )
                    AudioToggle(
                        label = "START GONG",
                        enabled = state.startGongEnabled,
                        onClick = viewModel::toggleStartGong,
                        modifier = Modifier.weight(1.5f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AudioToggle(
                        label = "MID GONG",
                        enabled = state.midGongEnabled,
                        onClick = viewModel::toggleMidGong,
                        modifier = Modifier.weight(1f)
                    )
                    AudioToggle(
                        label = "END GONG",
                        enabled = state.endGongEnabled,
                        onClick = viewModel::toggleEndGong,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                text = "WHITE_HISS + BOWL. ALL FOUR CAN GO SILENT.",
                color = Color.White.copy(alpha = 0.3f),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )

            Spacer(Modifier.weight(1f))

            // Primary Action
            Button(
                onClick = { if (state.isRunning) viewModel.stopStilljack() else viewModel.startStilljack() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (state.isRunning) Color.Red.copy(alpha = 0.4f) else systemColor,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (state.isRunning) "ABORT STILLJACK" else "BEGIN STILLJACK",
                    fontSize = 16.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            }
            
            Spacer(Modifier.height(100.dp))
        }

        // Close Button
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
        }
    }
}

@Composable
fun AudioToggle(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val systemColor = Color(0xFF00FF9C)
    Box(
        modifier = modifier
            .height(44.dp)
            .border(
                1.dp,
                if (enabled) systemColor.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.1f),
                RoundedCornerShape(8.dp)
            )
            .background(
                if (enabled) systemColor.copy(alpha = 0.05f) else Color.Transparent,
                RoundedCornerShape(8.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$label ${if (enabled) "ON" else "OFF"}",
            color = if (enabled) systemColor else Color.White.copy(alpha = 0.4f),
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun formatSeconds(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return String.format(Locale.US, "%02d:%02d", m, s)
}
