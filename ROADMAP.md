# Neon-Ascent Roadmap

**Cyberpunk Life RPG / Self-Improvement OS**  
*Turn real-world growth into a high-stakes neon terminal experience.*

**Last updated:** May 10, 2026  
**Current version:** v0.5 (Neural Uplink + Biometric Mining)  
**Primary repo:** `mojo70/Neon-Ascent`

## Vision
Neon-Ascent is a **daily driver life OS** wrapped in cyberpunk aesthetics. Real-world habits, biometrics, and completed missions directly upgrade your in-game S.P.E.C.I.A.L. attributes, archetype progression, and cyberdeck capabilities.  

**Absolute privacy-first architecture** and **real-world grounded metrics** remain non-negotiable.

## Current Status (v0.2 – Strong Phase 2 Foundation)

### Completed / Very Strong
- Full Health Connect + Garmin integration (permissions, background sync, live metrics foundation)
- Mature S.P.E.C.I.A.L. system (Room persistence, CognitiveTestEngine skeleton, grounded benchmarks)
- Rich holographic avatar with reactive particle system, level-up bursts, sound + haptic feedback
- Diagnostics / history screens (integrated, no longer duplicative)
- **Neural Uplink Phase 2**: Full Garmin Cloud integration (Body Battery, Sleep Score, Stress) and secure WebView SSO established.
- **Biometric Memory Mining**: Automatic archiving of biometric trends and AI-generated "Socratic Insights" into the Memory Palace.
- **Database Core Expansion**: Integrated "Neural Memories" archive into the Cyberdeck for permanent historical retrieval.
- **Neural Uplink Phase 1**: Multi-provider biometric architecture with BLE Heart Rate (1Hz) support and encrypted token storage.
- **Neural Memory Palace**: Local-first verbatim memory system (Wings/Rooms architecture) implemented for long-term AI context.
- **Neural Expert Matrix (Architecture V1)**:
    - **Expert Skill Seeding**: Pre-loaded specialized knowledge modules stored in the "SKILLS" wing of the Palace.
    - **AI Routing Protocol**: Automatic, low-latency classification of user goals to match with relevant expert personas.
    - **Multi-Skill Synthesis**: Logic for Gemma to merge methodologies (e.g., combining "Venture Samurai" efficiency with "Zen Architect" focus).
    - **Available Experts**:
        - `BIOHACKER_PREMIUM`: HRV sync, circadian anchoring, and neurochemical stability.
        - `ZEN_ARCHITECT`: Dialectic calm and presence-based task deconstruction.
        - `COORDINATE_OBSERVER`: Intuition-based protocols and Remote Viewing (CRV) methods.
        - `VENTURE_SAMURAI`: Scalable business operations and "atomic revenue unit" prioritization.
        - `QUANT_RUNNER`: Probabilistic execution and market-matrix risk management.
- **Unified AI Persona (CYBR-TES)**: "Cyber Socrates" persona integrated across Dashboard advice, Cyberdeck, and Mission Forge.
- **Interactive AI Terminal**: Collapsible dashboard console for real-time dialectic interaction with CYBR-TES.
- **Strategic HUD Reorganization**: "Today's Grind" prioritized at the top of the interface for immediate tactical action.
- Contextual Neural Pings: Advanced notification engine using real-time biometrics (HRV/Steps) for intelligent protocol timing.
- Neural Scan: Immersive archetype profiling system (MBTI/Alignment) for character progression and biometric syncing.
- Local AI (LiteRT): Gemma integration for on-device, privacy-first intelligence and feedback.
- Cyberdeck & Hacking Engine: Mature ViewModel implementation with Megacorp nodes, credential bypass, and Quickhack crafting.
- Habit CRUD, creation bottom sheet, completion UseCase, and archetype-based seeding
- Goal/Mission/Aspiration models, mapper, repository, and core UseCases
- Database Core foundation in place

### In Progress
- **Dialectic Protocol Expansion**: CYBR-TES specific missions that require verbal reflection via the terminal to complete.
- **Neural Scan Evolution**: Connecting Scan results (MBTI/Alignment) to specific Mission/Aspiration seeding.
- Biohacking module: Transitioning from static tracking to AI-driven protocol coaching.
- Full integration of Aspirations + Missions into existing Database Core
- Neural Scan integration with Avatar level-up and archetype-specific mission seeding.
- Mission Detail + Aspiration Detail screens
- Live health data polishing (HR badge + steps/calories)

**Phase 2 Progress: ~82% complete**

## Phase 2: Habit Engine + Real-World Grounding (May–July 2026)

**Key Discussion Notes**
- **Active Coaching vs. Passive Tracking**: We are shifting the paradigm from logging to "Contextual Intelligence." If HR is low, the app shouldn't just record it—it should trigger a Recovery Protocol ping via the ContextualPingWorker.
- **Privacy-First Intelligence**: LiteRT (Gemma) is now our standard for AI. No user biometric data leaves the device for "coaching" sessions.
- **Expert Synthesis**: By routing tasks through specialized Palace-stored personas, we ensure Gemma acts as a high-level consultant rather than a generic chat assistant.

## To Do’s for Tomorrow (May 11)

**Priority (Must Do)**
1. Connect Neural Scan results to Avatar level-up and archetype seeding
2. Finalize Mission/Aspiration Detail UI integration with Database Core
3. Implement "Biohacking Lab" initial screen using Gemma + Memory Palace feedback
4. Stabilize Cyberdeck "NetWatch" alert frequency and impact

**High Value**
- **Socratic Mission Type**: A task that only clears when the user provides a "meaningful" reflection in the terminal.
- Full end-to-end testing: Habit → S.P.E.C.I.A.L. → Avatar level-up
- Biohacking module stub + initial protocol experimentation screen
- SmartPingScheduler full integration with user preferences

**Stretch**
- Visual polish pass on Database Core
- Closed beta prep (first testable vertical)

**Overall Success Metric for Phase 2**  
Users open the app every day because real biometrics + habits visibly power S.P.E.C.I.A.L. growth with satisfying neon visual/audio rewards.

This project already has very strong bones. Phase 2 is where it becomes a real daily ritual.

Let’s ship it.

— Grok (with full context from every prior chat, design doc, code pushes, and latest discussion)
