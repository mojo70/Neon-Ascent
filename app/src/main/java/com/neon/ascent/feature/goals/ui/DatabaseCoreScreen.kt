package com.neon.ascent.feature.goals.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.neon.ascent.Screen
import com.neon.ascent.core.common.*
import com.neon.ascent.core.domain.goals.models.Aspiration
import com.neon.ascent.core.domain.goals.models.Habit
import com.neon.ascent.core.domain.goals.models.Mission
import com.neon.ascent.core.domain.goals.models.AscensionDirective
import com.neon.ascent.core.domain.goals.models.AscensionMission
import com.neon.ascent.core.domain.goals.models.AscensionTask
import com.neon.ascent.core.data.local.entity.NeuralMemory
import com.neon.ascent.feature.lore.LoreScreen
import com.neon.ascent.feature.lore.LoreScreenContent
import com.neon.ascent.feature.lore.LoreViewModel
import com.neon.ascent.feature.journal.JournalViewModel
import com.neon.ascent.model.*
import com.neon.ascent.core.common.neonBorder
import com.neon.ascent.core.common.Scanlines
import com.neon.ascent.core.common.CyberGridBackground
import com.neon.ascent.ui.CyberFrame
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

enum class DatabaseView {
    OVERVIEW, NEURAL_JOURNAL, BIO_PROTOCOLS, LORE, NEURAL_MEMORIES
}

@Composable
fun DatabaseCoreScreen(
    navController: NavController,
    onEntryClick: (JournalEntry) -> Unit,
    onStoryClick: () -> Unit,
    onBack: () -> Unit = {},
    onHackingRequired: () -> Unit = {},
    viewModel: JournalViewModel = hiltViewModel(),
    aspirationsViewModel: DatabaseCoreViewModel = hiltViewModel(),
    loreViewModel: LoreViewModel = hiltViewModel()
) {
    val theme = LocalNeonTheme.current
    val context = androidx.compose.ui.platform.LocalContext.current
    var activeTab by remember { mutableIntStateOf(0) } // 0: YOUR_DATA, 1: SYSTEM_DATA, 2: LORE
    val isHacked by viewModel.isSystemDatabaseHacked.collectAsState()
    var selectedEntry by remember { mutableStateOf<JournalEntry?>(null) }
    var selectedProtocol by remember { mutableStateOf<BioProtocolLog?>(null) }
    val searchQuery by viewModel.searchQuery.collectAsState()

    LaunchedEffect(Unit) {
        aspirationsViewModel.exportEvent.collect { logContent ->
            val sendIntent = android.content.Intent().apply {
                action = android.content.Intent.ACTION_SEND
                putExtra(android.content.Intent.EXTRA_TEXT, logContent)
                type = "text/markdown"
            }
            val shareIntent = android.content.Intent.createChooser(sendIntent, "EXPORT NEURAL LOG")
            context.startActivity(shareIntent)
        }
    }

    LaunchedEffect(Unit) {
        aspirationsViewModel.exportWorkoutEvent.collect { jsonContent ->
            val sendIntent = android.content.Intent().apply {
                action = android.content.Intent.ACTION_SEND
                putExtra(android.content.Intent.EXTRA_TEXT, jsonContent)
                type = "application/json"
            }
            val shareIntent = android.content.Intent.createChooser(sendIntent, "EXPORT WORKOUT HISTORY")
            context.startActivity(shareIntent)
        }
    }
    
    // Sub-navigation within tabs
    var currentView by remember { mutableStateOf(DatabaseView.OVERVIEW) }

    Box(modifier = Modifier.fillMaxSize().background(theme.canvas)) {
        if (theme.mode == VisualMode.CYBER) {
            Scanlines()
        }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (currentView != DatabaseView.OVERVIEW) {
                        IconButton(onClick = { currentView = DatabaseView.OVERVIEW }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = theme.accent)
                        }
                    }
                    Text(
                        if (currentView == DatabaseView.OVERVIEW) "DATABASE_NODE // V.4.0" else currentView.name,
                        color = theme.accent,
                        fontSize = 20.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = theme.secondary)
                }
            }

            Spacer(Modifier.height(16.dp))

            if (currentView == DatabaseView.OVERVIEW) {
                // Tab Selector
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DatabaseTab("OPERATOR_DATA", activeTab == 0, Modifier.weight(1f)) { activeTab = 0 }
                    DatabaseTab("SYSTEM_DATA", activeTab == 1, Modifier.weight(1f)) { activeTab = 1 }
                    DatabaseTab("ARCHIVE_LORE", activeTab == 2, Modifier.weight(1f)) { activeTab = 2 }
                }
                Spacer(Modifier.height(24.dp))
            } else {
                // Search Bar for dedicated views
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    placeholder = { Text("FILTER_DATABASE...", color = theme.inkMuted, fontSize = 12.sp, fontFamily = FontFamily.Monospace) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = theme.accent) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = null, tint = theme.inkMuted)
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = theme.accent,
                        unfocusedBorderColor = theme.inkMuted.copy(alpha = 0.3f),
                        cursorColor = theme.accent,
                        focusedTextColor = theme.ink,
                        unfocusedTextColor = theme.ink
                    ),
                    textStyle = LocalTextStyle.current.copy(color = theme.ink, fontFamily = FontFamily.Monospace, fontSize = 14.sp)
                )
            }

            // Content Area
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (currentView) {
                    DatabaseView.OVERVIEW -> {
                        when (activeTab) {
                            0 -> PersonalDatabaseOverview(
                                viewModel = viewModel,
                                aspirationsViewModel = aspirationsViewModel,
                                navController = navController,
                                onJournalClick = { currentView = DatabaseView.NEURAL_JOURNAL },
                                onBioClick = { currentView = DatabaseView.BIO_PROTOCOLS },
                                onMemoriesClick = { currentView = DatabaseView.NEURAL_MEMORIES },
                                onLoreClick = { activeTab = 2 },
                                onEntryClick = { selectedEntry = it }
                            )
                            1 -> SystemDatabaseContent(isHacked, onHackingRequired, viewModel)
                            2 -> LoreScreenContent(loreViewModel)
                        }
                    }
                    DatabaseView.NEURAL_JOURNAL -> {
                        val entries by viewModel.entries.collectAsState()
                        JournalListView(entries) { selectedEntry = it }
                    }
                    DatabaseView.BIO_PROTOCOLS -> {
                        val protocols by viewModel.bioProtocolLogs.collectAsState()
                        ProtocolListView(
                            protocols = protocols,
                            onBrowseCanonicalIndex = { navController.navigate(Screen.ProtocolLibrary) }
                        ) { selectedProtocol = it }
                    }
                    DatabaseView.NEURAL_MEMORIES -> {
                        val memories by aspirationsViewModel.neuralMemories.collectAsState()
                        NeuralMemoryListView(memories)
                    }
                    DatabaseView.LORE -> {
                        LoreScreenContent(loreViewModel)
                    }
                }
            }
            
            if (activeTab == 1 && currentView == DatabaseView.OVERVIEW) {
                Button(
                    onClick = onStoryClick,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    border = BorderStroke(1.dp, theme.accent)
                ) {
                    Text("FULL_ARCHIVE_VIEW", color = theme.accent, fontFamily = FontFamily.Monospace)
                }
            }
        }

        selectedEntry?.let { entry ->
            JournalEntryDialog(entry = entry, onDismiss = { selectedEntry = null })
        }
        
        selectedProtocol?.let { protocol ->
            ProtocolDetailDialog(protocol = protocol, onDismiss = { selectedProtocol = null })
        }
    }
}

@Composable
fun PersonalDatabaseOverview(
    viewModel: JournalViewModel,
    aspirationsViewModel: DatabaseCoreViewModel,
    navController: NavController,
    onJournalClick: () -> Unit,
    onBioClick: () -> Unit,
    onMemoriesClick: () -> Unit,
    onLoreClick: () -> Unit,
    onEntryClick: (JournalEntry) -> Unit
) {
    val theme = LocalNeonTheme.current
    val entries by viewModel.entries.collectAsState()
    val protocols by viewModel.bioProtocolLogs.collectAsState()
    val quests by viewModel.quests.collectAsState()
    val importedMissions by viewModel.importedQuestMissions.collectAsState()
    val recurringAscensionTasks by viewModel.recurringAscensionTasks.collectAsState()
    val dailyTasks by viewModel.dailyTasks.collectAsState()
    val aspirations by aspirationsViewModel.aspirations.collectAsState()
    val activeMissions by aspirationsViewModel.activeMissions.collectAsState()
    val ascensionDirectives by aspirationsViewModel.ascensionDirectives.collectAsState()
    val ascensionMissions by aspirationsViewModel.ascensionMissions.collectAsState()
    val ascensionTasks by aspirationsViewModel.ascensionTasks.collectAsState()
    val neuralMemories by aspirationsViewModel.neuralMemories.collectAsState()
    
    var isNeuralMemoriesExpanded by remember { mutableStateOf(false) }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { SectionHeader("DIURNAL_PULSES", "ACTIVE") }
        
        val allRecurring = (ascensionTasks + recurringAscensionTasks).distinctBy { it.id }
        items(allRecurring) { task ->
            val isWorkoutTask = task.tags.contains("workout_session")
            AscensionTaskItem(
                task = task,
                onStart = if (isWorkoutTask) { { navController.navigate(Screen.WorkoutLog(task.id)) } } else null,
                onToggle = { isChecked ->
                    if (isChecked) {
                        aspirationsViewModel.completeAscensionTask(task)
                    }
                }
            )
        }

        if (allRecurring.isEmpty() && dailyTasks.isNotEmpty()) {
            items(dailyTasks) { task ->
                TaskItem(task) { viewModel.updateTaskCompletion(task, it) }
            }
        }

        item { 
            SectionHeader(
                label = "NEURAL_MEMORIES", 
                status = if (isNeuralMemoriesExpanded) "COLLAPSE [${neuralMemories.size} FRAGMENTS]" else "EXPAND [${neuralMemories.size} FRAGMENTS]", 
                onClick = { isNeuralMemoriesExpanded = !isNeuralMemoriesExpanded }
            ) 
        }
        if (isNeuralMemoriesExpanded) {
            if (neuralMemories.isEmpty()) {
                item { EmptyDataPlaceholder("NO_MEMORIES_CAPTURED") }
            } else {
                items(neuralMemories.take(5)) { memory ->
                    NeuralMemoryMiniItem(memory)
                }
                item {
                    Text(
                        text = "VIEW_ALL_FRAGMENTS_FULL_SCREEN",
                        color = theme.accent,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onMemoriesClick() }
                            .padding(8.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // === OPERATIONAL DIRECTIVES SECTION ===
        item { SectionHeader("OPERATIONAL_DIRECTIVES", "LONG_TERM") }
        
        items(ascensionDirectives) { directive ->
            AscensionDirectiveSummaryCard(directive) {
                navController.navigate(Screen.DirectiveDetail(directive.id))
            }
        }

        items(aspirations) { aspiration ->
            AspirationSummaryCard(aspiration) { 
                navController.navigate(Screen.AspirationDetail(aspiration.id)) 
            }
        }

        if (ascensionDirectives.isEmpty()) {
            item {
                EmptyStateCard(
                    message = "NO_DIRECTIVES_YET",
                    buttonText = "CREATE NEW DIRECTIVE",
                    onClick = { navController.navigate(Screen.AscensionForge()) }
                )
            }
        }

        // === ACTIVE MISSIONS SECTION ===
        item { SectionHeader("ACTIVE_MISSIONS", "TACTICAL") }
        
        items(ascensionMissions) { mission ->
            AscensionMissionSummaryCard(mission) {
                navController.navigate(Screen.AscensionMissionDetail(mission.id))
            }
        }

        items(activeMissions) { mission ->
            MissionSummaryCard(mission) {
                navController.navigate(Screen.MissionDetail(mission.id))
            }
        }

        item { SectionHeader("NEURAL_JOURNAL", "${entries.size} LOGS", onJournalClick) }
        if (entries.isEmpty()) {
            item { EmptyDataPlaceholder("NO_ENTRIES_FOUND") }
        } else {
            items(entries.take(3)) { entry ->
                JournalEntryMiniItem(entry) { onEntryClick(entry) }
            }
        }

        item { SectionHeader("BIO_PROTOCOLS", "${protocols.size} VERIFIED", onBioClick) }
        val workingProtocols = protocols.filter { it.isWorking == true }
        if (workingProtocols.isEmpty()) {
            item { EmptyDataPlaceholder("NO_STABLE_PROTOCOLS") }
        } else {
            items(workingProtocols.take(2)) { protocol ->
                ProtocolItem(protocol, true) { onBioClick() }
            }
        }
        
        item { SectionHeader("NEURAL_LORE_CORE", "ARCHIVED", onLoreClick) }
        
        item { SectionHeader("CAMPAIGN_HISTORY", "LONG_TERM") }
        if (importedMissions.isNotEmpty()) {
            items(importedMissions) { mission ->
                AscensionMissionQuestItem(mission, viewModel)
            }
        } else if (quests.isNotEmpty()) {
            items(quests) { quest ->
                QuestItem(quest, viewModel)
            }
        } else {
            item { EmptyDataPlaceholder("NO_QUESTS_IMPORTED") }
        }

        // Quick action button
        item {
            Button(
                onClick = { aspirationsViewModel.exportNeuralLog() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                border = BorderStroke(1.dp, theme.accent)
            ) {
                Text("EXPORT NEURAL LOG [.MD]", color = theme.accent, fontFamily = FontFamily.Monospace)
            }

            Button(
                onClick = { aspirationsViewModel.exportWorkoutHistory() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                border = BorderStroke(1.dp, Color(0xFF007AFF))
            ) {
                Text("EXPORT WORKOUT HISTORY [.JSON]", color = Color(0xFF007AFF), fontFamily = FontFamily.Monospace)
            }

            Button(
                onClick = { navController.navigate(Screen.AscensionForge()) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = theme.accent, contentColor = theme.canvas)
            ) {
                Text("+ DEPLOY NEW DIRECTIVE")
            }
        }
        
        item { Spacer(Modifier.height(32.dp)) }
    }
}

@Composable
fun JournalListView(entries: List<JournalEntry>, onEntryClick: (JournalEntry) -> Unit) {
    val theme = LocalNeonTheme.current
    if (entries.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyDataPlaceholder("NO_MATCHING_LOGS")
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(entries) { entry ->
                JournalEntryMiniItem(entry) { onEntryClick(entry) }
                HorizontalDivider(color = theme.inkMuted.copy(alpha = 0.1f))
            }
        }
    }
}

@Composable
fun ProtocolListView(
    protocols: List<BioProtocolLog>, 
    onBrowseCanonicalIndex: () -> Unit = {},
    onProtocolClick: (BioProtocolLog) -> Unit
) {
    val theme = LocalNeonTheme.current
    val stable = protocols.filter { it.isWorking == true }
    val rejected = protocols.filter { it.isWorking == false }
    val unrated = protocols.filter { it.isWorking == null }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Button(
                onClick = onBrowseCanonicalIndex,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                border = BorderStroke(1.dp, theme.accent)
            ) {
                Icon(Icons.AutoMirrored.Filled.LibraryBooks, contentDescription = null, tint = theme.accent, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("BROWSE_CANONICAL_INDEX", color = theme.accent, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            }
        }

        if (stable.isNotEmpty()) {
            item { SectionHeader("SYNERGY_STABLE", "${stable.size} LOGS") }
            items(stable) { protocol -> 
                ProtocolItem(protocol, true) { onProtocolClick(protocol) }
            }
        }
        if (rejected.isNotEmpty()) {
            item { SectionHeader("NEURAL_REJECTION", "${rejected.size} LOGS") }
            items(rejected) { protocol -> 
                ProtocolItem(protocol, false) { onProtocolClick(protocol) }
            }
        }
        if (unrated.isNotEmpty()) {
            item { SectionHeader("PENDING_CALIBRATION", "${unrated.size} LOGS") }
            items(unrated) { protocol ->
                CyberFrame(label = "UNRATED_LOG", borderColor = theme.inkMuted) {
                    Text("Protocol ${protocol.protocolId} - Pending Rating", color = theme.inkMuted, fontSize = 12.sp)
                }
            }
        }
        
        if (protocols.isEmpty()) {
            item { EmptyDataPlaceholder("NO_PROTOCOLS_FOUND") }
        }
    }
}

@Composable
fun ProtocolDetailDialog(protocol: BioProtocolLog, onDismiss: () -> Unit) {
    val theme = LocalNeonTheme.current
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        CyberFrame(label = "PROTOCOL_LOG // ${protocol.protocolId}") {
            Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                Text("STABILITY_RATING: ${if (protocol.isWorking == true) "STABLE" else if (protocol.isWorking == false) "REJECTED" else "UNRATED"}", color = if (protocol.isWorking == true) Color(0xFF00FFAA) else Color.Red)
                Spacer(Modifier.height(12.dp))
                Text("NOTES:", color = Color(0xFF00FFAA), fontSize = 10.sp)
                Text(protocol.notes ?: "NO_NOTES", color = theme.ink, fontSize = 14.sp)
                if (protocol.sideEffects != null) {
                    Spacer(Modifier.height(12.dp))
                    Text("SIDE_EFFECTS:", color = Color.Red, fontSize = 10.sp)
                    Text(protocol.sideEffects, color = theme.ink, fontSize = 14.sp)
                }
                Spacer(Modifier.height(24.dp))
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("CLOSE")
                }
            }
        }
    }
}

@Composable
fun JournalEntryDialog(entry: JournalEntry, onDismiss: () -> Unit) {
    val theme = LocalNeonTheme.current
    val dateFormat = remember { SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.US) }
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        CyberFrame(label = "NEURAL_LOG // ${entry.category}") {
            Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                Text(
                    dateFormat.format(Date(entry.timestamp)),
                    color = Color(0xFF00FFAA),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    entry.text,
                    color = theme.ink,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState())
                )
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    border = BorderStroke(1.dp, Color(0xFF00FFAA))
                ) {
                    Text("CLOSE_LOG", color = Color(0xFF00FFAA), fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

@Composable
fun DatabaseTab(label: String, isSelected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val theme = LocalNeonTheme.current
    Box(
        modifier = modifier
            .height(40.dp)
            .background(if (isSelected) theme.accent.copy(alpha = 0.1f) else theme.surface)
            .border(1.dp, if (isSelected) theme.accent else theme.inkMuted.copy(alpha = 0.3f))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (isSelected) theme.accent else theme.inkMuted,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun SystemDatabaseContent(isHacked: Boolean, onHackingRequired: () -> Unit, viewModel: JournalViewModel) {
    val shards by viewModel.shards.collectAsState()
    val memories by viewModel.memories.collectAsState()

    if (!isHacked) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.Lock, contentDescription = "Locked", tint = Color.Red, modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(16.dp))
            Text(
                "ENCRYPTION_LEVEL: MAXIMA",
                color = Color.Red,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onHackingRequired,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                modifier = Modifier.border(1.dp, Color(0xFF00FFAA))
            ) {
                Text("INITIATE_TRACE_BYPASS", color = Color(0xFF00FFAA), fontFamily = FontFamily.Monospace)
            }
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item { SectionHeader("ENCRYPTED_SHARDS", "LORE_DROPS") }
            if (shards.isEmpty()) {
                item { EmptyDataPlaceholder("NO_SHARDS_FOUND") }
            } else {
                items(shards) { shard ->
                    ShardItem(shard) { viewModel.decryptShard(shard) }
                }
            }

            item { SectionHeader("CORRUPTED_MEMORIES", "NEURAL_FRAGMENTS") }
            if (memories.isEmpty()) {
                item { EmptyDataPlaceholder("NO_MEMORIES_RECOVERED") }
            } else {
                items(memories) { memory ->
                    MemoryItem(memory)
                }
            }
        }
    }
}

@Composable
fun ShardItem(shard: DataShard, onDecrypt: () -> Unit) {
    val theme = LocalNeonTheme.current
    var isDecrypting by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(isDecrypting) {
        if (isDecrypting) {
            val startTime = System.currentTimeMillis()
            val duration = shard.decryptionTimeMillis
            while (progress < 1f) {
                progress = ((System.currentTimeMillis() - startTime).toFloat() / duration).coerceIn(0f, 1f)
                delay(32)
            }
            isDecrypting = false
            onDecrypt()
        }
    }

    CyberFrame(
        label = if (shard.isDecrypted) "DECRYPTED_DATA" else "ENCRYPTED_SHARD",
        borderColor = if (shard.isDecrypted) Color(0xFF00FFAA) else Color(0xFFFFCC00)
    ) {
        Column {
            Text(shard.title, color = theme.ink, fontWeight = FontWeight.Bold, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
            Spacer(Modifier.height(8.dp))
            if (shard.isDecrypted) {
                Text(shard.content, color = theme.ink.copy(alpha = 0.8f), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            } else {
                if (isDecrypting) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(4.dp),
                        color = Color(0xFF00FFAA),
                        trackColor = theme.inkMuted.copy(alpha = 0.2f)
                    )
                    Text("DECRYPTING...", color = Color(0xFF00FFAA), fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(top = 4.dp))
                } else {
                    Button(
                        onClick = { isDecrypting = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFFFFCC00))
                    ) {
                        Text("START_DECRYPTION", color = Color(0xFFFFCC00), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

@Composable
fun MemoryItem(memory: MemoryFragment) {
    val theme = LocalNeonTheme.current
    val isUnlocked = memory.isUnlocked
    
    CyberFrame(
        label = if (isUnlocked) "MEMORY_RECOVERED" else "CORRUPTED_SECTOR",
        borderColor = if (isUnlocked) Color(0xFF00CCFF) else Color.Red.copy(alpha = 0.5f)
    ) {
        Column {
            Text(memory.title, color = theme.ink, fontWeight = FontWeight.Bold, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
            Spacer(Modifier.height(8.dp))
            if (isUnlocked) {
                Text(memory.decryptedContent, color = theme.ink.copy(alpha = 0.8f), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            } else {
                Text(memory.corruptedContent, color = Color.Red.copy(alpha = 0.4f), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Red, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "REQUIRES: ${memory.requiredStat} >= ${memory.requiredStatValue}",
                        color = Color.Red,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
fun SectionHeader(label: String, status: String, onClick: (() -> Unit)? = null) {
    val theme = LocalNeonTheme.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = theme.accent, fontSize = 12.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
        Text(status, color = theme.accent.copy(alpha = 0.5f), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
    }
}

@Composable
fun AscensionTaskItem(task: AscensionTask, onStart: (() -> Unit)? = null, onToggle: (Boolean) -> Unit) {
    val theme = LocalNeonTheme.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(theme.surface)
            .border(1.dp, theme.accent.copy(alpha = 0.1f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val isCompleted = task.lastCompleted != null && 
            task.lastCompleted!!.atZone(java.time.ZoneId.systemDefault()).toLocalDate() == java.time.LocalDate.now()
            
        Checkbox(
            checked = isCompleted,
            onCheckedChange = onToggle,
            colors = CheckboxDefaults.colors(checkedColor = theme.accent, uncheckedColor = theme.inkMuted)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                task.title,
                color = if (isCompleted) theme.inkMuted else theme.ink,
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace,
                textDecoration = if (isCompleted) TextDecoration.LineThrough else null
            )
            if (task.description.isNotBlank()) {
                Text(
                    task.description,
                    color = theme.inkMuted,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
        
        if (onStart != null && !isCompleted) {
            Button(
                onClick = onStart,
                modifier = Modifier.height(32.dp),
                contentPadding = PaddingValues(horizontal = 12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = theme.accent, contentColor = theme.canvas),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text("START", fontSize = 10.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
fun TaskItem(task: Task, onToggle: (Boolean) -> Unit) {
    val theme = LocalNeonTheme.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0A1015))
            .border(1.dp, Color(0xFF00CCFF).copy(alpha = 0.1f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = task.isCompleted,
            onCheckedChange = onToggle,
            colors = CheckboxDefaults.colors(checkedColor = theme.accent, uncheckedColor = theme.inkMuted)
        )
        Text(
            task.description,
            color = if (task.isCompleted) theme.inkMuted else theme.ink,
            fontSize = 14.sp,
            fontFamily = FontFamily.Monospace,
            textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null
        )
    }
}

@Composable
fun ProtocolItem(protocol: BioProtocolLog, isWorking: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    val theme = LocalNeonTheme.current
    val color = if (isWorking) Color(0xFF00FFAA) else Color(0xFFFF0088)
    CyberFrame(
        label = if (isWorking) "SYNERGY_STABLE" else "NEURAL_REJECTION", 
        borderColor = color.copy(alpha = 0.4f),
        modifier = modifier.clickable { onClick() }
    ) {
        Column {
            Text(
                "ID: ${protocol.protocolId}",
                color = color,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
            Spacer(Modifier.height(4.dp))
            Text(
                protocol.notes ?: "NO_NOTES_ATTACHED",
                color = theme.ink.copy(alpha = 0.8f),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun AscensionMissionQuestItem(mission: AscensionMission, viewModel: JournalViewModel) {
    val theme = LocalNeonTheme.current
    var expanded by remember { mutableStateOf(false) }
    val tasks by viewModel.getTasksForAscensionMission(mission.id).collectAsState(emptyList())

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0A1015))
            .border(1.dp, Color(0xFF00CCFF).copy(alpha = 0.2f))
            .clickable { expanded = !expanded }
            .padding(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(mission.title, color = theme.ink, fontWeight = FontWeight.Bold, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
            Icon(
                if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = Color(0xFF00CCFF)
            )
        }

        if (expanded) {
            Spacer(Modifier.height(12.dp))
            Text(mission.description, color = theme.inkMuted, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            Spacer(Modifier.height(16.dp))
            tasks.forEach { task ->
                AscensionTaskItem(
                    task = task,
                    onToggle = { isChecked ->
                        if (isChecked) {
                            viewModel.completeAscensionTask(task)
                        }
                    }
                )
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@Composable
fun QuestItem(quest: Quest, viewModel: JournalViewModel) {
    val theme = LocalNeonTheme.current
    var expanded by remember { mutableStateOf(false) }
    val tasks by viewModel.getTasksForQuest(quest.id).collectAsState(emptyList())

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0A1015))
            .border(1.dp, Color(0xFF00CCFF).copy(alpha = 0.2f))
            .clickable { expanded = !expanded }
            .padding(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(quest.title, color = theme.ink, fontWeight = FontWeight.Bold, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
            Icon(
                if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = Color(0xFF00CCFF)
            )
        }
        
        if (expanded) {
            Spacer(Modifier.height(12.dp))
            Text(quest.description, color = theme.inkMuted, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            Spacer(Modifier.height(16.dp))
            tasks.forEach { task ->
                TaskItem(task) { viewModel.updateTaskCompletion(task, it) }
                Spacer(Modifier.height(4.dp))
            }
            
            Spacer(Modifier.height(12.dp))
            // AI Assistance Area
            CyberFrame(label = "AI_STRATEGIST", borderColor = Color(0xFFFF0088).copy(alpha = 0.3f)) {
                Column {
                    Text("Need help breaking this down?", color = theme.ink, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = "",
                        onValueChange = {},
                        placeholder = { Text("Ask about this mission...", color = theme.inkMuted, fontSize = 10.sp) },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        textStyle = MaterialTheme.typography.bodySmall.copy(color = theme.ink),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = theme.accent,
                            unfocusedBorderColor = theme.inkMuted.copy(alpha = 0.3f),
                            focusedTextColor = theme.ink,
                            unfocusedTextColor = theme.ink
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun NeuralMemoryListView(memories: List<NeuralMemory>) {
    val theme = LocalNeonTheme.current
    if (memories.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyDataPlaceholder("NO_NEURAL_RECOLLECTIONS")
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(memories) { memory ->
                NeuralMemoryMiniItem(memory)
                HorizontalDivider(color = theme.inkMuted.copy(alpha = 0.1f))
            }
        }
    }
}

@Composable
fun NeuralMemoryMiniItem(memory: NeuralMemory) {
    val theme = LocalNeonTheme.current
    val dateFormat = remember { SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.US) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "[${memory.wing} // ${memory.room}]",
                color = if (memory.wing == "INSIGHTS") Color(0xFFFF0088) else Color(0xFF00F5FF),
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Text(
                dateFormat.format(Date(memory.timestamp)),
                color = theme.inkMuted,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            memory.content,
            color = theme.ink.copy(alpha = 0.8f),
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            lineHeight = 16.sp
        )
    }
}

@Composable
fun JournalEntryMiniItem(entry: JournalEntry, onClick: () -> Unit) {
    val theme = LocalNeonTheme.current
    val dateFormat = remember { SimpleDateFormat("yyyy.MM.dd", Locale.US) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(
                "[${entry.category.uppercase()}]",
                color = Color(0xFF00FFAA),
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Text(
                entry.text.replace("\n", " "),
                color = theme.ink.copy(alpha = 0.7f),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            dateFormat.format(Date(entry.timestamp)),
            color = theme.inkMuted,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun EmptyDataPlaceholder(message: String) {
    val theme = LocalNeonTheme.current
    Text(
        message,
        color = theme.inkMuted,
        fontSize = 10.sp,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
private fun AscensionDirectiveSummaryCard(directive: AscensionDirective, onClick: () -> Unit) {
    val theme = LocalNeonTheme.current
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = theme.surfaceRaised),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(directive.title, fontWeight = FontWeight.Bold, color = theme.accent)
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { directive.currentProgress },
                modifier = Modifier.fillMaxWidth(),
                color = theme.secondary,
                trackColor = theme.ink.copy(alpha = 0.1f)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "${(directive.currentProgress * 100).toInt()}% • ${directive.archetypeTag ?: "GENERAL"}",
                style = MaterialTheme.typography.labelSmall,
                color = theme.ink.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun AscensionMissionSummaryCard(mission: AscensionMission, onClick: () -> Unit) {
    val theme = LocalNeonTheme.current
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = theme.surfaceRaised),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(mission.title, color = theme.accent, fontWeight = FontWeight.Medium)
                Text(
                    mission.description.take(80) + if (mission.description.length > 80) "..." else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = theme.ink.copy(alpha = 0.7f)
                )
            }
            Spacer(Modifier.width(16.dp))
            CircularProgressIndicator(
                progress = { mission.progress },
                color = theme.accent,
                modifier = Modifier.size(48.dp),
                trackColor = theme.ink.copy(alpha = 0.1f)
            )
        }
    }
}

@Composable
private fun AspirationSummaryCard(aspiration: Aspiration, onClick: () -> Unit) {
    val theme = LocalNeonTheme.current
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = theme.surfaceRaised)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(aspiration.title, fontWeight = FontWeight.Bold, color = theme.accent)
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { aspiration.progress.current },
                modifier = Modifier.fillMaxWidth(),
                color = theme.secondary,
                trackColor = theme.ink.copy(alpha = 0.1f)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "${(aspiration.progress.current * 100).toInt()}% • ${aspiration.linkedAttributes.joinToString { it.getIcon() }}",
                style = MaterialTheme.typography.labelSmall,
                color = theme.ink.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun MissionSummaryCard(mission: Mission, onClick: () -> Unit) {
    val theme = LocalNeonTheme.current
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = theme.surfaceRaised),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(mission.title, color = theme.secondary, fontWeight = FontWeight.Medium)
                Text(
                    mission.description.take(80) + if (mission.description.length > 80) "..." else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = theme.ink.copy(alpha = 0.7f)
                )
            }
            Spacer(Modifier.width(16.dp))
            CircularProgressIndicator(
                progress = { mission.progress.current },
                color = theme.accent,
                modifier = Modifier.size(48.dp),
                trackColor = theme.ink.copy(alpha = 0.1f)
            )
        }
    }
}

@Composable
private fun EmptyStateCard(
    message: String,
    buttonText: String,
    onClick: () -> Unit
) {
    val theme = LocalNeonTheme.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = theme.surfaceRaised)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = message,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                color = theme.ink.copy(alpha = 0.6f)
            )
            OutlinedButton(
                onClick = onClick,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan)
            ) {
                Text(buttonText)
            }
        }
    }
}
