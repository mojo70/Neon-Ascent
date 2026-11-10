# Neon Ascent – Self Map Intake (LardAss Slice 1)

**Status**: Design lock  
**Updated**: November 9, 2026 (specific-grounding lock)  
**Repo**: `mojo70/Neon-Ascent`  
**Studio pack**: `SelfMapIntakeGeminiPrompts.md` (local only)  
**Depends on**: `SelfMap.md` (V1 shipped, V2 section), existing Self Map canvas + repository, AiPersonality.md, `LardAss.md`

Codename LardAss Slice 1. Reference implementation of the LardAss principles (deterministic shell, hard prompt isolation, narrow structured model call, post-validation, eval gate). See `LardAss.md`.

---

## Product call

Mentor / guided chat is the default Self Map item genesis surface.  
Manual form (or direct canvas add) is always one tap away.  
Settings controls the default mode per user.  
User confirms every proposed item.  
After finish or skip, land on the radial canvas with accepted items already placed.

Agent loop (local, lightweight):
```
Entry → Load context (current items, categories, pre-seed)
     → One focused question (skippable, sequence-aware)
     → Structured proposals (name, category, importance, note, optional rationale)
     → User: accept / accept-and-edit / reject / more-like-this / skip-category
     → Insert only on accept via SelfMapRepository
     → Resume-safe session state
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
| Sequence | One question at a time. Skippable. Can adapt if category already well covered. |
| Landing | Radial canvas with accepted items placed. |
| Resume | Persist lightweight session state (step, accepted, rejected set) so interrupted chat resumes. |
| Context | Load non-archived Self Map items + categories + pre-seed Body & Senses before first proposal. |
| Control flow | Deterministic state machine. Guide asks (template or short fixed question) and waits. Model is called only after a non-empty user reply, and only for structured proposal JSON. Introductory sentence is a template. No free-form monologue is possible. |
| Prompt isolation | Dedicated minimal system prompt for Self Map intake. Never loads CYBR-TES / Socrates base. Never injects Faith Lens. Blocked patterns (CYBR-TES, Runner, protocol, vision/stimulus, etc.) are rejected at validation. |
| Validation | Schema + specific grounding required. Rationale and proposal name must reference a concrete phrase from the user’s most recent utterance. Generic catch-all names (“Core Focus”, “Personal Growth Drive”, “Primary Identity Anchor”, etc.) and ungrounded rationales (“Grounded in operator baseline setup”, “Essential psychological vector”) are rejected. On rejection, fall back to a short confirmation of what was heard instead of showing bad cards. |
| Tone | Curious, non-judgmental, meta-aware. Short, operational, light cyber. No Faith Lens, no Socrates, no moralizing. |
| Reject memory | Session rejects are not re-proposed. |
| UI surface | Conversational terminal: scrolling message thread is the primary canvas. Proposals appear as compact inline cards inside the chat. Terminal-style input is the clear bottom focus. Phase is a light header crumb only. No separate proposal section or large Next Phase button. |

---

## Intake surface behavior

### Chat mode (default) — conversational terminal

- Opens as a terminal-style sheet or full screen on the Avatar / Self Map entry.
- Header: small “SELF MAP // TERMINAL”, light phase crumb (e.g. 1/5), “Switch to Form” secondary.
- Main surface is a scrolling message thread (Guide → user → Guide + inline proposals).
- Agent loads current non-archived items and categories first.
- One focused question at a time (base sequence, skippable, can shorten if coverage high).
- Guide asks the question and waits. No proposals are emitted until the user has replied on that step.
- After a real user reply that contains clear entities or multi-sentence content, Guide emits 1–2 structured proposals immediately as compact inline cards inside the thread. Each proposal name and rationale must cite a concrete phrase from the user’s reply (e.g., “coding, building”, “lift weights”, “God”). Generic catch-alls are rejected.
- If the model returns only generic or ungrounded nodes, discard them and fall back to a short confirmation (“I heard coding/building and lifting — want nodes for those?”) instead of showing bad cards.
- Follow-up question only if the reply is empty, one word, or pure skip. Never ask for elaboration on a substantive reply.
- Never surface the raw category enum (“Custom”) in Guide text.
- Introductory sentence is a short template (“Based on that, two nodes:”).
- User actions on each card: accept, accept-and-edit, reject, “more like this”. Rejected items collapse or leave the thread and are not re-proposed this session.
- Bottom terminal-style input is always primary (“> Reply to Guide…”). User can type free text at any time to refine or continue.
- On accept: insert via `SelfMapRepository`, brief confirmation line in thread, canvas updates immediately.
- Session state persisted so interruption resumes at the same step with the same accepted/rejected set and visible history.
- Dual visual mode (CYBER / STEVE / PILOT) respected. No large progress bar. No wizard “Next Phase” button.

### Form mode (manual or switched)

- Same surface switches to the existing manual add / wizard fields.
- Fields: name, category, importance, notes.
- Optional “Ask Mentor” can suggest a single structured name + importance without forcing full chat.
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
- Companion flag: `conversationalIntakeSeen` so returning users are not re-prompted unless they choose Guided start.

---

## Guardrails

- User confirms every AI-proposed item.
- No proposals until the user has replied on the current step. Do not invent nodes from the opening question alone.
- No invented happiness or emotion scores.
- Structured proposals only; validate schema before display.
- Session rejects not re-proposed.
- Biometric lock still covers the canvas.
- Manual form and direct canvas add remain fully functional.
- No cloud requirement.
- No Faith Lens. No Socrates. No moralizing.

---

## Out of scope (this slice)

- Mood check-in entity / UI
- Review reminders
- Chronicle tagging
- Ascension anchoring changes
- Codex mood-vs-mass chart
- Neural Brief Self Map shades
- Changing radial mass formula or categories
- Full multi-agent or external RAG (local context only)

---

## Call-site notes

- Reuse existing `SelfMapConversationalIntakeUseCase` and Neon Guide / mentor chat infrastructure.
- Thin local agent state machine or graph around the step sequence.
- Insert only through `SelfMapRepository`.
- Do not regress V1 canvas, pre-seed, or export/delete.
- Any new session-state fields follow BackupCompleteness (payload version bump if persisted beyond session).
- Dual visual mode (CYBER / STEVE / PILOT) respected.
- Tiny eval set (8–12 scenarios) for empty map, pre-seed only, high coverage, skip, resume.
