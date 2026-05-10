package com.neon.ascent.feature.habits.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neon.ascent.core.common.*
import com.neon.ascent.core.domain.goals.models.RecurrenceType
import com.neon.ascent.core.domain.model.SpecialType

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HabitCreationBottomSheet(
    onDismiss: () -> Unit,
    viewModel: HabitCreationViewModel = hiltViewModel()
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val uiState by viewModel.uiState.collectAsState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF0A001F)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = "NEW PROTOCOL",
                style = MaterialTheme.typography.headlineMedium,
                color = NeonCyan
            )

            // Title
            OutlinedTextField(
                value = uiState.title,
                onValueChange = viewModel::updateTitle,
                label = { Text("Habit / Protocol Name") },
                placeholder = { Text("e.g. Morning Neural Focus") },
                modifier = Modifier.fillMaxWidth(),
                colors = neonTextFieldColors()
            )

            // Description
            OutlinedTextField(
                value = uiState.description,
                onValueChange = viewModel::updateDescription,
                label = { Text("Description / Objective") },
                placeholder = { Text("30 minutes deep work with no distractions") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                colors = neonTextFieldColors()
            )

            // Recurrence
            RecurrenceSelector(
                selectedType = uiState.recurrenceType,
                onTypeSelected = viewModel::updateRecurrenceType
            )

            // Linked S.P.E.C.I.A.L. Attributes
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("LINK TO S.P.E.C.I.A.L. ATTRIBUTES", color = NeonPink, fontWeight = FontWeight.Medium)
                SpecialAttributeChips(
                    selected = uiState.linkedAttributes,
                    onSelectionChanged = viewModel::updateLinkedAttributes
                )
            }

            Spacer(Modifier.height(8.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("CANCEL")
                }

                Button(
                    onClick = {
                        viewModel.saveHabit()
                        onDismiss()
                    },
                    enabled = uiState.isValid,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black)
                ) {
                    Text("DEPLOY PROTOCOL")
                }
            }
        }
    }
}

@Composable
private fun RecurrenceSelector(
    selectedType: RecurrenceType,
    onTypeSelected: (RecurrenceType) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        RecurrenceType.entries.forEach { type ->
            FilterChip(
                selected = type == selectedType,
                onClick = { onTypeSelected(type) },
                label = { Text(type.name) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = NeonCyan.copy(alpha = 0.3f),
                    labelColor = Color.White,
                    selectedLabelColor = NeonCyan
                )
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SpecialAttributeChips(
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
                    selectedContainerColor = color.copy(alpha = 0.4f),
                    labelColor = Color.White.copy(alpha = 0.7f),
                    selectedLabelColor = color
                )
            )
        }
    }
}
