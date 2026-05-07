package com.neon.ascent.feature.goals

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AspirationsScreen(
    viewModel: AspirationsViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val aspirations by viewModel.aspirations.collectAsState()
    val yearlyReviewEnabled by viewModel.yearlyReviewEnabled.collectAsState()
    var newAspirationText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Aspirations Management") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Your Grand Aspirations define the direction of your journey.",
                style = MaterialTheme.typography.bodyMedium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newAspirationText,
                    onValueChange = { newAspirationText = it },
                    label = { Text("New Aspiration") },
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = {
                    if (newAspirationText.isNotBlank()) {
                        viewModel.addAspiration(newAspirationText)
                        newAspirationText = ""
                    }
                }) {
                    Icon(Icons.Default.Add, "Add")
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(aspirations) { aspiration ->
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(aspiration, style = MaterialTheme.typography.titleMedium)
                            IconButton(onClick = { viewModel.removeAspiration(aspiration) }) {
                                Icon(Icons.Default.Delete, "Remove")
                            }
                        }
                    }
                }
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Yearly Review Prompt", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Automatically prompt for an aspirations review every year.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Switch(
                    checked = yearlyReviewEnabled,
                    onCheckedChange = { viewModel.toggleYearlyReview(it) }
                )
            }
        }
    }
}
