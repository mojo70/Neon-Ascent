# Blast blocks, Injury Guard, and Nutrition Uplink

This update integrates intelligent physiological tracking and safety features, completing the "High Fidelity" CyberCrapp experience and enhancing adaptive custom workouts.

## 🚀 Key Features

### Blast Block Tracker
The app now tracks your high-intensity training blocks (Blasts) automatically.
- **Dynamic Calculation**: "BLAST WEEK X" appears on your dashboard, calculated from your first full-intensity session after a deload.
- **Auto-Reset**: Reset occurs automatically upon completion of a "Soft Deload" session, allowing your connective tissues to recover.

### 🛡️ Injury Guard & Stable Library
The app now proactively protects your joints by scanning your routines for contraindications.
- **Neural Scanning**: When starting a routine, the app cross-references your listed injuries (e.g., "Shoulder Pain") with the exercise requirements.
- **Stable Alternatives**: Added 12+ new "Stability-First" exercises (Hammer Strength, Smith Machine, etc.) to serve as safe replacements.
- **Neural Swap**: One tap "Auto-Swap" instantly replaces high-risk movements with safe, high-stability alternatives from the library.

### 🍱 Biometric Nutrition Uplink
Nutrition targets are now visible in the Biohacking hub, adjusted for your training and somatotype.
- **MacroCalculator**: Implemented the Mifflin-St Jeor equation with somatotype modifiers.
- **Adaptive Targets**: Protein, Carbs, and Fats are calculated based on your protocol intensity and goal.
- **Visual Breakdown**: High-contrast chart showing your daily calorie and macronutrient split.

## 🧠 Technical Implementation
- **Database v41**: Schema updated to persist Blast dates and exercise risk tags.
- **Macro Logic**: Centralized logic for TDEE and nutritional distribution in `core:domain`.
- **UI HUD**: Integrated Blast Week into the active workout header for real-time motivation.

---
> [!TIP]
> Make sure your **Injuries** are updated in your Profile to enable the full functionality of the **Injury Guard** neural link!
