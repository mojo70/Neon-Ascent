# Neon Ascent – Greased Pig

**Status**: Design lock  
**Updated**: October 17, 2026  
**Repo**: `mojo70/Neon-Ascent`  
**Studio pack**: `GreasedPigGeminiPrompts.md`  
**Depends on**: DirectiveIntake, DirectiveShade, CodexDirectivesPolish, MissionCascadePolish, SelfMapIntake (all shipped)

Release theme: reduce friction. No new hierarchy. No fifth wing. No new scores.

---

## Product call

The morning loop closes.  
The highest-frequency path (workout) gets lighter.  
Status and Self Map speak the same sentence.  
Priority is visible without scrolling.  
Non-training surfaces appear only when the day asks for them.

```
Shade → DECK (status + today’s Directive) → OPS or Directive
                ↓
         Avatar (status ↔ map bridge)
                ↓
         Codex (pinned next Directive)
```

---

## Locked slices

### 1. DECK — today’s Directive chip

- When the morning brief names a Directive (or a clear soonest A-tier / near-term exists), show a single compact chip on the NeuralBriefCard or directly under it.
- Content: status token (if any) + truncated title + days left.
- Tap → Directive detail.
- If no Directive is worth naming, chip is absent. Never a empty placeholder.
- Does not replace the brief body. Does not add a second action button on the shade.
- Source: same selection rules as DirectiveShade (soonest A-tier ≤90d, else soonest near-term with open Mission, else omit).

### 2. Workout — progressive disclosure (toggle)

- **Default remains the current logging layout.** Do not replace it.
- Add a Settings toggle (or in-session control) `workoutLayoutMode`: `CLASSIC` (default) | `FOCUS`.
- CLASSIC = existing screen, unchanged. All current controls, cues, history, and density stay as they are today.
- FOCUS = progressive disclosure:
  - Primary zone: current exercise, current set, rest timer, complete.
  - Secondary (collapsed or one tap): cues, history, last session, protocol notes.
- THROTTLE / GROUNDED status banner appears in **both** modes when the stance is not OVERDRIVE/NOMINAL. Dismissible for the session. Copy example: “THROTTLE — RHR elevated”.
- On session complete, if the session was linked to a Directive (via pulse task or explicit anchor), offer one return chip: “Back to [Directive]”. Applies to both modes.
- Toggle can be changed in Settings and takes effect on the next session open. Optional in-session switch is allowed but must not lose set state.
- No removal of CLASSIC in this release. Both stay shippable.

### 3. Avatar — status ↔ map bridge

- On the holographic avatar / Self Map hub, one persistent line under the status badge:
  - OVERDRIVE → “Clear to push.”
  - NOMINAL → “Normal pace.”
  - THROTTLE → “Ease intensity. Map shows what you’re protecting.”
  - GROUNDED → “Pull back. Rest or Zone 2.”
- No new ring. No new score. Existing neon accents only.
- Optional: highlight the highest-importance Self Map item for 2 seconds on first open of the day when status is THROTTLE or GROUNDED. Skip if biometric lock is active and not unlocked.

### 4. Codex — pinned next Directive

- Directives panel keeps horizontal scroll and existing sort.
- The first card (soonest A-tier, or soonest near-term) is visually pinned or given a “NEXT” marker so it cannot be missed off-screen.
- Density one-liner unchanged.
- No create/edit on the panel.

### 5. Contextual non-training nudge

- Only when status is THROTTLE or GROUNDED, or sleep was short (≥45 min under need).
- One quiet line on DECK or the brief card: “Short sit available” or “Altar open” (deep-link to Altar).
- Never on OVERDRIVE. Never more than one nudge. Dismissible for the day.
- Does not create a new notification. In-app only.

### 6. Forge entry clarity

- When opening Forge for a new Directive, DIRECTIVE tab is selected and the mode switch is primary.
- MISSION / PULSE tabs remain available but are visually secondary until a Directive exists in the session or the user explicitly switches.
- Prevents accidental type change before the first deploy.
- No change to cascade or parent picker logic.

---

## Out of scope

- New goal hierarchy or entity
- Fifth Codex wing
- Workout logging full rewrite
- New Neural Brief channel or ID
- Mood check-in, review reminders, Chronicle tagging (Self Map V2 remainder)
- Cloud sync
- Changing density thresholds or shade driver priority

---

## Success signals

- Morning open → Directive detail in one tap when a Directive is named.
- Logging screen CLASSIC remains default. FOCUS shows current set + rest without scrolling. User can switch.
- Status badge and map share one sentence.
- Soonest Directive is visible on Codex open without horizontal scroll.
- THROTTLE/GROUNDED days surface Altar once; OVERDRIVE days do not.

---

## Tests (minimum)

1. Brief with named Directive → DECK chip appears and deep-links.
2. Brief with no Directive → chip absent.
3. THROTTLE session shows banner in both layouts; OVERDRIVE does not.
4. Session complete with linked Directive offers return chip (both layouts).
5. Avatar bridge line matches status token.
6. Codex first card carries NEXT marker.
7. Nudge appears only on THROTTLE/GROUNDED or short night, dismisses for the day.
8. New Forge open defaults to DIRECTIVE tab.
