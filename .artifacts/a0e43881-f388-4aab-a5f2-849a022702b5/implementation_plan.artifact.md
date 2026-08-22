# Cyber-Style Bottom Navigation Integration

Implement a persistent, stylized bottom navigation bar for the main hub, inspired by the requested "Gutter Signal" aesthetic.

## Proposed Changes

### [Component: UI Components]

#### [NEW] [NeonBottomBar.kt](file:///Users/joeevans/StudioProjects/Neon-Ascent/app/src/main/java/com/neon/ascent/ui/components/NeonBottomBar.kt)
Create a new composable that implements the visual style from the screenshot:
- Monospace labels with wide letter spacing.
- Cyber-minimalist icons.
- Persistent top border line.
- Custom colors (`onBackground` for active, muted gray for inactive).

### [Component: Navigation]

#### [MODIFY] [AppNavigation.kt](file:///Users/joeevans/StudioProjects/Neon-Ascent/app/src/main/java/com/neon/ascent/AppNavigation.kt)
- Update the `MainHub` composable to use a `Scaffold`.
- Integrate `NeonBottomBar` into the `Scaffold.bottomBar`.
- Expand the `HorizontalPager` to 6 pages:
    - Index 0: **BOARD** (Dashboard)
    - Index 1: **ALTAR** (Lore)
    - Index 2: **ICE** (Cyberdeck)
    - Index 3: **LABS** (Biohacking)
    - Index 4: **RIG** (Workout/Performance)
    - Index 5: **OPS** (Ascension/Terminal)
- Sync `PagerState` with the selected item in the bottom bar.
- Update existing `animateScrollToPage` calls to match new indices.

## Verification Plan

### Automated Tests
- Build the project to ensure no compilation errors in the navigation logic.

### Manual Verification
- Deploy to device/emulator.
- Verify that tapping bottom bar items smooth-scrolls the pager.
- Verify that swiping the pager updates the active item in the bottom bar.
- Confirm the styling matches the target aesthetic (font, spacing, colors).
