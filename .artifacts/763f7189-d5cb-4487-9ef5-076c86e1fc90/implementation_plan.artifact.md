# Support for 16 KB Page Sizes

Starting November 1st, 2025, Android 15+ devices require apps to support 16 KB memory page sizes. This primarily affects apps with native code (`.so` files). Your current build of `app-debug.apk` is failing because `libsqlcipher.so` (part of SQLCipher) is not aligned to 16 KB boundaries.

## User Review Required

> [!IMPORTANT]
> To fully support 16 KB page sizes while maintaining the performance benefits of uncompressed native libraries, we need to update several dependencies to their latest versions. Some of these updates (like ObjectBox) might involve breaking changes if the version jump is large.

## Proposed Changes

We will update the project's dependencies to versions that are known to be 16 KB compatible. If any library remains incompatible, we can enable "Legacy Packaging" which compresses the libraries (allowing them to work on 16 KB devices via extraction, though with a slight performance hit).

### [Gradle Configuration]

#### [MODIFY] [libs.versions.toml](file:///Users/joeevans/StudioProjects/Neon-Ascent/gradle/libs.versions.toml)
Update versions for SQLCipher, ObjectBox, and ONNX Runtime to ensure 16 KB alignment.
- `sqlcipher`: `4.5.4` → `4.6.1` (or latest)
- `objectbox`: `4.1.0` → `5.4.2`
- `onnxruntime`: `1.25.1` → `1.29.0`
- `security-crypto`: `1.1.0-alpha06` → `1.1.0`

#### [MODIFY] [app/build.gradle.kts](file:///Users/joeevans/StudioProjects/Neon-Ascent/app/build.gradle.kts)
We can optionally set `useLegacyPackaging = true` if we want to ensure compatibility for all libraries immediately, even those that might not be updated yet. However, since you are on a very recent version of AGP (9.3.1), the preferred approach is to use uncompressed, aligned libraries. I will update the packaging options to be explicit.

## Verification Plan

### Automated Tests
- Run `./gradlew assembleDebug` to ensure the build still passes after dependency updates.
- Use `zipalign` to verify 16 KB alignment of the generated APK:
  ```bash
  zipalign -c -P 16 -v 4 app/build/outputs/apk/debug/app-debug.apk
  ```

### Manual Verification
- Deploy to an Android 15 emulator with 16 KB page size support (if available in your setup) to verify the app launches without the "compat mode" warning.
