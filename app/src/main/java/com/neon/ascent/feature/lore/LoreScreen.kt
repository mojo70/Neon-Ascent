package com.neon.ascent.feature.lore

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neon.ascent.core.common.*
import com.neon.ascent.core.domain.chronicle.ChronicleEntry
import com.neon.ascent.core.domain.chronicle.DailyOriginPayload
import com.neon.ascent.ui.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoreScreen(
    onBack: () -> Unit,
    viewModel: LoreViewModel = hiltViewModel()
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("NEURAL_HISTORY", style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 2.sp)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "BACK")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.resetStory() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "RESET", tint = Color.Red.copy(alpha = 0.6f))
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color(0xFF00FF9C)
                )
            )
        },
        containerColor = Color.Black
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            LoreScreenContent(viewModel)
        }
    }
}

@Composable
fun LoreScreenContent(viewModel: LoreViewModel) {
    val chronicleEntries by viewModel.chronicleEntries.collectAsState()
    val userStory by viewModel.userStory.collectAsState()
    var editingIndex by remember { mutableIntStateOf(-2) } // -2: none, -1: main story, 0+: chapter
    var editBuffer by remember { mutableStateOf("") }

    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var showDateJournalDialog by remember { mutableStateOf(false) }
    var journalBuffer by remember { mutableStateOf("") }

    val dayPayload by viewModel.observeDayPayload(selectedDate)
        .collectAsState(initial = DailyOriginPayload(selectedDate.toString()))

    val visualMode = LocalVisualMode.current
    val theme = LocalNeonTheme.current
    val isCyber = visualMode == VisualMode.CYBER

    val primaryColor = if (isCyber) Color(0xFF00FF9C) else theme.ink
    val accentColor = if (isCyber) Color(0xFFFF006E) else theme.secondary

    val hasLegacyStory = userStory.cyberLore.isNotBlank() || userStory.weeklyChapters.isNotEmpty()
    val isEmpty = chronicleEntries.isEmpty() && !hasLegacyStory

    Box(modifier = Modifier.fillMaxSize()) {
        if (isCyber) {
            PerspectiveGrid()
            Scanlines(intensity = 0.1f)
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item { Spacer(Modifier.height(16.dp)) }

            // Day Selector above CHRONICLE
            item {
                CodexChronicleDaySelector(
                    selectedDate = selectedDate,
                    onDateSelected = { selectedDate = it },
                    primaryColor = primaryColor,
                    isCyber = isCyber
                )
            }

            // Compact Day Header (Telemetry Summary)
            item {
                CodexChronicleDayHeader(
                    payload = dayPayload,
                    selectedDate = selectedDate,
                    onOpenJournal = {
                        journalBuffer = dayPayload.chronicleContent ?: ""
                        showDateJournalDialog = true
                    },
                    primaryColor = primaryColor,
                    accentColor = accentColor,
                    isCyber = isCyber
                )
            }

            if (isEmpty) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "NO_ORIGIN_YET",
                            color = primaryColor.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            )
                        )
                    }
                }
            } else {
                // Render ChronicleEntries (wing = CHRONICLE, newest first)
                itemsIndexed(chronicleEntries) { _, entry ->
                    val isMatch = entry.localDate == selectedDate.toString() ||
                            entry.metadata?.contains("\"localDate\":\"$selectedDate\"") == true

                    val sectionTitle = when (entry.room) {
                        "ORIGIN" -> if (isMatch) "ORIGIN_LOG // SELECTED_DATE ($selectedDate)" else "ORIGIN_LOG // BASE_BIO"
                        "SHARD" -> if (isMatch) "DATA_SHARD // SELECTED_DATE" else "DATA_SHARD // CHRONICLE"
                        "FRAGMENT" -> if (isMatch) "MEMORY_FRAGMENT // SELECTED_DATE" else "MEMORY_FRAGMENT // CHRONICLE"
                        else -> if (isMatch) "${entry.room} // SELECTED_DATE" else "${entry.room} // CHRONICLE"
                    }

                    LoreSection(
                        title = sectionTitle,
                        content = entry.content,
                        isHacked = entry.hearted || isMatch,
                        accentColor = if (isMatch) primaryColor else (if (entry.hearted) accentColor else Color(0xFF00FFFF)),
                        onEditClick = {
                            editingIndex = -1
                            editBuffer = entry.content
                        }
                    )
                }

                // Main Story / Biography (if present and not duplicated)
                if (userStory.cyberLore.isNotBlank() && chronicleEntries.none { it.room == "ORIGIN" && it.content == userStory.cyberLore }) {
                    item {
                        LoreSection(
                            title = "ORIGIN_LOG // BASE_BIO",
                            content = userStory.cyberLore,
                            isHacked = false,
                            onEditClick = {
                                editingIndex = -1
                                editBuffer = userStory.cyberLore
                            }
                        )
                    }
                }

                // Weekly Chapters (if present and not duplicated)
                itemsIndexed(userStory.weeklyChapters) { index, chapter ->
                    if (chronicleEntries.none { it.content == chapter.content }) {
                        LoreSection(
                            title = chapter.title,
                            content = chapter.content,
                            isHacked = chapter.isHacked,
                            onEditClick = {
                                editingIndex = index
                                editBuffer = chapter.content
                            }
                        )
                    }
                }
            }

            item {
                Spacer(Modifier.height(64.dp))
            }
        }

        // Journal Note Dialog pre-linked to selectedDate
        if (showDateJournalDialog) {
            AlertDialog(
                onDismissRequest = { showDateJournalDialog = false },
                containerColor = Color(0xFF0A0A0A),
                title = {
                    Text(
                        text = if (isCyber) "CHRONICLE_NOTE // $selectedDate" else "Chronicle Note ($selectedDate)",
                        color = primaryColor,
                        fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default
                    )
                },
                text = {
                    OutlinedTextField(
                        value = journalBuffer,
                        onValueChange = { journalBuffer = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp),
                        placeholder = { Text("Write free-text reflection for $selectedDate...") },
                        textStyle = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = primaryColor.copy(alpha = 0.5f),
                            focusedContainerColor = Color.Black,
                            unfocusedContainerColor = Color.Black,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.saveChronicleEntry(selectedDate, journalBuffer)
                        showDateJournalDialog = false
                    }) {
                        Text(
                            text = if (isCyber) "SAVE_TO_CHRONICLE" else "Save Note",
                            color = primaryColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDateJournalDialog = false }) {
                        Text("CANCEL", color = Color.Gray)
                    }
                },
                modifier = Modifier.neonBorder(primaryColor)
            )
        }

        // Quickhack Edit Dialog
        if (editingIndex != -2) {
            AlertDialog(
                onDismissRequest = { editingIndex = -2 },
                containerColor = Color(0xFF0A0A0A),
                title = { Text("QUICKHACK_INTERFACE", color = Color(0xFFFF006E), fontFamily = FontFamily.Monospace) },
                text = {
                    OutlinedTextField(
                        value = editBuffer,
                        onValueChange = { editBuffer = it },
                        modifier = Modifier.fillMaxWidth().height(300.dp),
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFFF006E),
                            unfocusedBorderColor = Color(0xFFFF006E).copy(alpha = 0.5f),
                            focusedContainerColor = Color.Black,
                            unfocusedContainerColor = Color.Black,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (editingIndex == -1) {
                            viewModel.hackMainStory(editBuffer)
                        } else {
                            viewModel.hackChapter(editingIndex, editBuffer)
                        }
                        editingIndex = -2
                    }) {
                        Text("UPLOAD_EXPLOIT", color = Color(0xFF00FF9C))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { editingIndex = -2 }) {
                        Text("ABORT", color = Color.Gray)
                    }
                },
                modifier = Modifier.neonBorder(Color(0xFFFF006E))
            )
        }
    }
}

@Composable
fun CodexChronicleDaySelector(
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
            .border(1.dp, primaryColor.copy(alpha = 0.3f))
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
                fontSize = 14.sp,
                fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default
            )
            if (isToday) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "[TODAY]",
                    color = primaryColor.copy(alpha = 0.7f),
                    fontSize = 10.sp,
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
fun CodexChronicleDayHeader(
    payload: DailyOriginPayload,
    selectedDate: LocalDate,
    onOpenJournal: () -> Unit,
    primaryColor: Color,
    accentColor: Color,
    isCyber: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(primaryColor.copy(alpha = 0.05f))
            .border(1.dp, primaryColor.copy(alpha = 0.4f))
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isCyber) "DAY_TELEMETRY // $selectedDate" else "Telemetry Summary ($selectedDate)",
                    color = primaryColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default
                )
                TextButton(onClick = onOpenJournal) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (payload.chronicleContent != null) {
                            if (isCyber) "EDIT_JOURNAL" else "Edit Journal"
                        } else {
                            if (isCyber) "+ JOURNAL" else "+ Journal"
                        },
                        color = accentColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Telemetry Chips Grid / Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TelemetryBadge(
                    label = "OPS",
                    value = payload.workoutSummary ?: "None",
                    color = primaryColor,
                    isCyber = isCyber,
                    modifier = Modifier.weight(1f)
                )
                TelemetryBadge(
                    label = "RITES",
                    value = if (payload.riteIds.isNotEmpty()) "${payload.riteIds.size} done" else "None",
                    color = primaryColor,
                    isCyber = isCyber,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TelemetryBadge(
                    label = "SANCTUM",
                    value = payload.sanctumSummary ?: "None",
                    color = primaryColor,
                    isCyber = isCyber,
                    modifier = Modifier.weight(1f)
                )
                TelemetryBadge(
                    label = "BODY",
                    value = if (payload.bodyLogIds.isNotEmpty()) "${payload.bodyLogIds.size} samples" else "None",
                    color = primaryColor,
                    isCyber = isCyber,
                    modifier = Modifier.weight(1f)
                )
                TelemetryBadge(
                    label = "STACKS",
                    value = if (payload.protocolIds.isNotEmpty()) "${payload.protocolIds.size} active" else "None",
                    color = primaryColor,
                    isCyber = isCyber,
                    modifier = Modifier.weight(1f)
                )
            }

            if (payload.chronicleContent != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(primaryColor.copy(alpha = 0.08f))
                        .padding(8.dp)
                ) {
                    Text(
                        text = payload.chronicleContent!!,
                        color = primaryColor,
                        fontSize = 12.sp,
                        fontStyle = FontStyle.Italic,
                        fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Serif
                    )
                }
            }
        }
    }
}

@Composable
private fun TelemetryBadge(
    label: String,
    value: String,
    color: Color,
    isCyber: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(color.copy(alpha = 0.05f))
            .border(1.dp, color.copy(alpha = 0.15f))
            .padding(vertical = 4.dp, horizontal = 6.dp)
    ) {
        Text(
            text = label,
            color = color.copy(alpha = 0.6f),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default
        )
        Text(
            text = value,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = if (isCyber) FontFamily.Monospace else FontFamily.Default
        )
    }
}

@Composable
fun LoreSection(
    title: String,
    content: String,
    isHacked: Boolean,
    accentColor: Color = Color(0xFFFF006E),
    onEditClick: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = if (isHacked) accentColor else Color(0xFF00FFFF),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            )
            IconButton(onClick = onEditClick, modifier = Modifier.size(24.dp)) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "HACK",
                    tint = if (isHacked) accentColor else Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.05f))
                .border(1.dp, if (isHacked) accentColor.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.1f))
                .padding(16.dp)
        ) {
            Text(
                text = content,
                color = Color.White.copy(alpha = 0.9f),
                lineHeight = 24.sp,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Serif,
                    fontStyle = FontStyle.Italic
                )
            )
        }
    }
}
