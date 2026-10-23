# Neon Ascent – Phone Migration V2 (Workout Completeness + Preferences + Cycle Graph)

**Status**: Design lock for next workstream  
**Updated**: October 22, 2026  
**Repo**: `mojo70/Neon-Ascent`  
**Prior lock**: `PhoneMigration.md` (payload v3 + P2P + Identity Forge escape)  
**HEAD at lock**: post-90b4811 (SelfMap prefs in backup)

V1 got the high-level sections and the offline QR path working. V2 closes the workout-critical holes that survived the first transfer: schedule, cycle state, exercise lists (routines/augments), progression, and preference-backed settings (timers, layout mode).  
Still **not** a new tab or fifth wing. Still one restore pipeline. Still 100 % offline.

---

## Why this exists

After the first real phone transfer the user observed:

- Workout logging history missing or incomplete (critical).
- Workout schedule did not transfer.
- Active protocol showed as active but the exercise list was empty / wrong day appeared in the cycle.
- Rest timer settings did not transfer.
- Workout layout mode (FOCUS vs CLASSIC) did not transfer.

Root causes in current code (`FullDataBackupRepositoryImpl` + models):

1. `UserWorkoutProfileEntity.scheduledDays` and `rotationIndex` are **hardcoded** on restore (`"[]"` / `0`) and are absent from the DTO.
2. `ProtocolCycleEntity`, `ProgressionStateEntity`, `ExerciseAccomplishmentsEntity`, the full routine/augment graph (`WorkoutRoutineEntity`, `WorkoutAugmentEntity`, cross-refs, sets, activations), and `ProtocolRepTargetEntity` are never exported.
3. Rest timers, rest-timer mode, workout zoom, and layout mode live in `HealthPreferencesDataStore` and `WorkoutPreferencesDataStore` and are not in any backup section.
4. Several other profile fields (injuries, timePerSession, customSequenceIds, lastBlastStartDate, etc.) are replaced with defaults.

A restore that drops these is a failed migration for the core training loop.

---

## Locked product calls

1. **No more hardcodes on profile restore.**  
   `scheduledDays`, `rotationIndex`, `deepLinkToRoutine`, injuries, timePerSessionMinutes, age/height/weight/gender/unitSystem, customSequenceIds, lastBlastStartDate, and the existing progression/increment fields must round-trip exactly.

2. **Workout graph is first-class.**  
   Expand `WorkoutBackupSection` (or add a tightly-coupled sibling section that is always restored with it) to include:
   - Protocol cycles (`ProtocolCycleEntity`)
   - Progression states + exercise accomplishments
   - Full routine graph (routines, routine-exercise cross-refs, routine sets)
   - Full augment graph (augments, augment-exercise cross-refs, augment sets, augment activations)
   - Protocol rep targets

3. **Preferences that affect daily training move into the payload.**  
   New optional `preferencesPayload` (or extend `SettingsBackupSection`) that serializes:
   - From `HealthPreferencesDataStore`: default/work/warmup/drop rest times, auto-start rest timer, rest timer mode, workout zoom, codex period/wing/last exercise (and any other user-tuned keys).
   - From `WorkoutPreferencesDataStore`: `workoutLayoutMode`, `useCustomNumpadInFocusMode`.
   - Other durable user prefs that are cheap to add (brief frequency already covered; add any remaining that are user-visible).

4. **Single restore pipeline remains.**  
   File restore and P2P both still call `restoreBackupJson`. On REPLACE, parent rows are inserted before children. Session/set/cycle counts are returned in `RestoreResult` so the UI can confirm.

5. **Schedule + protocol configuration stay usable.**  
   Once `scheduledDays` and the routine/augment/cycle data survive restore, the existing protocol/schedule UI is sufficient. No new “special recovery” screen is required for V2. If a protocol is active but has no routines, the user can still set days and add exercises the same way they do today.

6. **Dual visual mode unchanged.**  
   Any new progress/summary UI around restore must use `LocalNeonTheme` / `LocalVisualMode`.

7. **Out of scope for V2**  
   - Health Connect raw samples (re-ingest).  
   - Cloud or account transfer.  
   - Full SharedPreferences dump of every internal flag.  
   - Changing the P2P transport itself.

---

## Architecture fit

- DTOs and section additions live in `:core:domain` `FullBackupModels.kt`. Bump payload version to 4 (additive; v3 still restores).
- Export/restore logic stays in `FullDataBackupRepositoryImpl`. Inject the missing DAOs / DataStores.
- Profile restore must stop writing literal `"[]"` / `0` / default biometrics; map every field that exists on the entity.
- Restore order: exercise definitions → routines/augments → cross-refs/sets → sessions → logs → sets → cycles → progression → profile (last, so activeProtocol + schedule win).
- Preferences restore happens after DB restore and must not require a process restart to take effect.

---

## Verification

**Automated**
- `FullDataBackupRepositoryImplTest` (or new focused tests):
  - Profile round-trips `scheduledDays` (non-empty JSON) and `rotationIndex`.
  - At least one ProtocolCycle, one Routine + exercises, one Augment activation, and rest-timer values survive export → restore.
  - Layout mode survives.

**Manual smoke (must pass before calling V2 done)**
1. Old phone: set a non-default schedule, custom rest timers, FOCUS mode, active protocol with at least one routine that has exercises, note current cycle day.
2. Export (file or QR).
3. New phone / fresh emulator: restore.
4. Confirm:
   - Historical sessions and sets visible in OPS / session history.
   - Schedule days match.
   - Active protocol is selectable and the correct day/cycle is shown.
   - Exercise list for that day is populated.
   - Rest timer defaults and auto-start match.
   - Layout mode is FOCUS (or whatever was set).
5. Change a setting on the new phone, export again, restore onto a third target — values stick.

---

## Non-negotiables (reassert)

- Ascension V3 remains the primary goals model.
- Single `restoreBackupJson` path.
- No fifth wing.
- Dual visual mode on any new UI.
- Profile schedule and rotationIndex must not be hardcoded.
- Workout routines/augments/cycles are part of the training backup.
- Rest timers and layout mode are part of the preferences backup.
- v3 backups still restore (new fields optional).
