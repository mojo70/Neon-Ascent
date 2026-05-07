package com.neon.ascent.feature.goals

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neon.ascent.domain.model.Goal
import com.neon.ascent.domain.model.SpecialType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalIntakeScreen(
    viewModel: GoalIntakeViewModel = hiltViewModel(),
    onGoalCreated: () -> Unit,
    onManageAspirations: () -> Unit,
    onCancel: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val suggestions by viewModel.suggestedGoals.collectAsState()

    Scaffold(
        topBar = { 
            TopAppBar(
                title = { Text("Forge New Mission") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        viewModel.createGoal()
                        onGoalCreated() 
                    }) {
                        Icon(Icons.Default.Check, "Forge Mission")
                    }
                }
            ) 
        },
        bottomBar = {
            // Terminal AI Chat - always visible
            LocalAiTerminal(
                onSend = { message -> viewModel.sendToLocalAI(message) },
                messages = state.aiMessages
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            // Mission Title / Objective
            OutlinedTextField(
                value = state.title,
                onValueChange = viewModel::updateTitle,
                label = { Text("Mission Name") },
                placeholder = { Text("e.g. Get Closer to God") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = state.objective,
                onValueChange = viewModel::updateObjective,
                label = { Text("Objective") },
                placeholder = { Text("What does success look like? (e.g. Pray 1 hour daily)") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            // Linked Aspiration (Dropdown + Add New)
            AspirationDropdown(
                currentAspiration = state.aspirationLink,
                allAspirations = state.allAspirations,
                onAspirationSelected = viewModel::updateAspirationLink,
                onCreateNewAspiration = viewModel::createNewAspiration,
                onManageAspirations = onManageAspirations
            )

            // Special Stat Picker
            SpecialStatPicker(
                selected = state.linkedSpecial,
                onSelected = viewModel::updateLinkedSpecial
            )

            // Suggested Missions (quick start)
            if (suggestions.isNotEmpty()) {
                Text("Suggested Missions", style = MaterialTheme.typography.titleMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(suggestions) { sug ->
                        SuggestionMissionCard(suggestion = sug, onSelect = viewModel::applySuggestion)
                    }
                }
            }

            Spacer(Modifier.weight(1f))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AspirationDropdown(
    currentAspiration: String,
    allAspirations: List<String>,
    onAspirationSelected: (String) -> Unit,
    onCreateNewAspiration: (String) -> Unit,
    onManageAspirations: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var newAspirationText by remember { mutableStateOf("") }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = currentAspiration.ifEmpty { "No Aspiration Linked" },
            onValueChange = {},
            readOnly = true,
            label = { Text("Linked Aspiration") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )

        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            allAspirations.forEach { asp ->
                DropdownMenuItem(
                    text = { Text(asp) }, 
                    onClick = {
                        onAspirationSelected(asp)
                        expanded = false
                    }
                )
            }
            HorizontalDivider()
            // "Manage" option
            DropdownMenuItem(
                text = { Text("MANAGE_ASPIRATIONS", color = MaterialTheme.colorScheme.primary) },
                onClick = {
                    onManageAspirations()
                    expanded = false
                }
            )
            HorizontalDivider()
            // "Create New" option
            Column(modifier = Modifier.padding(8.dp)) {
                OutlinedTextField(
                    value = newAspirationText,
                    onValueChange = { newAspirationText = it },
                    label = { Text("Quick Add Aspiration") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        if (newAspirationText.isNotBlank()) {
                            onCreateNewAspiration(newAspirationText)
                            newAspirationText = ""
                            expanded = false
                        }
                    }
                ) {
                    Text("Add to Aspirations")
                }
            }
        }
    }
}

@Composable
fun LocalAiTerminal(
    messages: List<AiMessage>,
    onSend: (String) -> Unit
) {
    var input by remember { mutableStateOf("") }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.8f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Mini header
            Text(
                "Local AI Terminal — Ask anything", 
                style = MaterialTheme.typography.labelSmall,
                color = Color.Green
            )
            Spacer(Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.height(120.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(messages) { msg ->
                    Text(
                        text = if (msg.isUser) "> ${msg.content}" else msg.content,
                        color = if (msg.isUser) Color.Cyan else Color.White,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Row {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Ask AI for ideas...", style = MaterialTheme.typography.bodySmall) },
                    textStyle = MaterialTheme.typography.bodySmall
                )
                IconButton(onClick = {
                    if (input.isNotBlank()) {
                        onSend(input)
                        input = ""
                    }
                }) {
                    Icon(Icons.Default.Send, "Send", tint = Color.Green)
                }
            }
        }
    }
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

@Composable
fun SuggestionMissionCard(suggestion: Goal, onSelect: (Goal) -> Unit) {
    Card(
        modifier = Modifier
            .width(240.dp)
            .clickable { onSelect(suggestion) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(suggestion.title, style = MaterialTheme.typography.titleMedium)
            Text(suggestion.objective, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
            Spacer(Modifier.height(8.dp))
            Text(suggestion.description, style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }
    }
}
