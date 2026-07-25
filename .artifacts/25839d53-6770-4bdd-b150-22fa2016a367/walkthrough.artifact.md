# Fix: Database Cascade and Finish Crash

I have fixed the crash that occurred when finishing a workout and the associated data loss issue.

## Key Changes

### 1. Database Stability (Insert vs. Update)
- **Problem**: The app was using `OnConflictStrategy.REPLACE` for sessions, logs, and sets. When finishing a workout, updating the session duration would trigger a "replace" (Delete + Insert) of the session record. Because of the `CASCADE` delete constraint, this accidentally wiped all underlying logs and sets for that session.
- **Solution**: Changed the database logic to use explicit `@Update` and `@Insert(onConflict = ABORT)` methods. This ensures that updating parent records (like a session) is non-destructive to their children.

### 2. Surgical Session Updates
- **[WorkoutDao.kt](file:///Users/joeevans/StudioProjects/Neon-Ascent/core/data/src/main/java/com/neon/ascent/core/data/local/dao/WorkoutDao.kt)**: Added a specific `updateSessionDuration` query to update only the time spent in a workout, completely bypassing any risk of triggering relationship cascades.
- **[WorkoutViewModel.kt](file:///Users/joeevans/StudioProjects/Neon-Ascent/feature/workout/src/main/java/com/neon/ascent/feature/workout/ui/WorkoutViewModel.kt)**: Updated the `performFinalFinish` method to use this new surgical update instead of saving the entire session object.

### 3. Improved Repository Patterns
- **[WorkoutRepositoryImpl.kt](file:///Users/joeevans/StudioProjects/Neon-Ascent/core/data/src/main/java/com/neon/ascent/core/data/repository/WorkoutRepositoryImpl.kt)**: Implemented "check-before-insert" logic for logs and sets to support the new non-destructive conflict strategy.

## Verification Results

### Manual Testing
- [x] Started a workout and logged multiple sets.
- [x] Finished the workout with "Discard empty sets" and "Do not save routine".
- [x] **Result**: App finished cleanly without crashing.
- [x] Verified that logs and sets are still present in the database after the session ended.
- [x] Verified that the session duration was correctly updated in the session record.

> [!TIP]
> This architectural change makes the workout logging engine significantly more robust against data loss and crashes during critical lifecycle transitions.
