package com.neon.ascent.feature.journal

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neon.ascent.model.*
import com.neon.ascent.ui.CyberFrame
import com.neon.ascent.ui.Scanlines
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun JournalScreen(
    onBack: () -> Unit,
    onHackingRequired: () -> Unit,
    viewModel: JournalViewModel = hiltViewModel()
) {
    var activeTab by remember { mutableIntStateOf(0) }
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
                Text(
                    "DATABASE_NODE // V.4.0",
                    color = Color(0xFF00FFAA),
                    fontSize = 20.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Red)
                }
            }

            Spacer(Modifier.height(16.dp))

            // Tab Selector
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DatabaseTab("YOUR_DATA", activeTab == 0, Modifier.weight(1f)) { activeTab = 0 }
                DatabaseTab("SYSTEM_DATA", activeTab == 1, Modifier.weight(1f)) { activeTab = 1 }
            }

            Spacer(Modifier.height(24.dp))

            // Content Area
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (activeTab == 0) {
                    PersonalDatabaseContent(viewModel)
                } else {
                    SystemDatabaseContent(isHacked, onHackingRequired)
                }
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
fun PersonalDatabaseContent(viewModel: JournalViewModel) {
    val entries by viewModel.entries.collectAsState()
    val protocols by viewModel.bioProtocolLogs.collectAsState()
    val quests by viewModel.quests.collectAsState()
    val dailyTasks by viewModel.dailyTasks.collectAsState()
    
    var expandedSection by remember { mutableStateOf<String?>(null) }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // 1. Daily Tasks (Priority)
        item {
            SectionHeader("DAILY_OBJECTIVES", "ACTIVE")
        }
        items(dailyTasks) { task ->
            TaskItem(task) { viewModel.updateTaskCompletion(task, it) }
        }

        // 2. Journal Saves
        item {
            SectionHeader("NEURAL_JOURNAL", "${entries.size} LOGS")
        }
        if (entries.isEmpty()) {
            item { EmptyDataPlaceholder("NO_ENTRIES_FOUND") }
        } else {
            items(entries.take(3)) { entry ->
                JournalEntryMiniItem(entry)
            }
        }

        // 3. Biohacking Protocols
        item {
            SectionHeader("BIO_PROTOCOLS", "VERIFIED")
        }
        items(protocols.filter { it.isWorking == true }) { protocol ->
            ProtocolItem(protocol, true)
        }
        item {
            SectionHeader("BIO_PROTOCOLS", "FAILED_SYNERGY")
        }
        items(protocols.filter { it.isWorking == false }) { protocol ->
            ProtocolItem(protocol, false)
        }

        // 4. Long Term Quests
        item {
            SectionHeader("CAMPAIGN_DATA", "LONG_TERM")
        }
        items(quests) { quest ->
            QuestItem(quest, viewModel)
        }
        
        item { Spacer(Modifier.height(32.dp)) }
    }
}

@Composable
fun SystemDatabaseContent(isHacked: Boolean, onHackingRequired: () -> Unit) {
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
        // Hacked system content
        Column {
            Text("SYSTEM_ARCHIVE_ACCESS: GRANTED", color = Color(0xFF00FFAA), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            Spacer(Modifier.height(16.dp))
            // Placeholder for system data
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(listOf("ARASAKA_PERSONNEL_FILES", "NIGHT_CITY_INFRASTRUCTURE", "BIOTEC_LEAKS_2077")) { file ->
                    CyberFrame(label = "FILE_NODE") {
                        Text(file, color = Color.White, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(label: String, status: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
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
fun ProtocolItem(protocol: BioProtocolLog, isWorking: Boolean) {
    val color = if (isWorking) Color(0xFF00FFAA) else Color(0xFFFF0088)
    CyberFrame(label = if (isWorking) "SYNERGY_STABLE" else "NEURAL_REJECTION", borderColor = color.copy(alpha = 0.4f)) {
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
fun JournalEntryMiniItem(entry: JournalEntry) {
    val dateFormat = remember { SimpleDateFormat("yyyy.MM.dd", Locale.US) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            "> ${entry.text.take(30)}...",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )
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
