# Neon Ascent – Labs Focus

**Status**: Design lock  
**Updated**: October 31, 2026  
**Repo**: `mojo70/Neon-Ascent`  
**Studio pack**: `LabsFocusGeminiPrompts.md` (local only)  
**Depends on**: Outcome Domains shared by `Biohacking_Diets_Database_v0.1.md`, `Biohacking_Protocols_Database_v0.1.md`, `Biohacking_Supplements_Database_v0.1.md`; `GoalStack.md` (V3 is the only goal system); `Codex.md` (STACK wing); LABS = live cockpit.

DECK frozen. No fifth wing. No second goal hierarchy. No second create door for goals.

---

## Product call

Labs gets a selectable **Focus** card: one primary Outcome Domain + zero or more supporting domains.

This is a lens, not a goal.

It personalizes ranking and highlighting on LABS biohacking surfaces and on Codex STACK catalog panels. It does not create Directives, Missions, or Tasks. Creation of dated, measurable big goals stays in Forge.

```
LabsFocus (preference)
  primary: OutcomeDomain?          // exactly one or null
  supporting: Set<OutcomeDomain>   // 0–N
```

Example: primary = Longevity / Healthspan; supporting = Physical Performance / Recovery + Mental Energy / Fatigue Resistance.

---

## Why this surface

The three biohacking catalogs already share Outcome Domain as the primary filter. Users already think in those terms (“my main goal is longevity, but I also want performance and mental energy”).

A Labs card that lets them pick primary + supporting makes the catalogs and “what now” surfaces respond without inventing a parallel goal system.

---

## Locked decisions

| Decision | Value |
|---|---|
| What it is | Preference / lens over existing Outcome Domains |
| What it is not | Not a Directive, Mission, Task, or any other goal entity |
| Storage | DataStore or small preferences pattern. Not Ascension tables |
| Cardinality | Exactly one primary (or none). Zero or more supporting. Primary cannot also be in supporting |
| Free text | None. Selection only from the shared domain list |
| Progress / dates | None |
| Create surface | LABS card only. Does not open Forge. Does not write Ascension |
| Optional later | Soft “Forge a Directive for this?” deep-link. Not in v1 |
| Backup / P2P | Include in next payload version. Same workstream. Single restore path |
| Dual visual mode | Required on the card and any new surfaces |
| Fifth wing | No |

---

## Outcome Domains (shared)

Use the domains already published in the three catalogs. Do not invent new ones in this pack.

Current set (extend only when a catalog adds one):

- Longevity / Healthspan
- Physical Performance / Recovery
- Mental Energy / Fatigue Resistance
- Sleep Architecture
- Stress Resilience / Mood
- Attention / Focus

Map to a stable enum (or sealed type) used by both the catalogs and LabsFocus. Keep the display names human and the keys stable for backup.

---

## LABS surface

Placement: prominent compact card on the biohacking / Labs sheet (near existing active diet / protocols / stack tiles). Not on DECK.

Shows:

- Primary domain (large, labeled PRIMARY)
- Supporting domains as chips (or “none”)
- Tap → edit sheet or inline picker

Edit:

- Single-select for primary
- Multi-select for supporting (exclude the current primary)
- Clear primary allowed (empty focus is valid)
- Save on dismiss or explicit save. Persist immediately.

Empty state: “Set a focus” with the domain list. Not a void.

Does not show progress, dates, affirmations, or outcome statements. Those belong on Directives.

---

## How the focus is used

**LABS (live)**

- Rank or highlight active diet / protocols / stack items whose `primaryOutcomeDomains` intersect the user’s primary + supporting set.
- Primary matches float above supporting matches.
- Items with no domain overlap still appear; they are just not boosted.
- Does not auto-activate or auto-pause any protocol, diet, or supplement. Selection of what is actually running stays where it already lives.

**Codex STACK (archive)**

- DIET / PROTOCOLS / SUPPLEMENTS panels default to the same ranking (primary then supporting then rest).
- Always offer a “show all” escape so the full catalog remains reachable.
- SERUM is unaffected (bloodwork is not domain-filtered this way).

**Not used for**

- Neural Brief copy (unless a later explicit shade pack says so)
- Directive creation or ranking
- Workout protocol selection
- DECK chrome

---

## Data model

```kotlin
enum class OutcomeDomain {
  LONGEVITY_HEALTHSPAN,
  PHYSICAL_PERFORMANCE_RECOVERY,
  MENTAL_ENERGY_FATIGUE,
  SLEEP_ARCHITECTURE,
  STRESS_RESILIENCE_MOOD,
  ATTENTION_FOCUS
}

data class LabsFocus(
  val primary: OutcomeDomain? = null,
  val supporting: Set<OutcomeDomain> = emptySet()
)
```

Invariant: if primary is non-null it is not present in supporting. Enforce on write.

Preferences / repository:

- `observeLabsFocus(): Flow<LabsFocus>`
- `setLabsFocus(focus: LabsFocus)`

---

## Backup + P2P

Include `LabsFocus` in the next payload version.

- Serialize primary + supporting as stable enum keys (not display strings).
- Single restore path. Round-trip test.
- Empty focus is valid and must restore as empty.
- Catalog seeds remain non-user data; this preference is user data.

---

## Guardrails

- Do not write to Ascension tables from this surface.
- Do not add a second goal create UI.
- Do not add free-text goals.
- Do not invent Outcome Domains that are not in the catalogs.
- Do not auto-mutate active stacks, diets, or protocols based on focus.
- Dual visual mode on the card. DECK frozen. No fifth wing.
- Honor-system: empty focus is fine. Do not shame it.

---

## Locked product calls (2026-10-31)

1. Labs Focus is a preference over existing Outcome Domains. Not a goal.
2. One primary + zero or more supporting. Selectable on the LABS biohacking card.
3. Used for ranking / highlighting on LABS and Codex STACK catalog panels. Does not activate or deactivate anything.
4. Ascension V3 remains the only goal system. Forge remains the only create door for Directives.
5. Backup + P2P in the same workstream. Dual visual mode. No fifth wing.
