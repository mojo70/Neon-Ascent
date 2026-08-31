package com.neon.ascent.feature.workout.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neon.ascent.core.common.*
import com.neon.ascent.core.domain.workout.models.SetLog
import com.neon.ascent.core.domain.workout.models.SetType
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

@Composable
fun SetLogRow(
    setNumber: Int,
    set: SetLog,
    showGoal: Boolean,
    previousData: String,
    previousWeight: Float? = null,
    zoomLevel: Float = 1.0f,
    prescribedWeight: Float? = null,
    prescribedReps: Int? = null,
    percentOfMax: Float? = null,
    isAmrap: Boolean = false,
    onUpdateWeight: (Float) -> Unit,
    onUpdateReps: (Int) -> Unit,
    onUpdateGoal: (String) -> Unit,
    onCompleteToggle: () -> Unit,
    onSetLabelClick: () -> Unit
) {
    val theme = LocalNeonTheme.current
    val haptic = LocalHapticFeedback.current
    val backgroundColor = if (setNumber % 2 == 0) Color.Transparent else theme.surfaceRaised.copy(alpha = 0.3f)
    val weightPlaceholder = prescribedWeight?.let { if (it % 1 == 0f) it.toInt().toString() else it.toString() } 
        ?: previousWeight?.let { if (it % 1 == 0f) it.toInt().toString() else it.toString() } ?: "0"
    
    val showPrescribed = (prescribedWeight != null || prescribedReps != null) && zoomLevel < 1.5f
    
    val setLabel = when (set.type) {
        SetType.NORMAL -> "$setNumber"
        SetType.WARMUP -> "W"
        SetType.DROP -> "D"
        SetType.FAILURE -> "F"
        SetType.REST_PAUSE -> if (set.clusterMiniSetIndex != null) "RP ${set.clusterMiniSetIndex}" else "RP"
        SetType.WIDOWMAKER -> "WM"
        SetType.POWER -> "P"
        SetType.GS -> "GS"
        SetType.PARTIAL -> "PAR"
        SetType.STRETCH -> "STR"
        SetType.MAX_EFFORT -> "ME"
    }
    
    val labelColor = if (theme.mode == VisualMode.STEVE) {
        theme.ink
    } else {
        when (set.type) {
            SetType.WARMUP -> Color(0xFFFFA500)
            SetType.DROP -> Color(0xFF00CCFF)
            SetType.FAILURE -> Color(0xFFFF4444)
            SetType.REST_PAUSE -> Color(0xFF00FFAA)
            SetType.WIDOWMAKER -> Color(0xFFFF00FF)
            SetType.POWER -> Color(0xFFFFD700)
            SetType.GS -> Color(0xFF00CCFF)
            SetType.PARTIAL -> theme.accent
            SetType.STRETCH -> Color(0xFFFF006E)
            SetType.MAX_EFFORT -> Color(0xFFFF0000)
            else -> theme.ink
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Set Label
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSetLabelClick() },
                contentAlignment = Alignment.CenterStart
            ) {
                Text(setLabel, color = labelColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            // Prescribed Pill / Goal Input
            if (showPrescribed) {
                Column(modifier = Modifier.weight(1.5f), horizontalAlignment = Alignment.CenterHorizontally) {
                    val repsText = if (isAmrap) "${prescribedReps ?: ""} +" else (prescribedReps ?: "").toString()
                    Text("$repsText @ ${if (prescribedWeight!! % 1 == 0f) prescribedWeight.toInt() else prescribedWeight}", 
                         color = theme.accent, fontSize = 10.sp, fontWeight = FontWeight.Black)
                    
                    val subText = if (percentOfMax != null) "${(percentOfMax * 100).toInt()}% TM" else "TARGET"
                    Text(subText, color = theme.inkMuted, fontSize = 8.sp)
                }
            } else if (showGoal && zoomLevel < 1.5f) {
                val isGoalMandatory = set.type == SetType.WIDOWMAKER
                EditableValueBox(
                    value = set.goalReps ?: "",
                    onValueChange = { onUpdateGoal(it) },
                    modifier = Modifier.weight(1.5f),
                    keyboardType = KeyboardType.Text,
                    enabled = !isGoalMandatory
                )
            }

            // Previous Data (Standard)
            if (zoomLevel < 1.5f) {
                Text(previousData, color = theme.inkMuted, fontSize = 14.sp, modifier = Modifier.weight(2f))
            }
            
            // Weight Input
            EditableValueBox(
                value = if (set.weight % 1 == 0f) set.weight.toInt().toString() else set.weight.toString(),
                onValueChange = { it.toFloatOrNull()?.let { w -> onUpdateWeight(w) } },
                placeholder = weightPlaceholder,
                modifier = Modifier.weight(1.5f),
                keyboardType = KeyboardType.Decimal
            )
            
            // Reps Input
            EditableValueBox(
                value = if (set.type == SetType.STRETCH) "${set.reps}s" else set.reps.toString(),
                onValueChange = { 
                    val clean = it.replace("s", "")
                    clean.toIntOrNull()?.let { r -> onUpdateReps(r) } 
                },
                modifier = Modifier.weight(1.5f),
                keyboardType = KeyboardType.Number
            )
            
            // Completion Check
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(
                        if (set.isCompleted) Color(0xFF4CD964) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f), 
                        RoundedCornerShape(4.dp)
                    )
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onCompleteToggle()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Check, 
                    contentDescription = null, 
                    tint = if (set.isCompleted) Color.Black else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Sub-row for Stacked Data (Zoom >= 1.5f)
        if (zoomLevel >= 1.5f) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 32.dp, top = 2.dp, end = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (showGoal || prescribedReps != null) {
                    val repsText = if (isAmrap) "${prescribedReps ?: ""} +" else (prescribedReps ?: "").toString()
                    val goalText = if (prescribedReps != null) "$repsText @ ${prescribedWeight}" else (set.goalReps ?: "-")
                    Text(
                        "GOAL: $goalText", 
                        color = if (prescribedReps != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), 
                        fontSize = 10.sp, 
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                }
                Text(
                    "PREV: $previousData", 
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), 
                    fontSize = 10.sp, 
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
