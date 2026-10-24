# Neon Ascent – Soft Deload (CyberCrapp)

**Status**: Design lock for next workstream  
**Updated**: October 23, 2026  
**Repo**: `mojo70/Neon-Ascent`  
**Depends on**: `CyberCrapp.md`, `CCProgram.md`, `NeonCharge.md` (LOGBOOK_RECOVERY), `Recovery.md` (CLEARANCE is not a deload switch), `BackupCompleteness.md`  
**Studio pack**: `SoftDeloadGeminiPrompts.md`  
**Not this pack**: Arsenal, protocol engine math for HST/531/SS, CLEARANCE formula changes

`RecoveryEngine` already scores logbook recovery and can return `RecoveryStatus.DELOAD`. `WorkoutSession.isDeload` already exists. Nothing initiates a soft deload, nothing offers one-tap accept, and nothing changes the session structure. This pack ships that.

---

## What already works

- `RecoveryEngine.calculateScore` implements the NeonCharge lock: session RPE is the cake signal, set RIR is unused for CYBER_CRAPP, empty average never becomes NaN, status bands include DELOAD (30–49) and CRITICAL (<30).
- Summary copy already says “Soft Deload suggested” on DELOAD.
- `WorkoutSession.isDeload` is on the model. Codex shading expects it.
- CLEARANCE / SEED is explicitly not a deload switch. OPS uses LOGBOOK_RECOVERY.

---

## Product lock

1. **Soft deload is the only default.** Keep the same exercises. Convert rest-pause clusters to 2–3 straight sets. Target RIR 3–4. Minimize or drop lengthened partials and loaded stretches. Duration is one full A/B/C rotation plus 1–2 extra rest days (~7–10 days). Harder cruise is out of scope.
2. **Two triggers, both optional to accept.**
   - Recovery: `LOGBOOK_RECOVERY` status is DELOAD or CRITICAL.
   - Time: days since last soft-deload end (or `lastBlastStartDate` if no prior deload) is ≥ 28. Default window is 5 weeks; user can delay.
   - Never auto-apply. One-tap accept, delay, or dismiss.
3. **One-tap accept applies the structure.** Accept writes an active deload window (start, end). Sessions started inside the window are stamped `isDeload = true`. Session builder for CYBER_CRAPP in that window emits 2–3 `NORMAL` sets (not REST_PAUSE cluster), omits PARTIAL and STRETCH, and shows RIR target 3–4. Weight stays the current working weight.
4. **Manual always available.** Settings or protocol area has START SOFT DELOAD and END EARLY. Manual start uses the same window and the same session changes.
5. **End is automatic or early.** When the window ends, the next session is full CC again. Ending early clears the window. No silent extension.
6. **Do not deload on cake with no stalls.** RecoveryEngine already protects this. Do not add a second score. If status is OPTIMAL or CAUTION, time trigger may still offer (user can delay).
7. **CLEARANCE is not a trigger.** SEED GROUND does not start a deload by itself.
8. **Backup.** Active deload window (start, end, source) is durable user state. Include it in the workout/preferences payload section, bump version, single `restoreBackupJson` path. Older backups still restore.

---

## Surfaces

| Surface | Behavior |
|---|---|
| Before CC session start (or DECK) | Sheet when a trigger fires: reason (RECOVERY or TIME), one line of what changes, ACCEPT / DELAY / DISMISS |
| In-session header | `DELOAD` chip while window is active. No cluster panel. Straight sets + RIR 3–4 hint. No partials/stretch cards. |
| Settings / protocol | START SOFT DELOAD / END EARLY |
| Codex | Existing `isDeload` shading. No new wing. |

Copy stays short. No ERROR language. Neon theme, dual visual mode on any new sheet.

---

## What it is not

- Not a forced deload. User accepts.
- Not a change to CLEARANCE or NEON_CHARGE.
- Not a rewrite of RecoveryEngine scoring.
- Not a fifth wing.
- Not a harder cruise or percentage drop of working weight (weight holds; effort drops).
- Not HST strategic deconditioning.

---

## Architecture fit

- Trigger evaluation lives in domain next to RecoveryEngine (or a thin `SoftDeloadAdvisor`). Inputs: recovery status, last deload end / lastBlastStartDate, now.
- Window state lives on the profile or a small `DeloadWindow` row (userId, start, end, source). Persist it.
- Session builder (existing CC path in WorkoutViewModel / session start) checks the active window and emits NORMAL sets instead of the cluster when active. Stamp `isDeload` on the session.
- Do not fork a second logging schema.
- Backup per `BackupCompleteness.md`.

---

## Verification

**Automated**
- Advisor: DELOAD status → offer; OPTIMAL + 40 days → offer; OPTIMAL + 10 days → no offer; CRITICAL → offer.
- Accept writes a window whose end is ~7–10 days out.
- Session started inside the window is `isDeload` and has no REST_PAUSE / PARTIAL / STRETCH.
- Session after window end is full cluster again.
- Round-trip: window survives export/restore.

**Manual**
1. Force a recovery DELOAD (or wait for time) and confirm the sheet appears with ACCEPT / DELAY / DISMISS.
2. Accept. Next CC session is straight sets, RIR hint 3–4, no partials/stretch, header chip DELOAD.
3. End early. Next session is full cluster.
4. Confirm Codex shades the deload sessions.

---

## Non-negotiables

- One-tap accept. Never auto-mutate.
- Same exercises. Weight holds. Effort drops (straight sets, higher RIR, no partials/stretch).
- LOGBOOK_RECOVERY and time are the only triggers. CLEARANCE is not.
- `isDeload` stamped on sessions in the window.
- Backup included before the feature is done.
- Dual visual mode on the sheet.
