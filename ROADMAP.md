# Neon-Ascent Roadmap

**Cyberpunk Life RPG / Self-Improvement OS**  
*Turn real-world growth into a high-stakes neon terminal experience.*

**Last updated:** May 9, 2026  
**Current version:** v0.2 (Health + S.P.E.C.I.A.L. + partial Habit/Mission engine)  
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
- Notification system (Neural Pings) with smart contextual scheduling
- Habit CRUD, creation bottom sheet, completion UseCase, and archetype-based seeding
- Goal/Mission/Aspiration models, mapper, repository, and core UseCases
- Database Core foundation in place

### In Progress
- Full integration of Aspirations + Missions into existing Database Core
- Mission Detail + Aspiration Detail screens
- Live health data polishing (HR badge + steps/calories)
- Cognitive testing UI (currently stub — crashes on "Run Adaptive Intelligence Test")

**Phase 2 Progress: ~78% complete**

## Phase 2: Habit Engine + Real-World Grounding (May–July 2026)

**Key Discussion Notes**
- **Live Health Data**: Health Connect is primarily batched/pull-based. True continuous HR is limited (wearables push at intervals). Trending + historical data is excellent. We'll use best-available polling for the HR badge and live steps/calories for now. A companion Wear OS app is a strong future option for true real-time.
- **Holographic Avatar vs Biohacking Interface**: The avatar hub is the personal “About Me / My Story” screen. Biohacking should feel like an active laboratory for experimentation, AI-driven self-hacks, and protocol testing. We will keep them as distinct experiences.

## To Do’s for Tomorrow (May 10)

**Priority (Must Do)**
1. Fix Cognitive Testing Engine crash + implement minimal viable test UI
2. Merge Aspirations + Missions into existing Database Core screen (additive only)
3. Finalize Aspiration Detail + Mission Detail screens
4. Polish live health metrics (HR pulsing badge + steps/calories)

**High Value**
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
