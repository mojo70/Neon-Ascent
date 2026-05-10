package com.neon.ascent.feature.goals.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neon.ascent.core.common.*
import com.neon.ascent.core.domain.model.SpecialType
import java.time.LocalDate

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AspirationCreationScreen(
    onCreated: () -> Unit,
    onCancel: () -> Unit,
    viewModel: AspirationCreationViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        Text(
            text = "NEW ASCENSION PROTOCOL",
            style = MaterialTheme.typography.headlineLarge,
            color = NeonCyan,
            fontWeight = FontWeight.Bold
        )

        Text(
            "Define your long-term directive. The deck will generate missions to support it.",
            color = Color.White.copy(alpha = 0.75f)
        )

        // Title
        OutlinedTextField(
            value = state.title,
            onValueChange = viewModel::updateTitle,
            label = { Text("Aspiration / Endgame Directive") },
            placeholder = { Text("Become a Tier-1 Netrunner") },
            modifier = Modifier.fillMaxWidth(),
            colors = neonTextFieldColors()
        )

        // Description
        OutlinedTextField(
            value = state.description,
            onValueChange = viewModel::updateDescription,
            label = { Text("Core Objective") },
            placeholder = { Text("Master on-device AI systems and achieve financial sovereignty...") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 4,
            colors = neonTextFieldColors()
        )

        // Target Date (Optional)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("TARGET ASCENSION DATE (OPTIONAL)", color = NeonPink)
            Spacer(Modifier.weight(1f))
            Switch(
                checked = state.hasTargetDate,
                onCheckedChange = viewModel::toggleTargetDate
            )
        }

        if (state.hasTargetDate) {
            // Simple date picker can be expanded later
            OutlinedTextField(
                value = state.targetDate?.toString() ?: LocalDate.now().plusMonths(6).toString(),
                onValueChange = { },
                label = { Text("Target Date") },
                modifier = Modifier.fillMaxWidth(),
                enabled = false,
                colors = neonTextFieldColors()
            )
        }

        // Linked S.P.E.C.I.A.L. Attributes
        Text(
            "PRIMARY ATTRIBUTES THIS ASCENSION TARGETS",
            color = NeonPink,
            fontWeight = FontWeight.Medium
        )

        SpecialAttributeMultiSelect(
            selected = state.linkedAttributes,
            onSelectionChanged = viewModel::updateLinkedAttributes
        )

        Spacer(Modifier.weight(1f))

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f)
            ) {
                Text("ABORT DIRECTIVE")
            }

            Button(
                onClick = {
                    viewModel.saveAspiration()
                    onCreated()
                },
                enabled = state.isValid,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
            ) {
                Text("DEPLOY ASCENSION PROTOCOL")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SpecialAttributeMultiSelect(
    selected: List<SpecialType>,
    onSelectionChanged: (List<SpecialType>) -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SpecialType.entries.forEach { type ->
            val isSelected = type in selected
            val color = getNeonColorForAttribute(type)
            FilterChip(
                selected = isSelected,
                onClick = {
                    val newList = if (isSelected) selected - type else selected + type
                    onSelectionChanged(newList)
                },
                label = { Text("${type.getIcon()} ${type.name}") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = color.copy(alpha = 0.5f),
                    labelColor = Color.White.copy(alpha = 0.7f),
                    selectedLabelColor = color
                )
            )
        }
    }
}
