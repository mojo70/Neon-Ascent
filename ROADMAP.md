# Neon-Ascent Roadmap

**Cyberpunk Life RPG / Self-Improvement OS**  
*Turn real-world growth into a high-stakes neon terminal experience.*

**Last updated:** June 27, 2026  
**Current version:** v0.5 → V3 Neural Ascension Protocol + Stickiness Release  
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

### 3. Neural Pings & External Surface (The "Neural Brief")
**Philosophy (Cyberdeck Contract):**  
When the user is *outside* the app, keep it polite, low-density, and value-teasing. Never aggressive. The goal is gentle re-entry into the immersive cyberdeck where all rich feedback, analysis, and decision support lives.

**Neural Brief (Compiled Summary Notification) – Target for this release**
- **Single primary notification** per day (or adaptive window) that aggregates the most important signals.
- **Payload design** (keep it scannable, ~3-5 lines max):
  - Greeting / status tone: Calm, competent, slightly neon-flavored but never cute or salesy.
  - 1-2 key biometric insights (e.g., “HRV recovered overnight. Body Battery strong.” or “Sleep quality dipped — recovery flag raised.”).
  - 1 high-value “what to do next” recommendation tied to current Directives or S.P.E.C.I.A.L. (e.g., “Today’s Strength protocol looks good. Add the mobility micro-mission?”).
  - Quick actions: “Log Complete”, “Open Deck”, “Snooze 2h”, “Skip + Reflect”.
- **Trigger logic**: WorkManager with smart constraints (battery not low, device idle or charging preferred, Doze-aware, user-defined quiet hours + adaptive wake).
- **Grouping & channels**: One dedicated “Neural Brief” channel. Future expansion can group secondary pings under it.
- **Anti-spam**: Burnout detection, auto-throttling, and respect for recent app opens / completions.

**WorkManager Setup Plan**
- `NeuralBriefWorker` (CoroutineWorker) scheduled via `WorkManager`.
- Use `setRequiredNetworkType(NetworkType.NOT_REQUIRED)`, `setRequiresBatteryNotLow(true)`, `setRequiresDeviceIdle(false)` initially.
- `Constraints` + `BackoffPolicy` for reliability.
- Input data via `Data` (user prefs for quiet hours, preferred insight depth).
- Hilt integration via `HiltWorkerFactory`.
- Unique work name (`"neural_brief_daily"`) with `ExistingPeriodicWorkPolicy.KEEP`.
- On success: Build `NotificationCompat` with `BigTextStyle` or custom layout, actions via `PendingIntent` to deep links (`complete`, `open_deck`, `snooze`).
- Future: Expedited work for time-sensitive recovery flags.

This is the primary “pull me back in” mechanism. It must feel *useful* on its own while clearly promising richer value inside the deck.

**UI Philosophy Split for Biometrics & Insights**
- **Holographic Avatar Hub**: Heads-up display for key metrics, real-time status (HRV trend, Body Battery, S.P.E.C.I.A.L. resonance), and one-line insights. Tap any element to deepen into the Biohacking Screen (contextual navigation). Keeps the avatar reactive and immersive for quick daily checks.
- **Biohacking Screen**: Dedicated deep-dive layer for trends, charts, full Socratic Insights, correlations, and detailed recommendations. This is where users go for analysis and "why this matters + what to do next."

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

### 7. Biometric Insight Engine & Memory Palace Projections (Stickiness Core)
**Problem we’re solving:** We can pull data from Health Connect, Garmin, and BLE, but the app still doesn’t feel like the *single source of truth* for “what my body is telling me and what I should actually do next.” Users still open other apps for charts and understanding.

**Solution direction — Lightweight Event Sourcing / CQRS-lite for Insights**
- Treat raw biometric readings and user actions as an immutable **event log** (`BiometricEvent` + `ActionEvent` entities in Room).
- Maintain **derived projections** (`SocraticInsight`, `RecommendationProjection`, attribute trend summaries) that are rebuilt incrementally or on-demand.
- `InsightProjectionProcessor` (or `MemoryPalaceProjector`):
  - Triggered on new events (via Flow after uplink ingest), periodic WorkManager job, or explicit refresh.
  - Runs lightweight local rules first → feeds summarized context to Gemma (CYBR-TES or specialist persona) for synthesis.
  - Upserts into projection tables (with `basedOnEventRange` + version for auditability).
- **Benefits**: Cheap fresh prompts (read projection instead of replaying everything), replay/debug capability, easy evolution of insight logic, excellent privacy (all local + SQLCipher).

**What “rich enough” looks like for this release**
- In-app surfaces that replace the need to open other apps: simple trend cards + AI interpretation for HRV, sleep, Body Battery, steps, and S.P.E.C.I.A.L.-relevant metrics.
- Actionable “what to do next” tied directly to current Directives, S.P.E.C.I.A.L. attributes, and Directive Forge pre-fill.
- Biometric mining already feeds Memory Palace → now make the *output* (insights + recs) first-class and visible.
- Close the loop: Insights → AttributeProtocols → Directive Forge suggestions.

**Status:** Architecture direction set. Implementation of event model + basic processor is high priority for stickiness.

**Neon Guide Chat Mode (Hybrid Conversational Layer)**
- **Goal:** Build a high-quality guided conversational interface ("Ask the Neon Guide" or enhanced CYBR-TES mode) that supports natural chat while maintaining structure.
- **Guided inputs & behavior rules:** The AI should start conversations with clear goal-setting prompts (similar to Google Health Coach), ask clarifying questions about challenges/barriers, and always ground responses in your real data (Memory Palace + biometric projections).
- **Hybrid architecture:**
    - **Local-first** (Gemma LiteRT + projections + Memory Palace context) for privacy, speed, and deep personal history (Directives, S.P.E.C.I.A.L., past patterns).
    - **Optional cloud fallback** (Gemini) for complex synthesis, with explicit user toggle and clear data boundaries.
- **Data usage & structure:** Every response must reference recent projections/events, tie back to active Directives or S.P.E.C.I.A.L. attributes, and end with actionable next steps (ideally one-tap to Forge Directive or log a task).
- **Expert personas** (especially `BIOHACKER_PREMIUM`, `RECOVERY_SAGE`, `PROGRESS_ARCHITECT`) route intelligently during conversations.

*This hybrid gives us the best of both worlds: Google's frictionless guided conversation style + our superior privacy, long-term memory, and integration into a full life OS/RPG.*

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

## Phase 2: V3 Core Loop Delivery + Stickiness Release (May–July 2026)

### Priority To-Do’s (Next 10–14 Days — Focus on Daily Value & Retention)
1. **Data Model Consolidation** — Finish clean `AscensionDirective` / `Mission` / `Task` entities with all rich fields.
2. **Directive Forge MVP** — Full concierge flow with contextual awareness from Memory Palace + recent biometrics. High friction reducer.
3. **Neural Brief + WorkManager** — Implement the primary external notification (polite payload, quick actions, constraints). This is the primary re-entry mechanism.
4. **Insight Projection Layer** — Event store + `InsightProjectionProcessor` + basic SocraticInsight / recommendation projections. Enables reliable, cheap “what next” intelligence.
5. **In-App Biometric Intelligence Surface** — Enhance BiohackingScreen / Dashboard / new hub so users see trends + AI guidance *here* instead of other apps. Simple visuals + interpretation.
6. **Neural Pings Polish & Internal Celebration** — Batching, adaptive tone, big “LOG COMPLETE” flow with mood/notes, neon feedback.

### High-Value Additions for Stickiness (This Release)
- **Close the Data → Insight → Action Loop:** From any insight or S.P.E.C.I.A.L. attribute on Avatar Hub / Biohacking Screen, offer immediate “Forge Directive / Add Mission” with smart pre-fill from current biometric context + Memory Palace.
- **Polish completion ritual & celebration:** Big LOG COMPLETE + mood/notes + neon feedback + terminal log.
- **Basic visual feedback:** Trend indicators and resonance effects on Avatar Hub; deeper charts on Biohacking Screen. Replace “I have to check Garmin/Heavy” behavior.
- **“Today’s Intelligence” smart summary card:** Surfaces the best insight + recommended action on Dashboard.
- **Expert Matrix expansion:** Add `RECOVERY_SAGE` and strengthen `BIOHACKER_PREMIUM` routing for health data.

**Success Metric for this release:** Users can get quick status on the Avatar Hub, dive deep when needed, chat naturally with the Neon Guide, and receive actionable recommendations — all without leaving Neon Ascent for other apps.

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

## Self Map (Mind Hacking Happiness Inspired)
A visual, evolving "Self Map" for spiritual journey, enlightenment, "dying to self," identity reconstruction, and sonship in the cyberpunk narrative.

**Key Features:**
- Interactive node/graph map (beliefs, strongholds, identity layers, Holy Spirit upgrades).
- Reflection prompts tied to Scripture (Romans 8, "I Am", parables) and biometric state.
- Progress linked to S.P.E.C.I.A.L. and Directives.
- Integration with Neon Guide for guided mapping sessions.
- Export/share options + privacy-first local storage.

**Status:** Planning. Seed initial templates from Book of Mojo themes (duality, growth mindset, consciousness).

## Learning Courses & "The Book of Mojo" Integration
Gamified, in-app courses drawing from app themes and "The Book of Mojo" (personal manifesto on learning, meditation, strength, health, love, etc.).

**Initial Courses (from Book of Mojo + App):**
- Learning Mastery (Feynman, visualization, conscious practice, growth mindset).
- Meditation & Breathwork (Wim Hof, mindfulness, types from journaling to Kundalini).
- Strength as Skill (mind-body, visualization, progressive overload).
- Health Optimization & Biohacking (supplements, methylation, routines).
- Love, Duality & Relationships (red pill insights, solipsism, connection).
- Building the Ark (legacy, expansion, consciousness).

**Implementation:**
- Modular player with XP, quizzes, real-world missions.
- "The Book of Mojo" as in-app eBook (neon reader with highlights, chapter unlocks via Ascension, AI discussion via Neon Guide).
- Completion grants shards, menu items, avatar upgrades.

**Status:** Planning. Extract chapters from Book of Mojo for outlines. Prioritize Meditation/Strength/Health first.

## Data Shards System (Cyberpunk Lore & Progression)
Collectible, combinable fragments of knowledge/power in the Neon Ascent universe.

**Mechanics:**
- **Acquisition:** Mission completion, biometric milestones, course progress, streaks, cognitive tests.
- **Types:** Bio, Faith, Hack, Strength, Enlightenment (new for Self Map).
- **Collecting & Joining:** Gather shards → combine matching sets into "ICE Keys" or full protocols (e.g., Bio shards → Recovery Protocol unlock).
- **Storage & Reading:** Secure Vault with holographic reader UI (animated assembly, lore text, practical application — auto-suggests menu items or Directives).
- **Graphics & Feel:** Neon glowing shards, circuit-trace combine animations, scan-line reader with haptic feedback. Cyberpunk aesthetic (data streams, holographic pop-ups).

**Integration:** Feeds Self Map (spiritual shards), courses (knowledge shards), and overall progression. Balance rarity for rewarding grind without paywalls.

**Status:** Planning. Design types, combine logic, Vault UI, and visuals first.

---

## Success Metric for V3
You (and users) open the app every morning because the system feels like a trusted, non-annoying cyberpunk co-pilot that actually helps you ascend — with visible progress, grace for missed days, and deep AI guidance when needed.

---

## Implementation Notes (Veteran Dev Perspective)

### For the UI Split (Avatar Hub + Biohacking Screen)
- In `HolographicAvatarHub`, use cards or overlaid status indicators for the heads-up metrics. Leverage tap gestures or clickable areas that call `navController.navigate` with arguments for context (e.g., `biohacking?focus=hrv`).
- This keeps the hub visually rich without clutter. `BiohackingScreen` can use tabs or expandable sections for different categories (Recovery, Strength/CyberCrapp, Longevity, etc.).

### For Neon Guide Chat Mode
- **Structure:** Use a `ChatViewModel` with message history + system prompt that includes explicit instructions (e.g., "Always ground answers in user's current S.P.E.C.I.A.L. state, recent projections, and active Directives. Suggest specific Missions/Tasks when appropriate. End with clear next action.").
- **Context Injection:** Inject context from the projection layer + Memory Palace at the start of each conversation (or on key turns).
- **Guided Inputs:** Start new chats with quick buttons or suggested prompts ("Tell me about my recovery", "Help set a new Directive", "Analyze this week's sleep").
- **Hybrid Toggle:** Simple setting "Use Cloud Synthesis for Complex Queries" with warning.

---

**Last updated:** June 27, 2026 (added Neural Brief spec, WorkManager plan, insight projection architecture, UI split philosophy, Neon Guide hybrid chat, Self Map, Mojo Courses, and Data Shards)

