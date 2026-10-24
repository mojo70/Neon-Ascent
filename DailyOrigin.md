# Neon Ascent – Daily Origin (Day Card)

**Status**: Design lock  
**Updated**: October 23, 2026  
**Repo**: `mojo70/Neon-Ascent`  
**Depends on**: `ChronicleSpine.md`, `BodyLog.md`, `BackupCompleteness.md`  
**Not this pack**: Correlation / AI analysis (next pass), Neural Brief auto-shade, new Codex wing

First working slice only: aggregation + free text. Correlation engine is a later pass once the day payload is stable and used.

DECK frozen. No fifth Codex wing. No third database. Dual visual mode on any new surface.

---

## Why this exists

Structured signals (workout, rites, sleep, BODY_LOG, biohacking protocols) and free-text journal already exist but are scattered. A single day-level surface that assembles them by date gives a usable “what happened today” view without forcing a new store or waiting on local AI quality.

User gets hands on the aggregation first. Analysis option stays gated until ≥7 days of data exist.

---

## Locked product calls

1. **Day card is the daily home.**  
   One card (or sheet) per calendar day. Auto-assembles read-only structured signals that already exist for that date. Empty sections collapse or show “none.” User can dismiss any section.

2. **Both surfaces to start.**  
   - LABS sheet (primary logging context)  
   - Codex CHRONICLE header / day selector (archive context)  
   May collapse to one later after hands-on use. No new top-level tab.

3. **Free text stays in Chronicle.**  
   Tapping the note area (or explicit “Journal”) opens existing Chronicle compose, pre-linked to the day. Archive remains `neural_memories` wing `CHRONICLE` room `ORIGIN`. Direct Chronicle open still works; day link is optional.

4. **Structured payload (references only).**  
   Day object holds date + optional IDs / summaries of:  
   - Workout session (OPS)  
   - Rites that day (PRAYER / SIT / KEGEL)  
   - Prior-night SANCTUM  
   - BODY_LOG entries (weight, BP, tape, etc.)  
   - Active biohacking protocols / stacks on that date  
   No duplication of the underlying rows. Structured data stays in its current homes.

5. **Protocols.**  
   If a protocol or supplement stack was active on the date (or marked running that day), it is included. “Currently active as of that date” is sufficient for v1.

6. **No AI in this slice.**  
   Analysis / correlation button is not present. 7-day gate (at least one structured signal or journal entry on ≥7 distinct days) will be added when the correlation pass starts. Button stays hidden or disabled until then.

7. **Storage.**  
   Thin day-link entity or metadata on the Chronicle entry is acceptable. Prefer metadata / existing tables if possible. Do not create a parallel journal store.

---

## Backup & P2P

- [ ] New user rows identified (day links / metadata that survive restore)
- [ ] Payload section or additive fields (version bump to v6)
- [ ] Export in FullDataBackupRepositoryImpl
- [ ] Restore in the single restoreBackupJson path
- [ ] Parent-before-child order respected
- [ ] Round-trip test
- [ ] File + P2P both covered (same pipeline)

Older payloads still restore. No secrets.

---

## Out of scope (this lock)

Correlation engine, precomputed stats, local Gemma prompt, insight storage, Neural Brief integration, auto-generation of insights, new data model for metrics. Those are the next pass after the day card is usable.