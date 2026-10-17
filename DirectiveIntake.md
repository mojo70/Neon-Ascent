# Neon Ascent – Directive Intake

**Status**: Design lock  
**Updated**: October 17, 2026  
**Repo**: `mojo70/Neon-Ascent`  
**Studio pack**: `DirectiveIntakeGeminiPrompts.md`  
**Depends on**: `GoalStack.md` (V3 winner), `BigGoals.md` (Codex glance + additive fields)

DECK frozen. No fifth wing. No new hierarchy. Single create door.

---

## Product call

Mentor chat is the default Directive genesis surface.  
Form view is always one tap away.  
Settings controls the default mode per user.  
Soft density guidance only. Never block.  
Self Map anchor is the primary domain signal.  
Codex remains the daily glance. Creation stays in Forge.

```
Entry → Intake surface (Mentor default | Form)
     → Clarify what / when / why
     → Optional proposals
     → Write AscensionDirective (+ optional cascade)
     → Codex panel updates
```

---

## Locked decisions

| Decision | Value |
|---|---|
| Default intake | Mentor chat window |
| Escape hatch | In-session switch to form view (always available) |
| Preference | Settings toggle `directiveIntakeMode`: `MENTOR` (default) \| `FORM` |
| Active limit | None. Soft non-blocking note only |
| Soft thresholds | ≥4 active with targetEndDate inside next 90 days, or ≥3 A-tier |
| Domain primary | Self Map anchor (`anchoredSelfItemId` / category of the item) |
| Domain secondary | Optional free tag or domain tag (user-overridable) |
| No new enum | Do not add a first-class `lifeDomain` field |
| Single door | Forge / Directive intake only. Redirect or freeze remaining V1/V2 create surfaces |
| Progress | Stay on Missions. Do not invent Directive-level progress math |

---

## Intake surface behavior

### Mentor mode (default)

- Opens as chat (full screen or terminal-style sheet, cyberpunk HUD).
- Header shows mode switch chip: “Switch to Form”.
- Mentor starts with 1–2 clarifying questions focused on:
  - What (specific outcome)
  - By when (date or horizon)
  - Why it matters (Self Map anchor or linked attributes)
- Does not dump a full plan on first turn.
- User can accept proposals, accept-and-edit, or continue chatting.
- On accept: write Directive with locked Big Goals fields + optional Missions/Tasks cascade.
- Dopamine event on successful deploy.

### Form mode (manual or switched)

- Same surface switches to focused Directive form.
- Field order: Title, Description/Objective, Target date (date picker), Priority tier (A/B/C, default B), Outcome statement, Affirmation, Visual pick, Self Map anchor, Linked attributes.
- Optional “Ask Mentor” injects suggestions into fields without forcing chat.
- Save writes Directive only unless user explicitly requests cascade.

### Shared post-save

- Codex panel refreshes immediately (active Directives, A→B→C, soonest date first).
- Navigate to Directive detail or back to Codex.
- Soft density check runs after write. If threshold hit, dismissible banner on detail or Codex panel. Never blocks create.

---

## Entry points (all land on same intake surface)

- Codex empty state / “Forge one”
- OPS / LABS “New Directive”
- Self Map item overflow → “Anchor a Directive”
- Insight / Neural Brief deep-link (prefill title/description when available)

---

## Settings

Preference stored in existing preferences / DataStore:

- Key: `directiveIntakeMode`
- Values: `MENTOR` (default) | `FORM`
- Takes effect on next intake open. Current session respects in-session switch.

---

## Density guidance

After insert:

- Count active Directives where `targetEndDate` is non-null and ≤ today + 90 days.
- Count active A-tier.
- If either threshold met, show one dismissible note. Copy example: “You have several near-term Directives. Want to review priority or archive one?”
- Group density indicator quietly by Self Map category of anchored items when present. No hard per-domain cap.

---

## Out of scope

- New goal entity or hierarchy
- Fifth Codex wing
- Create/edit controls on Codex panel
- Hard blocks or forced archiving
- New `lifeDomain` enum
- Handwriting capture
- Changing Mission/Task model beyond parent picker cleanup
- V1/V2 table drops

---

## Call-site notes

- `AscensionForgeScreen` / `AscensionForgeViewModel` become the single intake. Add Mentor-default path and mode switch.
- `AspirationCreationScreen` must be redirected or removed from navigation.
- Parent linking for Missions/Tasks needs a real picker (not raw ID).
- Codex panel (per BigGoals) consumes the written Directive fields with no create surface.
