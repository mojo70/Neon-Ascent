# Neon Ascent Troubleshooting & Common Fixes

This document serves as a reference for recurring issues and architectural patterns in the Neon Ascent project to optimize development and debugging.

## 1. Feature: Workout

### `WorkoutLoggingScreen.kt` & `WorkoutViewModel.kt`
*   **Issue**: `Unresolved reference 'name'` or `'id'` when iterating over exercise lists.
*   **Cause**: `WorkoutRoutine.exercises` and `WorkoutAugment.exercises` are `List<RoutineExercise>`, not `List<Exercise>`.
*   **Pattern**: `RoutineExercise` is a wrapper containing an `Exercise` object and its specific `sets`.
*   **Fix**: Always access properties through the nested `exercise` object.
    *   *Incorrect*: `routine.exercises.map { it.name }`
    *   *Correct*: `routine.exercises.map { it.exercise.name }`
    *   *Incorrect*: `routineExercise.id`
    *   *Correct*: `routineExercise.exercise.id`

## 2. Core: Data

### Room Database Schema Mismatch
*   **Issue**: App crashes on load with `java.lang.IllegalStateException: Room cannot verify the data integrity.`
*   **Cause**: Schema changes (entities added/modified, data types changed, DAO `@Relation` structural changes) without a corresponding database version bump.
*   **Primary Database**: `NeonAscentDatabase` located in `:core:data`.
*   **Fix**:
    1.  Increment the `version` number in `NeonAscentDatabase.kt`.
    2.  If the app still crashes with an identity hash mismatch, increment it **again**. Room only triggers destructive migration if the version on disk differs from the version in code. If you modified the schema but kept the same version number, Room won't wipe the old database automatically.
    3.  **Intermediate Builds**: If you made several changes and the app crashes, it's often easiest to just bump the version to trigger a clean state.
    4.  Common schema changes: adding/renaming fields, changing data types (e.g., `Int?` to `String?`), adding/removing entities, or changing `@Relation` definitions.
    5.  Ensure `fallbackToDestructiveMigration()` is enabled in `DatabaseModule.kt` (currently enabled).

### DAO Relationship Mapping
*   **Issue**: `The class must be either @Entity or @DatabaseView` in Room DAO/Relation classes.
*   **Cause**: Using a data class (POJO) in a `@Relation` without specifying the `entity` parameter. Room defaults to the return type class if `entity` is omitted.
*   **Fix**: If the `List<T>` type `T` is not an `@Entity`, you must specify the actual entity class to query.
    *   *Incorrect*:
        ```kotlin
        @Relation(parentColumn = "id", entityColumn = "routineId")
        val exercises: List<RoutineExerciseWithOrder>
        ```
    *   *Correct*:
        ```kotlin
        @Relation(entity = RoutineExerciseCrossRef::class, parentColumn = "id", entityColumn = "routineId")
        val exercises: List<RoutineExerciseWithOrder>
        ```
*   **Schema Impact**: Changing DAO relationship mappings or `@Relation` structure often counts as a schema change even if entities themselves haven't changed. **Bump the database version** to trigger migration/wipe.

## 3. Core: Domain

### Models
*   **WorkoutProtocol**: Enums include `GENERAL`, `CYBER_CRAPP`, `STRAIGHT_SETS`, `DUP`, `SUPERSETS`.
*   **SetType**: Enums include `NORMAL`, `WARMUP`, `DROP`, `FAILURE`, `REST_PAUSE`.

---
*Last Updated: 2024-05-22*
