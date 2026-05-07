package com.neon.ascent.feature.goals

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.neon.ascent.domain.model.Goal

@Composable
fun SuggestionGoalCard(goal: Goal, onSelect: (Goal) -> Unit) {
    Card(
        modifier = Modifier
            .width(240.dp)
            .clickable { onSelect(goal) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(goal.title, style = MaterialTheme.typography.titleMedium)
            Text(goal.description, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
            Spacer(Modifier.height(8.dp))
            Text("${goal.targetValue} ${goal.unit}", style = MaterialTheme.typography.labelMedium)
        }
    }
}
