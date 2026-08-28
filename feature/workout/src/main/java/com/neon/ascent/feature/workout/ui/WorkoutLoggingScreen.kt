package com.neon.ascent.feature.workout.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTransformGestures
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
    onViewInCodex: (String) -> Unit = {},
    viewModel: WorkoutViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showRoutineActionMenuFor by remember { mutableStateOf<WorkoutRoutine?>(null) }
    var showAugmentActionMenuFor by remember { mutableStateOf<WorkoutAugment?>(null) }
    var showAugmentScheduleDialogFor by remember { mutableStateOf<WorkoutAugment?>(null) }
    var showRestTimerAdjustment by remember { mutableStateOf(false) }

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
                if (uiState.showProtocolChangeReminderDialog != null) {
                    val newProtocol = uiState.showProtocolChangeReminderDialog!!
                    AlertDialog(
                        onDismissRequest = { viewModel.cancelProtocolChange() },
                        title = { Text("RESET REMINDERS?", color = Color.White, fontWeight = FontWeight.Black) },
                        text = { Text("Replace training reminders with the ${newProtocol.displayName} recommended pattern?", color = Color.Gray) },
                        confirmButton = {
                            TextButton(onClick = { viewModel.confirmProtocolChange(true) }) {
                                Text("YES, RESET", color = Color(0xFF00FF9C), fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { viewModel.confirmProtocolChange(false) }) {
                                Text("NO, KEEP CUSTOM", color = Color.White)
                            }
                        },
                        containerColor = Color(0xFF1C1C1E)
                    )
                }

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
                        val dossier = uiState.configuringProtocol!!.dossier
                        ProtocolConfigScreen(
                            protocol = uiState.configuringProtocol!!,
                            recommendedDays = dossier.daysPerWeek,
                            frequencyCopy = dossier.frequencyCopy,
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
                    } else if (uiState.isShowingSettings) {
                        WorkoutSettingsScreen(
                            uiState = uiState,
                            onBack = { viewModel.hideSettings() },
                            onSave = { viewModel.saveWorkoutSettings() },
                            onUpdateProfile = { viewModel.updateTempSettingsProfile(it) }
                        )
                    } else if (uiState.selectedProtocolForDetail != null) {
                        ProtocolDetailScreen(
                            protocol = uiState.selectedProtocolForDetail!!,
                            uiState = uiState,
                            viewModel = viewModel,
                            onBack = { viewModel.hideProtocolDetail() },
                            onStartProtocol = { 
                                viewModel.startSession(it)
                                viewModel.hideExploreProtocols()
                            },
                            onAddProtocol = { viewModel.addProtocolToLibrary(it) },
                            onAddRoutine = { viewModel.toggleRoutineLibrary(it) },
                            onRoutineClick = { viewModel.showRoutinePreview(it) }
                        )
                    } else if (uiState.protocolIntakeNeeded != null) {
                        if (uiState.protocolIntakeNeeded == WorkoutProtocol.HST) {
                            HstIntakeScreen(
                                onDismiss = { viewModel.dismissProtocolIntake() },
                                onSubmit = { 
                                    viewModel.startHstCycle(it)
                                    viewModel.dismissProtocolIntake()
                                },
                                uiState = uiState
                            )
                        } else {
                            ProtocolIntakeScreen(
                                protocol = uiState.protocolIntakeNeeded!!,
                                onDismiss = { viewModel.dismissProtocolIntake() },
                                onSubmit = { weights -> 
                                    when (uiState.protocolIntakeNeeded) {
                                        WorkoutProtocol.STARTING_STRENGTH -> viewModel.startStartingStrengthCycle(weights)
                                        WorkoutProtocol.FIVE_THREE_ONE -> viewModel.startFiveThreeOneCycle(weights)
                                        WorkoutProtocol.DUP -> viewModel.startDupCycle(weights)
                                        WorkoutProtocol.WESTSIDE -> viewModel.startWestsideCycle(weights)
                                        else -> {}
                                    }
                                    viewModel.dismissProtocolIntake()
                                }
                            )
                        }
                    } else if (uiState.isStrategicDeconditioningActive) {
                        StrategicDeconditioningWindow(
                            onDismiss = { viewModel.dismissSdWindow() }
                        )
                    } else {
                        WorkoutIntakeScreen(
                            uiState = uiState,
                            onBack = onBack,
                            onShowArchive = { onViewInCodex("") },
                            onStartProtocol = { protocol, deload -> viewModel.startSession(protocol, deload) },
                            onStartRoutine = { routine, deload -> viewModel.handleRoutineSelection(routine, deload) },
                            onStartAugment = { viewModel.startAugment(it) },
                            onCreateRoutine = { viewModel.startCreateRoutine() },
                            onCreateAugment = { viewModel.startCreateAugment() },
                            onRoutineActionClick = { showRoutineActionMenuFor = it },
                            onAugmentActionClick = { showAugmentActionMenuFor = it },
                            onExplore = { viewModel.startExploreProtocols() },
                            onAddAugment = { viewModel.toggleAugmentLibrary(it) },
                            onAddRoutine = { viewModel.toggleRoutineLibrary(it) },
                            onDeactivateProtocol = { viewModel.initiateDeactivateProtocol() },
                            onShowSettings = { viewModel.showSettings() }
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
                    val haptic = LocalHapticFeedback.current
                    
                    // Pro-Calibrated Zoom Levels for HUD Stability
                    val zoomLevels = listOf(0.8f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)
                    
                    var currentZoom by remember { mutableFloatStateOf(uiState.zoomLevel) }
                    var accumulatedZoomChange by remember { mutableFloatStateOf(1f) }

                    LaunchedEffect(uiState.zoomLevel) {
                        currentZoom = uiState.zoomLevel
                    }

                    val scaledDensity = remember(originalDensity, currentZoom) {
                        Density(
                            density = originalDensity.density * currentZoom,
                            fontScale = originalDensity.fontScale
                        )
                    }
                    
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTransformGestures { _, _, zoom, _ ->
                                    if (zoom != 1f) {
                                        accumulatedZoomChange *= zoom
                                        
                                        // Sensitivity threshold: 15% change to trigger a snap
                                        if (kotlin.math.abs(accumulatedZoomChange - 1f) > 0.15f) {
                                            val direction = if (accumulatedZoomChange > 1f) 1 else -1
                                            val currentIndex = zoomLevels.indexOf(currentZoom).coerceAtLeast(0)
                                            val nextIndex = (currentIndex + direction).coerceIn(zoomLevels.indices)
                                            
                                            val newZoom = zoomLevels[nextIndex]
                                            if (newZoom != currentZoom) {
                                                currentZoom = newZoom
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                viewModel.updateZoomLevel(newZoom)
                                            }
                                            accumulatedZoomChange = 1f // Reset after snap
                                        }
                                    }
                                }
                            }
                    ) {
                        CompositionLocalProvider(LocalDensity provides scaledDensity) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                ActiveWorkoutHeader(
                                    duration = durationFormatted,
                                    zoomLevel = uiState.zoomLevel,
                                    onBack = onBack,
                                    onFinish = { viewModel.finishWorkout() },
                                    isPaused = uiState.isPaused,
                                    onPauseToggle = {
                                        if (uiState.isPaused) viewModel.resumeWorkout() else viewModel.pauseWorkout()
                                    },
                                    onDiscard = { viewModel.discardWorkout() },
                                    onViewInCodex = { onViewInCodex("") },
                                    protocol = uiState.session?.protocol,
                                    dayType = uiState.session?.protocolDayType
                                )
                            WorkoutSummaryBar(uiState, viewModel, onToggleSomatotype = { viewModel.toggleSomatotypeInfluence() })
                            
                            if (uiState.somatotypeNudgeText != null) {
                                SomatotypeBadge(uiState.somatotypeNudgeText!!, uiState.userProfile?.somatotype)
                            }

                            if (uiState.stagnantExerciseId != null) {
                                StagnationBadge(
                                    exerciseName = uiState.logs.find { it.first.exerciseId == uiState.stagnantExerciseId }?.first?.exerciseName ?: "Exercise",
                                    onRotate = { viewModel.forceRotateStagnant(uiState.stagnantExerciseId!!) }
                                )
                            }

                            Box(modifier = Modifier.weight(1f)) {
                                ActiveWorkoutContent(
                                    uiState = uiState, 
                                    viewModel = viewModel, 
                                    onRestTimerClick = { showRestTimerAdjustment = true },
                                    onViewInCodex = onViewInCodex
                                )
                                
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
                    Column(modifier = Modifier.fillMaxWidth()) {
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

                        Spacer(Modifier.height(8.dp))

                        TextButton(
                            onClick = { viewModel.cancelDeactivateProtocol() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("CANCEL", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                dismissButton = null,
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
                onSchedule = {
                    showAugmentScheduleDialogFor = showAugmentActionMenuFor
                    showAugmentActionMenuFor = null
                },
                onDelete = { viewModel.deleteAugment(showAugmentActionMenuFor!!) },
                onDismiss = { showAugmentActionMenuFor = null }
            )
        }

        if (showAugmentScheduleDialogFor != null) {
            AugmentScheduleDialog(
                augment = showAugmentScheduleDialogFor!!,
                onSave = { updatedDays ->
                    viewModel.updateAugmentSchedule(showAugmentScheduleDialogFor!!, updatedDays)
                    showAugmentScheduleDialogFor = null
                },
                onDismiss = { showAugmentScheduleDialogFor = null }
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
            CyberFinisherDialog(onDone = { reps -> viewModel.startStretch(reps) })
        }
        if (uiState.showLoadedStretch && uiState.workoutPhase == RestPausePhase.LOADED_STRETCH) {
            LoadedStretchDialog(remaining = uiState.stretchTimeRemaining)
        }

        if (showRestTimerAdjustment) {
            RestTimerAdjustmentDialog(
                uiState = uiState,
                viewModel = viewModel,
                onDismiss = { showRestTimerAdjustment = false }
            )
        }

        if (uiState.isResting && (uiState.restTimerMode == RestTimerMode.POPUP || uiState.restTimerMode == RestTimerMode.BOTH)) {
            RestTimerPopup(
                remaining = uiState.restTimeRemaining,
                total = uiState.restTimerTotalSeconds,
                onAdjust = { viewModel.adjustRestTimer(it) },
                onSkip = { viewModel.skipRestTimer() }
            )
        }

        if (uiState.isResting && uiState.session != null) {
            StickyBottomTimer(
                remaining = uiState.restTimeRemaining,
                total = uiState.restTimerTotalSeconds,
                onAdjust = { viewModel.adjustRestTimer(it) },
                onSkip = { viewModel.skipRestTimer() }
            )
        }
        if (uiState.showPostWorkoutCheckIn) {
            PostWorkoutCheckInDialog(
                onFinish = { rpe: Int, joint: Int -> viewModel.submitPostWorkoutCheckIn(rpe, joint) },
                onCancel = { viewModel.cancelPostWorkoutCheckIn() }
            )
        }

        if (uiState.showSequenceOverrideDialog && uiState.pendingSequenceRoutine != null) {
            SequenceOverrideDialog(
                routine = uiState.pendingSequenceRoutine!!,
                onConfirm = { viewModel.confirmSequenceOverride(it) },
                onDismiss = { viewModel.dismissSequenceOverride() }
            )
        }

        if (uiState.showInjuryWarningDialog && uiState.pendingInjuryRoutine != null) {
            InjuryWarningDialog(
                injuredExercises = uiState.injuredExercises,
                onAutoSwap = { viewModel.confirmInjuryAutoSwap() },
                onIgnore = { viewModel.ignoreInjuryWarning() }
            )
        }
    }
}

@Composable
fun PostWorkoutCheckInDialog(
    onFinish: (Int, Int) -> Unit,
    onCancel: () -> Unit
) {
    var rpe by remember { mutableFloatStateOf(7f) }
    var jointHealth by remember { mutableFloatStateOf(1f) }

    Dialog(onDismissRequest = onCancel) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            color = Color(0xFF050505),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFF00FF9C).copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    "INTENSITY & WELLNESS",
                    color = Color(0xFF00FF9C),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
                
                Spacer(Modifier.height(24.dp))

                Text("SESSION RPE: ${rpe.toInt()}", color = Color.White, fontWeight = FontWeight.Bold)
                Text(
                    when {
                        rpe < 4 -> "Light recovery work."
                        rpe < 7 -> "Moderate training effort."
                        rpe < 9 -> "Hard session. Near limit."
                        else -> "Max effort. All-out intensity."
                    },
                    color = Color.Gray,
                    fontSize = 12.sp
                )
                Slider(
                    value = rpe,
                    onValueChange = { rpe = it },
                    valueRange = 1f..10f,
                    steps = 8,
                    colors = SliderDefaults.colors(thumbColor = Color(0xFF00FF9C), activeTrackColor = Color(0xFF00FF9C))
                )

                Spacer(Modifier.height(24.dp))

                Text("JOINT PAIN: ${jointHealth.toInt()}", color = Color.White, fontWeight = FontWeight.Bold)
                Text(
                    when {
                        jointHealth < 2 -> "Pain-free / Feeling strong."
                        jointHealth < 3 -> "Mild stiffness or fatigue."
                        jointHealth < 4 -> "Significant aches. Use caution."
                        else -> "Sharp pain. Immediate recovery needed."
                    },
                    color = Color.Gray,
                    fontSize = 12.sp
                )
                Slider(
                    value = jointHealth,
                    onValueChange = { jointHealth = it },
                    valueRange = 1f..5f,
                    steps = 3,
                    colors = SliderDefaults.colors(thumbColor = Color(0xFFFF006E), activeTrackColor = Color(0xFFFF006E))
                )

                Spacer(Modifier.height(32.dp))

                Button(
                    onClick = { onFinish(rpe.toInt(), jointHealth.toInt()) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF9C)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("FINISH & LOG BIOMETRICS", color = Color.Black, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
fun ActiveWorkoutHeader(
    duration: String,
    zoomLevel: Float,
    onBack: () -> Unit,
    onFinish: () -> Unit,
    isPaused: Boolean,
    onPauseToggle: () -> Unit,
    onDiscard: () -> Unit,
    onViewInCodex: () -> Unit = {},
    protocol: WorkoutProtocol? = null,
    dayType: ProtocolDayType? = null
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

    val zoom = zoomLevel
    val hPadding = if (zoom >= 1.75f) 8.dp else 16.dp
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = hPadding, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onBack() }) {
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Color.White)
            if (zoom < 1.25f) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (zoom <= 0.8f) "ASCENT NEURAL LINK" else if (protocol != null) {
                        if (protocol == WorkoutProtocol.DUP) "UNDULATION" else protocol.displayName
                    } else "Log Workout", 
                    color = Color.White, 
                    fontSize = 18.sp, 
                    fontWeight = FontWeight.Black
                )
                if (dayType != null && zoom < 1.0f) {
                    Text(
                        dayType.name,
                        color = Color(0xFF00FF9C),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            val iconSize = if (zoom >= 1.5f) 18.dp else 20.dp
            
            if (zoom < 1.25f) {
                IconButton(onClick = onViewInCodex, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.History, contentDescription = "View In Archive", tint = Color(0xFF00FF9C), modifier = Modifier.size(iconSize))
                }
            }

            if (zoom < 1.75f) {
                IconButton(onClick = { showDiscardDialog = true }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Discard", tint = Color.Red.copy(alpha = 0.7f), modifier = Modifier.size(iconSize))
                }
            }
            
            IconButton(onClick = onPauseToggle, modifier = Modifier.size(32.dp)) {
                Icon(
                    if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = if (isPaused) "Resume" else "Pause",
                    tint = Color.White,
                    modifier = Modifier.size(iconSize)
                )
            }

            Spacer(modifier = Modifier.width(if (zoom >= 1.5f) 2.dp else 4.dp))
            Text(
                duration, 
                color = Color.White, 
                fontSize = if (zoom >= 1.5f) 14.sp else 16.sp, 
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                softWrap = false
            )
            
            val buttonWidth = if (zoom >= 1.75f) 56.dp else 80.dp
            val buttonText = if (zoom >= 1.75f) "OK" else "Finish"
            
            Spacer(modifier = Modifier.width(if (zoom >= 1.5f) 6.dp else 12.dp))
            Button(
                onClick = onFinish,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF)),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = if (zoom >= 1.5f) 4.dp else 8.dp, vertical = 0.dp),
                modifier = Modifier.height(30.dp).widthIn(min = buttonWidth)
            ) {
                Text(buttonText, color = Color.White, fontWeight = FontWeight.Bold, fontSize = if (zoom >= 1.5f) 12.sp else 14.sp)
            }
        }
    }
}

@Composable
fun WorkoutSummaryBar(uiState: WorkoutUiState, viewModel: WorkoutViewModel, onToggleSomatotype: () -> Unit = {}) {
    val totalVolume = remember(uiState.logs) {
        uiState.logs.sumOf { (_, sets) -> 
            sets.sumOf { (it.weight * it.reps).toDouble() }
        }.toInt()
    }
    val totalSets = remember(uiState.logs) {
        uiState.logs.sumOf { it.second.size }
    }
    val durationMinutes = uiState.workoutDurationSeconds / 60

    val zoom = uiState.zoomLevel
    val hPadding = if (zoom >= 1.75f) 8.dp else 16.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = hPadding, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val blastWeek = viewModel.getBlastWeek()
        if (blastWeek != null && zoom < 1.75f) {
            SummaryStat("Blast", "Wk $blastWeek", Color(0xFF00FF9C))
        }
        SummaryStat("Duration", "${durationMinutes}m", Color(0xFF007AFF))
        SummaryStat("Volume", "%,d".format(totalVolume), Color.White)
        
        if (zoom < 1.5f) {
            SummaryStat("Sets", "$totalSets", Color.White)
        }
        
        if (zoom <= 0.8f) {
            val status = uiState.recoveryScore?.status?.name ?: "READY"
            SummaryStat("Status", status, Color(0xFF00FF9C))
        }

        IconButton(onClick = onToggleSomatotype) {
            Icon(
                Icons.Default.SettingsInputComponent, 
                contentDescription = "Toggle Somatotype Influence", 
                tint = if (uiState.useSomatotypeInfluence) Color(0xFF00FF9C) else Color.Gray,
                modifier = Modifier.size(if (zoom >= 1.75f) 16.dp else 20.dp)
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
        Text(label, color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Text(value, color = valueColor, fontSize = 14.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
fun WorkoutIntakeScreen(
    uiState: WorkoutUiState,
    onBack: () -> Unit,
    onShowArchive: () -> Unit,
    onStartProtocol: (WorkoutProtocol, Boolean) -> Unit,
    onStartRoutine: (WorkoutRoutine, Boolean) -> Unit,
    onStartAugment: (WorkoutAugment) -> Unit,
    onCreateRoutine: () -> Unit,
    onCreateAugment: () -> Unit,
    onRoutineActionClick: (WorkoutRoutine) -> Unit,
    onAugmentActionClick: (WorkoutAugment) -> Unit,
    onExplore: () -> Unit,
    onAddAugment: (WorkoutAugment) -> Unit,
    onAddRoutine: (WorkoutRoutine) -> Unit,
    onDeactivateProtocol: () -> Unit,
    onShowSettings: () -> Unit
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
                IconButton(onClick = onShowSettings) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                IconButton(onClick = onShowArchive) {
                    Icon(
                        Icons.AutoMirrored.Filled.TrendingUp,
                        contentDescription = "Archive",
                        tint = Color(0xFF00FF9C),
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

        // NEXT MISSION SEQUENCER
        uiState.nextSequencedRoutine?.let { routine ->
            NextMissionCard(
                routine = routine,
                onStart = { onStartRoutine(routine, false) }
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        // RECOVERY BRAIN DASHBOARD
        uiState.recoveryScore?.let { score ->
            RecoveryScoreCard(
                score = score, 
                onStartDeload = {
                    val activeRoutine = uiState.routines.find { it.protocol == uiState.userProfile?.activeProtocol }
                    if (activeRoutine != null) {
                        onStartRoutine(activeRoutine, true)
                    } else {
                        onStartProtocol(uiState.userProfile?.activeProtocol ?: WorkoutProtocol.GENERAL, true)
                    }
                }
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Start Empty Workout Button
        Button(
            onClick = { onStartProtocol(WorkoutProtocol.GENERAL, false) },
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
                    onStart = { onStartRoutine(routine, false) },
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
fun NextMissionCard(routine: WorkoutRoutine, onStart: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onStart() },
        color = Color(0xFF00FF9C).copy(alpha = 0.05f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF00FF9C).copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(48.dp).background(Color(0xFF00FF9C).copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color(0xFF00FF9C))
            }
            
            Spacer(Modifier.width(20.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "NEXT MISSION",
                    color = Color(0xFF00FF9C),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
                Text(
                    routine.name,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    routine.exercises.take(3).joinToString(", ") { it.exercise.name } + if (routine.exercises.size > 3) "..." else "",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color(0xFF00FF9C))
        }
    }
}

@Composable
fun SequenceOverrideDialog(
    routine: WorkoutRoutine,
    onConfirm: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1C1C1E),
        title = { 
            Text(
                "OFF-MISSION DETECTED", 
                color = Color(0xFFFF006E), 
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                fontSize = 18.sp
            ) 
        },
        text = {
            Column {
                Text(
                    "You've selected ${routine.name}, but a different routine was due in your rotation.",
                    color = Color.White
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "Would you like to update your sequence baseline to this routine, or treat this as a one-off session?",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
        },
        confirmButton = {
            Column(Modifier.fillMaxWidth()) {
                Button(
                    onClick = { onConfirm(true) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF9C)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("UPDATE SEQUENCE BASELINE", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = { onConfirm(false) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("ONE-OFF SESSION (KEEP SEQUENCE)", color = Color.White)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = Color.Gray)
            }
        }
    )
}

@Composable
fun InjuryWarningDialog(
    injuredExercises: List<Pair<Exercise, List<Exercise>>>,
    onAutoSwap: () -> Unit,
    onIgnore: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onIgnore,
        containerColor = Color(0xFF1C1C1E),
        title = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFF006E))
                Spacer(Modifier.width(12.dp))
                Text(
                    "INJURY GUARD DETECTED", 
                    color = Color(0xFFFF006E), 
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    fontSize = 16.sp
                )
            }
        },
        text = {
            Column {
                Text(
                    "The following exercises in this routine are contraindicated for your current injuries:",
                    color = Color.White,
                    fontSize = 14.sp
                )
                Spacer(Modifier.height(16.dp))
                injuredExercises.forEach { (highRisk, alternatives) ->
                    Column(modifier = Modifier.padding(bottom = 12.dp)) {
                        Text(highRisk.name, color = Color(0xFFFF006E), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        if (alternatives.isNotEmpty()) {
                            Text("Safe swap: ${alternatives.first().name}", color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onAutoSwap,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF9C)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("NEURAL SWAP (STABILITY-FIRST)", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onIgnore,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("IGNORE & PROCEED AT RISK", color = Color.White.copy(alpha = 0.6f))
            }
        }
    )
}

@Composable
fun RecoveryScoreCard(score: RecoveryScore, onStartDeload: () -> Unit) {
    val color = when (score.status) {
        RecoveryStatus.OPTIMAL -> Color(0xFF00FF9C)
        RecoveryStatus.CAUTION -> Color(0xFFFFD700)
        RecoveryStatus.DELOAD -> Color(0xFFFF006E)
        RecoveryStatus.CRITICAL -> Color(0xFFFF0000)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF1C1C1E),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "RECOVERY BRAIN",
                        color = color,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                    Text(
                        score.status.name,
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black
                    )
                }
                
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(60.dp)) {
                    CircularProgressIndicator(
                        progress = { score.totalScore / 100f },
                        modifier = Modifier.fillMaxSize(),
                        color = color,
                        trackColor = Color.White.copy(alpha = 0.1f),
                        strokeWidth = 6.dp,
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                    Text(
                        "${score.totalScore}",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                score.plainLanguageSummary,
                color = Color.Gray,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )

            if (score.status == RecoveryStatus.DELOAD || score.status == RecoveryStatus.CRITICAL) {
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onStartDeload,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = color),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("START SOFT DELOAD SESSION", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 12.sp)
                }
            }
        }
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
    val exerciseFamilies by viewModel.exerciseFamilies.collectAsState()

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
                    families = exerciseFamilies,
                    onSearchChange = { viewModel.updateExerciseSearch(it) },
                    onDetailClick = { viewModel.showExerciseDetail(it) },
                    onSelect = {
                        viewModel.addExerciseToNewRoutine(it)
                        showExercisePicker = false
                    },
                    onSelectFamily = { viewModel.selectFamilyInPicker(it) },
                    onToggleShowAll = { viewModel.toggleShowAllVariants(it) },
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
    val exerciseFamilies by viewModel.exerciseFamilies.collectAsState()

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
                    families = exerciseFamilies,
                    onSearchChange = { viewModel.updateExerciseSearch(it) },
                    onDetailClick = { viewModel.showExerciseDetail(it) },
                    onSelect = {
                        viewModel.addExerciseToNewRoutine(it)
                        showExercisePicker = false
                    },
                    onSelectFamily = { viewModel.selectFamilyInPicker(it) },
                    onToggleShowAll = { viewModel.toggleShowAllVariants(it) },
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
                        SetType.PARTIAL -> "PAR"
                        SetType.STRETCH -> "STR"
                        SetType.MAX_EFFORT -> "ME"
                    }
                    val color = when (set.type) {
                        SetType.WARMUP -> Color(0xFFFFA500)
                        SetType.DROP -> Color(0xFF00CCFF)
                        SetType.FAILURE -> Color(0xFFFF4444)
                        SetType.REST_PAUSE -> Color(0xFF00FFAA)
                        SetType.WIDOWMAKER -> Color(0xFFFF00FF)
                        SetType.POWER -> Color(0xFFFFD700)
                        SetType.GS -> Color(0xFF00CCFF)
                        SetType.PARTIAL -> Color(0xFF00FF9C)
                        SetType.STRETCH -> Color(0xFFFF006E)
                        SetType.MAX_EFFORT -> Color(0xFFFF0000)
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
fun ActiveWorkoutContent(
    uiState: WorkoutUiState,
    viewModel: WorkoutViewModel,
    onRestTimerClick: () -> Unit,
    onViewInCodex: (String) -> Unit
) {
    var showExercisePicker by remember { mutableStateOf(false) }
    var showAugmentPicker by remember { mutableStateOf(false) }
    var exerciseToReplace by remember { mutableStateOf<WorkoutLog?>(null) }
    var showActionMenuFor by remember { mutableStateOf<WorkoutLog?>(null) }
    var showSupersetMenuFor by remember { mutableStateOf<WorkoutLog?>(null) }
    val exerciseFamilies by viewModel.exerciseFamilies.collectAsState()
    val codexNavigation = onViewInCodex

    LazyColumn(
        modifier = Modifier.fillMaxSize().imePadding(),
        contentPadding = PaddingValues(bottom = 200.dp)
    ) {
        items(uiState.logs, key = { it.first.id }) { (log, sets) ->
            WorkoutLogCard(
                log = log,
                sets = sets,
                viewModel = viewModel,
                onActionMenuClick = { showActionMenuFor = log },
                onRestTimerClick = onRestTimerClick,
                onViewInCodex = codexNavigation
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
                    families = exerciseFamilies,
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
                    onSelectFamily = { viewModel.selectFamilyInPicker(it) },
                    onToggleShowAll = { viewModel.toggleShowAllVariants(it) },
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
    onSchedule: () -> Unit,
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
            ActionMenuItem(
                icon = Icons.Default.CalendarMonth,
                label = "Schedule & Reminders",
                onClick = {
                    onSchedule()
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
fun AugmentScheduleDialog(
    augment: WorkoutAugment,
    onSave: (List<ScheduledDay>) -> Unit,
    onDismiss: () -> Unit
) {
    val days = listOf("M", "T", "W", "T", "F", "S", "S")
    var scheduledDays by remember(augment.scheduledDays) { mutableStateOf(augment.scheduledDays) }
    var applyTimeToAll by remember { mutableStateOf(true) }
    var showTimePickerForDay by remember { mutableStateOf<Int?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            color = Color(0xFF0D0D0D),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFF00FF9C).copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    "SUB-PROTOCOL SCHEDULE",
                    color = Color(0xFF00FF9C),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Text(
                    "Schedule independent training & reminders for ${augment.name}.",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("APPLY SAME TIME TO ALL", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Switch(
                        checked = applyTimeToAll,
                        onCheckedChange = { applyTimeToAll = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00FF9C))
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    days.forEachIndexed { index, label ->
                        val dayId = index + 1
                        val scheduled = scheduledDays.find { it.dayOfWeek == dayId }
                        val isSelected = scheduled != null
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 2.dp)
                        ) {
                            Surface(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clickable {
                                        scheduledDays = if (isSelected) {
                                            scheduledDays.filter { it.dayOfWeek != dayId }
                                        } else {
                                            val baseTime = scheduledDays.firstOrNull()?.time ?: "18:00"
                                            scheduledDays + ScheduledDay(dayId, baseTime)
                                        }
                                    },
                                color = if (isSelected) Color(0xFF00FF9C).copy(alpha = 0.15f) else Color(0xFF1C1C1E),
                                shape = CircleShape,
                                border = BorderStroke(1.5.dp, if (isSelected) Color(0xFF00FF9C) else Color.DarkGray)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        label,
                                        color = if (isSelected) Color(0xFF00FF9C) else Color.Gray,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 15.sp
                                    )
                                }
                            }
                            if (scheduled != null) {
                                Surface(
                                    modifier = Modifier
                                        .padding(top = 6.dp)
                                        .clickable { showTimePickerForDay = dayId },
                                    color = Color(0xFF2C2C2E),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        scheduled.time,
                                        color = Color(0xFF00FF9C),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("CANCEL", color = Color.Gray)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { onSave(scheduledDays) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF9C)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("SAVE SCHEDULE", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showTimePickerForDay != null) {
        val initialTime = scheduledDays.find { it.dayOfWeek == showTimePickerForDay }?.time ?: "18:00"
        val initialHour = initialTime.split(":")[0].toIntOrNull() ?: 18
        val initialMinute = initialTime.split(":")[1].toIntOrNull() ?: 0

        val timePickerState = rememberTimePickerState(
            initialHour = initialHour,
            initialMinute = initialMinute
        )

        AlertDialog(
            onDismissRequest = { showTimePickerForDay = null },
            confirmButton = {
                TextButton(onClick = {
                    val newTime = "%02d:%02d".format(timePickerState.hour, timePickerState.minute)
                    scheduledDays = if (applyTimeToAll) {
                        scheduledDays.map { it.copy(time = newTime) }
                    } else {
                        scheduledDays.map { if (it.dayOfWeek == showTimePickerForDay) it.copy(time = newTime) else it }
                    }
                    showTimePickerForDay = null
                }) {
                    Text("CONFIRM", color = Color(0xFF00FF9C), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePickerForDay = null }) {
                    Text("CANCEL", color = Color.Gray)
                }
            },
            title = {
                Text(
                    "TRAINING TIME WINDOW",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            },
            text = {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    TimePicker(state = timePickerState)
                }
            },
            containerColor = Color(0xFF1C1C1E),
            shape = RoundedCornerShape(16.dp)
        )
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
                icon = Icons.Default.Link,
                label = "PAIR LIFTS",
                onClick = {
                    onAddSuperset()
                    onDismiss()
                }
            )
            Text(
                "Pair two lifts. Not a program.",
                color = Color.Gray,
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 64.dp, vertical = 0.dp)
            )
            Spacer(Modifier.height(16.dp))

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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExercisePicker(
    uiState: WorkoutUiState,
    families: List<ExerciseFamily>,
    onSearchChange: (String) -> Unit,
    onDetailClick: (Exercise) -> Unit,
    onSelect: (Exercise) -> Unit,
    onSelectFamily: (String?) -> Unit,
    onToggleShowAll: (Boolean) -> Unit,
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

    Column(
        modifier = Modifier
            .fillMaxSize()
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
            Text(
                if (uiState.selectedFamilyIdInPicker != null) "Back" else "Cancel",
                color = Color(0xFF007AFF),
                modifier = Modifier.clickable { 
                    if (uiState.selectedFamilyIdInPicker != null) onSelectFamily(null) else onDismiss() 
                },
                fontSize = 16.sp
            )
            Text(
                if (uiState.selectedFamilyIdInPicker != null) "Select Variant" else "Add Exercise",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
            Text(
                "Create",
                color = Color(0xFF007AFF),
                modifier = Modifier.clickable { showCustomExerciseForm = true },
                fontSize = 16.sp
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
            placeholder = { Text("Search family or variant...", color = Color.Gray, fontSize = 14.sp) },
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

        Spacer(modifier = Modifier.height(10.dp))

        // Mode Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = !uiState.isShowingAllVariants,
                onClick = { onToggleShowAll(false) },
                label = { Text("FAMILIES") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF007AFF).copy(alpha = 0.2f),
                    selectedLabelColor = Color(0xFF007AFF)
                )
            )
            FilterChip(
                selected = uiState.isShowingAllVariants,
                onClick = { onToggleShowAll(true) },
                label = { Text("ALL VARIANTS") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF00FF9C).copy(alpha = 0.2f),
                    selectedLabelColor = Color(0xFF00FF9C)
                )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        val selectedFamily = remember(uiState.selectedFamilyIdInPicker, families) {
            families.find { it.id == uiState.selectedFamilyIdInPicker }
        }

        Box(modifier = Modifier.weight(1f)) {
            if (selectedFamily != null) {
                VariantSelector(
                    family = selectedFamily,
                    onSelect = onSelect,
                    onDetailClick = onDetailClick
                )
            } else if (uiState.isShowingAllVariants || uiState.exerciseSearchQuery.length > 1) {
                AllVariantsList(
                    families = families,
                    onSelect = onSelect,
                    onDetailClick = onDetailClick
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    items(families, key = { it.id }) { family ->
                        ExerciseFamilyItem(
                            family = family,
                            onClick = { onSelectFamily(family.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ExerciseFamilyItem(family: ExerciseFamily, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val primary = family.variants.find { it.isPrimaryVariant } ?: family.variants.firstOrNull()
            val exerciseIcon = when (primary?.equipment?.firstOrNull()) {
                "Cable", "Plate Loaded" -> Icons.Default.SettingsInputComponent
                "Bodyweight", "Weighted" -> Icons.AutoMirrored.Filled.DirectionsRun
                else -> Icons.Default.FitnessCenter
            }
            Box(
                modifier = Modifier.size(48.dp).background(Color.White.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(exerciseIcon, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(family.name.uppercase(), color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp, letterSpacing = 1.sp)
                Text("${family.variants.size} implements", color = Color.Gray, fontSize = 12.sp)
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.DarkGray)
        }
        HorizontalDivider(modifier = Modifier.padding(start = 80.dp), color = Color.DarkGray.copy(alpha = 0.5f), thickness = 0.5.dp)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VariantSelector(
    family: ExerciseFamily,
    onSelect: (Exercise) -> Unit,
    onDetailClick: (Exercise) -> Unit
) {
    var selectedImplement by remember { mutableStateOf<Implement?>(family.variants.firstOrNull()?.implement) }
    
    val implements = remember(family) { family.variants.map { it.implement }.distinct() }
    val variantsForImplement = remember(family, selectedImplement) {
        family.variants.filter { it.implement == selectedImplement }
    }
    val stances = remember(variantsForImplement) { variantsForImplement.map { it.stance }.distinct() }
    var selectedStance by remember(selectedImplement) { 
        mutableStateOf<Stance?>(variantsForImplement.find { it.isPrimaryVariant }?.stance ?: stances.firstOrNull()) 
    }
    
    val finalVariant = remember(variantsForImplement, selectedStance) {
        variantsForImplement.find { it.stance == selectedStance } ?: variantsForImplement.firstOrNull()
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("1. CHOOSE IMPLEMENT", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            implements.forEach { imp ->
                val isSelected = selectedImplement == imp
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedImplement = imp },
                    label = { Text(imp.name.replace("_", " ")) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF00FF9C).copy(alpha = 0.2f),
                        selectedLabelColor = Color(0xFF00FF9C)
                    )
                )
            }
        }

        if (stances.size > 1) {
            Spacer(Modifier.height(24.dp))
            Text("2. CHOOSE STANCE", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                stances.forEach { stance ->
                    val isSelected = selectedStance == stance
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedStance = stance },
                        label = { Text(stance.name.replace("_", " ")) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF00CCFF).copy(alpha = 0.2f),
                            selectedLabelColor = Color(0xFF00CCFF)
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        if (finalVariant != null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF1C1C1E),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF00FF9C).copy(alpha = 0.3f))
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(finalVariant.name.uppercase(), color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
                        val details = buildString {
                            append(finalVariant.stance.name.replace("_", " "))
                            append(" · ")
                            append(finalVariant.specialtyBar ?: finalVariant.implement.name.replace("_", " "))
                        }
                        Text(details, color = Color.Gray, fontSize = 12.sp)
                    }
                    IconButton(onClick = { onDetailClick(finalVariant) }) {
                        Icon(Icons.Default.Info, contentDescription = "Info", tint = Color.Gray)
                    }
                    Button(
                        onClick = { onSelect(finalVariant) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF9C)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("ADD TO SESSION", color = Color.Black, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable
fun AllVariantsList(
    families: List<ExerciseFamily>,
    onSelect: (Exercise) -> Unit,
    onDetailClick: (Exercise) -> Unit
) {
    val allVariants = remember(families) { families.flatMap { it.variants }.distinctBy { it.id }.sortedBy { it.name } }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(allVariants, key = { it.id }) { variant ->
            ExerciseListItem(
                exercise = variant,
                onDetailClick = { onDetailClick(it) },
                onSelect = { onSelect(variant) }
            )
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorkoutLogCard(
    log: WorkoutLog,
    sets: List<SetLog>,
    viewModel: WorkoutViewModel,
    onActionMenuClick: () -> Unit,
    onRestTimerClick: () -> Unit,
    onViewInCodex: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val previousLogs = uiState.previousLogs
    val previousSets = previousLogs[log.exerciseId] ?: emptyList()
    val progressionState = uiState.progressionStates[log.exerciseId]
    var showSetTypeSelector by remember { mutableStateOf<SetLog?>(null) }
    var showClusterDialogFor by remember { mutableStateOf<List<SetLog>?>(null) }
    var warmupsCollapsed by remember { mutableStateOf(true) }
    
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
    
    val isCC = uiState.currentUiMode == ProtocolUiMode.CLUSTER
    val showGoalColumn = isCC || log.showGoalReps || sets.any { it.goalReps != null || it.prescribedReps != null }
    
    val augmentColor = log.augmentColor?.let { Color(android.graphics.Color.parseColor(it)) } ?: Color(0xFF007AFF)
    val neonColor = Color(0xFF00FF9C)

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
        if (showWeightIncrease && isCC) {
            ProgressionBanner(
                text = "WEIGHT INCREASE DUE (+2.5-5 lb) ⚡",
                color = Color(0xFF00FFCC),
                icon = Icons.AutoMirrored.Filled.TrendingUp
            )
        }
        
        if (showStall && isCC) {
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
                modifier = Modifier
                    .weight(1f)
                    .clickable { 
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
                        .size(36.dp)
                        .background(Color(0xFF1C1C1E), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        exerciseIcon,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    val famName = exercise?.familyName ?: log.exerciseName
                    val variantTitle = remember(exercise) {
                        if (exercise != null) {
                            if (exercise.isPrimaryVariant) "" 
                            else buildString {
                                if (exercise.stance != Stance.STANDARD) {
                                    append(exercise.stance.name.replace("_", " "))
                                }
                                if (exercise.implement != Implement.BARBELL || exercise.specialtyBar != null) {
                                    if (isNotEmpty()) append(" · ")
                                    append(exercise.specialtyBar ?: exercise.implement.name.replace("_", " "))
                                }
                            }
                        } else {
                            "" // Fallback if no exercise metadata, but we'll show log.exerciseName as famName
                        }
                    }

                    Text(
                        famName.uppercase(),
                        color = neonColor,
                        fontSize = (16 * (if (uiState.zoomLevel >= 1.5f) 0.9f else 1.0f)).sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        maxLines = 2
                    )

                    if (variantTitle.isNotEmpty()) {
                        Text(
                            variantTitle.uppercase(),
                            color = Color.Gray,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    }

                    Text(
                        "VIEW_IN_CODEX",
                        color = Color(0xFF00FF9C).copy(alpha = 0.5f),
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.clickable { onViewInCodex(log.exerciseId) }
                    )
                    
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

                    // Accomplishments badge display
                    val acc = uiState.accomplishments[log.exerciseId]
                    if (acc != null && (acc.heaviestWeight > 0f || acc.maxEstimatedOneRepMax > 0f || acc.bestClusterReps > 0)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            if (acc.heaviestWeight > 0f) {
                                Text(
                                    "MAX: ${if (acc.heaviestWeight % 1 == 0f) acc.heaviestWeight.toInt() else acc.heaviestWeight}lbs × ${acc.heaviestWeightReps}",
                                    color = Color(0xFFFFD700),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            if (acc.maxEstimatedOneRepMax > 0f) {
                                Text(
                                    "1RM: ~${acc.maxEstimatedOneRepMax.toInt()}lbs",
                                    color = Color(0xFF00FFAA),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            if (acc.bestClusterReps > 0) {
                                Text(
                                    "RP: ${acc.bestClusterReps}r @ ${acc.bestClusterWeight.toInt()}lbs",
                                    color = Color(0xFF00CCFF),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // Neural Uplink Hints
                    val hint = remember(log.exerciseId, uiState.recoveryScore) {
                        viewModel.getHintForExercise(log.exerciseId)
                    }
                    hint?.let {
                        Text(
                            it,
                            color = Color(0xFFFFA500).copy(alpha = 0.8f),
                            fontSize = 11.sp,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
            IconButton(
                onClick = onActionMenuClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Default.MoreHoriz, 
                    contentDescription = "Exercise Actions", 
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(24.dp)
                )
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
            val minutes = uiState.restTimeRemaining / 60
            val seconds = uiState.restTimeRemaining % 60
            "RESTING: %d:%02d".format(minutes, seconds)
        } else if (uiState.showLoadedStretch) {
            "STRETCHING: ${uiState.stretchTimeRemaining}s"
        } else {
            val minutes = uiState.defaultRestTime / 60
            val seconds = uiState.defaultRestTime % 60
            "Rest Timer: %d:%02d".format(minutes, seconds)
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { onRestTimerClick() }
        ) {
            Icon(
                Icons.Default.Timer, 
                contentDescription = null, 
                tint = if (uiState.isResting || uiState.showLoadedStretch) Color(0xFF00FF9C) else Color(0xFF007AFF), 
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                timerText, 
                color = if (uiState.isResting || uiState.showLoadedStretch) Color(0xFF00FF9C) else Color(0xFF007AFF), 
                fontSize = 14.sp
            )
            
            if (!uiState.isResting && !uiState.showLoadedStretch) {
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = { viewModel.startManualRestTimer() }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Start Timer", tint = Color(0xFF007AFF), modifier = Modifier.size(16.dp))
                }
            } else if (uiState.isResting) {
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = { viewModel.stopRestTimer() }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Stop, contentDescription = "Stop Timer", tint = Color.Red, modifier = Modifier.size(16.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Set Table Header
        val zoom = uiState.zoomLevel
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            val setLabel = if (zoom >= 1.5f) "S" else "SET"
            val lbsLabel = if (zoom >= 1.5f) "LB" else "LBS"
            val repsLabel = if (zoom >= 1.5f) "R" else "REPS"

            Text(setLabel, color = Color.Gray, fontSize = 12.sp, modifier = Modifier.weight(1f))
            if (showGoalColumn && zoom < 1.5f) {
                Text("GOAL", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.weight(1.5f), textAlign = TextAlign.Center)
            }
            if (zoom < 1.5f) {
                val prevLabel = if (zoom <= 0.8f) "LAST BEST" else "PREVIOUS"
                Text(prevLabel, color = Color.Gray, fontSize = 12.sp, modifier = Modifier.weight(2f))
            }
            Text(lbsLabel, color = Color.Gray, fontSize = 12.sp, modifier = Modifier.weight(1.5f), textAlign = TextAlign.Center)
            Text(repsLabel, color = Color.Gray, fontSize = 12.sp, modifier = Modifier.weight(1.5f), textAlign = TextAlign.Center)
            Icon(Icons.Default.Check, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Grouping sets for display
        val displayItems = remember(sets, uiState.currentUiMode) {
            val items = mutableListOf<Any>()
            var handledCluster = false
            val isCCMode = uiState.currentUiMode == ProtocolUiMode.CLUSTER
            
            sets.forEach { set ->
                if (isCCMode && (set.clusterMiniSetIndex != null || set.type == SetType.PARTIAL || set.type == SetType.STRETCH)) {
                    if (!handledCluster) {
                        items.add(sets.filter { it.clusterMiniSetIndex != null || it.type == SetType.PARTIAL || it.type == SetType.STRETCH })
                        handledCluster = true
                    }
                } else {
                    items.add(set)
                }
            }
            items
        }

        val isDynamicMode = uiState.currentUiMode == ProtocolUiMode.DYNAMIC
        val workSets = displayItems.filterIsInstance<SetLog>().filter { it.type != SetType.WARMUP }
        val warmupSets = displayItems.filterIsInstance<SetLog>().filter { it.type == SetType.WARMUP }

        // Render Warmups
        if (uiState.currentUiMode == ProtocolUiMode.MAX_EFFORT && warmupSets.isNotEmpty() && warmupsCollapsed) {
            Text(
                "${warmupSets.size} WARMUPS COLLAPSED", 
                color = Color.Gray, 
                fontSize = 10.sp, 
                fontWeight = FontWeight.Bold, 
                modifier = Modifier.fillMaxWidth().clickable { warmupsCollapsed = false }.padding(vertical = 8.dp), 
                textAlign = TextAlign.Center
            )
        } else {
            warmupSets.forEachIndexed { i, s -> 
                SetLogRow(
                    setNumber = i + 1,
                    set = s,
                    showGoal = showGoalColumn,
                    previousData = "-",
                    zoomLevel = uiState.zoomLevel,
                    prescribedWeight = s.prescribedWeight,
                    prescribedReps = s.prescribedReps,
                    percentOfMax = s.percentOfMax,
                    isAmrap = s.isAmrap,
                    onUpdateWeight = { viewModel.updateSet(s, weight = it) },
                    onUpdateReps = { viewModel.updateSet(s, reps = it) },
                    onUpdateGoal = { viewModel.updateSet(s, goalReps = it) },
                    onCompleteToggle = { viewModel.updateSet(s, isCompleted = !s.isCompleted) },
                    onSetLabelClick = { showSetTypeSelector = s }
                )
            }
        }

        // Render Work Sets
        if (isDynamicMode) {
            FlowRow(modifier = Modifier.fillMaxWidth(), maxItemsInEachRow = 2) {
                workSets.forEachIndexed { i, s -> 
                    Box(modifier = Modifier.fillMaxWidth(0.5f)) {
                        SetLogRow(
                            setNumber = i + warmupSets.size + 1,
                            set = s,
                            showGoal = showGoalColumn,
                            previousData = "-",
                            zoomLevel = uiState.zoomLevel,
                            prescribedWeight = s.prescribedWeight,
                            prescribedReps = s.prescribedReps,
                            percentOfMax = s.percentOfMax,
                            isAmrap = s.isAmrap,
                            onUpdateWeight = { viewModel.updateSet(s, weight = it) },
                            onUpdateReps = { viewModel.updateSet(s, reps = it) },
                            onUpdateGoal = { viewModel.updateSet(s, goalReps = it) },
                            onCompleteToggle = { viewModel.updateSet(s, isCompleted = !s.isCompleted) },
                            onSetLabelClick = { showSetTypeSelector = s }
                        )
                    }
                }
            }
        } else {
            displayItems.filter { it !in warmupSets }.forEachIndexed { index, item ->
                if (item is List<*>) {
                    @Suppress("UNCHECKED_CAST")
                    val clusterSets = item as List<SetLog>
                    val clusterKey = clusterSets.firstOrNull()?.workoutLogId ?: "cluster_$index"
                    key(clusterKey) {
                        val prevClusterSets = previousSets.filter { it.clusterMiniSetIndex != null }
                        val prevW = prevClusterSets.firstOrNull()?.weight ?: progressionState?.currentWeight
                        ClusterSetRow(
                            sets = clusterSets,
                            previousSets = prevClusterSets,
                            previousWeight = if (prevW != null && prevW > 0) prevW else null,
                            zoomLevel = zoom,
                            onUpdateWeight = { weight -> clusterSets.forEach { viewModel.updateSet(it, weight = weight) } },
                            onUpdateGoal = { goal -> clusterSets.forEach { viewModel.updateSet(it, goalReps = goal) } },
                            onClick = { showClusterDialogFor = clusterSets },
                            onSetLabelClick = { showSetTypeSelector = clusterSets.first() }
                        )
                    }
                } else if (item is SetLog) {
                    val s = item
                    key(s.id) {
                        SetLogRow(
                            setNumber = index + warmupSets.size + 1,
                            set = s,
                            showGoal = showGoalColumn,
                            previousData = "-",
                            zoomLevel = uiState.zoomLevel,
                            prescribedWeight = s.prescribedWeight,
                            prescribedReps = s.prescribedReps,
                            percentOfMax = s.percentOfMax,
                            isAmrap = s.isAmrap,
                            onUpdateWeight = { viewModel.updateSet(s, weight = it) },
                            onUpdateReps = { viewModel.updateSet(s, reps = it) },
                            onUpdateGoal = { viewModel.updateSet(s, goalReps = it) },
                            onCompleteToggle = { viewModel.updateSet(s, isCompleted = !s.isCompleted) },
                            onSetLabelClick = { showSetTypeSelector = s }
                        )
                    }
                }
                
                // Inline Rest Timer Injection
                val isMatch = if (item is SetLog) {
                    item.id == uiState.lastCompletedSetId
                } else {
                    (item as? List<SetLog>)?.any { it.id == uiState.lastCompletedSetId } == true
                }
                
                if (uiState.isResting && isMatch && 
                    (uiState.restTimerMode == RestTimerMode.INLINE || uiState.restTimerMode == RestTimerMode.BOTH)) {
                    InlineRestTimer(
                        remaining = uiState.restTimeRemaining,
                        total = uiState.restTimerTotalSeconds,
                        onAdjust = { viewModel.adjustRestTimer(it) },
                        onSkip = { viewModel.skipRestTimer() }
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
            }
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
fun CyberFinisherDialog(onDone: (Int) -> Unit) {
    var partialReps by remember { mutableStateOf(3) }
    
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
                    "LENGTHENED PARTIALS",
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

                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(
                        onClick = { if (partialReps > 0) partialReps-- },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = null, tint = Color(0xFF00FF9C))
                    }
                    
                    Text(
                        text = partialReps.toString(),
                        color = Color.White,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    
                    IconButton(
                        onClick = { partialReps++ },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF00FF9C))
                    }
                }
                
                Text(
                    "TARGET: 3-5 REPS",
                    color = if (partialReps in 3..5) Color(0xFF00FF9C) else Color.Gray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = { onDone(partialReps) },
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
                
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { 1f },
                        modifier = Modifier.size(120.dp),
                        color = Color.White.copy(alpha = 0.1f),
                        strokeWidth = 8.dp
                    )
                    CircularProgressIndicator(
                        progress = { remaining / 45f },
                        modifier = Modifier.size(120.dp),
                        color = Color(0xFFFF006E),
                        strokeWidth = 8.dp
                    )
                    Text(
                        remaining.toString(),
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                val isInhale = (remaining % 6) >= 3
                Text(
                    text = if (isInhale) "BREATHE IN" else "BREATHE OUT",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.graphicsLayer { alpha = if (remaining % 3 == 0) 0.5f else 1f }
                )
                
                Text(
                    "DEEP DIAPHRAGMATIC BREATHING",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 8.dp)
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
    zoomLevel: Float = 1.0f,
    onUpdateWeight: (Float) -> Unit,
    onUpdateGoal: (String) -> Unit,
    onClick: () -> Unit,
    onSetLabelClick: (() -> Unit)? = null
) {
    val totalReps = sets.filter { it.isCompleted && it.type == SetType.REST_PAUSE }.sumOf { it.reps }
    val isCompleted = sets.filter { it.type == SetType.REST_PAUSE }.all { it.isCompleted } && sets.isNotEmpty()
    val weight = sets.firstOrNull()?.weight ?: 0f
    val goalReps = sets.firstOrNull()?.goalReps ?: ""
    val prevTotalReps = previousSets.sumOf { it.reps }
    val weightPlaceholder = previousWeight?.let { if (it % 1 == 0f) it.toInt().toString() else it.toString() } ?: "0"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF00FFAA).copy(alpha = 0.05f), RoundedCornerShape(4.dp))
            .padding(vertical = 4.dp, horizontal = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Label (Clickable area)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSetLabelClick?.invoke() ?: onClick() }
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    "RP",
                    color = Color(0xFF00FFAA),
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp
                )
            }

            // Goal Input (Standard)
            if (zoomLevel < 1.5f) {
                EditableValueBox(
                    value = goalReps,
                    onValueChange = { onUpdateGoal(it) },
                    modifier = Modifier.weight(1.5f),
                    keyboardType = KeyboardType.Text
                )
            }

            // Previous (Standard area)
            if (zoomLevel < 1.5f) {
                Box(
                    modifier = Modifier
                        .weight(2f)
                        .clickable { onClick() }
                        .padding(vertical = 4.dp)
                ) {
                    Column {
                        Text(
                            if (prevTotalReps > 0) "${prevTotalReps} total" else "-",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                        
                        val partials = sets.find { it.type == SetType.PARTIAL }
                        val stretch = sets.find { it.type == SetType.STRETCH }
                        
                        if (partials != null || stretch != null) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                                if (partials != null) {
                                    Text("P:${partials.reps}", color = Color(0xFF00FF9C), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    if (stretch != null) Spacer(Modifier.width(6.dp))
                                }
                                if (stretch != null) {
                                    Text("S:${stretch.reps}s", color = Color(0xFFFF006E), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
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

        // Sub-row for Stacked Data (Zoom >= 1.5f)
        if (zoomLevel >= 1.5f) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 32.dp, top = 2.dp, end = 32.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "GOAL: $goalReps", 
                    color = Color.Gray, 
                    fontSize = 10.sp, 
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                
                Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.End) {
                    Text(
                        "PREV: $prevTotalReps", 
                        color = Color.Gray, 
                        fontSize = 10.sp, 
                        fontWeight = FontWeight.Bold
                    )
                    
                    val partials = sets.find { it.type == SetType.PARTIAL }
                    val stretch = sets.find { it.type == SetType.STRETCH }
                    if (partials != null || stretch != null) {
                        Spacer(Modifier.width(8.dp))
                        if (partials != null) {
                            Text("P:${partials.reps}", color = Color(0xFF00FF9C), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            if (stretch != null) Spacer(Modifier.width(4.dp))
                        }
                        if (stretch != null) {
                            Text("S:${stretch.reps}s", color = Color(0xFFFF006E), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
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
    LaunchedEffect(uiState.workoutPhase) {
        if (uiState.workoutPhase == RestPausePhase.FINISHER || uiState.workoutPhase == RestPausePhase.LOADED_STRETCH) {
            onDismiss()
        }
    }

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

                    if (uiState.userProfile?.rirCapturePerMiniSet == true) {
                        if (isActive || (set.isCompleted && set.rir != null)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("REPS IN RESERVE", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf(0, 1, 2, 3).forEach { rirValue ->
                                        val label = if (rirValue == 3) ">2" else rirValue.toString()
                                        val isSelected = set.rir == rirValue
                                        val chipColor = if (isSelected) Color(0xFF00FFAA) else Color.Gray.copy(alpha = 0.2f)
                                        
                                        Box(
                                            modifier = Modifier
                                                .size(width = 44.dp, height = 28.dp)
                                                .background(chipColor, RoundedCornerShape(4.dp))
                                                .clickable { viewModel.updateSet(set, rir = rirValue) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(label, color = if (isSelected) Color.Black else Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (uiState.userProfile?.rirCapturePerMiniSet == false) {
                    val lastSet = sets.find { it.clusterMiniSetIndex == 3 }
                    if (lastSet != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("CLUSTER INTENSITY", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("How many left in the tank?", color = Color.Gray, fontSize = 10.sp)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(0, 1, 2, 3).forEach { rirValue ->
                                    val label = if (rirValue == 3) ">2" else rirValue.toString()
                                    val isSelected = lastSet.rir == rirValue
                                    val chipColor = if (isSelected) Color(0xFF00FFAA) else Color.Gray.copy(alpha = 0.2f)
                                    
                                    Box(
                                        modifier = Modifier
                                            .size(width = 44.dp, height = 28.dp)
                                            .background(chipColor, RoundedCornerShape(4.dp))
                                            .clickable { viewModel.updateSet(lastSet, rir = rirValue) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(label, color = if (isSelected) Color.Black else Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                // Show Finishers if they exist
                val partials = sets.find { it.type == SetType.PARTIAL }
                val stretch = sets.find { it.type == SetType.STRETCH }
                
                if (partials != null || stretch != null) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Text("CYBER FINISHERS", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (partials != null) {
                            Surface(
                                modifier = Modifier.weight(1f),
                                color = Color(0xFF00FF9C).copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFF00FF9C).copy(alpha = 0.3f))
                            ) {
                                Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("PARTIALS", color = Color(0xFF00FF9C), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Text("${partials.reps}", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                        
                        if (stretch != null) {
                            Surface(
                                modifier = Modifier.weight(1f),
                                color = Color(0xFFFF006E).copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFFFF006E).copy(alpha = 0.3f))
                            ) {
                                Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("STRETCH", color = Color(0xFFFF006E), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Text("${stretch.reps}s", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                val totalReps = sets.filter { it.isCompleted && it.type == SetType.REST_PAUSE }.sumOf { it.reps }
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
    zoomLevel: Float = 1.0f,
    onUpdateWeight: (Float) -> Unit,
    onUpdateReps: (Int) -> Unit,
    onUpdateGoal: (String) -> Unit,
    onCompleteToggle: () -> Unit,
    onSetLabelClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val backgroundColor = if (setNumber % 2 == 0) Color.Transparent else Color(0xFF1C1C1E).copy(alpha = 0.3f)
    val weightPlaceholder = previousWeight?.let { if (it % 1 == 0f) it.toInt().toString() else it.toString() } ?: "0"
    
    val setLabel = when (set.type) {
        SetType.NORMAL -> "$setNumber"
        SetType.WARMUP -> "W"
        SetType.DROP -> "D"
        SetType.FAILURE -> "F"
        SetType.REST_PAUSE -> if (set.clusterMiniSetIndex != null) "RP ${set.clusterMiniSetIndex}" else "RP"
        SetType.WIDOWMAKER -> "WM"
        SetType.POWER -> "P"
        SetType.GS -> "GS"
        SetType.PARTIAL -> "PAR"
        SetType.STRETCH -> "STR"
        SetType.MAX_EFFORT -> "ME"
    }
    val labelColor = when (set.type) {
        SetType.WARMUP -> Color(0xFFFFA500)
        SetType.DROP -> Color(0xFF00CCFF)
        SetType.FAILURE -> Color(0xFFFF4444)
        SetType.REST_PAUSE -> Color(0xFF00FFAA)
        SetType.WIDOWMAKER -> Color(0xFFFF00FF)
        SetType.POWER -> Color(0xFFFFD700)
        SetType.GS -> Color(0xFF00CCFF)
        SetType.PARTIAL -> Color(0xFF00FF9C)
        SetType.STRETCH -> Color(0xFFFF006E)
        SetType.MAX_EFFORT -> Color(0xFFFF0000)
        else -> Color.White
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Set Label
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSetLabelClick() },
                contentAlignment = Alignment.CenterStart
            ) {
                Text(setLabel, color = labelColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            // Goal Input (Standard)
            if (showGoal && zoomLevel < 1.5f) {
                val isGoalMandatory = set.type == SetType.WIDOWMAKER
                EditableValueBox(
                    value = set.goalReps ?: "",
                    onValueChange = { onUpdateGoal(it) },
                    modifier = Modifier.weight(1.5f),
                    keyboardType = KeyboardType.Text,
                    enabled = !isGoalMandatory
                )
            }

            // Previous Data (Standard)
            if (zoomLevel < 1.5f) {
                Text(previousData, color = Color.Gray, fontSize = 14.sp, modifier = Modifier.weight(2f))
            }
            
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
                value = if (set.type == SetType.STRETCH) "${set.reps}s" else set.reps.toString(),
                onValueChange = { 
                    val clean = it.replace("s", "")
                    clean.toIntOrNull()?.let { r -> onUpdateReps(r) } 
                },
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
                    tint = if (set.isCompleted) Color.Black else Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Sub-row for Stacked Data (Zoom >= 1.5f)
        if (zoomLevel >= 1.5f) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 32.dp, top = 2.dp, end = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (showGoal) {
                    Text(
                        "GOAL: ${set.goalReps ?: "-"}", 
                        color = Color.Gray, 
                        fontSize = 10.sp, 
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                }
                Text(
                    "PREV: $previousData", 
                    color = Color.Gray, 
                    fontSize = 10.sp, 
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun EditableValueBox(
    value: String,
    onValueChange: (String) -> Unit,
    onCommit: () -> Unit = {},
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
                autoCorrectEnabled = false,
                imeAction = androidx.compose.ui.text.input.ImeAction.Done
            ),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                onDone = { onCommit() }
            ),
            cursorBrush = SolidColor(Color(0xFF007AFF)),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { 
                    val wasFocused = isFocused
                    isFocused = it.isFocused
                    if (wasFocused && !it.isFocused) {
                        onCommit()
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorkoutExploreScreen(
    uiState: WorkoutUiState,
    onBack: () -> Unit,
    onProtocolClick: (WorkoutProtocol) -> Unit,
    onAddProtocol: (WorkoutProtocol) -> Unit,
    onAddAugment: (WorkoutAugment) -> Unit,
    onAddRoutine: (WorkoutRoutine) -> Unit
) {
    val sortedProtocols = remember {
        WorkoutProtocol.entries
            .filter { it.isSelectableEngine }
            .sortedBy { protocol ->
                when (protocol.dossier.recommendedLevel) {
                    ExperienceLevel.ANY -> 0
                    ExperienceLevel.NOVICE -> 1
                    ExperienceLevel.INTERMEDIATE -> 2
                    ExperienceLevel.ADVANCED -> 3
                }
            }
    }

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

            items(sortedProtocols) { protocol ->
                val isActive = uiState.userProfile?.activeProtocol == protocol
                val dossier = protocol.dossier
                
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(),
                    color = Color(0xFF1C1C1E),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (isActive) Color(0xFF00FF9C) else Color.White.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onProtocolClick(protocol) }
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    protocol.displayName,
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black
                                )
                                if (isActive) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        color = Color(0xFF00FF9C),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            "ACTIVE",
                                            color = Color.Black,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Black,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            
                            Row(
                                modifier = Modifier.padding(top = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    protocol.loreName,
                                    color = Color(0xFF00FF9C).copy(alpha = 0.7f),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                
                                Text(
                                    protocol.frequencyCaption,
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                
                                val levelColor = when (dossier.recommendedLevel) {
                                    ExperienceLevel.ADVANCED -> Color(0xFFFF006E) // Cyber Red
                                    ExperienceLevel.INTERMEDIATE -> Color(0xFFFFA500) // Hazard Orange
                                    else -> Color(0xFF00FF9C) // Cyber Green
                                }

                                Surface(
                                    color = levelColor.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(4.dp),
                                    border = BorderStroke(0.5.dp, levelColor.copy(alpha = 0.5f))
                                ) {
                                    Text(
                                        dossier.recommendedLevel.name,
                                        color = levelColor,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (!isActive) {
                                IconButton(
                                    onClick = { onAddProtocol(protocol) },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(Color(0xFF00FF9C).copy(alpha = 0.2f), CircleShape)
                                ) {
                                    Icon(
                                        Icons.Default.Add, 
                                        contentDescription = "Set Protocol Active", 
                                        tint = Color(0xFF00FF9C), 
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
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
    recommendedDays: Int,
    frequencyCopy: String,
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
                "REMINDERS · ${protocol.displayName} · ${recommendedDays}× / WEEK",
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
                    frequencyCopy,
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                val selectedCount = tempProfile.scheduledDays.size
                if (selectedCount != recommendedDays) {
                    val warnText = when {
                        protocol == WorkoutProtocol.HST && selectedCount > recommendedDays -> "LADDER ASSUMES 3×. EXTRA DAYS COMPRESS THE WAVE."
                        protocol == WorkoutProtocol.STARTING_STRENGTH && selectedCount != recommendedDays -> "3× IS THE LINEAR PROGRESSION CADENCE."
                        protocol == WorkoutProtocol.FIVE_THREE_ONE && selectedCount < recommendedDays -> "4TH MAIN WILL LAG THIS CYCLE."
                        protocol == WorkoutProtocol.WESTSIDE && selectedCount != recommendedDays -> "MISSING A ME OR DE DAY."
                        else -> null
                    }
                    
                    warnText?.let {
                        Surface(
                            color = Color(0xFFFF006E).copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFFFF006E).copy(alpha = 0.5f)),
                            modifier = Modifier.padding(bottom = 24.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFF006E), modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(it, color = Color(0xFFFF006E), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

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
            val initialHour = initialTime.split(":")[0].toIntOrNull() ?: 9
            val initialMinute = initialTime.split(":")[1].toIntOrNull() ?: 0
            
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
                        Text("CONFIRM", color = Color(0xFF00FF9C), fontWeight = FontWeight.Bold) 
                    } 
                },
                dismissButton = {
                    TextButton(onClick = { showTimePickerForDay = null }) {
                        Text("CANCEL", color = Color.Gray)
                    }
                },
                title = { 
                    Text(
                        "TRAINING TIME WINDOW", 
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    ) 
                }, 
                text = { 
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        TimePicker(state = timePickerState) 
                    }
                }, 
                containerColor = Color(0xFF1C1C1E),
                shape = RoundedCornerShape(16.dp)
            )
        }

    }
}

@Composable
fun ProtocolDetailScreen(
    protocol: WorkoutProtocol,
    uiState: WorkoutUiState,
    viewModel: WorkoutViewModel,
    onBack: () -> Unit,
    onStartProtocol: (WorkoutProtocol) -> Unit,
    onAddProtocol: (WorkoutProtocol) -> Unit,
    onAddRoutine: (WorkoutRoutine) -> Unit,
    onRoutineClick: (WorkoutRoutine) -> Unit
) {
    val dossier = protocol.dossier
    val isIntakeNeeded = viewModel.isIntakeNeededForProtocol(protocol)
    val isActive = uiState.userProfile?.activeProtocol == protocol

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
                "PROTOCOL DOSSIER",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                modifier = Modifier.align(Alignment.Center),
                fontFamily = FontFamily.Monospace
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f).padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                Column {
                    Text(
                        dossier.displayName,
                        color = Color.White,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        dossier.loreName,
                        color = Color(0xFF00FF9C),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }
            }

            item { DossierSection("FOCUS", dossier.focus) }

            item { 
                DossierSection("RECOMMENDED", "") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = Color(0xFF00FF9C).copy(alpha = 0.1f),
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(1.dp, Color(0xFF00FF9C))
                        ) {
                            Text(
                                dossier.recommendedLevel.name,
                                color = Color(0xFF00FF9C),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(dossier.alsoFits, color = Color.Gray, fontSize = 13.sp)
                    }
                }
            }

            item { DossierSection("FREQUENCY", "${dossier.daysPerWeek}× / WEEK") }

            item { DossierSection("CONTRACT", dossier.loggingContract) }

            item { DossierSection("INTAKE", dossier.intake) }

            item {
                DossierSection("TENETS", "") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        dossier.tenets.forEachIndexed { index, tenet ->
                            Row {
                                Text("0", color = Color(0xFF00FF9C), fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                                Text("${index + 1}", color = Color(0xFF00FF9C), fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                                Spacer(Modifier.width(12.dp))
                                Text(tenet, color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                            }
                        }
                    }
                }
            }

            item { DossierSection("SESSION", dossier.sessionAnatomy) }

            item { DossierSection("NOT THIS", dossier.notThis) }
        }

        // Footer
        Box(modifier = Modifier.padding(24.dp).navigationBarsPadding()) {
            if (isActive) {
                Button(
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1C1C1E)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF00FF9C))
                ) {
                    Text("CURRENTLY ACTIVE", color = Color(0xFF00FF9C), fontWeight = FontWeight.Black)
                }
            } else {
                val cta = if (isIntakeNeeded) "CALIBRATE ENGINE" else "SET ACTIVE"
                Button(
                    onClick = { onAddProtocol(protocol) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF9C)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(cta, color = Color.Black, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
fun DossierSection(label: String, content: String, customContent: @Composable () -> Unit = {}) {
    Column {
        Text(
            label,
            color = Color(0xFF00FF9C),
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp,
            fontFamily = FontFamily.Monospace
        )
        Spacer(Modifier.height(8.dp))
        if (content.isNotEmpty()) {
            Text(
                content,
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 15.sp,
                lineHeight = 22.sp
            )
        } else {
            customContent()
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
                                    SetType.PARTIAL -> "PARTIAL"
                                    SetType.STRETCH -> "STRETCH"
                                    else -> "SET ${index + 1}"
                                }
                                val typeColor = when (set.type) {
                                    SetType.WARMUP -> Color(0xFFFFA500)
                                    SetType.REST_PAUSE -> Color(0xFF00FFAA)
                                    SetType.POWER -> Color(0xFFFFD700)
                                    SetType.WIDOWMAKER -> Color(0xFFFF00FF)
                                    SetType.GS -> Color(0xFF00CCFF)
                                    SetType.PARTIAL -> Color(0xFF00FF9C)
                                    SetType.STRETCH -> Color(0xFFFF006E)
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

@Composable
fun RestTimerAdjustmentDialog(
    uiState: WorkoutUiState,
    viewModel: WorkoutViewModel,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            color = Color(0xFF1C1C1E),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFF007AFF).copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "UPDATE REST TIMERS",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                
                Text(
                    "Completed timers will not be affected. Durations will be saved for next time.",
                    color = Color.Gray,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
                )
                
                TimerConfigRow("Work Set", uiState.workSetRestTime) { viewModel.updateWorkSetRestTime(it) }
                Spacer(Modifier.height(12.dp))
                TimerConfigRow("Warm-up Set", uiState.warmupSetRestTime) { viewModel.updateWarmupSetRestTime(it) }
                Spacer(Modifier.height(12.dp))
                TimerConfigRow("Drop Set", uiState.dropSetRestTime) { viewModel.updateDropSetRestTime(it) }
                
                Spacer(Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("AUTO-START TIMER", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("Starts when set is completed", color = Color.Gray, fontSize = 11.sp)
                    }
                    Switch(
                        checked = uiState.isAutoStartTimerEnabled,
                        onCheckedChange = { viewModel.toggleAutoStartTimer() },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00FF9C))
                    )
                }

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("DISPLAY MODE", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    
                    val modes = RestTimerMode.entries.filter { it != RestTimerMode.NONE }
                    Row {
                        modes.forEach { mode ->
                            val isSelected = uiState.restTimerMode == mode
                            Surface(
                                modifier = Modifier
                                    .padding(start = 4.dp)
                                    .clickable { viewModel.updateRestTimerMode(mode) },
                                color = if (isSelected) Color(0xFF007AFF) else Color.DarkGray,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    mode.name, 
                                    color = Color.White, 
                                    fontSize = 10.sp, 
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("UPDATE REST TIMERS", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

    }
}

@Composable
fun TimerConfigRow(label: String, currentSeconds: Int, onUpdate: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        
        Surface(
            color = Color.Black.copy(alpha = 0.3f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.width(100.dp).height(40.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onUpdate((currentSeconds - 15).coerceAtLeast(0)) }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Remove, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                }
                
                val minutes = currentSeconds / 60
                val seconds = currentSeconds % 60
                Text(
                    if (currentSeconds == 0) "None" else "%d:%02d".format(minutes, seconds),
                    color = Color.White,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace
                )
                
                IconButton(onClick = { onUpdate((currentSeconds + 15).coerceAtMost(600)) }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                }
            }
        }

    }
}

@Composable
fun RestTimerPopup(
    remaining: Int,
    total: Int,
    onAdjust: (Int) -> Unit,
    onSkip: () -> Unit
) {
    Dialog(onDismissRequest = { }) { // Non-dismissable by tapping outside
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            color = Color.White,
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onSkip) { Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray) }
                    Text("Rest Timer", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Box(Modifier.size(48.dp))
                }

                Text(
                    "Adjust duration via the +/- buttons.",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(vertical = 16.dp)
                )

                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(200.dp)) {
                    val progress = if (total > 0) remaining.toFloat() / total else 0f
                    CircularProgressIndicator(
                        progress = { 1f - progress },
                        modifier = Modifier.fillMaxSize(),
                        strokeWidth = 12.dp,
                        color = Color(0xFF007AFF).copy(alpha = 0.2f),
                        trackColor = Color.Transparent
                    )
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxSize(),
                        strokeWidth = 12.dp,
                        color = Color(0xFF007AFF),
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val minutes = remaining / 60
                        val seconds = remaining % 60
                        Text(
                            "%d:%02d".format(minutes, seconds),
                            color = Color.Black,
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        val tMin = total / 60
                        val tSec = total % 60
                        Text(
                            "%d:%02d".format(tMin, tSec),
                            color = Color.Gray,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onAdjust(-10) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF2F2F7)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("-10s", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { onAdjust(10) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF2F2F7)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("+10s", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onSkip,
                        modifier = Modifier.weight(1.2f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Skip", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

    }
}

@Composable
fun InlineRestTimer(
    remaining: Int,
    total: Int,
    onAdjust: (Int) -> Unit,
    onSkip: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .height(44.dp),
        color = Color(0xFF007AFF).copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color(0xFF007AFF).copy(alpha = 0.3f))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val progress = if (total > 0) remaining.toFloat() / total else 0f
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress)
                    .background(Color(0xFF007AFF).copy(alpha = 0.6f))
            )
            
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val minutes = remaining / 60
                val seconds = remaining % 60
                Text(
                    "%d:%02d".format(minutes, seconds),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    fontFamily = FontFamily.Monospace
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onAdjust(-10) }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Remove, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = { onAdjust(10) }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Skip", 
                        color = Color.White, 
                        fontWeight = FontWeight.Bold, 
                        fontSize = 12.sp,
                        modifier = Modifier.clickable { onSkip() }
                    )
                }
            }
        }

    }
}

@Composable
fun StickyBottomTimer(
    remaining: Int,
    total: Int,
    onAdjust: (Int) -> Unit,
    onSkip: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().padding(bottom = 80.dp), contentAlignment = Alignment.BottomCenter) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(56.dp),
            color = Color(0xFF1C1C1E).copy(alpha = 0.95f),
            shape = RoundedCornerShape(12.dp),
            shadowElevation = 8.dp,
            border = BorderStroke(1.dp, Color(0xFF007AFF).copy(alpha = 0.5f))
        ) {
            Column {
                val progress = if (total > 0) remaining.toFloat() / total else 0f
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                    color = Color(0xFF007AFF),
                    trackColor = Color.Transparent
                )
                
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Timer, contentDescription = null, tint = Color(0xFF007AFF), modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        val minutes = remaining / 60
                        val seconds = remaining % 60
                        Text(
                            "RESTING: %d:%02d".format(minutes, seconds),
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { onAdjust(15) }) {
                            Text("+15s", color = Color(0xFF007AFF), fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = onSkip,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("SKIP", color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

    }
}

@Composable
fun StagnationBadge(exerciseName: String, onRotate: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        color = Color(0xFFFF4444).copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color(0xFFFF4444))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "STAGNATION DETECTED",
                    color = Color(0xFFFF4444),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    "You've failed to beat the log for $exerciseName twice. System recommends rotation.",
                    color = Color.White,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            
            Button(
                onClick = onRotate,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4444)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text("ROTATE MISSION", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

data class HstFamilyState(
    val rm15: String = "",
    val rm10: String = "",
    val rm5: String = "",
    val rm1: String = "",
    val manualFields: Set<String> = emptySet()
)

@Composable
fun HstIntakeScreen(
    onDismiss: () -> Unit,
    onSubmit: (Map<String, Triple<Float?, Float?, Float?>>) -> Unit,
    uiState: WorkoutUiState
) {
    val families = listOf(
        "squat" to "SQUAT",
        "bench_press" to "BENCH PRESS",
        "rows" to "ROW",
        "overhead_press" to "OVERHEAD PRESS",
        "deadlift" to "RDL"
    )

    var familyStates by remember { 
        mutableStateOf(families.associate { it.first to HstFamilyState() }) 
    }

    LaunchedEffect(uiState.accomplishments) {
        val newState = familyStates.toMutableMap()
        var changed = false
        families.forEach { (id, _) ->
            val acc = uiState.accomplishments[id]
            if (acc != null && acc.maxEstimatedOneRepMax > 0f && newState[id]?.rm1.isNullOrEmpty()) {
                val current = newState[id] ?: HstFamilyState()
                newState[id] = current.copy(rm1 = acc.maxEstimatedOneRepMax.toInt().toString())
                changed = true
            }
        }
        if (changed) familyStates = newState
    }

    fun updateField(familyId: String, field: String, value: String) {
        val current = familyStates[familyId] ?: HstFamilyState()
        val nextManual = if (value.isEmpty()) current.manualFields - field else current.manualFields + field
        
        val nextState = current.copy(
            rm1 = if (field == "rm1") value else current.rm1,
            rm15 = if (field == "rm15") value else current.rm15,
            rm10 = if (field == "rm10") value else current.rm10,
            rm5 = if (field == "rm5") value else current.rm5,
            manualFields = nextManual
        )

        val nextMap = familyStates.toMutableMap()
        nextMap[familyId] = nextState
        familyStates = nextMap
    }

    fun performEstimation(familyId: String, sourceField: String) {
        val current = familyStates[familyId] ?: return
        val sourceVal = when(sourceField) {
            "rm1" -> current.rm1
            "rm15" -> current.rm15
            "rm10" -> current.rm10
            "rm5" -> current.rm5
            else -> ""
        }.toFloatOrNull() ?: 0f

        if (sourceVal < 20f) return 

        val pivot = when (sourceField) {
            "rm1" -> sourceVal
            "rm15" -> sourceVal / 0.65f
            "rm10" -> sourceVal / 0.75f
            "rm5" -> sourceVal / 0.86f
            else -> 0f
        }

        if (pivot <= 0) return

        fun roundToHst(v: Float) = Math.round(v).toString()

        val nextState = current.copy(
            rm1 = if (!current.manualFields.contains("rm1") || sourceField == "rm1") Math.round(pivot).toString() else current.rm1,
            rm15 = if (!current.manualFields.contains("rm15") || sourceField == "rm15") roundToHst(pivot * 0.65f) else current.rm15,
            rm10 = if (!current.manualFields.contains("rm10") || sourceField == "rm10") roundToHst(pivot * 0.75f) else current.rm10,
            rm5 = if (!current.manualFields.contains("rm5") || sourceField == "rm5") roundToHst(pivot * 0.86f) else current.rm5
        )

        val nextMap = familyStates.toMutableMap()
        nextMap[familyId] = nextState
        familyStates = nextMap
    }

    fun estimateEmpty(familyId: String) {
        val current = familyStates[familyId] ?: return
        val sourceField = when {
            current.rm1.isNotEmpty() -> "rm1"
            current.rm5.isNotEmpty() -> "rm5"
            current.rm10.isNotEmpty() -> "rm10"
            current.rm15.isNotEmpty() -> "rm15"
            else -> null
        } ?: return
        performEstimation(familyId, sourceField)
    }

    var showPreview by remember { mutableStateOf(false) }

    if (showPreview) {
        HstLadderPreview(
            rms = families.associate { (id, label) ->
                label to Triple(
                    familyStates[id]?.rm15?.toFloatOrNull() ?: 0f,
                    familyStates[id]?.rm10?.toFloatOrNull() ?: 0f,
                    familyStates[id]?.rm5?.toFloatOrNull() ?: 0f
                )
            },
            onConfirm = {
                onSubmit(families.associate { (id, _) ->
                    id to Triple(
                        familyStates[id]?.rm15?.toFloatOrNull(),
                        familyStates[id]?.rm10?.toFloatOrNull(),
                        familyStates[id]?.rm5?.toFloatOrNull()
                    )
                })
            },
            onBack = { showPreview = false }
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .statusBarsPadding()
                .padding(24.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("INITIALIZATION", color = Color(0xFF00FF9C), fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray) }
            }

            Text("HST", color = Color.White, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
            Text("ENTER 15 / 10 / 5 RMS. WE BUILD THE LADDER.", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))

            Spacer(modifier = Modifier.height(24.dp))

            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                items(families) { (id, label) ->
                    val state = familyStates[id] ?: HstFamilyState()
                    HstFamilyInput(
                        label = label,
                        state = state,
                        onUpdate = { field, value -> updateField(id, field, value) },
                        onCommit = { field -> performEstimation(id, field) },
                        onEstimateEmpty = { estimateEmpty(id) }
                    )
                }
            }

            val allFilled = families.all { (id, _) ->
                val s = familyStates[id]
                s != null && s.rm15.isNotEmpty() && s.rm10.isNotEmpty() && s.rm5.isNotEmpty()
            }

            Button(
                onClick = { showPreview = true },
                enabled = allFilled,
                modifier = Modifier.fillMaxWidth().height(56.dp).padding(top = 16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00FF9C),
                    disabledContainerColor = Color.DarkGray
                )
            ) {
                Text("CALIBRATE ENGINE", color = if (allFilled) Color.Black else Color.Gray, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
fun HstFamilyInput(
    label: String,
    state: HstFamilyState,
    onUpdate: (String, String) -> Unit,
    onCommit: (String) -> Unit,
    onEstimateEmpty: () -> Unit
) {
    Column {
        Text(label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val isEst = !state.manualFields.contains("rm1")
            Column(modifier = Modifier.weight(1f)) {
                Text(if (isEst && state.rm1.isNotEmpty()) "1RM (EST)" else "1RM", color = if (isEst && state.rm1.isNotEmpty()) Color(0xFF00FF9C).copy(alpha = 0.6f) else Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                EditableValueBox(
                    value = state.rm1,
                    onValueChange = { onUpdate("rm1", it) },
                    onCommit = { onCommit("rm1") },
                    placeholder = "1RM",
                    keyboardType = KeyboardType.Number
                )
            }
            Button(
                onClick = onEstimateEmpty,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1C1C1E)),
                shape = RoundedCornerShape(4.dp),
                contentPadding = PaddingValues(horizontal = 12.dp),
                modifier = Modifier.align(Alignment.Bottom)
            ) {
                Text("ESTIMATE EMPTY", color = Color(0xFF00FF9C), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RmInputField("15RM", state.rm15, !state.manualFields.contains("rm15"), { onUpdate("rm15", it) }, { onCommit("rm15") }, Modifier.weight(1f))
            RmInputField("10RM", state.rm10, !state.manualFields.contains("rm10"), { onUpdate("rm10", it) }, { onCommit("rm10") }, Modifier.weight(1f))
            RmInputField("5RM", state.rm5, !state.manualFields.contains("rm5"), { onUpdate("rm5", it) }, { onCommit("rm5") }, Modifier.weight(1f))
        }
    }
}

@Composable
fun RmInputField(label: String, value: String, isEstimated: Boolean, onValueChange: (String) -> Unit, onCommit: () -> Unit, modifier: Modifier) {
    Column(modifier = modifier) {
        Text(
            text = if (isEstimated && value.isNotEmpty()) "$label (EST)" else label, 
            color = if (isEstimated && value.isNotEmpty()) Color(0xFF00FF9C).copy(alpha = 0.6f) else Color.Gray, 
            fontSize = 10.sp, 
            fontWeight = FontWeight.Bold, 
            modifier = Modifier.fillMaxWidth(), 
            textAlign = TextAlign.Center
        )
        EditableValueBox(
            value = value,
            onValueChange = onValueChange,
            onCommit = onCommit,
            placeholder = "0",
            keyboardType = KeyboardType.Number
        )
    }
}

@Composable
fun HstLadderPreview(
    rms: Map<String, Triple<Float, Float, Float>>,
    onConfirm: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .padding(24.dp)
    ) {
        Text("LADDER PREVIEW", color = Color(0xFF00FF9C), fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
        Text("6 SESSIONS PER BLOCK", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
        
        Spacer(modifier = Modifier.height(24.dp))
        
        val previewFamilies = rms.keys.toList()
        
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(previewFamilies) { familyLabel ->
                val rmsValue = rms[familyLabel] ?: Triple(0f, 0f, 0f)
                Column(modifier = Modifier.background(Color(0xFF1C1C1E), RoundedCornerShape(8.dp)).padding(16.dp)) {
                    Text(familyLabel, color = Color(0xFF00FF9C), fontWeight = FontWeight.Black, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        LadderBlockPreview("15s", rmsValue.first)
                        LadderBlockPreview("10s", rmsValue.second)
                        LadderBlockPreview("5s", rmsValue.third)
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onConfirm,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF9C))
        ) {
            Text("START CYCLE", color = Color.Black, fontWeight = FontWeight.Black)
        }
        
        TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("BACK TO EDIT", color = Color.Gray)
        }
    }
}

@Composable
fun LadderBlockPreview(label: String, rm: Float) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(4.dp)) {
        Text(label, color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        val start = rm * 0.8f
        val step = (rm - start) / 5f
        (0 until 6).forEach { i ->
            val weight = Math.round(start + i * step)
            Text("$weight", color = if (i == 5) Color(0xFF00FF9C) else Color.White, fontSize = 11.sp, fontWeight = if (i == 5) FontWeight.Black else FontWeight.Normal)
        }
    }
}

@Composable
fun ProtocolIntakeScreen(
    protocol: WorkoutProtocol,
    onDismiss: () -> Unit,
    onSubmit: (Map<String, Float>) -> Unit
) {
    val families = when (protocol) {
        WorkoutProtocol.STARTING_STRENGTH -> listOf("squat", "bench_press", "overhead_press", "deadlift")
        WorkoutProtocol.FIVE_THREE_ONE -> listOf("overhead_press", "deadlift", "bench_press", "squat")
        WorkoutProtocol.WESTSIDE -> listOf("squat", "bench_press", "deadlift")
        WorkoutProtocol.DUP -> listOf("squat", "bench_press", "rows")
        else -> emptyList()
    }

    var weights by remember { mutableStateOf(families.associateWith { "" }) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .padding(24.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("INITIALIZATION", color = Color(0xFF00FF9C), fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
            IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray) }
        }

        Text(
            protocol.displayName,
            color = Color.White,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Black
        )
        
        val intakeNote = when (protocol) {
            WorkoutProtocol.FIVE_THREE_ONE, WorkoutProtocol.WESTSIDE -> "ENTER CURRENT 1RM (ESTIMATED OK)"
            else -> "ENTER CURRENT WORKING WEIGHT (STRENGTH DAY)"
        }
        
        Text(intakeNote, color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))

        Spacer(modifier = Modifier.height(32.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(families) { familyId ->
                val label = familyId.replace("_", " ").uppercase()
                Column {
                    Text(label, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    EditableValueBox(
                        value = weights[familyId] ?: "",
                        onValueChange = { val newMap = weights.toMutableMap(); newMap[familyId] = it; weights = newMap },
                        placeholder = "0.0",
                        modifier = Modifier.fillMaxWidth(),
                        keyboardType = KeyboardType.Decimal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { 
                val results = weights.mapValues { it.value.toFloatOrNull() ?: 0f }
                onSubmit(results)
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF9C))
        ) {
            Text("CALIBRATE ENGINE", color = Color.Black, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun StrategicDeconditioningWindow(onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.9f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Icon(Icons.Default.HourglassEmpty, contentDescription = null, tint = Color(0xFF00CCFF), modifier = Modifier.size(64.dp))
            Spacer(modifier = Modifier.height(24.dp))
            Text("STRATEGIC DECONDITIONING", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
            Text(
                "Neural recovery in progress. System is purging systemic fatigue to restore hypertrophy sensitivity.",
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp)
            )
            Spacer(modifier = Modifier.height(48.dp))
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00CCFF))
            ) {
                Text("UNDERSTOOD", color = Color.Black, fontWeight = FontWeight.Black)
            }
        }
    }
}


