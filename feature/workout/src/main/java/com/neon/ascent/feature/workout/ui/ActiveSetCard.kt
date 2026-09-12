package com.neon.ascent.feature.workout.ui

import androidx.compose.runtime.Composable
import com.neon.ascent.core.domain.workout.models.SetLog

@Composable
fun ActiveSetCard(
    setNumber: Int,
    set: SetLog,
    showGoal: Boolean = true,
    previousData: String = "-",
    zoomLevel: Float = 1.0f,
    prescribedWeight: Float? = null,
    prescribedReps: Int? = null,
    percentOfMax: Float? = null,
    isAmrap: Boolean = false,
    onUpdateWeight: (Float) -> Unit,
    onUpdateReps: (Int) -> Unit,
    onUpdateGoal: (String) -> Unit,
    onCompleteToggle: () -> Unit,
    onSetLabelClick: (() -> Unit)? = null
) {
    SetLogRow(
        setNumber = setNumber,
        set = set,
        showGoal = showGoal,
        previousData = previousData,
        zoomLevel = zoomLevel,
        prescribedWeight = prescribedWeight,
        prescribedReps = prescribedReps,
        percentOfMax = percentOfMax,
        isAmrap = isAmrap,
        onUpdateWeight = onUpdateWeight,
        onUpdateReps = onUpdateReps,
        onUpdateGoal = onUpdateGoal,
        onCompleteToggle = onCompleteToggle,
        onSetLabelClick = onSetLabelClick ?: {}
    )
}
