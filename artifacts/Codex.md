# Codex – Neural Archive (Analytics & Reporting)

**Status**: Planning  
**Last Updated**: October 31, 2026 (IA rebalanced for Labs redesign + biohacking catalogs)  
**Surface today**: MainHub pager index 0 (`NavItem.Codex`) → `LoreScreen` titled `NEURAL_HISTORY`  
**Intent**: Repurpose Codex from weekly lore dump into the longitudinal reporting layer for training, biometrics, bloodwork, and biohacking catalogs — without turning it into a second logging app.

---

## Product decision

Codex is the **archive**, not the **cockpit**.

| Tab | Job | Mode |
|-----|-----|------|
| OPS | Do the session | Write |
| LABS | Live protocols, HUD insights, “what now” | Write + interpret |
| CODEX | History, trends, PRs, labs, fuel history, catalog detail | Read + query; write only for serum/labs |

If a user is mid-set, they stay in OPS. If they want “is my squat actually moving” or “what was my active stack last month,” they open Codex.

Do **not** delete Origin Log / weekly chapters. Codex means book. Keep Chronicle as a wing so the tab does not lose identity flavor.

Existing `WorkoutProgressScreen` (`BIOMETRIC ANALYSIS`: streak, total volume, crude volume chart, last-10 PRs, TDEE card) is the seed. Absorb it. Do not ship a third progress surface.

---

## IA – four wings (rebalanced 2026-10-31)

Top of Codex = compact header + wing switcher (not six more bottom tabs).

1. **OPS_LOG** — lifting analytics (ship first)  
2. **VITALS** — measured body & device trends (wearable / HC + BodyLog series + fuel history)  
3. **STACK** — intervention archive + catalogs (SERUM bloodwork + DIET + PROTOCOLS + SUPPLEMENTS panels)  
4. **CHRONICLE** — current Origin Log + weekly chapters (move, don’t rebuild)

Default landing: **OPS_LOG**. Empty Chronicle stays one tap away.

**Why STACK instead of stuffing catalogs into VITALS or leaving SERUM blood-only:**

- Vitals stays pure measured physiology (what the body did / what devices reported). Diet, biohacking protocols, and supplements are reference + active-history surfaces, not vital-sign series.
- SERUM (blood draws) groups naturally with the other intervention catalogs. Blood is the lab result of the inputs you ran. Diets / Protocols / Supplements are peer catalogs with the same A–D evidence model and LABS-today / Codex-detail split.
- Still four wings. Sub-panels (horizontal chips) inside STACK handle density without a fifth top-level item. Matches the “no fifth wing” lock.
- LABS owns live “what today” for diet / protocols / supplements. Codex owns archive, detail, and the one archival write (serum draws).

---

## OPS_LOG – what actually earns a tap

Generic gym apps dump vanity volume. CyberCrapp users care about **logbook beats**.

### Hub (no exercise selected)

Period chips: `7D` / `30D` / `90D` / `YTD` / `ALL`

Cards:

- Sessions this week / month / year (tappable → calendar heatmap)
- Hit rate vs scheduled days (from `UserWorkoutProfile.scheduledDays`)
- A/B/C coverage last 8–9 days (protocol target is ~2× per muscle / 8–9 days)
- Current blast week + recovery score sparkline
- Soft-deload windows marked on every chart (session `isDeload`)
- Top movers: last weight jump / last cluster PR / stalled (2 misses)

### Exercise dossier (deep dive)

Picker: recents from current rotation first, then library search.

For one exercise show:

- Working weight over time (step chart — weight holds, then jumps)
- Cluster total reps at that weight, vs range band (e.g. 11–20)
- Next-session prediction copy: “Hit 20 @ 225 → next load 230, min 11”
- Heaviest completed set + date
- Best cluster (reps @ weight + date) from `ExerciseAccomplishments`
- Estimated 1RM trend (already stored)
- Session volume for that movement
- RIR drift (are mini-sets getting easier or grinding)
- Consecutive misses + rotation history
- Last 8 sessions as a compact log table (weight / cluster / RIR / notes)

Squats / widowmaker: show heavy set **and** the 20-rep set as sibling series. Do not flatten them into one “max weight.”

Rows / deadlifts: honor their own ranges from `CyberCrapp.md`.

### Frequency views

- Heatmap of session dates (GitHub-style, neon)
- Count by week / month / year
- Muscle-group frequency (from `ExerciseDefinition.muscleGroups`) vs the 8–9 day target
- Protocol mix: CyberCrapp vs Classic vs Free

---

## VITALS

Pure measured physiology. Read from `daily_vital_rollups` written by HC deep sync + BodyLog series. No new wearable stack. No catalogs.

**Ingest lock:** `VitalsCards.md`. Totals = Health Connect aggregates. BodyLog series from `BodyLog.md`.

### Why KCAL_EATEN is empty (2026-09-06)

The chip is wired to rollup metric `KCAL_EATEN`. The reader works. The series is empty because **nothing writes meals**:

1. Nutrition is requested (`NutritionRecord`) but **not in the core permission set**. “Connected” can be true while Nutrition is still denied. User never got a second prompt.
2. Even granted, HC only has eaten kcal if Fit / MFP / Samsung / a diary **wrote** `NutritionRecord`. Pixel Watch and Google Health do **not** invent intake from the watch. Burned ≠ eaten.
3. `aggregateNutritionKcal` returns null when the sum is 0, so the rollup writer correctly skips the day. Empty chart is honest.

Fix order: re-request Nutrition if missing → empty state `NOT_LOGGED` on the chip (not a flat line at 0) → optional later Neon meal log that writes HC. Do not fill eaten with TDEE.

### Chart chrome (must ship)

Current STEPS_TIMELINE is a sparkline: no Y scale, no X dates, no last value. Unreadable at 30D.

Every VITALS series:

- Y ticks: min, mid, max of the visible window (not 0 unless the metric can be 0)
- X ticks: first, mid, last date (`M/D`)
- Header value: last point + unit (`22.2K STEPS`, `62 BPM`, `7.4 H`)
- Training-day ticks stay
- Empty series: `NO_ROLLUPS_YET` or `NOT_LOGGED` for eaten — never a fake zero line

Keep interpretation short. `NEURAL_ANALYSIS` must quote the last number. No “remarkable stability” with no figures.

---

## FUEL (macros)

Split is locked:

| Question | Surface |
|---|---|
| What do I eat **today**? TDEE + protein/carb/fat targets, somatotype note | **LABS** — live card |
| How has weight, intake, or target fuel **moved**? | **CODEX VITALS** — history |

Codex fuel views sit under VITALS (or a compact FUEL block on the OPS_LOG hub if implement prefers):

- Bodyweight series from `BodyLog.md` (`WEIGHT` rollup: HC scale or Neon log, prefer AM_FASTED). Same writer as VITALS chip WEIGHT.
- Target TDEE / macros over time (recomputed snapshots when profile or activity factor changes — do not overwrite a single current triple)
- Optional logged intake later; do not invent a food diary in P0
- Overlay: bodyweight vs working-weight jumps on a chosen lift (high-signal, cheap)

LABS keeps the current `MacroCard` behavior: “ESTIMATED TDEE / PRO / CARB / FAT” for today. Codex never becomes the place you check before lunch.

Why this split is right: macros-as-targets are a daily decision. Macros-as-history are an experiment log (cut vs blast, weight stalls, TDEE drift). Same numbers, different job. One write path for targets (profile + calculator), two read paths.

---

## STACK – intervention archive + catalogs

Sub-panels / chips inside the wing (horizontal segmented control). Default to last-used or SERUM. Dual visual mode on every surface.

### SERUM (bloodwork)

`BiohackingData.labResultsPath` is a file pointer, not a time series. That is the gap.

Add:

```
BiomarkerSample
  id, markerKey, displayName, value, unit, drawnAt, source, notes
```

Canonical keys (seeded, user can add): total T, free T, SHBG, estradiol, CBC staples, CMP staples, lipids, A1C, TSH/FT3/FT4, ferritin, vitamin D, hsCRP, etc.

Per marker:

- Sparkline + last value + delta vs previous draw
- Optional reference band (male/female + unit)
- Bio-age history from existing ONNX path (`calculatedBioAge` needs dated snapshots, not a single overwrite)

**Entry lives in Codex STACK → SERUM** (manual form first, PDF/OCR later). Trends, deltas, and history also live here. Do not split “add a draw” into LABS — that is the one write Codex is allowed, because a blood draw is archival by nature, not a daily cockpit action.

Do not block analytics on scanner quality.

### DIET

Full catalog + detail from `Biohacking_Diets_Database_v0.1.md`. Active diet history, parameter overrides, CCD flag. Live “today” surface stays in LABS. Link from LABS detail button lands here. Backup + P2P in same workstream.

### PROTOCOLS

Full catalog + detail from `Biohacking_Protocols_Database_v0.1.md` (non-diet: sauna, light, Zone 2, breathwork, etc.). Active set history. Do not confuse with workout ProtocolLibrary / CyberCrapp. Live “today” surface stays in LABS. Backup + P2P in same workstream.

### SUPPLEMENTS

Full catalog + detail from `Biohacking_Supplements_Database_v0.1.md`. Active stack history, dose notes, Rx / research flags. Live “today” surface stays in LABS. Backup + P2P in same workstream.

Catalog seeds are not user data (re-seed on restore if needed). Personal differential responses stay in user logs only.

---

## Data & performance rules

`WorkoutRepository.getFullHistory()` loading every session+set into memory is fine for early personal use and **wrong** as the Codex query path.

Add focused DAO queries / projections:

- `countSessions(from, to)`
- `sessionsByDay(from, to)`
- `logsForExercise(exerciseId, from, to)`
- `getAllAccomplishments()` already exists — use it for PR hub
- Optional `ExerciseTrendPoint` projection rebuilt on session complete (same CQRS-lite idea as `InsightProjectionProcessor`)

Charts must render from **already aggregated** points, not by folding sets in Compose.

Indexes already on `workout_sessions.date`, `workout_logs.exerciseId`. Add `set_logs.timestamp` if trend queries touch sets directly.

---

## Navigation & engagement

- Codex tab stays index 0. Replace `LoreScreen` body with a Codex hub; Chronicle hosts current lore UI.
- From in-workout exercise header: `VIEW_IN_CODEX` → exercise dossier.
- From Physical Ops: “ARCHIVE” card → Codex OPS_LOG.
- From LABS diet / protocol / supplement “details” buttons: deep-link to matching STACK sub-panel.
- Absorb `WorkoutProgressScreen` into OPS_LOG hub. Strip the live TDEE “today’s targets” card from that seed — that belongs in LABS.
- Empty state is a hook, not a void: “0 sessions logged. Open OPS and the archive writes itself.”
- Period + exercise choice persist in DataStore so the tab feels sticky. STACK sub-panel choice also persists.

Keep cyber labels (`OPS_LOG`, `STACK`, `SERUM`, `LOAD_TRACE`) but put the number first. Flavor after the stat.

---

## Build order

**P0 – worth opening the tab**

- Codex hub + wing switcher
- Session counts W/M/Y + heatmap
- Accomplishments-backed heaviest / cluster PR list
- Exercise dossier: weight step-trace + cluster reps vs range
- Move Chronicle under wing 4

**P1 – protocol-native**

- A/B/C + muscle frequency vs 8–9 day target
- Deload shading, miss/rotation badges
- Widowmaker dual series
- Deep link from workout card

**P2 – vitals**

- Event-backed HRV / sleep / RHR / Body Battery charts
- Volume × recovery overlay
- BodyLog series (WEIGHT, BF_PCT by method, tape, BP)

**P3 – stack**

- STACK wing + sub-panel chips
- `BiomarkerSample` + manual entry + per-marker history (SERUM)
- Bio-age snapshots
- DIET / PROTOCOLS / SUPPLEMENTS catalog detail + active history panels (link from LABS)

**P4 – synthesis**

- “Ask Guide about this chart” with the visible series injected
- Optional correlations (volume vs HRV, T vs cluster PRs) only after N samples exist

---

## Guardrails

- Logging remains in OPS. Codex does not grow a second set-entry UI.
- Codex writes: serum/lab draws only. Everything else in Codex is query.
- LABS keeps live “what to do today” including **today’s** macro targets, active diet, active protocols, active stack. Codex does not steal Biohacking.
- SPECIAL still updates only from real measurements.
- Honor-system: show logged truth, never shame empty weeks.
- Local-only. Export can reuse `exportHistoryToJson()` plus a serum CSV later.
- No fifth wing. STACK uses internal sub-panels.

---

## Locked product calls (updated 2026-10-31)

1. **Chronicle stays as wing 4.** Origin Log + weekly chapters move, they are not deleted.
2. **Serum entry + serum reporting both live in Codex** (now under STACK → SERUM sub-panel).
3. **Macros: live targets in LABS; trending/history in Codex VITALS.** See FUEL section.
4. **First-open period = `30D`.** Persist last-used period after that. `ALL` remains a chip.
5. **STACK wing** groups SERUM + DIET + PROTOCOLS + SUPPLEMENTS. VITALS stays measured physiology only. No fifth wing.
6. **Diets / Protocols / Supplements**: live surfaces in LABS; full catalog + detail + active history in Codex STACK. Backup + P2P in same workstream. Catalog seeds are not user data.

Default landing remains OPS_LOG.
