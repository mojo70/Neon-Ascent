package com.neon.ascent.feature.dashboard.selfmap

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
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
import com.neon.ascent.core.domain.selfmap.SelfMapIntakeMode
import com.neon.ascent.ui.CyberFrame
import com.neon.ascent.ui.SoftGridBackground

val wizardCategories = listOf(
    SelfMapCategory.BODY_SENSES,
    SelfMapCategory.PEOPLE,
    SelfMapCategory.STUFF,
    SelfMapCategory.ROLES_ACCOMPLISHMENTS,
    SelfMapCategory.IDEAS_LIKES,
    SelfMapCategory.CUSTOM
)

fun getRingLabel(importance: Float): Pair<String, Color> {
    return when {
        importance >= 0.7f -> "CORE RING [0.7 - 1.0]" to Color(0xFFFF0055)
        importance >= 0.4f -> "HIGH RING [0.4 - 0.7]" to Color(0xFF00FFFF)
        importance >= 0.2f -> "MEDIUM RING [0.2 - 0.4]" to Color(0xFFFFD700)
        else -> "PERIPHERAL RING [0.0 - 0.2]" to Color(0xFF00FF9F)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelfMapIntakeWizardScreen(
    viewModel: SelfMapViewModel,
    onFinishWizard: () -> Unit,
    onExitWizard: () -> Unit,
    onSwitchToChat: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val theme = LocalNeonTheme.current
    val cyan = theme.accent
    val danger = theme.accentDanger
    val uiState by viewModel.uiState.collectAsState()

    var currentStepIndex by remember { mutableIntStateOf(0) }
    val currentCategory = wizardCategories[currentStepIndex]

    // Form fields for adding/editing items
    var nameInput by remember { mutableStateOf(uiState.draftName) }
    var importanceInput by remember { mutableFloatStateOf(uiState.draftImportance) }
    var notesInput by remember { mutableStateOf(uiState.draftNotes) }
    var editingItem by remember { mutableStateOf<SelfItemEntity?>(null) }
    var showForm by remember { mutableStateOf(false) }

    val categoryItems = remember(uiState.activeItems, currentCategory) {
        uiState.activeItems.filter { it.category == currentCategory }
    }

    // Sync draft state from ViewModel if populated from Chat / Ask Mentor
    LaunchedEffect(uiState.draftName, uiState.draftImportance, uiState.draftNotes) {
        if (uiState.draftName.isNotBlank() && editingItem == null) {
            nameInput = uiState.draftName
            importanceInput = uiState.draftImportance
            notesInput = uiState.draftNotes
            showForm = true
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        SoftGridBackground(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // --- TOP HEADER & PROGRESS BAR ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "SELF MAP // INTAKE WIZARD",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = cyan,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 2.sp
                        )
                    )
                    Text(
                        text = "STEP ${currentStepIndex + 1} OF ${wizardCategories.size}: ${currentCategory.displayName.uppercase()}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onSwitchToChat != null) {
                        OutlinedButton(
                            onClick = {
                                viewModel.switchSessionIntakeMode(SelfMapIntakeMode.CHAT)
                                onSwitchToChat()
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.LightGray),
                            shape = RoundedCornerShape(4.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.DarkGray),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                text = "Switch to Chat",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp
                            )
                        }
                    }

                    IconButton(onClick = onExitWizard) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Exit Wizard",
                            tint = Color.Gray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Linear Progress Indicator
            LinearProgressIndicator(
                progress = { (currentStepIndex + 1) / wizardCategories.size.toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = cyan,
                trackColor = Color.DarkGray
            )

            Spacer(modifier = Modifier.height(16.dp))

            // --- CATEGORY HEADER FRAME ---
            CyberFrame(
                label = "CATEGORY // ${currentCategory.name}",
                borderColor = cyan,
                accentColor = cyan,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = currentCategory.description,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.LightGray,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Active items in this category: ${categoryItems.size}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = cyan,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // --- MAIN LIST + ADD/EDIT FORM ---
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Inline Add / Edit Form Card
                item {
                    if (showForm || categoryItems.isEmpty()) {
                        CyberFrame(
                            label = if (editingItem != null) "EDIT ITEM" else "ADD NEW ITEM",
                            borderColor = if (editingItem != null) danger else cyan,
                            accentColor = cyan,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                // ASK MENTOR BUTTON HEADER
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "MANUAL INPUT FIELDS",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color.Gray,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    )

                                    OutlinedButton(
                                        onClick = { viewModel.askMentorForSuggestion(currentCategory) },
                                        enabled = !uiState.isSuggestingFromMentor,
                                        shape = RoundedCornerShape(4.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = cyan),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, cyan.copy(alpha = 0.6f))
                                    ) {
                                        if (uiState.isSuggestingFromMentor) {
                                            CircularProgressIndicator(modifier = Modifier.size(12.dp), color = cyan, strokeWidth = 1.5.dp)
                                            Spacer(modifier = Modifier.width(4.dp))
                                        }
                                        Text("⚡ ASK MENTOR", fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                OutlinedTextField(
                                    value = nameInput,
                                    onValueChange = {
                                        nameInput = it
                                        viewModel.updateDraftForm(name = it, category = currentCategory)
                                    },
                                    label = {
                                        Text(
                                            "Item Name (Required)",
                                            fontFamily = FontFamily.Monospace
                                        )
                                    },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = cyan,
                                        unfocusedBorderColor = Color.Gray,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // Slider + Ring Label
                                val (ringLabel, ringColor) = getRingLabel(importanceInput)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "IMPORTANCE: ${(importanceInput * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    )
                                    Text(
                                        text = ringLabel,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = ringColor,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    )
                                }

                                Slider(
                                    value = importanceInput,
                                    onValueChange = {
                                        importanceInput = it
                                        viewModel.updateDraftForm(importance = it)
                                    },
                                    valueRange = 0.0f..1.0f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = ringColor,
                                        activeTrackColor = ringColor,
                                        inactiveTrackColor = Color.DarkGray
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = notesInput,
                                    onValueChange = {
                                        notesInput = it
                                        viewModel.updateDraftForm(notes = it)
                                    },
                                    label = {
                                        Text(
                                            "Notes / Why it matters (Optional)",
                                            fontFamily = FontFamily.Monospace
                                        )
                                    },
                                    maxLines = 3,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = cyan,
                                        unfocusedBorderColor = Color.Gray,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    if (categoryItems.isNotEmpty()) {
                                        TextButton(
                                            onClick = {
                                                showForm = false
                                                editingItem = null
                                                nameInput = ""
                                            }
                                        ) {
                                            Text("CANCEL", color = Color.Gray, fontFamily = FontFamily.Monospace)
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }

                                    Button(
                                        onClick = {
                                            if (nameInput.isNotBlank()) {
                                                if (editingItem != null) {
                                                    viewModel.updateItem(
                                                        editingItem!!.copy(
                                                            name = nameInput.trim(),
                                                            importance = importanceInput,
                                                            notes = notesInput.trim().ifEmpty { null }
                                                        )
                                                    )
                                                } else {
                                                    viewModel.addItem(
                                                        name = nameInput.trim(),
                                                        category = currentCategory,
                                                        importance = importanceInput,
                                                        notes = notesInput.trim().ifEmpty { null }
                                                    )
                                                }
                                                // Reset local & viewmodel draft
                                                nameInput = ""
                                                importanceInput = 0.7f
                                                notesInput = ""
                                                editingItem = null
                                                showForm = false
                                                viewModel.updateDraftForm(name = "", category = currentCategory, importance = 0.7f, notes = "")
                                            }
                                        },
                                        enabled = nameInput.isNotBlank(),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = cyan,
                                            contentColor = Color.Black
                                        ),
                                        shape = MaterialTheme.shapes.extraSmall
                                    ) {
                                        Icon(
                                            imageVector = if (editingItem != null) Icons.Default.Check else Icons.Default.Add,
                                            contentDescription = null,
                                            modifier = Modifier.padding(end = 4.dp)
                                        )
                                        Text(
                                            text = if (editingItem != null) "SAVE CHANGES" else "SAVE ITEM",
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Button(
                            onClick = {
                                showForm = true
                                editingItem = null
                                nameInput = ""
                                importanceInput = 0.7f
                                notesInput = ""
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1E2630),
                                contentColor = cyan
                            ),
                            shape = MaterialTheme.shapes.extraSmall,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 6.dp)
                            )
                            Text(
                                text = "+ ADD ITEM TO ${currentCategory.displayName.uppercase()}",
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // Existing Items Cards in Current Category
                items(categoryItems, key = { it.id }) { item ->
                    val (ringLabel, ringColor) = getRingLabel(item.importance)
                    CyberFrame(
                        label = ringLabel,
                        borderColor = ringColor,
                        accentColor = ringColor,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Importance: ${(item.importance * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = ringColor,
                                        fontWeight = FontWeight.SemiBold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                )
                                if (!item.notes.isNull_or_blank()) {
                                    Text(
                                        text = item.notes!!,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = Color.LightGray,
                                            fontFamily = FontFamily.Monospace
                                        ),
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }

                            Row {
                                IconButton(
                                    onClick = {
                                        editingItem = item
                                        nameInput = item.name
                                        importanceInput = item.importance
                                        notesInput = item.notes ?: ""
                                        showForm = true
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit",
                                        tint = cyan
                                    )
                                }

                                IconButton(
                                    onClick = { viewModel.archiveItem(item.id) }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Archive,
                                        contentDescription = "Archive",
                                        tint = Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // --- BOTTOM NAVIGATION CONTROLS ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentStepIndex > 0) {
                    OutlinedButton(
                        onClick = { currentStepIndex-- },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Text("BACK", fontFamily = FontFamily.Monospace)
                    }
                } else {
                    Spacer(modifier = Modifier.width(80.dp))
                }

                TextButton(
                    onClick = {
                        if (currentStepIndex < wizardCategories.size - 1) {
                            currentStepIndex++
                        } else {
                            onFinishWizard()
                        }
                    }
                ) {
                    Text(
                        text = "ADD LATER / SKIP",
                        color = Color.Gray,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                val isLastStep = currentStepIndex == wizardCategories.size - 1

                Button(
                    onClick = {
                        if (isLastStep) {
                            onFinishWizard()
                        } else {
                            currentStepIndex++
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isLastStep) theme.accent else cyan,
                        contentColor = Color.Black
                    ),
                    shape = MaterialTheme.shapes.extraSmall
                ) {
                    Text(
                        text = if (isLastStep) "FINISH & OPEN CANVAS" else "NEXT CATEGORY",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                    if (!isLastStep) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun String?.isNull_or_blank(): Boolean {
    return this == null || this.isBlank()
}
