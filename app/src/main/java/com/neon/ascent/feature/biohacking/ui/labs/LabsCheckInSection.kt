package com.neon.ascent.feature.biohacking.ui.labs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neon.ascent.core.common.LocalNeonTheme
import com.neon.ascent.feature.biohacking.BioSliderField

@Composable
fun LabsCheckInSection(
    energy: Int,
    mood: Int,
    focus: Int,
    stress: Int,
    objective: String?,
    contraindications: String?,
    pregnancyFlag: Boolean,
    showPregnancy: Boolean,
    onEnergyChange: (Int) -> Unit,
    onMoodChange: (Int) -> Unit,
    onFocusChange: (Int) -> Unit,
    onStressChange: (Int) -> Unit,
    onObjectiveCommit: (String?) -> Unit,
    onContraindicationsCommit: (String?) -> Unit,
    onPregnancyChange: (Boolean) -> Unit,
    neonCyan: Color,
    neonMagenta: Color,
    modifier: Modifier = Modifier
) {
    val theme = LocalNeonTheme.current
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Energy, mood, focus, and stress are self-reported.",
            color = theme.ink,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace
        )
        BioSliderField("Energy", energy.toFloat(), 1f..10f, neonCyan) { onEnergyChange(it.toInt()) }
        BioSliderField("Mood", mood.toFloat(), 1f..10f, neonCyan) { onMoodChange(it.toInt()) }
        BioSliderField("Focus", focus.toFloat(), 1f..10f, neonCyan) { onFocusChange(it.toInt()) }
        BioSliderField("Stress", stress.toFloat(), 1f..10f, neonCyan) { onStressChange(it.toInt()) }
        CommitTextField(
            label = "Primary objective",
            stored = objective,
            accent = neonCyan,
            onCommit = onObjectiveCommit
        )
        CommitTextField(
            label = "Contraindications",
            stored = contraindications,
            accent = neonCyan,
            onCommit = onContraindicationsCommit
        )
        if (showPregnancy) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = pregnancyFlag,
                    onCheckedChange = onPregnancyChange,
                    colors = CheckboxDefaults.colors(checkedColor = neonMagenta)
                )
                Column {
                    Text(
                        text = "Pregnancy",
                        color = theme.ink,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Changes how scan advice is phrased. This is not a medical record.",
                        color = theme.inkMuted,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
private fun CommitTextField(
    label: String,
    stored: String?,
    accent: Color,
    onCommit: (String?) -> Unit
) {
    val theme = LocalNeonTheme.current
    var text by remember(stored) { mutableStateOf(stored.orEmpty()) }
    var hadFocus by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        label = { Text(label, color = accent, fontSize = 12.sp) },
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { state ->
                if (state.isFocused) {
                    hadFocus = true
                } else if (hadFocus) {
                    hadFocus = false
                    val next = text.ifBlank { null }
                    if (next != stored?.ifBlank { null }) onCommit(next)
                }
            },
        textStyle = androidx.compose.material3.MaterialTheme.typography.bodyMedium.copy(
            color = theme.ink,
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp
        ),
        singleLine = true
    )
}
