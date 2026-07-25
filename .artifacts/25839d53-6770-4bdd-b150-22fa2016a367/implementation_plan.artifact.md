# Fix Data Persistence and Implement History Export

This plan addresses the data saving issues in the workout system and adds the requested JSON history export feature.

## User Review Required

> [!IMPORTANT]
> I will simplify the database saving logic by using Room's `@Upsert` functionality. This is more robust than the previous check-then-update logic and will prevent data from "failing to save" due to race conditions or stale state checks.

> [!TIP]
> The JSON export will be accessible from the Workout Progress screen, allowing you to download your full performance history including all sessions, exercises, and sets with timestamps.

## Proposed Changes

### Core Data Layer (DAO)

#### [MODIFY] [WorkoutDao.kt](file:///Users/joeevans/StudioProjects/Neon-Ascent/core/data/src/main/java/com/neon/ascent/core/data/local/dao/WorkoutDao.kt)
- Replace `@Insert` and `@Update` methods with `@Upsert` for `WorkoutSessionEntity`, `WorkoutLogEntity`, and `SetLogEntity`.
- Remove the inefficient `updateSessionDuration` and replace with a standard upsert for the session.

### Repository Layer

#### [MODIFY] [WorkoutRepositoryImpl.kt](file:///Users/joeevans/StudioProjects/Neon-Ascent/core/data/src/main/java/com/neon/ascent/core/data/repository/WorkoutRepositoryImpl.kt)
- Simplify `saveSession`, `saveWorkoutLog`, and `saveSetLog` to directly call the DAO `@Upsert` methods.
- Implement `exportHistoryToJson()`: Fetches the full history from `getAllSessionsWithDetails()` and serializes it to JSON using `Gson`.

### ViewModel Layer

#### [MODIFY] [WorkoutViewModel.kt](file:///Users/joeevans/StudioProjects/Neon-Ascent/feature/workout/src/main/java/com/neon/ascent/feature/workout/ui/WorkoutViewModel.kt)
- Update `performFinalFinish` to use `saveSession` instead of the specific duration update.
- Ensure progression state calculation happens *after* the session is marked as completed in the database.

#### [MODIFY] [WorkoutProgressViewModel.kt](file:///Users/joeevans/StudioProjects/Neon-Ascent/feature/workout/ui/WorkoutProgressViewModel.kt)
- Add `exportHistory()` method to trigger the JSON generation and provide it to the UI.

### UI Layer

#### [MODIFY] [WorkoutProgressScreen.kt](file:///Users/joeevans/StudioProjects/Neon-Ascent/feature/workout/ui/WorkoutProgressScreen.kt)
- Add an "Export History (JSON)" button in the header or overflow menu.

## Verification Plan

### Manual Verification
1. Start a workout, log a set with weight and reps, mark it as completed.
2. Click **Finish**.
3. Re-enter the same routine.
4. Verify the **"Previous"** column correctly shows the data you just logged.
5. Go to the **Progress** screen and click **"Export History"**.
6. Verify that a JSON string is generated containing your session data.
