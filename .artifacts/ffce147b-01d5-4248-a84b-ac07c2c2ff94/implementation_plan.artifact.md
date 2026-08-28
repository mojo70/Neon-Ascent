# Fix overlapping UI elements in Dashboard Header

The time, name, and temperature are overlapping on the Dashboard screen because the `SlimChromeHeader` uses a `Box` layout which allows its children (the left-aligned identity section and the right-aligned status section) to occupy the same space if the name is long.

## Proposed Changes

### Dashboard Feature

#### [MODIFY] [DashboardScreen.kt](file:///Users/joeevans/StudioProjects/Neon-Ascent/app/src/main/java/com/neon/ascent/feature/dashboard/DashboardScreen.kt)

- Replace the root `Box` in `SlimChromeHeader` with a `Row`.
- Use `Modifier.weight(1f)` on the left section to ensure it respects the space of the right section.
- Add `maxLines = 1` and `overflow = TextOverflow.Ellipsis` to the runner name text to handle long names gracefully.
- Add a `Spacer` between the sections to ensure a minimum gap.

## Verification Plan

### Manual Verification
- Deploy the app and verify the Dashboard header.
- Test with a long character name to ensure it ellipses instead of overlapping with the clock/weather.
- Verify that the clock and weather are still correctly aligned to the top-right.
