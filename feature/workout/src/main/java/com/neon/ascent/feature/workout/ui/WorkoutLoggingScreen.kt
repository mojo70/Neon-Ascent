package com.neon.ascent.feature.workout.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.zIndex
import androidx.compose.ui.input.pointer.pointerInput
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
import androidx.compose.ui.unit.Density
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import android.os.Build
import androidx.compose.ui.graphics.vector.ImageVector
import com.neon.ascent.core.domain.workout.models.*
import com.neon.ascent.core.domain.workout.rules.CyberCrappRules
import com.neon.ascent.core.domain.workout.rules.RepRange

@Composable
fun WorkoutLoggingScreen(
    onBack: () -> Unit,
    viewModel: WorkoutViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showRoutineActionMenuFor by remember { mutableStateOf<WorkoutRoutine?>(null) }
    var showAugmentActionMenuFor by remember { mutableStateOf<WorkoutAugment?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
    ) {
        if (uiState.userProfile == null && !uiState.isLoading) {
            OnboardingScreen(onComplete = { viewModel.resumeUserProfile() })
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                if (uiState.isExploringProtocols) {
                    WorkoutExploreScreen(
                        uiState = uiState,
                        onBack = { viewModel.hideExploreProtocols() },
                        onProtocolClick = { viewModel.showProtocolDetail(it) },
                        onAddProtocol = { viewModel.addProtocolToLibrary(it) },
                        onAddAugment = { 
                            viewModel.toggleAugmentLibrary(it) 
                            if (uiState.session != null) {
                                viewModel.injectAugment(it)
                                viewModel.hideExploreProtocols()
                            }
                        },
                        onAddRoutine = { viewModel.toggleRoutineLibrary(it) }
                    )
                } else if (uiState.session == null) {
                    if (uiState.isShowingProgress) {
                        WorkoutProgressScreen(
                            onBack = { viewModel.hideProgress() }
                        )
                    } else if (uiState.isCreatingRoutine) {
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
                    } else if (uiState.configuringProtocol != null) {
                        ProtocolConfigScreen(
                            protocol = uiState.configuringProtocol!!,
                            tempProfile = uiState.tempConfigProfile!!,
                            onUpdateSchedule = { viewModel.updateConfigSchedule(it) },
                            onSave = { viewModel.saveProtocolConfiguration() },
                            onCancel = { viewModel.cancelProtocolConfiguration() }
                        )
                    } else if (uiState.selectedRoutineForPreview != null) {
                        RoutinePreviewScreen(
                            routine = uiState.selectedRoutineForPreview!!,
                            onBack = { viewModel.hideRoutinePreview() },
                            onAdd = { 
                                viewModel.toggleRoutineLibrary(it)
                                viewModel.hideRoutinePreview()
                            },
                            onStart = { 
                                viewModel.startRoutine(it)
                                viewModel.hideRoutinePreview()
                                viewModel.hideProtocolDetail()
                                viewModel.hideExploreProtocols()
                            }
                        )
                    } else if (uiState.selectedProtocolForDetail != null) {
                        ProtocolDetailScreen(
                            protocol = uiState.selectedProtocolForDetail!!,
                            uiState = uiState,
                            onBack = { viewModel.hideProtocolDetail() },
                            onStartProtocol = { 
                                viewModel.startSession(it)
                                viewModel.hideExploreProtocols()
                            },
                            onAddProtocol = { viewModel.addProtocolToLibrary(it) },
                            onAddRoutine = { viewModel.toggleRoutineLibrary(it) },
                            onRoutineClick = { viewModel.showRoutinePreview(it) }
                        )
                    } else {
                        WorkoutIntakeScreen(
                            uiState = uiState,
                            onBack = onBack,
                            onShowProgress = { viewModel.showProgress() },
                            onStartProtocol = { viewModel.startSession(it) },
                            onStartRoutine = { viewModel.startRoutine(it) },
                            onStartAugment = { viewModel.startAugment(it) },
                            onCreateRoutine = { viewModel.startCreateRoutine() },
                            onCreateAugment = { viewModel.startCreateAugment() },
                            onRoutineActionClick = { showRoutineActionMenuFor = it },
                            onAugmentActionClick = { showAugmentActionMenuFor = it },
                            onExplore = { viewModel.startExploreProtocols() },
                            onAddAugment = { viewModel.toggleAugmentLibrary(it) },
                            onAddRoutine = { viewModel.toggleRoutineLibrary(it) },
                            onDeactivateProtocol = { viewModel.initiateDeactivateProtocol() }
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
                    val originalDensity = LocalDensity.current
                    val scaledDensity = remember(originalDensity, uiState.zoomLevel) {
                        Density(
                            density = originalDensity.density * uiState.zoomLevel,
                            fontScale = originalDensity.fontScale
                        )
                    }
                    val transformableState = rememberTransformableState { zoomChange, _, _ ->
                        val newZoom = (uiState.zoomLevel * zoomChange).coerceIn(0.9f, 2.0f)
                        viewModel.updateZoomLevel(newZoom)
                    }

                    CompositionLocalProvider(LocalDensity provides scaledDensity) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .transformable(state = transformableState)
                        ) {
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
                            WorkoutSummaryBar(uiState, onToggleSomatotype = { viewModel.toggleSomatotypeInfluence() })
                            
                            if (uiState.somatotypeNudgeText != null) {
                                SomatotypeBadge(uiState.somatotypeNudgeText!!, uiState.userProfile?.somatotype)
                            }

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
        }

        // Deactivate Protocol Dialog
        if (uiState.showDeactivateProtocolDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.cancelDeactivateProtocol() },
                title = { Text("DEACTIVATE PROTOCOL", color = Color.White, fontWeight = FontWeight.Black) },
                text = { 
                    Column {
                        Text("Are you sure you want to deactivate your active protocol?", color = Color.Gray)
                        Spacer(Modifier.height(16.dp))
                        Text("Would you also like to remove the associated routines from your library?", color = Color.White, fontSize = 14.sp)
                    }
                },
                confirmButton = {
                    Column {
                        Button(
                            onClick = { viewModel.confirmDeactivateProtocol(removeRoutines = true) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                        ) {
                            Text("DEACTIVATE & REMOVE ROUTINES", fontSize = 12.sp, fontWeight = FontWeight.Black)
                        }
                        Spacer(Modifier.height(8.dp))
                        TextButton(
                            onClick = { viewModel.confirmDeactivateProtocol(removeRoutines = false) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("DEACTIVATE ONLY (KEEP ROUTINES)", color = Color(0xFF00FF9C), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.cancelDeactivateProtocol() }) {
                        Text("CANCEL", color = Color.Gray)
                    }
                },
                containerColor = Color(0xFF1C1C1E)
            )
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

        if (showAugmentActionMenuFor != null) {
            WorkoutAugmentActionMenu(
                augment = showAugmentActionMenuFor!!,
                onShare = { viewModel.shareAugment(showAugmentActionMenuFor!!) },
                onDuplicate = { viewModel.duplicateAugment(showAugmentActionMenuFor!!) },
                onEdit = { 
                    viewModel.editAugment(showAugmentActionMenuFor!!)
                    showAugmentActionMenuFor = null
                },
                onDelete = { viewModel.deleteAugment(showAugmentActionMenuFor!!) },
                onDismiss = { showAugmentActionMenuFor = null }
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

        if (uiState.selectedExerciseForDetail != null) {
            ExerciseDetailModal(
                exercise = uiState.selectedExerciseForDetail!!,
                availableExercises = uiState.availableExercises,
                onExerciseClick = { viewModel.showExerciseDetail(it) },
                onDismiss = { viewModel.hideExerciseDetail() }
            )
        }

        if (uiState.showSubstitutionDialog && uiState.exerciseToSubstitute != null) {
            ExerciseSubstitutionDialog(
                oldExerciseId = uiState.exerciseToSubstitute!!,
                recommendations = uiState.recommendedSubstitutes,
                onSubstitute = { newExercise ->
                    viewModel.substituteExercise(uiState.exerciseToSubstitute!!, newExercise)
                },
                onBrowseLibrary = {
                    viewModel.dismissSubstitution()
                },
                onDismiss = { viewModel.dismissSubstitution() }
            )
        }

        // Rest Pause Phase Dialogs
        if (uiState.showCyberFinisher && uiState.workoutPhase == RestPausePhase.FINISHER) {
            CyberFinisherDialog(onDone = { viewModel.startStretch() })
        }
        if (uiState.showLoadedStretch && uiState.workoutPhase == RestPausePhase.LOADED_STRETCH) {
            LoadedStretchDialog(remaining = uiState.stretchTimeRemaining)
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
fun WorkoutSummaryBar(uiState: WorkoutUiState, onToggleSomatotype: () -> Unit = {}) {
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
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SummaryStat("Duration", "${durationMinutes}m", Color(0xFF007AFF))
        SummaryStat("Volume", "%,d lbs".format(totalVolume), Color.White)
        SummaryStat("Sets", "$totalSets", Color.White)
        
        IconButton(onClick = onToggleSomatotype) {
            Icon(
                Icons.Default.SettingsInputComponent, 
                contentDescription = "Toggle Somatotype Influence", 
                tint = if (uiState.useSomatotypeInfluence) Color(0xFF00FF9C) else Color.Gray,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun SomatotypeBadge(text: String, somatotype: Somatotype?) {
    val color = when (somatotype) {
        Somatotype.ECTOMORPH -> Color(0xFF00CCFF)
        Somatotype.ENDOMORPH -> Color(0xFFFF006E)
        else -> Color(0xFF00FF9C)
    }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
            .border(0.5.dp, color.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
            .padding(vertical = 4.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text.uppercase(),
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp
        )
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
    onShowProgress: () -> Unit,
    onStartProtocol: (WorkoutProtocol) -> Unit,
    onStartRoutine: (WorkoutRoutine) -> Unit,
    onStartAugment: (WorkoutAugment) -> Unit,
    onCreateRoutine: () -> Unit,
    onCreateAugment: () -> Unit,
    onRoutineActionClick: (WorkoutRoutine) -> Unit,
    onAugmentActionClick: (WorkoutAugment) -> Unit,
    onExplore: () -> Unit,
    onAddAugment: (WorkoutAugment) -> Unit,
    onAddRoutine: (WorkoutRoutine) -> Unit,
    onDeactivateProtocol: () -> Unit
) {
    var isRoutinesExpanded by remember { mutableStateOf(true) }
    var isAugmentsExpanded by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text(
                    "WORKOUT",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
            }
            Row {
                IconButton(onClick = onShowProgress) {
                    Icon(
                        Icons.AutoMirrored.Filled.TrendingUp,
                        contentDescription = "Progress",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
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
        }

        Spacer(modifier = Modifier.height(20.dp))

        uiState.userProfile?.activeProtocol?.let { protocol ->
            ActiveProtocolCard(
                protocol = protocol,
                onDeactivate = onDeactivateProtocol
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

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
                onClick = onExplore
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
                    onStart = { onStartAugment(augment) },
                    onActionClick = { onAugmentActionClick(augment) }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
        
        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
fun ActiveProtocolCard(protocol: WorkoutProtocol, onDeactivate: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        color = Color(0xFF00FF9C).copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFF00FF9C).copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Adjust, contentDescription = null, tint = Color(0xFF00FF9C), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ACTIVE PROTOCOL", color = Color(0xFF00FF9C), fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                }
                Icon(
                    if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color(0xFF00FF9C).copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                protocol.displayName,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(top = 4.dp)
            )
            
            if (expanded) {
                Text(
                    protocol.description,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(top = 12.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                Text("PROTOCOL TENANTS", color = Color(0xFF00FF9C), fontSize = 10.sp, fontWeight = FontWeight.Black)
                protocol.tenants.forEach { tenant ->
                    Row(modifier = Modifier.padding(top = 8.dp)) {
                        Text("•", color = Color(0xFF00FF9C), modifier = Modifier.padding(end = 8.dp))
                        Text(tenant, color = Color.Gray, fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = onDeactivate,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                    border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f))
                ) {
                    Text("DEACTIVATE PROTOCOL", fontSize = 12.sp, fontWeight = FontWeight.Black)
                }
            } else {
                Text(
                    "Strict rotation enabled. Next routine auto-sequenced.",
                    color = Color.Gray,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
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
fun AugmentCard(augment: WorkoutAugment, onStart: () -> Unit, onActionClick: () -> Unit) {
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

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                colors = ButtonDefaults.buttonColors(containerColor = color.copy(alpha = 0.8f)),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text("Start Protocol", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
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
                    onDetailClick = { viewModel.showExerciseDetail(it) },
                    onSelect = {
                        viewModel.addExerciseToNewRoutine(it)
                        showExercisePicker = false
                    },
                    onSaveCustomExercise = { name: String, muscle: String, equip: String, desc: String ->
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
                    myAugments = uiState.augments,
                    libraryAugments = uiState.exploreAugments,
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
                    onDetailClick = { viewModel.showExerciseDetail(it) },
                    onSelect = {
                        viewModel.addExerciseToNewRoutine(it)
                        showExercisePicker = false
                    },
                    onSaveCustomExercise = { name: String, muscle: String, equip: String, desc: String ->
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
    isPrescriptive: Boolean = false,
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
            Text("GOAL", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.weight(1.5f), textAlign = TextAlign.Center)
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
                // Set Label (Clickable to change type)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showSetTypeSelectorForIndex = index },
                    contentAlignment = Alignment.CenterStart
                ) {
                    val label = when (set.type) {
                        SetType.NORMAL -> "${index + 1}"
                        SetType.WARMUP -> "W"
                        SetType.DROP -> "D"
                        SetType.FAILURE -> "F"
                        SetType.REST_PAUSE -> "RP"
                        SetType.WIDOWMAKER -> "WM"
                        SetType.POWER -> "P"
                        SetType.GS -> "GS"
                    }
                    val color = when (set.type) {
                        SetType.WARMUP -> Color(0xFFFFA500)
                        SetType.DROP -> Color(0xFF00CCFF)
                        SetType.FAILURE -> Color(0xFFFF4444)
                        SetType.REST_PAUSE -> Color(0xFF00FFAA)
                        SetType.WIDOWMAKER -> Color(0xFFFF00FF)
                        SetType.POWER -> Color(0xFFFFD700)
                        SetType.GS -> Color(0xFF00CCFF)
                        else -> Color.White
                    }
                    Text(label, color = color, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                // Goal Input
                val isGoalMandatory = set.type == SetType.WIDOWMAKER || isPrescriptive
                EditableValueBox(
                    value = set.goalReps ?: "",
                    onValueChange = { viewModel.updateRoutineExerciseSet(routineExercise, index, goalReps = it) },
                    modifier = Modifier.weight(1.5f),
                    keyboardType = KeyboardType.Text,
                    enabled = !isGoalMandatory
                )

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
        val isPrescriptiveAugment = remember(showActionMenuFor, uiState.augments, uiState.exploreAugments) {
            showActionMenuFor?.augmentId != null && (uiState.augments + uiState.exploreAugments).any { it.id == showActionMenuFor?.augmentId && (it.isSystem) }
        }
        val isMandatoryGoal = uiState.session?.protocol == WorkoutProtocol.CYBER_CRAPP || isPrescriptiveAugment

        WorkoutExerciseActionMenu(
            workoutLog = showActionMenuFor!!,
            isMandatoryGoal = isMandatoryGoal,
            onReorder = { 
                viewModel.startReordering()
                showActionMenuFor = null
            },
            onReplace = {
                exerciseToReplace = showActionMenuFor
                showExercisePicker = true
                showActionMenuFor = null
            },
            onSubstitute = {
                viewModel.startSubstitution(showActionMenuFor!!.exerciseId)
                showActionMenuFor = null
            },
            onAddSuperset = { 
                showSupersetMenuFor = showActionMenuFor
                showActionMenuFor = null
            },
            onToggleGoalReps = {
                viewModel.toggleGoalReps(showActionMenuFor!!)
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
                    onDetailClick = { viewModel.showExerciseDetail(it) },
                    onSelect = {
                        if (exerciseToReplace != null) {
                            viewModel.replaceWorkoutLog(exerciseToReplace!!, it)
                            exerciseToReplace = null
                        } else {
                            viewModel.selectExercise(it)
                        }
                        showExercisePicker = false
                    },
                    onSaveCustomExercise = { name: String, muscle: String, equip: String, desc: String ->
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
                    myAugments = uiState.augments,
                    libraryAugments = uiState.exploreAugments,
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
    myAugments: List<WorkoutAugment>,
    libraryAugments: List<WorkoutAugment>,
    onSelect: (WorkoutAugment) -> Unit,
    onDismiss: () -> Unit
) {
    var showLibrary by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
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
            if (myAugments.isNotEmpty()) {
                item {
                    Text(
                        "MY SUB-PROTOCOLS",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                items(myAugments) { augment ->
                    AugmentPickerItem(augment, onSelect)
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showLibrary = !showLibrary }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "LIBRARY / EXPLORE",
                        color = Color(0xFF00CCFF),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        if (showLibrary) Icons.Default.ArrowDropDown else Icons.AutoMirrored.Filled.ArrowRight,
                        contentDescription = null,
                        tint = Color(0xFF00CCFF)
                    )
                }
            }

            if (showLibrary) {
                items(libraryAugments) { augment ->
                    AugmentPickerItem(augment, onSelect)
                }
            }
        }
    }
}

@Composable
fun AugmentPickerItem(augment: WorkoutAugment, onSelect: (WorkoutAugment) -> Unit) {
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
fun WorkoutAugmentActionMenu(
    augment: WorkoutAugment,
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
                    augment.name,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            ActionMenuItem(
                icon = Icons.Default.Share,
                label = "Share Sub Protocol",
                onClick = {
                    onShare()
                    onDismiss()
                }
            )
            ActionMenuItem(
                icon = Icons.Default.ContentCopy,
                label = "Duplicate Sub Protocol",
                onClick = {
                    onDuplicate()
                    onDismiss()
                }
            )
            ActionMenuItem(
                icon = Icons.Default.Edit,
                label = "Edit Sub Protocol",
                onClick = {
                    onEdit()
                    onDismiss()
                }
            )
            
            HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))
            
            ActionMenuItem(
                icon = Icons.Default.Close,
                label = "Delete Sub Protocol",
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
    workoutLog: WorkoutLog,
    isMandatoryGoal: Boolean,
    onReorder: () -> Unit,
    onReplace: () -> Unit,
    onSubstitute: () -> Unit,
    onAddSuperset: () -> Unit,
    onToggleGoalReps: () -> Unit,
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
                }
            )
            ActionMenuItem(
                icon = Icons.Default.Autorenew,
                label = "Substitute (Force Rotation)",
                onClick = {
                    onSubstitute()
                    onDismiss()
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

            if (!isMandatoryGoal) {
                ActionMenuItem(
                    icon = if (workoutLog.showGoalReps) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    label = if (workoutLog.showGoalReps) "Hide Goal Reps" else "Show Goal Reps",
                    onClick = {
                        onToggleGoalReps()
                        onDismiss()
                    }
                )
            }
            
            HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))
            
            ActionMenuItem(
                icon = Icons.Default.Close,
                label = "Remove Exercise",
                color = Color.Red,
                onClick = {
                    onRemove()
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
                val exercise = remember(uiState.availableExercises, log.exerciseId) {
                    uiState.availableExercises.find { it.id == log.exerciseId }
                }
                
                ReorderExerciseItem(
                    log = log,
                    exercise = exercise,
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
    exercise: Exercise?,
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
        val exerciseIcon = when (exercise?.equipment?.firstOrNull()) {
            "Cable", "Plate Loaded" -> Icons.Default.SettingsInputComponent
            "Bodyweight", "Weighted" -> Icons.AutoMirrored.Filled.DirectionsRun
            else -> Icons.Default.FitnessCenter
        }
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(exerciseIcon, contentDescription = null, tint = Color.Black, modifier = Modifier.size(24.dp))
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
    onDetailClick: (Exercise) -> Unit,
    onSelect: (Exercise) -> Unit,
    onSaveCustomExercise: (String, String, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var showCustomExerciseForm by remember { mutableStateOf(false) }

    if (showCustomExerciseForm) {
        CustomExerciseForm(
            onSave = { name: String, muscle: String, equip: String, desc: String ->
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
                ExerciseListItem(
                    exercise = exercise,
                    onDetailClick = { onDetailClick(it) },
                    onSelect = { onSelect(exercise) }
                )
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
fun ExerciseListItem(exercise: Exercise, onDetailClick: (Exercise) -> Unit, onSelect: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .clickable { onSelect() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Exercise Icon/Image
            val exerciseIcon = when (exercise.equipment.firstOrNull()) {
                "Cable", "Plate Loaded" -> Icons.Default.SettingsInputComponent
                "Bodyweight", "Weighted" -> Icons.AutoMirrored.Filled.DirectionsRun
                else -> Icons.Default.FitnessCenter
            }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color.White, CircleShape)
                    .clickable { onDetailClick(exercise) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    exerciseIcon,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onDetailClick(exercise) }
            ) {
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

            IconButton(onClick = onSelect) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add",
                    tint = Color(0xFF007AFF),
                    modifier = Modifier.size(24.dp)
                )
            }
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
    val previousLogs = uiState.previousLogs
    val previousSets = previousLogs[log.exerciseId] ?: emptyList()
    val progressionState = uiState.progressionStates[log.exerciseId]
    var showSetTypeSelector by remember { mutableStateOf<SetLog?>(null) }
    var showClusterDialogFor by remember { mutableStateOf<List<SetLog>?>(null) }
    
    val exercise = remember(uiState.availableExercises, log.exerciseId) {
        uiState.availableExercises.find { it.id == log.exerciseId }
    }

    val repRange: RepRange? = remember(exercise) { 
        exercise?.movementType?.let { CyberCrappRules.getRepRange(it) } 
    }
    
    val showWeightIncrease = remember(progressionState, repRange) {
        progressionState != null && repRange != null && progressionState.bestClusterReps >= repRange.max
    }
    
    val showStall = remember(progressionState) {
        progressionState != null && progressionState.consecutiveMisses >= 2
    }

    val isPrescriptiveAugment = remember(log.augmentId, uiState.augments, uiState.exploreAugments) {
        log.augmentId != null && (uiState.augments + uiState.exploreAugments).any { it.id == log.augmentId && (it.isSystem) }
    }
    
    val showGoalColumn = uiState.session?.protocol == WorkoutProtocol.CYBER_CRAPP || 
                        isPrescriptiveAugment ||
                        log.showGoalReps ||
                        sets.any { it.goalReps != null }
    
    val augmentColor = log.augmentColor?.let { Color(android.graphics.Color.parseColor(it)) } ?: Color(0xFF007AFF)

    var notesText by remember(exercise?.notes) { mutableStateOf(exercise?.notes ?: "") }
    var isEditing by remember { mutableStateOf(false) }
    var hasBeenFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isEditing) {
        if (isEditing) {
            hasBeenFocused = false
            focusRequester.requestFocus()
        }
    }

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
        if (showWeightIncrease) {
            ProgressionBanner(
                text = "WEIGHT INCREASE DUE (+2.5-5 lb) ⚡",
                color = Color(0xFF00FFCC),
                icon = Icons.Default.TrendingUp
            )
        }
        
        if (showStall) {
            ProgressionBanner(
                text = "STALL DETECTED: ROTATION RECOMMENDED ⚠️",
                color = Color(0xFFFF0066),
                icon = Icons.Default.Warning,
                onActionClick = { viewModel.startSubstitution(log.exerciseId) }
            )
        }

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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { 
                    uiState.availableExercises.find { it.id == log.exerciseId }?.let {
                        viewModel.showExerciseDetail(it)
                    }
                }
            ) {
                val exerciseIcon = when (exercise?.equipment?.firstOrNull()) {
                    "Cable", "Plate Loaded" -> Icons.Default.SettingsInputComponent
                    "Bodyweight", "Weighted" -> Icons.AutoMirrored.Filled.DirectionsRun
                    else -> Icons.Default.FitnessCenter
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFF1C1C1E), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        exerciseIcon,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(log.exerciseName, color = augmentColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    
                    // Comparison Logic
                    val comparisonText = remember(sets, previousSets, uiState.session?.protocol) {
                        val isCC = uiState.session?.protocol == WorkoutProtocol.CYBER_CRAPP
                        if (isCC) {
                            val prevClusterTotal = previousSets.filter { it.type == SetType.REST_PAUSE }.sumOf { it.reps }
                            val currentClusterTotal = sets.filter { it.type == SetType.REST_PAUSE }.sumOf { it.reps }
                            if (currentClusterTotal > 0 && prevClusterTotal > 0) {
                                val diff = currentClusterTotal - prevClusterTotal
                                "${if (diff >= 0) "+" else ""}$diff vs last 🔥"
                            } else null
                        } else {
                            val prevMaxW = previousSets.maxOfOrNull { it.weight } ?: 0f
                            val currentMaxW = sets.maxOfOrNull { it.weight } ?: 0f
                            if (currentMaxW > prevMaxW && prevMaxW > 0) "New Weight PR! 🚀"
                            else if (currentMaxW == prevMaxW && currentMaxW > 0) {
                                val prevMaxR = previousSets.filter { it.weight == prevMaxW }.maxOfOrNull { it.reps } ?: 0
                                val currentMaxR = sets.filter { it.weight == currentMaxW }.maxOfOrNull { it.reps } ?: 0
                                if (currentMaxR > prevMaxR) "+${currentMaxR - prevMaxR} reps vs last 🔥" else null
                            } else null
                        }
                    }
                    
                    comparisonText?.let {
                        Text(it, color = Color(0xFF00FF9C), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            IconButton(onClick = onActionMenuClick) {
                Icon(Icons.Default.MoreHoriz, contentDescription = "Exercise Actions", tint = Color.Gray)
            }
        }

        if (log.supersetId != null) {
            val label = if (sets.any { it.type == SetType.GS }) "GIANT SET" else "SUPERSET"
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
                    label,
                    color = Color(0xFF007AFF),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            }
        }

        if (isEditing) {
            OutlinedTextField(
                value = notesText,
                onValueChange = { newValue ->
                    notesText = newValue
                    if (exercise != null) {
                        viewModel.updateExerciseNotes(exercise.id, newValue)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused) {
                            hasBeenFocused = true
                        }
                        if (hasBeenFocused && !focusState.isFocused) {
                            isEditing = false
                        }
                    }
                    .padding(vertical = 4.dp),
                placeholder = { Text("Add notes...", color = Color.Gray, fontSize = 14.sp) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = augmentColor,
                    unfocusedBorderColor = Color.DarkGray,
                    cursorColor = augmentColor,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color(0xFF1C1C1E),
                    unfocusedContainerColor = Color(0xFF1C1C1E)
                ),
                textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                maxLines = 3
            )
        } else if (notesText.isNotEmpty()) {
            Text(
                text = notesText,
                color = Color.Gray,
                fontSize = 14.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isEditing = true }
                    .padding(vertical = 8.dp)
            )
        } else {
            Row(
                modifier = Modifier
                    .clickable { isEditing = true }
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = null,
                    tint = Color.Gray.copy(alpha = 0.7f),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "Add notes...",
                    color = Color.Gray.copy(alpha = 0.7f),
                    fontSize = 13.sp
                )
            }
        }
        
        val timerText = if (uiState.isResting) {
            "RESTING: ${uiState.restTimeRemaining}s"
        } else if (uiState.showLoadedStretch) {
            "STRETCHING: ${uiState.stretchTimeRemaining}s"
        } else {
            "Rest Timer: 1min 0s"
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Timer, contentDescription = null, tint = if (uiState.isResting || uiState.showLoadedStretch) Color(0xFF00FF9C) else Color(0xFF007AFF), modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(timerText, color = if (uiState.isResting || uiState.showLoadedStretch) Color(0xFF00FF9C) else Color(0xFF007AFF), fontSize = 14.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Set Table Header
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("SET", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.weight(1f))
            if (showGoalColumn) {
                Text("GOAL", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.weight(1.5f), textAlign = TextAlign.Center)
            }
            Text("PREVIOUS", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.weight(2f))
            Text("LBS", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.weight(1.5f), textAlign = TextAlign.Center)
            Text("REPS", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.weight(1.5f), textAlign = TextAlign.Center)
            Icon(Icons.Default.Check, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Grouping sets for display
        val displayItems = remember(sets) {
            val items = mutableListOf<Any>()
            var handledCluster = false
            sets.forEach { set ->
                if (set.clusterMiniSetIndex != null) {
                    if (!handledCluster) {
                        items.add(sets.filter { it.clusterMiniSetIndex != null })
                        handledCluster = true
                    }
                } else {
                    items.add(set)
                }
            }
            items
        }

        val userWeight = uiState.userProfile?.let { profile ->
        if (profile.unitSystem == UnitSystem.IMPERIAL) profile.weightKg * 2.20462f else profile.weightKg
    }
    val isBWExercise = exercise?.equipment?.contains("Bodyweight") == true || exercise?.equipment?.contains("Weighted") == true

    displayItems.forEachIndexed { index, item ->
            if (item is List<*>) {
                @Suppress("UNCHECKED_CAST")
                val clusterSets = item as List<SetLog>
                val clusterKey = clusterSets.firstOrNull()?.workoutLogId ?: "cluster_$index"
                key(clusterKey) {
                    val prevClusterSets = previousSets.filter { it.clusterMiniSetIndex != null }
                    val prevW = prevClusterSets.firstOrNull()?.weight ?: progressionState?.currentWeight ?: if (isBWExercise) userWeight else null
                    ClusterSetRow(
                        sets = clusterSets,
                        previousSets = prevClusterSets,
                        previousWeight = if (prevW != null && prevW > 0) prevW else null,
                        onUpdateWeight = { weight -> clusterSets.forEach { viewModel.updateSet(it, weight = weight) } },
                        onUpdateGoal = { goal -> clusterSets.forEach { viewModel.updateSet(it, goalReps = goal) } },
                        onClick = { showClusterDialogFor = clusterSets }
                    )
                }
            } else if (item is SetLog) {
                val set = item
                key(set.id) {
                    val currentTypeSets = sets.filter { it.type == set.type && it.clusterMiniSetIndex == null }
                    val setIndexInType = currentTypeSets.indexOf(set)
                    val prevTypeSets = previousSets.filter { it.type == set.type && it.clusterMiniSetIndex == null }
                    val prevSet = prevTypeSets.getOrNull(setIndexInType)
                    
                    val hasPrevData = prevSet != null && (prevSet.weight > 0 || prevSet.reps > 0)
                    val prevWFallback = if (set.type != SetType.WARMUP) (progressionState?.currentWeight ?: if (isBWExercise) userWeight else null) else (if (isBWExercise) userWeight else null)
                    val prevWeight = if (prevSet != null && prevSet.weight > 0) prevSet.weight else prevWFallback

                    SetLogRow(
                        setNumber = index + 1,
                        set = set,
                        showGoal = showGoalColumn,
                        previousData = if (hasPrevData) "${if (prevSet!!.weight % 1 == 0f) prevSet.weight.toInt() else prevSet.weight}lbs x ${prevSet.reps}" else if (prevSet != null) "0lbs x 0" else "0",
                        previousWeight = if (prevWeight != null && prevWeight > 0) prevWeight else null,
                        onUpdateWeight = { viewModel.updateSet(set, weight = it) },
                        onUpdateReps = { viewModel.updateSet(set, reps = it) },
                        onUpdateGoal = { viewModel.updateSet(set, goalReps = it) },
                        onCompleteToggle = { viewModel.updateSet(set, isCompleted = !set.isCompleted) },
                        onSetLabelClick = { showSetTypeSelector = set }
                    )
                }
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

    if (showClusterDialogFor != null) {
        val currentSets = uiState.logs.find { it.first.id == log.id }?.second ?: showClusterDialogFor!!
        val clusterSetsInLog = currentSets.filter { it.clusterMiniSetIndex != null }
        ClusterLoggingDialog(
            sets = if (clusterSetsInLog.isNotEmpty()) clusterSetsInLog else showClusterDialogFor!!,
            previousSets = previousSets.filter { it.clusterMiniSetIndex != null },
            uiState = uiState,
            viewModel = viewModel,
            onDismiss = { showClusterDialogFor = null }
        )
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
fun ProgressionBanner(
    text: String,
    color: Color,
    icon: ImageVector,
    onActionClick: (() -> Unit)? = null
) {
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .then(if (onActionClick != null) Modifier.clickable { onActionClick() } else Modifier)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = text,
                color = color,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseSubstitutionDialog(
    oldExerciseId: String,
    recommendations: List<Exercise>,
    onSubstitute: (Exercise) -> Unit,
    onBrowseLibrary: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1C1C1E),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.Gray) }
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "SUBSTITUTION RECOMMENDED",
                color = Color(0xFFFF0066),
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )
            Text(
                text = "Performance has plateaued. Select a similar movement to continue progression.",
                color = Color.Gray,
                fontSize = 12.sp,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            
            Spacer(Modifier.height(16.dp))
            
            recommendations.forEach { exercise ->
                Surface(
                    onClick = { onSubstitute(exercise) },
                    color = Color.Black.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = Color.White)
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(exercise.name, color = Color.White, fontWeight = FontWeight.Bold)
                            Text(exercise.equipment.joinToString(", "), color = Color.Gray, fontSize = 10.sp)
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            Button(
                onClick = onBrowseLibrary,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f))
            ) {
                Text("BROWSE MAIN LIBRARY", color = Color.White)
            }
        }
    }
}

@Composable
fun ExerciseDetailModal(
    exercise: Exercise,
    availableExercises: List<Exercise>,
    onExerciseClick: (Exercise) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            color = Color(0xFF050505),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        exercise.name.uppercase(),
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // GIF Section
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .background(Color(0xFF1C1C1E), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (exercise.gifAssetPath != null) {
                        val imageRequest = ImageRequest.Builder(context)
                            .data("file:///android_asset/${exercise.gifAssetPath}")
                            .decoderFactory(if (Build.VERSION.SDK_INT >= 28) ImageDecoderDecoder.Factory() else GifDecoder.Factory())
                            .build()
                        
                        AsyncImage(
                            model = imageRequest,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            Icons.Default.FitnessCenter,
                            contentDescription = null,
                            tint = Color.DarkGray,
                            modifier = Modifier.size(64.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Tags (Muscles & Equipment)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    exercise.muscleGroups.forEach { muscle ->
                        Surface(
                            color = Color(0xFF00FF9C).copy(alpha = 0.1f),
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(0.5.dp, Color(0xFF00FF9C).copy(alpha = 0.5f))
                        ) {
                            Text(
                                muscle.uppercase(),
                                color = Color(0xFF00FF9C),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    exercise.equipment.forEach { equip ->
                        Surface(
                            color = Color.Gray.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(0.5.dp, Color.Gray.copy(alpha = 0.5f))
                        ) {
                            Text(
                                equip.uppercase(),
                                color = Color.Gray,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Description
                Text(
                    "PROTOCOL DIRECTIVE",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
                Text(
                    exercise.description,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Cues
                if (exercise.cues.isNotEmpty()) {
                    Text(
                        "NEURAL CUES",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                    exercise.cues.forEach { cue ->
                        Row(modifier = Modifier.padding(top = 12.dp)) {
                            Text("•", color = Color(0xFF00FF9C), fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                cue,
                                color = Color.White,
                                fontSize = 14.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                // Substitutions
                if (exercise.injurySubstitutions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        "INJURY SUBSTITUTIONS",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                    exercise.injurySubstitutions.forEach { subId ->
                        val subName = availableExercises.find { it.id == subId }?.name ?: subId
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                                .clickable { 
                                    availableExercises.find { it.id == subId }?.let {
                                        onExerciseClick(it)
                                    }
                                },
                            color = Color.White.copy(alpha = 0.05f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(0.5.dp, Color.Gray.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = Color(0xFF00FF9C), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(subName, color = Color.White, fontSize = 14.sp)
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(40.dp))
                
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.05f)),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.2f))
                ) {
                    Text("RESUME PROTOCOL", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun CyberFinisherDialog(onDone: () -> Unit) {
    Dialog(onDismissRequest = { /* Force completion */ }) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            color = Color(0xFF050505),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFF00FF9C))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "CYBER FINISHER",
                    color = Color(0xFF00FF9C),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    "3-5 LENGTHENED PARTIALS",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    "Perform reps in the stretched position only. Focus on the bottom half of the movement.",
                    color = Color.Gray,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = onDone,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF9C)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("LOG & START STRETCH", color = Color.Black, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
fun LoadedStretchDialog(remaining: Int) {
    Dialog(onDismissRequest = { /* Force completion */ }) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            color = Color(0xFF050505),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFFFF006E))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "LOADED STRETCH",
                    color = Color(0xFFFF006E),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    "${remaining}s",
                    color = Color.White,
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    "Deep, weighted stretch. Breathe through the intensity.",
                    color = Color.Gray,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                LinearProgressIndicator(
                    progress = { remaining / 45f }, // Approx default
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                    color = Color(0xFFFF006E),
                    trackColor = Color.White.copy(alpha = 0.1f)
                )
            }
        }
    }
}

@Composable
fun ClusterSetRow(
    sets: List<SetLog>,
    previousSets: List<SetLog>,
    previousWeight: Float? = null,
    onUpdateWeight: (Float) -> Unit,
    onUpdateGoal: (String) -> Unit,
    onClick: () -> Unit
) {
    val totalReps = sets.filter { it.isCompleted }.sumOf { it.reps }
    val isCompleted = sets.all { it.isCompleted } && sets.isNotEmpty()
    val weight = sets.firstOrNull()?.weight ?: 0f
    val goalReps = sets.firstOrNull()?.goalReps ?: ""
    val prevTotalReps = previousSets.sumOf { it.reps }
    val weightPlaceholder = previousWeight?.let { if (it % 1 == 0f) it.toInt().toString() else it.toString() } ?: "0"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF00FFAA).copy(alpha = 0.05f), RoundedCornerShape(4.dp))
            .padding(vertical = 4.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Label (Clickable area)
        Box(
            modifier = Modifier
                .weight(1f)
                .clickable { onClick() }
                .padding(vertical = 4.dp)
        ) {
            Text(
                "RP",
                color = Color(0xFF00FFAA),
                fontWeight = FontWeight.Black,
                fontSize = 16.sp
            )
        }

        // Goal Input
        EditableValueBox(
            value = goalReps,
            onValueChange = { onUpdateGoal(it) },
            modifier = Modifier.weight(1.5f),
            keyboardType = KeyboardType.Text
        )

        // Previous (Clickable area)
        Box(
            modifier = Modifier
                .weight(2f)
                .clickable { onClick() }
                .padding(vertical = 4.dp)
        ) {
            Text(
                if (prevTotalReps > 0) "${prevTotalReps} total" else "-",
                color = Color.Gray,
                fontSize = 12.sp
            )
        }

        // Weight Input
        EditableValueBox(
            value = if (weight % 1 == 0f) weight.toInt().toString() else weight.toString(),
            onValueChange = { it.toFloatOrNull()?.let { w -> onUpdateWeight(w) } },
            placeholder = weightPlaceholder,
            modifier = Modifier.weight(1.5f),
            keyboardType = KeyboardType.Decimal
        )

        // Total Reps (Clickable area)
        Box(
            modifier = Modifier
                .weight(1.5f)
                .clickable { onClick() }
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                if (totalReps > 0) totalReps.toString() else "-",
                color = Color(0xFF00FFAA),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }

        // Completion Check (Clickable area)
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(
                    if (isCompleted) Color(0xFF4CD964) else Color.Gray.copy(alpha = 0.5f),
                    RoundedCornerShape(4.dp)
                )
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            if (isCompleted) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClusterLoggingDialog(
    sets: List<SetLog>,
    previousSets: List<SetLog>,
    uiState: WorkoutUiState,
    viewModel: WorkoutViewModel,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            color = Color(0xFF050505),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFF00FFAA).copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "REST-PAUSE CLUSTER",
                            color = Color(0xFF00FFAA),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        val weight = sets.firstOrNull()?.weight ?: 0f
                        Text(
                            "${if (weight % 1 == 0f) weight.toInt() else weight} LBS",
                            color = Color.Gray,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Timer Display if resting
                if (uiState.isResting) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF00FFAA).copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "NEURAL RECOVERY",
                            color = Color(0xFF00FFAA),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp
                        )
                        Text(
                            "${uiState.restTimeRemaining}s",
                            color = Color(0xFF00FFAA),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        )
                        LinearProgressIndicator(
                            progress = { uiState.restTimeRemaining / 15f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                                .height(2.dp),
                            color = Color(0xFF00FFAA),
                            trackColor = Color.Gray.copy(alpha = 0.2f),
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }

                sets.sortedBy { it.clusterMiniSetIndex }.forEachIndexed { index, set ->
                    val prevSet = previousSets.find { it.clusterMiniSetIndex == set.clusterMiniSetIndex }
                    val isActive = !set.isCompleted && (index == 0 || sets[index - 1].isCompleted) && !uiState.isResting
                    
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        color = if (isActive) Color(0xFF00FFAA).copy(alpha = 0.05f) else Color.Transparent,
                        shape = RoundedCornerShape(8.dp),
                        border = if (isActive) BorderStroke(1.dp, Color(0xFF00FFAA).copy(alpha = 0.3f)) else null
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "M-${index + 1}",
                                color = if (set.isCompleted) Color(0xFF00FFAA) else if (isActive) Color.White else Color.Gray,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(40.dp)
                            )
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text("LAST", color = Color.Gray, fontSize = 10.sp)
                                Text(
                                    prevSet?.reps?.toString() ?: "-",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            EditableValueBox(
                                value = if (set.reps == 0 && !set.isCompleted) "" else set.reps.toString(),
                                onValueChange = { 
                                    val newVal = it.toIntOrNull() ?: 0
                                    viewModel.updateSet(set, reps = newVal) 
                                },
                                modifier = Modifier.width(80.dp),
                                keyboardType = KeyboardType.Number,
                                enabled = !uiState.isResting
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            IconButton(
                                onClick = { viewModel.updateSet(set, isCompleted = !set.isCompleted) },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        if (set.isCompleted) Color(0xFF00FFAA) else Color.Gray.copy(alpha = 0.2f),
                                        RoundedCornerShape(8.dp)
                                    )
                            ) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = if (set.isCompleted) Color.Black else Color.White.copy(alpha = 0.5f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                val totalReps = sets.filter { it.isCompleted }.sumOf { it.reps }
                val prevTotal = previousSets.sumOf { it.reps }
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(8.dp))
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("TOTAL CLUSTER VOLUME", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text("$totalReps REPS", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
                    }
                    
                    if (prevTotal > 0) {
                        val diff = totalReps - prevTotal
                        val color = if (diff >= 0) Color(0xFF00FF9C) else Color(0xFFFF006E)
                        Column(horizontalAlignment = Alignment.End) {
                            Text("VS LAST", color = Color.Gray, fontSize = 10.sp)
                            Text(
                                "${if (diff >= 0) "+" else ""}$diff",
                                color = color,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFAA)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("SYNC DATA", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
fun SetLogRow(
    setNumber: Int,
    set: SetLog,
    showGoal: Boolean,
    previousData: String,
    previousWeight: Float? = null,
    onUpdateWeight: (Float) -> Unit,
    onUpdateReps: (Int) -> Unit,
    onUpdateGoal: (String) -> Unit,
    onCompleteToggle: () -> Unit,
    onSetLabelClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val backgroundColor = if (setNumber % 2 == 0) Color.Transparent else Color(0xFF1C1C1E).copy(alpha = 0.3f)
    val weightPlaceholder = previousWeight?.let { if (it % 1 == 0f) it.toInt().toString() else it.toString() } ?: "0"
    
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
                SetType.REST_PAUSE -> if (set.clusterMiniSetIndex != null) "RP ${set.clusterMiniSetIndex}" else "RP"
                SetType.WIDOWMAKER -> "WM"
                SetType.POWER -> "P"
                SetType.GS -> "GS"
            }
            val color = when (set.type) {
                SetType.WARMUP -> Color(0xFFFFA500)
                SetType.DROP -> Color(0xFF00CCFF)
                SetType.FAILURE -> Color(0xFFFF4444)
                SetType.REST_PAUSE -> Color(0xFF00FFAA)
                SetType.WIDOWMAKER -> Color(0xFFFF00FF)
                SetType.POWER -> Color(0xFFFFD700)
                SetType.GS -> Color(0xFF00CCFF)
                else -> Color.White
            }
            Text(label, color = color, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        // Goal Input
        if (showGoal) {
            val isGoalMandatory = set.type == SetType.WIDOWMAKER
            EditableValueBox(
                value = set.goalReps ?: "",
                onValueChange = { onUpdateGoal(it) },
                modifier = Modifier.weight(1.5f),
                keyboardType = KeyboardType.Text,
                enabled = !isGoalMandatory
            )
        }

        // Previous Data
        Text(previousData, color = Color.Gray, fontSize = 14.sp, modifier = Modifier.weight(2f))
        
        // Weight Input
        EditableValueBox(
            value = if (set.weight % 1 == 0f) set.weight.toInt().toString() else set.weight.toString(),
            onValueChange = { it.toFloatOrNull()?.let { w -> onUpdateWeight(w) } },
            placeholder = weightPlaceholder,
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
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onCompleteToggle()
                },
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
    keyboardType: KeyboardType = KeyboardType.Number,
    placeholder: String = "0",
    enabled: Boolean = true
) {
    var text by remember { mutableStateOf(if (value == "0" || value == "0.0") "" else value) }
    var isFocused by remember { mutableStateOf(false) }

    LaunchedEffect(value) {
        if (!isFocused) {
            text = if (value == "0" || value == "0.0") "" else value
        }
    }

    Box(
        modifier = modifier
            .padding(horizontal = 4.dp)
            .background(if (enabled) Color(0xFF1C1C1E) else Color.Transparent, RoundedCornerShape(4.dp))
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        if (text.isEmpty()) {
            Text(
                text = placeholder,
                color = if (isFocused) Color.Gray.copy(alpha = 0.3f) else Color.Gray.copy(alpha = 0.5f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                fontFamily = FontFamily.Monospace
            )
        }

        BasicTextField(
            value = text,
            onValueChange = {
                val isValid = if (keyboardType == KeyboardType.Text) {
                    it.length <= 10
                } else {
                    it.isEmpty() || it.toDoubleOrNull() != null || it == "." || it == "," || it == "-"
                }
                
                if (isValid) {
                    text = it
                    onValueChange(it)
                }
            },
            enabled = enabled,
            textStyle = TextStyle(
                color = if (enabled) Color.White else Color.Gray,
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
                    val wasFocused = isFocused
                    isFocused = it.isFocused
                    if (wasFocused && !it.isFocused) {
                        if (text.isEmpty()) {
                            onValueChange("0")
                        } else {
                            onValueChange(text)
                        }
                    } else if (!wasFocused && it.isFocused) {
                        if (text == "0" || text == "0.0") {
                            text = ""
                        }
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
            SetTypeOption("P", "Power Set (Explosive)", Color(0xFFFFD700)) { onTypeSelected(SetType.POWER) }
            SetTypeOption("F", "Failure Set", Color(0xFFFF4444)) { onTypeSelected(SetType.FAILURE) }
            SetTypeOption("D", "Drop Set", Color(0xFF00CCFF)) { onTypeSelected(SetType.DROP) }
            SetTypeOption("RP", "Rest Pause", Color(0xFF00FFAA)) { onTypeSelected(SetType.REST_PAUSE) }
            SetTypeOption("WM", "Widowmaker (20 Reps)", Color(0xFFFF00FF)) { onTypeSelected(SetType.WIDOWMAKER) }
            SetTypeOption("GS", "Giant Set", Color(0xFF00CCFF)) { onTypeSelected(SetType.GS) }
            
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
fun WorkoutExploreScreen(
    uiState: WorkoutUiState,
    onBack: () -> Unit,
    onProtocolClick: (WorkoutProtocol) -> Unit,
    onAddProtocol: (WorkoutProtocol) -> Unit,
    onAddAugment: (WorkoutAugment) -> Unit,
    onAddRoutine: (WorkoutRoutine) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text(
                "PROTOCOL ARCHIVE",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    "CORE METHODOLOGIES",
                    color = Color(0xFF00FF9C),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            items(WorkoutProtocol.entries) { protocol ->
                val protocolRoutines = uiState.exploreRoutines.filter { it.protocol == protocol }
                val allRoutinesAdded = protocolRoutines.isNotEmpty() && protocolRoutines.all { it.isAddedToLibrary }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onProtocolClick(protocol) },
                    color = Color(0xFF1C1C1E),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                protocol.displayName,
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                protocol.description,
                                color = Color.Gray,
                                fontSize = 12.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { onAddProtocol(protocol) },
                                modifier = Modifier.size(32.dp).background(if (allRoutinesAdded) Color(0xFF00FF9C).copy(alpha = 0.1f) else Color(0xFF00FF9C).copy(alpha = 0.2f), CircleShape)
                            ) {
                                Icon(
                                    if (allRoutinesAdded) Icons.Default.CheckCircle else Icons.Default.Add,
                                    contentDescription = "Add Protocol", 
                                    tint = Color(0xFF00FF9C), 
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.Gray)
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    "AUGMENTATION MODULES",
                    color = Color(0xFFFF006E),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            items(uiState.exploreAugments) { augment ->
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
                                Text(augment.name, color = Color.White, fontWeight = FontWeight.Bold)
                                Text(augment.focusBodyPart.uppercase(), color = color, fontSize = 10.sp, fontWeight = FontWeight.Black)
                            }
                            
                            IconButton(
                                onClick = { onAddAugment(augment) },
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(color.copy(alpha = 0.2f), CircleShape)
                            ) {
                                Icon(
                                    if (augment.isAddedToLibrary) Icons.Default.CheckCircle else Icons.Default.Add,
                                    contentDescription = "Add to Library", 
                                    tint = color, 
                                    modifier = Modifier.size(20.dp)
                                )
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
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProtocolConfigScreen(
    protocol: WorkoutProtocol,
    tempProfile: UserWorkoutProfile,
    onUpdateSchedule: (List<ScheduledDay>) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    val days = listOf("M", "T", "W", "T", "F", "S", "S")
    var showTimePickerForDay by remember { mutableStateOf<Int?>(null) }
    var applyTimeToAll by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
    ) {
        // Header
        Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            IconButton(onClick = onCancel, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Color.White)
            }
            Text(
                "PROTOCOL INITIALIZATION",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f).padding(horizontal = 24.dp)
        ) {
            item {
                Text(
                    "CALIBRATE UPLINK",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black
                )
                
                Text(
                    "Configure your training window and neural reminders for ${protocol.displayName}.",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    "TRAINING SCHEDULE",
                    color = Color(0xFF00FF9C),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("APPLY SAME TIME TO ALL", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Switch(
                        checked = applyTimeToAll, 
                        onCheckedChange = { applyTimeToAll = it }, 
                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00FF9C))
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    days.forEachIndexed { index, label ->
                        val dayId = index + 1
                        val scheduled = tempProfile.scheduledDays.find { it.dayOfWeek == dayId }
                        val isSelected = scheduled != null
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clickable { 
                                        if (isSelected) {
                                            onUpdateSchedule(tempProfile.scheduledDays.filter { it.dayOfWeek != dayId })
                                        } else {
                                            val baseTime = tempProfile.scheduledDays.firstOrNull()?.time ?: "09:00"
                                            onUpdateSchedule(tempProfile.scheduledDays + ScheduledDay(dayId, baseTime))
                                        }
                                    }, 
                                color = if (isSelected) Color(0xFF00FF9C).copy(alpha = 0.1f) else Color(0xFF1C1C1E), 
                                shape = CircleShape, 
                                border = BorderStroke(1.dp, if (isSelected) Color(0xFF00FF9C) else Color.DarkGray)
                            ) {
                                Box(contentAlignment = Alignment.Center) { 
                                    Text(label, color = if (isSelected) Color(0xFF00FF9C) else Color.Gray, fontWeight = FontWeight.Bold) 
                                }
                            }
                            if (scheduled != null) {
                                Text(
                                    scheduled.time, 
                                    color = Color.Gray, 
                                    fontSize = 9.sp, 
                                    modifier = Modifier.padding(top = 4.dp).clickable { showTimePickerForDay = dayId }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))

                Text(
                    "NEURAL REMINDERS",
                    color = Color(0xFF00FF9C),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF1C1C1E),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = Color(0xFF00FF9C))
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text("PUSH NOTIFICATIONS", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("Receive pings at the start of your neural windows.", color = Color.Gray, fontSize = 11.sp)
                        }
                        Spacer(Modifier.weight(1f))
                        Switch(checked = true, onCheckedChange = {}, enabled = false, colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00FF9C)))
                    }
                }
            }
        }

        // Action Button
        Box(modifier = Modifier.padding(24.dp).navigationBarsPadding()) {
            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF9C)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("ESTABLISH NEURAL LINK", color = Color.Black, fontWeight = FontWeight.Black)
            }
        }

        if (showTimePickerForDay != null) {
            val initialTime = tempProfile.scheduledDays.find { it.dayOfWeek == showTimePickerForDay }?.time ?: "09:00"
            val initialHour = initialTime.split(":")[0].toInt()
            val initialMinute = initialTime.split(":")[1].toInt()
            
            val timePickerState = rememberTimePickerState(
                initialHour = initialHour,
                initialMinute = initialMinute
            )

            AlertDialog(
                onDismissRequest = { showTimePickerForDay = null }, 
                confirmButton = { 
                    TextButton(onClick = { 
                        val newTime = "%02d:%02d".format(timePickerState.hour, timePickerState.minute)
                        if (applyTimeToAll) {
                            onUpdateSchedule(tempProfile.scheduledDays.map { it.copy(time = newTime) })
                        } else {
                            onUpdateSchedule(tempProfile.scheduledDays.map { if (it.dayOfWeek == showTimePickerForDay) it.copy(time = newTime) else it })
                        }
                        showTimePickerForDay = null 
                    }) { 
                        Text("CONFIRM", color = Color(0xFF00FF9C)) 
                    } 
                }, 
                title = { Text("SELECT TIME", color = Color.White) }, 
                text = { TimePicker(state = timePickerState) }, 
                containerColor = Color(0xFF1C1C1E)
            )
        }
    }
}

@Composable
fun ProtocolDetailScreen(
    protocol: WorkoutProtocol,
    uiState: WorkoutUiState,
    onBack: () -> Unit,
    onStartProtocol: (WorkoutProtocol) -> Unit,
    onAddProtocol: (WorkoutProtocol) -> Unit,
    onAddRoutine: (WorkoutRoutine) -> Unit,
    onRoutineClick: (WorkoutRoutine) -> Unit
) {
    val protocolRoutines = uiState.exploreRoutines.filter { it.protocol == protocol }
    val allRoutinesAdded = protocolRoutines.isNotEmpty() && protocolRoutines.all { it.isAddedToLibrary }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
            .statusBarsPadding()
    ) {
        // Header
        Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text(
                "PROTOCOL DETAIL",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f).padding(horizontal = 24.dp)
        ) {
            item {
                Text(
                    protocol.displayName,
                    color = Color.White,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Black
                )
                
                Text(
                    protocol.description,
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 16.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))
                
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF1C1C1E),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFF00FF9C).copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("METHODOLOGY", color = Color(0xFF00FF9C), fontSize = 10.sp, fontWeight = FontWeight.Black)
                        Text(
                            protocol.methodology,
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
                
                Text(
                    "PROTOCOL TENANTS",
                    color = Color(0xFF00FF9C),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                protocol.tenants.forEach { tenant ->
                    Row(modifier = Modifier.padding(vertical = 8.dp)) {
                        Text("•", color = Color(0xFF00FF9C), modifier = Modifier.padding(end = 12.dp))
                        Text(
                            tenant,
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    "INTEGRATED ROUTINES",
                    color = Color(0xFF00FF9C),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                
                Spacer(modifier = Modifier.height(16.dp))
            }

            items(protocolRoutines) { routine ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .clickable { onRoutineClick(routine) },
                    color = Color(0xFF1C1C1E),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(routine.name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    routine.exercises.joinToString(", ") { it.exercise.name },
                                    color = Color.Gray,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                            IconButton(
                                onClick = { onAddRoutine(routine) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    if (routine.isAddedToLibrary) Icons.Default.CheckCircle else Icons.Default.Add,
                                    contentDescription = "Add Routine", 
                                    tint = if (routine.isAddedToLibrary) Color(0xFF00FF9C) else Color(0xFF00CCFF)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Bottom Actions
        Column(modifier = Modifier.padding(24.dp).navigationBarsPadding()) {
            Button(
                onClick = { onAddProtocol(protocol) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (allRoutinesAdded) Color.DarkGray else Color(0xFF1C1C1E)),
                shape = RoundedCornerShape(12.dp),
                border = if (!allRoutinesAdded) BorderStroke(1.dp, Color(0xFF00FF9C)) else null
            ) {
                Icon(
                    if (allRoutinesAdded) Icons.Default.CheckCircle else Icons.Default.Download,
                    contentDescription = null,
                    tint = if (allRoutinesAdded) Color(0xFF00FF9C) else Color.White
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    if (allRoutinesAdded) "PROTOCOL ARCHIVED" else "ADD PROTOCOL TO UPLINK",
                    color = if (allRoutinesAdded) Color(0xFF00FF9C) else Color.White,
                    fontWeight = FontWeight.Black
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { onStartProtocol(protocol) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF9C)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("INITIALIZE PROTOCOL SESSION", color = Color.Black, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
fun RoutinePreviewScreen(
    routine: WorkoutRoutine,
    onBack: () -> Unit,
    onAdd: (WorkoutRoutine) -> Unit,
    onStart: (WorkoutRoutine) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
            .statusBarsPadding()
    ) {
        // Header
        Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text(
                "ROUTINE PREVIEW",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f).padding(horizontal = 24.dp)
        ) {
            item {
                Text(
                    routine.name,
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black
                )
                
                Text(
                    routine.protocol.displayName,
                    color = Color(0xFF00FF9C),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )

                routine.description?.let {
                    Text(
                        it,
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
                
                Text(
                    "PRESCRIBED EXERCISES",
                    color = Color(0xFF00FF9C),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                
                Spacer(modifier = Modifier.height(16.dp))
            }

            items(routine.exercises) { routineExercise ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    color = Color.Transparent
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                routineExercise.exercise.name.uppercase(),
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black
                            )
                            Spacer(Modifier.weight(1f))
                            Text(
                                "${routineExercise.sets.size} SETS",
                                color = Color(0xFF00FF9C),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (routineExercise.exercise.muscleGroups.isNotEmpty()) {
                            Text(
                                routineExercise.exercise.muscleGroups.joinToString(" • ").uppercase(),
                                color = Color.Gray,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Sets Preview
                        routineExercise.sets.forEachIndexed { index, set ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val typeLabel = when (set.type) {
                                    SetType.WARMUP -> "WARMUP"
                                    SetType.REST_PAUSE -> "REST-PAUSE"
                                    SetType.POWER -> "POWER"
                                    SetType.WIDOWMAKER -> "WIDOWMAKER"
                                    SetType.GS -> "GIANT SET"
                                    else -> "SET ${index + 1}"
                                }
                                val typeColor = when (set.type) {
                                    SetType.WARMUP -> Color(0xFFFFA500)
                                    SetType.REST_PAUSE -> Color(0xFF00FFAA)
                                    SetType.POWER -> Color(0xFFFFD700)
                                    SetType.WIDOWMAKER -> Color(0xFFFF00FF)
                                    SetType.GS -> Color(0xFF00CCFF)
                                    else -> Color.White.copy(alpha = 0.7f)
                                }
                                
                                Text(
                                    typeLabel,
                                    color = typeColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.width(100.dp)
                                )
                                
                                if (set.goalReps != null) {
                                    Text(
                                        "GOAL: ${set.goalReps}",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        if (routineExercise.exercise.cues.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = Color(0xFF1C1C1E).copy(alpha = 0.5f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    "NEURAL CUE: ${routineExercise.exercise.cues.first()}",
                                    color = Color.Gray,
                                    fontSize = 10.sp,
                                    lineHeight = 14.sp,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Bottom Actions
        Column(modifier = Modifier.padding(24.dp).navigationBarsPadding()) {
            Button(
                onClick = { onAdd(routine) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (routine.isAddedToLibrary) Color.DarkGray else Color(0xFF1C1C1E)),
                shape = RoundedCornerShape(12.dp),
                border = if (!routine.isAddedToLibrary) BorderStroke(1.dp, Color(0xFF00FF9C)) else null
            ) {
                Icon(
                    if (routine.isAddedToLibrary) Icons.Default.CheckCircle else Icons.Default.Download,
                    contentDescription = null,
                    tint = if (routine.isAddedToLibrary) Color(0xFF00FF9C) else Color.White
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    if (routine.isAddedToLibrary) "ROUTINE ARCHIVED" else "ADD TO MY ROUTINES",
                    color = if (routine.isAddedToLibrary) Color(0xFF00FF9C) else Color.White,
                    fontWeight = FontWeight.Black
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { onStart(routine) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF9C)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("INITIALIZE SESSION", color = Color.Black, fontWeight = FontWeight.Black)
            }
        }
    }
}
