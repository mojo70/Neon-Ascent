package com.neon.ascent.feature.biohacking

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neon.ascent.ui.CyberCutShape
import com.neon.ascent.core.domain.health.models.VitalsSnapshot
import kotlin.math.sqrt

import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextOverflow
import com.neon.ascent.core.domain.health.ChargeConfidence
import com.neon.ascent.core.domain.health.NeonCharge

@Composable
fun VitalsStack(
    snapshot: VitalsSnapshot?,
    rhrSeries: List<Pair<java.time.LocalDate, Double>>,
    hrvSeries: List<Pair<java.time.LocalDate, Double>>,
    completedSessions: Int,
    scheduledSessions: Int,
    recoveryScore: com.neon.ascent.core.domain.workout.models.RecoveryScore?,
    neonCharge: NeonCharge?,
    macros: com.neon.ascent.core.domain.workout.rules.Macros?,
    hasNutritionPermission: Boolean,
    neonCyan: Color,
    neonMagenta: Color,
    onNavigateToOps: () -> Unit,
    onRequestNutritionPermission: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        NeonChargeCard(
            charge = neonCharge,
            neonCyan = neonCyan,
            neonMagenta = neonMagenta
        )

        SleepLastNightCard(snapshot, neonCyan, neonMagenta)
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RecoverySignalTile(
                label = "RHR",
                value = snapshot?.restingHeartRate?.toDouble(),
                unit = "BPM",
                series = rhrSeries,
                color = neonCyan,
                modifier = Modifier.weight(1f),
                emptyLabel = "RHR_NOT_IN_YET"
            )
            RecoverySignalTile(
                label = "HRV",
                value = snapshot?.hrvRmssd,
                unit = "ms",
                series = hrvSeries,
                color = Color(0xFF00FF9C),
                modifier = Modifier.weight(1f),
                emptyLabel = "HRV_NOT_IN_YET"
            )
        }

        WeeklyCompoundsCard(completedSessions, scheduledSessions, neonCyan, onNavigateToOps)

        FuelCard(
            targetKcal = macros?.calories,
            burnedKcal = snapshot?.calories?.toInt(),
            consumedKcal = snapshot?.caloriesConsumed,
            hasNutritionPermission = hasNutritionPermission,
            neonCyan = neonCyan,
            neonMagenta = neonMagenta,
            onRequestNutritionPermission = onRequestNutritionPermission
        )

        TodayLoadCard(snapshot, neonCyan, neonMagenta)

        LogbookRecoveryCard(
            score = recoveryScore?.totalScore,
            status = recoveryScore?.status?.name,
            neonCyan = neonCyan
        )
    }
}

@Composable
fun NeonChargeCard(
    charge: NeonCharge?,
    neonCyan: Color,
    neonMagenta: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = CyberCutShape,
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.4f)),
        border = BorderStroke(1.dp, neonCyan.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "NEON_CHARGE",
                    color = neonCyan,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "[CONFIDENCE: ${charge?.confidence?.name ?: "LOW"}]",
                    color = when (charge?.confidence) {
                        ChargeConfidence.HIGH -> Color(0xFF00FF9C)
                        ChargeConfidence.MED -> Color(0xFFFF8C00)
                        else -> Color.Gray
                    },
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                val valPct = (charge?.value ?: 62) / 100f
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(72.dp)) {
                        Canvas(modifier = Modifier.size(72.dp)) {
                            drawArc(
                                color = Color.White.copy(alpha = 0.1f),
                                startAngle = 135f,
                                sweepAngle = 270f,
                                useCenter = false,
                                style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                            )
                            drawArc(
                                color = if (valPct > 0.6f) Color(0xFF00FF9C) else if (valPct > 0.3f) Color(0xFFFF8C00) else neonMagenta,
                                startAngle = 135f,
                                sweepAngle = 270f * valPct,
                                useCenter = false,
                                style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                        Text(
                            text = "${charge?.value ?: 62}%",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black
                        )
                    }

                    val hasClearance = charge != null && charge.drivers.any { it.first == "SLEEP" }
                    if (hasClearance) {
                        Spacer(modifier = Modifier.height(6.dp))
                        val seedVal = charge.wakeSeed
                        val band = when (seedVal) {
                            in 80..100 -> "CLEAR"
                            in 65..79 -> "WATCH"
                            in 50..64 -> "HOLD"
                            else -> "GROUND"
                        }
                        val lowSuffix = if (charge.confidence == ChargeConfidence.LOW) " · LOW" else ""
                        Text(
                            text = "SEED $seedVal $band · FROZEN$lowSuffix",
                            color = neonCyan,
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "PRIMARY DRIVERS:",
                        color = Color.Gray,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val nonSleepDrivers = charge?.drivers?.filter { (tag, _) ->
                        tag != "SLEEP" && tag != "HRV_STRESS" && tag != "RHR_STRESS"
                    } ?: emptyList()
                    val displayDrivers = if (nonSleepDrivers.isNotEmpty()) nonSleepDrivers else (charge?.drivers ?: listOf("COLD_START" to "Default baseline (62%)"))
                    displayDrivers.take(3).forEach { (tag, desc) ->
                        val lineText = when (tag) {
                            "HR_LOAD" -> "• HR_LOAD $desc"
                            "STEPS_DRAIN", "WORKOUT_DRAIN" -> "• $desc"
                            else -> "• $tag: $desc"
                        }
                        Text(
                            text = lineText,
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "NEON_MODEL · NOT_GARMIN",
                color = Color.DarkGray,
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun WeeklyCompoundsCard(
    completed: Int,
    scheduled: Int,
    neonCyan: Color,
    onDeepLink: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onDeepLink() },
        shape = CyberCutShape,
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.4f)),
        border = BorderStroke(1.dp, neonCyan.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "WEEKLY_COMPOUNDS",
                color = neonCyan,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$completed / $scheduled",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black
                )
                Text(
                    " heavy days",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            
            if (scheduled > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (i in 1..scheduled) {
                        val isDone = i <= completed
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .border(1.dp, if (isDone) neonCyan else Color.DarkGray, RoundedCornerShape(2.dp))
                                .background(if (isDone) neonCyan.copy(alpha = 0.2f) else Color.Transparent),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isDone) {
                                Text("✓", color = neonCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SleepLastNightCard(
    snapshot: VitalsSnapshot?,
    neonCyan: Color,
    neonMagenta: Color
) {
    val duration = snapshot?.sleepDurationMinutes
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = CyberCutShape,
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.4f)),
        border = BorderStroke(1.dp, neonCyan.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "SLEEP_LAST_NIGHT",
                color = neonCyan,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (duration != null && duration > 0) {
                val h = duration / 60
                val m = duration % 60
                Text(
                    text = "${h}h ${m}m",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black
                )

                if (!snapshot.sleepStages.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    SleepStageBar(snapshot.sleepStages)
                }
            } else {
                Text(
                    "SLEEP_SESSION_NOT_IN_YET",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun SleepStageBar(stages: Map<String, Int>) {
    val total = stages.values.sum().toFloat().coerceAtLeast(1f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(1.dp))
    ) {
        StageSegment(stages["DEEP"] ?: 0, total, Color(0xFF0066FF))
        StageSegment(stages["LIGHT"] ?: 0, total, Color(0xFF00CCFF))
        StageSegment(stages["REM"] ?: 0, total, Color(0xFF8800FF))
        StageSegment(stages["AWAKE"] ?: 0, total, Color(0xFFFF006E))
    }
}

@Composable
fun RowScope.StageSegment(value: Int, total: Float, color: Color) {
    if (value > 0) {
        Box(
            modifier = Modifier
                .weight(value / total)
                .fillMaxHeight()
                .background(color)
        )
    }
}

@Composable
fun RecoverySignalTile(
    label: String,
    value: Double?,
    unit: String,
    series: List<Pair<java.time.LocalDate, Double>>,
    color: Color,
    modifier: Modifier = Modifier,
    emptyLabel: String
) {
    val inRange = if (series.size >= 3 && value != null) {
        val values = series.map { it.second }
        val mean = values.average()
        val variance = values.map { (it - mean) * (it - mean) }.average()
        val sd = sqrt(variance)
        value >= (mean - sd) && value <= (mean + sd)
    } else false

    Card(
        modifier = modifier.height(110.dp),
        shape = CyberCutShape,
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.4f)),
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(label, color = color, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                if (inRange) {
                    Text("IN_RANGE", color = color, fontSize = 8.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                }
            }

            if (value != null) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        value.toInt().toString(),
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        " $unit",
                        color = Color.Gray,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 3.dp)
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                MiniSparkline(series.map { it.second.toFloat() }, color, Modifier.fillMaxWidth().height(24.dp))
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                Text(emptyLabel, color = Color.DarkGray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
fun TodayLoadCard(
    snapshot: VitalsSnapshot?,
    neonCyan: Color,
    neonMagenta: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = CyberCutShape,
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.4f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "TODAY_LOAD",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                LoadItem("STEPS", snapshot?.steps?.toString() ?: "--", neonCyan)
                LoadItem("KCAL", snapshot?.calories?.toInt()?.toString() ?: "--", Color(0xFFFF8C00), sub = "TOTAL")
                
                val isFreshHr = snapshot != null && snapshot.liveHeartRate != null && (System.currentTimeMillis() - snapshot.timestamp) < 300000
                LoadItem(
                    "HR", 
                    if (isFreshHr) snapshot?.liveHeartRate?.toString() ?: "--" else "--", 
                    neonMagenta, 
                    sub = if (isFreshHr) "LIVE" else "STALE"
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "SOURCE: HC",
                color = Color.DarkGray,
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun LogbookRecoveryCard(
    score: Int?,
    status: String?,
    neonCyan: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = CyberCutShape,
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.4f)),
        border = BorderStroke(1.dp, neonCyan.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "LOGBOOK_RECOVERY",
                color = neonCyan,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))

            if (score != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$score",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = " / 100",
                        color = Color.Gray,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    status?.let {
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "[${it.uppercase()}]",
                            color = if (it.equals("OPTIMAL", ignoreCase = true)) Color(0xFF00FF9C) else Color(0xFFFF8C00),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                Text(
                    "LOGBOOK_DATA_NOT_IN_YET",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun LoadItem(label: String, value: String, color: Color, sub: String? = null) {
    Column {
        Text(label, color = color, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        Text(value, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
        sub?.let {
            Text(it, color = Color.DarkGray, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
fun MiniSparkline(data: List<Float>, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        if (data.size < 2) return@Canvas
        val min = data.minOrNull() ?: 0f
        val max = data.maxOrNull() ?: 1f
        val range = (max - min).coerceAtLeast(1f)
        val width = size.width
        val height = size.height
        val stepX = width / (data.size - 1)
        val points = data.mapIndexed { i, v -> Offset(i * stepX, height - ((v - min) / range * height)) }
        val path = Path().apply {
            moveTo(points.first().x, points.first().y)
            for (i in 1 until points.size) lineTo(points[i].x, points[i].y)
        }
        drawPath(path, color, style = Stroke(1.5.dp.toPx()))
    }
}

@Composable
fun FuelCard(
    targetKcal: Int?,
    burnedKcal: Int?,
    consumedKcal: Double?,
    hasNutritionPermission: Boolean,
    neonCyan: Color,
    neonMagenta: Color,
    onRequestNutritionPermission: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = CyberCutShape,
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.4f)),
        border = BorderStroke(1.dp, neonCyan.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "FUEL // ENERGY_BALANCE",
                    color = neonCyan,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )

                if (!hasNutritionPermission) {
                    Text(
                        "[GRANT_NUTRITION]",
                        color = Color(0xFFFF8C00),
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onRequestNutritionPermission() }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                FuelMetricItem(
                    label = "TARGET",
                    value = targetKcal?.toString() ?: "--",
                    unit = "KCAL",
                    color = Color.White
                )
                FuelMetricItem(
                    label = "EATEN",
                    value = if (!hasNutritionPermission) {
                        "[GRANT_NUTRITION]"
                    } else if (consumedKcal != null && consumedKcal > 0) {
                        consumedKcal.toInt().toString()
                    } else {
                        "NOT_LOGGED"
                    },
                    unit = if (hasNutritionPermission && consumedKcal != null && consumedKcal > 0) "KCAL" else null,
                    color = if (!hasNutritionPermission) {
                        Color(0xFFFF8C00)
                    } else if (consumedKcal != null && consumedKcal > 0) {
                        Color(0xFF00FF9C)
                    } else {
                        Color.Gray
                    },
                    onClick = if (!hasNutritionPermission) onRequestNutritionPermission else null
                )
                FuelMetricItem(
                    label = "BURNED",
                    value = burnedKcal?.toString() ?: "--",
                    unit = "KCAL",
                    color = Color(0xFFFF8C00)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = if (!hasNutritionPermission) {
                    "Grant Nutrition permission to sync intake from Health Connect."
                } else if (consumedKcal == null || consumedKcal <= 0) {
                    "Log meals in Fit or another app connected to Health Connect."
                } else {
                    "SOURCE: HEALTH CONNECT"
                },
                color = if (!hasNutritionPermission) Color(0xFFFF8C00) else Color.DarkGray,
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.then(
                    if (!hasNutritionPermission) Modifier.clickable { onRequestNutritionPermission() } else Modifier
                )
            )
        }
    }
}

@Composable
fun FuelMetricItem(
    label: String,
    value: String,
    unit: String? = null,
    color: Color,
    onClick: (() -> Unit)? = null
) {
    Column(modifier = if (onClick != null) Modifier.clickable { onClick() } else Modifier) {
        Text(
            label,
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                value,
                color = color,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )
            unit?.let {
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    " $it",
                    color = Color.Gray,
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
        }
    }
}
