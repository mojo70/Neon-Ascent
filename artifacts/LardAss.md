# Neon Ascent – LardAss Principles

**Status**: Design lock  
**Updated**: November 8, 2026  
**Repo**: `mojo70/Neon-Ascent`  
**Applies to**: Every AI interaction surface (Self Map intake, Directive intake, Neon Guide, form assist, biohacking recommendations, etc.)

Codename LardAss. Stop treating free-form model calls as the driver of product surfaces. The model is a narrow tool inside a deterministic shell.

---

## Core rules

1. **Deterministic shell**  
   State machine or explicit control flow owns the interaction. The model is never allowed to both “talk” and “act” in one unconstrained call. High-frequency or trust surfaces stay template where possible.

2. **Hard prompt isolation**  
   Each surface uses a dedicated, minimal system prompt. It never inherits the CYBR-TES / Socrates base, never auto-injects Faith Lens, never uses cryptic examples. Personality modes (OPERATOR, GUIDE, LORE, etc.) stay isolated per AiPersonality.md.

3. **Narrow model calls**  
   The model is called only for a specific structured task (e.g., emit proposal JSON grounded in the user’s last utterance). Guide messages that introduce results are templates or extremely constrained one-liners.

4. **Post-generation validation**  
   Schema validation is mandatory. Additional checks (grounding in user text, blocked phrase patterns, tone constraints) reject bad output. On rejection, fall back to a follow-up question or template instead of showing the bad generation.

5. **Eval gate**  
   Every major surface has a small golden set (8–12 cases). The slice does not ship if the model violates the control-flow rules, emits off-tone flavor, or produces ungrounded results.

6. **Local-first & confirmation**  
   Prefer on-device. User confirms every write. No cloud requirement for core flows. Dual visual mode, backup/P2P completeness, and no fifth wing remain standing rules.

---

## What this replaces

- Free-form calls that can emit monologue + structured proposals in one shot.
- Soft prompt instructions that the base persona can override.
- Shipping on “it looked better in one test.”

Self Map intake is the first reference implementation of these rules.
