# Neon Ascent – Arsenal (Exercise Catalog Surface)

**Status**: Design lock  
**Updated**: October 22, 2026  
**Repo**: `mojo70/Neon-Ascent`  
**Studio pack**: `ArsenalGeminiPrompts.md`  
**Depends on**: `ExerciseCatalog.md` (family + implement + stance data model, slot map, rotation rule), `CyberCrapp.md`, `CCProgram.md`  
**Not this pack**: Protocol engine math, Codex wings, data-spine merges, deload automation

Protocol Archive is for engines and dossiers (how the system trains you). Arsenal is the lift library (what patterns and implements exist, and what can rotate into a slot). They are different objects. Do not nest the browsable catalog inside a protocol dossier.

---

## Product lock

1. **Own surface.** Arsenal is a top-level (or clear Library-area sibling card), not a section inside Protocol Archive. Protocol dossiers may deep-link into it (“see the thickness pool”), but Arsenal owns the browse.
2. **Grouping is Slot → Family → Implement.** Stance chips appear only when the chosen family + implement has more than one stance. Matches the existing picker and rotation rule.
3. **Ranking is fixed.** Same family / other implement first, then same CyberCrapp slot. Do not invent a third ranking axis for V1.
4. **Short tags only for V1.** One to three short tags per family or variant. No essays. Examples: `LOWER_BACK_FRIENDLY`, `LENGTHENED_PARTIALS`, `CONSTANT_TENSION`, `FORM_PRIORITY`, `AXIAL_LOAD`, `UNILATERAL`.
5. **Customs allowed.** User can create a custom variant (or family) that still slots into an existing family or slot so ranges and rotation keep working. Custom does not become a free-floating string.
6. **Logging identity unchanged.** `workout_logs.exerciseId` stays the variant row. Arsenal never logs against a family id.
7. **Shared ranking.** Arsenal browse, stall/rotate sheet, and manual Force Rotate all call the same domain ranking. No divergent substitute lists.
8. **Read + act.** Browse is the primary job. From a card the user can “Use as substitute” (pre-fills the rotation or template swap) or “Add to template.” No silent mutation.

---

## Entry points

| From | Behavior |
|---|---|
| Main Library / Protocol area | Arsenal card → full catalog |
| Protocol dossier | Deep link into the relevant slot or family for that engine |
| Workout template editor | “Browse alternatives” on a slot opens Arsenal pre-filtered |
| Stall / Force Rotate | Opens the same ranked list the catalog would show for that exercise |
| Exercise header (log) | Optional “View family” that opens Arsenal at that family |

Primary entry is the Arsenal card. Deep links are secondary.

---

## Catalog structure (V1)

Reuse the slot map already locked in `ExerciseCatalog.md`:

| Slot | Families (rotation pool) |
|---|---|
| Horizontal press | Bench Press, Push-Up, Dip (chest lean) |
| Vertical press | Overhead Press, Landmine Press |
| Vertical pull | Pull-Up, Chin-Up, Lat Pulldown |
| Horizontal pull | Row, Chest-Supported Row, Face Pull (accessory) |
| Quad / squat | Squat, Leg Press, Hack Squat, Belt Squat, Split Squat, Lunge |
| Hinge | Deadlift, Romanian Deadlift, Good Morning, Back Extension, KB Swing |
| Elbow flex | Curl, Jerry Curl, Hammer Curl, Preacher Curl |
| Elbow extend | Tricep Extension, Pushdown, Skull Crusher, Close-Grip Press, Dip |
| Delts isolation | Lateral Raise, Rear Delt Fly |
| Posterior isolation | Leg Curl, Hip Thrust, Nordic / GHR |
| Calves | Calf Raise |
| Abs | Cable Crunch, Hanging Raise, Ab Wheel |

Inside a slot the user sees families. Inside a family they see implements (and stance only when needed). Current variant is highlighted when arriving from a specific exercise.

For back thickness the path is Horizontal pull → Row family → implements (bent-over barbell, single-arm DB, seated cable, plate-loaded, chest-supported T-bar, inverted, KB single-arm). Short tags explain why one might replace another. Protocol range for the family/slot is shown (thickness = 10–15 cluster).

---

## Short tags (V1)

Stored as a small list on the family (copied to variants) or overridden on a variant. Display as neon chips. Do not free-text.

Seed tags that matter for the common swaps:

- Row / thickness: `FORM_PRIORITY`, `LENGTHENED_PARTIALS`; chest-supported variants get `LOWER_BACK_FRIENDLY`
- Pull-up / width: `BACK_WIDTH`
- Bench family: `LENGTHENED_PARTIALS` on incline / floor where relevant
- Squat family: `AXIAL_LOAD` on barbell back; machines get `LOWER_BACK_FRIENDLY`
- RDL / hinge: `POSTERIOR_CHAIN`

Add more only when a real swap decision needs them. Do not tag every accessory.

---

## Customs

- User can add a custom **variant** under an existing family (name, implement, optional stance, optional tags). It inherits the family’s movementType, range, and slot.
- User can add a custom **family** only by picking an existing slot (so it enters the rotation pool). It gets a movementType from the slot’s default.
- Customs are user-scoped rows. They do not mutate the seed catalog.
- “Use as substitute” works for customs the same as seeded variants.
- Delete / archive is allowed for customs only. Seed rows are immutable.

---

## What it is not

- Not a fifth Codex wing.
- Not a replacement for the two-step add-exercise picker.
- Not a place to edit protocol rules or ranges (those stay on the family / protocol_rep_targets).
- Not a free-text exercise dump. Customs still slot.
- Not Protocol Archive content. Archive stays dossiers.

---

## UI contract

1. Top level is Slot list (searchable). Tap slot → family cards.
2. Family card shows name, short tags, protocol range, and “implements” count. Tap → implement chips (stance secondary).
3. Variant card shows display name, tags, and last-used / best-cluster if logged (optional V1, nice).
4. Actions on a variant: Use as substitute, Add to template, View in Codex (if history exists).
5. Create custom is a FAB or sheet on the family or slot screen. Requires family or slot choice.
6. Neon theme. Match existing Library / Archive card density. No new visual language.
7. Empty state is a hook: “No customs yet. Seed catalog is ready.”

---

## Engagement notes

- The win is “I can see the thickness alternatives before I decide to rotate,” not a bigger exercise list.
- Short tags make the recommendation feel intentional instead of a random substitute dump.
- Customs keep power users from feeling locked out without breaking the family/slot model that makes rotation smart.
- Shared ranking means the catalog and the stall sheet never disagree.
