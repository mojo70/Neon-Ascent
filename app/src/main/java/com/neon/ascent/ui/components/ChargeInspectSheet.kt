package com.neon.ascent.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neon.ascent.core.common.LocalNeonTheme
import com.neon.ascent.core.domain.health.ChargeConfidence
import com.neon.ascent.core.domain.health.NeonCharge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChargeInspectSheet(
    charge: NeonCharge?,
    onDismiss: () -> Unit,
    isLabsView: Boolean = false
) {
    val theme = LocalNeonTheme.current
    val cyan = Color(0xFF00F5FF)
    val magenta = Color(0xFFFF0088)
    val orange = Color(0xFFFF8C00)
    val green = Color(0xFF00FF9C)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = theme.canvas,
        contentColor = theme.ink,
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = cyan.copy(alpha = 0.5f))
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            val chargeVal = charge?.value ?: 62
            val bandStr = when {
                chargeVal <= 35 -> "LOW"
                chargeVal <= 70 -> "OK"
                else -> "HIGH"
            }
            val bandColor = when (bandStr) {
                "LOW" -> magenta
                "OK" -> orange
                else -> green
            }

            // Header: Tank score & band
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "CHARGE INSPECTION // DRIVERS",
                        color = cyan,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tank $chargeVal · $bandStr",
                        color = theme.ink,
                        fontSize = 22.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .background(bandColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                        .border(1.dp, bandColor, RoundedCornerShape(4.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "CONF: ${charge?.confidence?.name ?: "LOW"}",
                        color = bandColor,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = theme.ink.copy(alpha = 0.15f))
            Spacer(modifier = Modifier.height(16.dp))

            // Wake Seed Section
            val drivers = charge?.drivers ?: emptyList()
            val coldStart = drivers.any { it.first == "COLD_START" }
            val sleepDriver = drivers.find { it.first == "SLEEP" }
            val seedDriver = drivers.find { it.first == "SEED" }

            val wakeLine = when {
                seedDriver != null -> "Wake ${charge?.wakeSeed ?: 62} (clearance frozen)"
                coldStart -> "Wake ${charge?.wakeSeed ?: 62} (baseline cold start, missing sleep)"
                sleepDriver != null -> {
                    val detail = sleepDriver.second
                    if (detail.startsWith("SANCTUM")) {
                        "Wake ${charge?.wakeSeed ?: 72} · $detail"
                    } else {
                        "Wake ${charge?.wakeSeed ?: 72} after ${detail.removePrefix("SLEEP ")} / 7h30"
                    }
                }
                else -> "Wake ${charge?.wakeSeed ?: 62}"
            }

            Text(
                text = wakeLine,
                color = theme.ink,
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Drains & Drivers Section
            val workoutDriver = drivers.find { it.first == "WORKOUT_DRAIN" }
            val stepsDriver = drivers.find { it.first == "STEPS_DRAIN" }
            val hrLoadDriver = drivers.find { it.first == "HR_LOAD" }
            val sitDriver = drivers.find { it.first == "SIT" }

            val drainClauses = mutableListOf<String>()

            // Passive clock drain (wakeSeed - workout - steps - hrLoad vs current value)
            val wakeSeedVal = charge?.wakeSeed ?: 62
            val totalDrainVal = (wakeSeedVal - chargeVal).coerceAtLeast(0)
            if (totalDrainVal > 0) {
                drainClauses.add("Clock −$totalDrainVal total drain")
            }

            stepsDriver?.let {
                val detail = it.second
                val pts = detail.substringAfter("(-").substringBefore(" pts)").ifEmpty { "" }
                val clause = if (pts.isNotEmpty()) "steps −$pts" else detail
                drainClauses.add(clause)
            }

            workoutDriver?.let {
                val detail = it.second
                val pts = detail.substringAfter("(-").substringBefore(" pts)").ifEmpty { "" }
                val clause = if (pts.isNotEmpty()) "session −$pts" else detail
                drainClauses.add(clause)
            }

            hrLoadDriver?.let {
                drainClauses.add("HR_LOAD ${it.second}")
            }

            sitDriver?.let {
                drainClauses.add("Sit ${it.second} masked")
            }

            if (drainClauses.isNotEmpty()) {
                Text(
                    text = drainClauses.joinToString(" · "),
                    color = theme.ink.copy(alpha = 0.85f),
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Confidence / thin data warnings
            if (charge?.confidence == ChargeConfidence.LOW) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "HR_SERIES_THIN · insufficient baseline samples",
                    color = orange,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            // LABS Verbose Driver Tokens (if in LABS view)
            if (isLabsView && drivers.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = theme.ink.copy(alpha = 0.15f))
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "RAW ENGINE TOKENS:",
                    color = cyan.copy(alpha = 0.7f),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                drivers.forEach { (tag, desc) ->
                    Text(
                        text = "• [$tag] $desc",
                        color = theme.ink.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = cyan.copy(alpha = 0.15f),
                    contentColor = cyan
                ),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = "DISMISS",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
