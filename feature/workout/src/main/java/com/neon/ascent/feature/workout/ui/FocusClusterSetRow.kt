package com.neon.ascent.feature.workout.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neon.ascent.core.common.LocalNeonTheme
import com.neon.ascent.core.common.VisualMode
import com.neon.ascent.core.domain.workout.models.SetLog
import com.neon.ascent.core.domain.workout.models.SetType

@Composable
fun FocusClusterSetRow(
    sets: List<SetLog>,
    previousSets: List<SetLog>,
    previousWeight: Float? = null,
    onUpdateWeight: (Float) -> Unit,
    onClick: () -> Unit,
    onSetLabelClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val theme = LocalNeonTheme.current
    val totalReps = sets.filter { it.isCompleted && it.type == SetType.REST_PAUSE }.sumOf { it.reps }
    val isCompleted = sets.filter { it.type == SetType.REST_PAUSE }.all { it.isCompleted } && sets.isNotEmpty()
    val weight = sets.firstOrNull()?.weight ?: 0f
    val goalReps = sets.firstOrNull()?.goalReps ?: ""
    val prevTotalReps = previousSets.sumOf { it.reps }
    val weightPlaceholder = previousWeight?.let { if (it % 1 == 0f) it.toInt().toString() else it.toString() } ?: "0"

    val primaryColor = MaterialTheme.colorScheme.primary
    val rpColor = Color(0xFF00FFAA)

    val containerBg = if (isCompleted) {
        if (theme.mode == VisualMode.STEVE) Color(0xFF1B5E20).copy(alpha = 0.25f)
        else Color(0xFF00FF9C).copy(alpha = 0.18f)
    } else theme.surfaceRaised

    val borderStroke = if (isCompleted) {
        BorderStroke(2.dp, if (theme.mode == VisualMode.STEVE) Color(0xFF4CD964) else Color(0xFF00FF9C))
    } else {
        BorderStroke(1.dp, rpColor.copy(alpha = 0.4f))
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        color = containerBg,
        shape = RoundedCornerShape(12.dp),
        border = borderStroke
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clickable { onSetLabelClick?.invoke() ?: onClick() }
                        .background(rpColor.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "REST-PAUSE",
                        color = rpColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (goalReps.isNotEmpty()) {
                        Text(
                            text = "GOAL: $goalReps",
                            color = primaryColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Text(
                        text = "PREV: ${if (prevTotalReps > 0) "$prevTotalReps total" else "-"}",
                        color = theme.inkMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Main Input & Action Row (Matches FocusSetLogRow)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Weight Large Input Box
                ClusterWeightValueBox(
                    value = if (weight % 1 == 0f) weight.toInt().toString() else weight.toString(),
                    label = "LBS",
                    placeholder = weightPlaceholder,
                    onValueChange = { it.toFloatOrNull()?.let { w -> onUpdateWeight(w) } },
                    onImeAction = { onClick() },
                    modifier = Modifier.weight(1f)
                )

                // Total Reps Large Touch Box (Clicking opens ClusterLoggingDialog!)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .background(rpColor.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                        .border(1.dp, rpColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .clickable { onClick() }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "TOTAL REPS",
                        color = rpColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.align(Alignment.TopStart)
                    )

                    Text(
                        text = if (totalReps > 0) "$totalReps" else "-",
                        color = rpColor,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center
                    )
                }

                // Giant Action / Check Button (56dp x 56dp)
                val checkBg = if (isCompleted) {
                    if (theme.mode == VisualMode.STEVE) Color(0xFF4CD964) else Color(0xFF00FF9C)
                } else rpColor.copy(alpha = 0.2f)

                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(checkBg, RoundedCornerShape(10.dp))
                        .border(
                            1.dp,
                            if (isCompleted) Color.Transparent else rpColor,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { onClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Log Rest Pause",
                        tint = if (isCompleted) Color.Black else rpColor,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ClusterWeightValueBox(
    value: String,
    label: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    onImeAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = LocalNeonTheme.current
    var isFocused by remember { mutableStateOf(false) }

    val rawText = if (value == "0" || value == "0.0") "" else value
    var textFieldValue by remember { mutableStateOf(TextFieldValue(rawText)) }

    LaunchedEffect(value) {
        if (!isFocused) {
            val fresh = if (value == "0" || value == "0.0") "" else value
            textFieldValue = TextFieldValue(fresh)
        }
    }

    val primaryColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .height(56.dp)
            .background(
                if (isFocused) primaryColor.copy(alpha = 0.12f) else theme.surface,
                RoundedCornerShape(8.dp)
            )
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) primaryColor else theme.hairline,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isFocused) primaryColor else theme.inkMuted,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.align(Alignment.TopStart)
        )

        if (textFieldValue.text.isEmpty()) {
            Text(
                text = placeholder,
                color = theme.inkMuted.copy(alpha = 0.4f),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center
            )
        }

        BasicTextField(
            value = textFieldValue,
            onValueChange = { newTfv ->
                val str = newTfv.text
                val isValid = str.isEmpty() || str.toDoubleOrNull() != null || str == "." || str == ","
                if (isValid) {
                    textFieldValue = newTfv
                    onValueChange(str)
                }
            },
            textStyle = TextStyle(
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center
            ),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                autoCorrectEnabled = false,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { onImeAction() }
            ),
            cursorBrush = SolidColor(primaryColor),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    val wasFocused = isFocused
                    isFocused = focusState.isFocused
                    if (!wasFocused && focusState.isFocused) {
                        textFieldValue = textFieldValue.copy(
                            selection = TextRange(0, textFieldValue.text.length)
                        )
                    }
                }
        )
    }
}
