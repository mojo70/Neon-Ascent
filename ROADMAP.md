# Neon-Ascent Roadmap

**Cyberpunk Life RPG / Self-Improvement OS**  
*Turn real-world growth into a high-stakes neon terminal experience.*

**Last updated:** May 23, 2026  
**Current version:** v0.5 → V3 Neural Ascension Protocol (in progress)  
**Primary repo:** `mojo70/Neon-Ascent`

## Vision
Neon Ascent is your daily driver Life OS wrapped in immersive cyberpunk aesthetics. It transforms real-world habits, biometrics, and personal growth into a living RPG experience with S.P.E.C.I.A.L. progression, holographic avatar reactivity, and a powerful on-device AI co-pilot.

### Core Non-Negotiables
- **Honor system first** (no mandatory proof).
- **S.P.E.C.I.A.L. only updates** from real measurements (Health Connect, wearables, benchmarks).
- **Privacy-first, local-only AI** (Gemma LiteRT).
- **ADHD-friendly**: grace, flexibility, low friction, dopamine without annoyance.
- **Flexible hierarchy**: Top-down + Bottom-up creation.

---

## V3 Neural Ascension Protocol (Core Loop – Priority #1)
The heart of the app: Ascension Directives → Missions → Tasks with hybrid creation, deep local AI mentorship, gentle Neural Pings, and forgiving progression.

### Key Components

#### 1. Hierarchy
- **Ascension Directives**: High-level, long-term life quests (e.g., “Fortify the Frame”, “Launch Side Hustle”).
- **Missions**: Medium-term campaigns that advance Directives.
- **Tasks**: Actionable units (recurring, one-time, standalone).

#### 2. Hybrid Creation
- **Top-down**: Directive → Local AI generates Missions + Tasks.
- **Bottom-up**: Standalone Tasks or direct linking later.

#### 3. Neural Pings (Notifications)
- Gentle, useful, batched (“Daily Neural Brief”).
- Adaptive wake-time support (global + per-task).
- No aggressive escalation. Burnout detection + auto-throttling.
- Quick actions: Log Done, Snooze, Skip with Reflection.

#### 4. Completion & Progression
- Instant neon dopamine feedback (XP, streaks, avatar micro-reactions, terminal logs).
- Lenient streaks with configurable grace buffer (1-3 days default).
- Recovery Missions for missed periods.

#### 5. Local AI Mentor (Gemma – The Neon Guide)
Configurable modes per Directive/Mission:
- **Review**: Progress summaries & pattern detection.
- **Sounding Board**: Reflective questions.
- **Guide/Mentor**: Full step-by-step breakdowns, suggested prompts for external AIs (e.g., “Paste this into Grok…”), resource suggestions, sub-checks, and adaptive follow-ups.

#### 6. Enhanced Directive Creation – "The Directive Forge" (New Priority)
**Goal:** Deliver a true concierge-level onboarding where the Neon Mentor takes the lead the moment a user begins creating an Ascension Directive.

##### Key Requirements
- **Proactive AI from Step 1:** As soon as the user enters a title or vision statement, the AI (in Guide mode by default) analyzes it and suggests 2–4 Missions with starter Tasks, recurrence, time windows, and grace buffers.
- **Contextual Awareness:** The AI must maintain full conversation memory for that Directive (title, vision, user refinements, past user patterns from Memory Palace, Health trends).
- **Conversational Builder:** User can chat naturally (“Make the hydration mission more gentle” or “Add evening wind-down”) and the AI iteratively updates the proposed structure.
- **One-Tap Acceptance:** Preview full hierarchy (Directive → Missions → Tasks) with suggested timing. User can accept all, select specific ones, or reject and continue chatting.
- **Seamless Handoff:** Once accepted, items are created with `aiGenerated = true` and immediately appear in Dashboard + scheduled for Neural Pings.

##### What We Need to Build
- **DirectiveForgeScreen:** A dedicated creation wizard (modal or full flow) that defaults to AI Guide mode.
- **Persistent Creation Context:** A temporary in-memory + Room-backed session for the current Directive being forged.
- **Enhanced AI Routing:** During creation, prioritize `HABIT_FORGE` + `PROGRESS_ARCHITECT` + `ADHD_RUNNER` + `NEON_NARRATOR`.
- **Preview & Acceptance UI:** Visual hierarchy tree with expandable Missions/Tasks + one-tap “Accept & Integrate”.
- **Deep Integration** with existing Quick Task Creation and AI Mentor flows.

**Status:** In Progress (leverage recent hierarchy UI work)  
**Priority:** High – Critical for stickiness and “actually usable for my own cases”

---

## Neural Expert Matrix & Mempalace (Local AI Foundation)
The Memory Palace + Expert Matrix is the brain of the AI system.

### Current Experts (from recent Neural Expert Matrix implementation)
- `BIOHACKER_PREMIUM`: HRV sync, circadian anchoring, and neurochemical stability.
- `ZEN_ARCHITECT`: Dialectic calm and presence-based task deconstruction.
- `COORDINATE_OBSERVER`: Intuition-based protocols and Remote Viewing (CRV) methods.
- `VENTURE_SAMURAI`: Scalable business operations and "atomic revenue unit" prioritization.
- `QUANT_RUNNER`: Probabilistic execution and market-matrix risk management.
- `CYBR-TES`: Unified “Cyber Socrates” persona integrated across Dashboard advice, Cyberdeck, and Mission Forge.

### To Do – Expand Expert Matrix
Add these experts to Mempalace (SKILLS wing) with detailed system prompts:
- **HABIT_FORGE**: Atomic Habits + Fabulous-style systems expert. Specializes in recurrence, habit stacking, and gentle consistency building.
- **ADHD_RUNNER**: Specialist in executive function, grace buffers, dopamine menu design, and low-decision-fatigue flows.
- **PROGRESS_ARCHITECT**: Expert at quarterly reviews, progression visualization, and turning data into meaningful identity shifts.
- **NEON_NARRATOR**: Immersive flavor text and cyberpunk storytelling for avatar reactions, pings, and terminal logs.
- **RECOVERY_SAGE**: Burnout detection, recovery mission generation, and forgiving streak logic.

### Expert Settings To Do
- **UI screen** in Settings → Neural Core → Expert Matrix to toggle experts and adjust weighting.
- **Routing logic** to intelligently combine experts (e.g., `HABIT_FORGE` + `ADHD_RUNNER` for daily tasks).
- **Memory Palace integration** so experts have persistent context from user’s past completions.

---

## Current Status
**V3 Progress:** ~78-82%  
Strong recent wins in Dopamine celebrations, Recovery Missions, Terminal Ritual, and Neural Expert Matrix migration to core.

### Completed / Strong
- Modular feature architecture (`feature/goals`, `feature/habits`, `feature/notifications`, etc.).
- Health Connect + Garmin grounding (permissions, background sync, live metrics, Body Battery, Sleep, Stress).
- S.P.E.C.I.A.L. system (real measurements only).
- Holographic avatar + reactive particles.
- Neural Expert Matrix + Mempalace foundation.
- Dopamine celebration system.
- Recovery logic + ADHD-friendly streaks.
- Terminal Ritual screen with AI analysis.

### In Progress / Gaps
- Full parent-child hierarchy wiring (Task → Mission/Directive or standalone).
- Rich Task model fields (recurrence, `adaptiveWakeEnabled`, `graceBufferDays`, `timeWindow`, `completionHistory` with mood/notes).
- Deep AI Guide/Mentor mode with actionable breakdowns + prompt suggestions.
- Neural Pings batching, adaptive wake, gentle tone, snooze/skip+reflect.
- Forgiving neon streak visuals (flickering chain).
- Dashboard hierarchy view (Directives → Missions → Today’s Tasks).

---

## Phase 2: V3 Core Loop Delivery (May–June 2026)

### Priority To Do’s (Next 7-10 Days)
1. **Data Model Consolidation** — Unify goals + habits into clean V3 entities (`AscensionDirective`, `Mission`, `Task`) with all new fields.
2. **AI Mentor Modes** — Implement Review / Sounding Board / Guide per item + expand Expert Matrix with new personas.
3. **Neural Pings Polish** — Batching, adaptive wake, gentle tone, snooze/skip+reflect.
4. **Task Completion Flow** — Big LOG COMPLETE button + instant neon feedback + optional notes.
5. **Roadmap & Documentation** — Keep this file current after every major sprint.

### High Value
- Visual streak system with grace buffer (neon chain that flickers but doesn’t die).
- Quarterly Terminal Review ritual with AI-generated next Directives.
- Heatmaps and progress visualization.
- Habit stacking cues and flexible time windows.

---

## Directive Forge – Concierge-Level Creation Experience (High Priority)
**Goal:** Make creating an Ascension Directive feel like having a personal neon cyberpunk coach. The AI takes the lead from the first input and delivers a full, reviewable structure (Missions + Tasks with suggested timing) that users can accept conversationally.

### Key Features
- **Defaults to Guide mode on Directive creation** (user can switch to Review/Sounding Board).
- **Real-time contextual awareness:** Maintains conversation state for the current Directive (title, vision, refinements, user history from Memory Palace).
- **Auto-suggest timing/recurrence/grace buffers** based on user patterns + Health data (overridable).
- **Conversational builder:** User chats naturally; AI iteratively refines Missions/Tasks.
- **Visual preview** of full hierarchy before final acceptance.
- **One-tap “Accept & Integrate”** (all or selected items) with immediate celebration.

### Settings Integration (New Section: Neural Forge Preferences)
- **Toggle:** “AI Auto-Suggest Timing & Structure” (default On)
- **Toggle:** “Default to Guide Mode on New Directive”
- **Toggle:** “Use Health Data for Timing Suggestions”
- **Toggle:** “Full Manual Mode” (disables most AI assistance)

**Status:** Planning → Implementation  
**Priority:** Very High – Core to user stickiness and “I actually use this daily” goal

---

## Deep Integration: SPECIAL Training Protocols + CyberCrapp + Biohacking
**Goal:** Create a unified system where clicking any S.P.E.C.I.A.L. letter on the holographic avatar screen launches relevant Ascension Directives, seeded Biohacking protocols, and training programs (including CyberCrapp). Everything must feel like one cohesive Life OS — no silos.

### Key Integrations
- **Holographic Avatar Screen:** Clicking a letter (e.g., S for Strength) opens a contextual menu → “Forge Directive” pre-filled with relevant seeded protocols.
- **CyberCrapp Program:** Integrated as a flagship Strength Directive with progressive Missions (e.g., Beginner Iron Temple → Advanced Cyber Crapp Overload). It links to Lifting protocols.
- **Biohacking Page:** Acts as a hub/selector. Selecting categories (Longevity, Recovery, etc.) surfaces related seeded Directives that can be cloned into the user’s Ascension system.
- **Unified Data Model:** All protocols are stored as `AscensionDirectives` (with tags like `biohacking`, `special:strength`, `cybercrapp`). This enables AI Guide mode to reference them naturally.

---

## Seeded Directives (New Library – Expandable via AI)
These will be seeded in the DB on first launch and discoverable via avatar or Biohacking page.

### S.P.E.C.I.A.L. Core Set
- **Strength:** CyberCrapp Protocol (Progressive overload lifting program)
- **Perception:** Signal Clarity (Sensory + cold exposure training)
- **Endurance:** Fortify the Frame (Hydration + cardio base)
- **Charisma:** Neon Presence (Social energy + storytelling)
- **Intelligence:** Mind Palace Expansion (Learning systems + spaced repetition)
- **Agility:** Shadow Runner (Mobility + reflexes)

### Biohacking Protocols (Tied to selector on Biohacking page)
- **Longevity Protocol**
- **Metabolic Overclock** (16:8 Intermittent Fasting, Carb Cycling)
- **Recovery & Rebuild** (Sauna, Cold Exposure, Vagus Nerve Activation)
- **Neural Citadel** (Meditation stack, Breathwork, Visualization)
- **Soul Anchor** (Christianity integration via Deep Node: Scripture, Prayer, Fellowship)
- **Optimized Human 2.0** (Umbrella protocol for general optimization)

**Status:** High Priority – Ties avatar, biohacking, and core Ascension system together.

---

## Success Metric for V3
You (and users) open the app every morning because the system feels like a trusted, non-annoying cyberpunk co-pilot that actually helps you ascend — with visible progress, grace for missed days, and deep AI guidance when needed.

