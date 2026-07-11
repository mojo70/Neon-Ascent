package com.neon.ascent.feature.workout.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neon.ascent.core.domain.repository.WorkoutRepository
import com.neon.ascent.core.domain.workout.models.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject

data class ProgressUiState(
    val volumeHistory: List<VolumePoint> = emptyList(),
    val totalVolume: Int = 0,
    val streakDays: Int = 0,
    val prs: List<PersonalRecord> = emptyList(),
    val tdee: Int = 0,
    val proteinGrams: Int = 0,
    val carbGrams: Int = 0,
    val fatGrams: Int = 0,
    val isLoading: Boolean = true,
    val profile: UserWorkoutProfile? = null
)

data class VolumePoint(
    val date: LocalDate,
    val volume: Double
)

data class PersonalRecord(
    val exerciseName: String,
    val value: String,
    val date: LocalDate,
    val isCluster: Boolean = false
)

@HiltViewModel
class WorkoutProgressViewModel @Inject constructor(
    private val repository: WorkoutRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProgressUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadProgressData()
    }

    private fun loadProgressData() {
        viewModelScope.launch {
            combine(
                repository.getFullHistory(),
                repository.getUserProfile("default_user") // Match the ID used in WorkoutViewModel
            ) { history, profile ->
                calculateStats(history, profile)
            }.collect { stats ->
                _uiState.update { stats }
            }
        }
    }

    private fun calculateStats(
        history: List<Pair<WorkoutSession, List<Pair<WorkoutLog, List<SetLog>>>>>,
        profile: UserWorkoutProfile?
    ): ProgressUiState {
        if (history.isEmpty()) return ProgressUiState(isLoading = false, profile = profile)

        // 1. Volume History
        val volumePoints = history.map { (session, logs) ->
            val date = session.date.atZone(ZoneId.systemDefault()).toLocalDate()
            val volume = logs.sumOf { (_, sets) ->
                sets.filter { it.isCompleted }.sumOf { (it.weight * it.reps).toDouble() }
            }
            VolumePoint(date, volume)
        }.sortedBy { it.date }

        val totalVolume = volumePoints.sumOf { it.volume }.toInt()

        // 2. Streak
        val streak = calculateStreak(history.map { it.first.date })

        // 3. PRs (Last 10 unique exercise PRs)
        val prs = calculatePRs(history)

        // 4. Macros (Mifflin-St Jeor)
        val macros = if (profile != null) calculateMacros(profile) else Triple(0, 0, 0)
        val tdee = if (profile != null) calculateTDEE(profile) else 0

        return ProgressUiState(
            volumeHistory = volumePoints,
            totalVolume = totalVolume,
            streakDays = streak,
            prs = prs,
            tdee = tdee,
            proteinGrams = macros.first,
            carbGrams = macros.second,
            fatGrams = macros.third,
            isLoading = false,
            profile = profile
        )
    }

    private fun calculateStreak(dates: List<Instant>): Int {
        if (dates.isEmpty()) return 0
        val sortedDates = dates.map { it.atZone(ZoneId.systemDefault()).toLocalDate() }
            .distinct()
            .sortedDescending()
        
        var currentStreak = 0
        var today = LocalDate.now()
        
        // Check if latest workout was today or yesterday
        if (sortedDates.first() != today && sortedDates.first() != today.minusDays(1)) {
            return 0
        }

        var checkDate = sortedDates.first()
        for (date in sortedDates) {
            if (date == checkDate) {
                currentStreak++
                checkDate = checkDate.minusDays(1)
            } else {
                break
            }
        }
        return currentStreak
    }

    private fun calculatePRs(history: List<Pair<WorkoutSession, List<Pair<WorkoutLog, List<SetLog>>>>>): List<PersonalRecord> {
        val exercisePrs = mutableMapOf<String, PersonalRecord>()

        history.forEach { (session, logs) ->
            logs.forEach { (log, sets) ->
                val completedSets = sets.filter { it.isCompleted }
                if (completedSets.isEmpty()) return@forEach

                val date = session.date.atZone(ZoneId.systemDefault()).toLocalDate()
                
                // Handle Cluster PR (Sum of RP sets)
                val clusterSets = completedSets.filter { it.type == SetType.REST_PAUSE && it.clusterMiniSetIndex != null }
                if (clusterSets.isNotEmpty()) {
                    val totalReps = clusterSets.sumOf { it.reps }
                    val weight = clusterSets.first().weight
                    val existing = exercisePrs[log.exerciseId]
                    
                    if (existing == null || (!existing.isCluster && weight >= 0) || (existing.isCluster && totalReps > existing.value.toInt())) {
                        exercisePrs[log.exerciseId] = PersonalRecord(
                            exerciseName = log.exerciseName,
                            value = "$totalReps reps @ ${weight.toInt()} lbs",
                            date = date,
                            isCluster = true
                        )
                    }
                } else {
                    // Handle Standard PR (Max Weight)
                    val maxWeightSet = completedSets.maxByOrNull { it.weight }!!
                    val existing = exercisePrs[log.exerciseId]
                    
                    if (existing == null || (!existing.isCluster && maxWeightSet.weight > existing.value.split(" ")[0].toFloat())) {
                        exercisePrs[log.exerciseId] = PersonalRecord(
                            exerciseName = log.exerciseName,
                            value = "${maxWeightSet.weight.toInt()} lbs x ${maxWeightSet.reps}",
                            date = date
                        )
                    }
                }
            }
        }

        return exercisePrs.values.sortedByDescending { it.date }.take(10)
    }

    private fun calculateTDEE(profile: UserWorkoutProfile): Int {
        // Mifflin-St Jeor Equation
        val bmr = (10 * profile.weightKg) + (6.25 * profile.heightCm) - (5 * profile.age) + 
            if (profile.gender == Gender.MALE) 5 else -161
        
        return (bmr * profile.activityFactor).toInt()
    }

    private fun calculateMacros(profile: UserWorkoutProfile): Triple<Int, Int, Int> {
        val tdee = calculateTDEE(profile)
        
        // Somatotype-adjusted splits
        val proteinPerKg = when (profile.somatotype) {
            Somatotype.ENDOMORPH -> 2.2f
            else -> 2.0f
        }
        
        val proteinGrams = (profile.weightKg * proteinPerKg).toInt()
        val fatPercentage = when (profile.somatotype) {
            Somatotype.ECTOMORPH -> 0.20f
            Somatotype.ENDOMORPH -> 0.30f
            else -> 0.25f
        }
        
        val fatGrams = ((tdee * fatPercentage) / 9).toInt()
        val carbGrams = ((tdee - (proteinGrams * 4) - (fatGrams * 9)) / 4).toInt()
        
        return Triple(proteinGrams, carbGrams, fatGrams)
    }
}
