# Swap Exercise and Implement Visual Hierarchy

Swap the size, highlighting, and order for the exercise (Movement Name) and the implement (Variant Details) in the `WorkoutLogCard`. The Movement Name will become the primary visual anchor (Large, Neon), and the Implement/Stance details will be secondary (Small, Gray).

## Proposed Changes

### feature:workout

#### [MODIFY] [WorkoutLoggingScreen.kt](file:///Users/joeevans/StudioProjects/Neon-Ascent/feature/workout/src/main/java/com/neon/ascent/feature/workout/ui/WorkoutLoggingScreen.kt)

- Refactor `WorkoutLogCard` layout:
    - Swap the vertical order of `familyName` and `variantTitle`.
    - Apply `neonColor` and larger font size (16-18sp) to the `familyName`.
    - Apply `Color.Gray` and smaller font size (10sp) to the `variantTitle`.
    - Update `variantTitle` logic to return an empty string if the exercise is a primary variant with standard stance/implement (to hide the "STANDARD" label).

## Verification Plan

### Automated Tests
- Run `androidTest` to ensure no UI regressions in the workout feature.

### Manual Verification
- Deploy to device/emulator.
- Start a workout (e.g., Pull-Up).
- Verify "PULL-UP" is large and neon green on top.
- Verify "BODYWEIGHT" is small and gray below it.
- Verify that for "Bench Press (Barbell)" (primary variant), the gray sub-label is hidden.
