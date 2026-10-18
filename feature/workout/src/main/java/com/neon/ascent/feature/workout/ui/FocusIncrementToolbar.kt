package com.neon.ascent.feature.workout.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neon.ascent.core.common.LocalNeonTheme
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

@Composable
fun FocusIncrementToolbar(
    onAdjustWeight: (Float) -> Unit,
    onAdjustReps: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = LocalNeonTheme.current
    val haptic = LocalHapticFeedback.current
    val primaryColor = MaterialTheme.colorScheme.primary

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(theme.surfaceRaised, RoundedCornerShape(8.dp))
            .border(1.dp, theme.hairline, RoundedCornerShape(8.dp))
            .padding(horizontal = 6.dp, vertical = 6.dp)
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Label
        Text(
            "LB:",
            color = theme.inkMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(start = 4.dp, end = 2.dp)
        )

        // Weight Step Pills
        listOf(-10f, -5f, -2.5f, 2.5f, 5f, 10f).forEach { delta ->
            val label = if (delta > 0) "+${if (delta % 1 == 0f) delta.toInt() else delta}" else "${if (delta % 1 == 0f) delta.toInt() else delta}"
            val isPositive = delta > 0
            
            Box(
                modifier = Modifier
                    .height(36.dp)
                    .defaultMinSize(minWidth = 44.dp)
                    .background(
                        if (isPositive) primaryColor.copy(alpha = 0.15f) else Color.Red.copy(alpha = 0.15f),
                        RoundedCornerShape(6.dp)
                    )
                    .border(
                        1.dp,
                        if (isPositive) primaryColor.copy(alpha = 0.5f) else Color.Red.copy(alpha = 0.5f),
                        RoundedCornerShape(6.dp)
                    )
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onAdjustWeight(delta)
                    }
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = if (isPositive) primaryColor else Color(0xFFFF6666),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        // Reps Label
        Text(
            "REPS:",
            color = theme.inkMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(start = 4.dp, end = 2.dp)
        )

        // Rep Step Pills
        listOf(-1, 1).forEach { delta ->
            val label = if (delta > 0) "+$delta" else "$delta"
            val isPositive = delta > 0

            Box(
                modifier = Modifier
                    .height(36.dp)
                    .defaultMinSize(minWidth = 44.dp)
                    .background(
                        if (isPositive) primaryColor.copy(alpha = 0.15f) else Color.Red.copy(alpha = 0.15f),
                        RoundedCornerShape(6.dp)
                    )
                    .border(
                        1.dp,
                        if (isPositive) primaryColor.copy(alpha = 0.5f) else Color.Red.copy(alpha = 0.5f),
                        RoundedCornerShape(6.dp)
                    )
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onAdjustReps(delta)
                    }
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = if (isPositive) primaryColor else Color(0xFFFF6666),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
