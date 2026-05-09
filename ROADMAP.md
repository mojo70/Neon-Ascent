# Neon-Ascent Roadmap

**Cyberpunk Life RPG / Self-Improvement OS**  
*Turn real-world growth into a high-stakes neon terminal experience.*

**Last updated:** May 9, 2026  
**Current version:** v0.1 MVP (character synthesis + basic terminal)  
**Primary repo:** `mojo70/Neon-Ascent` (modular Clean Arch + Jetpack Compose + Hilt + Room + Gemma LiteRT)

## Vision
Neon-Ascent is a **daily driver life OS** wrapped in cyberpunk aesthetics. Real-world habits, biometrics, and completed missions directly upgrade your in-game S.P.E.C.I.A.L. attributes, archetype progression, and cyberdeck capabilities.  

**Absolute privacy-first architecture is non-negotiable.** Every line of code, every permission request, and every data flow is designed so that nothing leaves the device unless the user explicitly opts in. Local-first, on-device AI (Gemma LiteRT), no cloud dependency for core gameplay, no third-party analytics that aren’t fully transparent and opt-outable. This is the foundation that makes the cyberpunk fantasy trustworthy in users’ actual lives.

**All user-visible measures and stats are grounded in real-world, evidence-based metrics.** S.P.E.C.I.A.L. attributes, progress bars, benchmarks, and comparisons are never arbitrary or purely fictional. They are derived from validated population norms, scientific standards, and peer-reviewed fitness/longevity data (e.g., strength standards by age/sex/bodyweight from established sources like Legion Athletics strength tables, CDC/ACS guidelines for steps/sleep/HRV, etc.). The neon RPG flavor enhances the experience — it never replaces the real data.

## Current Status (v0.1 – Shippable Prototype)
- Full character intake (MBTI + Alignment + physical/quiz benchmarks → archetypes + S.P.E.C.I.A.L. derivation using real-world normative data)
- Basic cyberdeck terminal UI with neon theming
- On-device Gemma LiteRT for profile generation and flavor text
- Local-first data layer (Room DB + DataStore)
- Clean modular architecture (feature modules, MVI, Hilt, Coroutines/Flow, Compose)
- Playable APK available

**Next priority (your explicit directive):** Shift from pure entertainment hook to **real daily retention** by building deep **habit/tasking/mission systems**, intelligent reminders, and **usable wearable data ingestion** — all while enforcing absolute privacy and grounding every stat in real-world evidence.

## Phase 2: Habit Engine + Real-World Grounding (Immediate – May–July 2026)

**Goal:** Make daily app use feel like plugging into your own cyberdeck. Real biometrics + consistent action = tangible, data-backed in-game power spikes.

### 2.1 Habit & Mission System (Core Loop)
- Flexible habit creation (daily/weekly/custom recurrence) with archetype-themed suggestions
- Direct mapping of habits to S.P.E.C.I.A.L. attributes using **real-world, evidence-based benchmarks**:
  - Steps/activity → Agility (grounded in CDC/ACS guidelines and population norms)
  - Sleep/HRV/recovery → Endurance (validated sleep science metrics)
  - Focused work blocks / cognitive tasks → Intelligence (evidence-based productivity studies)
  - Strength/mobility benchmarks → Strength (compared against age/sex/bodyweight normative data from peer-reviewed strength standards)
- Procedural + archetype-driven daily/weekly missions that feel personal
- Streak tracking with cyberpunk visual rewards (neon intensity, terminal level-ups, glitch animations) — streaks backed by habit-formation research showing self-monitoring drives adherence
- Progress visualization inside the cyberdeck UI (charts with scanlines, particle effects, dynamic theming based on attribute balance) — every chart displays real normative comparisons (“You’re at the 65th percentile for your demographic”)

### 2.2 Smart Reminders & Notifications ("Neural Pings")
- Context-aware reminders (time-of-day, location, biometric triggers) using evidence-based timing to maximize habit adherence without notification fatigue
- Themed notification styles: holographic alerts, urgent ICE-breach warnings, motivational deck messages
- Use **WorkManager** + **AlarmManager** (exact alarms where permitted) for reliable background delivery
- Respect user focus modes / DND; smart scheduling informed by retention research
- A/B test notification tone & timing for maximum engagement while respecting privacy boundaries

### 2.3 Wearables & Health Data Integration (The "Real" Anchor)
- **Absolute privacy-first implementation:**
  - Primary integration: **Android Health Connect** (2026 standard) — read-only where possible, full user consent and granular permissions per official UI/UX guidelines
  - All processing and derivation happens **on-device**. Raw data never leaves the device unless user explicitly approves export or backup
  - Transparent, plain-language permission flows explaining exactly why each data type powers ascension (no dark patterns)
  - No cloud sync by default. Optional encrypted backup only after explicit opt-in
  - Regular privacy audits baked into the development process
- Background sync via WorkManager (periodic + event-driven)
- Data processing pipeline: raw Health Connect metrics → normalized, evidence-based contributions to S.P.E.C.I.A.L. attributes (using validated formulas and population norms)
- Optional companion Wear OS app (Compose for Wear OS) for real-time HR, quick mission check-ins, or glanceable terminal status — still 100% local-first
- Clear revocation path: one-tap “Disconnect all health data” that immediately stops all reads

**Success metrics for Phase 2** (all measurable via local, privacy-preserving analytics)
- ≥70% of active users logging at least one real-world data point daily
- Measurable lift in 7-day and 30-day retention (tracked via anonymized, on-device events)
- Clear correlation between consistent wearable data + habit completion and in-game attribute progression (grounded in real metrics, not fiction)

**Tech & best-practice notes**
- Dedicated feature modules: `feature:habits`, `feature:health`, `feature:notifications`
- Repository pattern + offline-first + full encryption at rest (Android Keystore + EncryptedSharedPreferences)
- Comprehensive testing (unit + UI + integration) for sync, reminders, permission flows, and privacy edge cases
- Performance budgeting for Compose (neon particles, dynamic terminals, heavy animations)
- Modular monorepo keeps compile times sane as cyberdeck mini-games grow
- Every stat displayed follows the “real-world grounded” rule — no exceptions

**Target ship date:** Closed beta ready by end of July 2026

## Phase 3: Cyberdeck Polish & Immersion (August–October 2026)
- Full ICE Breach, Neural Core, Data-Vault, Edge Intelligence mini-games
- Advanced UI/animations (particle systems, scanlines, dynamic theming based on real attribute balance)
- Biohacking / religion / node system expansion tied to evidence-based habits
- Progress dashboard with on-device AI insights (still privacy-first)
- Performance optimization pass

## Phase 4: Social Ascension & Multiplayer (Q4 2026)
- Optional PvP hacking challenges (opt-in only, no personal data sharing)
- Shared missions / crew systems
- Community archetype sharing (anonymized)
- Leaderboards (anonymized or opt-in)

## Phase 5: Launch & Long-Term (Q1 2027+)
- Closed → open beta via Google Play Internal / Open Testing
- Play Store listing optimization (cyberpunk RPG hook + real results proof)
- Premium features (cosmetic cyberware, advanced AI coaching, export) — still privacy-first
- Potential iOS version (similar local-first stack)
- Hardware accessory explorations (custom cyberdeck companions)

## Development & Contribution Guidelines
- Every major feature starts with a living design doc update (main Google Doc remains source of truth)
- **Privacy-first** and **real-world grounded metrics** are hard requirements — no feature ships without them
- Prioritize privacy, offline capability, and retention metrics
- Measure everything that impacts daily use (on-device only)
- Keep the cyberpunk immersion consistent across every screen

**Overall success metric:** Users open Neon-Ascent every day because it meaningfully tracks and rewards their real habits with scientifically grounded feedback — while delivering the neon RPG fantasy they fell in love with.
