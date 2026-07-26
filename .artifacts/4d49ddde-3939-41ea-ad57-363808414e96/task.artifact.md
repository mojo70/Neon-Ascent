# Rest Timer UI & RP Consistency Fixes

- [x] **Service Enhancements**
    - [x] Add immediate tick and set package in `WorkoutTimerService.kt`
- [x] **ViewModel Logic Unification**
    - [x] Update `triggerRestTimer` for immediate `isResting` state
    - [x] Connect RP logic to `WorkoutTimerService`
    - [x] Remove old internal timer loop
    - [x] Fix `expandToCluster` state race condition
- [x] **UI Refinement**
    - [x] Ensure timer overlays are z-indexed correctly in `WorkoutLoggingScreen.kt`
    - [x] Update `InlineRestTimer` matching logic for clusters
- [x] **Verification**
    - [x] Verify instant UI feedback on set completion
    - [x] Verify 3-round RP clusters in General workouts
    - [x] Verify background sync for all set types
