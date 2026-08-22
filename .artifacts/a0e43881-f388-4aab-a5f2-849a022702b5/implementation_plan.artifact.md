# Dashboard OS-Style Redesign

Implement a high-density, technical "OS interface" redesign of the Dashboard as per the provided mock.

## User Review Required

> [!NOTE]
> The top "Chrome" area includes pager dots (DECK · HOME · BIO · GUIDE). These are currently visual indicators. "HOME" will be highlighted when on this screen.

## Proposed Changes

### [Component: UI Theme]

#### [MODIFY] [Theme.kt](file:///Users/joeevans/StudioProjects/Neon-Ascent/app/src/main/java/com/neon/ascent/ui/theme/Theme.kt)
Add specialized colors if needed, but primary focus will be on custom drawing.

### [Component: Dashboard Feature]

#### [MODIFY] [DashboardScreen.kt](file:///Users/joeevans/StudioProjects/Neon-Ascent/app/src/main/java/com/neon/ascent/feature/dashboard/DashboardScreen.kt)
- **Visuals**: Implement `SoftGridBackground` and `VignetteOverlay`. Remove heavy particles/glitch.
- **Chrome Header**:
    - Small 48dp avatar with corner brackets.
    - Identity text: "RUNNER // [NAME] · RANK_0[LEVEL]".
    - Navigation dots row.
    - Status block: Time, Weather, Heart Rate.
- **Neural Brief**:
    - `NeuralBriefCard` with circular gauge (Neural Load).
    - Primary action button: "COMPLETE PULSE // [TASK NAME]".
- **Hero Pulse**:
    - `HeroPulseRow` showing only the top priority task.
- **Metric Pills**:
    - Single thin row for STEPS, KCAL, STREAK.
- **Mission Chips**:
    - Horizontal scroll/flow of active missions as small chips.
- **AI Terminal**:
    - Collapsed technical terminal bar at the bottom.

#### [MODIFY] [AppNavigation.kt](file:///Users/joeevans/StudioProjects/Neon-Ascent/app/src/main/java/com/neon/ascent/AppNavigation.kt)
- Update `DashboardScreen` call if needed (e.g., passing level or name directly if not using character object).

## Verification Plan

### Automated Tests
- Build verification.

### Manual Verification
- Confirm UI density and layout against the provided screenshot.
- Verify "Primary Action" button correctly triggers task completion.
- Verify Mission chips correctly display active mission titles.
