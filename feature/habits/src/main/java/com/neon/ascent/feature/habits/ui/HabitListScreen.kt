package com.neon.ascent.feature.habits.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neon.ascent.core.common.*
import com.neon.ascent.core.domain.goals.models.AscensionMission
import com.neon.ascent.core.domain.goals.models.AscensionTask

@Composable
fun HabitListScreen(
    viewModel: HabitsViewModel = hiltViewModel()
) {
    val tasks by viewModel.recurringTasks.collectAsState()
    val todayMissions by viewModel.activeMissions.collectAsState()
    val todayProgress by viewModel.todayProgress.collectAsState()

    var showCreationSheet by remember { mutableStateOf(false) }

    if (showCreationSheet) {
        HabitCreationBottomSheet(onDismiss = { showCreationSheet = false })
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Header
        item {
            Column {
                Text(
                    text = "DAILY DECK PROTOCOL",
                    style = MaterialTheme.typography.headlineLarge,
                    color = NeonCyan
                )
                Text(
                    text = todayProgress,
                    color = NeonPink,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        // Today's Missions
        if (todayMissions.isNotEmpty()) {
            item {
                Text(
                    text = "ACTIVE MISSIONS",
                    style = MaterialTheme.typography.titleMedium,
                    color = NeonPink
                )
            }
            items(todayMissions) { mission ->
                MissionCard(mission)
            }
        }

        // Recurrent Habits
        item {
            Text(
                text = "RECURRENT HABITS",
                style = MaterialTheme.typography.titleMedium,
                color = NeonCyan
            )
        }

        items(tasks) { task ->
            TaskCard(
                task = task,
                onComplete = { viewModel.completeTask(task) }
            )
        }

        // Quick Add
        item {
            OutlinedButton(
                onClick = { showCreationSheet = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("+ CREATE NEW HABIT")
            }
        }
    }
}

@Composable
fun TaskCard(task: AscensionTask, onComplete: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A0033)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(task.title, fontWeight = FontWeight.Bold)
                Text(
                    text = "${task.currentStreak} day streak 🔥",
                    color = if (task.currentStreak > 3) NeonOrange else Color.White.copy(alpha = 0.7f)
                )
            }

            Button(
                onClick = onComplete,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (task.lastCompleted != null) NeonGreen else NeonCyan
                )
            ) {
                Text(if (task.lastCompleted != null) "COMPLETE" else "LOG")
            }
        }
    }
}

@Composable
fun MissionCard(mission: AscensionMission) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F001A))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(mission.title, fontWeight = FontWeight.Bold, color = NeonPink)
            Text(mission.description, color = Color.White.copy(alpha = 0.8f))
            LinearProgressIndicator(
                progress = { mission.progress },
                modifier = Modifier.fillMaxWidth(),
                color = NeonCyan
            )
        }
    }
}
