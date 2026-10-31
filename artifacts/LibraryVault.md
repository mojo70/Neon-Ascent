# Neon Ascent – Library, Altar catalog, Sanctuary, Vault

**Status**: Design lock  
**Updated**: 2026-10-30  
**Repo**: `mojo70/Neon-Ascent`  
**Studio pack**: `LibraryVaultGeminiPrompts.md`  
**Depends on**: `DataSpine.md` (character owns streak flags), `RepoHardening.md`, `Sanctuary.md`  
**Not this pack**: Chronicle prose (`ChronicleSpine.md`), goals (`GoalStack.md`), quests, sayings

DECK frozen. No fifth Codex wing. No third database. Do not dump books, prayers, or scripture passages into `neural_memories`.

---

## What still lives on AppDatabase

| Surface | Tables | Product home |
|---|---|---|
| Library | `books`, `chapters`, `highlights`, `quotes` (`BookDao`) | E-reader / library feature |
| Altar catalog | `daily_prayers` (`DailyPrayerDao`) + `DailyPrayerSeeds.kt` | Altar. Streak is on **character** |
| Sanctuary catalog | `sanctuary_passages` (or assets/Kotlin seed) | Sanctuary. Parallel to Altar. See `Sanctuary.md` |
| Vault | `asset_accounts`, `asset_snapshots` (`NetWorthDao`), `watchlist` (`StockDao`) | Cyberdeck wallet / NetworkHub |
| Leave | quests, sayings, `corpo_trust` | `QuestSayings.md` for quests/sayings; trust stays game state |

EPUB binaries live in `app/src/main/assets/library/`. They are not rows. Play Asset Delivery is hardening-adjacent, not this pack.

---

## Winner per surface

### Library

Core tables with prefixed names (avoid colliding with anything named `books` later):

- `library_books`
- `library_chapters`
- `library_highlights`
- `library_quotes`

Domain `LibraryRepository`: get book, chapters, highlights, quotes, upsert progress/highlight/quote.

Asset file path stays an asset URI (`library/Meditations.epub`). Do not blob the EPUB into Room.

### Altar catalog

`daily_prayers` is a **seed catalog** (day number → text), not a journal.

- Do **not** import into `neural_memories`.
- Do **not** invent a second streak table.
- `prayerStreak` / `lastPrayerDate` / baptism flags stay on domain `UserCharacter` (DataSpine).
- Catalog may stay as `DailyPrayerSeeds` / assets JSON. If you add `altar_prayers` in core, it is seed-only (replace on version bump), never user prose.

Altar session timing (sits ≥10 min) is already CHARGE’s sit mask (`NeonCharge.md`). Do not double-count as prayer points.

### Sanctuary catalog

Parallel seed catalog of theme-tagged scripture passages (ESV). See `Sanctuary.md` and `SanctuarySeeds.md`.

- Seed-only. Never user prose.
- Prefixed table or assets/Kotlin seed matching the Altar pattern.
- Does not replace or merge into the Altar daily prayer catalog.
- User favorites/progress (if added later) follow the backup completeness rule.

### Vault

Core tables:

- `vault_accounts`
- `vault_snapshots`
- `vault_watchlist`

Domain `VaultRepository`. Finnhub calls stay behind the existing API. Do not log API keys. Watchlist symbols are not vitals.

Character `eddies` / `secureEddies` / `walletConnected` stay on the character row. Vault accounts are the detailed ledger; do not fork a second eddie balance in NetworkHub state.

---

## Copy rules

Insert-if-absent by stable id (book id, highlight id, account id, symbol, passage id).  
If copy throws, leave AppDatabase rows. No `deleteDatabase`. No destructive migration.  
Do not drop app tables this pack.

---

## Out of scope

Journal/chat/lore. Goals. Quests/sayings. ONNX/EPUB Play delivery. Garmin. Pulse. DECK chrome. Merging the two Room files into one `@Database` list in a single turn — add only the new prefixed tables + migrations.
