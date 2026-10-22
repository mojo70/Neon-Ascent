# Phone Migration Manual Verification Checklist

## 1. Fresh Install ABORT Escape Hatch
- [x] Launch app with a fresh/empty database (or reinstall).
- [x] On Identity Forge screen, tap the **ABORT** button in header.
- [x] Verify the **ABORT IDENTITY FORGE** dialog appears with 4 options:
  1. `RESTORE FROM BACKUP / VAULT`
  2. `MIGRATE FROM NEARBY DEVICE (QR)`
  3. `BYPASS ONBOARDING (SKIP)`
  4. `CANCEL`
- [x] Verify button styling respects `LocalNeonTheme` / `LocalVisualMode` (CYBER & STEVE).

## 2. File Restore Flow
- [x] Tap **RESTORE FROM BACKUP / VAULT** (or the header **RESTORE BACKUP** button).
- [x] Select a valid v3 backup JSON file from the system file picker.
- [x] Verify database restoration succeeds, onboarding complete flag is set, and app navigates directly to `MainHub`.
- [x] Verify Workouts, Biometrics, Goals/Directives, Vault, SelfMap, Biohacking, and Chat history are fully populated in Main Hub.

## 3. Bypass Onboarding Flow
- [x] On Identity Forge -> ABORT -> tap **BYPASS ONBOARDING (SKIP)**.
- [x] Verify default minimal character profile ("OPERATIVE") is created.
- [x] Verify onboarding is marked complete and app navigates directly to `MainHub`.

## 4. Offline P2P QR Device Migration Flow
- [x] **Sender Device (Old Phone)**:
  - Open `Settings` -> `DATA_AND_CLOUD_BACKUP`.
  - Tap **MIGRATE TO NEW DEVICE (QR)**.
  - Verify QR Code dialog appears with connection status `WAITING FOR CONNECTION...` and Local IP/Port display.
- [x] **Receiver Device (New Phone)**:
  - On Identity Forge -> ABORT -> tap **MIGRATE FROM NEARBY DEVICE (QR)**.
  - Grant Camera permission if prompted.
  - Scan the QR code displayed on the sender device screen.
- [x] **Transfer & Completion**:
  - Verify receiver shows real-time progress (`DOWNLOADING BACKUP PAYLOAD... X%` -> `DECRYPTING...` -> `RESTORING...`).
  - Verify sender updates status to `MIGRATION COMPLETE` and automatically shuts down.
  - Verify receiver completes restoration, sets onboarding complete, and opens `MainHub` with all transferred data intact.

## 5. Dual Visual Mode Contrast Audit
- [x] **CYBER Mode**: Dark canvas (`#050505`), neon green/pink borders, high contrast monospace typography.
- [x] **STEVE Mode**: Clean white canvas (`#FFFFFF`), dark ink text (`#000000`), clear borders, zero forced neon glow or unreadable dark backgrounds.
