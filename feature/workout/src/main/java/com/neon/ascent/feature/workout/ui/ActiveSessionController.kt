package com.neon.ascent.feature.workout.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import com.neon.ascent.core.common.HapticService
import com.neon.ascent.core.domain.workout.models.RestPausePhase
import com.neon.ascent.core.domain.workout.models.SetLog
import com.neon.ascent.core.domain.workout.models.SetType
import com.neon.ascent.feature.workout.services.WorkoutTimerService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

data class ActiveSessionState(
    val isResting: Boolean = false,
    val restTimeRemaining: Int = 0,
    val restTimerTotalSeconds: Int = 0,
    val lastCompletedSetId: String? = null,
    val currentClusterIndex: Int? = null,
    val showCyberFinisher: Boolean = false,
    val showLoadedStretch: Boolean = false,
    val stretchTimeRemaining: Int = 0,
    val workoutDurationSeconds: Long = 0,
    val isPaused: Boolean = false,
    val workoutPhase: RestPausePhase = RestPausePhase.NOT_ACTIVE,
    val showUncompletedSetsDialog: Boolean = false,
    val activeSessionError: String? = null
)

@Singleton
class ActiveSessionController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val hapticService: HapticService
) {
    private val _state = MutableStateFlow(ActiveSessionState())
    val state: StateFlow<ActiveSessionState> = _state.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main)
    private var workoutDurationJob: Job? = null
    private var stretchTimerJob: Job? = null

    private val timerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                WorkoutTimerService.ACTION_TIMER_TICK -> {
                    val remaining = intent.getIntExtra(WorkoutTimerService.EXTRA_REMAINING, 0)
                    _state.update { it.copy(restTimeRemaining = remaining, isResting = true) }
                }
                WorkoutTimerService.ACTION_TIMER_FINISHED -> {
                    val isClusterTimer = _state.value.workoutPhase == RestPausePhase.MINI_SET_2 ||
                            _state.value.workoutPhase == RestPausePhase.MINI_SET_3
                    if (isClusterTimer) {
                        hapticService.clusterTimerBuzz()
                    }
                    _state.update { it.copy(restTimeRemaining = 0, isResting = false) }
                }
            }
        }
    }

    init {
        registerTimerReceiver()
    }

    private fun registerTimerReceiver() {
        val filter = IntentFilter().apply {
            addAction(WorkoutTimerService.ACTION_TIMER_TICK)
            addAction(WorkoutTimerService.ACTION_TIMER_FINISHED)
        }
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(timerReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                context.registerReceiver(timerReceiver, filter)
            }
        }
    }

    fun unregisterTimerReceiver() {
        runCatching {
            context.unregisterReceiver(timerReceiver)
        }
    }

    fun startDurationTimer(initialDurationSeconds: Long = 0) {
        workoutDurationJob?.cancel()
        _state.update { it.copy(workoutDurationSeconds = initialDurationSeconds, isPaused = false) }
        workoutDurationJob = scope.launch {
            while (true) {
                delay(1000)
                if (!_state.value.isPaused) {
                    _state.update { it.copy(workoutDurationSeconds = it.workoutDurationSeconds + 1) }
                }
            }
        }
    }

    fun stopDurationTimer() {
        workoutDurationJob?.cancel()
        workoutDurationJob = null
    }

    fun pauseWorkout() {
        _state.update { it.copy(isPaused = true) }
    }

    fun resumeWorkout() {
        _state.update { it.copy(isPaused = false) }
    }

    fun startManualRestTimer(defaultRestTime: Int) {
        WorkoutTimerService.start(context, defaultRestTime)
    }

    fun stopRestTimer() {
        WorkoutTimerService.stop(context)
        _state.update { it.copy(isResting = false, restTimeRemaining = 0, lastCompletedSetId = null) }
    }

    fun skipRestTimer() {
        stopRestTimer()
    }

    fun adjustRestTimer(seconds: Int) {
        val intent = Intent(context, WorkoutTimerService::class.java).apply {
            action = WorkoutTimerService.ACTION_ADD_TIME
            putExtra(WorkoutTimerService.EXTRA_SECONDS, seconds)
        }
        context.startService(intent)
    }

    fun triggerRestTimer(setLog: SetLog, customDuration: Int? = null, warmupRest: Int = 60, dropRest: Int = 45, workRest: Int = 120) {
        val duration = customDuration ?: when (setLog.type) {
            SetType.WARMUP -> warmupRest
            SetType.DROP -> dropRest
            else -> workRest
        }

        if (duration > 0) {
            _state.update { it.copy(
                isResting = true,
                restTimerTotalSeconds = duration,
                restTimeRemaining = duration,
                lastCompletedSetId = setLog.id
            ) }
            WorkoutTimerService.start(context, duration)
        }
    }

    fun setClusterIndex(index: Int?) {
        _state.update { it.copy(currentClusterIndex = index) }
    }

    fun setWorkoutPhase(phase: RestPausePhase) {
        _state.update { it.copy(workoutPhase = phase) }
    }

    fun setShowCyberFinisher(show: Boolean) {
        _state.update { it.copy(showCyberFinisher = show) }
    }

    fun setShowLoadedStretch(show: Boolean) {
        _state.update { it.copy(showLoadedStretch = show) }
    }

    fun startStretchTimer(seconds: Int = 30) {
        stretchTimerJob?.cancel()
        _state.update { it.copy(showLoadedStretch = true, stretchTimeRemaining = seconds) }
        stretchTimerJob = scope.launch {
            var remaining = seconds
            while (remaining > 0) {
                delay(1000)
                remaining--
                _state.update { it.copy(stretchTimeRemaining = remaining) }
            }
            hapticService.clusterTimerBuzz()
            _state.update { it.copy(showLoadedStretch = false, stretchTimeRemaining = 0) }
        }
    }

    fun setShowUncompletedSetsDialog(show: Boolean) {
        _state.update { it.copy(showUncompletedSetsDialog = show) }
    }

    fun setActiveSessionError(error: String?) {
        _state.update { it.copy(activeSessionError = error) }
    }

    fun resetSession() {
        stopDurationTimer()
        stretchTimerJob?.cancel()
        stopRestTimer()
        _state.update { ActiveSessionState() }
    }
}
