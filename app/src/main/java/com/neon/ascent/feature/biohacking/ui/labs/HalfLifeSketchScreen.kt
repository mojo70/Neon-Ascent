package com.neon.ascent.feature.biohacking.ui.labs

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neon.ascent.core.domain.labs.HalfLifeCalculator
import com.neon.ascent.ui.CyberFrame
import com.neon.ascent.ui.GlitchText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HalfLifeSketchScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    cyan: Color = Color(0xFF00F5FF),
    magenta: Color = Color(0xFFFF0088),
    darkBg: Color = Color(0xFF0A0F14)
) {
    var halfLifeInput by remember { mutableStateOf("24") }
    var halfLifeInDays by remember { mutableStateOf(false) }

    var intervalInput by remember { mutableStateOf("24") }
    var intervalInDays by remember { mutableStateOf(false) }

    var doseAmountInput by remember { mutableStateOf("1.0") }
    var numDosesInput by remember { mutableStateOf("10") }

    val halfLifeValue = halfLifeInput.toDoubleOrNull() ?: 0.0
    val halfLifeHours = if (halfLifeInDays) halfLifeValue * 24.0 else halfLifeValue

    val intervalValue = intervalInput.toDoubleOrNull() ?: 0.0
    val intervalHours = if (intervalInDays) intervalValue * 24.0 else intervalValue

    val doseAmountMg = doseAmountInput.toDoubleOrNull() ?: 0.0
    val numDoses = numDosesInput.toIntOrNull() ?: 10

    val decayResult = remember(halfLifeHours, intervalHours, doseAmountMg, numDoses) {
        if (halfLifeHours > 0 && intervalHours > 0 && doseAmountMg > 0 && numDoses > 0) {
            HalfLifeCalculator.calculateDecay(halfLifeHours, intervalHours, doseAmountMg, numDoses)
        } else null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    GlitchText(
                        text = "HALF_LIFE_SKETCH",
                        color = cyan,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = cyan)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = darkBg)
            )
        },
        containerColor = darkBg
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Disclaimer Banner
            CyberFrame(label = "DISCLAIMER // RESEARCH_ONLY", borderColor = magenta.copy(alpha = 0.8f)) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.WarningAmber,
                        contentDescription = "Warning",
                        tint = magenta,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Math reference tool for personal/research reference only. Not medical advice or a dosing recommendation.",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 14.sp
                    )
                }
            }

            // Inputs Section
            CyberFrame(label = "PARAMETERS", borderColor = cyan) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Half Life Input
                    Column {
                        Text("COMPOUND_HALF_LIFE", color = cyan, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = halfLifeInput,
                                onValueChange = { halfLifeInput = it },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(color = Color.White, fontFamily = FontFamily.Monospace),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = cyan, unfocusedBorderColor = Color.DarkGray)
                            )
                            Row(
                                modifier = Modifier
                                    .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                    .border(1.dp, cyan.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                            ) {
                                Button(
                                    onClick = { halfLifeInDays = false },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = if (!halfLifeInDays) cyan.copy(alpha = 0.3f) else Color.Transparent, contentColor = if (!halfLifeInDays) cyan else Color.Gray),
                                    shape = RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp)
                                ) { Text("hrs", fontSize = 11.sp, fontFamily = FontFamily.Monospace) }
                                Button(
                                    onClick = { halfLifeInDays = true },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = if (halfLifeInDays) cyan.copy(alpha = 0.3f) else Color.Transparent, contentColor = if (halfLifeInDays) cyan else Color.Gray),
                                    shape = RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp)
                                ) { Text("days", fontSize = 11.sp, fontFamily = FontFamily.Monospace) }
                            }
                        }
                    }

                    // Dose Interval Input
                    Column {
                        Text("DOSE_INTERVAL", color = cyan, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = intervalInput,
                                onValueChange = { intervalInput = it },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(color = Color.White, fontFamily = FontFamily.Monospace),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = cyan, unfocusedBorderColor = Color.DarkGray)
                            )
                            Row(
                                modifier = Modifier
                                    .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                    .border(1.dp, cyan.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                            ) {
                                Button(
                                    onClick = { intervalInDays = false },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = if (!intervalInDays) cyan.copy(alpha = 0.3f) else Color.Transparent, contentColor = if (!intervalInDays) cyan else Color.Gray),
                                    shape = RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp)
                                ) { Text("hrs", fontSize = 11.sp, fontFamily = FontFamily.Monospace) }
                                Button(
                                    onClick = { intervalInDays = true },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = if (intervalInDays) cyan.copy(alpha = 0.3f) else Color.Transparent, contentColor = if (intervalInDays) cyan else Color.Gray),
                                    shape = RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp)
                                ) { Text("days", fontSize = 11.sp, fontFamily = FontFamily.Monospace) }
                            }
                        }
                    }

                    // Dose Amount (mg) & Number of Doses
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("DOSE_AMOUNT (mg)", color = cyan, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = doseAmountInput,
                                onValueChange = { doseAmountInput = it },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(color = Color.White, fontFamily = FontFamily.Monospace),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = cyan, unfocusedBorderColor = Color.DarkGray)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("NUM_DOSES", color = cyan, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = numDosesInput,
                                onValueChange = { numDosesInput = it },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(color = Color.White, fontFamily = FontFamily.Monospace),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = cyan, unfocusedBorderColor = Color.DarkGray)
                            )
                        }
                    }
                }
            }

            // Results Section
            CyberFrame(label = "ACCUMULATION_&_STEADY_STATE", borderColor = cyan) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (decayResult != null) {
                        val peakVal = decayResult.peakSteadyStateMg.toPlainString()
                        val troughVal = decayResult.troughSteadyStateMg.toPlainString()
                        val timeSsHours = decayResult.timeToSteadyStateHours.toDouble()
                        val timeSsDays = (timeSsHours / 24.0).let { String.format(java.util.Locale.US, "%.1f", it) }

                        ResultRow("APPROX_STEADY_STATE_PEAK", "$peakVal mg", cyan, isHighlight = true)
                        ResultRow("APPROX_STEADY_STATE_TROUGH", "$troughVal mg", cyan)
                        ResultRow("TIME_TO_STEADY_STATE (~4.5 t½)", "$timeSsHours hrs ($timeSsDays days)", cyan)
                        ResultRow("PEAK_TO_TROUGH_RATIO", "${decayResult.peakTroughRatio.toPlainString()}x", cyan)

                        Spacer(modifier = Modifier.height(8.dp))

                        // Decay Canvas Chart
                        Text("EXPONENTIAL_DECAY_CURVE", color = cyan, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        DecayCurveChart(points = decayResult.curvePoints, cyan = cyan, magenta = magenta)

                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("ENTER VALID PARAMETERS TO COMPUTE DECAY", color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }
                    }

                    Text(
                        text = "ℹ️ MODEL NOTE: Simple exponential 1-compartment decay approximation. Does not model multi-compartment kinetics or depot absorption delays.",
                        color = Color.Gray,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
private fun DecayCurveChart(
    points: List<HalfLifeCalculator.TimePoint>,
    cyan: Color,
    magenta: Color,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) return

    val maxAmount = points.maxOf { it.amountMg }.coerceAtLeast(0.001)
    val maxTime = points.last().timeHours.coerceAtLeast(1.0)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(140.dp)
            .background(Color(0xFF0F1722), RoundedCornerShape(4.dp))
            .border(1.dp, cyan.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
            .padding(12.dp)
    ) {
        val w = size.width
        val h = size.height

        // Draw Grid Lines
        val gridLines = 4
        for (i in 1 until gridLines) {
            val y = h * (i.toFloat() / gridLines)
            drawLine(
                color = Color.Gray.copy(alpha = 0.2f),
                start = Offset(0f, y),
                end = Offset(w, y),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
            )
        }

        // Draw Decay Curve
        val path = Path()
        points.forEachIndexed { idx, pt ->
            val x = (pt.timeHours / maxTime).toFloat() * w
            val y = h - ((pt.amountMg / maxAmount).toFloat() * h)
            if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        drawPath(
            path = path,
            color = cyan,
            style = Stroke(width = 3f)
        )
    }
}

@Composable
private fun ResultRow(label: String, value: String, color: Color, isHighlight: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isHighlight) color.copy(alpha = 0.15f) else Color.Transparent)
            .padding(vertical = 4.dp, horizontal = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = if (isHighlight) color else Color.LightGray, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Normal)
        Text(value, color = if (isHighlight) color else Color.White, fontSize = if (isHighlight) 13.sp else 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
    }
}
