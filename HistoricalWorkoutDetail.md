# Neon Ascent – Historical Workout Detail (Full Set + Cluster Archive)

**Status**: Design lock  
**Updated**: October 26, 2026  
**Repo**: `mojo70/Neon-Ascent`  
**HEAD at lock**: current main (post-JourneyContinuity / v4–v5 backup work)

Codex OPS_LOG already has a session heatmap and a session history list. Both currently stop at summaries (protocol, day type, duration, RPE, volume). The underlying data model already stores every set—including rest-pause cluster mini-sets—as individual `SetLog` rows with `clusterMiniSetIndex`. The missing piece is a read-only historical view that surfaces the full log when a day is selected.

Still **not** a new wing. Still uses the existing session / log / set tables. Dual visual mode mandatory. Backup + P2P coverage for any new fields is required in the same workstream.

---

## Why this exists

User wants to go all the way back in time and:

- Pull up a rest-pause cluster and see the exercise plus the reps for each of the three mini-sets.
- Click a day in the Codex heatmap (or a row in session history) and load a read-only view of that day’s full workout log.

Current state:

- `WorkoutSessionEntity` → `WorkoutLogEntity` (exercise) → `SetLogEntity` (individual set).
- `SetLogEntity` already carries `setType`, `weight`, `reps`, `clusterMiniSetIndex`, `rir`, `isWarmup`, `isLengthenedPartial`, `isLoadedStretch`, `stretchDurationSeconds`, prescribed fields, `isAmrap`, `accommodatingLoad`.
- Logging UI groups sets that share a log and have `clusterMiniSetIndex != null` (or REST_PAUSE / PARTIAL / STRETCH) into a single visual cluster row and opens `ClusterLoggingDialog` for the mini-sets.
- Codex `SessionHeatmap` lets you select a date and shows a short summary line. `SessionRow` is not clickable into a full log.
- No read-only historical detail surface exists.

The data is largely already there. The gap is query + presentation + navigation.

---

## Locked product calls

1. **Read-only Session Detail is the single surface.**  
   A transient sheet or overlay (not a new nav destination or wing) that shows one session in full. Opened from:
   - Codex heatmap day click (when the day has one or more sessions).
   - Codex session history row click.
   - Optionally from any other session list later; same component.

2. **What the detail must show (minimum).**  
   - Session header: date, protocol, day type, duration, session RPE, joint health, deload badge, primary augment, notes.
   - Exercises in logged order (`WorkoutLog.order`).
   - For each exercise: name, any augment, then its sets.
   - Sets in order. For rest-pause / cluster groups (same log, `clusterMiniSetIndex` present): render as one cluster block with the three (or however many) mini-sets explicitly listed, e.g.  
     `RP cluster @ 185`  
     `1: 8` `2: 6` `3: 4`  
     plus any RIR / partial / stretch flags on the mini-sets.
   - Non-cluster sets show type label (W / D / F / WM / etc.), weight, reps, RIR, and any stretch duration or partial flag.
   - Volume per exercise and session total (already computable).

3. **Data contract.**  
   The detail loads via existing repository methods that return `SessionWithDetails` (session + logs + sets). No new tables for V1.  
   If a cluster was logged, the three mini-set rows must already exist with distinct `clusterMiniSetIndex` values. The detail view must not collapse or drop them.  
   If historical rows are missing `clusterMiniSetIndex` but are typed REST_PAUSE, still show them; do not invent indices.

4. **Empty / multi-session days.**  
   - Day with no session: heatmap selection shows the existing empty state; no detail.  
   - Day with multiple sessions: heatmap or list presents a short picker (session time / protocol) then opens the chosen one. Do not merge sessions.

5. **Dual visual mode.**  
   The detail sheet uses `LocalNeonTheme` / `LocalVisualMode`. CYBER can use accent highlights. STEVE and PILOT stay within their locked palettes (no forced neon, no glow in STEVE). Layout of the log itself is the same across modes.

6. **No editing in this surface.**  
   Read-only. Any correction of a past session stays in the existing logging flow (out of scope here).

7. **Backup completeness.**  
   `SetLog` fields used by the detail (including `clusterMiniSetIndex`, type, weight, reps, RIR, flags) are already in the workout backup section. Confirm they round-trip. If any new display-only field is added to the entity, it must be added to the payload and tested in the same workstream.

8. **Out of scope for this slice**  
   - Editing historical sessions.  
   - New analytics beyond the detail view.  
   - Changing how clusters are logged during an active session (unless a bug is found that drops mini-set rows).  
   - Fifth wing or permanent Codex route.

---

## Architecture fit

- Query: existing `workoutDao.getAllSessionsWithDetails()` / `getSessionsBetween` already return the graph. Add or reuse a `getSessionWithDetails(sessionId)` if a single-session fetch is cleaner for the sheet.
- UI: new composable (e.g. `SessionDetailSheet`) hosted by Codex OPS_LOG. It receives the session graph and renders. No new top-level navigation.
- Grouping logic for clusters lives in one place (shared or duplicated carefully from the logging screen’s grouping) so the detail matches what was logged.
- Dual visual mode tokens only.

---

## Verification

**Automated**
- Given a session with one exercise and three `SetLog` rows that have `clusterMiniSetIndex` 1/2/3 and distinct reps, the detail model (or a pure function that formats the cluster) returns all three mini-set reps.
- A normal set and a warmup set render with correct type labels.
- Multi-session day produces a picker, not a merged log.

**Manual**
1. Log a session that includes a rest-pause cluster with three mini-sets of different reps.
2. Open Codex OPS_LOG, click that day on the heatmap → detail opens, exercise and all three mini-set reps are visible.
3. Click the same session in the history list → same detail.
4. Toggle STEVE / PILOT → detail remains readable and on-palette.
5. Export / restore a backup that contains the cluster; reopen the day → mini-sets still present.

---

## Non-negotiables

- No new wing.
- Read-only.
- Cluster mini-sets are never collapsed or dropped in the detail view.
- Dual visual mode (CYBER / STEVE / PILOT).
- Existing session/log/set tables; no parallel history store.
- Backup + P2P for any new persisted field in the same workstream.
- Ascension V3 and other domain locks unchanged.
