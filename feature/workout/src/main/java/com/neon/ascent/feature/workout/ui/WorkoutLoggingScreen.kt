package com.neon.ascent.feature.workout.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
                } else {
                    WorkoutIntakeScreen(
                        uiState = uiState,
                        onBack = onBack,
                        onStartProtocol = { viewModel.startSession(it) },
                        onStartRoutine = { viewModel.startRoutine(it) },
                        onCreateRoutine = { viewModel.startCreateRoutine() }
                    )
                }
            } else {
                val durationFormatted = remember(uiState.workoutDurationSeconds) {
                    val minutes = uiState.workoutDurationSeconds / 60
                    val seconds = uiState.workoutDurationSeconds % 60
                    "%d:%02d".format(minutes, seconds)
                }
                ActiveWorkoutHeader(
                    duration = durationFormatted,
                    onBack = onBack,
                    onFinish = { /* Finish logic */ },
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
    onCreateRoutine: () -> Unit
) {
    var isRoutinesExpanded by remember { mutableStateOf(true) }
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

        // New Routine & Explore Buttons
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IntakeActionButton(
                icon = Icons.AutoMirrored.Filled.Assignment,
                label = "New Routine",
                modifier = Modifier.weight(1f),
                onClick = onCreateRoutine
            )
            IntakeActionButton(
                icon = Icons.Default.Search,
                label = "Explore",
                modifier = Modifier.weight(1f),
                onClick = { showExploreProtocols = true }
            )
        }

        if (showExploreProtocols) {
            AlertDialog(
                onDismissRequest = { showExploreProtocols = false },
                title = { Text("Explore Protocols", color = Color.White) },
                text = {
                    Column {
                        WorkoutProtocol.entries.forEach { protocol ->
                            Text(
                                protocol.name,
                                color = Color(0xFF00CCFF),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onStartProtocol(protocol)
                                        showExploreProtocols = false
                                    }
                                    .padding(16.dp)
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showExploreProtocols = false }) {
                        Text("Close")
                    }
                },
                containerColor = Color(0xFF1C1C1E)
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
                RoutineCard(routine, onStart = { onStartRoutine(routine) })
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
fun RoutineCard(routine: WorkoutRoutine, onStart: () -> Unit) {
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
                Icon(Icons.Default.MoreHoriz, contentDescription = null, tint = Color.Gray)
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                routine.exercises.joinToString(", ") { it.name },
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
fun CreateRoutineScreen(
    uiState: WorkoutUiState,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onUpdateName: (String) -> Unit,
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
                "Create Routine",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
            Text(
                "Save",
                color = if (uiState.newRoutineName.isNotBlank() && uiState.newRoutineExercises.isNotEmpty()) Color(0xFF007AFF) else Color.Gray,
                modifier = Modifier.clickable(enabled = uiState.newRoutineName.isNotBlank() && uiState.newRoutineExercises.isNotEmpty()) { onSave() },
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

        if (uiState.newRoutineExercises.isEmpty()) {
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
                    "Get started by adding an exercise to your routine.",
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
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add exercise", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(uiState.newRoutineExercises) { exercise ->
                    RoutineExerciseItem(
                        exercise = exercise,
                        onRemove = { viewModel.removeExerciseFromNewRoutine(exercise) }
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
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF007AFF))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add exercise", color = Color(0xFF007AFF), fontWeight = FontWeight.Bold)
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
                    onDismiss = { showExercisePicker = false }
                )
            }
        }
    }
}

@Composable
fun RoutineExerciseItem(exercise: Exercise, onRemove: () -> Unit) {
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
                exercise.name,
                color = Color(0xFF007AFF),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.Gray, modifier = Modifier.size(20.dp))
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Mocking set entry for now as domain doesn't support routine sets yet
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RoutineSetPlaceholder(Modifier.weight(1f), "1")
            RoutineSetPlaceholder(Modifier.weight(1f), "2")
            RoutineSetPlaceholder(Modifier.weight(1f), "3")
        }
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
    val filteredExercises by viewModel.filteredExercises.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        items(uiState.logs) { (log, sets) ->
            WorkoutLogCard(log, sets, viewModel)
            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            Button(
                onClick = { showExercisePicker = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1C1C1E)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Add Exercise", color = Color(0xFF007AFF), fontWeight = FontWeight.Bold)
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
                        viewModel.selectExercise(it)
                        showExercisePicker = false
                    },
                    onDismiss = { showExercisePicker = false }
                )
            }
        }
    }
}

@Composable
fun ExercisePicker(
    uiState: WorkoutUiState,
    exercises: List<Exercise>,
    onSearchChange: (String) -> Unit,
    onSelect: (Exercise) -> Unit,
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
                "Add Exercise",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
            Text(
                "Create",
                color = Color(0xFF007AFF),
                modifier = Modifier.clickable { /* Create custom logic */ }
            )
        }

        // Search Bar
        TextField(
            value = uiState.exerciseSearchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(44.dp),
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
fun WorkoutLogCard(log: WorkoutLog, sets: List<SetLog>, viewModel: WorkoutViewModel) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(40.dp).background(Color(0xFF1C1C1E), CircleShape))
                Spacer(modifier = Modifier.width(12.dp))
                Text(log.exerciseName, color = Color(0xFF007AFF), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Icon(Icons.Default.MoreHoriz, contentDescription = null, tint = Color.Gray)
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
            SetLogRow(index + 1, set)
            Spacer(modifier = Modifier.height(4.dp))
        }

        Button(
            onClick = { viewModel.logSet(log, 135f, 10) }, // Default values for now
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
}

@Composable
fun SetLogRow(setNumber: Int, set: SetLog) {
    val backgroundColor = if (setNumber % 2 == 0) Color.Transparent else Color(0xFF1C1C1E).copy(alpha = 0.3f)
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            if (set.isWarmup) "W" else "$setNumber",
            color = if (set.isWarmup) Color(0xFFFFA500) else Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        Text("135lbs x 5", color = Color.Gray, fontSize = 14.sp, modifier = Modifier.weight(2f))
        
        Box(
            modifier = Modifier
                .weight(1.5f)
                .padding(horizontal = 4.dp)
                .background(Color(0xFF1C1C1E), RoundedCornerShape(4.dp))
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("${set.weight.toInt()}", color = Color.White, fontSize = 14.sp)
        }
        
        Box(
            modifier = Modifier
                .weight(1.5f)
                .padding(horizontal = 4.dp)
                .background(Color(0xFF1C1C1E), RoundedCornerShape(4.dp))
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("${set.reps}", color = Color.White, fontSize = 14.sp)
        }
        
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
    }
}
