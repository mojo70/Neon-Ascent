package com.neon.ascent.feature.workout.ui.arsenal

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neon.ascent.core.common.LocalNeonTheme
import com.neon.ascent.core.domain.workout.models.Exercise
import com.neon.ascent.core.domain.workout.models.Implement
import com.neon.ascent.core.domain.workout.models.MovementType
import com.neon.ascent.core.domain.workout.models.Stance
import java.util.UUID

enum class CustomCreationType { VARIANT, FAMILY }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomExerciseDialog(
    creationType: CustomCreationType,
    parentSlot: SlotCategory?,
    parentFamily: FamilyCardData?,
    onSaveCustom: (Exercise) -> Unit,
    onDismiss: () -> Unit
) {
    val theme = LocalNeonTheme.current
    var nameInput by remember { mutableStateOf("") }
    var selectedImplement by remember { mutableStateOf(Implement.BARBELL) }
    var selectedStance by remember { mutableStateOf(Stance.STANDARD) }
    var selectedTags by remember { mutableStateOf(setOf("FORM_PRIORITY")) }

    val availableTags = listOf(
        "FORM_PRIORITY",
        "LENGTHENED_PARTIALS",
        "LOWER_BACK_FRIENDLY",
        "UNILATERAL",
        "AXIAL_LOAD",
        "CONSTANT_TENSION",
        "POSTERIOR_CHAIN",
        "BACK_WIDTH"
    )

    val implementsList = Implement.entries
    val stancesList = Stance.entries

    val movementType = when (parentSlot?.id) {
        "horizontal_press", "vertical_press" -> MovementType.COMPOUND_UPPER
        "horizontal_pull" -> MovementType.BACK_THICKNESS
        "vertical_pull" -> MovementType.BACK_WIDTH
        "quad_squat" -> MovementType.QUAD_DOMINANT
        "hinge" -> MovementType.DEADLIFT
        "delts_isolation", "elbow_flex", "elbow_extend" -> MovementType.ISOLATION_UPPER
        "posterior_isolation" -> MovementType.POSTERIOR_CHAIN
        "calves" -> MovementType.CALVES
        "abs" -> MovementType.ABS
        else -> MovementType.COMPOUND_UPPER
    }

    val familyId = when (creationType) {
        CustomCreationType.VARIANT -> parentFamily?.familyId ?: "custom_family"
        CustomCreationType.FAMILY -> "custom_fam_" + UUID.randomUUID().toString().take(8)
    }

    val familyName = when (creationType) {
        CustomCreationType.VARIANT -> parentFamily?.familyName ?: "Custom Family"
        CustomCreationType.FAMILY -> nameInput.ifBlank { "Custom Family" }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = theme.surfaceRaised,
        title = {
            Text(
                text = if (creationType == CustomCreationType.VARIANT) "CREATE CUSTOM VARIANT" else "CREATE CUSTOM FAMILY",
                color = theme.accent,
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (creationType == CustomCreationType.VARIANT) 
                        "Adding variant under family: ${parentFamily?.familyName?.uppercase() ?: ""}" 
                    else 
                        "Adding family under slot: ${parentSlot?.title?.uppercase() ?: ""}",
                    color = theme.inkMuted,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Name Input
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { 
                        Text(
                            if (creationType == CustomCreationType.VARIANT) "VARIANT NAME" else "FAMILY NAME",
                            fontSize = 10.sp
                        ) 
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = theme.accent,
                        unfocusedBorderColor = theme.inkMuted.copy(alpha = 0.5f),
                        focusedTextColor = theme.ink,
                        unfocusedTextColor = theme.ink
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Implement Selection
                Text(
                    text = "IMPLEMENT:",
                    color = theme.accent,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(implementsList) { impl ->
                        FilterChip(
                            selected = impl == selectedImplement,
                            onClick = { selectedImplement = impl },
                            label = { Text(impl.name, fontSize = 8.sp, fontFamily = FontFamily.Monospace) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = theme.accent.copy(alpha = 0.25f),
                                selectedLabelColor = theme.accent,
                                containerColor = theme.surface,
                                labelColor = theme.inkMuted
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Stance Selection
                Text(
                    text = "STANCE:",
                    color = theme.accent,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(stancesList) { stance ->
                        FilterChip(
                            selected = stance == selectedStance,
                            onClick = { selectedStance = stance },
                            label = { Text(stance.name, fontSize = 8.sp, fontFamily = FontFamily.Monospace) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = theme.secondary.copy(alpha = 0.25f),
                                selectedLabelColor = theme.secondary,
                                containerColor = theme.surface,
                                labelColor = theme.inkMuted
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Short Tags Selection
                Text(
                    text = "SHORT TAGS (1–3 CHIPS):",
                    color = theme.accent,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(availableTags) { tag ->
                        val isSelected = selectedTags.contains(tag)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedTags = if (isSelected) {
                                    selectedTags - tag
                                } else {
                                    if (selectedTags.size < 3) selectedTags + tag else selectedTags
                                }
                            },
                            label = { Text(tag, fontSize = 8.sp, fontFamily = FontFamily.Monospace) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = theme.secondary.copy(alpha = 0.3f),
                                selectedLabelColor = theme.secondary,
                                containerColor = theme.surface,
                                labelColor = theme.inkMuted
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (nameInput.isNotBlank()) {
                        val newCustomExercise = Exercise(
                            id = "custom_ex_" + UUID.randomUUID().toString(),
                            name = nameInput.trim(),
                            description = "User created custom exercise",
                            cues = listOf("Controlled eccentric", "Squeeze at peak contraction"),
                            muscleGroups = listOf(parentSlot?.title ?: "Custom"),
                            equipment = listOf(selectedImplement.name),
                            movementType = movementType,
                            familyId = familyId,
                            familyName = familyName,
                            implement = selectedImplement,
                            stance = selectedStance,
                            tags = selectedTags.toList(),
                            isCustom = true,
                            isPrimaryVariant = creationType == CustomCreationType.FAMILY
                        )
                        onSaveCustom(newCustomExercise)
                    }
                },
                enabled = nameInput.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = theme.accent, contentColor = theme.canvas),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text("SAVE CUSTOM", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = theme.inkMuted, fontFamily = FontFamily.Monospace)
            }
        }
    )
}
