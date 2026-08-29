package com.neon.ascent.feature.workout.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neon.ascent.core.domain.workout.models.*
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AugmentActivationDialog(
    augment: WorkoutAugment,
    existingActivation: AugmentActivation? = null,
    userProfile: UserWorkoutProfile?,
    onActivate: (AugmentActivation) -> Unit,
    onAdHoc: (WorkoutAugment) -> Unit,
    onDismiss: () -> Unit
) {
    var runMode by remember { 
        mutableStateOf(
            when (existingActivation?.mode) {
                AugmentRunMode.AD_HOC -> "JUST_TODAY"
                AugmentRunMode.ATTACHED_ONGOING, AugmentRunMode.ATTACHED_WINDOW -> "BOLT_ON"
                else -> "SOLO"
            }
        ) 
    }
    
    // Window Duration Option: "30_DAYS", "ONGOING", "CUSTOM"
    var durationOption by remember { 
        mutableStateOf(
            if (existingActivation == null) "30_DAYS"
            else if (existingActivation.windowEnd == null) "ONGOING"
            else "30_DAYS"
        ) 
    }
    var customDays by remember { mutableStateOf(30) }

    var loggingStyle by remember { 
        mutableStateOf(existingActivation?.loggingStyle ?: 
            if (augment.id == "augment_gorilla_arms") AugmentLoggingStyle.CYBER_CRAPP else AugmentLoggingStyle.INHERIT) 
    }
    
    val initialScheduledDays = existingActivation?.scheduledDays ?: augment.scheduledDays.ifEmpty {
        if (augment.id == "augment_gorilla_arms") {
            listOf(1, 2, 3, 4, 5).map { ScheduledDay(it, "06:30") }
        } else emptyList()
    }
    var scheduledDays by remember { mutableStateOf(initialScheduledDays) }
    
    var hostProtocolFilter by remember { mutableStateOf(existingActivation?.hostProtocolFilter) }
    var dayTypeFilter by remember { mutableStateOf(existingActivation?.dayTypeFilter) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0D0D0D),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.Gray) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 40.dp)
        ) {
            Text(
                augment.name.uppercase(),
                color = Color(0xFF00FF9C),
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
            Text(
                "SUB-PROTOCOL CONFIGURATION",
                color = Color.Gray,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Text("HOW DOES THIS RUN?", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
            Spacer(modifier = Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ModeSelectionItem(
                    title = "SOLO",
                    description = "Own session. Own alarms. Main protocol untouched.",
                    isSelected = runMode == "SOLO",
                    onClick = { runMode = "SOLO" }
                )
                ModeSelectionItem(
                    title = "BOLT ON",
                    description = "Adds to the main workout when you start OPS.",
                    isSelected = runMode == "BOLT_ON",
                    onClick = { runMode = "BOLT_ON" }
                )
                ModeSelectionItem(
                    title = "JUST TODAY",
                    description = "One-time ad hoc execution.",
                    isSelected = runMode == "JUST_TODAY",
                    onClick = { runMode = "JUST_TODAY" }
                )
            }

            if (runMode != "JUST_TODAY") {
                Spacer(modifier = Modifier.height(24.dp))
                Text("FOCUS WINDOW. DEFAULT 30 DAYS. THEN REEVALUATE.", color = Color(0xFF00FF9C), fontSize = 11.sp, fontWeight = FontWeight.Black)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LoggingStyleChip("30 DAYS", durationOption == "30_DAYS") { durationOption = "30_DAYS" }
                    LoggingStyleChip("ONGOING", durationOption == "ONGOING") { durationOption = "ONGOING" }
                    LoggingStyleChip("CUSTOM N", durationOption == "CUSTOM") { durationOption = "CUSTOM" }
                }

                if (durationOption == "30_DAYS" || durationOption == "CUSTOM") {
                    val days = if (durationOption == "30_DAYS") 30 else customDays
                    if (durationOption == "CUSTOM") {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Slider(
                                value = customDays.toFloat(),
                                onValueChange = { customDays = it.toInt() },
                                valueRange = 1f..90f,
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(thumbColor = Color(0xFF00FF9C), activeTrackColor = Color(0xFF00FF9C))
                            )
                            Text("$customDays DAYS", color = Color.White, fontSize = 14.sp, modifier = Modifier.padding(start = 12.dp))
                        }
                    }
                    val endDate = LocalDate.now().plusDays(days.toLong())
                    Text("ENDS ${endDate.format(DateTimeFormatter.ofPattern("dd MMM"))}", color = Color.Gray, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            when (runMode) {
                "SOLO" -> {
                    Text("OWN CALENDAR", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
                    Spacer(modifier = Modifier.height(12.dp))
                    DaySelectionRow(
                        selectedDays = scheduledDays,
                        onDaysChanged = { scheduledDays = it }
                    )
                }
                "BOLT_ON" -> {
                    if (userProfile?.activeProtocol != null) {
                        Text("ATTACH TO DAYS", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
                        Spacer(modifier = Modifier.height(12.dp))
                        ProtocolDayFilterRow(
                            protocol = userProfile.activeProtocol!!,
                            selectedDayType = dayTypeFilter,
                            onDayTypeSelected = { dayTypeFilter = it }
                        )
                    }
                }
                "JUST_TODAY" -> {
                    Text("No schedule required for ad hoc runs.", color = Color.Gray, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("LOGGING STYLE", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LoggingStyleChip("INHERIT", loggingStyle == AugmentLoggingStyle.INHERIT) { loggingStyle = AugmentLoggingStyle.INHERIT }
                LoggingStyleChip("CLUSTER", loggingStyle == AugmentLoggingStyle.CYBER_CRAPP) { loggingStyle = AugmentLoggingStyle.CYBER_CRAPP }
                LoggingStyleChip("FREE", loggingStyle == AugmentLoggingStyle.GENERAL) { loggingStyle = AugmentLoggingStyle.GENERAL }
            }

            Spacer(modifier = Modifier.height(32.dp))

            val isSoloWithTime = runMode == "SOLO" && scheduledDays.isNotEmpty()

            if (isSoloWithTime) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            val finalDays = if (durationOption == "30_DAYS") 30 else if (durationOption == "CUSTOM") customDays else null
                            val activation = AugmentActivation(
                                id = existingActivation?.id ?: UUID.randomUUID().toString(),
                                augmentId = augment.id,
                                mode = AugmentRunMode.INDEPENDENT,
                                loggingStyle = loggingStyle,
                                scheduledDays = scheduledDays,
                                windowStart = if (finalDays != null) Instant.now() else null,
                                windowEnd = if (finalDays != null) Instant.now().plusSeconds(finalDays * 24L * 3600L) else null,
                                status = AugmentActivationStatus.ACTIVE
                            )
                            onActivate(activation)
                            onAdHoc(augment) // Start session immediately while keeping window + reminders
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f).height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF9C)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("START NOW", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            val finalDays = if (durationOption == "30_DAYS") 30 else if (durationOption == "CUSTOM") customDays else null
                            val activation = AugmentActivation(
                                id = existingActivation?.id ?: UUID.randomUUID().toString(),
                                augmentId = augment.id,
                                mode = AugmentRunMode.INDEPENDENT,
                                loggingStyle = loggingStyle,
                                scheduledDays = scheduledDays,
                                windowStart = if (finalDays != null) Instant.now() else null,
                                windowEnd = if (finalDays != null) Instant.now().plusSeconds(finalDays * 24L * 3600L) else null,
                                status = AugmentActivationStatus.ACTIVE
                            )
                            onActivate(activation)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f).height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1C1C1E)),
                        border = BorderStroke(1.dp, Color(0xFF00FF9C)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("WAIT FOR ALARM", color = Color(0xFF00FF9C), fontWeight = FontWeight.Black, fontSize = 13.sp)
                    }
                }
            } else {
                Button(
                    onClick = {
                        if (runMode == "JUST_TODAY") {
                            onAdHoc(augment)
                            onDismiss()
                        } else {
                            val finalMode = when (runMode) {
                                "SOLO" -> AugmentRunMode.INDEPENDENT
                                "BOLT_ON" -> if (durationOption == "ONGOING") AugmentRunMode.ATTACHED_ONGOING else AugmentRunMode.ATTACHED_WINDOW
                                else -> AugmentRunMode.INDEPENDENT
                            }
                            val finalDays = if (durationOption == "30_DAYS") 30 else if (durationOption == "CUSTOM") customDays else null

                            val activation = AugmentActivation(
                                id = existingActivation?.id ?: UUID.randomUUID().toString(),
                                augmentId = augment.id,
                                mode = finalMode,
                                loggingStyle = loggingStyle,
                                scheduledDays = if (finalMode == AugmentRunMode.INDEPENDENT) scheduledDays else emptyList(),
                                windowStart = if (finalDays != null) Instant.now() else null,
                                windowEnd = if (finalDays != null) Instant.now().plusSeconds(finalDays * 24L * 3600L) else null,
                                hostProtocolFilter = if (finalMode != AugmentRunMode.INDEPENDENT) userProfile?.activeProtocol else null,
                                dayTypeFilter = dayTypeFilter,
                                status = AugmentActivationStatus.ACTIVE
                            )
                            onActivate(activation)
                            onDismiss()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF9C)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        if (runMode == "JUST_TODAY") "START AD HOC NOW" else "ACTIVATE SUB-PROTOCOL",
                        color = Color.Black,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

@Composable
fun ModeSelectionItem(title: String, description: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        color = if (isSelected) Color(0xFF00FF9C).copy(alpha = 0.1f) else Color(0xFF1C1C1E),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, if (isSelected) Color(0xFF00FF9C) else Color.Transparent)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = if (isSelected) Color(0xFF00FF9C) else Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
                Text(description, color = Color.Gray, fontSize = 11.sp)
            }
            if (isSelected) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF00FF9C))
            }
        }
    }
}

@Composable
fun LoggingStyleChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable { onClick() },
        color = if (isSelected) Color(0xFF00FF9C) else Color(0xFF2C2C2E),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            color = if (isSelected) Color.Black else Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DaySelectionRow(selectedDays: List<ScheduledDay>, onDaysChanged: (List<ScheduledDay>) -> Unit) {
    val days = listOf("M", "T", "W", "T", "F", "S", "S")
    var showTimePickerForDay by remember { mutableStateOf<Int?>(null) }
    
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        days.forEachIndexed { index, label ->
            val dayId = index + 1
            val scheduled = selectedDays.find { it.dayOfWeek == dayId }
            val isSelected = scheduled != null
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    modifier = Modifier
                        .size(40.dp)
                        .clickable {
                            if (isSelected) {
                                onDaysChanged(selectedDays.filter { it.dayOfWeek != dayId })
                            } else {
                                val baseTime = selectedDays.firstOrNull()?.time ?: "06:30"
                                onDaysChanged(selectedDays + ScheduledDay(dayId, baseTime))
                            }
                        },
                    color = if (isSelected) Color(0xFF00FF9C).copy(alpha = 0.2f) else Color(0xFF1C1C1E),
                    shape = CircleShape,
                    border = BorderStroke(1.dp, if (isSelected) Color(0xFF00FF9C) else Color.Gray)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(label, color = if (isSelected) Color(0xFF00FF9C) else Color.Gray, fontSize = 14.sp)
                    }
                }
                if (scheduled != null) {
                    Text(
                        scheduled.time,
                        color = Color(0xFF00FF9C),
                        fontSize = 9.sp,
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .clickable { showTimePickerForDay = dayId }
                    )
                }
            }
        }
    }

    if (showTimePickerForDay != null) {
        val initialTime = selectedDays.find { it.dayOfWeek == showTimePickerForDay }?.time ?: "06:30"
        val timePickerState = rememberTimePickerState(
            initialHour = initialTime.split(":")[0].toInt(),
            initialMinute = initialTime.split(":")[1].toInt()
        )
        AlertDialog(
            onDismissRequest = { showTimePickerForDay = null },
            confirmButton = {
                TextButton(onClick = {
                    val newTime = "%02d:%02d".format(timePickerState.hour, timePickerState.minute)
                    onDaysChanged(selectedDays.map { it.copy(time = newTime) })
                    showTimePickerForDay = null
                }) { Text("OK", color = Color(0xFF00FF9C)) }
            },
            title = { Text("SET TIME", color = Color.White) },
            text = { TimePicker(state = timePickerState) },
            containerColor = Color(0xFF1C1C1E)
        )
    }
}

@Composable
fun ProtocolDayFilterRow(protocol: WorkoutProtocol, selectedDayType: ProtocolDayType?, onDayTypeSelected: (ProtocolDayType?) -> Unit) {
    val dayTypes = when (protocol) {
        WorkoutProtocol.CYBER_CRAPP -> listOf(ProtocolDayType.CC_A, ProtocolDayType.CC_B, ProtocolDayType.CC_C)
        WorkoutProtocol.STARTING_STRENGTH -> listOf(ProtocolDayType.SS_A, ProtocolDayType.SS_B)
        WorkoutProtocol.HST -> listOf(ProtocolDayType.HST_15, ProtocolDayType.HST_10, ProtocolDayType.HST_5, ProtocolDayType.HST_NEG)
        WorkoutProtocol.FIVE_THREE_ONE -> listOf(ProtocolDayType.FTV_W1, ProtocolDayType.FTV_W2, ProtocolDayType.FTV_W3)
        WorkoutProtocol.WESTSIDE -> listOf(ProtocolDayType.WS_ME_LOWER, ProtocolDayType.WS_ME_UPPER, ProtocolDayType.WS_DE_LOWER, ProtocolDayType.WS_DE_UPPER)
        else -> emptyList()
    }

    Column {
        FilterChip(
            selected = selectedDayType == null,
            onClick = { onDayTypeSelected(null) },
            label = { Text("ALL MAIN DAYS") },
            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFF00FF9C))
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(dayTypes) { type ->
                FilterChip(
                    selected = selectedDayType == type,
                    onClick = { onDayTypeSelected(type) },
                    label = { Text(type.name.replace("_", " ")) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFF00FF9C))
                )
            }
        }
    }
}
