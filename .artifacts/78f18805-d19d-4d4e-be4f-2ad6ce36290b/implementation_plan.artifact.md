# Implementation Plan - Fix Rest Timer Options and Rest-Pause Cluster Timer Isolation

## User Review Required

> [!IMPORTANT]
> This plan addresses two issues:
> 1. **Workout Settings Timer Options**: Rest timer mode selection in `WorkoutSettingsScreen` was missing interactive click handling (it had a placeholder comment) and parameter pass-through for updating `restTimerMode`.
> 2. **Rest-Pause Cluster Timer Isolation**: Rest-pause cluster mini-set transitions should use their own timer flow / state and not mix up with main set rest timers or popup/inline options unexpectedly.

## Proposed Changes

### [Workout Feature]

#### [MODIFY] [WorkoutSettingsScreen.kt](file:///Users/joeevans/StudioProjects/Neon-Ascent/feature/workout/src/main/java/com/neon/ascent/feature/workout/ui/WorkoutSettingsScreen.kt)
- Add `onUpdateRestTimerMode: (RestTimerMode) -> Unit` parameter to `WorkoutSettingsScreen`.
- Wire up `.clickable { onUpdateRestTimerMode(mode) }` for rest timer mode selection.

#### [MODIFY] [WorkoutLoggingScreen.kt](file:///Users/joeevans/StudioProjects/Neon-Ascent/feature/workout/src/main/java/com/neon/ascent/feature/workout/ui/WorkoutLoggingScreen.kt)
- Pass `onUpdateRestTimerMode = { viewModel.updateRestTimerMode(it) }` to `WorkoutSettingsScreen`.

#### [MODIFY] [WorkoutViewModel.kt](file:///Users/joeevans/StudioProjects/Neon-Ascent/feature/workout/src/main/java/com/neon/ascent/feature/workout/ui/WorkoutViewModel.kt)
- Review and isolate rest-pause cluster timer logic so rest-pause mini-set transitions operate independently of main rest timer popups/inline settings if specified, or use dedicated cluster timer handling.

## Verification Plan

### Automated Tests
- Build test: `./gradlew :feature:workout:assembleDebug`

### Manual Verification
- Verify Workout Settings allows selecting POPUP, INLINE, or BOTH independently.
- Verify Rest-Pause cluster flows work with dedicated cluster timer behavior.
