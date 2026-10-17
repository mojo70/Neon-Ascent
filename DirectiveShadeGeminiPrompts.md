# Neon Ascent – Directive Shade Gemini/Android Studio Prompts

**Status**: Ready for Studio  
**Updated**: October 17, 2026  
**Spec**: `DirectiveShade.md`  
**Depends on**: NeuralBrief AM slot (8888) already facts-first. Do not re-open the whole brief pipeline.

Run in order. Template must succeed with zero AI. Shade stays one action. Do not print bare “keep it easy” when a driver exists.

---

## PROMPT 0 — Reassert + locate writers

```
You are working in mojo70/Neon-Ascent.

Reassert:
- AM shade (8888) is facts-first. Headline ≤48 chars + one sentence + optional Directive clause + at most one action.
- Sleep length is required in the headline when a core night exists.
- Bare “keep it easy” / “Easy day” is forbidden when a driver (RHR, HRV, SEED band, recovery status) exists. The driver is the reason.
- Directive clause is optional and never replaces the sleep headline.
- No SANCTUM/SEED/WATCH tokens on the shade. No second button. No error strings.

Locate:
- BriefFactsBuilder / TemplateCopyWriter / NeuralBriefWorker (or current AM packet writer).
- Where RHR, HRV night, SEED band, and recovery status are already available.
- Where active Directives are read (AscensionRepository.getActiveDirectives).

Report the current AM template strings that contain “keep it easy” or “Easy day”. Do not edit yet.
```

---

## PROMPT 1 — Driver phrase + status token (replace bare easy)

```
Update the AM template writer so driverOrDayPhrase replaces bare “keep it easy” and sets a status token.

Status tokens (deterministic, same priority):
- OVERDRIVE — clear to push (SEED clear + no flags + sleep on target)
- NOMINAL — default / normal pace
- THROTTLE — ease intensity (RHR +8, HRV ≤70 %, SEED HOLD, short night)
- GROUNDED — pull back (SEED GROUND, recovery CRITICAL/DELOAD, sleep wreck + heavy)

Priority order (first match wins, never invent numbers). Set status from the match:

1. RHR last ≥ +8 bpm vs 7-day mean → status THROTTLE, copy `RHR {last} (usual {mean}) — keep intensity easy`
2. HRV night ≤ 70 % of 7-day mean → status THROTTLE, copy `HRV {night} vs {mean} — recovery day`
3. SEED band GROUND or HOLD and heavy/C/ME scheduled → status GROUNDED, copy `Morning tank low — hold the heavy work`
4. Recovery CRITICAL or DELOAD → status GROUNDED, copy `Recovery says deload`
5. Week hit rate complete → status NOMINAL, copy `Week is already {n}/{n}. Easy day.`
6. PICKUP open → status NOMINAL, copy `You still have {Push|Pull|Legs} open.`
7. PUSH + scheduled + no prior driver → status OVERDRIVE, copy `{Push} is up.`
8. Else status NOMINAL, omit or day-type only (`Legs today.`)

If a driver from 1–4 fires, do not also emit the old bare “keep it easy” or “Easy day” strings.
Missing data → NOMINAL or omit token. Never guess OVERDRIVE.

Sleep phrase rules stay exactly as locked in NeuralBrief.md.
Lift clause stays optional and only inside TTL.

Template must still succeed when any vital is missing (skip that driver).
```

---

## PROMPT 2 — Directive clause

```
Add optional directiveClause to the AM shade (and card if space allows).

Selection (first match):
1. Soonest A-tier with targetEndDate inside 90 days.
2. Soonest near-term (≤90 days) that has an open Mission.
3. If density ≥4 near-term or ≥3 A-tier → count only: `{n} near-term Directives open.`
4. Else omit.

Copy:
- Named: `{title truncated to ≤22 chars} — {days}d left` (append `A-tier` if tier is A and space allows).
- Count form when dense.

Rules:
- Never replace the sleep headline.
- Never add a second action button.
- Action remains OPEN OPS if a session is due; else OPEN DIRECTIVE (deep-link) only when a named Directive is shown; else omit.
- Optional preference “show Directives on morning shade” default true. If false, skip the clause.

Read active Directives from AscensionRepository. Reuse existing density counts if already computed.
```

---

## PROMPT 3 — Wire into shade packet + holographic avatar

```
Ensure the AM shade (8888) packet uses the new shape:

{sleepPhrase}.
{status?} {driverOrDayPhrase}.
{directiveClause?}

Headline ≤48 chars.
One sentence (driver or day, with optional status token).
Optional short Directive clause.
At most one action.

DECK NeuralBriefCard shows the same packet (status always visible when computed).

Holographic Avatar screen (LABS entry):
- Add a persistent status badge/label using the same four tokens (OVERDRIVE / NOMINAL / THROTTLE / GROUNDED).
- Source it from the morning packet or live SEED/recovery computation.
- Use existing neon accents. No new ring or color system.
- Updates when the brief packet refreshes.

Do not change PM shade. Do not change notification ID or channel.
Validator still rejects UUID, SESSION, ERROR tokens, and any string not backed by BriefFacts.
```

---

## PROMPT 4 — Examples + smoke

```
Add or update unit tests / snapshot checks for these cases (template only, no AI):

1. Short night + RHR +8 bpm + named Directive → driver mentions RHR, Directive clause present, no bare “keep it easy”.
2. On-target sleep + HRV ≤70 % + A-tier → HRV driver, Directive clause, OPEN DIRECTIVE or OPEN OPS.
3. Dense (≥4 near-term) → count clause only, no single title.
4. No driver, week complete → “Week is already n/n. Easy day.” allowed.
5. Missing RHR/HRV → those drivers skipped, template still emits.

Confirm shade never contains SANCTUM, SEED, WATCH, or a second button.
```

---

## PROMPT R — Reassert

```
Reassert:

- Status tokens OVERDRIVE / NOMINAL / THROTTLE / GROUNDED are deterministic from the driver priority. Missing data → NOMINAL or omit. Never guess OVERDRIVE.
- Driver replaces bare “keep it easy” when RHR, HRV, SEED band, or recovery status is the reason.
- Directive clause is optional, secondary, and never the headline.
- Sleep length required in AM headline when core night exists.
- Status appears on shade (optional), DECK card, and Holographic Avatar screen.
- One action max. Template succeeds with zero AI.
- No new channel, no fifth wing, no new ring, no Directive progress math on the shade.

Report any remaining “keep it easy” strings that fire when a driver is present.
```
