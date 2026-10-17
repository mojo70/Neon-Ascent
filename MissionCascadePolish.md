# Neon Ascent – Mission Cascade Polish

**Status**: Design lock  
**Updated**: October 17, 2026  
**Repo**: `mojo70/Neon-Ascent`  
**Studio pack**: `MissionCascadePolishGeminiPrompts.md`  
**Depends on**: `DirectiveIntake.md` (single door, Mentor default), `GoalStack.md` (V3 hierarchy)

No new hierarchy. No fifth wing. Progress stays on Missions.

---

## Product call

Accepting a Mentor proposal must land the Directive and its first Missions (and opening Tasks) in one step.  
Parent selection for a manual Mission must be a clear picker of active Directives, not a raw ID.  
Standalone remains explicit.  
No second manual linking step after deploy.

---

## Current state

- `acceptProposals` in `AscensionForgeViewModel` already inserts the Directive, then Missions (with `directiveId` set), then Tasks.
- Mission create path uses chips of active Directives + “NONE (STANDALONE)”.
- Task parent is still loosely tied (`parentId` sometimes receives a directive id).

---

## Polish lock

### Cascade on accept

- When the user taps “Accept proposals” (or equivalent) in Mentor mode:
  1. Insert `AscensionDirective` with all locked Big Goals fields (title, description, targetEndDate, priorityTier, outcomeStatement, affirmation, visualUri, Self Map anchor, linked attributes).
  2. Insert each proposed Mission with `directiveId` set to the new Directive.
  3. Insert each proposed Task under its Mission (not under the Directive).
- One dopamine event after the full cascade succeeds.
- Navigate to the new Directive detail.
- Soft density check still runs after the Directive insert.
- If the user has switched to Form and hits plain Save, write Directive only (no auto-cascade) unless proposals were explicitly accepted earlier in the session.

### Parent picker (Mission)

- Chips (or equivalent) of **active** Directives only.
- Explicit “NONE (STANDALONE)” option.
- Selected parent is stored as `directiveId` on the Mission.
- No raw ID text field.
- Empty state: “No active directives. Mission will be standalone.”

### Parent picker (Task)

- Prefer parent Mission when creating a Task from Forge.
- If the current flow only has Directives, show active Missions under the selected Directive, or a clear “parent Mission” picker.
- Do not store a Directive id in a Task’s `parentId` when a Mission id is available.
- Standalone Task remains possible but is not the default when a Mission context exists.

### Copy / UX

- Accept button label stays cyberpunk-terminal (e.g. “ACCEPT_PROPOSALS” or “DEPLOY_CASCADE”).
- After success, the Directive detail shows the new Missions without requiring a refresh or second save.
- No silent failures: if Mission or Task insert fails, surface a short error and do not claim full deploy.

---

## Out of scope

- New entity types
- Changing contribution weights or progress math
- Auto-cascade on plain Form save
- Fifth wing
- Editing existing Missions from the Codex panel

---

## Tests (minimum)

1. Accept proposals writes Directive + at least one Mission with correct `directiveId`.
2. Tasks land under their Mission, not the Directive.
3. Mission picker lists only active Directives + explicit standalone.
4. Plain Form save writes Directive only.
5. Density check still fires after cascade.
