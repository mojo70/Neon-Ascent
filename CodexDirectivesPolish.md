# Neon Ascent – Codex Directives Panel Polish

**Status**: Design lock  
**Updated**: October 17, 2026  
**Repo**: `mojo70/Neon-Ascent`  
**Studio pack**: `CodexDirectivesPolishGeminiPrompts.md`  
**Depends on**: `BigGoals.md` (panel contract), `DirectiveIntake.md` (density + forge deep-link)

No fifth wing. No create/edit controls beyond the existing forge deep-link. No progress math on Directive.

---

## Product call

The Codex directives panel is the daily Pease glance.  
It must be scannable in under two seconds.  
Active Directives only. Sort locked. Density note stays dismissible.  
Forge is a deep-link only.

---

## Current state (HEAD)

- `CodexDirectivesPanel` + `CodexDirectiveCard` exist in `CodexScreen.kt`.
- Density banner (amber warning + dismiss) is wired from `CodexViewModel`.
- Sort via `AscensionDirectiveComparator` (A → B → C, soonest date, undated last).
- Empty state + “+ FORGE” deep-link present.
- Cards show title, tier badge, date + days, one-line outcome/affirmation/description, “VISUAL ATTACHED” text.

---

## Polish lock

### Layout

- Keep the panel as a persistent header card above the wing switcher (or equivalent prominent placement). Do not bury it.
- Cards stay horizontal scroll. Tighten vertical padding so 2–3 cards are visible without scrolling the whole Codex page.
- Card width ~160–180 dp. Fixed height so the row doesn’t jump.

### Card content (order)

1. Priority badge (A / B / C). Hide if null. A uses stronger accent (NeonPink or existing high-priority color).
2. Title (uppercase, 1 line, ellipsis).
3. Date line: `12d` or `DUE TODAY` or `3d OVER` or `NO DATE`. Prefer short form over full date string when space is tight.
4. One-line subtitle: outcomeStatement → affirmation → description (truncated).
5. Optional thumbnail: if `visualUri` is a local content/file URI that Coil/AsyncImage can load, show a 32 dp image. Fallback remains the small “VISUAL” icon + text. Never crash on bad URI.

### Density banner

- Keep dismissible.
- Tighten copy to one line when possible: `3 near-term · 2 A-tier. Review priority?`
- Amber/warning treatment stays. One tap dismisses. Does not block.

### Empty state

- “No directives. Tap to forge one.” when forge deep-link is available.
- Otherwise “No directives. Forge one in OPS.”
- Not a void.

### Interaction

- Tap card → existing Directive detail (read-only entry fine).
- “+ FORGE” remains a deep-link to the intake surface. No inline create or edit on the panel.
- No long-press menus in this polish.

### Sort (reaffirm)

`AscensionDirectiveComparator`: A then B then C (null = B), then soonest `targetEndDate`, undated last. Do not change.

---

## Out of scope

- Fifth wing
- Create/edit forms on the panel
- Progress bars or Mission roll-up on the card
- Cloud image sync
- Changing density thresholds
- Neural Brief shade (separate lock)

---

## Tests (minimum)

1. A-tier with date sorts before B and before undated.
2. Null priority sorts as B.
3. Panel receives only active Directives.
4. Short date format renders for future, today, overdue, and null.
5. Bad visualUri does not crash the card.
