# Blast Tracker, Injury Logic & Biohacking Macros

This plan aligns the app with the high-fidelity requirements of the CyberCrapp protocol by adding intensity block tracking (Blast Week), automated injury-aware substitutions with an expanded safe-exercise library, and nutrition targets in the Biohacking hub.

## User Review Required

> [!IMPORTANT]
> **Blast Counter**: A "Blast Week X" indicator will now appear on your dashboard. Completing a deload session will reset this counter to 0. Returning to a full-intensity protocol will start a new Blast.

> [!CAUTION]
> **Injury Detection & Safe Library**: If your profile lists injuries (e.g., "Shoulder Pain"), the app will proactively warn you if a routine exercise is contraindicated. We are adding 12+ new "Stability-First" exercises (Hammer Strength, Smith Machine, Chest Supported) to serve as safe alternatives.

## Proposed Changes

### 📡 Core Data & Bio-Calculators

#### [MODIFY] [WorkoutModels.kt](file:///Users/joeevans/StudioProjects/Neon-Ascent/core/domain/src/main/java/com/neon/ascent/core/domain/workout/models/WorkoutModels.kt)
- Add `lastBlastStartDate: Instant?` to `UserWorkoutProfile`.
- Add `dangerousFor: List<String>` to `Exercise`.
- [NEW] `MacroCalculator`: Central logic for TDEE and somatotype-adjusted protein/carb/fat targets.

#### [MODIFY] [WorkoutEntities.kt](file:///Users/joeevans/StudioProjects/Neon-Ascent/core/data/src/main/java/com/neon/ascent/core/data/local/entity/WorkoutEntities.kt)
- Update `UserWorkoutProfileEntity` with `lastBlastStartDate`.
- Update `ExerciseDefinitionEntity` with `dangerousFor`.
- **Database Version 41**.

#### [MODIFY] [WorkoutRepositoryImpl.kt](file:///Users/joeevans/StudioProjects/Neon-Ascent/core/data/src/main/java/com/neon/ascent/core/data/repository/WorkoutRepositoryImpl.kt)
- **Seed Expansion**: Add 12+ new exercises focused on stability and safety:
    - *Chest (Shoulder Safe)*: Incline Smith Press, Hammer Strength Press, Floor Press.
    - *Shoulders (Cuff Safe)*: Seated Smith Overhead Press, Hammer Strength Shoulder Press.
    - *Back (Spine Safe)*: Chest-Supported T-Bar Row, Rack Pulls (Below Knee), Trap Bar Deadlift.
    - *Quads (Knee Safe)*: Hack Squat Machine, Belt Squat, Pendulum Squat.
    - *Triceps (Elbow Safe)*: Cable Pushdowns, Close Grip Smith Press.

### 🧪 Neural Brain (Intelligence)

#### [MODIFY] [WorkoutViewModel.kt](file:///Users/joeevans/StudioProjects/Neon-Ascent/feature/workout/src/main/java/com/neon/ascent/feature/workout/ui/WorkoutViewModel.kt)
- **Blast Logic**:
    - If `session.isDeload == false` and `lastBlastStartDate == null`, set it to `now`.
    - If `session.isDeload == true`, set `lastBlastStartDate` to `null`.
- **Injury Guard**:
    - Function `checkForInjuries(routine)`: Compares `routine.exercises` against `profile.injuries`.
    - If match found (e.g., "Shoulder Pain" vs Exercise tagged for "Shoulder Pain"), update state to `showInjuryWarningDialog`.
    - `autoSubstituteInjuredExercises()`: Replaces high-risk movements with the new stable alternatives from the library.

#### [MODIFY] [BiohackingViewModel.kt](file:///Users/joeevans/StudioProjects/Neon-Ascent/app/src/main/java/com/neon/ascent/feature/biohacking/BiohackingViewModel.kt)
- Integrate `MacroCalculator` to expose nutrition targets to the Biohacking UI.

### 🎨 Neon HUD (UI)

#### [MODIFY] [WorkoutLoggingScreen.kt](file:///Users/joeevans/StudioProjects/Neon-Ascent/feature/workout/src/main/java/com/neon/ascent/feature/workout/ui/WorkoutLoggingScreen.kt)
- **Dashboard**: Add "BLAST WEEK X" to the summary bar or Next Mission card.
- **InjuryWarningDialog**: High-contrast warning modal with "Auto-Swap" action.

#### [MODIFY] [BiohackingScreen.kt](file:///Users/joeevans/StudioProjects/Neon-Ascent/app/src/main/java/com/neon/ascent/feature/biohacking/BiohackingScreen.kt)
- Add `MacrosCard` showing the "Biometric Nutrition Uplink" (Daily kcal, P/C/F).

## Verification Plan

### Manual Verification
1. Set "Shoulder Pain" in Profile.
2. Start *CyberCrapp A* (Bench Press) and verify the Injury Warning appears.
3. Tap "Auto-Swap" and verify Bench Press is replaced by a Dumbbell or Hammer Strength alternative.
4. Finish 3 full intensity workouts and verify "Blast Week 1" appears.
5. Finish a "Soft Deload" and verify the Blast counter resets.
6. Open Biohacking page and verify the Macros Card shows somatotype-adjusted numbers.
