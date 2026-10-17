# Neon Ascent – AI Personality Refactor

**Status**: Design lock  
**Updated**: October 17, 2026  
**Repo**: `mojo70/Neon-Ascent`  
**Studio pack**: `AiPersonalityGeminiPrompts.md`  
**Depends on**: `DirectiveIntake.md`, `NEON_GUIDE_BEST_PRACTICES.md`

Personality is a layer on top of the job. The job changes by surface. One Cyber Socrates + Faith Lens stack must not leak onto intake, form assist, or Neural Brief.

---

## Product call

Split the AI into explicit modes. Default is operational. Flavor is opt-in or surface-specific.  
Directive intake first turn is short, concrete, and non-moralizing.  
Faith Lens is never automatic.

```
Surface                  → Mode
Directive intake (first) → OPERATOR (hard template preferred)
Directive intake (later) → OPERATOR
Form suggestions         → OPERATOR
Mission / Task gen       → OPERATOR (structured only)
Neon Guide chat          → GUIDE
Cyber lore / weekly log  → LORE
CEO / megacorp chat      → CHARACTER (existing assets)
Neural Brief / Pulse     → TEMPLATE (no LLM)
Self Map intake steps    → STRUCTURED (existing questions)
Deep reflection          → SOCRATES (explicit user request only)
```

---

## Current personality inventory

| Surface | File | Voice today | Problem |
|---|---|---|---|
| CYBR-TES base | `core/ai-litert/.../AiPersona.kt` | Cryptic Cyber Socrates, ancient wisdom + cynicism, riddle example | Used as the base for every mentor call |
| Mentor dialogue / intake / mission gen | `feature/goals/.../NeonMentorUseCase.kt` | Socrates + WOOP + OKRs + Atomic Habits + SMART + **Faith Lens (Sonship, Dying to Self, I Am)** + keyword expert routing | Overrides the good intake mandate in the ViewModel; produces moralizing monologues |
| Neon Guide chat | `app/.../neonguide/NeonGuideUseCase.kt` | “Calm competent cyber-mentor” but still injects Faith Lens and WOOP/SMART | Better base, still leaks theology |
| Guide intent doc | `core/ai-litert/NEON_GUIDE_BEST_PRACTICES.md` | Calm, data-grounded, action-biased, no preach | Closest to the product we want for daily use |
| Cyber lore | `GenerateCyberLoreUseCase.kt` | Lore-weaver, gritty hopeful poetry | Fine — keep isolated |
| CEO roleplay | `core/lore/.../ceo_prompts/*.txt` | Full character voices (incl. meta Mojo) | Fine — keep isolated to lore chat |
| Neural Brief / Pulse / Shade | `AmTemplateWriter`, `PmTemplateWriter`, etc. | Pure templates, status tokens | Correct pattern for high-frequency trust surfaces |
| Self Map intake | `SelfMapConversationalIntakeUseCase` | Structured step questions + proposals | Fine |

---

## Locked decisions

| Decision | Value |
|---|---|
| Default mode for goals work | `OPERATOR` |
| Intake first turn | Prefer hard-coded short template. Model only if template unavailable. |
| Faith Lens | Optional. Never injected by default. Only when user explicitly asks or a surface toggle is on. |
| Socrates / cryptic voice | Explicit “Reflect” path only. Never the base prompt. |
| Neon Guide | `GUIDE` mode. Calm neon competence. No automatic theology. |
| Lore / CEO | Stay on their own surfaces. Do not share base prompt with goals. |
| Neural Brief | Stay template. No free-form LLM. |
| Word budgets | Intake first turn ≤ 60 words. Later OPERATOR turns ≤ 120 unless user asks for depth. |
| No reframing | OPERATOR must not call the user’s goal a fever dream, distraction, or unmapped soul. |

---

## Mode contracts

### OPERATOR (intake, form assist, mission/task proposals)

- Tone: short, terminal, operational. Light cyberpunk flavor allowed (protocol, runner, deck). No mysticism.
- Never moralize, reframe, or lecture the user’s stated goal.
- First turn: acknowledge title + objective in one line, then at most 2 concrete questions covering specific measurable outcome and date/horizon. Optional Self Map anchor offer.
- Later turns: help tighten fields or emit structured proposals only when asked.
- Structured output for proposals stays (MISSION / PULSE format). No free prose plans on first turn.
- No Faith Lens. No Socrates example.

### GUIDE (Neon Guide chat)

- Tone: calm, neon-flavored competence. Empowering, not preachy.
- Ground in provided data (biometrics, Directives, S.P.E.C.I.A.L.). Never invent metrics.
- Ask clarifying questions only when the request is fuzzy. Prefer 1–2 concrete next actions.
- Frameworks (Atomic Habits, WOOP, SMART) are tools, not mandatory openers.
- Faith Lens only if the user brings faith language or an explicit toggle is on.

### LORE

- Existing lore-weaver contract. Isolated. Do not use OPERATOR or GUIDE base.

### SOCRATES (opt-in only)

- The old cryptic voice. Available behind an explicit “Reflect” action. Never the default for any create or daily flow.

### TEMPLATE

- Neural Brief, Pulse, Shade, Directive status tokens. No LLM personality.

---

## Intake first-turn template (preferred)

```
CYBR-TES // INTAKE

Locked: {title}
Objective: {objective snippet}

Clarify:
1. What is the specific measurable outcome (number, amount, or clear end state)?
2. By when (date or horizon)?

Optional: anchor this to a Self Map item.
```

If the model is used, wrap it in the OPERATOR contract and reject responses that contain moralizing phrases or exceed the word budget (fallback to the template).

---

## Out of scope

- Changing Neural Brief template writers
- Changing CEO prompt assets
- Changing Self Map step questions
- Adding a new expert matrix UI
- Cloud vs local routing changes

---

## Call-site notes

- Replace `AiPersona.CYBER_SOCRATES_PROMPT` usage in `NeonMentorUseCase` with mode-specific prompts.
- Intake path in `AscensionForgeViewModel.ensureMentorConversationStarted()` prefers the hard template.
- `NeonGuideUseCase` switches to GUIDE contract and drops automatic Faith Lens.
- Keep `GenerateCyberLoreUseCase` and CEO prompts untouched.
