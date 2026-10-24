package com.neon.ascent.feature.biohacking.ui.sheet

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neon.ascent.core.common.LocalNeonTheme
import com.neon.ascent.core.common.LocalVisualMode
import com.neon.ascent.core.common.VisualMode
import com.neon.ascent.core.domain.chronicle.DailyOriginPayload
import com.neon.ascent.feature.biohacking.BiohackingViewModel
import com.neon.ascent.ui.CyberFrame
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyOriginSheet(
    onDismiss: () -> Unit,
    onOpenChronicleCompose: (LocalDate, String?) -> Unit,
    viewModel: BiohackingViewModel,
    initialDate: LocalDate = LocalDate.now()
) {
    var selectedDate by remember { mutableStateOf(initialDate) }
    var dismissedSections by remember { mutableStateOf(setOf<String>()) }

    val payload by viewModel.observeDayPayload(selectedDate)
        .collectAsState(initial = DailyOriginPayload(selectedDate.toString()))

    val theme = LocalNeonTheme.current
    val visualMode = LocalVisualMode.current
    val isCyber = visualMode == VisualMode.CYBER

    val primaryColor = if (isCyber) Color(0xFF00F5FF) else theme.ink
    val accentColor = if (isCyber) Color(0xFFFF0088) else theme.secondary
    val bgColor = if (isCyber) Color(0xFF0A0A0A) else theme.canvas
    val cardBgColor = if (isCyber) Color(0xFF121212) else theme.overlay

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = bgColor,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isCyber) "DAILY_ORIGIN // DAY_CARD" else "Daily Origin",
                        color = primaryColor,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default
                    )
                    Text(
                        text = if (isCyber) "READ_ONLY TELEMETRY AGGREGATION" else "Daily telemetry summary & journal",
                        color = primaryColor.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = primaryColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Date Selector Bar
            DateSelectorBar(
                selectedDate = selectedDate,
                onDateSelected = { selectedDate = it },
                primaryColor = primaryColor,
                isCyber = isCyber
            )

            if (dismissedSections.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                TextButton(
                    onClick = { dismissedSections = emptySet() },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isCyber) "RESTORE DISMISSED SECTIONS [${dismissedSections.size}]" else "Restore hidden sections (${dismissedSections.size})",
                        color = accentColor,
                        fontSize = 11.sp,
                        fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Section 1: Workout Session
            if (!dismissedSections.contains("WORKOUT")) {
                SignalSectionCard(
                    title = if (isCyber) "WORKOUT_SESSION // OPS" else "Workout Session",
                    isCyber = isCyber,
                    primaryColor = primaryColor,
                    cardBgColor = cardBgColor,
                    onDismissSection = { dismissedSections = dismissedSections + "WORKOUT" }
                ) {
                    if (payload.workoutSummary != null) {
                        Text(
                            text = payload.workoutSummary!!,
                            color = primaryColor,
                            fontWeight = FontWeight.Bold,
                            fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default,
                            fontSize = 14.sp
                        )
                    } else {
                        EmptySignalText(isCyber = isCyber, text = "NONE // No workout recorded for this date")
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Section 2: Rites
            if (!dismissedSections.contains("RITES")) {
                SignalSectionCard(
                    title = if (isCyber) "RITES // DAILY_PRACTICE" else "Rites",
                    isCyber = isCyber,
                    primaryColor = primaryColor,
                    cardBgColor = cardBgColor,
                    onDismissSection = { dismissedSections = dismissedSections + "RITES" }
                ) {
                    if (payload.riteIds.isNotEmpty()) {
                        Text(
                            text = "${payload.riteIds.size} rite session(s) completed today",
                            color = primaryColor,
                            fontWeight = FontWeight.Bold,
                            fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default,
                            fontSize = 14.sp
                        )
                    } else {
                        EmptySignalText(isCyber = isCyber, text = "NONE // No rites completed for this date")
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Section 3: SANCTUM Night
            if (!dismissedSections.contains("SANCTUM")) {
                SignalSectionCard(
                    title = if (isCyber) "SANCTUM // PRIOR_NIGHT_SLEEP" else "Prior-Night SANCTUM",
                    isCyber = isCyber,
                    primaryColor = primaryColor,
                    cardBgColor = cardBgColor,
                    onDismissSection = { dismissedSections = dismissedSections + "SANCTUM" }
                ) {
                    if (payload.sanctumSummary != null) {
                        Text(
                            text = payload.sanctumSummary!!,
                            color = primaryColor,
                            fontWeight = FontWeight.Bold,
                            fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default,
                            fontSize = 14.sp
                        )
                    } else {
                        EmptySignalText(isCyber = isCyber, text = "NONE // No SANCTUM sleep telemetry for this date")
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Section 4: Body Log
            if (!dismissedSections.contains("BODY_LOG")) {
                SignalSectionCard(
                    title = if (isCyber) "BODY_LOG // VITAL_SAMPLES" else "Body Log",
                    isCyber = isCyber,
                    primaryColor = primaryColor,
                    cardBgColor = cardBgColor,
                    onDismissSection = { dismissedSections = dismissedSections + "BODY_LOG" }
                ) {
                    if (payload.bodyLogIds.isNotEmpty()) {
                        Text(
                            text = "${payload.bodyLogIds.size} body sample(s) recorded",
                            color = primaryColor,
                            fontWeight = FontWeight.Bold,
                            fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default,
                            fontSize = 14.sp
                        )
                    } else {
                        EmptySignalText(isCyber = isCyber, text = "NONE // No body log entries for this date")
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Section 5: Active Protocols
            if (!dismissedSections.contains("PROTOCOLS")) {
                SignalSectionCard(
                    title = if (isCyber) "BIOHACKING // ACTIVE_PROTOCOLS" else "Active Biohacking Protocols",
                    isCyber = isCyber,
                    primaryColor = primaryColor,
                    cardBgColor = cardBgColor,
                    onDismissSection = { dismissedSections = dismissedSections + "PROTOCOLS" }
                ) {
                    if (payload.protocolIds.isNotEmpty()) {
                        Text(
                            text = "${payload.protocolIds.size} active protocol(s) on date",
                            color = primaryColor,
                            fontWeight = FontWeight.Bold,
                            fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default,
                            fontSize = 14.sp
                        )
                    } else {
                        EmptySignalText(isCyber = isCyber, text = "NONE // No active biohacking protocols on date")
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Section 6: Free-Text Journal / Chronicle Note
            if (!dismissedSections.contains("CHRONICLE")) {
                SignalSectionCard(
                    title = if (isCyber) "CHRONICLE // FREE_TEXT_JOURNAL" else "Chronicle Journal Note",
                    isCyber = isCyber,
                    primaryColor = primaryColor,
                    cardBgColor = cardBgColor,
                    onDismissSection = { dismissedSections = dismissedSections + "CHRONICLE" }
                ) {
                    Column {
                        if (payload.chronicleContent != null) {
                            Text(
                                text = payload.chronicleContent!!,
                                color = primaryColor,
                                fontSize = 13.sp,
                                fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Serif
                            )
                        } else {
                            EmptySignalText(isCyber = isCyber, text = "No Chronicle note pre-linked for this date yet.")
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedButton(
                            onClick = { onOpenChronicleCompose(selectedDate, payload.chronicleEntryId) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = primaryColor)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (payload.chronicleContent != null) {
                                    if (isCyber) "OPEN_IN_CHRONICLE_COMPOSE" else "Edit in Chronicle"
                                } else {
                                    if (isCyber) "+ WRITE_CHRONICLE_NOTE_FOR_DATE" else "+ Write Chronicle Note"
                                },
                                fontWeight = FontWeight.Bold,
                                fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DateSelectorBar(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    primaryColor: Color,
    isCyber: Boolean
) {
    val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
    val isToday = selectedDate == LocalDate.now()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, primaryColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { onDateSelected(selectedDate.minusDays(1)) }) {
            Icon(
                imageVector = Icons.Default.ChevronLeft,
                contentDescription = "Previous Day",
                tint = primaryColor
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = selectedDate.format(formatter).uppercase(),
                color = primaryColor,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default
            )
            if (isToday) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "[TODAY]",
                    color = primaryColor.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!isToday) {
                IconButton(onClick = { onDateSelected(LocalDate.now()) }) {
                    Icon(
                        imageVector = Icons.Default.Today,
                        contentDescription = "Go to Today",
                        tint = primaryColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            IconButton(onClick = { onDateSelected(selectedDate.plusDays(1)) }) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Next Day",
                    tint = primaryColor
                )
            }
        }
    }
}

@Composable
private fun SignalSectionCard(
    title: String,
    isCyber: Boolean,
    primaryColor: Color,
    cardBgColor: Color,
    onDismissSection: () -> Unit,
    content: @Composable () -> Unit
) {
    if (isCyber) {
        CyberFrame(
            label = title,
            borderColor = primaryColor.copy(alpha = 0.6f),
            backgroundColor = cardBgColor
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(
                        onClick = onDismissSection,
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss section",
                            tint = primaryColor.copy(alpha = 0.4f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                content()
            }
        }
    } else {
        Card(
            colors = CardDefaults.cardColors(containerColor = cardBgColor),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, primaryColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        color = primaryColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    IconButton(
                        onClick = onDismissSection,
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss section",
                            tint = primaryColor.copy(alpha = 0.4f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                content()
            }
        }
    }
}

@Composable
private fun EmptySignalText(isCyber: Boolean, text: String) {
    Text(
        text = text,
        color = Color.Gray,
        fontSize = 12.sp,
        fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default
    )
}
