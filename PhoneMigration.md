# Neon Ascent – Phone Migration (Identity Forge Bypass + Full Backup Completeness + Offline P2P)

**Status**: Design lock for next workstream  
**Updated**: October 21, 2026  
**Repo**: `mojo70/Neon-Ascent`  
**HEAD at lock**: current main (post-AiPersonality)

This is the device-migration and backup-completeness layer. It is **not** a new tab or fifth wing.  
Onboarding (Identity Forge) gains an escape hatch. Settings gains a sender entry. The payload is the existing `NeonAscentBackupPayload` expanded to cover every durable domain. Transfer is 100 % offline.

---

## Why this exists

User is moving to a new phone. Fresh install currently traps them on Identity Forge: the only ABORT path saves a partial draft and still requires completing the forge. There is no “I already have a backup” or “pull from my old deck” door.

The Full Data Backup pipeline (WorkManager + `FullDataBackupRepositoryImpl` + `NeonAscentBackupPayload` v2) covers Workout, Biometrics, Codex (lore/quotes), Journal (journal + prayers + chronicle), Character (UserCharacter + OperativeProfile + dopamine), and Settings. It does **not** cover:

- Ascension V3 Goals (Directive / Mission / Task)
- Vault
- Self Map (items, snapshots, mood, EoE)
- Biohacking (saved reconstitutions, protocols, logs)
- Chat / Neural memories

Those domains are now load-bearing. A restore that drops them is a failed migration.

---

## Locked product calls

1. **Identity Forge ABORT is an escape hatch, not a dead end.**  
   On a fresh install (or incomplete creation), tapping ABORT opens a modal with exactly four options:
   - RESTORE FROM BACKUP / VAULT → system file picker for the existing JSON backup.
   - MIGRATE FROM NEARBY DEVICE (QR) → CameraX scanner.
   - BYPASS ONBOARDING (SKIP) → minimal default profile + `isCreationComplete = true` + `setOnboardingComplete(true)` → Main Hub.
   - CANCEL → stay on the forge.

2. **File restore and P2P restore share one pipeline.**  
   Both end in `restoreBackupJson(...)` (REPLACE mode for full migration). After success: character sync (OperativeProfile “default_user” + UserCharacterDao), avatar path sanitization, `setOnboardingComplete(true)`, navigate to Main Hub. No second restore path.

3. **Payload expansion is additive and versioned.**  
   Bump `NeonAscentBackupPayload.version` to 3. New optional sections:
   - `goalsPayload` — primary: AscensionDirectiveDto / AscensionMissionDto / AscensionTaskDto. Secondary (frozen, import-if-absent): legacy GoalDto.
   - `vaultPayload` — VaultAccount, Snapshot, Watchlist.
   - `selfMapPayload` — SelfItem, Snapshot, Mood, EoEEntry.
   - `biohackingPayload` — SavedReconstitution, BioProtocolLog, etc.
   - `chatPayload` — NeuralMemory / ChatSession / ChatMessage as currently persisted.

4. **P2P is 100 % offline, serverless, local Wi-Fi / hotspot only.**  
   - Sender (old phone): generate ephemeral AES-256-GCM key + single-use token, start short-lived local HTTP server, display cyberpunk-styled QR containing `http://<local-ip>:<port>`, token, and key.  
   - Receiver (new phone): scan → OkHttp download (progress) → decrypt → SHA-256 verify → hand to `restoreBackupJson`.  
   - Server lifetime ≤ 10 minutes, single-use token, bind to local interface only. No cloud, no Google Nearby Connections required.

5. **Sender entry point lives in Settings under existing DATA / backup section.**  
   “MIGRATE TO NEW DEVICE (QR)”. Receiver surface only appears on the Identity Forge abort path (and optionally later in Settings for non-fresh installs — out of scope for V1).

6. **Dual visual mode is mandatory.**  
   All new UI (abort modal, QR server view, scanner dialog, progress) must derive colors from `LocalNeonTheme` / `LocalVisualMode`. Suppress glow, scanlines, and forced neon in STEVE. Do not hardcode `Color(0xFF020202)` or pure neon on these surfaces. (Existing Identity Forge hardcoding is a known debt; do not make it worse.)

7. **No fifth wing. No new top-level nav. No cloud dependency.**  
   Chronicle, Codex, LABS, DECK stay exactly where they are. Backup remains the existing FullBackupWorker + local vault + optional Drive path. P2P is an additional transport, not a replacement.

---

## Architecture fit

- Domain contracts stay in `:core:domain` (`FullBackupModels.kt`, `FullDataBackupRepository`).  
- Implementation stays in `FullDataBackupRepositoryImpl` (cross-database: `NeonAscentDatabase` + legacy `AppDatabase`).  
- New P2P classes live under `:app` (or `:core:data` if pure) — `P2pMigrationServer`, `P2pMigrationClient`, `QrCodeUtils`, `QrCodeScannerDialog`.  
- Onboarding state remains the existing `isCreationComplete` flag + DataStore `setOnboardingComplete`. Bypass and successful restore both set both.  
- Goals: Ascension V3 is the winner (per GoalStack lock). Legacy GoalEntity is included only for completeness of old backups.

---

## Verification

**Automated**
- `FullDataBackupRepositoryImplTest`: export includes all new sections; restore round-trips Directives/Missions/Tasks, Self Map, Vault, Biohacking, Chat, OperativeProfile, lore unlocks.
- `CreationViewModelTest`: abort modal actions mark onboarding complete; bypass creates minimal profile.

**Manual**
1. Fresh install → ABORT → options visible.  
2. File restore of a v3 backup → Main Hub with full data, onboarding complete.  
3. Bypass → Main Hub with minimal profile.  
4. Old device Settings → MIGRATE → QR visible. New device scan → download/decrypt/restore → Main Hub.

---

## Out of scope (V1)

- Cloud P2P or account-based transfer.  
- Incremental / differential migration.  
- Migrating Health Connect raw samples (HC is device-local; re-ingest on new phone).  
- Auto-discovery without QR.  
- Changing the existing periodic FullBackupWorker schedule or Drive path.

---

## Non-negotiables (reassert before every prompt)

- Privacy-first, local-only.  
- Ascension V3 is the primary goals model.  
- Dual visual mode (CYBER / STEVE) on every new surface.  
- Single restore pipeline.  
- No fifth wing.  
- Character restore hits both OperativeProfile and UserCharacterDao.  
- Onboarding complete flag set on every successful path out of the forge.  
