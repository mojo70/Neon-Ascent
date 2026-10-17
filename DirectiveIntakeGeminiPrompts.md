# Neon Ascent – Directive Intake Gemini/Android Studio Prompts

**Status**: Ready for Studio  
**Updated**: October 17, 2026  
**Spec**: `DirectiveIntake.md`  
**Depends on**: GoalStack (V3) and BigGoals fields already present on `AscensionDirectiveEntity`.

Run prompts in order. Reassert after each major change. Do not invent progress math on Directive. Do not add a fifth wing. Do not hard-cap creates.

---

## PROMPT 0 — Kill list + reassert

```
You are working in mojo70/Neon-Ascent.

Reassert:
- V3 Ascension (Directive → Mission → Task) is the only product goal stack.
- Mentor chat is the default Directive intake. Form is one tap away. Settings controls default mode.
- Soft density note only. Never block create.
- Self Map anchor is primary domain. Optional tag secondary. No new lifeDomain enum.
- Codex is glance only. Creation stays in Forge.
- No fifth wing. No smash-merge of Room files.

First actions:
1. Confirm AspirationCreationScreen is not reachable as a primary create path. Redirect any remaining entry to AscensionForge or remove from nav.
2. Confirm no other create surface writes AscensionDirective except Forge / the new intake.
3. List current call sites that insertDirective / open Forge.

Do not change models yet. Report only.
```

---

## PROMPT 1 — Preference + mode switch skeleton

```
Add directiveIntakeMode preference.

- Store in existing preferences / DataStore (or BriefPreferencesDataStore / HealthPreferencesDataStore if that is the pattern; prefer the cleanest existing one).
- Key: directiveIntakeMode
- Values: MENTOR (default) | FORM
- Expose read/write from a small repository or DataStore wrapper that Forge can inject.

In AscensionForgeScreen / ViewModel:
- Read the preference on init.
- Add in-session mode switch (chip or button in header): “Switch to Form” / “Switch to Mentor”.
- Switching does not write the preference unless the user explicitly saves settings. Preference only sets the default on next open.
- Header title remains cyberpunk terminal style (e.g. DIRECTIVE_GENESIS or FORGE // PROTOCOL_INTAKE).

Do not implement the full Mentor chat yet. Just the preference read + mode state + switch UI that toggles between two placeholder containers.
```

---

## PROMPT 2 — Mentor chat default surface

```
Implement Mentor mode as the default intake surface for a new Directive.

Requirements:
- When mode == MENTOR (or preference is MENTOR and no in-session override), show a chat-style column (messages list + input).
- Reuse or adapt the existing NeonMentorUseCase / mentor dialogue path already present in AscensionForgeViewModel.
- Opening prompt must ask 1–2 clarifying questions about:
  - What (specific outcome)
  - By when (date or horizon)
  - Why it matters (offer Self Map anchor or linked attributes)
- Do not auto-propose full Missions/Tasks on the first assistant message.
- User can continue chatting.
- Provide an explicit “Accept proposals” path that writes:
  - AscensionDirective with title, description, targetEndDate, priorityTier (default B), outcomeStatement, affirmation, visualUri, anchoredSelfItem when available.
  - Optional cascade of proposed Missions and first Tasks (only if user accepted them).
- Fire the existing dopamine event on successful deploy.
- After write, run the soft density check (see PROMPT 4) and navigate to Directive detail or back to Codex.

Keep cyberpunk terminal aesthetic. No generic Material chat bubbles if it fights the HUD.
```

---

## PROMPT 3 — Form view + field order

```
Implement the Form view on the same intake surface.

When mode == FORM (or user switched):
- Show focused Directive form.
- Field order exactly:
  1. Title
  2. Description / Objective
  3. Target date (prefer DatePicker over free-text parse)
  4. Priority tier chips (A / B / C, default B)
  5. Outcome statement (optional multi-line)
  6. Affirmation (optional single line)
  7. Visual pick / URI (optional, local only)
  8. Self Map anchor picker (reuse existing AnchorToSelfMapSection if present)
  9. Linked attributes (S.P.E.C.I.A.L. chips)
- Optional “Ask Mentor” button that can call the mentor use case and inject suggested outcome / affirmation into the fields without forcing the chat view.
- Save writes Directive only (no auto-cascade) unless the user has explicitly accepted proposals earlier in the session.
- Same post-save navigation and density check as Mentor path.

Ensure switching from Mentor to Form (and back) preserves any already-entered title/description/date/priority so the user does not lose work.
```

---

## PROMPT 4 — Soft density check

```
After a successful Directive insert (both paths):

- Query active Directives.
- Count those with targetEndDate != null && targetEndDate <= today.plusDays(90).
- Count those with priorityTier == A.
- If either count meets threshold (≥4 near-term or ≥3 A-tier), set a one-shot UI flag for a dismissible banner.
- Banner copy (example): “You have several near-term Directives. Want to review priority or archive one?”
- Banner appears on the Directive detail screen or Codex panel. One tap dismisses. Never blocks the create that just happened.
- Optionally surface a quiet density line grouped by Self Map category of anchored items (e.g. “Body: 2”). No hard per-domain cap. No new enum.

Do not invent Directive-level progress. Do not auto-archive.
```

---

## PROMPT 5 — Parent picker cleanup + entry points

```
1. Replace raw parentDirectiveId text field (in Mission create path inside Forge) with a real picker of existing active Directives. Same for Task parent if it is still raw.
2. Wire entry points so they all land on the intake surface:
   - Codex empty state / “Forge one” CTA
   - Any OPS / LABS “New Directive” button
   - Self Map item overflow “Anchor a Directive” (pre-select the anchor)
   - Insight / Neural Brief deep-link (prefill title/description when present)
3. Confirm AspirationCreationScreen is either removed from navigation or immediately redirects to the new intake.

Do not add create controls to the Codex panel itself.
```

---

## PROMPT 6 — Settings UI

```
Add a simple Settings control for directiveIntakeMode.

- Segmented button or radio: Mentor (default) / Form.
- Lives in the existing Settings / VISUAL_STIMULI or preferences screen (Steve Mode area is fine if that is where other toggles live; otherwise a small Goals or Intake section).
- Writing the preference does not affect the currently open intake session.
- Label clearly: “Directive intake default”.

No other Settings changes.
```

---

## PROMPT R — Reassert + smoke

```
Reassert the lock:

- Mentor is default. Form is one tap. Settings controls next-open default.
- Soft density only. Never blocks.
- Self Map anchor primary. No new lifeDomain enum.
- Single door. Codex glance only.
- V3 fields (targetEndDate, priorityTier, outcomeStatement, affirmation, visualUri, anchoredSelfItem) are written on both paths.

Smoke:
1. Open intake with preference MENTOR → chat appears.
2. Switch to Form mid-session → fields appear, prior text preserved.
3. Save a Directive with date + A-tier + Self Map anchor.
4. Confirm Codex panel shows it.
5. Create enough near-term ones to trigger the dismissible note.
6. Confirm AspirationCreationScreen is not a reachable primary path.

Report any remaining dual-create paths.
```
