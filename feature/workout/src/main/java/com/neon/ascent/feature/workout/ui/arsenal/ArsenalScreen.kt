package com.neon.ascent.feature.workout.ui.arsenal

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neon.ascent.core.common.LocalNeonTheme
import com.neon.ascent.core.domain.workout.models.Exercise
import com.neon.ascent.core.domain.workout.models.WorkoutRoutine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArsenalScreen(
    onBack: () -> Unit,
    initialSlot: String? = null,
    initialFamilyId: String? = null,
    highlightExerciseId: String? = null,
    onSubstitute: ((Exercise) -> Unit)? = null,
    viewModel: ArsenalViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val theme = LocalNeonTheme.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(initialSlot, initialFamilyId, highlightExerciseId) {
        viewModel.initDeepLink(initialSlot, initialFamilyId, highlightExerciseId)
    }

    LaunchedEffect(uiState.substituteSuccessMessage) {
        uiState.substituteSuccessMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSuccessMessage()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val titleText = when {
                            uiState.selectedFamily != null -> "ARSENAL // ${uiState.selectedFamily!!.familyName.uppercase()}"
                            uiState.selectedSlot != null -> "ARSENAL // ${uiState.selectedSlot!!.title.uppercase()}"
                            else -> "ARSENAL // LIFT CATALOG"
                        }
                        Text(
                            text = titleText,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "CYBERCRAPP_EXERCISE_SURFACE",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            color = theme.inkMuted
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        when {
                            uiState.selectedFamily != null -> viewModel.selectFamily(null)
                            uiState.selectedSlot != null -> viewModel.selectSlot(null)
                            else -> onBack()
                        }
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = theme.accent
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = theme.canvas,
                    titleContentColor = theme.accent
                )
            )
        },
        floatingActionButton = {
            if (uiState.selectedSlot != null) {
                FloatingActionButton(
                    onClick = {
                        if (uiState.selectedFamily != null) {
                            viewModel.openCustomCreationDialog(CustomCreationType.VARIANT)
                        } else {
                            viewModel.openCustomCreationDialog(CustomCreationType.FAMILY)
                        }
                    },
                    containerColor = theme.accent,
                    contentColor = theme.canvas,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Create Custom", modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = if (uiState.selectedFamily != null) "+ VARIANT" else "+ FAMILY",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = theme.canvas
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Search Bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::updateSearchQuery,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("SEARCH_SLOTS_FAMILIES_OR_EXERCISES...", color = theme.inkMuted, fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = theme.accent) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = theme.accent,
                    unfocusedBorderColor = theme.inkMuted.copy(alpha = 0.5f),
                    unfocusedContainerColor = theme.surface,
                    focusedContainerColor = theme.surface,
                    focusedTextColor = theme.ink,
                    unfocusedTextColor = theme.ink
                ),
                shape = RoundedCornerShape(4.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = theme.accent)
                }
            } else if (uiState.selectedSlot == null) {
                // View 1: Slot List View
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item {
                        Text(
                            text = "SELECT_EXERCISE_SLOT [${uiState.slots.size} CATEGORIES]",
                            color = theme.accent.copy(alpha = 0.8f),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    items(uiState.slots) { slot ->
                        SlotCard(slot = slot, onClick = { viewModel.selectSlot(slot) })
                    }
                }
            } else if (uiState.selectedFamily == null) {
                // View 2: Family List View for Selected Slot
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "FAMILIES IN ${uiState.selectedSlot!!.title.uppercase()}",
                                color = theme.accent.copy(alpha = 0.8f),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "← RETURN_TO_SLOTS",
                                color = theme.accent,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable { viewModel.selectSlot(null) }
                            )
                        }
                    }
                    items(uiState.familyCards) { family ->
                        FamilyCard(family = family, onClick = { viewModel.selectFamily(family) })
                    }
                }
            } else {
                // View 3: Family Implement + Variant View
                val family = uiState.selectedFamily!!

                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = family.familyName.uppercase(),
                                color = theme.ink,
                                fontSize = 16.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = family.rangeText,
                                color = theme.accent,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Text(
                            text = "← FAMILIES",
                            color = theme.accent,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { viewModel.selectFamily(null) }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Short Tags for Family
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        family.tags.forEach { tag ->
                            Surface(
                                color = theme.secondary.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp),
                                border = BorderStroke(1.dp, theme.secondary.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = tag,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    color = theme.secondary,
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Implement Chips
                    if (uiState.availableImplements.isNotEmpty()) {
                        Text(
                            text = "IMPLEMENT:",
                            color = theme.inkMuted,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(uiState.availableImplements) { implement ->
                                val isSelected = implement == uiState.selectedImplement
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.selectImplement(implement) },
                                    label = { Text(implement.uppercase(), fontSize = 9.sp, fontFamily = FontFamily.Monospace) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = theme.accent.copy(alpha = 0.25f),
                                        selectedLabelColor = theme.accent,
                                        containerColor = theme.surface,
                                        labelColor = theme.inkMuted
                                    )
                                )
                            }
                        }
                    }

                    // Stance Chips (ONLY shown when > 1 stance exists!)
                    if (uiState.availableStances.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "STANCE VARIATION:",
                            color = theme.inkMuted,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(uiState.availableStances) { stance ->
                                val isSelected = stance == uiState.selectedStance
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.selectStance(stance) },
                                    label = { Text(stance.uppercase(), fontSize = 9.sp, fontFamily = FontFamily.Monospace) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = theme.secondary.copy(alpha = 0.25f),
                                        selectedLabelColor = theme.secondary,
                                        containerColor = theme.surface,
                                        labelColor = theme.inkMuted
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "VARIANTS (${uiState.filteredVariants.size})",
                        color = theme.accent.copy(alpha = 0.8f),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(uiState.filteredVariants) { exercise ->
                            val isHighlighted = exercise.id == uiState.highlightExerciseId
                            val accomplishment = uiState.accomplishmentsMap[exercise.id]
                            VariantCard(
                                exercise = exercise,
                                isHighlighted = isHighlighted,
                                accomplishment = accomplishment,
                                onUseAsSubstitute = {
                                    viewModel.useAsSubstitute(exercise, onSubstitute)
                                },
                                onAddToTemplate = {
                                    viewModel.openAddToTemplateDialog(exercise)
                                },
                                onDeleteCustom = {
                                    viewModel.deleteCustomExercise(exercise.id)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Add to Template Dialog
    uiState.showAddToTemplateDialogFor?.let { exercise ->
        AddToTemplateDialog(
            exercise = exercise,
            routines = uiState.userRoutines,
            onSelectRoutine = { routine ->
                viewModel.addExerciseToTemplate(exercise, routine)
            },
            onDismiss = { viewModel.dismissAddToTemplateDialog() }
        )
    }

    // Custom Creation Dialog
    uiState.showCustomCreationType?.let { creationType ->
        CustomExerciseDialog(
            creationType = creationType,
            parentSlot = uiState.selectedSlot,
            parentFamily = uiState.selectedFamily,
            onSaveCustom = { customEx ->
                viewModel.saveCustomExercise(customEx)
            },
            onDismiss = { viewModel.dismissCustomCreationDialog() }
        )
    }
}

@Composable
fun SlotCard(slot: SlotCategory, onClick: () -> Unit) {
    val theme = LocalNeonTheme.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(1.dp, theme.accent.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = theme.surfaceRaised)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = slot.title.uppercase(),
                    color = theme.ink,
                    fontSize = 15.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = Icons.Default.FitnessCenter,
                    contentDescription = null,
                    tint = theme.accent,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = slot.description,
                color = theme.inkMuted,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(slot.familyNames) { familyName ->
                    Surface(
                        color = theme.accent.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(1.dp, theme.accent.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = familyName,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = theme.accent,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FamilyCard(family: FamilyCardData, onClick: () -> Unit) {
    val theme = LocalNeonTheme.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(1.dp, theme.accent.copy(alpha = 0.35f), RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = theme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = family.familyName.uppercase(),
                    color = theme.ink,
                    fontSize = 15.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${family.variants.size} VARIANTS",
                    color = theme.accent,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = family.rangeText,
                color = theme.accent,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Short tag chips
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                family.tags.forEach { tag ->
                    Surface(
                        color = theme.secondary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(1.dp, theme.secondary.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = tag,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            color = theme.secondary,
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VariantCard(
    exercise: Exercise,
    isHighlighted: Boolean,
    accomplishment: AccomplishmentSummary?,
    onUseAsSubstitute: () -> Unit,
    onAddToTemplate: () -> Unit,
    onDeleteCustom: () -> Unit
) {
    val theme = LocalNeonTheme.current
    val borderColor = if (isHighlighted) Color(0xFFFF0066) else theme.accent.copy(alpha = 0.25f)
    val containerColor = if (isHighlighted) Color(0xFFFF0066).copy(alpha = 0.08f) else theme.surface

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(if (isHighlighted) 2.dp else 1.dp, borderColor, RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = exercise.name,
                            color = theme.ink,
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        if (exercise.isCustom) {
                            Spacer(Modifier.width(6.dp))
                            Surface(
                                color = theme.accent.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp),
                                border = BorderStroke(1.dp, theme.accent.copy(alpha = 0.6f))
                            ) {
                                Text(
                                    text = "CUSTOM",
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                    color = theme.accent,
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Text(
                        text = "${exercise.implement.name} · Stance: ${exercise.stance.name}",
                        color = theme.inkMuted,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isHighlighted) {
                        Surface(
                            color = Color(0xFFFF0066),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "CURRENT_EXERCISE",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                color = Color.White,
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    if (exercise.isCustom) {
                        Spacer(Modifier.width(4.dp))
                        IconButton(
                            onClick = onDeleteCustom,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete custom exercise",
                                tint = theme.inkMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            if (accomplishment != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "BEST CLUSTER: ${accomplishment.bestClusterWeight.toInt()} lbs × ${accomplishment.bestClusterReps} reps",
                    color = theme.accent,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            val tags = exercise.effectiveTags()
            if (tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    tags.forEach { tag ->
                        Surface(
                            color = theme.secondary.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(1.dp, theme.secondary.copy(alpha = 0.35f))
                        ) {
                            Text(
                                text = tag,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                color = theme.secondary,
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onUseAsSubstitute,
                    modifier = Modifier.weight(1f).height(32.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = theme.accent, contentColor = theme.canvas),
                    shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("USE AS SUBSTITUTE", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onAddToTemplate,
                    modifier = Modifier.height(32.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = theme.accent),
                    border = BorderStroke(1.dp, theme.accent),
                    shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("ADD TO TEMPLATE", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AddToTemplateDialog(
    exercise: Exercise,
    routines: List<WorkoutRoutine>,
    onSelectRoutine: (WorkoutRoutine) -> Unit,
    onDismiss: () -> Unit
) {
    val theme = LocalNeonTheme.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = theme.surfaceRaised,
        title = {
            Text(
                text = "ADD TO ROUTINE TEMPLATE",
                color = theme.accent,
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "Select a template to add ${exercise.name}:",
                    color = theme.inkMuted,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                if (routines.isEmpty()) {
                    Text(
                        text = "NO_USER_ROUTINES_FOUND",
                        color = theme.inkMuted,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                } else {
                    routines.forEach { routine ->
                        Surface(
                            onClick = { onSelectRoutine(routine) },
                            color = theme.surface,
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, theme.accent.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = routine.name,
                                    color = theme.ink,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${routine.exercises.size} EXERCISES",
                                    color = theme.accent,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = theme.inkMuted, fontFamily = FontFamily.Monospace)
            }
        }
    )
}
