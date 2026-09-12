# Neon Ascent – Neural Brief (Morning Pulse + Notification Policy)

**Status:** Design lock for next workstream  
**Updated:** September 7, 2026 (AM/PM slots, TARGET_WAKE, evening NEED_ONLY)  
**Repo:** mojo70/Neon-Ascent  

This is the notification + biometric-review layer. It is not a new tab.  
OPS logs. LABS decides “what now.” Codex archives. The brief is LABS delivered to the shade.

---

## Why this exists

Google Fit’s morning card works because it does four things in order:
1. Names yesterday in plain language.
2. Cites 2–3 real numbers.
3. Links those numbers to a cause the user recognizes.
4. Gives one move for today.

Neon Ascent already has richer inputs (last squat session, cluster vs range, recovery score, scheduled day type, HRV/sleep via Health Connect / Garmin). The current brief wastes them.

Current shade (2026-08-30):
```
⚡ NEURAL BRIEF // SYNC_SUCCESS
ERROR: CORE_MALFUNCTION: AICore failed wi...
```
That is a product failure, not a flavor miss.

---

## What is broken today (code, not vibes)

| Symptom | Actual path |
|---|---|
| **AI error printed as the brief** | `GeminiNanoClient.generateContent` returns `"ERROR: CORE_MALFUNCTION: ${e.message}"`. `InsightProjectionProcessor.synthesizeWithAi` stores that string as `SocraticInsightEntity.content`. `NeuralBriefWorker.formatBriefContent` dumps `insight.content` verbatim. Title still says `SYNC_SUCCESS`. |
| **Brief has no training facts** | Worker reads latest insight + recommendation + a random mission. It never reads `WorkoutRepository`, `RecoveryEngine`, last session loads, or next A/B/C day. |
| **LLM is on the critical path** | Nightly `InsightProjectionWorker` must call `aiCore.generateContent(..., forceLocal = true)`. If local is cold or `AICore` is unbound, the stored “insight” is an error. Heuristics exist (`LOW_RECOVERY_SIGNAL`, `HIGH_MOMENTUM`) and are then ignored. |
| **Local-only in background** | Processor forces local. `AiProvider` will not fall back to cloud when `forceLocal=true`. Gemma file missing + Nano bind fail = guaranteed error string. |
| **Engine never warmed before send** | Brief worker does not check `AiCore` readiness. It does not retry generation. It does not wait for Health Connect sleep session to close. |
| **Notify-on-open** | Two culprits. (1) `MainActivity.onCreate` → `HealthSyncWorker.schedulePeriodicSync` + worker `getForegroundInfo()` posts “Neural Uplink Active / Syncing biometric data...”. (2) `smartPingScheduler.scheduleSmartPings()` on every cold start enqueues `NeuralBriefWorker` / insight work; first run or expedited path can fire a shade ping while the user is already in the app. Neither is a user-facing product event. |
| **Wrong actions** | Shade buttons are `LOG COMPLETE` / `SKIP + REFLECT` / `SNOOZE 2H` even when the body is an AI crash. Google’s chips answer a real question. Ours do not. |
| **Tone** | “Systems nominal.” “Window is open for the mission.” Socratic filler from Gemma simulated fallback. Corny and empty. |
| **Scheduling** | `PeriodicWorkRequest` 24h + 1h flex, `KEEP`, `requiresBatteryNotLow`. Not anchored to wake time or a user clock. Flex + idle means 09:37 one day and 14:10 the next. |

Do not “prompt harder.” Change the pipeline.

---

## Product lock

1. **Facts first. Copy second. LLM optional.** A brief must be sendable with Room + Health Connect and zero AI.
2. **Never publish an error string.** If content starts with `ERROR`, is blank, or equals a known malfunction token, it is not an insight. Discard it. Use the template writer.
3. **No notification because the app opened.** Opening the app is not an event. Health sync is silent unless the user asked for a manual sync and we need a foreground service (then use a low-importance, non-alerting channel, not Neural Brief).
4. **Slots, not one recycled paragraph.** Morning Pulse and Evening Load. IDs 8888 AM / 8889 PM. Hash includes slot + lead. A lift is a lead once.
5. **Slot-first, not Fit-clone.** AM default lead is last night + SEED. Training is a clause when it explains the seed or today’s prescription. PM default lead is lights-out + CHARGE. We are not a second Fitbit Morning Brief.
6. **Shade = 1 action.** DECK card may have two. Never three generic mission buttons.
7. **AM shade after detected wake + 20 min.** Quiet hours still win. No shade before that even if the clock says 07:00.
8. **LABS owns “what now.” Codex owns history.** Tapping the pulse opens LABS / OPS as the action demands. Do not invent a Brief tab. Optionally append the sent brief to Codex VITALS later (P2). 8b. DECK chrome is locked. The main page is `DashboardScreen.kt` as shipped: `SlimChromeHeader` (RUNNER // name, RANK, clock, weather), existing `NeuralBriefCard`, `MetricPillRow` (STEPS / KCAL / STREAK from Health Connect), terminal, LOG SESSION / refresh / +. Grid background stays. CODEX stays on the bottom bar. Do not restyle DECK to land the pulse. Only swap the insight text inside `NeuralBriefCard`. Empty-state copy may replace `NEURAL_LINK_ESTABLISHED. SCANNING_SYSTEM...` — it may not replace the page.
9. **Tone: calm, specific, short.** Number first. Flavor after. No Socratic questions in the shade. No “SYNC_SUCCESS.” No “systems nominal.” No “cybernetic traveler.”
10. **Honor-system.** Missing sleep or missing session is stated as missing data, not as failure.

---

## What “as good as Fit, less corny” means

Fit template (from the reference screenshot):
> Yesterday's heavy lifting and weeding left a mark  
> Readiness 57 after HRV dropped to 26 ms. 320 lb squats + yard work raised recovery need.  
> Stick to a light family walk today. Did something interrupt sleep?

Neon Ascent equivalent, same morning, same user:
> 320 on the squat, HRV 26 — hold the blast  
> Recovery 57. Overnight HRV 26 ms vs 7-day 41. Sleep 6h 12m.  
> Today: walk or Zone 2 only. Next heavy C-day moves if joints are still loud tomorrow.

That is the quality bar. Specific load. Specific vital vs baseline. One stance. One action.

### Forbidden in the shade:
- Raw exceptions, stack fragments, `AICore`, `GEMMA_MALFUNCTION`, `NEURAL_LINK_EMPTY`
- Mission titles the user did not schedule today
- “Maintain current momentum”
- Asking three sleep-cause chips unless we actually persist the answer into a note

### Allowed flavor:
`NEURAL BRIEF`, `HOLD`, `PUSH`, `RECOVER` as a short status token after the headline. Not as the headline.

---

## Architecture

```
 Health Connect / Garmin uplink     WorkoutSession + SetLog
                 \                     /      RecoveryEngine
                  \                   /
               BriefFactsBuilder  ---------------------> BriefFacts (pure data)
                                        |
                               BriefStanceResolver  --> Stance + Action
                                        |
                     +------------------+------------------+
                     |                                     |
              TemplateCopyWriter                    Optional AiPolish
              (always succeeds)                    (only if AiCore.ready)
                     |                                     |
                     +-------- BriefCopyValidator ---------+
                                        |
                               DeliveryPolicy
                               (channel, quiet hours,
                                1/day, not-in-foreground)
                                        |
                               NeuralBriefManager.show
```

### New types (domain)

```kotlin
BriefFacts
  generatedAt
  lastSession: { date, dayType, protocol, topSets: List<{name, weight, reps}>, notes }
  recovery: { score, status, drivers[] }
  vitals: {
    sleepMinutes, needMin, sanctum, sanctumTier,
    seed, seedBand, chargeNow,
    hrvNight, hrvBaseline7d, rhrLast,
    stepsSoFar
  }
  schedule: { targetWakeWd, targetWakeWe, derivedWakeWd, derivedWakeWe, lightsOut }
  nextSession: { scheduled, dayType, liftsDue[], weightJumpsDue[] }
  dataQuality: { hasSleep, hasHrv, hasSession24h, sources[] }

enum BriefStance { PUSH, HOLD, RECOVER, DELOAD, LIGHT_DAY, MISSING_DATA }

BriefAction
  label          // "OPEN OPS" | "START WALK" | "REVIEW DELOAD" | "ASK GUIDE"
  route          // existing deep link only
  extras

BriefCopy
  headline       // ≤ 48 chars, no ALL_CAPS status codes
  body           // 2–4 short sentences
  stance
  actions        // 1 required, 1 optional
  source         // TEMPLATE | POLISHED
  factsHash      // so we do not re-send identical packets
```

Keep `SocraticInsightEntity` for now. Stop treating it as the notification body.  
New table is optional in P1:

```kotlin
MorningBrief
  id, localDate, factsJson, copyHeadline, copyBody, stance,
  source, deliveredAt, openedAt, actionTaken
```

If you skip the table in P0, persist `lastBriefLocalDate` + `lastFactsHash` + `lastLeadSessionId` + `slot` in DataStore.

---

## Freshness lock (2026-09-03)

Why pulses feel like a template today:
- `TemplateCopyWriter` always opens with `Last {protocol} complete. Top set: …` whenever `lastSession` is non-null.
- `BriefFactsBuilder` takes `sessions.maxByOrNull { it.date }` with no recency cap. Tuesday’s squat is still “last session” Thursday night.
- `factsHash` is `sessionId + recovery + hrv + sleep + rhr`. If vitals never land (builder reads `SLEEP_DURATION` / `HRV` events that ingest often does not write), hash stays the same or the copy stays the same even when hash changes.
- Evening fires the same writer. There is no AM vs PM story.

Rules:
- **Lead picker.** Each pulse has one lead: the metric or event with the largest change vs that user’s 7-day baseline since the last delivered pulse. Candidates: new sleep session, RHR delta, HRV delta, Body Battery wake vs now, steps vs typical by this hour, new workout in last 18h, logbook recovery status change.
- **Session mention TTL.** Name a lift only if `session.date` is after the previous pulse or the session is the causal link for the lead (“HRV 26 after yesterday’s 320”). Never open with the lift on pulse 2+.
- **Two slots.** See Pulse slots (2026-09-07) below. Do not use Garmin sleep score or Body Battery as the named lead. `SANCTUM` / `SEED` / `CHARGE` / `needMin`.
- **Hash includes slot + leadMetric + leadValue.** Same squat must not suppress a new sleep packet, and must not reprint as the headline.
- **If nothing moved: do not notify.** Update the DECK card in place with the current snapshot. Silence is better than “systems stable.”
- **Builder must read VitalsSnapshot + daily rollups + SANCTUM / CLEARANCE / CHARGE.** Not sparse `biometric_events`.

---

## Pulse slots (2026-09-07)

DECK `NeuralBriefCard` always gets the latest copy for the current slot. Shade is a shorter copy of that packet, and only when delivery policy says so.

### Target wake (ships)

Settings / notification prefs, not a sleep calendar:
```kotlin
TARGET_WAKE_WD    LocalTime?    // Mon–Fri
TARGET_WAKE_WE    LocalTime?    // Sat–Sun
```

Empty → derive from last 14 core nights: median `session.end` split WD/WE if each bucket has ≥4 nights, else one median. Label derived times USUAL, set times TARGET.

`needMin` = SANCTUM personal need for last night’s user (`SleepQuality.md`). Fallback 450 min only if SANCTUM has never scored.

`lightsOut` = `targetOrUsualWake` − `needMin`

Round to 5 min. Do not suggest lights-out in the past tense on a PM pulse — if they already missed it, say SHORT tonight + minutes they can still get before usual wake.

### AM packet

Fire builder at `sleepEnd + 20m` when a core night exists. If no session by `targetOrUsualWake + 20m`, send `MISSING_DATA` training-only packet or stay silent if there is also no session news.

---

## Human copy (DECK + shade) — 2026-09-12

The card is for a runner at 07:36, not a logcat. Telemetry tokens (`SANCTUM`, `SEED`, `WATCH`, `T0`, `TRAINING_COMPLETE`) live on LABS. DECK speaks English first. Numbers stay; jargon drops or moves to a second line the user can ignore.

### Shape
- Headline = the one fact that would change their morning. Usually sleep vs need, or “week is done.”
- One sentence that connects that fact to today.
- Optional second line: last lift in plain words. No (barbell). No letter codes.
- Button = the verb, or omit. Never `TRAINING_COMPLETE // RECOVERY ACTIVE`.

### This morning’s packet, rewritten

**Shipped (gibberish):**
> SLEEP 5h27 / need 7h20 · SANCTUM 82 T0  
> SEED 73 WATCH  
> 325 back squat (barbell) logged  
> Weekly target met (3/3) – RECOVERY.  
> [TRAINING_COMPLETE // RECOVERY ACTIVE]  

**Human:**
> Short night — 5h 27m of 7h 20m.  
> Week is already 3/3. Easy day.  
> 325 squat is in the book.  

**Shade (shorter):**
> Short night — 5h27 of 7h20  
> Week is done. Easy day.  

No button if there is nothing to open. `OPEN OPS` only if a session is still on the table.

### Voice rules

| Do | Don’t |
|---|---|
| 5h 27m of 7h 20m | SLEEP 5h27 / need 7h20 · |
| Short night / On target / Long night | SANCTUM 82 T0 on DECK |
| Morning 73 only if we must show SEED | SEED 73 WATCH |
| 325 squat is in the book | 325 back squat (barbell) logged |
| Week is 3/3 | Weekly target met (3/3) – RECOVERY. |
| Easy day / Legs today — keep it easy | RECOVERY ACTIVE (collides with LOGBOOK + SEED) |
| Sentence case | TRAINING_COMPLETE // |

SEED / SANCTUM / CHARGE stay on the LABS cards that already explain them. DECK may show the CHARGE ring (already there). Do not also narrate SEED when the ring is the tank.

If sleep is ≥45 min under need, that is the headline even if SANCTUM printed high. Do not lead with a quality score that argues with the clock.

### AM shade / card templates (replace the token dump)

`{sleepPhrase}.`  
`{weekOrDayPhrase}.`  
`{liftPhrase?}`  

**sleepPhrase:**
- under need by ≥45m → `Short night — {got} of {need}`
- within 20m → `Slept {got} (need {need})`
- ≥45m over → `Long night — {got}`

**weekOrDayPhrase:**
- hit rate complete → `Week is already {n}/{n}. Easy day.`
- PICKUP → `You still have {Push|Pull|Legs} open.`
- HOLD + scheduled → `{Legs} today — keep it easy.`
- PUSH + scheduled → `{Push} is up.`

`liftPhrase` only if the session is inside the TTL: `{weight} {shortName} is in the book.`

**Shade (8888)** — headline ≤48 chars + one sentence + one action if the action is real.
> Short night — 6h12 of 7h20  
> 325 squat is in the book. Legs today — keep it easy.  
> [OPEN OPS]   // omit if week is already complete

Do not print CHARGE on AM when it equals SEED.

Squat clause: yesterday’s top set may appear in the sentence, not as the only headline, when it explains SEED or today’s stance. Pure ANS morning (no session in 36h): sleep + SEED only.

HOLD (locked): SEED GROUND or HOLD and today is scheduled heavy / C / ME → stance HOLD. Copy recommends holding intensity. Action still OPEN OPS (they decide). LOGBOOK CRITICAL/DELOAD still wins first (existing table).

### PM packet

Default delivery NEED_ONLY. Shade (8889) only if at least one:
- Predicted hours before `targetOrUsualWake` < `needMin` (already short or will be if they sleep now)
- CHARGE ≤ 35 or CHARGE dropped ≥ 30 from SEED
- PICKUP window is open (below)
- Unlogged session completed this afternoon (ask to log — once)

Otherwise: update the card only. No “time to wind down” because it is Tuesday.

Settings override (`PULSE_PM_MODE`):

| Mode | Shade |
|---|---|
| **NEED_ONLY** | default, above |
| **WEEKDAY_CLOCK** | Mon–Fri at `lightsOut − 90m` (clamp 19:30–21:30), even if on track |
| **CUSTOM** | user days + clock |

Card always shows lights-out + CHARGE vs SEED when we have them.
> LIGHTS 22:10 for 7h20 · wake 05:30 WD  
> CHARGE 41 · SEED was 79  

No step shame. No HR_LOAD unless it is why CHARGE dumped (one clause).

### PICKUP (missed day, looks good)

Recommend grabbing a session only when all of these hold:
1. A scheduled training day in the last 7d has no `WorkoutSession` (miss), or weekly hit rate is below their `scheduledDays` count and today is not already a completed session.
2. SEED is CLEAR or WATCH.
3. Last SANCTUM is not a wreck: score ≥ 60 or asleep ≥ 0.85 × `needMin`.
4. LOGBOOK is not CAUTION / CRITICAL / DELOAD.
5. No session logged today.
6. Local time is before 16:00 (AM pulse or early PM). After 16:00, card may mention it; shade does not push PICKUP.
7. They are not already at or above weekly frequency target (2× per muscle / 8–9d still applies — do not pile a fourth session onto a blast week that is already full).

Stance PICKUP. Action OPEN OPS with the next due day type (the miss), not a random pump.

If they look good but the week is already complete: say nothing about pickup. That is not a nag hole.

PICKUP never overrides DELOAD / RECOVER / HOLD from SEED GROUND.

---

## Shade vs card length

If the honest message is already one sentence, shade = card headline + that sentence. Do not pad.

If the card needs drivers (SANCTUM tier, z, squat clause), shade keeps headline + the single most useful sentence. Drivers stay on the card.

Day-type copy: never print bare A / B / C. Map CC letters: A → Push, B → Pull, C → Legs. Optional letter in parens on the card only: Legs (C). Free/Classic uses the template name.

Forbidden in both: weather, streak guilt, “Great sleep!”, readiness, Body Battery, Garmin score, sleep-cause chips we do not persist, session UUIDs, CYBERCRAPP LOG: dump, “the sprawl waits”, “system reboot”, markdown `**`.

Validator rejects copy containing a UUID regex, `SESSION `, or any token not in `BriefFacts`. Polish that fails ships the template.

AM headline must include sleep length when a core night exists (6h12 or SLEEP 6h12 / 7h20). SEED-only headline is a bug.

SEED 72 with LOW confidence (all factors 1.0, no sleep minutes) must not be the headline. Omit SEED or print SEED — until SanctumEngine fed minutes.

---

## Stance rules (deterministic)

Evaluate in this order. First match wins.

| Order | Condition | Stance | Default action |
|---|---|---|---|
| 1 | Recovery status CRITICAL or DELOAD, or 2+ stalls + rising joints | DELOAD | REVIEW DELOAD (existing soft-deload accept path) |
| 2 | SEED GROUND/HOLD and today is heavy / C / ME | HOLD | OPEN OPS — recommend easy |
| 3 | HRV_N ≤ 70% of 7-day mean and last session was heavy legs / high RPE | RECOVER | START WALK or skip today’s intensity |
| 4 | Sleep / SANCTUM wreck (asleep < 0.75 × needMin or SANCTUM < 50) and next session is C / ME | HOLD | OPEN OPS with intensity note |
| 5 | PICKUP gates all true | PICKUP | OPEN OPS on the missed day type |
| 6 | Weight-jump due + LOGBOOK OPTIMAL + SEED CLEAR/WATCH | PUSH | OPEN OPS |
| 7 | Scheduled rest / no miss + vitals fine | LIGHT_DAY | no shade; card only |
| 8 | No night and no session in 48h | MISSING_DATA | card; shade only if PM NEED hits |
| 9 | Else | HOLD | OPEN OPS if scheduled today, else card only |

Do not invent yard work. If session notes or a tagged activity exist, you may cite them. Otherwise stay inside the logbook + vitals.

---

## Copy writer

`TemplateCopyWriter` is the production writer.

**Pattern:**
```
{stanceToken} · {one concrete fact}
{vital vs baseline}. {training fact}.
Today: {one instruction}.
```

Examples the writer must be able to emit without AI:
- **PUSH:** “Cluster hit 20 @ 225 on bench. Jump to 230 next Push, min 11.”
- **HOLD:** “Recovery 57 after 320 squat. Keep today’s work easy.”
- **RECOVER:** “HRV 26 vs 41 baseline. Walk, not C-day.”
- **DELOAD:** “Soft deload is on the table. Same lifts, 3–4 RIR, no partials.”
- **MISSING_DATA:** “No overnight HRV yet. Last session still stands: Pull day logged yesterday.”

### AiPolish may rewrite headline + body only:
- Input is `BriefFacts` + stance + template draft.
- Output must keep every number the template used. Validator rejects any number not present in facts.
- Max 80 words.
- Temperature low.
- If polish fails or returns `ERROR*`, ship the template. Log internally.

---

## AiCore reliability (fix before any “smarter” copy)

Add to `AiCore`:
```kotlin
suspend fun isReady(): Boolean
suspend fun warmup()
```

`AiProvider` implementation:
- `isReady()` = Gemma file exists and engine non-null, or Nano bind succeeded in the last 15 minutes, or cloud allowed and key present.
- `warmup()` called from Application + from a 05:30 worker before the pulse window. Do not warmup on UI thread.
- `generateContent` must throw or return a sealed `AiResult.Failure`. Stop returning `"ERROR: ..."` strings. Callers cannot be trusted to filter poetry from stack traces.
- Circuit breaker: 3 failures / 6 hours → `isReady()=false` until next warmup success.
- Background brief polish: allow cloud only if user has not set local-only and local is not ready. Never `forceLocal=true` unless `isReady()` already passed for local.
- `GeminiNanoClient`: `isSupported()` today creates a model and treats any constructor success as ready. That is why AICore “works” at init and dies at generate. Split supported vs ready. Cache the last successful generate.

Insight processor change:
- Always persist heuristic findings as structured fields.
- AI synthesis is optional and stored separately (`polishedContent`).
- `NeuralBriefWorker` never reads raw AI text unless `polishedContent` passed the validator.

---

## Delivery policy

### Channels

| Channel | Importance | When | ID |
|---|---|---|---|
| `neural_brief_v2` | DEFAULT (not HIGH) | Morning Pulse only | 8888 |
| `neural_pings_channel` | HIGH | Chronos / scheduled session / augment reminder | per task |
| `health_sync_channel` | MIN / no sound / no badge | Foreground service obligation only | 888 |
| Do not create | — | App-open, AI-down, “sync success” | — |

Downgrade Neural Brief from `IMPORTANCE_HIGH`. A morning read is not an alarm. Session reminders can stay high.

### Morning Pulse schedule
- Detected wake = core night `endTime`. Shade at `wake + 20 min`, after quiet-end.
- No session by `targetOrUsualWake + 20 min`: update card. Shade only if there is training news (yesterday session not yet pulsed). Do not wait all morning for sleep.
- Better sleep packet before 11:00 and hash changed: update 8888. Do not post a second.
- Stop relying on 24h periodic + 1h flex as the only timer.

`WorkManager`: `OneTimeWorkRequest` to next valid instant, rescheduled on fire. Stop relying on 24h periodic + 1h flex as the only timer.

### Kill list (P0)
- Any `notify()` from `MainActivity`, `AppNavigation`, or dashboard first-frame.
- `HealthSyncWorker.getForegroundInfo` must use a dedicated MIN channel titled “Syncing health data”, not Neural Brief, and must not run as expedited on app start. Periodic sync stays periodic. Do not call `triggerManualSync` from `onCreate`.
- `scheduleSmartPings()` on app start may schedule. It may not enqueue a one-shot brief with zero delay.
- Test buttons stay on the notification prefs screen only.
- Title string `⚡ NEURAL BRIEF // SYNC_SUCCESS` is deleted.

### In-app vs shade
If the user is in the foreground at pulse time: write the same `BriefCopy` into LABS / dashboard card. Do not also post a shade notification. Shade is for when they are not looking.

---

## Surfaces

| Surface | Job |
|---|---|
| **Shade** | Short packet: headline + one sentence + 1 action. IDs 8888/8889 |
| **DECK NeuralBriefCard** | Full packet for the slot. Up to two extra fact lines + up to two actions. Same chrome. |
| **LABS (optional later)** | Deeper facts / Ask Guide. Not a second home screen. |
| **OPS** | Destination of PUSH / HOLD session actions |
| **Codex VITALS (P2)** | List of delivered briefs + factsHash. Read only |
| **Neon Guide** | Conversational follow-up. Not the shade author |

Do not put the pulse on Codex as the live cockpit.

---

## Engagement rules

Notifications earn the next one.
- If the last 3 pulses were opened 0 times, drop to 3 days/week (Mon/Wed/Sat) until an open.
- If user taps DELOAD and accepts, suppress intensity pings for the deload window.
- Session reminders remain on their own calendar (already locked in SubProtocols). Pulse does not replace Chronos.
- Ping budget LOW = pulse only. MEDIUM = pulse + session reminders. HIGH = those + exception (HRV crash) with 6h cooldown.

This is how we stay less noisy than Fit while feeling more useful.

---

## Build order

### P0 — stop the bleeding (ship this week)
- Sealed `AiResult`. No more `"ERROR:"` return strings from Nano / Gemma / Provider.
- `InsightProjectionProcessor` refuses to persist AI failures. Heuristic content only on failure.
- `NeuralBriefWorker` builds copy from `RecoveryEngine` + last session + latest `biometric_events`. Template writer only.
- Kill app-open notifies: health sync channel split, no one-shot brief on `onCreate`.
- Title + actions rewritten. ID 8888 reused.
- 1/day DataStore guard.

*Acceptance:* opening the app produces zero shade rows. Morning (or test button) produces a brief that cites a real lift or a real vital, never AICore.

### P1 — Fit-quality pulse
- `BriefFactsBuilder` + `BriefStanceResolver` as domain objects with tests.
- Wake-anchored schedule + quiet hours.
- Foreground detection → in-app card instead of shade.
- `AiCore` `isReady` / `warmup` + 05:30 warmup worker.
- Optional polish behind validator + circuit breaker.

*Acceptance:* on a day after 320 squat + low HRV, the pulse matches the quality bar above without cloud.

### P2 — memory and tighter loops
- Persist `MorningBrief` rows. Codex VITALS can list them.
- Guide “ask about this brief” injects the facts packet (Codex P4 already wanted this).
- Exception channel: HRV crash midday, only if pulse stance was PUSH and new data contradicts it.
- Engagement downshift after ignored pulses.

---

## Test matrix

| Case | Expected |
|---|---|
| AICore unbound | Template brief, no error text |
| Gemma missing, Nano throws | Same |
| Local-only setting + local down | Template, no cloud |
| Last session 320 squat, HRV 26 vs 41 | RECOVER or HOLD, cites both numbers |
| Cluster 20 @ 225 bench, HRV fine | PUSH, names the jump |
| Soft deload already accepted | Pulse says deload rules, no “hit it hard” |
| App opened at 09:37 | No new notification |
| Pulse due while app open | LABS card only |
| Sleep data arrives 08:40 after 07:00 pulse | Update 8888 if hash changed |
| Test button on prefs | One brief, marked test, still cannot contain ERROR |

---

## Files to touch (implementation map)

Do not boil the ocean. Prefer these:
- `app/.../biohacking/AiProvider.kt`
- `app/.../biohacking/GeminiNanoClient.kt`
- `core/ai-litert/.../GemmaClient.kt`
- `core/domain/.../ai/AiCore.kt`
- `core/data/.../processor/InsightProjectionProcessor.kt`
- `feature/notifications/.../workers/NeuralBriefWorker.kt` (rewrite)
- `feature/notifications/.../NeuralBriefManager.kt` (title, channel importance, actions)
- `feature/notifications/.../SmartPingScheduler.kt` (schedule ≠ fire)
- `app/.../MainActivity.kt` (stop treating start as a notify event)
- `feature/health/.../HealthSyncWorker.kt` (MIN channel, no start-expedite)
- **New:** `core/domain/.../brief/BriefFacts.kt` + builder + stance + template writer
- Tests next to the resolver / writer

Workout DAO already has what P0 needs if you query last session + recent `biometric_events`. Do not call `getFullHistory()`.

---

## Locked calls (2026-08-30)

1. Deterministic brief is the product. AI polish is a bonus.
2. Error strings never leave the process as user copy.
3. App-open is silent.
4. One pulse per local day, update-in-place allowed before 11:00.
5. Training-first. Fit is the quality bar, not the feature set.
6. Neural Brief channel is DEFAULT importance.
7. Same copy lands as text inside the existing DECK `NeuralBriefCard`. Codex only stores history later.
8. Soft-deload language stays consistent with `CyberCrapp.md` / `CCProgram.md`.
9. DECK chrome is frozen (`DashboardScreen.kt`): RUNNER header, grid, STEPS / KCAL / STREAK, terminal, LOG SESSION, CODEX on the bar. No ENG/FOC/MOD chips on DECK. Those are self-report 1–10 defaults in `BiohackingData`, not live vitals.
