package com.neon.ascent.feature.workout.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neon.ascent.core.common.LocalNeonTheme
import com.neon.ascent.core.common.VisualMode
import com.neon.ascent.core.domain.workout.models.SetLog
import com.neon.ascent.core.domain.workout.models.SetType

@Composable
fun ClusterSetRow(
    sets: List<SetLog>,
    previousSets: List<SetLog>,
    previousWeight: Float? = null,
    zoomLevel: Float = 1.0f,
    onUpdateWeight: (Float) -> Unit,
    onUpdateGoal: (String) -> Unit,
    onClick: () -> Unit,
    onSetLabelClick: (() -> Unit)? = null
) {
    val theme = LocalNeonTheme.current
    val totalReps = sets.filter { it.isCompleted && it.type == SetType.REST_PAUSE }.sumOf { it.reps }
    val isCompleted = sets.filter { it.type == SetType.REST_PAUSE }.all { it.isCompleted } && sets.isNotEmpty()
    val weight = sets.firstOrNull()?.weight ?: 0f
    val goalReps = sets.firstOrNull()?.goalReps ?: ""
    val prevTotalReps = previousSets.sumOf { it.reps }
    val weightPlaceholder = previousWeight?.let { if (it % 1 == 0f) it.toInt().toString() else it.toString() } ?: "0"

    val rowGreenHighlight = if (theme.mode == VisualMode.STEVE) {
        Color(0xFF4CD964).copy(alpha = 0.22f)
    } else {
        Color(0xFF4CD964).copy(alpha = 0.15f)
    }
    val defaultBackgroundColor = if (theme.mode == VisualMode.STEVE) theme.surfaceRaised.copy(alpha = 0.5f) else Color(0xFF00FFAA).copy(alpha = 0.05f)
    val rowBackgroundColor = if (isCompleted) rowGreenHighlight else defaultBackgroundColor

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(rowBackgroundColor, RoundedCornerShape(4.dp))
            .padding(vertical = 4.dp, horizontal = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Label (Clickable area)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSetLabelClick?.invoke() ?: onClick() }
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    "RP",
                    color = if (theme.mode == VisualMode.STEVE) theme.ink else Color(0xFF00FFAA),
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp
                )
            }

            // Goal Input (Standard)
            if (zoomLevel < 1.5f) {
                EditableValueBox(
                    value = goalReps,
                    onValueChange = { onUpdateGoal(it) },
                    modifier = Modifier.weight(1.5f),
                    keyboardType = KeyboardType.Text
                )
            }

            // Previous (Standard area)
            if (zoomLevel < 1.5f) {
                Box(
                    modifier = Modifier
                        .weight(2f)
                        .clickable { onClick() }
                        .padding(vertical = 4.dp)
                ) {
                    Column {
                        Text(
                            if (prevTotalReps > 0) "${prevTotalReps} total" else "-",
                            color = theme.inkMuted,
                            fontSize = 12.sp
                        )
                        
                        val partials = sets.find { it.type == SetType.PARTIAL }
                        val stretch = sets.find { it.type == SetType.STRETCH }
                        
                        if (partials != null || stretch != null) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                                if (partials != null) {
                                    Text("P:${partials.reps}", color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    if (stretch != null) Spacer(Modifier.width(6.dp))
                                }
                                if (stretch != null) {
                                    Text("S:${stretch.reps}s", color = Color(0xFFFF006E), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Weight Input
            EditableValueBox(
                value = if (weight % 1 == 0f) weight.toInt().toString() else weight.toString(),
                onValueChange = { it.toFloatOrNull()?.let { w -> onUpdateWeight(w) } },
                placeholder = weightPlaceholder,
                modifier = Modifier.weight(1.5f),
                keyboardType = KeyboardType.Decimal
            )

            // Total Reps (Clickable area)
            Box(
                modifier = Modifier
                    .weight(1.5f)
                    .clickable { onClick() }
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (totalReps > 0) totalReps.toString() else "-",
                    color = Color(0xFF00FFAA),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }

            // Completion Check (Clickable area)
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(
                        if (isCompleted) Color(0xFF4CD964) else theme.inkMuted.copy(alpha = 0.5f),
                        RoundedCornerShape(4.dp)
                    )
                    .clickable { onClick() },
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(16.dp))
                }
            }
        }

        // Sub-row for Stacked Data (Zoom >= 1.5f)
        if (zoomLevel >= 1.5f) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 32.dp, top = 2.dp, end = 32.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "GOAL: $goalReps", 
                    color = theme.inkMuted, 
                    fontSize = 10.sp, 
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                
                Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.End) {
                    Text(
                        "PREV: $prevTotalReps", 
                        color = theme.inkMuted, 
                        fontSize = 10.sp, 
                        fontWeight = FontWeight.Bold
                    )
                    
                    val partials = sets.find { it.type == SetType.PARTIAL }
                    val stretch = sets.find { it.type == SetType.STRETCH }
                    if (partials != null || stretch != null) {
                        Spacer(Modifier.width(8.dp))
                        if (partials != null) {
                            Text("P:${partials.reps}", color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            if (stretch != null) Spacer(Modifier.width(4.dp))
                        }
                        if (stretch != null) {
                            Text("S:${stretch.reps}s", color = Color(0xFFFF006E), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
