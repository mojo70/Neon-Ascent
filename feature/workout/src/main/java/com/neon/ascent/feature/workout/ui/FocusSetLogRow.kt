package com.neon.ascent.feature.workout.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
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
import com.neon.ascent.core.common.*
import com.neon.ascent.core.domain.workout.models.SetLog
import com.neon.ascent.core.domain.workout.models.SetType
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FocusSetLogRow(
    setNumber: Int,
    set: SetLog,
    previousData: String,
    goalData: String? = null,
    previousWeight: Float? = null,
    previousReps: Int? = null,
    onUpdateWeight: (Float) -> Unit,
    onUpdateReps: (Int) -> Unit,
    onCompleteToggle: () -> Unit,
    onSetLabelClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = LocalNeonTheme.current
    val haptic = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val coroutineScope = rememberCoroutineScope()

    val primaryColor = MaterialTheme.colorScheme.primary
    val isCompleted = set.isCompleted

    val setLabel = when (set.type) {
        SetType.NORMAL -> "SET $setNumber"
        SetType.WARMUP -> "WARMUP $setNumber"
        SetType.DROP -> "DROP $setNumber"
        SetType.FAILURE -> "FAILURE $setNumber"
        SetType.REST_PAUSE -> "REST-PAUSE $setNumber"
        else -> "${set.type.name} $setNumber"
    }

    val containerBg = if (isCompleted) {
        if (theme.mode == VisualMode.STEVE) Color(0xFF1B5E20).copy(alpha = 0.25f)
        else if (theme.mode == VisualMode.PILOT) theme.accent.copy(alpha = 0.25f)
        else Color(0xFF00FF9C).copy(alpha = 0.18f)
    } else theme.surfaceRaised

    val borderStroke = if (isCompleted) {
        BorderStroke(2.dp, if (theme.mode == VisualMode.STEVE) Color(0xFF4CD964) else if (theme.mode == VisualMode.PILOT) theme.accent else Color(0xFF00FF9C))
    } else {
        BorderStroke(1.dp, theme.hairline)
    }

    // Weight & Reps Placeholders
    val weightPlaceholder = previousWeight?.let { if (it % 1 == 0f) it.toInt().toString() else it.toString() } ?: "0"
    val repsPlaceholder = previousReps?.takeIf { it > 0 }?.toString() ?: "0"

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .bringIntoViewRequester(bringIntoViewRequester),
        color = containerBg,
        shape = RoundedCornerShape(12.dp),
        border = borderStroke
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Header Row: Set Tag & Goal / Previous Reference
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clickable { onSetLabelClick() }
                        .background(
                            if (set.type == SetType.WARMUP) Color(0xFFFFA500).copy(alpha = 0.2f) else primaryColor.copy(alpha = 0.2f),
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = setLabel,
                        color = if (set.type == SetType.WARMUP) Color(0xFFFFA500) else primaryColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!goalData.isNullOrEmpty()) {
                        Text(
                            text = "GOAL: $goalData",
                            color = primaryColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Text(
                        text = "PREV: $previousData",
                        color = theme.inkMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Main Input & Complete Button Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Weight Large Input Box
                FocusValueBox(
                    value = if (set.weight % 1 == 0f) set.weight.toInt().toString() else set.weight.toString(),
                    label = "LBS",
                    placeholder = weightPlaceholder,
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next,
                    onValueChange = { it.toFloatOrNull()?.let { w -> onUpdateWeight(w) } },
                    onImeAction = { focusManager.moveFocus(FocusDirection.Next) },
                    onFocusGained = {
                        coroutineScope.launch { bringIntoViewRequester.bringIntoView() }
                    },
                    modifier = Modifier.weight(1f)
                )

                // Reps Large Input Box
                FocusValueBox(
                    value = set.reps.toString(),
                    label = "REPS",
                    placeholder = repsPlaceholder,
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done,
                    onValueChange = { it.toIntOrNull()?.let { r -> onUpdateReps(r) } },
                    onImeAction = {
                        focusManager.clearFocus()
                        if (!isCompleted) onCompleteToggle()
                    },
                    onFocusGained = {
                        coroutineScope.launch { bringIntoViewRequester.bringIntoView() }
                    },
                    modifier = Modifier.weight(1f)
                )

                // Giant Complete Button
                val checkBg = if (isCompleted) {
                    if (theme.mode == VisualMode.STEVE) Color(0xFF4CD964) else if (theme.mode == VisualMode.PILOT) theme.accent else Color(0xFF00FF9C)
                } else primaryColor.copy(alpha = 0.2f)

                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(checkBg, RoundedCornerShape(10.dp))
                        .border(
                            1.dp,
                            if (isCompleted) Color.Transparent else primaryColor,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onCompleteToggle()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Complete Set",
                        tint = if (isCompleted) Color.Black else primaryColor,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FocusValueBox(
    value: String,
    label: String,
    placeholder: String,
    keyboardType: KeyboardType,
    imeAction: ImeAction,
    onValueChange: (String) -> Unit,
    onImeAction: () -> Unit,
    onFocusGained: () -> Unit,
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
        // Label overlay in top-left
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
                val isValid = if (keyboardType == KeyboardType.Number) {
                    str.isEmpty() || str.all { it.isDigit() }
                } else {
                    str.isEmpty() || str.toDoubleOrNull() != null || str == "." || str == ","
                }
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
                keyboardType = keyboardType,
                autoCorrectEnabled = false,
                imeAction = imeAction
            ),
            keyboardActions = KeyboardActions(
                onNext = { onImeAction() },
                onDone = { onImeAction() }
            ),
            cursorBrush = SolidColor(primaryColor),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    val wasFocused = isFocused
                    isFocused = focusState.isFocused
                    if (!wasFocused && focusState.isFocused) {
                        // AUTO-SELECT ENTIRE TEXT ON FOCUS
                        textFieldValue = textFieldValue.copy(
                            selection = TextRange(0, textFieldValue.text.length)
                        )
                        onFocusGained()
                    }
                }
        )
    }
}
