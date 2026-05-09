package com.neon.ascent.feature.habits.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neon.ascent.core.common.NeonCyan
import com.neon.ascent.core.common.NeonPink
import com.neon.ascent.core.domain.goals.models.Habit
import com.neon.ascent.core.domain.goals.models.Mission

@Composable
fun HabitListScreen(
    viewModel: HabitsViewModel = hiltViewModel()
) {
    val habits by viewModel.habits.collectAsState()
    val todayMissions by viewModel.todayMissions.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("DAILY MISSIONS", style = MaterialTheme.typography.headlineMedium, color = NeonCyan)
        }
        items(todayMissions) { mission ->
            MissionCard(mission)
        }

        item {
            Text("RECURRENT HABITS", style = MaterialTheme.typography.headlineMedium, color = NeonPink)
        }
        items(habits) { habit ->
            HabitCard(
                habit = habit,
                onComplete = { viewModel.completeHabit(habit.id) }
            )
        }
    }
}

@Composable
fun MissionCard(mission: Mission) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(mission.title, style = MaterialTheme.typography.titleMedium, color = Color.White)
            Text(mission.description, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            LinearProgressIndicator(
                progress = { mission.progress.current },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                color = NeonCyan
            )
        }
    }
}

@Composable
fun HabitCard(habit: Habit, onComplete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(habit.title, style = MaterialTheme.typography.titleMedium, color = Color.White)
                Text("Streak: ${habit.streak} 🔥", style = MaterialTheme.typography.bodySmall, color = NeonPink)
            }
            Button(onClick = onComplete) {
                Text("COMPLETE")
            }
        }
    }
}
