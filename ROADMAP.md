# Neon-Ascent Roadmap

**Cyberpunk Life RPG / Self-Improvement OS**  
*Turn real-world growth into a high-stakes neon terminal experience.*

**Last updated:** May 10, 2026  
**Current version:** v0.4 (Neural Memory Palace + Unified Persona)  
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
- **Neural Memory Palace**: Local-first verbatim memory system (Wings/Rooms architecture) implemented for long-term AI context.
- **Neural Uplink Phase 1**: Multi-provider biometric architecture with BLE Heart Rate (1Hz) support and encrypted token storage.
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
- **Neural Uplink Phase 2**: Garmin Cloud API integration for Body Battery, Sleep Score, and Stress via WebView SSO.
- Biohacking module: Transitioning from static tracking to AI-driven protocol coaching.
- Full integration of Aspirations + Missions into existing Database Core
- Neural Scan integration with Avatar level-up and archetype-specific mission seeding.
- **Memory Palace Expansion**: Mining biometric trends (HRV/Sleep) into long-term memory "Drawers."
- **Dialectic Protocol Expansion**: CYBR-TES specific missions that require verbal reflection via the terminal to complete.
- Mission Detail + Aspiration Detail screens
- Live health data polishing (HR badge + steps/calories)

**Phase 2 Progress: ~78% complete**

## Phase 2: Habit Engine + Real-World Grounding (May–July 2026)

**Key Discussion Notes**
- **Active Coaching vs. Passive Tracking**: We are shifting the paradigm from logging to "Contextual Intelligence." If HR is low, the app shouldn't just record it—it should trigger a Recovery Protocol ping via the ContextualPingWorker.
- **Privacy-First Intelligence**: LiteRT (Gemma) is now our standard for AI. No user biometric data leaves the device for "coaching" sessions.
- **Lore Integration**: The Cyberdeck is the primary interface for "Netrunning" through real-life tasks. Megacorp nodes and NetWatch alerts add high-stakes flavor to mundane habit tracking.

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
