# Implementation Plan - CyberCrapp Protocol Completion & Progression Guardrails

This plan completes the "Success Guardrails" for the CyberCrapp protocol as defined in `CCProgram.md`. It focuses on logging lengthened partials/stretches and implementing automated exercise rotation when progress stalls.

## User Review Required

> [!IMPORTANT]
> - **Automated Rotation**: If you fail to beat your previous best (weight or reps) for two consecutive sessions on a CyberCrapp exercise, the app will now proactively suggest a rotation to a similar movement.
> - **Finisher Logging**: Lengthened partials and loaded stretches will now be saved as actual `SetLog` entries in your history, allowing you to track progress on these intensity techniques over time.

## Proposed Changes

### 🧪 Neural Brain (Intelligence)

#### [MODIFY] [WorkoutViewModel.kt](file:///Users/joeevans/StudioProjects/Neon-Ascent/feature/workout/src/main/java/com/neon/ascent/feature/workout/ui/WorkoutViewModel.kt)
- **Log Finishers**:
    - Update `startStretch(partialReps: Int)`:
        - Creates and saves a `SetLog` for lengthened partials (`isLengthenedPartial = true`).
    - Update `startStretchTimer` completion:
        - Creates and saves a `SetLog` for the loaded stretch (`isLoadedStretch = true`).
- **Stagnation Detection**:
    - In `updateProgressionForExercise`, if `misses >= 2`, update UI state with `stagnantExerciseId`.
    - Function `forceRotateStagnant()`: Triggers the substitution flow for the stalled exercise.

### 🎨 Neon HUD (UI)

#### [MODIFY] [WorkoutLoggingScreen.kt](file:///Users/joeevans/StudioProjects/Neon-Ascent/feature/workout/src/main/java/com/neon/ascent/feature/workout/ui/WorkoutLoggingScreen.kt)
- **CyberFinisherDialog**:
    - Add a `WheelPicker` or `RepCounter` for the 3-5 lengthened partials.
    - Pass the selected reps to `viewModel.startStretch(reps)`.
- **LoadedStretchDialog**:
    - Add rhythmic "BREATHE IN / BREATHE OUT" text synchronized with the timer.
- **Stagnation Nudge**:
    - Add a "STAGNATION DETECTED" card to the dashboard if an exercise has stalled.
    - Action: "ROTATE MISSION" which opens the substitution dialog.

## Verification Plan

### Manual Verification
1. **Partials Logging**: Finish the 3rd mini-set of a CC exercise. In the Finisher dialog, select 4 reps. Verify that after the workout, a set with `isLengthenedPartial=true` and `reps=4` is in the log.
2. **Stretch Logging**: Complete the loaded stretch timer. Verify a set with `isLoadedStretch=true` and the correct duration is saved.
3. **Auto-Rotation**:
    - Log a workout where you fail to beat the previous best.
    - Log a second workout for the same exercise, failing again.
    - Verify the "STAGNATION DETECTED" nudge appears on the dashboard or exercise card.
    - Click "ROTATE" and verify recommendations appear.
