# Neon Ascent – Sanctuary (scripture seed)

**Status**: Design lock  
**Updated**: 2026-10-30  
**Repo**: `mojo70/Neon-Ascent`  
**Studio pack**: `SanctuaryGeminiPrompts.md` (local only — do not push)  
**Depends on**: `LibraryVault.md` (Altar catalog parallel), `BackupCompleteness.md`  
**Not this pack**: Altar daily prayer text, Chronicle, goals, fifth wing

DECK frozen. No fifth Codex wing. No third database. Do not dump passages into `neural_memories`.

---

## Product

Sanctuary is a focused, theme-tagged scripture seed for protection, warfare, end-times readiness, healing, and authority of the believer.

- Parallel to the Altar catalog. Does not replace it.
- Seed-only catalog. Never user prose.
- Surfaces: own sheet or deep-link from Altar / Neural Brief / Codex. First-open default deck = Psalm 23 + Psalm 91 + Ephesians 6:10-18.
- Translation locked to ESV for this seed.
- Themes and tags drive filters. Biggs-priority passages are tagged.

Altar owns daily prayer text + rite timing (PRAYER / SIT / KEGEL) + character streak. Sanctuary owns the scripture passages.

---

## Data model

```kotlin
data class SanctuaryPassage(
    val id: String,                    // stable, e.g. "psalm_91"
    val book: String,
    val chapter: Int,
    val verses: String,                // "1-6" or "10-18" or "5"
    val reference: String,             // "Psalm 91"
    val translation: String = "ESV",
    val title: String?,
    val text: String,                  // full passage
    val themes: List<String>,
    val tags: List<String>,            // e.g. "biggs-priority", "protection-core"
    val sourceNote: String?,
    val sortOrder: Int,
    val isFavoriteDefault: Boolean = false
)
```

Table (if Room): `sanctuary_passages` (prefixed, seed-only, replace on version bump).  
Or assets JSON / Kotlin seed object matching the `DailyPrayerSeeds` pattern. Insert-if-absent by id.

User favorites or progress (if added later) live on the character or a small user table and are included in backup payload. Catalog itself is not user data.

---

## Seed (v0.1)

See `SanctuarySeeds.md` for full ESV text.

Priority order (sortOrder):

1. psalm_23 — shepherd, provision, protection, rest — biggs-priority
2. psalm_91 — protection, refuge, angels, pestilence — biggs-priority
3. eph_6_10_18 — warfare, armor, authority — biggs-priority
4. isa_53_5 — healing, atonement
5. matt_6_33 — priority, kingdom
6. 1john_4_4 — authority, greater-is-he
7. psalm_46 — refuge, strength, be-still
8. psalm_27 — light, salvation, fear, dwelling
9. psalm_121 — help, keeper
10. isa_54_17 — no-weapon, vindication
11. rom_8_31_39 — more-than-conquerors
12. 2tim_1_7 — no-fear, power
13. james_4_7 — submit, resist
14. luke_10_19 — authority, enemy
15. psalm_34_7 — angels, encamp
16. phil_4_6_7 — anxiety, peace, prayer

Matthew 24 (selected verses) is deferred to a later end-times group.

---

## Backup + P2P

Standing rule (`BackupCompleteness.md`): if users can favorite or track progress through passages, include those fields in the next payload version, single restore path, and a round-trip test. Pure catalog seed does not require a payload bump until user state exists.

---

## Out of scope

Altar daily prayer text. Chronicle prose. Fifth wing. User-authored passages in the catalog. Non-ESV translations in v0.1.
