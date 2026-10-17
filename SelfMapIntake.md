# Neon Ascent – Self Map Intake

**Status**: Design lock  
**Updated**: October 17, 2026  
**Repo**: `mojo70/Neon-Ascent`  
**Studio pack**: `SelfMapIntakeGeminiPrompts.md`  
**Depends on**: `SelfMap.md` (V1 shipped, V2 section), existing Self Map canvas + repository

Same pattern as Directive intake. Chat is default. Form is always one tap away. Settings controls the default. No forced conversation on returning users.

---

## Product call

Mentor / guided chat is the default Self Map item genesis surface.  
Manual form (or direct canvas add) is always one tap away.  
Settings controls the default mode per user.  
User confirms every proposed item.  
After finish or skip, land on the radial canvas with accepted items already placed.

```
Entry → Intake surface (Chat default | Form)
     → One question at a time
     → Propose items (name, category, importance, note)
     → User accepts / edits / rejects each
     → Insert via SelfMapRepository
     → Radial canvas
```

---

## Locked decisions

| Decision | Value |
|---|---|
| Default intake | Guided chat |
| Escape hatch | In-session switch to form / manual add (always available) |
| Preference | Settings toggle `selfMapIntakeMode`: `CHAT` (default) \| `FORM` |
| Returning users | Do not force the conversation. Preference + explicit “Guided start” only. |
| Confirmation | AI proposes; user confirms every item before insert. |
| Sequence | One question at a time. Skippable at any point. |
| Landing | Radial canvas with accepted items placed. |
| Resume | Persist lightweight session state so an interrupted chat can resume. |

---

## Intake surface behavior

### Chat mode (default)

- Opens as chat (full screen or terminal-style sheet on the Avatar / Self Map entry).
- Header shows mode switch: “Switch to Form”.
- One question at a time. Sequence (skippable):
  1. “Tell me a little about who you are right now.”
  2. People who matter most (suggest 2–4, propose importance).
  3. Stuff or roles you’d miss if gone.
  4. Ideas, values, or opinions that feel like part of you.
  5. Confirm or adjust pre-seeded Body & Senses items.
- AI proposes items: name, category, suggested importance (0.4–0.9), short note.
- User can accept, accept-and-edit, or reject each proposal.
- On accept: insert via existing `SelfMapRepository`.
- Tone: curious, non-judgmental, meta-awareness. No invented emotion scores.

### Form mode (manual or switched)

- Same surface switches to the existing manual add / wizard fields.
- Fields: name, category, importance, notes.
- Optional “Ask Mentor” can suggest a name + importance without forcing chat.
- Save inserts the item directly.
- Switching modes preserves any already-entered name/category/importance.

### Shared post-save

- Radial canvas updates immediately with the new item(s).
- No second confirmation step after accept.
- Pre-seeded Body & Senses items remain; chat may offer to adjust them, never auto-delete.

---

## Entry points

- First open of the Self Map canvas (if preference is CHAT and intake not yet completed).
- Explicit “Guided start” / “Add with Mentor” on the canvas.
- LABS entry card deep-link (respects preference).
- Settings can force FORM so first open skips chat.

---

## Settings

- Key: `selfMapIntakeMode`
- Values: `CHAT` (default) | `FORM`
- Takes effect on next intake open. Current session respects in-session switch.
- Optional companion flag: `conversationalIntakeSeen` so returning users are not re-prompted unless they choose Guided start.

---

## Guardrails

- User confirms every AI-proposed item.
- No invented happiness or emotion scores.
- Biometric lock still covers the canvas.
- Manual form and direct canvas add remain fully functional.
- No cloud requirement.

---

## Out of scope (this pack)

- Mood check-in entity / UI (separate V2 item)
- Review reminders
- Chronicle tagging
- Ascension anchoring changes (already exists on Directive side)
- Codex mood-vs-mass chart
- Neural Brief Self Map shades
- Changing radial mass formula or categories

---

## Call-site notes

- Reuse existing Neon Guide / mentor chat infrastructure if present; otherwise a lightweight local chat sheet.
- Insert only through `SelfMapRepository`.
- Do not regress V1 canvas, pre-seed, or export/delete.
