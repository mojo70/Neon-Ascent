package com.neon.ascent.feature.journal

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.neon.ascent.model.*
import com.neon.ascent.ui.CyberFrame
import com.neon.ascent.ui.Scanlines
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun JournalScreen(
    onEntryClick: (JournalEntry) -> Unit,
    onStoryClick: () -> Unit,
    onBack: () -> Unit = {},
    onHackingRequired: () -> Unit = {},
    viewModel: JournalViewModel = hiltViewModel()
) {
    var activeTab by remember { mutableIntStateOf(0) }
    val isHacked by viewModel.isSystemDatabaseHacked.collectAsState()
    var selectedEntry by remember { mutableStateOf<JournalEntry?>(null) }
    var selectedProtocol by remember { mutableStateOf<BioProtocolLog?>(null) }
    val searchQuery by viewModel.searchQuery.collectAsState()
    
    // Sub-navigation within tabs
    var currentView by remember { mutableStateOf(DatabaseView.OVERVIEW) }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF020508))) {
        Scanlines()
        
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (currentView != DatabaseView.OVERVIEW) {
                        IconButton(onClick = { currentView = DatabaseView.OVERVIEW }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF00FFAA))
                        }
                    }
                    Text(
                        if (currentView == DatabaseView.OVERVIEW) "DATABASE_NODE // V.4.0" else currentView.name,
                        color = Color(0xFF00FFAA),
                        fontSize = 20.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Red)
                }
            }

            Spacer(Modifier.height(16.dp))

            if (currentView == DatabaseView.OVERVIEW) {
                // Tab Selector
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DatabaseTab("YOUR_DATA", activeTab == 0, Modifier.weight(1f)) { activeTab = 0 }
                    DatabaseTab("SYSTEM_DATA", activeTab == 1, Modifier.weight(1f)) { activeTab = 1 }
                }
                Spacer(Modifier.height(24.dp))
            } else {
                // Search Bar for dedicated views
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    placeholder = { Text("FILTER_DATABASE...", color = Color.Gray, fontSize = 12.sp, fontFamily = FontFamily.Monospace) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF00FFAA)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = null, tint = Color.Gray)
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00FFAA),
                        unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f),
                        cursorColor = Color(0xFF00FFAA)
                    ),
                    textStyle = LocalTextStyle.current.copy(color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 14.sp)
                )
            }

            // Content Area
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (currentView) {
                    DatabaseView.OVERVIEW -> {
                        if (activeTab == 0) {
                            PersonalDatabaseOverview(
                                viewModel = viewModel,
                                onJournalClick = { currentView = DatabaseView.NEURAL_JOURNAL },
                                onBioClick = { currentView = DatabaseView.BIO_PROTOCOLS },
                                onEntryClick = { selectedEntry = it }
                            )
                        } else {
                            SystemDatabaseContent(isHacked, onHackingRequired, viewModel)
                        }
                    }
                    DatabaseView.NEURAL_JOURNAL -> {
                        val entries by viewModel.entries.collectAsState()
                        JournalListView(entries) { selectedEntry = it }
                    }
                    DatabaseView.BIO_PROTOCOLS -> {
                        val protocols by viewModel.bioProtocolLogs.collectAsState()
                        ProtocolListView(protocols) { selectedProtocol = it }
                    }
                }
            }
            
            if (activeTab == 1 && currentView == DatabaseView.OVERVIEW) {
                Button(
                    onClick = onStoryClick,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    border = BorderStroke(1.dp, Color(0xFF00FFAA))
                ) {
                    Text("FULL_ARCHIVE_VIEW", color = Color(0xFF00FFAA), fontFamily = FontFamily.Monospace)
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

enum class DatabaseView {
    OVERVIEW, NEURAL_JOURNAL, BIO_PROTOCOLS
}

@Composable
fun PersonalDatabaseOverview(
    viewModel: JournalViewModel,
    onJournalClick: () -> Unit,
    onBioClick: () -> Unit,
    onEntryClick: (JournalEntry) -> Unit
) {
    val entries by viewModel.entries.collectAsState()
    val protocols by viewModel.bioProtocolLogs.collectAsState()
    val quests by viewModel.quests.collectAsState()
    val dailyTasks by viewModel.dailyTasks.collectAsState()
    
    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { SectionHeader("DAILY_OBJECTIVES", "ACTIVE") }
        items(dailyTasks) { task ->
            TaskItem(task) { viewModel.updateTaskCompletion(task, it) }
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
        
        item { SectionHeader("CAMPAIGN_DATA", "LONG_TERM") }
        items(quests) { quest ->
            QuestItem(quest, viewModel)
        }
        
        item { Spacer(Modifier.height(32.dp)) }
    }
}

@Composable
fun JournalListView(entries: List<JournalEntry>, onEntryClick: (JournalEntry) -> Unit) {
    if (entries.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyDataPlaceholder("NO_MATCHING_LOGS")
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(entries) { entry ->
                JournalEntryMiniItem(entry) { onEntryClick(entry) }
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
            }
        }
    }
}

@Composable
fun ProtocolListView(protocols: List<BioProtocolLog>, onProtocolClick: (BioProtocolLog) -> Unit) {
    val stable = protocols.filter { it.isWorking == true }
    val rejected = protocols.filter { it.isWorking == false }
    val unrated = protocols.filter { it.isWorking == null }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
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
                CyberFrame(label = "UNRATED_LOG", borderColor = Color.Gray) {
                    Text("Protocol ${protocol.protocolId} - Pending Rating", color = Color.Gray, fontSize = 12.sp)
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
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        CyberFrame(label = "PROTOCOL_LOG // ${protocol.protocolId}") {
            Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                Text("STABILITY_RATING: ${if (protocol.isWorking == true) "STABLE" else if (protocol.isWorking == false) "REJECTED" else "UNRATED"}", color = if (protocol.isWorking == true) Color(0xFF00FFAA) else Color.Red)
                Spacer(Modifier.height(12.dp))
                Text("NOTES:", color = Color(0xFF00FFAA), fontSize = 10.sp)
                Text(protocol.notes ?: "NO_NOTES", color = Color.White, fontSize = 14.sp)
                if (protocol.sideEffects != null) {
                    Spacer(Modifier.height(12.dp))
                    Text("SIDE_EFFECTS:", color = Color.Red, fontSize = 10.sp)
                    Text(protocol.sideEffects, color = Color.White, fontSize = 14.sp)
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
                    color = Color.White,
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
fun StoryScreen(
    onBack: () -> Unit,
    onHackingRequired: () -> Unit = {},
    viewModel: JournalViewModel = hiltViewModel()
) {
    val isHacked by viewModel.isSystemDatabaseHacked.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF020508))) {
        Scanlines()
        
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF00FFAA))
                }
                Text(
                    "SYSTEM_ARCHIVE",
                    color = Color(0xFF00FFAA),
                    fontSize = 20.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.width(48.dp))
            }

            Spacer(Modifier.height(24.dp))

            // Content Area
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                SystemDatabaseContent(isHacked, onHackingRequired, viewModel)
            }
        }
    }
}

@Composable
fun DatabaseTab(label: String, isSelected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(40.dp)
            .background(if (isSelected) Color(0xFF00FFAA).copy(alpha = 0.1f) else Color.Transparent)
            .border(1.dp, if (isSelected) Color(0xFF00FFAA) else Color.Gray.copy(alpha = 0.3f))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (isSelected) Color(0xFF00FFAA) else Color.Gray,
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
            Text(shard.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
            Spacer(Modifier.height(8.dp))
            if (shard.isDecrypted) {
                Text(shard.content, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            } else {
                if (isDecrypting) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(4.dp),
                        color = Color(0xFF00FFAA),
                        trackColor = Color.Gray.copy(alpha = 0.2f)
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
    val isUnlocked = memory.isUnlocked // In a real app, check user stats here
    
    CyberFrame(
        label = if (isUnlocked) "MEMORY_RECOVERED" else "CORRUPTED_SECTOR",
        borderColor = if (isUnlocked) Color(0xFF00CCFF) else Color.Red.copy(alpha = 0.5f)
    ) {
        Column {
            Text(memory.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
            Spacer(Modifier.height(8.dp))
            if (isUnlocked) {
                Text(memory.decryptedContent, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color(0xFF00CCFF), fontSize = 12.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
        Text(status, color = Color(0xFF00CCFF).copy(alpha = 0.5f), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
    }
}

@Composable
fun TaskItem(task: Task, onToggle: (Boolean) -> Unit) {
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
            colors = CheckboxDefaults.colors(checkedColor = Color(0xFF00FFAA), uncheckedColor = Color.Gray)
        )
        Text(
            task.description,
            color = if (task.isCompleted) Color.Gray else Color.White,
            fontSize = 14.sp,
            fontFamily = FontFamily.Monospace,
            textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null
        )
    }
}

@Composable
fun ProtocolItem(protocol: BioProtocolLog, isWorking: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
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
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun QuestItem(quest: Quest, viewModel: JournalViewModel) {
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
            Text(quest.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
            Icon(
                if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = Color(0xFF00CCFF)
            )
        }
        
        if (expanded) {
            Spacer(Modifier.height(12.dp))
            Text(quest.description, color = Color.Gray, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            Spacer(Modifier.height(16.dp))
            tasks.forEach { task ->
                TaskItem(task) { viewModel.updateTaskCompletion(task, it) }
                Spacer(Modifier.height(4.dp))
            }
            
            Spacer(Modifier.height(12.dp))
            // AI Assistance Area
            CyberFrame(label = "AI_STRATEGIST", borderColor = Color(0xFFFF0088).copy(alpha = 0.3f)) {
                Column {
                    Text("Need help breaking this down?", color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = "",
                        onValueChange = {},
                        placeholder = { Text("Ask about this mission...", color = Color.Gray, fontSize = 10.sp) },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        textStyle = MaterialTheme.typography.bodySmall.copy(color = Color.White),
                        colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f))
                    )
                }
            }
        }
    }
}

@Composable
fun JournalEntryMiniItem(entry: JournalEntry, onClick: () -> Unit) {
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
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            dateFormat.format(Date(entry.timestamp)),
            color = Color.Gray,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun EmptyDataPlaceholder(message: String) {
    Text(
        message,
        color = Color.Gray,
        fontSize = 10.sp,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}
