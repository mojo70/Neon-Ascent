# Implementation Plan - Rest Timer UI Fixes and Logic Unification

This plan fixes the visibility issues with the rest timer and unifies the timer logic across all set types to ensure consistency in notifications and UI state.

## User Review Required

> [!IMPORTANT]
> - All rest timers (Normal sets, Rest-Pause, CyberCrapp) will now use the same background service. This means you'll always see the countdown in your notifications and on-screen.
> - The timer will now appear **immediately** on screen when a set is completed, instead of waiting for the first second to tick.
> - Fixed a race condition that caused Rest-Pause clusters to sometimes show only one mini-set.

## Proposed Changes

### 1. Service Layer

#### [MODIFY] [WorkoutTimerService.kt](file:///Users/joeevans/StudioProjects/Neon-Ascent/feature/workout/src/main/java/com/neon/ascent/feature/workout/services/WorkoutTimerService.kt)
- Update `startTimer` to send an initial `ACTION_TIMER_TICK` broadcast immediately before starting the loop.
- Set the package name on all broadcasts to ensure they reach the receiver even with strict `RECEIVER_NOT_EXPORTED` flags.

### 2. ViewModel Layer

#### [MODIFY] [WorkoutViewModel.kt](file:///Users/joeevans/StudioProjects/Neon-Ascent/feature/workout/src/main/java/com/neon/ascent/feature/workout/ui/WorkoutViewModel.kt)
- **Unify Timer Logic**:
    - Remove the internal `timerJob` and `startRestTimer()` loop.
    - Update `handleRestPauseLogic` to call the new `triggerRestTimer(setLog, customDuration)` method.
- **Immediate State Update**:
    - Update `triggerRestTimer` to set `isResting = true` immediately in the state flow.
- **Robust Cluster Generation**:
    - Refactor `expandToCluster` to use the current `uiState.logs` snapshot instead of a repository fetch to avoid stale data issues.
    - Ensure it sets `lastCompletedSetId` correctly for RP sets so the inline timer appears.

### 3. UI Layer

#### [MODIFY] [WorkoutLoggingScreen.kt](file:///Users/joeevans/StudioProjects/Neon-Ascent/feature/workout/src/main/java/com/neon/ascent/feature/workout/ui/WorkoutLoggingScreen.kt)
- **Timer Placement**: Ensure `RestTimerPopup` and `StickyBottomTimer` are at the very end of the top-level `Box` to guarantee they stay above all other content.
- **Inline Timer Logic**: Update the `currentSetId` logic to handle cluster sets better (showing the timer after each mini-set completion).

## Verification Plan

### Manual Verification
1. **Visibility**: Complete a Normal set. Verify the Popup and Sticky Bottom appear **instantly**.
2. **Inline Display**: Complete a set and scroll. Verify the blue progress bar is visible in the list at the correct position.
3. **RP Consistency**: Start an RP set in a General workout. Verify that all 3 mini-sets (M-1, M-2, M-3) are created and visible in the logging dialog.
4. **RP Timer**: Complete M-1 of an RP cluster. Verify the 15s timer starts and is visible both in the dialog and in system notifications.
