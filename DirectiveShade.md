# Neon Ascent – Directive Shade (AM Neural Brief)

**Status**: Design lock  
**Updated**: October 17, 2026  
**Repo**: `mojo70/Neon-Ascent`  
**Studio pack**: `DirectiveShadeGeminiPrompts.md`  
**Depends on**: `NeuralBrief.md` (AM slot 8888, facts-first, one action), `DirectiveIntake.md` (active Directives + density)

DECK card may be longer. Shade stays short. No fifth surface. No new notification channel.

---

## Why change the morning shade

“Keep it easy” without a reason is fluff. The original quality bar already required specific numbers vs baseline (HRV 26 ms vs 7-day 41). The shade currently drops the driver.

AM shade must answer:
1. What happened last night (sleep vs need).
2. Why today’s stance is what it is (the out-of-norm vital or recovery signal, if one exists).
3. The single highest-leverage open Directive, when one is worth naming.

---

## Operational Status (derived)

A single deterministic status token summarizes the day’s intensity ceiling. It is computed from the same driver priority used for the shade sentence. No LLM. Missing data falls back to NOMINAL or omits the token — never guesses OVERDRIVE.

| Status | Label | Intensity ceiling | Driven by |
|---|---|---|---|
| Blast | **OVERDRIVE** | 100 % / overdrive OK | SEED CLEAR or better + no recovery flag + sleep on/above need + week not full |
| Steady | **NOMINAL** | Normal prescription | Default / mid SEED / no major outliers |
| Ease Up | **THROTTLE** | Reduce load, keep moving | RHR ≥ +8 bpm, HRV ≤ 70 %, SEED HOLD, or short night without wreck |
| Rest | **GROUNDED** | No intensity; rest or Zone 2 only | SEED GROUND, recovery CRITICAL/DELOAD, or sleep wreck + heavy day |

Status is the summary. The driver sentence is the proof.

### Where it appears

- **AM shade (8888)**: optional prefix or first token on the driver line when space allows. Never replaces the sleep headline.
- **DECK NeuralBriefCard**: always shown when computed.
- **Holographic Avatar screen**: persistent status badge or label on the avatar hub (LABS entry). Same four tokens. Updates with the morning packet or live SEED/recovery. No new ring or color system — use existing neon accents.

---

## Locked shade shape (8888)

Headline ≤ 48 chars + one sentence + optional Directive clause + at most one action.

```
{sleepPhrase}.
{status?} {driverOrDayPhrase}.
{directiveClause?}
```

### sleepPhrase (unchanged)

- under need by ≥45 m → `Short night — {got} of {need}`
- within 20 m → `Slept {got} (need {need})`
- ≥45 m over → `Long night — {got}`

Sleep length is required in the headline when a core night exists.

### driverOrDayPhrase (replaces bare “keep it easy”)

Priority order. First real driver wins. Never invent a number. Status token is set from the same match.

| Condition | Status | Copy |
|---|---|---|
| RHR last ≥ +8 bpm vs 7-day mean | THROTTLE | `RHR {last} (usual {mean}) — keep intensity easy` |
| HRV night ≤ 70 % of 7-day mean | THROTTLE | `HRV {night} vs {mean} — recovery day` |
| SEED band GROUND or HOLD **and** heavy / C / ME scheduled | GROUNDED | `Morning tank low — hold the heavy work` |
| Recovery status CRITICAL or DELOAD | GROUNDED | `Recovery says deload` |
| Week hit rate complete | NOMINAL | `Week is already {n}/{n}. Easy day.` |
| PICKUP window open | NOMINAL | `You still have {Push\|Pull\|Legs} open.` |
| PUSH + scheduled + no driver | OVERDRIVE | `{Push} is up.` |
| No driver | NOMINAL | omit or fall back to day-type only (`Legs today.`) |

If a driver exists, do **not** also print the bare “keep it easy” or “Easy day.” The driver is the reason.

Lift clause (optional, same rules as NeuralBrief): only if session inside TTL and it explains the driver. `{weight} {shortName} is in the book.`

### directiveClause (new, optional)

Include only when all of these hold:

- At least one active Directive exists.
- Preference is not “never show Directives on shade” (default on).
- A clear candidate exists (see selection below).
- Adding it does not push the shade over one sentence + one short clause.

**Selection (first match):**

1. Soonest A-tier with `targetEndDate` inside 90 days.
2. Soonest near-term (≤90 days) with an open Mission.
3. If density thresholds are met (≥4 near-term or ≥3 A-tier) → count only: `{n} near-term Directives open.`
4. Otherwise omit.

**Copy:**

- Named: `{title truncated ≤22} — {days}d left` or `{title} — A-tier, {days}d`
- Count: `{n} near-term Directives open.`

Never replace the sleep headline with a Directive. Never add a second action button.

### Action

Unchanged from NeuralBrief:

- Session still due → `OPEN OPS`
- No session, Directive named → `OPEN DIRECTIVE` (deep-link to detail)
- Otherwise omit button.

---

## Examples

Short night + RHR elevated + Directive:

```
Short night — 5h27 of 7h20
THROTTLE — RHR 62 (usual 54)
Ship protocol — 12d left
[OPEN OPS]
```

On-target sleep + HRV down + A-tier:

```
Slept 7h10 (need 7h20)
THROTTLE — HRV 28 vs 41
Become netrunner — A-tier, 48d
[OPEN DIRECTIVE]
```

Clear day:

```
Slept 7h40 (need 7h20)
OVERDRIVE — Push is up.
[OPEN OPS]
```

Dense, no single driver:

```
Short night — 6h12 of 7h20
NOMINAL — Week is already 3/3.
3 near-term Directives open.
```

No Directive worth naming:

```
Long night — 8h05
OVERDRIVE — Push is up.
[OPEN OPS]
```

---

## What stays forbidden

- Bare “keep it easy” / “Easy day” when a driver exists.
- SANCTUM / SEED / WATCH tokens on the shade.
- Raw error strings, UUIDs, session dumps.
- Second button.
- Directive clause that overrides a short-night headline.
- Invented baselines.
- Status guessed when data is missing (fall back to NOMINAL or omit).

---

## Data sources (already available)

- Sleep minutes + needMin (SANCTUM)
- RHR last + 7-day mean (vitals rollup)
- HRV night + 7-day mean
- SEED band
- Recovery status
- Active Directives (priorityTier, targetEndDate, title)
- Density counts already computed in CodexViewModel

Builder must read these. Template writer must succeed with zero AI.

---

## Out of scope

- PM shade changes
- New notification channel or ID
- Fifth wing
- Directive progress math on the shade
- New ring or color system for status (use existing neon accents)
- Settings beyond an optional “show Directives on morning shade” toggle (default on)
