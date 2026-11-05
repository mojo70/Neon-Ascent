# Neon Ascent – NEON_CHARGE Accuracy (V2)

**Status**: Design lock  
**Updated**: 2026-11-04  
**Repo**: `mojo70/Neon-Ascent`  
**Source of truth for current behavior**: `core/domain/src/main/java/com/neon/ascent/core/domain/health/NeonChargeEngine.kt` (post `afc0d11`)  
**Studio pack**: `NeonChargeAccuracyGeminiPrompts.md` (local only)  
**Depends on**: `Recovery.md` (CLEARANCE), `Stress.md` (HR_LOAD rules), `BackupCompleteness.md`

The September `NeonCharge.md` lock is superseded for the **daytime drain**. Wake-seed improvements already on main stay. This lock fixes the observed failure: morning seed feels right, end-of-day number is still too full after meetings + coding + a CyberCrapp session.

---

## What stays (already on main)

- CLEARANCE, when present, is the frozen wake seed. Do not re-multiply sleep/HRV/RHR.
- Else non-linear sleep base (8 h / 480 min → 100, power 0.7 curve) + additive HRV/RHR z modifiers (±10 each). Range 10–100. Cold start 62 LOW.
- HR_LOAD definition: elevated minutes (HR ≥ baseline+25 **and** ≥ 100) outside workout and sit windows, 1-minute bins, coverage gate (≥20 samples and ≥2 h). Label `HR_LOAD`, never `STRESS`.
- Sits mask HR_LOAD only. No point grant. Not sleep. Not SANCTUM.
- Drivers published. Missing drivers omitted, not zeroed as punishment.
- Footer `NEON_MODEL · NOT_GARMIN`. Official Garmin BB replaces the ring only when a fresh official sample exists.
- Two signals remain separate: `LOGBOOK_RECOVERY` (protocol deload) vs `NEON_CHARGE` (tank right now).

---

## Problem with the live drain

Live coefficients under-count real load:

| Term | Live | Effect on a hard day |
|---|---|---|
| Passive | `hoursAwake * 1.8` (cap 50) | 10–12 h work → only ~18–22 pts |
| Session | `durationHours * 15 * (RPE/10) * 1.25 if CC` | 45 min CC at RPE 7 → ~10 pts |
| Steps | `steps * 0.0005` (cap 15) | 8–10 k → ~4–5 pts |
| HR_LOAD | `minutes / 12` (cap 15) | Desk meetings rarely trip the threshold |

A 75 seed after meetings + coding + one CC session still lands in the 35–45 range. That is not realistic.

---

## V2 drain model

Tank still 0–100. Sampled on read. Persist last value + timestamp.

### Passive — asymptotic

Linear `hours * k` is the wrong shape. Drain rate is higher when the tank is full and slows as it approaches the floor.

```
k passive          = 0.085          // personal k later
hoursAwake         = hours since sleepEnd (or 07:00 if no sleep)
passiveFraction    = 1 - exp(-k * hoursAwake)
passiveDrain       = wakeSeed * passiveFraction
```

Approximate effect on a 75 seed:

| Hours awake | Passive drain | Remaining from seed |
|---|---|---|
| 6 | ~30 | ~45 |
| 10 | ~43 | ~32 |
| 12 | ~48 | ~27 |
| 18 | ~58 | ~17 |

Cap passiveDrain at `wakeSeed * 0.85` so an idle day cannot zero the tank by itself. Driver: `PASSIVE {hours}h (−{n})`.

### Load terms (additive, stronger)

Applied after the asymptotic passive. Still published as separate drivers.

```
sessionDrain = sum over today's sessions:
                 durationHours * 48 * (sessionRpe.orNeutral(6) / 10.0)
                 * (1.4 if protocol is CYBER_CRAPP or max-effort else 1.0)

stepDrain    = max(0, stepsToday - 4000) / 280     // ~21 pts at 10 k

hrLoadDrain  = elevatedHrMinutes / 8               // cap 22 (was /12 cap 15)
```

Session examples (approximate):

- 45 min CC, RPE 7 → ~35 pts
- 60 min CC, RPE 8 → ~54 pts
- 40 min general, RPE 6 → ~19 pts

HR_LOAD threshold stays the same (no fake stress score). Weight increases so the minutes we *do* catch matter more. Soften only if coverage is thin: if samples < 40, use the old `/12` weight and note `HR_LOAD_THIN`.

### Recharge

Keep the live piecewise nap formula. Sits still do not recharge.

### Final

```
loadDrain   = sessionDrain + stepDrain + hrLoadDrain
recharge    = napRecharge
chargeNow   = clamp(wakeSeed - passiveDrain - loadDrain + recharge, 5, 98)
```

When a personal calibration is active (below), the three load terms and `k` are replaced by the fitted values before this clamp. The card still shows the raw drivers plus a `CALIBRATED` badge.

### Target behavior (acceptance)

A day with:

- Wake seed ~70–80
- ~10 h of meetings/coding
- One CyberCrapp session (RPE 6–8, 40–60 min)
- Ordinary steps (6–10 k)

must land in the **15–35** range by evening, not the 40s–50s. An easy day (low RPE or no session, low steps, low HR_LOAD) should still be in the 40s–60s. Idle-only should not hit single digits.

---

## Personal calibration (V2.1, ships with the drain)

Biohackers care about fit to *their* body. Hand-tuned coefficients are the global prior. Personal data is the update.

### Labels (quality first)

Optional, never required. Every label carries a source and a weight.

| Source | Weight | Notes |
|---|---|---|
| Explicit end-of-day or post-workout “felt remaining” 0–100 | 1.0 | Primary. Compact slider or reuse existing energy self-report if captured that day. |
| Next-morning CLEARANCE (when present) | 0.4 | Weak secondary. Only used as a regularizer, never the sole target. A low CLEARANCE after a high felt-charge is a signal, not an override. |
| Session RPE consistency | 0.2 | If the user logged RPE ≤4 (“cake”) but felt charge collapsed, or RPE ≥8 and felt charge barely moved, down-weight that day’s felt label. |

Store per day:

```
date, wakeSeed, hoursAwake,
sessionDrainRaw, stepDrainRaw, hrLoadMin, napMin,
protocolFlags, activeKcal, priorDayResidual,
feltCharge, feltSource, labelWeight,
nextClearance (nullable)
```

Outlier gate: if `feltCharge` differs from the prior prediction by >40 points and there is no corroborating signal (high RPE session, high HR_LOAD, or next-morning CLEARANCE also low), quarantine the label for 48 h. User can confirm or discard it. Quarantined labels are excluded from the fit.

Minimum to fit: **14 weighted labeled days** (sum of weights ≥14). Until then the hand-tuned V2 coefficients are used and the card shows `PRIOR`.

### Fit (ridge, the first personal model)

On-device, simple, published.

- Features: the raw drain terms + hoursAwake.
- Target: `wakeSeed - feltCharge` (effective total drain the user experienced), weighted by `labelWeight`.
- Model: ridge regression on `[passive_k, session_mult, step_mult, hr_mult]` with the hand-tuned values as the prior (strong L2 toward the global coefficients).
- Recency: exponential decay on labels older than 45 days (half-life 60 days) so a life change is not drowned by last quarter.
- Output: four personal multipliers (or a personal `k`). Clamp each multiplier to `[0.55, 1.9]`.
- Persist the multipliers + the effective label count + the fit date + the in-sample MAE.

Driver line when active: `CALIBRATED n={count} · k={k}`. User can reset to prior from Settings.

### Honesty

- Calibration never hides the raw drivers.
- If the fit’s leave-one-out MAE is worse than the prior’s MAE on the same labels, do not promote. Stay on `PRIOR`.
- No black-box score. The card always lists SLEEP/SEED, PASSIVE, WORKOUT, STEPS, HR_LOAD, NAP, and CALIBRATED when present.

---

## ML path (V2.2+, after labels exist)

The ridge calibration is the floor. The ML path only activates when it demonstrably beats that floor for this user. Accuracy and stability matter more than model complexity.

### When it is allowed to run

| Gate | Requirement |
|---|---|
| Labels | ≥40 weighted labeled days, spanning at least 3 distinct session-RPE bands and both high- and low-HR_LOAD days |
| Diversity | At least 8 days with a CyberCrapp or max-effort session and 8 days without |
| Stability | No more than 20 % of recent labels quarantined |
| Prior baseline | Ridge model already promoted and its rolling 14-day MAE is known |

If any gate fails, stay on the ridge calibration (or the hand-tuned prior).

### Feature set (only signals we actually have)

All features are computed from data already in the engine or the daily rollups. No invented scores.

**Core (same as ridge)**  
`wakeSeed`, `hoursAwake`, `sessionDrainRaw`, `stepDrainRaw`, `hrLoadMin`, `napMin`

**Context**  
- `priorDayResidual` — yesterday’s `chargeNow` at sleep start  
- `sleepDebt3d` — sum of (needMin − actual) over last 3 nights, from SANCTUM/CLEARANCE path  
- `rpe7dAvg` — mean session RPE of last 7 logged sessions  
- `hrLoad7dAvg` — mean elevated minutes  
- `activeKcalToday` — from HC aggregate (omit if missing)  
- `isHardProtocol` — 1 if any CC/ME session today  
- `hourOfDay` — for optional intra-day residual modeling later  

**Interactions (explicit, not learned blindly)**  
- `sessionDrainRaw * isHardProtocol`  
- `hrLoadMin * (1 if hoursAwake > 8 else 0.6)` — late-day sympathetic load costs more  
- `wakeSeed * hoursAwake` — already implicit in the asymptotic form; included so the model can adjust `k`  

Missing features are omitted, not zero-filled. The model must support sparse input.

### Target and loss

Primary target: felt remaining charge (or equivalently the drain `wakeSeed - feltCharge`).

Secondary regularizer: next-morning CLEARANCE, weight 0.35, only when the core night landed and CLEARANCE was emitted. This pulls the model toward autonomic reality without letting one bad night dominate a good subjective day.

Loss: weighted Huber (δ = 12) so a single 40-point outlier cannot dominate. Sample weights = `labelWeight * recencyDecay`.

### Model

Two-stage, both small enough for on-device:

1. **Stage A (default promotion target)** — Elastic net on the feature set above. L1 encourages sparsity so the card can still say which terms moved. Coefficients clamped per-feature after fit.
2. **Stage B (optional, ≥80 labels)** — Gradient-boosted trees, max depth 3, ≤40 trees, or a single small MLP (1 hidden layer, 8 units) if the tree MAE is not better. Exported as a few dozen floats. No large model, no cloud round-trip required for inference.

Training is incremental: refit nightly or after every 5 new labels, on-device. Keep the last 3 model snapshots.

### Validation before promotion (non-negotiable)

A new model is not shown to the user until it passes:

1. **Time-series split**: train on all but the most recent 10 labeled days; evaluate on those 10.
2. **Metrics** (must beat the current production model — ridge or previous ML — on the hold-out):
   - MAE ≤ current MAE − 1.5 points
   - No increase in maximum absolute error
   - Calibration: mean predicted vs mean actual within 4 points across tertiles of predicted charge
3. **Stability check**: coefficients or leaf values must not move more than 25 % from the previous promoted model unless the hold-out gain is ≥3 points.
4. **Fallback test**: if the new model is disabled, the previous model (or ridge) must still be loadable and produce the identical number for the same inputs.

Only then stamp `MODEL v2.2` (or later) and switch. Otherwise keep the previous model and log the rejection reason internally.

### Uncertainty and display

- Every prediction carries a simple uncertainty: residual std from the hold-out set, widened when features are missing or the day is outside the training support (e.g. first CC session after a month off, or HR series thin).
- If uncertainty > 18 points, the card shows the number but adds `LOW_CONF` and does not let the value drive any automated suggestion.
- Driver list remains. For the elastic net, show the top 3 contributing features that differ from the prior. For trees, show the two highest-gain features. Never a naked integer.

### Continuous monitoring

- Rolling 14-day MAE vs felt labels (when the user supplies them).
- If rolling MAE exceeds the prior’s MAE by >2 points for 10 days, auto-demote to the ridge calibration and surface `RECALIBRATING`.
- User can force-reset to prior at any time; the reset survives backup/restore.

### What this still cannot do

- Reproduce Firstbeat / Garmin Body Battery. We lack continuous daytime RMSSD.
- Predict illness or overtraining from charge alone.
- Override CLEARANCE or LOGBOOK_RECOVERY. Those stay independent signals.
- Train on unlabeled days. No pseudo-labeling.

This is the same evidence-minded progression used on the serum side: published heuristic → personal linear update → validated non-linear model only when the data justify it. The difference is the explicit hold-out gate and the demotion path, so accuracy cannot silently regress.

---

## Backup + P2P

Per `BackupCompleteness.md`. Calibration state is user data.

- Payload version bump.
- Include: personal multipliers (ridge), ML model snapshot(s) + version, label history (rolling 120-day window), quarantine flags, and the last hold-out MAE.
- Single restore path. Round-trip test: fit + promoted model on device A, transfer, device B produces the same charge for the same inputs and the same model version.
- Reset-to-prior and demotion state must survive restore.

---

## What we still do not do

- Call NEON_CHARGE Body Battery.
- Publish a 0–100 stress score from daytime HR.
- Infer sits from still + low HR.
- Add CHARGE points for meditation.
- Let set-level RIR from rest-pause drive the tank (session RPE and duration do).
- Train on a single day or fewer than 14 weighted labels.
- Promote an ML model that fails the hold-out gate.
- Hide the global prior or the raw drivers. User can always see and reset.

---

## Surfaces

- LABS: gauge + SEED line + driver list + `PRIOR` / `CALIBRATED` badge. Footer unchanged.
- DECK / Avatar: same number. No second formula.
- Codex: optional chart of charge trajectory + the calibration history (later).
- Pulse: out of scope this lock.

---

## Implementation notes (for the engine)

Replace the linear passive and the weak session term in `NeonChargeEngine.calculateCharge`. Keep the wake-seed block (CLEARANCE short-circuit + non-linear sleep + additive ANS) exactly as it is on main. Add the calibration store and the optional felt-charge capture in the same workstream as the drain change so the label pipe exists from day one.
