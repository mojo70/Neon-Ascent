package com.neon.ascent.feature.biohacking.ui.labs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neon.ascent.core.common.LocalNeonTheme
import com.neon.ascent.core.data.local.entity.NeuralMemory
import com.neon.ascent.feature.biohacking.BioSliderField
import com.neon.ascent.feature.biohacking.EffectivenessLogItem
import com.neon.ascent.feature.biohacking.UploadCard
import com.neon.ascent.model.BioProtocolLog
import com.neon.ascent.ui.CyberActionButton
import com.neon.ascent.ui.CyberFrame

private val scanSectors = listOf(
    "CRANIAL_NODE" to "Cognition",
    "CORE_CHASSIS" to "Endocrine",
    "MOTOR_EXTREMITIES" to "Recovery"
)

@Composable
fun LabsAnalyzeSection(
    selectedSector: String,
    onSectorSelected: (String) -> Unit,
    scanEnabled: Boolean,
    onRunScan: () -> Unit,
    isLocalAiAvailable: Boolean,
    isDownloading: Boolean,
    downloadProgress: Float?,
    onDownloadModel: () -> Unit,
    onUploadLab: () -> Unit,
    report: String?,
    showEffectivenessForm: Boolean,
    onLogHowThisFelt: () -> Unit,
    onTurnIntoDirective: (String) -> Unit,
    onLogEffectiveness: (energy: Int, sleep: Int, mood: Int, focus: Int, sideEffects: String, notes: String) -> Unit,
    logs: List<BioProtocolLog>,
    neuralInsights: List<NeuralMemory>,
    onForgeInsight: (NeuralMemory) -> Unit,
    neonCyan: Color,
    neonMagenta: Color,
    modifier: Modifier = Modifier
) {
    val theme = LocalNeonTheme.current
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "Pick a sector, then run the scan.",
            color = theme.ink,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            scanSectors.forEach { (id, label) ->
                FilterChip(
                    selected = selectedSector == id,
                    onClick = { onSectorSelected(id) },
                    label = { Text(label, fontSize = 12.sp) }
                )
            }
        }
        CyberActionButton(
            label = "Run scan",
            color = if (scanEnabled) neonCyan else theme.inkMuted,
            enabled = scanEnabled,
            onClick = onRunScan
        )
        UploadCard(
            label = "Upload lab file",
            modifier = Modifier.fillMaxWidth(),
            color = neonCyan,
            onClick = onUploadLab
        )
        if (!isLocalAiAvailable) {
            CyberFrame(label = "NEURAL_ENGINE_MISSING", borderColor = neonMagenta) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Local AI capabilities require the Neural Engine core (Gemma 4-E2B). Download now to enable secure, on-device biohacking protocols.",
                        color = theme.ink,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    if (isDownloading) {
                        LinearProgressIndicator(
                            progress = { downloadProgress ?: 0f },
                            modifier = Modifier.fillMaxWidth().height(2.dp),
                            color = neonCyan,
                            trackColor = neonCyan.copy(alpha = 0.1f)
                        )
                        Text(
                            text = "Downloading Gemma 4-E2B",
                            color = neonCyan,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    } else {
                        CyberActionButton(
                            label = "DOWNLOAD NEURAL_ENGINE (Gemma 4-E2B)",
                            color = neonCyan,
                            onClick = onDownloadModel
                        )
                    }
                }
            }
        }
        if (report != null) {
            Text(
                text = "Generated advice. Check it with a clinician before you act on it.",
                color = neonMagenta,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = report,
                color = theme.ink,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.fillMaxWidth()
            )
            CyberActionButton(
                label = "Log how this felt",
                color = neonCyan,
                onClick = onLogHowThisFelt
            )
            CyberActionButton(
                label = "Turn into a directive",
                color = neonMagenta,
                onClick = { onTurnIntoDirective(report) }
            )
        }
        if (showEffectivenessForm) {
            EffectivenessForm(neonCyan = neonCyan, onLog = onLogEffectiveness)
        }
        if (neuralInsights.isNotEmpty()) {
            CyberFrame(label = "NEURAL_INSIGHTS", borderColor = neonCyan) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    neuralInsights.take(3).forEach { insight ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = insight.content,
                                color = theme.ink,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontStyle = FontStyle.Italic,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { onForgeInsight(insight) }) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = "Turn insight into a directive",
                                    tint = neonCyan
                                )
                            }
                        }
                    }
                }
            }
        }
        if (logs.isNotEmpty()) {
            Text(
                text = "PROTOCOL LOG",
                color = neonCyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )
            logs.take(5).forEach { log ->
                EffectivenessLogItem(log, neonCyan)
            }
        }
    }
}

@Composable
private fun EffectivenessForm(
    neonCyan: Color,
    onLog: (energy: Int, sleep: Int, mood: Int, focus: Int, sideEffects: String, notes: String) -> Unit
) {
    val theme = LocalNeonTheme.current
    var energy by remember { mutableFloatStateOf(5f) }
    var sleep by remember { mutableFloatStateOf(5f) }
    var mood by remember { mutableFloatStateOf(5f) }
    var focus by remember { mutableFloatStateOf(5f) }
    var sideEffects by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "How this protocol felt",
            color = theme.ink,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace
        )
        BioSliderField("Energy", energy, 1f..10f, neonCyan) { energy = it }
        BioSliderField("Sleep quality", sleep, 1f..10f, neonCyan) { sleep = it }
        BioSliderField("Mood", mood, 1f..10f, neonCyan) { mood = it }
        BioSliderField("Focus", focus, 1f..10f, neonCyan) { focus = it }
        OutlinedTextField(
            value = sideEffects,
            onValueChange = { sideEffects = it },
            label = { Text("Side effects", color = neonCyan, fontSize = 12.sp) },
            modifier = Modifier.fillMaxWidth(),
            textStyle = androidx.compose.material3.MaterialTheme.typography.bodyMedium.copy(
                color = theme.ink,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp
            )
        )
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Notes", color = neonCyan, fontSize = 12.sp) },
            modifier = Modifier.fillMaxWidth(),
            textStyle = androidx.compose.material3.MaterialTheme.typography.bodyMedium.copy(
                color = theme.ink,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp
            )
        )
        CyberActionButton(
            label = "Save log",
            color = neonCyan,
            onClick = {
                onLog(
                    energy.toInt(),
                    sleep.toInt(),
                    mood.toInt(),
                    focus.toInt(),
                    sideEffects,
                    notes
                )
            }
        )
    }
}
