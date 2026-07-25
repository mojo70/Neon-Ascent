# Generalize Rest-Pause Timer Logic

This plan generalizes the high-intensity rest-pause timer and phase management from the CyberCrapp-specific implementation to the `REST_PAUSE` set type, making it available across all training protocols.

## User Review Required

> [!IMPORTANT]
> The rest-pause mini-set transitions (15s timer between sets 1, 2, and 3) will now trigger automatically whenever a `REST_PAUSE` set is logged, regardless of whether the protocol is "CyberCrapp" or "General".

## Proposed Changes

### ViewModel & Logic

#### [MODIFY] [WorkoutViewModel.kt](file:///Users/joeevans/StudioProjects/Neon-Ascent/feature/workout/src/main/java/com/neon/ascent/feature/workout/ui/WorkoutViewModel.kt)
- Rename `handleCyberCrappLogic` to `handleRestPauseLogic`.
- Remove protocol checks (`session.protocol == WorkoutProtocol.CYBER_CRAPP`) when triggering rest-pause logic in `logSet` and `updateSet`.
- Ensure `clusterMiniSetIndex` is correctly assigned for any `REST_PAUSE` set type.
- Keep the `showCyberFinisher` and `LOADED_STRETCH` transitions, but ensure they don't break the UI if used in a protocol that doesn't expect them (or keep them as part of the "Rest Pause" experience).

#### [MODIFY] [WorkoutModels.kt](file:///Users/joeevans/StudioProjects/Neon-Ascent/core/domain/src/main/java/com/neon/ascent/core/domain/workout/models/WorkoutModels.kt)
- Rename `CyberCrappPhase` to `RestPausePhase` for better architectural alignment.

### UI Layer

#### [MODIFY] [WorkoutLoggingScreen.kt](file:///Users/joeevans/StudioProjects/Neon-Ascent/feature/workout/src/main/java/com/neon/ascent/feature/workout/ui/WorkoutLoggingScreen.kt)
- Update references to the renamed phase enum.
- Ensure the rest-pause timer display remains consistent.

## Verification Plan

### Manual Verification
1. Start a **General** (non-CyberCrapp) workout.
2. Add an exercise or change a set type to `REST_PAUSE`.
3. Log the first mini-set.
4. Verify that the 15-second rest timer starts automatically.
5. Verify that it progresses through mini-sets 2 and 3.
6. Verify the same behavior still works in a **CyberCrapp** session.
