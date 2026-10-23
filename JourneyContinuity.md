# Neon Ascent – Journey Continuity (Payload Completeness + Transfer Terminal)

**Status**: Design lock for next workstream  
**Updated**: October 22, 2026  
**Repo**: `mojo70/Neon-Ascent`  
**Prior locks**: `PhoneMigration.md` (v3 + P2P), `PhoneMigrationV2.md` (v4 workout graph + timers/layout)  
**HEAD at lock**: post-b6f7a4f (v4 workout graph + preferencesPayload)

V2 closed the training-loop holes. This lock finishes the “app still feels like mine after a phone switch” journey: remaining preference stores, rites, library state, a visible backup health line, a post-restore summary, and a cyberpunk terminal boot/checksum sequence that runs on the real restore stages.

Still **not** a new tab or fifth wing. Still one `restoreBackupJson` path. Still 100 % offline. Dual visual mode mandatory on every new surface.

---

## Why this exists

After V2 the core workout loop survives a transfer. The daily rhythm and secondary domains still reset or vanish:

- Brief / Pulse preferences (target wake, PM mode, adaptive wake, quiet hours, directive shade toggle) live in `BriefPreferencesDataStore` and are not in the payload.
- Directive intake mode (MENTOR vs form) lives in `DirectivePreferencesDataStore` and is not in the payload.
- Rite sessions (PRAYER / SIT / KEGEL) are not exported, so streaks and Codex rite cards reset.
- Library highlights (and any reading state) are not in the payload.
- Remaining Health prefs (auto-sync, enabled attributes, live monitoring) and a few SharedPreferences flags (netrunner mode, altar true-text, biometric lock settings) are still outside the backup.
- There is no visible “last backup” indicator and no confirmation of what actually arrived after restore.
- The transfer itself is a silent wait. A terminal-style boot + checksum sequence tied to real stages adds polish and trust without inventing a new surface.

---

## Locked product calls

1. **Preferences section expands (additive, payload v5).**  
   Extend `PreferencesBackupSection` (or keep it optional) to include:
   - From `BriefPreferencesDataStore`: `targetWakeWd`, `targetWakeWe`, `pulsePmMode`, `pulsePmCustomDays`, `pulsePmCustomTime`, `adaptiveWakeEnabled`, `quietHoursEnd`, `showDirectivesOnMorningShade`. Last-brief content may be omitted or included as optional (it regenerates).
   - From `DirectivePreferencesDataStore`: `directiveIntakeMode`.
   - From `HealthPreferencesDataStore` (remaining): `autoSyncEnabled`, `syncIntervalHours`, `enabledAttributes`, `liveMonitoringEnabled`, `showSyncNotification`.
   - From SettingsRepository / secure prefs: `isNetrunnerMode`, `isAltarTrueTextMode`, biometric lock enabled + timeout (do **not** export secrets or tokens).

2. **Rites are first-class.**  
   New optional `ritesPayload` with `RiteSessionDto` (id, kind, startedAt, endedAt, durationMin, source, notes, localDate). Restore into `rite_sessions`.

3. **Library state is included.**  
   New optional `libraryPayload` (or extend existing codex section carefully) covering:
   - Library highlights (`LibraryHighlightEntity`)
   - Any user-added or progress-bearing library rows that are not pure asset seeds.  
   Do not duplicate the existing quotes path if it already round-trips; prefer one source of truth.

4. **Backup health is visible in Settings.**  
   Under the existing DATA / backup section, show:
   - Last successful backup timestamp (local).
   - Payload version of that backup.
   - Optional one-line counts (sessions, directives, rites) if cheap.  
   Persist the timestamp in DataStore when an export or P2P send succeeds.

5. **Post-restore summary.**  
   After a successful file or QR restore, show a short summary (can be the final frame of the terminal or a small sheet) with the counts from `RestoreResult` (sessions, sets, cycles, rites, etc.) before navigating to Main Hub. Dual visual mode.

6. **Transfer Terminal (the polish).**  
   A transient full-screen (or near-full) overlay that appears during file restore and during QR receive + restore. It is **not** a new route in the nav graph and does not survive process death as a destination.

   **Real stages (driven by actual work, not a fake timer):**
   1. `HANDSHAKE` / `RECEIVING` (QR path) or `READING` (file path)
   2. `DECRYPT` (QR) / `PARSE`
   3. `CHECKSUM` — display a rolling SHA-256 (or short hex prefix) that settles on the real hash when verification finishes
   4. `RESTORE_WORKOUT` (sessions, graph, profile)
   5. `RESTORE_DOMAINS` (goals, vault, selfmap, bio, chat, rites, library)
   6. `RESTORE_PREFS`
   7. `INTEGRITY` — final check + counts
   8. `READY` — brief hold, then dismiss into Main Hub (or the summary)

   **Visual rules:**
   - CYBER: monospace terminal, cyan/green on near-black, subtle scanline or cursor blink, checksum digits that “lock in.” Use `LocalNeonTheme`. No hardcoded pure neon if theme tokens exist.
   - STEVE: same layout and text, grayscale / ink, **no glow, no scanlines, no forced neon**. Derive colors from `LocalVisualMode` / theme.
   - Keep the animation short once real work is done; do not artificially delay the user more than ~300–500 ms on the final READY frame.
   - If restore fails, the terminal shows a clear `FAIL` line with the existing error message and a way back (no silent hang).

7. **Single pipeline, additive version.**  
   Bump payload to 5. All new sections optional. v3 and v4 still restore. File and P2P both call `restoreBackupJson`. Parent-before-child order preserved.

8. **Out of scope**  
   - Health Connect raw samples.  
   - Cloud or account transfer.  
   - Exporting encryption keys, tokens, or biometric secrets.  
   - Making the terminal a persistent screen or fifth wing.  
   - Changing the P2P transport.

---

## Architecture fit

- DTOs and the new sections live in `:core:domain` `FullBackupModels.kt`.
- Export/restore stays in `FullDataBackupRepositoryImpl`. Inject the additional DAOs and DataStores.
- The terminal is a Compose overlay / dialog hosted by the existing restore callers (Identity Forge abort path and Settings restore / migrate). It observes a `RestoreProgress` (or similar) flow emitted by the repository or a thin coordinator so the stages are real.
- Last-backup timestamp lives in an existing preferences DataStore (or the backup prefs already used by `UserPreferencesRepository`).
- Dual visual mode is required on the terminal and the summary.

---

## Verification

**Automated**
- Round-trip target wake + pulse PM mode + directive intake mode.
- Round-trip at least one rite session.
- Round-trip at least one library highlight.
- v4 payload (no new sections) still restores cleanly.
- Terminal progress stages are emitted in order for a successful restore (unit-level or instrumentation-friendly).

**Manual**
1. Old device: set non-default target wake, PM pulse schedule, directive intake mode, log a rite, add a library highlight, enable netrunner or altar true-text if desired.
2. Export or QR.
3. New device: observe the terminal sequence (stages advance with real work, checksum settles, STEVE mode is grayscale if toggled).
4. Confirm: wake times, pulse schedule, intake mode, rite history, highlight, and the Settings last-backup line are correct.
5. Fail a restore (bad file) and confirm the terminal shows FAIL and returns cleanly.

---

## Non-negotiables (reassert)

- Single `restoreBackupJson` path.
- Payload v5 is additive; v3/v4 still work.
- No fifth wing; terminal is transient.
- Dual visual mode (CYBER / STEVE) on the terminal and summary.
- No secrets or tokens in the payload.
- Rites and library highlights are included.
- Brief and directive preferences are included.
- Last-backup timestamp is visible in Settings after a successful export.
- Ascension V3 remains the primary goals model.
