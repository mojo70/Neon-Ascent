# Neon Ascent – CyberCrapp Rotation Modes (Straight vs A/B)

**Status**: Design lock  
**Updated**: October 26, 2026  
**Repo**: `mojo70/Neon-Ascent`  
**Extends**: `CyberCrapp.md` (v2.1)  
**HEAD at lock**: current main

CyberCrapp keeps the 3-way Push / Pull / Legs rotation and the ~2× / 8–9 day frequency. This lock adds a user-selectable rotation mode so the same protocol can favor either concentrated strength practice or lower overuse risk.

Still not a new wing. Still one primary per muscle per session. Dual visual mode on any new UI. Backup + P2P for the preference in the same workstream.

---

## Why this exists

Classic DoggCrapp used an A/B body-part split plus a 1-2-3 exercise rotation. CyberCrapp deliberately uses a 3-way PPL-style split and, until now, a single exercise list per day type with reactive rotation (fail the log twice → substitute).

User decision (2026-10-26):

- Keep the straight single-list version for strength focus.
- Add an optional A/B per day type (Push A/B, Pull A/B, Legs A/B) for injury-risk reduction and sustainable variety.
- The user can choose either.

---

## Locked product calls

1. **Two modes, user-selectable.**  
   - **STRAIGHT** (default, strength-focused): one exercise list per Push, Pull, and Legs. Same primaries every time that day type appears until the existing “fail twice → rotate” rule fires.  
   - **A/B** (injury-risk / variety): each day type has two variants — Push A / Push B, Pull A / Pull B, Legs A / Legs B. Variants alternate within that day type.

2. **What A/B means.**  
   - Push A and Push B are two different primary selections (or emphasis shifts) for the same muscle groups. Same for Pull and Legs.  
   - Alternation is per day type, not global. Example: Push A → (later) Push B → Push A… independently of whether Pull or Legs has run.  
   - Logbook comparison stays per specific exercise (compare this incline session to the last incline session, not to last Push day).

3. **Frequency and session structure unchanged.**  
   - Still ~2× per muscle every 8–9 days.  
   - Still one primary compound per muscle per session.  
   - Still rest-pause cluster + lengthened partials + loaded stretch on primaries.  
   - Still the existing rep ranges, widowmaker squat rule, and soft deload.

4. **Straight mode stays the strength path.**  
   Higher practice frequency on the exact lifts the user cares about. No forced variation. Reactive rotation only.

5. **A/B mode is the overuse-reduction path.**  
   Different primary movement pattern every other exposure for that day type. Does not claim extra hypertrophy; it reduces repetitive strain on the same joints/tendons while keeping effort and frequency.

6. **Where the choice lives.**  
   Protocol setup / Settings (or the existing protocol configuration surface). Default = STRAIGHT. Switching modes does not delete history. Existing logged sessions remain valid.

7. **Seed content.**  
   Provide a minimal default pair for A/B (user can edit):  
   - Push A: flat bench + military press  
   - Push B: incline or DB press + different shoulder press  
   - Pull A: weighted pull-up + barbell row  
   - Pull B: chin-up or pulldown + different row  
   - Legs A: back squat + RDL  
   - Legs B: front / hack / leg press + different posterior  
   Exact exercises stay customizable. Jerry Curls and other accessories can live on either variant.

8. **Out of scope**  
   - Full 1-2-3 (three variants). Can be added later if wanted.  
   - Changing the 3-way split itself.  
   - Auto-switching modes based on recovery.  
   - New wing.

---

## Architecture fit

- Persist the mode (STRAIGHT | A_B) in the existing workout profile or protocol config. Include it in the backup payload (preferences or workout section) and round-trip test it.
- Schedule / sequencer uses the mode to pick the next variant for that day type.
- Progression and “fail twice” rules continue to key off exercise identity, not day-type label.
- UI copy should make the tradeoff plain: Straight = strength focus on the same lifts; A/B = lower overuse risk via alternating variants.
- Dual visual mode on the selector.

---

## Verification

**Automated**
- Mode persists and restores via backup.
- In A/B, successive Push days alternate A then B.
- In STRAIGHT, the same Push list is used until a manual or fail-twice rotation.

**Manual**
1. Set STRAIGHT, run two Push days → same primaries.
2. Switch to A/B, run two Push days → different primaries.
3. Confirm logbook still compares like-for-like exercises.
4. STEVE / PILOT: selector stays on-palette.

---

## Non-negotiables

- STRAIGHT remains available and is the default.
- A/B is optional and does not remove Straight.
- Frequency, one-primary-per-muscle, and cluster rules unchanged.
- Logbook comparison is per exercise.
- No new wing.
- Dual visual mode on new UI.
- Backup + P2P for the mode preference in the same workstream.
- Ascension V3 and other locks unchanged.
