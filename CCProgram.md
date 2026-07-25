# CCProgram.md – App Implementation & User Success Rules

**Version**: 2.1  
**Last Updated**: July 25, 2026  
**Purpose**: How CyberCrapp (and the broader General Lifting & Logging engine) is implemented in the Kotlin / Jetpack Compose app so users are set up for success.  
Training protocol details live in `CyberCrapp.md`.

---

## High-Level Architecture

- **General Lifting & Logging** is the core engine.  
- **CyberCrapp** is a selectable high-intensity Powerbuilding protocol that runs inside that engine.  
- Users can run free-form logging, Classic mode, or full CyberCrapp mode.  
- All rules (progression, rotation, deload, recovery) are enforced by the app, not left to memory.

---

## Onboarding & Personalization

At intake the app collects:

- Height, weight, age, gender
- Somatotype (slider with visuals)
- Experience level (Novice / Intermediate / Advanced)
- Basic strength / endurance / agility estimates
- Injuries / movement limitations
- Available training days and minutes per session
- Goal (bulk / cut / recomp / maintain)

**Experience Level Effects**

| Level          | Default Recommendation                              | CC Availability |
|----------------|-----------------------------------------------------|-----------------|
| Novice         | Full-body or Upper/Lower, mostly straight sets      | CC Lite or delayed introduction |
| Intermediate   | 4-day Upper/Lower or 3-day A/B/C                    | Full CC available |
| Advanced       | Flexible 3–6 day rotation                           | Full CC + advanced auto-nudges |

Somatotype applies light modifiers (volume, stretch duration, macro targets, finishers). Injuries trigger automatic exercise substitutions.

---

## Core Data Model (Room)

Key entities the app must maintain:

- `UserProfile` – all intake + ongoing preferences
- `Exercise` – name, GIF asset, description, cues, primary muscles, injury tags, substitution list, default rep range
- `WorkoutTemplate` – protocol type (CyberCrapp, Classic, Free), split structure, exercise mapping
- `WorkoutLog` / `ClusterLog` – weight, total reps, RIR, actual rest times, session RPE, notes
- `RecoveryScore` – calculated from recent logs, RIR trends, joint notes, optional future HRV/sleep
- `ProgressionState` – per-exercise best cluster, consecutive misses, current weight, rotation history

---

## Progression Engine (Must Enforce)

1. Weight is held constant while the user chases total cluster reps.
2. When the user hits the top of the defined range for that exercise, the app prompts a weight increase on the next session.
3. After a weight increase, the first session only requires hitting the **minumum** of the range.
4. If the user fails to beat the previous best on the same exercise **two consecutive times**:
   - App flags the exercise as stalled.
   - Offers 2–3 similar substitutes from the library.
   - Archives the old log history (still visible for comparison).
   - Old exercise can be rotated back in later if the new one stalls.
5. Manual “Force Rotate” is always available for variety.

Rep ranges are stored per exercise type and shown clearly in the workout UI.

---

## In-Workout Experience (CyberCrapp Mode)

**Theme**: Dark background with neon accents (green / purple).

**Key Screens & Behaviors**:

- **Exercise Header** – Tappable. Shows name + looping muted GIF thumbnail. Tap opens full modal with larger GIF, description, and bulleted cues.
- **Rest-Pause Cluster Panel**
  - Progress indicators for Mini-set 1 / 2 / 3
  - Large live rep counter
  - Running total cluster reps with comparison to previous best (“+2 vs last”)
  - 15-second rest timer (user adjustable 10–25 s for the session)
  - RIR quick chips (0 / 1 / 2)
  - One-tap log after each mini-set
- **Lengthened Partials Card** – Auto-unlocks after final mini-set. Clear instruction + rep counter.
- **Loaded Stretch Timer** – 30–45 s circular timer with breathing guidance.
- **Warm-up Guidance** – App suggests 1–2 (or 3) progressive sets based on planned working weight.
- **Rest between exercises** – Timer defaults to 2–3 minutes (longer for heavy compounds).

All logs write immediately to Room and feed the recovery score.

---

## Recovery Score & Auto-Regulation

Simple 0–100 score visible on the dashboard with a short explanation of drivers (recent RIR average, session RPE trend, joint notes, etc.).

**High recovery**:
- Optional extra accessory volume or light cluster on accessories
- Possible volume nudge on primary work

**Low recovery / stagnation / joint notes**:
- Soft deload suggestion
- Reduced intensity targets
- Early cruise prompt

Blast length is visible (“Blast Week X”) but the actual length is variable and driven by recovery + progression, not a fixed calendar.

---

## Deload / Cruise Implementation

When a soft deload is triggered:

- Same exercises stay in the template.
- Rest-pause clusters convert to 2–3 straight sets.
- Target RIR moves to 3–4.
- Lengthened partials and loaded stretches are minimized or removed.
- Duration defaults to one full rotation + 1–2 extra rest days.
- User can accept, delay, or request a longer reset.

The app makes the change automatic once accepted so the user does not have to remember the rules.

---

## Accessory Handling

- Primary compounds always run full CyberCrapp structure.
- Standard accessories default to 1–2 hard sets in the appropriate rep range.
- When recovery score is high, the app can offer an optional “light cluster” upgrade for that accessory.
- Specialization protocols (high-volume arm work, etc.) are separate selectable modes that can override normal rules.

---

## Macros (Simple Numbers)

Dashboard card only.  
TDEE calculated from profile + activity factor derived from the chosen protocol and availability.  
Protein 1.8–2.2 g/kg. Carbs and fats adjusted by goal and somatotype.  
No full meal plans in V2 — just clear daily targets.

---

## Endurance Hybrid

Optional run-focused add-on:
- 2–3 Zone 2 runs
- 1 HIIT session  
Can be scheduled around lifting days. The app respects recovery and does not stack high-intensity runs on heavy leg days when possible.

---

## User Success Guardrails

The app must:

1. Always show the current best for the exercise being performed.
2. Clearly indicate when a weight increase is due.
3. Automatically handle rotation after two consecutive misses.
4. Surface recovery score and explain it in plain language.
5. Make deload acceptance one-tap.
6. Keep form cues and GIFs one tap away during the session.
7. Never force a novice into full rest-pause intensity without an on-ramp.
8. Preserve log history even after exercise rotation so progress remains visible.

---

## Implementation Priority (Suggested)

1. Room entities + exercise library seed data (including Jerry Curls final description and all rep ranges).
2. Progression & rotation engine.
3. CyberCrapp in-workout screen (dark + neon, rest-pause panel, partials, stretch timer).
4. Recovery score calculation + dashboard visibility.
5. Soft deload automation.
6. Warm-up suggestions and rest timers.
7. Macros card + basic progress charts.
8. Endurance hybrid scheduling.

---

## Summary

`CyberCrapp.md` defines the training rules.  
`CCProgram.md` defines how those rules are enforced, surfaced, and adapted inside the app so the user does not have to remember them.

The combination creates a system that is both faithful to high-intensity progressive training and practical for real-world users of varying experience levels.
