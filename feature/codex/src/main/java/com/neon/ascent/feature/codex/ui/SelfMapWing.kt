package com.neon.ascent.feature.codex.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neon.ascent.core.common.LocalNeonTheme
import com.neon.ascent.core.data.local.entity.EoEEntryEntity
import com.neon.ascent.core.data.local.entity.SelfItemEntity
import com.neon.ascent.core.data.local.entity.SelfMapMoodEntryEntity
import com.neon.ascent.core.data.local.entity.SelfMapSnapshotEntity
import com.neon.ascent.core.domain.selfmap.SelfMapCategory
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs

val codexEmotionChips = listOf(
    "Anger", "Fear", "Anxiety", "Sadness", "Frustration", "Disappointment", "Jealousy", "Happiness", "Relief"
)

fun getCategoryColor(category: SelfMapCategory): Color {
    return when (category) {
        SelfMapCategory.BODY_SENSES -> Color(0xFF00FFFF)
        SelfMapCategory.PEOPLE -> Color(0xFFFF006E)
        SelfMapCategory.STUFF -> Color(0xFFFFD700)
        SelfMapCategory.ROLES_ACCOMPLISHMENTS -> Color(0xFF00FF9F)
        SelfMapCategory.IDEAS_LIKES -> Color(0xFFA020F0)
        SelfMapCategory.CUSTOM -> Color(0xFFFF8C00)
    }
}

fun getLoadBandLabel(loadPercentage: Float): Pair<String, Color> {
    return when {
        loadPercentage < 60f -> "LIGHT LOAD" to Color(0xFF00FF9F)
        loadPercentage <= 110f -> "MODERATE LOAD" to Color(0xFF00FFFF)
        else -> "HEAVY ATTACHMENT LOAD" to Color(0xFFFF0055)
    }
}

fun getRingLabel(importance: Float): Pair<String, Color> {
    return when {
        importance >= 0.7f -> "CORE RING [0.7 - 1.0]" to Color(0xFFFF0055)
        importance >= 0.4f -> "HIGH RING [0.4 - 0.7]" to Color(0xFF00FFFF)
        importance >= 0.2f -> "MEDIUM RING [0.2 - 0.4]" to Color(0xFFFFD700)
        else -> "PERIPHERAL RING [0.0 - 0.2]" to Color(0xFF00FF9F)
    }
}

@Composable
fun CodexFrame(
    label: String,
    borderColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .background(Color(0xFF0C1017), RoundedCornerShape(4.dp))
            .border(1.dp, borderColor.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
            .padding(12.dp)
    ) {
        Column {
            Text(
                text = "//$label",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = borderColor,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                ),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelfMapWing(
    viewModel: CodexSelfMapViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val theme = LocalNeonTheme.current
    val cyan = theme.accent
    val danger = theme.accentDanger
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val uiState by viewModel.uiState.collectAsState()

    var isUnlocked by remember { mutableStateOf(!uiState.biometricLockEnabled) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf<SelfMapCategory?>(null) }
    var selectedEmotionFilter by remember { mutableStateOf<String?>(null) }
    var showFullDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.biometricLockEnabled) {
        if (!uiState.biometricLockEnabled) {
            isUnlocked = true
        }
    }

    // Biometric Gate
    if (uiState.biometricLockEnabled && !isUnlocked) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.95f))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            CodexFrame(
                label = "NEURAL_ARCHIVE_LOCKED // BIOMETRIC_CLEARANCE",
                borderColor = danger
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked Archive",
                        tint = danger,
                        modifier = Modifier.size(56.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "CODEX SELF MAP ENCRYPTED",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.5.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Biometric node lock is active. Authenticate identity to view read-only Self Map telemetry and EoE reaction logs.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.LightGray,
                            fontFamily = FontFamily.Monospace
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { isUnlocked = true },
                        colors = ButtonDefaults.buttonColors(containerColor = danger, contentColor = Color.White),
                        shape = MaterialTheme.shapes.extraSmall,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                        Text("AUTHENTICATE ARCHIVE ACCESS", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
        return
    }

    val filteredItems = remember(uiState.activeItems, searchQuery, selectedCategoryFilter) {
        uiState.activeItems.filter { item ->
            val matchesCategory = selectedCategoryFilter == null || item.category == selectedCategoryFilter
            val matchesQuery = searchQuery.isBlank() || item.name.contains(searchQuery, ignoreCase = true) ||
                    (item.notes?.contains(searchQuery, ignoreCase = true) == true)
            matchesCategory && matchesQuery
        }
    }

    val filteredEoEs = remember(uiState.eoeEntries, selectedEmotionFilter) {
        if (selectedEmotionFilter == null) {
            uiState.eoeEntries
        } else {
            uiState.eoeEntries.filter { it.emotionLabel.equals(selectedEmotionFilter, ignoreCase = true) }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 1. DENSITY TELEMETRY HEADER ---
        item {
            val (bandText, bandColor) = getLoadBandLabel(uiState.coreAttachmentLoad)
            CodexFrame(
                label = "SELF_MAP // DENSITY_TELEMETRY",
                borderColor = cyan,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "CORE ATTACHMENT LOAD",
                                style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray, fontFamily = FontFamily.Monospace)
                            )
                            Text(
                                text = "${uiState.coreAttachmentLoad.toInt()}%",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    color = bandColor,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace
                                )
                            )
                            Text(
                                text = bandText,
                                style = MaterialTheme.typography.labelSmall.copy(color = bandColor, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Active Attachments: ${uiState.activeItems.size}",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color.White, fontFamily = FontFamily.Monospace)
                            )
                            Text(
                                text = "Core Count (≥0.7): ${uiState.coreCount}",
                                style = MaterialTheme.typography.bodySmall.copy(color = danger, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            )
                            val formattedAvg = (Math.round(uiState.avgImportance * 100.0) / 100.0).toString()
                            Text(
                                text = "Avg Importance: $formattedAvg",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color.LightGray, fontFamily = FontFamily.Monospace)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Export & Full Delete Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val json = viewModel.exportDataAsJson()
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, json)
                                        type = "text/plain"
                                    }
                                    val shareIntent = Intent.createChooser(sendIntent, "EXPORT SELF MAP ARCHIVE [JSON]")
                                    context.startActivity(shareIntent)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = cyan, contentColor = Color.Black),
                            shape = MaterialTheme.shapes.extraSmall
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                            Text("EXPORT ARCHIVE [JSON]", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }

                        OutlinedButton(
                            onClick = { showFullDeleteDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = danger),
                            shape = MaterialTheme.shapes.extraSmall
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                            Text("FULL DELETE", fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }

        // --- 1B. DENSITY REDUCTION CALLOUT (SUCCESS SIGNAL) ---
        item {
            val baseline = uiState.baselineMass
            val current = uiState.currentMass
            if (baseline != null && baseline > 0f && current < baseline) {
                val dropPct = (((baseline - current) / baseline) * 100f).toInt()
                val formattedCurrent = (Math.round(current * 100.0) / 100.0).toString()
                val formattedBaseline = (Math.round(baseline * 100.0) / 100.0).toString()

                CodexFrame(
                    label = "DENSITY_REDUCTION // SUCCESS_SIGNAL",
                    borderColor = Color(0xFF00FF9F),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = "▼ ATTACHMENT MASS DOWN $dropPct% SINCE BASELINE ($formattedBaseline → $formattedCurrent)",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = Color(0xFF00FF9F),
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                        Text(
                            text = "Fewer or less-central attachments generate fewer automatic negative reactions and increase baseline capacity for happiness.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.LightGray,
                                fontFamily = FontFamily.Monospace
                            ),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }

        // --- 1C. DUAL TELEMETRY TREND (MASS VS. REPORTED MOOD) ---
        item {
            CodexFrame(
                label = "DUAL_TELEMETRY_TREND // MASS VS. REPORTED MOOD",
                borderColor = cyan,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "Snapshot Mass vs. Your Reported Mood (1–10). Local trend history.",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color.LightGray, fontFamily = FontFamily.Monospace)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (uiState.snapshots.isEmpty() && uiState.moodEntries.isEmpty()) {
                        Text(
                            text = "No snapshots or mood check-ins recorded yet.",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray, fontFamily = FontFamily.Monospace)
                        )
                    } else {
                        val historyRows = uiState.snapshots.take(5)
                        historyRows.forEach { snap ->
                            val snapDate = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                                .withZone(ZoneId.systemDefault())
                                .format(snap.timestamp)
                            val closestMood = uiState.moodEntries.firstOrNull { it.snapshotId == snap.id }
                                ?: uiState.moodEntries.minByOrNull { abs(it.timestamp.toEpochMilli() - snap.timestamp.toEpochMilli()) }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = snapDate,
                                        style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray, fontFamily = FontFamily.Monospace)
                                    )
                                    val formattedMass = (Math.round(snap.mass * 100.0) / 100.0).toString()
                                    Text(
                                        text = "Mass: $formattedMass (${snap.itemCount} items, ${snap.coreCount} cores)",
                                        style = MaterialTheme.typography.bodySmall.copy(color = Color.White, fontFamily = FontFamily.Monospace)
                                    )
                                }

                                Text(
                                    text = closestMood?.let { "Mood: ${it.score}/10" } ?: "Mood: --",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = cyan,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                )
                            }
                            HorizontalDivider(color = Color.DarkGray.copy(alpha = 0.5f), thickness = 0.5.dp)
                        }
                    }
                }
            }
        }

        // --- 2. LIVE QUERY CATALOG OF ATTACHMENTS ---
        item {
            CodexFrame(
                label = "LIVE_QUERY // ATTACHMENT_CATALOG [${filteredItems.size}]",
                borderColor = cyan,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search catalog...", fontFamily = FontFamily.Monospace, fontSize = 12.sp) },
                        leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = cyan,
                            unfocusedBorderColor = Color.DarkGray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item {
                            FilterChip(
                                selected = selectedCategoryFilter == null,
                                onClick = { selectedCategoryFilter = null },
                                label = { Text("ALL", fontFamily = FontFamily.Monospace, fontSize = 10.sp) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = cyan, selectedLabelColor = Color.Black)
                            )
                        }

                        items(SelfMapCategory.entries) { cat ->
                            val isSel = selectedCategoryFilter == cat
                            FilterChip(
                                selected = isSel,
                                onClick = { selectedCategoryFilter = if (isSel) null else cat },
                                label = { Text(cat.displayName.uppercase(), fontFamily = FontFamily.Monospace, fontSize = 10.sp) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = getCategoryColor(cat), selectedLabelColor = Color.Black)
                            )
                        }
                    }
                }
            }
        }

        // Read-only Attachment Rows
        items(filteredItems, key = { it.id }) { item ->
            val (ringLabel, ringColor) = getRingLabel(item.importance)
            CodexFrame(
                label = ringLabel,
                borderColor = ringColor,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium.copy(color = Color.White, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Category: ${item.category.displayName} | Importance: ${(item.importance * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall.copy(color = ringColor, fontFamily = FontFamily.Monospace)
                    )
                    if (!item.notes.isNullOrBlank()) {
                        Text(
                            text = item.notes!!,
                            style = MaterialTheme.typography.bodySmall.copy(color = Color.LightGray, fontFamily = FontFamily.Monospace),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    val linkedChronicles = uiState.chronicleEntries.filter { it.selfItemId == item.id }
                    if (linkedChronicles.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Linked Chronicle Reflections (${linkedChronicles.size}):",
                            style = MaterialTheme.typography.labelSmall.copy(color = cyan, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        )
                        linkedChronicles.forEach { chronicle ->
                            Text(
                                text = "• \"${chronicle.content}\"",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color.LightGray, fontFamily = FontFamily.Monospace)
                            )
                        }
                    }
                }
            }
        }

        // --- 3. EQUATION OF EMOTION REACTION LOGS ARCHIVE + LINKED MOODS ---
        item {
            CodexFrame(
                label = "EQUATION_OF_EMOTION // REACTION_ARCHIVE [${filteredEoEs.size}]",
                borderColor = danger,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "Historical trace of emotional reactions compared against expectations.",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color.LightGray, fontFamily = FontFamily.Monospace)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item {
                            FilterChip(
                                selected = selectedEmotionFilter == null,
                                onClick = { selectedEmotionFilter = null },
                                label = { Text("ALL EMOTIONS", fontFamily = FontFamily.Monospace, fontSize = 10.sp) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = danger, selectedLabelColor = Color.White)
                            )
                        }

                        items(codexEmotionChips) { emotion ->
                            val isSel = selectedEmotionFilter.equals(emotion, ignoreCase = true)
                            FilterChip(
                                selected = isSel,
                                onClick = { selectedEmotionFilter = if (isSel) null else emotion },
                                label = { Text(emotion.uppercase(), fontFamily = FontFamily.Monospace, fontSize = 10.sp) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = danger, selectedLabelColor = Color.White)
                            )
                        }
                    }
                }
            }
        }

        // Read-only EoE Reaction Rows with Linked Mood Check-Ins
        if (filteredEoEs.isEmpty()) {
            item {
                Text(
                    text = "No reaction logs recorded matching current filter.",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray, fontFamily = FontFamily.Monospace),
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        } else {
            items(filteredEoEs, key = { it.id }) { entry ->
                val dateStr = remember(entry.timestamp) {
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                        .withZone(ZoneId.systemDefault())
                        .format(entry.timestamp)
                }

                val linkedItem = uiState.activeItems.firstOrNull { it.id == entry.selfItemId }
                val linkedMood = uiState.moodEntries.firstOrNull { it.eoeId == entry.id }

                CodexFrame(
                    label = "${entry.emotionLabel.uppercase()} // INTENSITY: ${entry.intensity ?: 5}/10",
                    borderColor = danger,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Attached Item: ${linkedItem?.name ?: "ID:${entry.selfItemId}"}",
                                style = MaterialTheme.typography.labelMedium.copy(color = cyan, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            )
                            Text(
                                text = dateStr,
                                style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray, fontFamily = FontFamily.Monospace)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "• EP (Expectation): \"${entry.epText}\"",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color.White, fontFamily = FontFamily.Monospace)
                        )
                        Text(
                            text = "• P (Perception): \"${entry.perceptionText}\"",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color.LightGray, fontFamily = FontFamily.Monospace)
                        )

                        if (!entry.appraisal.isNullOrBlank()) {
                            Text(
                                text = "• Appraisal: ${entry.appraisal}",
                                style = MaterialTheme.typography.bodySmall.copy(color = danger, fontFamily = FontFamily.Monospace)
                            )
                        }

                        if (!entry.canChangeNotes.isNullOrBlank()) {
                            Text(
                                text = "💡 Reflection: ${entry.canChangeNotes}",
                                style = MaterialTheme.typography.labelSmall.copy(color = cyan, fontFamily = FontFamily.Monospace),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        if (linkedMood != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "• Linked Reported Mood: ",
                                    style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray, fontFamily = FontFamily.Monospace)
                                )
                                Text(
                                    text = "${linkedMood.score}/10",
                                    style = MaterialTheme.typography.labelSmall.copy(color = cyan, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                )
                                if (!linkedMood.note.isNullOrBlank()) {
                                    Text(
                                        text = " (${linkedMood.note})",
                                        style = MaterialTheme.typography.labelSmall.copy(color = Color.LightGray, fontFamily = FontFamily.Monospace)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Full Delete Confirmation Dialog
    if (showFullDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showFullDeleteDialog = false },
            containerColor = Color(0xFF10141A),
            title = {
                Text(
                    text = "CONFIRM FULL DELETE?",
                    style = MaterialTheme.typography.titleMedium.copy(color = danger, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to permanently delete all Self Map items, EoE reaction logs, snapshots, and baseline data? This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color.LightGray, fontFamily = FontFamily.Monospace)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAllData()
                        showFullDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = danger, contentColor = Color.White),
                    shape = MaterialTheme.shapes.extraSmall
                ) {
                    Text("DELETE ALL DATA", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            },
            dismissButton = {
                TextButton(onClick = { showFullDeleteDialog = false }) {
                    Text("CANCEL", color = Color.Gray, fontFamily = FontFamily.Monospace)
                }
            }
        )
    }
}
