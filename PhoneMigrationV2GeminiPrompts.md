# Neon Ascent – Phone Migration V2 Gemini / Android Studio Prompt Pack

**Companion to**: `artifacts/PhoneMigrationV2.md`  
**Updated**: October 22, 2026  
**Usage**: Paste one prompt at a time. After each, reassert the lock (R block) before continuing.

---

## PROMPT 0 — Kill-list & reassert

You are working in `mojo70/Neon-Ascent`.

**Do not**:
- Hardcode `scheduledDays = "[]"`, `rotationIndex = 0`, or other profile defaults on restore.
- Leave ProtocolCycle, routines, augments, progression, or rest-timer / layout-mode preferences out of the payload.
- Create a parallel restore path.
- Add a new tab or wing.
- Break v3 backup deserialization.

**Do**:
- Expand the workout and preferences coverage exactly as locked in `PhoneMigrationV2.md`.
- Keep one `restoreBackupJson` entry point.
- Preserve dual visual mode rules.

Re-read `artifacts/PhoneMigrationV2.md` and confirm the lock before writing code.

---

## PROMPT 1 — Expand models (payload v4)

In `FullBackupModels.kt`:

1. Bump `NeonAscentBackupPayload.version` default to 4.
2. Extend `UserWorkoutProfileDto` with every field that exists on `UserWorkoutProfileEntity` that is currently missing (at minimum: `scheduledDays`, `rotationIndex`, `deepLinkToRoutine`, `injuries`, `timePerSessionMinutes`, `age`, `heightCm`, `weightKg`, `customSequenceIds`, `lastBlastStartDateEpochMs`, and any others that are user-meaningful).
3. Add DTOs and include them in `WorkoutBackupSection` (or a new `WorkoutGraphBackupSection` that is always restored with workouts):
   - ProtocolCycleDto
   - ProgressionStateDto
   - ExerciseAccomplishmentDto
   - WorkoutRoutineDto + RoutineExerciseCrossRefDto + RoutineSetDto
   - WorkoutAugmentDto + AugmentExerciseCrossRefDto + AugmentSetDto + AugmentActivationDto
   - ProtocolRepTargetDto
4. Add a `PreferencesBackupSection` (or extend Settings) covering:
   - Rest timer keys from `HealthPreferencesDataStore` (default, work, warmup, drop, auto-start, mode)
   - Workout zoom
   - `workoutLayoutMode` and `useCustomNumpadInFocusMode` from `WorkoutPreferencesDataStore`
   - Codex period/wing/last exercise if cheap
5. All new fields optional with safe defaults so v3 JSON still parses.

---

## PROMPT 2 — Export the new data

In `FullDataBackupRepositoryImpl.exportBackupJson`:

1. Query and serialize the new workout graph entities when `includeWorkout` is true.
2. Map the full `UserWorkoutProfileEntity` (including `scheduledDays` and `rotationIndex`) into the expanded DTO. Do not drop fields.
3. Read the relevant keys from `HealthPreferencesDataStore` and `WorkoutPreferencesDataStore` and put them in the preferences section.
4. Keep existing sections unchanged.

---

## PROMPT 3 — Restore without hardcodes + correct order

In `restoreBackupJson`:

1. Remove every hardcoded profile value (`scheduledDays = "[]"`, `rotationIndex = 0`, default age/height/weight, etc.). Map from the DTO.
2. Restore in safe order:
   - Exercise definitions
   - Routines and augments
   - Cross-refs and sets
   - Sessions → logs → set logs
   - Protocol cycles
   - Progression states and accomplishments
   - Protocol rep targets
   - User profile (last)
3. Restore the preferences section into the two DataStores after the DB work.
4. Return accurate counts (sessions, sets, cycles) in `RestoreResult`.
5. v3 payloads must still succeed (missing new sections = no-op).

---

## PROMPT 4 — Tests

Extend or add tests that:

- Export a profile with non-empty `scheduledDays` and non-zero `rotationIndex`, restore, and assert equality.
- Export/restore at least one ProtocolCycle, one routine with exercises, and one augment activation.
- Export/restore rest-timer values and `WorkoutLayoutMode.FOCUS`.
- Confirm a v3-style payload (no new sections) still restores without crashing.

---

## PROMPT 5 — Smoke-test checklist (manual)

Add a short comment block or debug log summary after a successful restore that prints:

- sessions restored, sets restored, cycles restored
- scheduledDays string
- activeProtocol
- layout mode
- work-set rest time

Document the manual steps from `PhoneMigrationV2.md` Verification section so they can be followed on device.

---

## R — Reassert

Reassert `PhoneMigrationV2.md`:

- No hardcoded schedule or rotation on restore.
- Protocol cycles + routine/augment graph are in the workout backup.
- Rest timers and layout mode are in the preferences backup.
- Single restore pipeline.
- Payload v4 is additive; v3 still works.
- No fifth wing.
- Dual visual mode on any new UI.
- Ascension V3 remains primary for goals.

Fix any violation before continuing.
