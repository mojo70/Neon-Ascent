package com.neon.ascent.feature.workout.ui.arsenal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neon.ascent.core.domain.repository.WorkoutRepository
import com.neon.ascent.core.domain.workout.models.Exercise
import com.neon.ascent.core.domain.workout.models.Implement
import com.neon.ascent.core.domain.workout.models.RoutineExercise
import com.neon.ascent.core.domain.workout.models.WorkoutRoutine
import com.neon.ascent.core.domain.workout.rules.ExerciseRanking
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SlotCategory(
    val id: String,
    val title: String,
    val description: String,
    val familyNames: List<String>,
    val defaultRange: String
)

data class FamilyCardData(
    val familyId: String,
    val familyName: String,
    val slotId: String,
    val rangeText: String,
    val tags: List<String>,
    val implements: List<Implement>,
    val variants: List<Exercise>
)

data class AccomplishmentSummary(
    val bestClusterWeight: Float,
    val bestClusterReps: Int
)

data class ArsenalUiState(
    val searchQuery: String = "",
    val slots: List<SlotCategory> = emptyList(),
    val selectedSlot: SlotCategory? = null,
    val selectedFamily: FamilyCardData? = null,
    val selectedImplement: String = "ALL",
    val selectedStance: String = "ALL",
    val highlightExerciseId: String? = null,
    val familyCards: List<FamilyCardData> = emptyList(),
    val filteredVariants: List<Exercise> = emptyList(),
    val availableImplements: List<String> = emptyList(),
    val availableStances: List<String> = emptyList(),
    val accomplishmentsMap: Map<String, AccomplishmentSummary> = emptyMap(),
    val userRoutines: List<WorkoutRoutine> = emptyList(),
    val showAddToTemplateDialogFor: Exercise? = null,
    val showCustomCreationType: CustomCreationType? = null,
    val substituteSuccessMessage: String? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class ArsenalViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ArsenalUiState())
    val uiState: StateFlow<ArsenalUiState> = _uiState.asStateFlow()

    val allSlots = listOf(
        SlotCategory("horizontal_press", "Horizontal Press", "Chest & anterior delts press variations", listOf("Bench Press", "Push-Up", "Dip"), "6–10 reps"),
        SlotCategory("vertical_press", "Vertical Press", "Overhead press & landmine variations", listOf("Overhead Press", "Landmine Press"), "6–10 reps"),
        SlotCategory("vertical_pull", "Vertical Pull", "Lat pulldown & vertical pull variations", listOf("Pull-Up", "Chin-Up", "Lat Pulldown"), "11–20 reps"),
        SlotCategory("horizontal_pull", "Horizontal Pull", "Rows & mid-back thickness variations", listOf("Row", "Chest-Supported Row", "Face Pull"), "10–15 reps"),
        SlotCategory("quad_squat", "Quad / Squat", "Squat, leg press & lunge variations", listOf("Squat", "Leg Press", "Hack Squat", "Belt Squat", "Split Squat", "Lunge"), "6–10 reps (or 20-rep widowmaker)"),
        SlotCategory("hinge", "Hinge", "Deadlift & posterior chain hinge variations", listOf("Deadlift", "Romanian Deadlift", "Good Morning", "Back Extension", "KB Swing"), "6–10 reps"),
        SlotCategory("elbow_flex", "Elbow Flex (Biceps)", "Bicep curl variations", listOf("Curl", "Jerry Curl", "Hammer Curl", "Preacher Curl"), "11–15 reps"),
        SlotCategory("elbow_extend", "Elbow Extend (Triceps)", "Tricep extension & dip variations", listOf("Tricep Extension", "Pushdown", "Skull Crusher", "Close-Grip Press", "Dip"), "11–15 reps"),
        SlotCategory("delts_isolation", "Delts Isolation", "Lateral & rear delt isolation", listOf("Lateral Raise", "Rear Delt Fly"), "12–20 reps"),
        SlotCategory("posterior_isolation", "Posterior Isolation", "Hamstring & glute isolation", listOf("Leg Curl", "Hip Thrust", "Nordic / GHR"), "10–15 reps"),
        SlotCategory("calves", "Calves", "Calf isolation variations", listOf("Calf Raise"), "12–20 reps"),
        SlotCategory("abs", "Abs", "Core & abdominal flexion", listOf("Cable Crunch", "Hanging Raise", "Ab Wheel"), "12–25 reps")
    )

    private var allExercises: List<Exercise> = emptyList()

    init {
        viewModelScope.launch {
            combine(
                workoutRepository.getExerciseDefinitions(),
                workoutRepository.getAllAccomplishments(),
                workoutRepository.getAllRoutines()
            ) { exercises, accomplishments, routines ->
                allExercises = exercises
                val accMap = accomplishments
                    .filter { it.bestClusterWeight > 0f && it.bestClusterReps > 0 }
                    .associate { it.exerciseId to AccomplishmentSummary(it.bestClusterWeight, it.bestClusterReps) }

                _uiState.update { it.copy(
                    accomplishmentsMap = accMap,
                    userRoutines = routines
                ) }
                updateUiState()
            }.collect()
        }
    }

    fun initDeepLink(initialSlotId: String?, initialFamilyId: String?, highlightExerciseId: String?) {
        if (!highlightExerciseId.isNullOrBlank()) {
            _uiState.update { it.copy(highlightExerciseId = highlightExerciseId) }
            val highlightedEx = allExercises.find { it.id == highlightExerciseId }
            if (highlightedEx != null) {
                val slot = allSlots.find { s -> s.familyNames.any { f -> f.equals(highlightedEx.familyName, ignoreCase = true) } }
                if (slot != null) {
                    selectSlot(slot)
                    val familyCards = buildFamilyCards(slot)
                    val family = familyCards.find { f -> f.familyName.equals(highlightedEx.familyName, ignoreCase = true) }
                    if (family != null) {
                        selectFamily(family)
                    }
                    return
                }
            }
        }

        if (!initialSlotId.isNullOrBlank()) {
            val slot = allSlots.find { 
                it.id.equals(initialSlotId, ignoreCase = true) || 
                it.title.equals(initialSlotId, ignoreCase = true) 
            }
            if (slot != null) {
                selectSlot(slot)
            }
        }

        if (!initialFamilyId.isNullOrBlank() && _uiState.value.selectedSlot != null) {
            val familyCards = buildFamilyCards(_uiState.value.selectedSlot!!)
            val family = familyCards.find { it.familyId.equals(initialFamilyId, ignoreCase = true) || it.familyName.equals(initialFamilyId, ignoreCase = true) }
            if (family != null) {
                selectFamily(family)
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        updateUiState()
    }

    fun selectSlot(slot: SlotCategory?) {
        _uiState.update { 
            it.copy(
                selectedSlot = slot,
                selectedFamily = null,
                selectedImplement = "ALL",
                selectedStance = "ALL"
            ) 
        }
        updateUiState()
    }

    fun selectFamily(family: FamilyCardData?) {
        _uiState.update { 
            it.copy(
                selectedFamily = family,
                selectedImplement = "ALL",
                selectedStance = "ALL"
            ) 
        }
        updateUiState()
    }

    fun selectImplement(implement: String) {
        _uiState.update { it.copy(selectedImplement = implement, selectedStance = "ALL") }
        updateUiState()
    }

    fun selectStance(stance: String) {
        _uiState.update { it.copy(selectedStance = stance) }
        updateUiState()
    }

    fun useAsSubstitute(newExercise: Exercise, onSubstituted: ((Exercise) -> Unit)? = null) {
        onSubstituted?.invoke(newExercise)
        _uiState.update { it.copy(substituteSuccessMessage = "SUBSTITUTED // ${newExercise.name.uppercase()}") }
    }

    fun openAddToTemplateDialog(exercise: Exercise) {
        _uiState.update { it.copy(showAddToTemplateDialogFor = exercise) }
    }

    fun dismissAddToTemplateDialog() {
        _uiState.update { it.copy(showAddToTemplateDialogFor = null) }
    }

    fun addExerciseToTemplate(exercise: Exercise, routine: WorkoutRoutine) {
        viewModelScope.launch {
            val updatedExercises = routine.exercises + RoutineExercise(exercise = exercise, sets = emptyList())
            workoutRepository.saveRoutine(routine.copy(exercises = updatedExercises))
            _uiState.update { it.copy(
                showAddToTemplateDialogFor = null,
                substituteSuccessMessage = "ADDED TO TEMPLATE // ${routine.name.uppercase()}"
            ) }
        }
    }

    fun openCustomCreationDialog(creationType: CustomCreationType) {
        _uiState.update { it.copy(showCustomCreationType = creationType) }
    }

    fun dismissCustomCreationDialog() {
        _uiState.update { it.copy(showCustomCreationType = null) }
    }

    fun saveCustomExercise(exercise: Exercise) {
        viewModelScope.launch {
            workoutRepository.saveExerciseDefinition(exercise)
            val currentSlot = _uiState.value.selectedSlot
            if (currentSlot != null && exercise.isPrimaryVariant) {
                val updatedSlot = currentSlot.copy(
                    familyNames = (currentSlot.familyNames + exercise.familyName).distinct()
                )
                _uiState.update { state ->
                    state.copy(
                        selectedSlot = updatedSlot,
                        showCustomCreationType = null,
                        substituteSuccessMessage = "CUSTOM FAMILY CREATED // ${exercise.familyName.uppercase()}"
                    )
                }
            } else {
                _uiState.update { state ->
                    state.copy(
                        showCustomCreationType = null,
                        substituteSuccessMessage = "CUSTOM VARIANT CREATED // ${exercise.name.uppercase()}"
                    )
                }
            }
        }
    }

    fun deleteCustomExercise(exerciseId: String) {
        viewModelScope.launch {
            workoutRepository.deleteCustomExercise(exerciseId)
            _uiState.update { it.copy(substituteSuccessMessage = "CUSTOM EXERCISE REMOVED") }
        }
    }

    fun clearSuccessMessage() {
        _uiState.update { it.copy(substituteSuccessMessage = null) }
    }

    private fun buildFamilyCards(slot: SlotCategory): List<FamilyCardData> {
        val customFamiliesInSlot = allExercises
            .filter { it.isCustom && it.muscleGroups.contains(slot.title) }
            .map { it.familyName }
            .distinct()

        val allFamilyNames = (slot.familyNames + customFamiliesInSlot).distinct()

        return allFamilyNames.map { familyName ->
            val matchingExercises = allExercises.filter { 
                it.familyName.equals(familyName, ignoreCase = true) || 
                it.name.contains(familyName, ignoreCase = true) 
            }
            val aggregatedTags = matchingExercises.flatMap { it.effectiveTags() }.distinct().take(3)
            val implements = matchingExercises.map { it.implement }.distinct()

            FamilyCardData(
                familyId = familyName.lowercase().replace(" ", "_"),
                familyName = familyName,
                slotId = slot.id,
                rangeText = "Protocol range: ${slot.defaultRange}",
                tags = aggregatedTags,
                implements = implements,
                variants = matchingExercises
            )
        }
    }

    private fun updateUiState() {
        val query = _uiState.value.searchQuery.trim().lowercase()
        val filteredSlots = if (query.isEmpty()) {
            allSlots
        } else {
            allSlots.filter { slot ->
                slot.title.lowercase().contains(query) ||
                slot.description.lowercase().contains(query) ||
                slot.familyNames.any { it.lowercase().contains(query) }
            }
        }

        val selectedSlot = _uiState.value.selectedSlot
        val familyCards = if (selectedSlot != null) {
            buildFamilyCards(selectedSlot)
        } else {
            emptyList()
        }

        val selectedFamily = _uiState.value.selectedFamily
        var filteredVariants = emptyList<Exercise>()
        var availableImplements = emptyList<String>()
        var availableStances = emptyList<String>()

        if (selectedFamily != null) {
            val variantsInFamily = selectedFamily.variants
            
            availableImplements = listOf("ALL") + variantsInFamily.map { it.implement.name }.distinct()
            
            val selImp = _uiState.value.selectedImplement
            val impFiltered = if (selImp == "ALL") {
                variantsInFamily
            } else {
                variantsInFamily.filter { it.implement.name.equals(selImp, ignoreCase = true) }
            }

            val stancesInImp = impFiltered.map { it.stance.name }.distinct()
            availableStances = if (stancesInImp.size > 1) {
                listOf("ALL") + stancesInImp
            } else {
                emptyList()
            }

            val selStance = _uiState.value.selectedStance
            val stanceFiltered = if (selStance == "ALL" || availableStances.isEmpty()) {
                impFiltered
            } else {
                impFiltered.filter { it.stance.name.equals(selStance, ignoreCase = true) }
            }

            val highlightId = _uiState.value.highlightExerciseId
            val highlightEx = allExercises.find { it.id == highlightId }
            filteredVariants = if (highlightEx != null) {
                ExerciseRanking.rankAlternatives(target = highlightEx, candidates = stanceFiltered)
            } else {
                stanceFiltered
            }
        }

        _uiState.update {
            it.copy(
                slots = filteredSlots,
                familyCards = familyCards,
                filteredVariants = filteredVariants,
                availableImplements = availableImplements,
                availableStances = availableStances,
                isLoading = false
            )
        }
    }
}
