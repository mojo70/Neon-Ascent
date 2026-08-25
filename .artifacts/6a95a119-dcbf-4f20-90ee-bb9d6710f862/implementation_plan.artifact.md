# Fuel History (P3/P4) Implementation Plan

Implement Codex fuel history tracking to visualize changes in bodyweight, TDEE, and macro targets over time.

## User Review Required
> [!IMPORTANT]
> - Snapshots are automatically triggered when `UserWorkoutProfile` fields (`weightKg`, `activityFactor`, `somatotype`, or `gender`) change.
> - A "CURRENT_BASELINE" snapshot will be backfilled if no snapshots exist when the Codex screen is first opened.
> - Live targets remain in the LABS/Workout progress area; Codex strictly shows historical snapshots.

## Proposed Changes

### Core Domain (:core:domain)
#### [MODIFY] [WorkoutModels.kt](file:///Users/joeevans/StudioProjects/Neon-Ascent/core/domain/src/main/java/com/neon/ascent/core/domain/workout/models/WorkoutModels.kt)
- Add `FuelSnapshot` data class with fields: `timestamp`, `weightKg`, `tdee`, `protein`, `carb`, `fat`, `activityFactor`, `somatotype`.

#### [MODIFY] [WorkoutRepository.kt](file:///Users/joeevans/StudioProjects/Neon-Ascent/core/domain/src/main/java/com/neon/ascent/core/domain/repository/WorkoutRepository.kt)
- Add `getFuelHistory(from: Instant, to: Instant): Flow<List<FuelSnapshot>>`.
- Add `saveFuelSnapshot(snapshot: FuelSnapshot)`.

---

### Core Data (:core:data)
#### [MODIFY] [WorkoutEntities.kt](file:///Users/joeevans/StudioProjects/Neon-Ascent/core/data/src/main/java/com/neon/ascent/core/data/local/entity/WorkoutEntities.kt)
- Add `FuelSnapshotEntity` Room entity.

#### [MODIFY] [WorkoutDao.kt](file:///Users/joeevans/StudioProjects/Neon-Ascent/core/data/src/main/java/com/neon/ascent/core/data/local/dao/WorkoutDao.kt)
- Add DAO methods for `FuelSnapshotEntity`.

#### [MODIFY] [WorkoutRepositoryImpl.kt](file:///Users/joeevans/StudioProjects/Neon-Ascent/core/data/repository/WorkoutRepositoryImpl.kt)
- Implement `getFuelHistory` and `saveFuelSnapshot`.
- Update `saveUserProfile` to detect relevant changes and trigger `saveFuelSnapshot`.

---

### Feature Codex (:feature:codex)
#### [MODIFY] [CodexViewModel.kt](file:///Users/joeevans/StudioProjects/Neon-Ascent/feature/codex/src/main/java/com/neon/ascent/feature/codex/ui/CodexViewModel.kt)
- Add `fuelHistory` to `CodexUiState`.
- Implement `loadFuelHistory` with backfill logic for "CURRENT_BASELINE".
- Integrate with the selected period.

#### [MODIFY] [CodexScreen.kt](file:///Users/joeevans/StudioProjects/Neon-Ascent/feature/codex/src/main/java/com/neon/ascent/feature/codex/ui/CodexScreen.kt)
- Add "FUEL" subtab to VITALS wing.
- Implement charts for:
    - Bodyweight (Line chart).
    - TDEE / Protein targets (Snapshots).
    - Bodyweight vs. Lift weight overlay (Optional).

## Verification Plan

### Automated Tests
- Unit test for `MacroCalculator` (if modified/extracted).
- Unit test for `WorkoutRepositoryImpl` profile change detection.

### Manual Verification
1. Change weight or activity factor in Workout Settings.
2. Open Codex -> VITALS -> FUEL.
3. Verify the chart shows the baseline and the new snapshot.
4. Verify "CURRENT_BASELINE" is created on first open.
