# Workout Onboarding Fixes

This plan addresses two issues in the Workout onboarding:
1.  The "Perform attribute scan" button in the `StepAttributeCalibration` step currently does nothing.
2.  The `StepProtocolSynthesis` step needs a way to view and select alternate protocols if the suggested one is not desired.

## User Review Required

> [!IMPORTANT]
> The "Perform attribute scan" button will navigate the user away from the onboarding flow to the `AttributeScanScreen`. After the scan, the user will need to navigate back (pop backstack) which should ideally return them to the same step in the onboarding.

## Proposed Changes

### Workout Feature

#### [MODIFY] [OnboardingViewModel.kt](file:///Users/joeevans/StudioProjects/Neon-Ascent/feature/workout/src/main/java/com/neon/ascent/feature/workout/ui/OnboardingViewModel.kt)
- Add `showAlternateProtocols` and `availableRoutines` to `OnboardingUiState`.
- Load all available routines from `WorkoutRepository` during initialization or when needed.
- Add methods to show/hide the alternate protocols dialog.
- Add `selectProtocol(routine: WorkoutRoutine)` to allow manual override of the recommended protocol.

#### [MODIFY] [OnboardingScreen.kt](file:///Users/joeevans/StudioProjects/Neon-Ascent/feature/workout/src/main/java/com/neon/ascent/feature/workout/ui/OnboardingScreen.kt)
- Add `onPerformScan: () -> Unit` parameter to `OnboardingScreen` and pass it down to `NoScanPanel`.
- Implement `AlternateProtocolsDialog` to display available protocols and allow selection.
- Add "VIEW ALTERNATE PROTOCOLS" button in `StepProtocolSynthesis`.

### App Module

#### [MODIFY] [AppNavigation.kt](file:///Users/joeevans/StudioProjects/Neon-Ascent/app/src/main/java/com/neon/ascent/AppNavigation.kt)
- Update `OnboardingScreen` call to pass a lambda that navigates to `Screen.AttributeScan`.

## Verification Plan

### Manual Verification
- Deploy the app to a device/emulator.
- Navigate to the Workout Onboarding.
- In Step 3 (Attribute Calibration), click "PERFORM ATTRIBUTE SCAN" and verify it navigates to the scan screen.
- In Step 5 (Protocol Synthesis), click "VIEW ALTERNATE PROTOCOLS", verify the dialog appears, and selecting a protocol updates the recommended one.
- Verify "do my own thing" (General protocol) is an option.
