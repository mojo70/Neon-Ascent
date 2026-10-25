# Neon Ascent – PILOT Mode (Roadster / Night HUD Theme)

**Status**: Design lock  
**Updated**: October 25, 2026  
**Repo**: `mojo70/Neon-Ascent`  
**Studio pack**: `PilotGeminiPrompts.md` (PROMPT 0–6 + R)

Third visual stimuli mode. Inspired by the Tesla Roadster fighter-jet cockpit interface (black canvas, red accents, pilot-centric, night-friendly). It is not a neon-green skin and it is not a light theme. It is a third HUD skin: **dim red-on-void cockpit**.

CYBER remains the default night HUD. STEVE remains the high-contrast daylight skin. PILOT is the night-vision-preserving alternative.

---

## Product lock

1. **Three visual modes.** `CYBER` (default) | `STEVE` | `PILOT`. Do not add Matrix Emerald / megacorp skins in this workstream. The existing `COLOR SCHEME: MATRIX_EMERALD` row stays a stub.
2. **Home is Settings → VISUAL_STIMULI.** Segmented control: `CYBER` | `STEVE` | `PILOT`. Same `CyberTabButton` pattern. Three options fit the existing row.
3. **Whole app flips on toggle.** DECK, OPS, LABS, CODEX, Settings, dialogs, bottom bar, system bars. No leftover neon-green or pure-white screens.
4. **Persist immediately.** Existing DataStore key `theme_mode` in `UserPreferencesRepository.PreferencesKeys.THEME_MODE`. Accept `"CYBER"` | `"STEVE"` | `"PILOT"`. Unknown values coerce to `"CYBER"`. Default `"CYBER"`. Survive process death.
5. **DECK chrome structure stays frozen.** `SlimChromeHeader`, STEPS/KCAL/STREAK, terminal, LOG SESSION / refresh / + do not change layout. Only tokens and overlay intensity change.
6. **PILOT is red-on-black by contract.** Pure black / near-black canvas. Light gray ink for body. Medium red accent only. No gold, no neon green, no cyberpink, no heavy glow. Flavor comes from geometry, type, hairlines, and the red accent — not bloom.
7. **Do not restyle copy.** Labels stay `//SYS_SETTINGS`, `NEURAL BRIEF`, `OPS_LOG`. PILOT is a skin, not a rewrite of voice.
8. **`neonIntensity` is reused.** In CYBER it is glow. In STEVE it is ink weight / hairline strength. In PILOT it is red hairline / subtle edge intensity (kept low). Same slider, different mapping.
9. **Accessibility beats flavor when they fight.** Body text ≥ 7:1 on canvas. Large numbers ≥ 4.5:1. Red is accent-only — never body text. No color-only selected state. No bloom, no heavy scanline, no matrix rain that drops contrast. Night-friendly by design (dim red accents, low overlay).

---

## Why this stays cyber

Cyber Mode is neon-green-on-void. Steve Mode is ink-on-paper. PILOT is **dim red-on-void cockpit**.

Keep:

- Monospace + tracked labels
- Cut-corner shapes (`CyberCutShape`, `CyberButtonShape`)
- `HudCornerAccents` ticks (now red, very subtle)
- Hairline frames
- Terminal feed layout
- Selected = filled or strongly outlined chip, unselected = outlined chip

Drop in PILOT:

- Green matrix rain
- Pink danger glow
- Heavy neon bloom / `neonBorder` bloom
- Gold or metallic accents
- White canvas

Read as: the same deck, cockpit lighting on, night vision preserved.

---

## Palette (locked)

Do not invent extra swatches in screens. Put these on `NeonThemeData` and read them.

### PILOT

| Token | Hex | Use |
|---|---|---|
| `canvas` | `#050505` | Page. Same void as CYBER. |
| `surface` | `#0F0F0F` | Cards |
| `surfaceRaised` | `#1A1A1A` | Inputs, unselected chips |
| `ink` | `#E0E0E0` | Body, icons, large numbers |
| `inkMuted` | `#9A9A9A` | Secondary labels |
| `inkFaint` | `#6E6E6E` | Captions, inactive tab |
| `accent` | `#D32F2F` | Selected, borders, key labels, indicators. Medium red. |
| `accentDanger` | `#B71C1C` | Destructive. Deeper red. Still high-contrast when filled. |
| `hairline` | `#D32F2F` | Borders. Thin (1 dp or less). Subtle. |
| `grid` | `#1A1A1A` | SoftGrid. Very faint; optional red tint at 8% or less. |
| `overlay` | `#D32F2F` @ 3% | Scanlines only. Never > 4%. |

**Text answer, locked:** Body text is light gray `#E0E0E0` on `#050505` (≈ 15:1). Red is never used for body or large numbers. Secondary is `#9A9A9A`. Captions only may use `#6E6E6E`. Selected chip is solid `#D32F2F` with `#E0E0E0` (or white) label.

Contrast checks (approx):

- `#E0E0E0` on `#050505` ≈ 15:1
- `#9A9A9A` on `#050505` ≈ 7.5:1
- White / `#E0E0E0` on `#D32F2F` selected chip ≈ 4.6–5.5:1 (acceptable for large/selected labels; do not use for small body)

Megacorp `accentFor(id)` in PILOT Mode **collapses to accent** (`#D32F2F`). Do not paint Helix green.

---

## Settings UI

`SettingsScreen.kt` section `VISUAL_STIMULI` becomes:

```
VISUAL_STIMULI
  MODE
  [ CYBER ] [ STEVE ] [ PILOT ]     // CyberTabButton row

  NEON INTENSITY          // keep slider
  // subtitle, 10sp muted:
  // CYBER: glow gain
  // STEVE: ink weight
  // PILOT: red edge / hairline

  CUSTOM AVATAR MODULE    // stub stays
  COLOR SCHEME: …         // stub stays
```

Copy under the tabs, one line:

- CYBER: `NIGHT HUD // NEON ON VOID`
- STEVE: `DAY HUD // INK ON PAPER`
- PILOT: `NIGHT HUD // RED ON VOID`

Haptic on toggle (already the Settings pattern). No confirm dialog. Flip is instant.

Settings page itself must theme. In PILOT: black canvas, red hairlines, no green matrix rain (or very faint red ticks @ 3%), scanlines 3%, section borders `hairline`.

---

## Effect mapping

| Effect | CYBER | STEVE | PILOT |
|---|---|---|---|
| `MatrixRainBackground` | green glyphs @ 8% | off, or gray ticks @ 4% | off, or red ticks @ 3% |
| Scanlines | black @ 10%, step 6 | ink @ 4%, step 8 | red @ 3%, step 8 |
| `SoftGridBackground` | `#1A1A1A` on void | `#D4D4D4` on white | `#1A1A1A` on void (optional faint red) |
| `neonBorder` / glow | on, scaled by intensity | **off**. Hairline stroke only | **minimal**. Thin red hairline only. No bloom. |
| `NightCityGlow` / `AcidRainOverlay` | allowed | skip / no-op | skip / no-op |
| `GlitchText` | chromatic split ok | same text, no color split | same text, no color split |
| `HeartRatePulse` | neon pulse | ink pulse, no bloom | red pulse, no bloom |
| Selected chip | green outline or fill | solid `#1C1C1C` + white label | solid `#D32F2F` + light label |
| Unselected chip | dark fill + dim green | `#FFFFFF` / `#E2E2E2` + hairline | dark fill + thin red hairline |
| Bottom bar | `#050505` + green active | `#FFFFFF` + ink active | `#050505` + red active, muted inactive |
| System bars | light icons | **dark icons** | light icons |
| Charts / heatmap | neon series | ink + two gray steps | red series + gray steps. No green series. |

---

## Architecture

```
UserPreferencesRepository.themeMode: Flow<String>   // "CYBER" | "STEVE" | "PILOT"
        |
SettingsViewModel.visualMode
        |
MainActivity.setContent
        |
NeonAscentTheme(mode)  --> Material colorScheme
                       --> CompositionLocalProvider(LocalNeonTheme, LocalVisualMode)
        |
Screens / CyberComponents read LocalNeonTheme.current
        |  never Color(0xFF00FF9C) for chrome
```

### New / extended types

```kotlin
enum class VisualMode { CYBER, STEVE, PILOT }

@Immutable
data class NeonThemeData(
    val mode: VisualMode,
    // ... existing tokens ...
)
```

Factory: `NeonThemeData.pilot()` with the locked hex values above.

`NeonAscentTheme` takes `visualMode` and installs:

- CYBER → existing dark NeonScheme
- STEVE → light-structured scheme (unchanged)
- PILOT → dark scheme with red primary / outline / accent. background/surface = canvas/surface. onBackground/onSurface = ink. primary = accent. onPrimary = light. outline = hairline. error = accentDanger. No green.

### Persistence

`UserPreferencesRepository`:

- `val themeMode: Flow<String>` default `"CYBER"`
- `suspend fun setThemeMode(mode: String)`
- Accept only `CYBER` / `STEVE` / `PILOT`. Anything else → `CYBER`.

Do not store a boolean. String enum matches existing pattern.

### Who reads the flow

`MainActivity` (or tiny `ThemeViewModel`) collects `themeMode` **above** `NeonAscentTheme` so the whole tree recomposes. Settings toggle writes DataStore; theme flips without navigation.

System bars: `SideEffect` + `WindowCompat.getInsetsController`. Light icons for CYBER and PILOT; dark icons for STEVE.

---

## Hardcoded-color problem (the real work)

Same as Steve Mode. Screens still paint literals. Migrate chrome to tokens. Priority order:

1. SettingsScreen (so the toggle itself themes)
2. CyberComponents (frames, buttons, terminal, grid)
3. NeonBottomBar
4. Dashboard / SlimChromeHeader
5. Workout / Codex / Labs cards (page + headers + chips first; do not rewrite large files)

Do not restyle layout. Do not invent new components. Read tokens.

---

## Out of scope

- No gold accents
- No heavy glow or bloom
- No fifth visual mode
- No layout changes
- No copy changes
- No conditional mode switching based on time of day or activity (pure manual toggle)
- Prompt pack stays local. Lock (this file) can be pushed.
