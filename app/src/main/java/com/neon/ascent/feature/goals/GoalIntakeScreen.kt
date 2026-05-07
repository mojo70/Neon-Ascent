package com.neon.ascent.feature.goals

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neon.ascent.domain.model.SpecialType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalIntakeScreen(
    viewModel: GoalIntakeViewModel = hiltViewModel(),
    onGoalCreated: () -> Unit,
    onCancel: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val suggestions by viewModel.suggestedGoals.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Forge New Quest") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.Close, "Cancel")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { 
                viewModel.createGoal()
                onGoalCreated() 
            }) {
                Icon(Icons.Default.Check, "Create Goal")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Suggested Goals (Quick Start)
            if (suggestions.isNotEmpty()) {
                Text("Suggested Quests", style = MaterialTheme.typography.titleMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(suggestions) { suggestion ->
                        SuggestionGoalCard(
                            goal = suggestion,
                            onSelect = { viewModel.applySuggestion(it) }
                        )
                    }
                }
            }

            // Manual Goal Creation
            Text("Create Custom Quest", style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = state.title,
                onValueChange = viewModel::updateTitle,
                label = { Text("Quest Title") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("e.g. 10,000 Hour Meditation Mastery") }
            )

            OutlinedTextField(
                value = state.description,
                onValueChange = viewModel::updateDescription,
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = state.targetValue,
                    onValueChange = viewModel::updateTargetValue,
                    label = { Text("Target") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                OutlinedTextField(
                    value = state.unit,
                    onValueChange = viewModel::updateUnit,
                    label = { Text("Unit") },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("hours / days / levels") }
                )
            }

            // Linked Aspiration & SPECIAL
            AspirationPicker(
                selected = state.aspirationLink,
                onSelected = viewModel::updateAspirationLink
            )

            SpecialStatPicker(
                selected = state.linkedSpecial,
                onSelected = viewModel::updateLinkedSpecial
            )
        }
    }
}

@Composable
fun AspirationPicker(
    selected: String,
    onSelected: (String) -> Unit
) {
    // Basic implementation for now
    OutlinedTextField(
        value = selected,
        onValueChange = onSelected,
        label = { Text("Linked Aspiration") },
        modifier = Modifier.fillMaxWidth()
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpecialStatPicker(
    selected: SpecialType?,
    onSelected: (SpecialType?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selected?.name ?: "No SPECIAL Link",
            onValueChange = {},
            readOnly = true,
            label = { Text("Linked SPECIAL Stat") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("None") },
                onClick = {
                    onSelected(null)
                    expanded = false
                }
            )
            SpecialType.entries.forEach { type ->
                DropdownMenuItem(
                    text = { Text(type.name) },
                    onClick = {
                        onSelected(type)
                        expanded = false
                    }
                )
            }
        }
    }
}
