# Neon Ascent – Backup & P2P Completeness (Standing Rule)

**Status**: Standing lock  
**Updated**: October 23, 2026  
**Repo**: `mojo70/Neon-Ascent`  
**Prior locks**: `PhoneMigration.md` (v3), `PhoneMigrationV2.md` (v4), `JourneyContinuity.md` (v5)  
**Applies to**: Every future durable user-facing feature

A restore that drops a domain the user created is a failed migration. New features must ship their backup + P2P inclusion in the same workstream, not as later cleanup.

---

## Standing rule

Any new feature that creates durable user data (rows the user typed, configured, or accumulated) must include:

1. **Payload section (or additive fields)** in `NeonAscentBackupPayload` / `FullBackupModels.kt`.
2. **Version bump** (additive). Older payloads still restore.
3. **Export + restore** in `FullDataBackupRepositoryImpl` (or the single restore path that both file and P2P call).
4. **Parent-before-child order** if the new data has foreign keys.
5. **Round-trip test** (at least one automated case that exports, restores, and asserts the new rows survive).
6. **No secrets.** Tokens, keys, biometric material stay out.

File restore and P2P QR both call the same `restoreBackupJson`. There is no second path.

If a feature is pure seed / catalog / read-only reference with no user rows, it does not need a payload section.

---

## What counts as durable user data

- User-created or user-edited entities (customs, notes, configurations)
- Progress, history, streaks, or state that would be painful to re-enter
- Preferences that change daily behavior (timers, layout, intake mode, pulse schedule, etc.)

Seed catalogs, static assets, and derived caches do not count.

---

## Arsenal (first application)

When Arsenal customs ship:

- Custom variants and custom families are user-scoped rows.
- They must be included in the workout / exercise section of the payload (or a tightly-coupled sibling section restored with it).
- Restore must re-attach them to the correct family or slot so ranking and ranges still work.
- Seed catalog rows are not re-exported; they are re-seeded on the new device.
- Bump payload version. Older backups (no customs) still restore.

Do not ship the Arsenal custom UI and then add the backup later.

---

## Checklist for new feature locks

Add this block to the feature lock (or its Gemini pack) before implementation starts:

```
Backup & P2P
- [ ] New user rows identified
- [ ] Payload section or fields added (version bumped)
- [ ] Export in FullDataBackupRepositoryImpl
- [ ] Restore in the single restoreBackupJson path
- [ ] Parent-before-child order respected
- [ ] Round-trip test
- [ ] File + P2P both covered (same pipeline)
```

---

## Non-negotiables

- One restore pipeline.
- Additive payload versions.
- No fifth wing or new transport.
- Dual visual mode on any new restore UI.
- No secrets in the payload.
- New durable user data is in the backup before the feature is considered done.
