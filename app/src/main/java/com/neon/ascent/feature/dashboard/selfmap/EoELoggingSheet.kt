package com.neon.ascent.feature.dashboard.selfmap

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neon.ascent.core.common.LocalNeonTheme
import com.neon.ascent.core.data.local.entity.SelfItemEntity
import com.neon.ascent.core.domain.selfmap.SelfMapCategory

val defaultEmotionChips = listOf(
    "Anger", "Fear", "Anxiety", "Sadness", "Frustration", "Disappointment", "Jealousy", "Happiness", "Relief"
)

val appraisalOptions = listOf("Negative", "Neutral", "Positive")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EoELoggingSheet(
    initialItem: SelfItemEntity?,
    viewModel: SelfMapViewModel,
    onDismiss: () -> Unit
) {
    val theme = LocalNeonTheme.current
    val cyan = theme.accent
    val danger = theme.accentDanger
    val uiState by viewModel.uiState.collectAsState()

    var selectedItem by remember { mutableStateOf(initialItem ?: uiState.activeItems.firstOrNull()) }
    var emotionLabel by remember { mutableStateOf("Frustration") }
    var epText by remember { mutableStateOf("") }
    var perceptionText by remember { mutableStateOf("") }
    var selectedAppraisal by remember { mutableStateOf("Negative") }
    var appraisalNotes by remember { mutableStateOf("") }
    var intensityInput by remember { mutableFloatStateOf(5f) }
    var canChangeNotes by remember { mutableStateOf("") }

    // Inline item creation state
    var showInlineCreate by remember { mutableStateOf(false) }
    var inlineItemName by remember { mutableStateOf("") }
    var inlineCategory by remember { mutableStateOf(SelfMapCategory.CUSTOM) }
    var inlineImportance by remember { mutableFloatStateOf(0.7f) }

    // Pre-fill EP suggestion when selected item changes
    LaunchedEffect(selectedItem) {
        if (epText.isBlank() && selectedItem != null) {
            epText = "Hold ${selectedItem!!.name} at status quo or better"
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF10141A),
        scrimColor = Color.Black.copy(alpha = 0.8f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "EQUATION OF EMOTION // LOG REACTION",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = danger,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.5.sp
                    )
                )

                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                }
            }

            Text(
                text = "EP ∆ P = ER  (Expectation/Preference compared with Perception = Emotional Reaction)",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color.LightGray,
                    fontFamily = FontFamily.Monospace
                ),
                modifier = Modifier.padding(vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Step 1: Select Emotion Label
            Text(
                text = "1. EMOTIONAL REACTION (ER)",
                style = MaterialTheme.typography.labelSmall.copy(color = cyan, fontFamily = FontFamily.Monospace)
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                items(defaultEmotionChips) { chip ->
                    val isSel = emotionLabel.equals(chip, ignoreCase = true)
                    FilterChip(
                        selected = isSel,
                        onClick = { emotionLabel = chip },
                        label = { Text(chip, fontFamily = FontFamily.Monospace, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = danger,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Step 2: Select Attached Self Item (or Inline Create)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "2. ATTACHED SELF ITEM",
                    style = MaterialTheme.typography.labelSmall.copy(color = cyan, fontFamily = FontFamily.Monospace)
                )

                TextButton(onClick = { showInlineCreate = !showInlineCreate }) {
                    Text(
                        text = if (showInlineCreate) "SELECT EXISTING" else "+ CREATE INLINE",
                        color = cyan,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (showInlineCreate) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E2630), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    OutlinedTextField(
                        value = inlineItemName,
                        onValueChange = { inlineItemName = it },
                        label = { Text("New Attachment Name", fontFamily = FontFamily.Monospace) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = cyan,
                            unfocusedBorderColor = Color.Gray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            if (inlineItemName.isNotBlank()) {
                                viewModel.addItem(
                                    name = inlineItemName.trim(),
                                    category = inlineCategory,
                                    importance = inlineImportance,
                                    notes = "Created during EoE reaction log"
                                )
                                showInlineCreate = false
                                inlineItemName = ""
                            }
                        },
                        enabled = inlineItemName.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = cyan, contentColor = Color.Black),
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                        Text("ADD & SELECT", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }
            } else {
                if (uiState.activeItems.isEmpty()) {
                    Text(
                        text = "No active attachments on map. Use '+ CREATE INLINE' above to add one.",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        items(uiState.activeItems) { item ->
                            val isSel = selectedItem?.id == item.id
                            FilterChip(
                                selected = isSel,
                                onClick = { selectedItem = item },
                                label = { Text(item.name, fontFamily = FontFamily.Monospace, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = cyan,
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Step 3: Expectation / Preference (EP)
            OutlinedTextField(
                value = epText,
                onValueChange = { epText = it },
                label = { Text("3. Expectation / Preference (EP)", fontFamily = FontFamily.Monospace) },
                placeholder = { Text("What did your mind expect or want?", fontFamily = FontFamily.Monospace) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = cyan,
                    unfocusedBorderColor = Color.Gray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Step 4: Perception (P)
            OutlinedTextField(
                value = perceptionText,
                onValueChange = { perceptionText = it },
                label = { Text("4. Perception / What Happened (P)", fontFamily = FontFamily.Monospace) },
                placeholder = { Text("What actually occurred or was perceived?", fontFamily = FontFamily.Monospace) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = cyan,
                    unfocusedBorderColor = Color.Gray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Step 5: Appraisal & Intensity
            Text(
                text = "5. APPRAISAL & INTENSITY",
                style = MaterialTheme.typography.labelSmall.copy(color = cyan, fontFamily = FontFamily.Monospace)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                appraisalOptions.forEach { option ->
                    val isSel = selectedAppraisal.equals(option, ignoreCase = true)
                    FilterChip(
                        selected = isSel,
                        onClick = { selectedAppraisal = option },
                        label = { Text(option.uppercase(), fontFamily = FontFamily.Monospace, fontSize = 10.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = if (option == "Negative") danger else cyan,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "REACTION INTENSITY: ${intensityInput.toInt()}/10",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = Color.White,
                        fontFamily = FontFamily.Monospace
                    )
                )
            }

            Slider(
                value = intensityInput,
                onValueChange = { intensityInput = it },
                valueRange = 1f..10f,
                steps = 8,
                colors = SliderDefaults.colors(
                    thumbColor = danger,
                    activeTrackColor = danger,
                    inactiveTrackColor = Color.DarkGray
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Step 6: Can you change EP, P, or both?
            OutlinedTextField(
                value = canChangeNotes,
                onValueChange = { canChangeNotes = it },
                label = { Text("6. Reflection: Can you change EP, P, or both?", fontFamily = FontFamily.Monospace) },
                placeholder = { Text("How can you loosen this preference or reframe perception?", fontFamily = FontFamily.Monospace) },
                maxLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = cyan,
                    unfocusedBorderColor = Color.Gray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Save Reaction Button
            val fullAppraisal = "$selectedAppraisal${if (appraisalNotes.isNotBlank()) ": $appraisalNotes" else ""}"
            Button(
                onClick = {
                    val target = selectedItem ?: uiState.activeItems.firstOrNull()
                    if (target != null && epText.isNotBlank() && perceptionText.isNotBlank()) {
                        viewModel.logEoEReaction(
                            selfItemId = target.id,
                            emotionLabel = emotionLabel,
                            epText = epText.trim(),
                            perceptionText = perceptionText.trim(),
                            appraisal = fullAppraisal,
                            intensity = intensityInput.toInt(),
                            canChangeNotes = canChangeNotes.trim().ifEmpty { null }
                        )
                        onDismiss()
                    }
                },
                enabled = (selectedItem != null || uiState.activeItems.isNotEmpty()) && epText.isNotBlank() && perceptionText.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = danger,
                    contentColor = Color.White
                ),
                shape = MaterialTheme.shapes.extraSmall,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "SAVE EoE REACTION LOG",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.5.sp
                    )
                )
            }
        }
    }
}
