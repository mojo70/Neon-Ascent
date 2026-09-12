package com.neon.ascent.feature.biohacking.ui.sheet

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neon.ascent.core.common.LocalNeonTheme
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class BodyLogInputState(
    val date: LocalDate = LocalDate.now(),
    val time: LocalTime = LocalTime.now(),
    val weightKg: String = "",
    val conditionTag: String = "AM_FASTED", // AM_FASTED, AM, PM, POST_TRAIN, UNSPECIFIED
    val bodyFatPct: String = "",
    val bfMethod: String = "CALIPER", // DEXA, BIA_SCALE, CALIPER, NAVY_TAPE, PHOTO_EST, OTHER
    val tapeNeckCm: String = "",
    val tapeChestCm: String = "",
    val tapeWaistNavelCm: String = "",
    val tapeHipsCm: String = "",
    val tapeBicepCm: String = "",
    val tapeThighCm: String = "",
    val tapeCalfCm: String = "",
    val tapeForearmCm: String = "",
    val tapeSide: String = "R", // R, L
    val bpSysMmHg: String = "",
    val bpDiaMmHg: String = "",
    val bpPosition: String = "SITTING", // SITTING, STANDING, LYING
    val bpArm: String = "L", // L, R, UNSPECIFIED
    val note: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BodyLogSheet(
    onDismiss: () -> Unit,
    onSave: (BodyLogInputState) -> Unit,
    measurementUnit: String = "Metric",
    showShareToHcFooter: Boolean = false,
    onRequestHcBodyPerms: (() -> Unit)? = null
) {
    val theme = LocalNeonTheme.current
    val cyan = Color(0xFF00F5FF)
    val magenta = Color(0xFFFF0088)
    var state by remember { mutableStateOf(BodyLogInputState()) }

    val isImperial = measurementUnit.equals("Imperial", ignoreCase = true)

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
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "BODY_LOG // BIO_TELEMETRY",
                color = cyan,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "Room-first diary. Scale, tape, and cuff data.",
                color = theme.inkMuted,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(16.dp))

            // DATE & TIME
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BodyInputField(
                    label = "DATE (YYYY-MM-DD)",
                    value = state.date.format(DateTimeFormatter.ISO_LOCAL_DATE),
                    onValueChange = {
                        try {
                            state = state.copy(date = LocalDate.parse(it))
                        } catch (_: Exception) {}
                    },
                    modifier = Modifier.weight(1f),
                    color = cyan
                )
                BodyInputField(
                    label = "TIME (HH:MM)",
                    value = state.time.format(DateTimeFormatter.ofPattern("HH:mm")),
                    onValueChange = {
                        try {
                            state = state.copy(time = LocalTime.parse(it))
                        } catch (_: Exception) {}
                    },
                    modifier = Modifier.weight(1f),
                    color = cyan
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            // WEIGHT + CONDITION
            Text(if (isImperial) "WEIGHT (LBS)" else "WEIGHT (KG)", color = cyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            BodyInputField(
                label = if (isImperial) "WEIGHT (LBS)" else "WEIGHT (KG)",
                value = state.weightKg,
                onValueChange = { state = state.copy(weightKg = it) },
                keyboardType = KeyboardType.Decimal,
                color = cyan
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("AM_FASTED", "AM", "PM", "POST_TRAIN").forEach { tag ->
                    ChoiceChip(
                        label = tag,
                        selected = state.conditionTag == tag,
                        color = cyan,
                        onClick = { state = state.copy(conditionTag = tag) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // BODY FAT % + METHOD
            Text("BODY_FAT_%", color = magenta, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            BodyInputField(
                label = "BF %",
                value = state.bodyFatPct,
                onValueChange = { state = state.copy(bodyFatPct = it) },
                keyboardType = KeyboardType.Decimal,
                color = magenta
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("DEXA", "BIA_SCALE", "CALIPER", "NAVY_TAPE", "PHOTO_EST").forEach { method ->
                    ChoiceChip(
                        label = method,
                        selected = state.bfMethod == method,
                        color = magenta,
                        onClick = { state = state.copy(bfMethod = method) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // BLOOD PRESSURE (SYS / DIA + POSITION + ARM)
            Text("BLOOD_PRESSURE", color = cyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BodyInputField(
                    label = "SYS (mmHg)",
                    value = state.bpSysMmHg,
                    onValueChange = { state = state.copy(bpSysMmHg = it) },
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f),
                    color = cyan
                )
                BodyInputField(
                    label = "DIA (mmHg)",
                    value = state.bpDiaMmHg,
                    onValueChange = { state = state.copy(bpDiaMmHg = it) },
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f),
                    color = cyan
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("SITTING", "STANDING", "LYING").forEach { pos ->
                    ChoiceChip(
                        label = pos,
                        selected = state.bpPosition == pos,
                        color = cyan,
                        onClick = { state = state.copy(bpPosition = pos) }
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                listOf("L", "R").forEach { arm ->
                    ChoiceChip(
                        label = "ARM: $arm",
                        selected = state.bpArm == arm,
                        color = cyan,
                        onClick = { state = state.copy(bpArm = arm) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // TAPE SITES (Short list: Neck, Chest, Waist, Hips, Bicep, Thigh, Calf, Forearm)
            Text("TAPE (CM)", color = cyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BodyInputField(
                    label = "WAIST (NAVEL)",
                    value = state.tapeWaistNavelCm,
                    onValueChange = { state = state.copy(tapeWaistNavelCm = it) },
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.weight(1f),
                    color = cyan
                )
                BodyInputField(
                    label = "CHEST",
                    value = state.tapeChestCm,
                    onValueChange = { state = state.copy(tapeChestCm = it) },
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.weight(1f),
                    color = cyan
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BodyInputField(
                    label = "NECK",
                    value = state.tapeNeckCm,
                    onValueChange = { state = state.copy(tapeNeckCm = it) },
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.weight(1f),
                    color = cyan
                )
                BodyInputField(
                    label = "HIPS",
                    value = state.tapeHipsCm,
                    onValueChange = { state = state.copy(tapeHipsCm = it) },
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.weight(1f),
                    color = cyan
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BodyInputField(
                    label = "BICEP",
                    value = state.tapeBicepCm,
                    onValueChange = { state = state.copy(tapeBicepCm = it) },
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.weight(1f),
                    color = cyan
                )
                BodyInputField(
                    label = "THIGH",
                    value = state.tapeThighCm,
                    onValueChange = { state = state.copy(tapeThighCm = it) },
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.weight(1f),
                    color = cyan
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BodyInputField(
                    label = "CALF",
                    value = state.tapeCalfCm,
                    onValueChange = { state = state.copy(tapeCalfCm = it) },
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.weight(1f),
                    color = cyan
                )
                BodyInputField(
                    label = "FOREARM",
                    value = state.tapeForearmCm,
                    onValueChange = { state = state.copy(tapeForearmCm = it) },
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.weight(1f),
                    color = cyan
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            // NOTE
            BodyInputField(
                label = "NOTE (OPTIONAL)",
                value = state.note,
                onValueChange = { state = state.copy(note = it) },
                color = theme.inkMuted
            )
            Spacer(modifier = Modifier.height(20.dp))

            // ACTION BUTTON
            Button(
                onClick = { onSave(state) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = cyan),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = "SAVE_BODY_LOG",
                    color = Color.Black,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp
                )
            }

            // OPTIONAL SHARE_TO_HC FOOTER
            if (showShareToHcFooter && onRequestHcBodyPerms != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onRequestHcBodyPerms() }
                        .border(1.dp, cyan.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "SHARE_TO_HC // GRANT WRITE ACCESS",
                        color = cyan,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun BodyInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    color: Color
) {
    val theme = LocalNeonTheme.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = {
            Text(label, fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = color.copy(alpha = 0.8f))
        },
        textStyle = LocalTextStyle.current.copy(
            color = theme.ink,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp
        ),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = color,
            unfocusedBorderColor = color.copy(alpha = 0.3f),
            cursorColor = color
        ),
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
private fun ChoiceChip(
    label: String,
    selected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    val theme = LocalNeonTheme.current
    Box(
        modifier = Modifier
            .clickable { onClick() }
            .border(
                width = 1.dp,
                color = if (selected) color else color.copy(alpha = 0.25f),
                shape = RoundedCornerShape(2.dp)
            )
            .background(if (selected) color.copy(alpha = 0.2f) else Color.Transparent)
            .padding(horizontal = 8.dp, vertical = 5.dp)
    ) {
        Text(
            text = label,
            color = if (selected) color else theme.inkMuted,
            fontSize = 9.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            fontFamily = FontFamily.Monospace
        )
    }
}
