package com.neon.ascent.feature.workout.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.zIndex
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.channels.Channel
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.neon.ascent.core.domain.workout.models.*

@Composable
fun WorkoutLoggingScreen(
    onBack: () -> Unit,
    viewModel: WorkoutViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showRoutineActionMenuFor by remember { mutableStateOf<WorkoutRoutine?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (uiState.session == null) {
                if (uiState.isCreatingRoutine) {
                    CreateRoutineScreen(
                        uiState = uiState,
                        onBack = { viewModel.cancelCreateRoutine() },
                        onSave = { viewModel.saveRoutine() },
                        onUpdateName = { viewModel.updateNewRoutineName(it) },
                        viewModel = viewModel
                    )
                } else if (uiState.isCreatingAugment) {
                    CreateAugmentScreen(
                        uiState = uiState,
                        onBack = { viewModel.cancelCreateAugment() },
                        onSave = { viewModel.saveAugment() },
                        onUpdateName = { viewModel.updateNewAugmentName(it) },
                        onUpdateBodyPart = { viewModel.updateNewAugmentBodyPart(it) },
                        viewModel = viewModel
                    )
                } else {
                    WorkoutIntakeScreen(
                        uiState = uiState,
                        onBack = onBack,
                        onStartProtocol = { viewModel.startSession(it) },
                        onStartRoutine = { viewModel.startRoutine(it) },
                        onCreateRoutine = { viewModel.startCreateRoutine() },
                        onCreateAugment = { viewModel.startCreateAugment() },
                        onRoutineActionClick = { showRoutineActionMenuFor = it },
                        onAddAugment = { viewModel.toggleAugmentLibrary(it) }
                    )
                }
            } else if (uiState.isReorderingExercises) {
                ReorderExercisesScreen(
                    uiState = uiState,
                    onBack = { viewModel.stopReordering() },
                    onMove = { from, to -> viewModel.moveWorkoutLog(from, to) },
                    onRemove = { viewModel.removeWorkoutLog(it) },
                    onDone = { viewModel.stopReordering() }
                )
            } else {
                val durationFormatted = remember(uiState.workoutDurationSeconds) {
                    val minutes = uiState.workoutDurationSeconds / 60
                    val seconds = uiState.workoutDurationSeconds % 60
                    "%d:%02d".format(minutes, seconds)
                }
                ActiveWorkoutHeader(
                    duration = durationFormatted,
                    onBack = onBack,
                    onFinish = { viewModel.finishWorkout() },
                    isPaused = uiState.isPaused,
                    onPauseToggle = {
                        if (uiState.isPaused) viewModel.resumeWorkout() else viewModel.pauseWorkout()
                    },
                    onDiscard = { viewModel.discardWorkout() }
                )
                WorkoutSummaryBar(uiState)
                
                Box(modifier = Modifier.weight(1f)) {
                    ActiveWorkoutContent(uiState, viewModel)
                    
                    if (uiState.isPaused) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.7f))
                                .clickable(enabled = false) {},
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "WORKOUT PAUSED",
                                    color = Color.White,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { viewModel.resumeWorkout() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF))
                                ) {
                                    Text("Resume", color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Active Session Error Dialog
        uiState.activeSessionError?.let { error ->
            AlertDialog(
                onDismissRequest = { viewModel.clearActiveSessionError() },
                title = { Text("Session Active", color = Color.White) },
                text = { Text(error, color = Color.Gray) },
                confirmButton = {
                    TextButton(onClick = { viewModel.clearActiveSessionError() }) {
                        Text("OK", color = Color(0xFF007AFF))
                    }
                },
                containerColor = Color(0xFF1C1C1E)
            )
        }

        if (showRoutineActionMenuFor != null) {
            WorkoutRoutineActionMenu(
                routine = showRoutineActionMenuFor!!,
                onShare = { viewModel.shareRoutine(showRoutineActionMenuFor!!) },
                onDuplicate = { viewModel.duplicateRoutine(showRoutineActionMenuFor!!) },
                onEdit = { 
                    viewModel.editRoutine(showRoutineActionMenuFor!!)
                    showRoutineActionMenuFor = null
                },
                onDelete = { viewModel.deleteRoutine(showRoutineActionMenuFor!!) },
                onDismiss = { showRoutineActionMenuFor = null }
            )
        }

        // Uncompleted Sets Dialog
        if (uiState.showUncompletedSetsDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissUncompletedSetsDialog(discard = false) },
                title = { Text("Uncompleted Sets", color = Color.White) },
                text = { Text("You have sets that haven't been completed. Do you want to discard them and finish?", color = Color.Gray) },
                confirmButton = {
                    TextButton(onClick = { viewModel.dismissUncompletedSetsDialog(discard = true) }) {
                        Text("Discard & Finish", color = Color.Red)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissUncompletedSetsDialog(discard = false) }) {
                        Text("Keep Editing", color = Color.Gray)
                    }
                },
                containerColor = Color(0xFF1C1C1E)
            )
        }

        // Save Routine Changes Dialog
        if (uiState.showSaveRoutineChangesDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.confirmSaveRoutineChanges(save = false) },
                title = { Text("Save Changes?", color = Color.White) },
                text = { Text("You've modified the exercises or set counts in this routine. Would you like to update the routine with these changes for next time?", color = Color.Gray) },
                confirmButton = {
                    TextButton(onClick = { viewModel.confirmSaveRoutineChanges(save = true) }) {
                        Text("Save Changes", color = Color(0xFF007AFF))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.confirmSaveRoutineChanges(save = false) }) {
                        Text("No, Finish Only", color = Color.Gray)
                    }
                },
                containerColor = Color(0xFF1C1C1E)
            )
        }
    }
}

@Composable
fun ActiveWorkoutHeader(
    duration: String,
    onBack: () -> Unit,
    onFinish: () -> Unit,
    isPaused: Boolean,
    onPauseToggle: () -> Unit,
    onDiscard: () -> Unit
) {
    var showDiscardDialog by remember { mutableStateOf(false) }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Discard Workout?") },
            text = { Text("Are you sure you want to discard this workout? All progress will be lost.") },
            confirmButton = {
                TextButton(onClick = {
                    onDiscard()
                    showDiscardDialog = false
                }) {
                    Text("Discard", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text("Cancel")
                }
            },
            containerColor = Color(0xFF1C1C1E),
            titleContentColor = Color.White,
            textContentColor = Color.Gray
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onBack() }) {
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Log Workout", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { showDiscardDialog = true }) {
                Icon(Icons.Default.Delete, contentDescription = "Discard", tint = Color.Red.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
            }
            
            IconButton(onClick = onPauseToggle) {
                Icon(
                    if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = if (isPaused) "Resume" else "Pause",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))
            Text(duration, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.width(12.dp))
            Button(
                onClick = onFinish,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF)),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text("Finish", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun WorkoutSummaryBar(uiState: WorkoutUiState) {
    val totalVolume = remember(uiState.logs) {
        uiState.logs.sumOf { (_, sets) -> 
            sets.sumOf { (it.weight * it.reps).toDouble() }
        }.toInt()
    }
    val totalSets = remember(uiState.logs) {
        uiState.logs.sumOf { it.second.size }
    }
    val durationMinutes = uiState.workoutDurationSeconds / 60

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        SummaryStat("Duration", "${durationMinutes}m", Color(0xFF007AFF))
        SummaryStat("Volume", "%,d lbs".format(totalVolume), Color.White)
        SummaryStat("Sets", "$totalSets", Color.White)
        Icon(Icons.Default.Person, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(24.dp))
    }
}

@Composable
fun SummaryStat(label: String, value: String, valueColor: Color) {
    Column {
        Text(label, color = Color.Gray, fontSize = 12.sp)
        Text(value, color = valueColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun WorkoutIntakeScreen(
    uiState: WorkoutUiState,
    onBack: () -> Unit,
    onStartProtocol: (WorkoutProtocol) -> Unit,
    onStartRoutine: (WorkoutRoutine) -> Unit,
    onCreateRoutine: () -> Unit,
    onCreateAugment: () -> Unit,
    onRoutineActionClick: (WorkoutRoutine) -> Unit,
    onAddAugment: (WorkoutAugment) -> Unit
) {
    var isRoutinesExpanded by remember { mutableStateOf(true) }
    var isAugmentsExpanded by remember { mutableStateOf(true) }
    var showExploreProtocols by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onBack() }
            ) {
                Text(
                    "Workout",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
            IconButton(onClick = { /* Refresh logic */ }) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "Sync",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Start Empty Workout Button
        Button(
            onClick = { onStartProtocol(WorkoutProtocol.GENERAL) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1C1C1E)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start Empty Workout", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Routines Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Routines",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onCreateRoutine) {
                Icon(
                    Icons.Default.AddBox,
                    contentDescription = "New Routine",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // New Routine, New Augment & Explore Buttons
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IntakeActionButton(
                icon = Icons.AutoMirrored.Filled.Assignment,
                label = "New Routine",
                modifier = Modifier.weight(1f),
                onClick = onCreateRoutine
            )
            IntakeActionButton(
                icon = Icons.Default.Bolt,
                label = "New Augment",
                modifier = Modifier.weight(1f),
                onClick = onCreateAugment
            )
            IntakeActionButton(
                icon = Icons.Default.Search,
                label = "Explore",
                modifier = Modifier.weight(1f),
                onClick = { showExploreProtocols = true }
            )
        }

        if (showExploreProtocols) {
            ExploreProtocolsDialog(
                uiState = uiState,
                onStartProtocol = {
                    onStartProtocol(it)
                    showExploreProtocols = false
                },
                onAddAugment = {
                    onAddAugment(it)
                },
                onDismiss = { showExploreProtocols = false }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // My Routines List Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isRoutinesExpanded = !isRoutinesExpanded }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (isRoutinesExpanded) Icons.Default.ArrowDropDown else Icons.AutoMirrored.Filled.ArrowRight,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(20.dp)
            )
            Text(
                "My Routines (${uiState.routines.size})",
                color = Color.Gray,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        if (isRoutinesExpanded) {
            Spacer(modifier = Modifier.height(8.dp))
            uiState.routines.forEach { routine ->
                RoutineCard(
                    routine = routine, 
                    onStart = { onStartRoutine(routine) },
                    onActionClick = { onRoutineActionClick(routine) }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Augments List Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isAugmentsExpanded = !isAugmentsExpanded }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (isAugmentsExpanded) Icons.Default.ArrowDropDown else Icons.AutoMirrored.Filled.ArrowRight,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(20.dp)
            )
            Text(
                "Sub Protocols / Augments (${uiState.augments.size})",
                color = Color.Gray,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        if (isAugmentsExpanded) {
            Spacer(modifier = Modifier.height(8.dp))
            uiState.augments.forEach { augment ->
                AugmentCard(
                    augment = augment,
                    onActionClick = { /* TODO: Augment actions */ }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
        
        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
fun IntakeActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Surface(
        modifier = modifier
            .height(56.dp)
            .clickable { onClick() },
        color = Color(0xFF1C1C1E),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 15.sp)
        }
    }
}

@Composable
fun RoutineCard(routine: WorkoutRoutine, onStart: () -> Unit, onActionClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF1C1C1E),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    routine.name,
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onActionClick, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.MoreHoriz, contentDescription = "Routine Actions", tint = Color.Gray)
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                routine.exercises.joinToString(", ") { it.exercise.name } + (if (routine.augments.isNotEmpty()) ", " + routine.augments.joinToString(", ") { it.name } else ""),
                color = Color.Gray,
                fontSize = 13.sp,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF)),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text("Start Routine", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

@Composable
fun AugmentCard(augment: WorkoutAugment, onActionClick: () -> Unit) {
    val color = Color(android.graphics.Color.parseColor(augment.colorHex))
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF1C1C1E),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        augment.name,
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        augment.focusBodyPart.uppercase(),
                        color = color,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black
                    )
                }
                IconButton(onClick = onActionClick, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.MoreHoriz, contentDescription = "Augment Actions", tint = Color.Gray)
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                augment.exercises.joinToString(", ") { it.exercise.name },
                color = Color.Gray,
                fontSize = 13.sp,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun CreateRoutineScreen(
    uiState: WorkoutUiState,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onUpdateName: (String) -> Unit,
    viewModel: WorkoutViewModel
) {
    var showExercisePicker by remember { mutableStateOf(false) }
    var showAugmentPicker by remember { mutableStateOf(false) }
    val filteredExercises by viewModel.filteredExercises.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Cancel",
                color = Color(0xFF007AFF),
                modifier = Modifier.clickable { onBack() },
                fontSize = 17.sp
            )
            Text(
                "Create Routine",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
            Text(
                "Save",
                color = if (uiState.newRoutineName.isNotBlank() && (uiState.newRoutineExercises.isNotEmpty() || uiState.newRoutineAugments.isNotEmpty())) Color(0xFF007AFF) else Color.Gray,
                modifier = Modifier.clickable(enabled = uiState.newRoutineName.isNotBlank() && (uiState.newRoutineExercises.isNotEmpty() || uiState.newRoutineAugments.isNotEmpty())) { onSave() },
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Title Input
        OutlinedTextField(
            value = uiState.newRoutineName,
            onValueChange = onUpdateName,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp),
            placeholder = { Text("Routine title", color = Color.Gray, fontSize = 20.sp, fontWeight = FontWeight.Bold) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF007AFF),
                unfocusedBorderColor = Color.DarkGray,
                cursorColor = Color(0xFF007AFF),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            textStyle = LocalTextStyle.current.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (uiState.newRoutineExercises.isEmpty() && uiState.newRoutineAugments.isEmpty()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.FitnessCenter,
                    contentDescription = null,
                    tint = Color.DarkGray,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Get started by adding exercises or augments to your routine.",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 48.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(modifier = Modifier.padding(horizontal = 32.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { showExercisePicker = true },
                        modifier = Modifier.weight(1f).height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Add Exercise", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { showAugmentPicker = true },
                        modifier = Modifier.weight(1f).height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1C1C1E)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Add Augment", color = Color(0xFF00CCFF), fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(uiState.newRoutineExercises) { routineExercise ->
                    RoutineExerciseItem(
                        routineExercise = routineExercise,
                        viewModel = viewModel,
                        onRemove = { viewModel.removeExerciseFromNewRoutine(routineExercise) }
                    )
                }
                items(uiState.newRoutineAugments) { augment ->
                    val color = Color(android.graphics.Color.parseColor(augment.colorHex))
                    Surface(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        color = Color(0xFF1C1C1E),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, color.copy(alpha = 0.5f))
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Bolt, contentDescription = null, tint = color)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(augment.name, color = Color.White, fontWeight = FontWeight.Bold)
                                Text(augment.focusBodyPart, color = color, fontSize = 12.sp)
                            }
                            IconButton(onClick = { viewModel.removeAugmentFromNewRoutine(augment) }) {
                                Icon(Icons.Default.Close, contentDescription = null, tint = Color.Gray)
                            }
                        }
                    }
                }
                item {
                    Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TextButton(onClick = { showExercisePicker = true }) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF007AFF))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Exercise", color = Color(0xFF007AFF), fontWeight = FontWeight.Bold)
                        }
                        TextButton(onClick = { showAugmentPicker = true }) {
                            Icon(Icons.Default.Bolt, contentDescription = null, tint = Color(0xFF00CCFF))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Augment", color = Color(0xFF00CCFF), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showExercisePicker) {
        Dialog(onDismissRequest = { showExercisePicker = false }) {
            Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
                ExercisePicker(
                    uiState = uiState,
                    exercises = filteredExercises,
                    onSearchChange = { viewModel.updateExerciseSearch(it) },
                    onSelect = {
                        viewModel.addExerciseToNewRoutine(it)
                        showExercisePicker = false
                    },
                    onSaveCustomExercise = { name, muscle, equip, desc ->
                        viewModel.saveCustomExercise(name, muscle, equip, desc)
                    },
                    onDismiss = { showExercisePicker = false }
                )
            }
        }
    }
    
    if (showAugmentPicker) {
        Dialog(onDismissRequest = { showAugmentPicker = false }) {
            Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
                AugmentPicker(
                    augments = uiState.augments,
                    onSelect = {
                        viewModel.addAugmentToNewRoutine(it)
                        showAugmentPicker = false
                    },
                    onDismiss = { showAugmentPicker = false }
                )
            }
        }
    }
}

@Composable
fun CreateAugmentScreen(
    uiState: WorkoutUiState,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onUpdateName: (String) -> Unit,
    onUpdateBodyPart: (String) -> Unit,
    viewModel: WorkoutViewModel
) {
    var showExercisePicker by remember { mutableStateOf(false) }
    val filteredExercises by viewModel.filteredExercises.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Cancel",
                color = Color(0xFF007AFF),
                modifier = Modifier.clickable { onBack() },
                fontSize = 17.sp
            )
            Text(
                "Create Augment",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
            Text(
                "Save",
                color = if (uiState.newAugmentName.isNotBlank() && uiState.newAugmentExercises.isNotEmpty()) Color(0xFF00CCFF) else Color.Gray,
                modifier = Modifier.clickable(enabled = uiState.newAugmentName.isNotBlank() && uiState.newAugmentExercises.isNotEmpty()) { onSave() },
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Title Input
        OutlinedTextField(
            value = uiState.newAugmentName,
            onValueChange = onUpdateName,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            placeholder = { Text("Augment title (e.g. Gorilla Arms)", color = Color.Gray, fontSize = 20.sp, fontWeight = FontWeight.Bold) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF00CCFF),
                unfocusedBorderColor = Color.DarkGray,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            textStyle = LocalTextStyle.current.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold),
            singleLine = true
        )

        // Body Part Input
        OutlinedTextField(
            value = uiState.newAugmentBodyPart,
            onValueChange = onUpdateBodyPart,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("Focus Body Part (e.g. Biceps)", color = Color.Gray, fontSize = 14.sp) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF00CCFF),
                unfocusedBorderColor = Color.DarkGray,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.newAugmentExercises.isEmpty()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.Bolt,
                    contentDescription = null,
                    tint = Color.DarkGray,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Select exercises to bundle into this sub-protocol.",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 48.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { showExercisePicker = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp)
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00CCFF)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add exercise", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(uiState.newAugmentExercises) { routineExercise ->
                    RoutineExerciseItem(
                        routineExercise = routineExercise,
                        viewModel = viewModel,
                        onRemove = { viewModel.removeExerciseFromNewRoutine(routineExercise) }
                    )
                }
                item {
                    TextButton(
                        onClick = { showExercisePicker = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF00CCFF))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add exercise", color = Color(0xFF00CCFF), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showExercisePicker) {
        Dialog(onDismissRequest = { showExercisePicker = false }) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.Black
            ) {
                ExercisePicker(
                    uiState = uiState,
                    exercises = filteredExercises,
                    onSearchChange = { viewModel.updateExerciseSearch(it) },
                    onSelect = {
                        viewModel.addExerciseToNewRoutine(it)
                        showExercisePicker = false
                    },
                    onSaveCustomExercise = { name, muscle, equip, desc ->
                        viewModel.saveCustomExercise(name, muscle, equip, desc)
                    },
                    onDismiss = { showExercisePicker = false }
                )
            }
        }
    }
}

@Composable
fun RoutineExerciseItem(
    routineExercise: RoutineExercise,
    viewModel: WorkoutViewModel,
    onRemove: () -> Unit
) {
    var showSetTypeSelectorForIndex by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(Color(0xFF1C1C1E), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                routineExercise.exercise.name,
                color = Color(0xFF007AFF),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.Gray, modifier = Modifier.size(20.dp))
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Set Table Header
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("SET", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.weight(1f))
            Text("TYPE", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.weight(2f))
            Text("LBS", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.weight(1.5f), textAlign = TextAlign.Center)
            Text("REPS", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.weight(1.5f), textAlign = TextAlign.Center)
            Box(modifier = Modifier.size(24.dp)) // Spacer for remove button
        }

        Spacer(modifier = Modifier.height(8.dp))

        routineExercise.sets.forEachIndexed { index, set ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${index + 1}", color = Color.White, fontSize = 14.sp, modifier = Modifier.weight(1f))
                
                // Set Type Button
                Box(
                    modifier = Modifier
                        .weight(2f)
                        .clickable { showSetTypeSelectorForIndex = index },
                    contentAlignment = Alignment.CenterStart
                ) {
                    val label = when (set.type) {
                        SetType.NORMAL -> "Normal"
                        SetType.WARMUP -> "Warmup"
                        SetType.DROP -> "Drop"
                        SetType.FAILURE -> "Failure"
                        SetType.REST_PAUSE -> "Rest-Pause"
                    }
                    val color = when (set.type) {
                        SetType.WARMUP -> Color(0xFFFFA500)
                        SetType.DROP -> Color(0xFF00CCFF)
                        SetType.FAILURE -> Color(0xFFFF4444)
                        SetType.REST_PAUSE -> Color(0xFF00FFAA)
                        else -> Color.White
                    }
                    Text(label, color = color, fontSize = 14.sp)
                }

                // Weight Input
                EditableValueBox(
                    value = if (set.weight % 1 == 0f) set.weight.toInt().toString() else set.weight.toString(),
                    onValueChange = { it.toFloatOrNull()?.let { w -> viewModel.updateRoutineExerciseSet(routineExercise, index, weight = w) } },
                    modifier = Modifier.weight(1.5f),
                    keyboardType = KeyboardType.Decimal
                )
                
                // Reps Input
                EditableValueBox(
                    value = set.reps.toString(),
                    onValueChange = { it.toIntOrNull()?.let { r -> viewModel.updateRoutineExerciseSet(routineExercise, index, reps = r) } },
                    modifier = Modifier.weight(1.5f),
                    keyboardType = KeyboardType.Number
                )

                // Remove Set button
                IconButton(
                    onClick = { viewModel.removeSetFromRoutineExercise(routineExercise, index) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Remove Set", tint = Color.Gray, modifier = Modifier.size(16.dp))
                }
            }
        }

        TextButton(
            onClick = { viewModel.addSetToRoutineExercise(routineExercise) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("+ Add Set", color = Color(0xFF007AFF), fontSize = 14.sp)
        }
    }

    if (showSetTypeSelectorForIndex != null) {
        SetTypeSelectorBottomSheet(
            onTypeSelected = { type ->
                viewModel.updateRoutineExerciseSet(routineExercise, showSetTypeSelectorForIndex!!, type = type)
                showSetTypeSelectorForIndex = null
            },
            onRemoveSet = {
                viewModel.removeSetFromRoutineExercise(routineExercise, showSetTypeSelectorForIndex!!)
                showSetTypeSelectorForIndex = null
            },
            onDismiss = { showSetTypeSelectorForIndex = null }
        )
    }
}

@Composable
fun RoutineSetPlaceholder(modifier: Modifier = Modifier, label: String) {
    Box(
        modifier = modifier
            .height(40.dp)
            .background(Color(0xFF2C2C2E), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text("SET $label", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ActiveWorkoutContent(uiState: WorkoutUiState, viewModel: WorkoutViewModel) {
    var showExercisePicker by remember { mutableStateOf(false) }
    var showAugmentPicker by remember { mutableStateOf(false) }
    var exerciseToReplace by remember { mutableStateOf<WorkoutLog?>(null) }
    var showActionMenuFor by remember { mutableStateOf<WorkoutLog?>(null) }
    var showSupersetMenuFor by remember { mutableStateOf<WorkoutLog?>(null) }
    val filteredExercises by viewModel.filteredExercises.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        items(uiState.logs, key = { it.first.id }) { (log, sets) ->
            WorkoutLogCard(
                log = log,
                sets = sets,
                viewModel = viewModel,
                onActionMenuClick = { showActionMenuFor = log }
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { 
                        exerciseToReplace = null
                        showExercisePicker = true 
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1C1C1E)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Add Exercise", color = Color(0xFF007AFF), fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { showAugmentPicker = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .border(1.dp, Color(0xFF00CCFF), RoundedCornerShape(8.dp)),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Bolt, contentDescription = null, tint = Color(0xFF00CCFF), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Augment", color = Color(0xFF00CCFF), fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showActionMenuFor != null) {
        WorkoutExerciseActionMenu(
            onReorder = { 
                viewModel.startReordering()
                showActionMenuFor = null
            },
            onReplace = {
                exerciseToReplace = showActionMenuFor
                showExercisePicker = true
                showActionMenuFor = null
            },
            onAddSuperset = { 
                showSupersetMenuFor = showActionMenuFor
                showActionMenuFor = null
            },
            onRemove = {
                viewModel.removeWorkoutLog(showActionMenuFor!!)
                showActionMenuFor = null
            },
            onDismiss = { showActionMenuFor = null }
        )
    }

    if (showSupersetMenuFor != null) {
        SupersetSelectionMenu(
            sourceLog = showSupersetMenuFor!!,
            allLogs = uiState.logs.map { it.first },
            onLogSelected = { targetLog: WorkoutLog ->
                viewModel.createSuperset(showSupersetMenuFor!!, targetLog)
                showSupersetMenuFor = null
            },
            onDismiss = { showSupersetMenuFor = null }
        )
    }

    if (showExercisePicker) {
        Dialog(onDismissRequest = { showExercisePicker = false }) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.Black
            ) {
                ExercisePicker(
                    uiState = uiState,
                    exercises = filteredExercises,
                    onSearchChange = { viewModel.updateExerciseSearch(it) },
                    onSelect = {
                        if (exerciseToReplace != null) {
                            viewModel.replaceWorkoutLog(exerciseToReplace!!, it)
                            exerciseToReplace = null
                        } else {
                            viewModel.selectExercise(it)
                        }
                        showExercisePicker = false
                    },
                    onSaveCustomExercise = { name, muscle, equip, desc ->
                        viewModel.saveCustomExercise(name, muscle, equip, desc)
                    },
                    onDismiss = { 
                        showExercisePicker = false
                        exerciseToReplace = null
                    }
                )
            }
        }
    }

    if (showAugmentPicker) {
        Dialog(onDismissRequest = { showAugmentPicker = false }) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.Black
            ) {
                AugmentPicker(
                    augments = uiState.augments,
                    onSelect = {
                        viewModel.injectAugment(it)
                        showAugmentPicker = false
                    },
                    onDismiss = { showAugmentPicker = false }
                )
            }
        }
    }
}

@Composable
fun AugmentPicker(
    augments: List<WorkoutAugment>,
    onSelect: (WorkoutAugment) -> Unit,
    onDismiss: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Cancel",
                color = Color(0xFF007AFF),
                modifier = Modifier.clickable { onDismiss() }
            )
            Text(
                "Select Augment",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
            Box(Modifier.size(48.dp))
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(augments) { augment ->
                val color = Color(android.graphics.Color.parseColor(augment.colorHex))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(augment) },
                    color = Color(0xFF1C1C1E),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, color.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                augment.name,
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Box(
                                modifier = Modifier
                                    .background(color.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    augment.focusBodyPart.uppercase(),
                                    color = color,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            augment.exercises.joinToString(" • ") { it.exercise.name },
                            color = Color.Gray,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupersetSelectionMenu(
    sourceLog: WorkoutLog,
    allLogs: List<WorkoutLog>,
    onLogSelected: (WorkoutLog) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1C1C1E),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.Gray) }
    ) {
        Column(modifier = Modifier.padding(bottom = 32.dp)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Superset",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Superset ${sourceLog.exerciseName} with...",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            allLogs.forEach { log ->
                val isSource = log.id == sourceLog.id
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isSource) { onLogSelected(log) }
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = log.exerciseName,
                        color = if (isSource) Color.White.copy(alpha = 0.5f) else Color.White,
                        fontSize = 16.sp,
                        modifier = Modifier.weight(1f)
                    )
                    if (isSource) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = Color(0xFF007AFF),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                HorizontalDivider(color = Color.Gray.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 24.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutRoutineActionMenu(
    routine: WorkoutRoutine,
    onShare: () -> Unit,
    onDuplicate: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1C1C1E),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.Gray) }
    ) {
        Column(modifier = Modifier.padding(bottom = 32.dp)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    routine.name,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            ActionMenuItem(
                icon = Icons.Default.Share,
                label = "Share Routine",
                onClick = {
                    onShare()
                    onDismiss()
                }
            )
            ActionMenuItem(
                icon = Icons.Default.ContentCopy,
                label = "Duplicate Routine",
                onClick = {
                    onDuplicate()
                    onDismiss()
                }
            )
            ActionMenuItem(
                icon = Icons.Default.Edit,
                label = "Edit Routine",
                onClick = {
                    onEdit()
                    onDismiss()
                }
            )
            
            HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))
            
            ActionMenuItem(
                icon = Icons.Default.Close,
                label = "Delete Routine",
                color = Color.Red,
                onClick = {
                    onDelete()
                    onDismiss()
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutExerciseActionMenu(
    onReorder: () -> Unit,
    onReplace: () -> Unit,
    onAddSuperset: () -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1C1C1E),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.Gray) }
    ) {
        Column(modifier = Modifier.padding(bottom = 32.dp)) {
            ActionMenuItem(
                icon = Icons.Default.SwapVert,
                label = "Reorder Exercises",
                onClick = {
                    onReorder()
                    onDismiss()
                }
            )
            ActionMenuItem(
                icon = Icons.Default.Refresh,
                label = "Replace Exercise",
                onClick = {
                    onReplace()
                    // onDismiss handled by parent
                }
            )
            ActionMenuItem(
                icon = Icons.Default.Add,
                label = "Add To Superset",
                onClick = {
                    onAddSuperset()
                    onDismiss()
                }
            )
            
            HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))
            
            ActionMenuItem(
                icon = Icons.Default.Close,
                label = "Remove Exercise",
                color = Color.Red,
                onClick = {
                    onRemove()
                    // onDismiss handled by parent
                }
            )
        }
    }
}

@Composable
fun ReorderExercisesScreen(
    uiState: WorkoutUiState,
    onBack: () -> Unit,
    onMove: (Int, Int) -> Unit,
    onRemove: (WorkoutLog) -> Unit,
    onDone: () -> Unit
) {
    val logs = uiState.logs
    val lazyListState = rememberLazyListState()
    val density = LocalDensity.current
    
    // Drag and drop state
    var draggedItemIndex by remember { mutableStateOf<Int?>(null) }
    var draggingOffset by remember { mutableFloatStateOf(0f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(Color.Black)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text(
                "Reorder",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
            Box(Modifier.size(48.dp)) // Spacer for center alignment
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Exercise List
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp)
        ) {
            itemsIndexed(
                items = logs, 
                key = { _, item -> item.first.id }
            ) { index, logPair ->
                val log = logPair.first
                val isDragged = index == draggedItemIndex
                
                ReorderExerciseItem(
                    log = log,
                    modifier = Modifier
                        .then(if (isDragged) Modifier.zIndex(1f) else Modifier)
                        .graphicsLayer {
                            translationY = if (isDragged) draggingOffset else 0f
                            scaleX = if (isDragged) 1.02f else 1f
                            scaleY = if (isDragged) 1.02f else 1f
                        },
                    onRemove = { onRemove(log) },
                    onMoveUp = { if (index > 0) onMove(index, index - 1) },
                    onMoveDown = { if (index < uiState.logs.size - 1) onMove(index, index + 1) },
                    onDragStart = {
                        draggedItemIndex = index
                    },
                    onDrag = { offset ->
                        draggingOffset += offset
                        
                        // Intelligent swap logic
                        val itemHeight = with(density) { 72.dp.toPx() }
                        val threshold = itemHeight * 0.6f
                        
                        if (draggingOffset > threshold && index < uiState.logs.size - 1) {
                            onMove(index, index + 1)
                            draggedItemIndex = index + 1
                            draggingOffset -= itemHeight
                        } else if (draggingOffset < -threshold && index > 0) {
                            onMove(index, index - 1)
                            draggedItemIndex = index - 1
                            draggingOffset += itemHeight
                        }
                    },
                    onDragEnd = {
                        draggedItemIndex = null
                        draggingOffset = 0f
                    }
                )
            }
        }

        // Done Button - Fixed squish with padding and explicit height
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .navigationBarsPadding()
        ) {
            Button(
                onClick = onDone,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Done", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
        }
    }
}

@Composable
fun ReorderExerciseItem(
    log: WorkoutLog,
    modifier: Modifier = Modifier,
    onRemove: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Remove button
        IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
            Icon(
                Icons.Default.RemoveCircle,
                contentDescription = "Remove",
                tint = Color(0xFFEB5757),
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Thumbnail
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = Color.Black, modifier = Modifier.size(24.dp))
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Exercise Name
        Text(
            log.exerciseName,
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )

        // Navigation Controls (Arrows) + Drag Handle (Hamburger)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onMoveUp, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move Up", tint = Color.Gray)
            }
            IconButton(onClick = onMoveDown, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move Down", tint = Color.Gray)
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Hamburger Drag Handle - using a Box with pointerInput instead of IconButton to ensure gestures aren't consumed
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .pointerInput(Unit) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { onDragStart() },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                onDrag(dragAmount.y)
                            },
                            onDragEnd = { onDragEnd() },
                            onDragCancel = { onDragEnd() }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Menu,
                    contentDescription = "Drag to reorder",
                    tint = Color.Gray.copy(alpha = 0.6f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun ActionMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color = Color.White,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(label, color = color, fontSize = 16.sp)
    }
}

@Composable
fun ExercisePicker(
    uiState: WorkoutUiState,
    exercises: List<Exercise>,
    onSearchChange: (String) -> Unit,
    onSelect: (Exercise) -> Unit,
    onSaveCustomExercise: (String, String, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var showCustomExerciseForm by remember { mutableStateOf(false) }

    if (showCustomExerciseForm) {
        CustomExerciseForm(
            onSave = { name, muscle, equip, desc ->
                onSaveCustomExercise(name, muscle, equip, desc)
                showCustomExerciseForm = false
            },
            onDismiss = { showCustomExerciseForm = false }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Cancel",
                color = Color(0xFF007AFF),
                modifier = Modifier.clickable { onDismiss() }
            )
            Text(
                "Add Exercise",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
            Text(
                "Create",
                color = Color(0xFF007AFF),
                modifier = Modifier.clickable { showCustomExerciseForm = true }
            )
        }

        // Search Bar
        TextField(
            value = uiState.exerciseSearchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .heightIn(min = 44.dp),
            placeholder = { Text("Search exercise", color = Color.Gray, fontSize = 15.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF1C1C1E),
                unfocusedContainerColor = Color(0xFF1C1C1E),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            shape = RoundedCornerShape(10.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Filters
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterButton(
                label = uiState.selectedEquipment ?: "All Equipment",
                onClick = { /* Equipment dialog/menu */ },
                modifier = Modifier.weight(1f)
            )
            FilterButton(
                label = uiState.selectedMuscleGroup ?: "All Muscles",
                onClick = { /* Muscles dialog/menu */ },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                Text(
                    "Recent Exercises",
                    color = Color.Gray,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            items(exercises) { exercise ->
                ExerciseListItem(exercise) { onSelect(exercise) }
            }
            
            item {
                Text(
                    "Custom Exercises",
                    color = Color.Gray,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
                )
            }
            // Mock custom exercises for now if needed, or just filtered list
        }
    }
}

@Composable
fun FilterButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .height(36.dp)
            .clickable { onClick() },
        color = Color(0xFF1C1C1E),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(label, color = Color.White, fontSize = 14.sp)
            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.Gray)
        }
    }
}

@Composable
fun ExerciseListItem(exercise: Exercise, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Exercise Icon/Image
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.FitnessCenter,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    exercise.name,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    exercise.muscleGroups.firstOrNull() ?: "",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }

            Icon(
                Icons.AutoMirrored.Filled.TrendingUp,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(20.dp)
            )
        }
        HorizontalDivider(
            modifier = Modifier.padding(start = 80.dp),
            color = Color.DarkGray,
            thickness = 0.5.dp
        )
    }
}

@Composable
fun CustomExerciseForm(
    onSave: (String, String, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var muscleGroup by remember { mutableStateOf("") }
    var equipment by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            color = Color(0xFF1C1C1E),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    "Create Custom Exercise",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Exercise Name *", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF007AFF),
                        unfocusedBorderColor = Color.DarkGray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = muscleGroup,
                    onValueChange = { muscleGroup = it },
                    label = { Text("Muscle Group", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF007AFF),
                        unfocusedBorderColor = Color.DarkGray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = equipment,
                    onValueChange = { equipment = it },
                    label = { Text("Equipment", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF007AFF),
                        unfocusedBorderColor = Color.DarkGray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Instructions", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF007AFF),
                        unfocusedBorderColor = Color.DarkGray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    minLines = 3
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color.Gray)
                    }
                    Button(
                        onClick = { onSave(name, muscleGroup, equipment, description) },
                        enabled = name.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF))
                    ) {
                        Text("Save Exercise", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun WorkoutLogCard(
    log: WorkoutLog,
    sets: List<SetLog>,
    viewModel: WorkoutViewModel,
    onActionMenuClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val previousSets = uiState.previousLogs[log.exerciseId] ?: emptyList()
    var showSetTypeSelector by remember { mutableStateOf<SetLog?>(null) }
    
    val augmentColor = log.augmentColor?.let { Color(android.graphics.Color.parseColor(it)) } ?: Color(0xFF007AFF)

    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .then(
                if (log.augmentId != null) {
                    Modifier
                        .background(Color.Black)
                        .padding(2.dp)
                        .border(
                            width = 1.dp,
                            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(augmentColor, augmentColor.copy(alpha = 0.3f))
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp)
                } else {
                    Modifier
                }
            )
    ) {
        if (log.augmentId != null) {
            Text(
                text = log.augmentName?.uppercase() ?: "AUGMENT",
                color = augmentColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(40.dp).background(Color(0xFF1C1C1E), CircleShape))
                Spacer(modifier = Modifier.width(12.dp))
                Text(log.exerciseName, color = augmentColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = onActionMenuClick) {
                Icon(Icons.Default.MoreHoriz, contentDescription = "Exercise Actions", tint = Color.Gray)
            }
        }

        if (log.supersetId != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Icon(
                    Icons.Default.Link,
                    contentDescription = null,
                    tint = Color(0xFF007AFF),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    "SUPERSET",
                    color = Color(0xFF007AFF),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            }
        }

        Text("Add weight", color = Color.Gray, fontSize = 14.sp, modifier = Modifier.padding(vertical = 8.dp))
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Timer, contentDescription = null, tint = Color(0xFF007AFF), modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Rest Timer: 1min 0s", color = Color(0xFF007AFF), fontSize = 14.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Set Table Header
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("SET", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.weight(1f))
            Text("PREVIOUS", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.weight(2f))
            Text("LBS", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.weight(1.5f), textAlign = TextAlign.Center)
            Text("REPS", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.weight(1.5f), textAlign = TextAlign.Center)
            Icon(Icons.Default.Check, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))

        sets.forEachIndexed { index, set ->
            key(set.id) {
                val prevSet = previousSets.getOrNull(index)
                SetLogRow(
                    setNumber = index + 1,
                    set = set,
                    previousData = prevSet?.let { "${if (it.weight % 1 == 0f) it.weight.toInt() else it.weight}lbs x ${it.reps}" } ?: "",
                    onUpdateWeight = { viewModel.updateSet(set, weight = it) },
                    onUpdateReps = { viewModel.updateSet(set, reps = it) },
                    onCompleteToggle = { viewModel.updateSet(set, isCompleted = !set.isCompleted) },
                    onSetLabelClick = { showSetTypeSelector = set }
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        Button(
            onClick = { 
                val lastWeight = sets.lastOrNull()?.weight ?: 0f
                val lastReps = sets.lastOrNull()?.reps ?: 0
                viewModel.logSet(log, lastWeight, lastReps) 
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .height(36.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1C1C1E)),
            shape = RoundedCornerShape(4.dp)
        ) {
            Text("+ Add Set", color = Color.White, fontSize = 14.sp)
        }
    }

    if (showSetTypeSelector != null) {
        SetTypeSelectorBottomSheet(
            onTypeSelected = { type ->
                viewModel.updateSet(showSetTypeSelector!!, type = type)
                showSetTypeSelector = null
            },
            onRemoveSet = {
                viewModel.removeSet(showSetTypeSelector!!)
                showSetTypeSelector = null
            },
            onDismiss = { showSetTypeSelector = null }
        )
    }
}

@Composable
fun SetLogRow(
    setNumber: Int,
    set: SetLog,
    previousData: String,
    onUpdateWeight: (Float) -> Unit,
    onUpdateReps: (Int) -> Unit,
    onCompleteToggle: () -> Unit,
    onSetLabelClick: () -> Unit
) {
    val backgroundColor = if (setNumber % 2 == 0) Color.Transparent else Color(0xFF1C1C1E).copy(alpha = 0.3f)
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Set Label (Clickable to change type)
        Box(
            modifier = Modifier
                .weight(1f)
                .clickable { onSetLabelClick() },
            contentAlignment = Alignment.CenterStart
        ) {
            val label = when (set.type) {
                SetType.NORMAL -> "$setNumber"
                SetType.WARMUP -> "W"
                SetType.DROP -> "D"
                SetType.FAILURE -> "F"
                SetType.REST_PAUSE -> "RP"
            }
            val color = when (set.type) {
                SetType.WARMUP -> Color(0xFFFFA500)
                SetType.DROP -> Color(0xFF00CCFF)
                SetType.FAILURE -> Color(0xFFFF4444)
                SetType.REST_PAUSE -> Color(0xFF00FFAA)
                else -> Color.White
            }
            Text(label, color = color, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        // Previous Data
        Text(previousData, color = Color.Gray, fontSize = 14.sp, modifier = Modifier.weight(2f))
        
        // Weight Input
        EditableValueBox(
            value = if (set.weight % 1 == 0f) set.weight.toInt().toString() else set.weight.toString(),
            onValueChange = { it.toFloatOrNull()?.let { w -> onUpdateWeight(w) } },
            modifier = Modifier.weight(1.5f),
            keyboardType = KeyboardType.Decimal
        )
        
        // Reps Input
        EditableValueBox(
            value = set.reps.toString(),
            onValueChange = { it.toIntOrNull()?.let { r -> onUpdateReps(r) } },
            modifier = Modifier.weight(1.5f),
            keyboardType = KeyboardType.Number
        )
        
        // Completion Check
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(
                    if (set.isCompleted) Color(0xFF4CD964) else Color.Gray.copy(alpha = 0.5f), 
                    RoundedCornerShape(4.dp)
                )
                .clickable { onCompleteToggle() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Check, 
                contentDescription = null, 
                tint = if (set.isCompleted) Color.White else Color.White.copy(alpha = 0.3f), 
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun EditableValueBox(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Number
) {
    var text by remember { mutableStateOf(value) }
    var isFocused by remember { mutableStateOf(false) }

    // Sync from external value only when not focused to prevent "jumping" or "predicting"
    // while the user is actively typing or has cleared the box.
    LaunchedEffect(value) {
        if (!isFocused) {
            text = value
        }
    }

    Box(
        modifier = modifier
            .padding(horizontal = 4.dp)
            .background(Color(0xFF1C1C1E), RoundedCornerShape(4.dp))
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        BasicTextField(
            value = text,
            onValueChange = {
                // Allow empty string or numeric values only, and limit length
                if (it.isEmpty() || it.toDoubleOrNull() != null || it == "." || it == ",") {
                    if (it.length <= 6) {
                        text = it
                        onValueChange(it)
                    }
                }
            },
            textStyle = TextStyle(
                color = Color.White,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                fontFamily = FontFamily.Monospace
            ),
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                autoCorrectEnabled = false
            ),
            cursorBrush = SolidColor(Color(0xFF007AFF)),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { 
                    isFocused = it.isFocused
                    if (!it.isFocused) {
                        // Re-sync on focus loss to ensure valid domain state
                        text = value
                    }
                }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetTypeSelectorBottomSheet(
    onTypeSelected: (SetType) -> Unit,
    onRemoveSet: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1C1C1E),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.Gray) }
    ) {
        Column(modifier = Modifier.padding(16.dp).padding(bottom = 32.dp)) {
            Text(
                "Select Set Type",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            
            SetTypeOption("W", "Warm Up Set", Color(0xFFFFA500)) { onTypeSelected(SetType.WARMUP) }
            SetTypeOption("1", "Normal Set", Color.White) { onTypeSelected(SetType.NORMAL) }
            SetTypeOption("F", "Failure Set", Color(0xFFFF4444)) { onTypeSelected(SetType.FAILURE) }
            SetTypeOption("D", "Drop Set", Color(0xFF00CCFF)) { onTypeSelected(SetType.DROP) }
            SetTypeOption("RP", "Rest Pause", Color(0xFF00FFAA)) { onTypeSelected(SetType.REST_PAUSE) }
            
            HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 8.dp))
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onRemoveSet() }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Close, contentDescription = null, tint = Color.Red, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Text("Remove Set", color = Color.Red, fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun SetTypeOption(label: String, description: String, color: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.width(40.dp),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(description, color = Color.White, fontSize = 16.sp, modifier = Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
    }
    HorizontalDivider(color = Color.Gray.copy(alpha = 0.1f))
}

@Composable
fun ExploreProtocolsDialog(
    uiState: WorkoutUiState,
    onStartProtocol: (WorkoutProtocol) -> Unit,
    onAddAugment: (WorkoutAugment) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            color = Color(0xFF1C1C1E),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFF00CCFF).copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    "PROTOCOL LIBRARY",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text("SYSTEM PROTOCOLS", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    
                    items(WorkoutProtocol.entries) { protocol ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onStartProtocol(protocol) },
                            color = Color(0xFF2C2C2E),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(protocol.name, color = Color.White, fontWeight = FontWeight.Bold)
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color(0xFF00CCFF))
                            }
                        }
                    }
                    
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("SUB-PROTOCOLS / AUGMENTS", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    
                    items(uiState.exploreAugments) { augment ->
                        val color = Color(android.graphics.Color.parseColor(augment.colorHex))
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFF2C2C2E),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(augment.name, color = Color.White, fontWeight = FontWeight.Bold)
                                        Text(augment.focusBodyPart.uppercase(), color = color, fontSize = 10.sp, fontWeight = FontWeight.Black)
                                    }
                                    
                                    IconButton(
                                        onClick = { onAddAugment(augment) },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(color.copy(alpha = 0.2f), CircleShape)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Add to Library", tint = color, modifier = Modifier.size(20.dp))
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Text(
                                    augment.exercises.joinToString(", ") { it.exercise.name },
                                    color = Color.Gray,
                                    fontSize = 12.sp,
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                ) {
                    Text("CLOSE", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
