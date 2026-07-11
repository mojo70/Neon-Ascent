# Neon Ascent - Architecture Blueprint

## Core Modules & Layout

Neon Ascent is structured as a multi-module Android application to ensure scalability, testability, and a clear separation of concerns. The project follows a strict **Clean Architecture** pattern.

### 1. Core Modules (`:core`)
These modules provide the foundation for the entire application.

*   **`:core:domain`**: The heart of the app. Contains:
    *   **Domain Models**: Pure data classes like `UserCharacter`, `Exercise`, and `WorkoutSession`.
    *   **Repository Interfaces**: Define the contracts for data access (e.g., `CharacterRepository`, `WorkoutRepository`).
    *   **Use Cases**: Encapsulate specific business logic (e.g., `CheckRecoveryMissionsUseCase`).
*   **`:core:data`**: Implements the repository interfaces defined in the domain layer. Handles data persistence using Room (Local) and potentially network requests (Remote).
    *   **Mappers**: Responsible for converting database Entities (e.g., `com.neon.ascent.model.UserCharacter`) to Domain Models (e.g., `com.neon.ascent.core.domain.character.models.UserCharacter`).
*   **`:core:common`**: Shared utilities, constants, and global coordinators (e.g., `DopamineCoordinator`).
*   **`:core:ai-litert`**: Manages on-device AI operations and personas (e.g., `AiPersona`).

### 2. Feature Modules (`:feature`)
Encapsulate specific user-facing functionality, following the **MVI/MVVM** pattern with Jetpack Compose.
*   **Pattern**: UI components interact *only* with Domain Models and Repository Interfaces. Legacy models from the `:app` data layer are strictly mapped to domain versions before reaching the ViewModel.
*   **Examples**: `:feature:workout`, `:feature:health`, `:feature:goals`, `:feature:terminal`.

### 3. App Module (`:app`)
The "Glue" of the application.
*   **Dependency Injection**: The Hilt `SingletonComponent` root. Bridges domain interfaces to implementations (e.g., binding `CharacterRepository` implementation to its domain interface).
*   **Navigation**: Orchestrates flows between feature modules.
*   **Legacy Data**: Houses the main Room database and legacy models that are gradually being migrated to `:core:data`.

---

## Data Integration & Neural Systems

### 1. The Unified UserCharacter Model
The `UserCharacter` domain model acts as the central source of truth for the operative's identity and state, spanning multiple subsystems:
*   **Biometrics**: Height, Weight, Somatotype, Neural Load.
*   **Attributes (S.P.E.C.I.A.L.)**: Strength, Perception, Endurance, Charisma, Intelligence, Agility, Luck.
*   **Cyberdeck & Equipment**: RAM slots, Quickhack slots, Equipped Cyberware lists.
*   **Spiritual Layer**: Prayer streaks, Baptism status flags.
*   **Economy**: Eddies and Secure Solana Wallet status.

### 2. Neural Initialization Flow
The workout system leverages the `UserCharacter` model to personalize protocols:
*   **Somatotype**: Adjusts volume (e.g., Ectomorph +10% volume).
*   **Hardware Check**: Cross-references physical constraints for auto-substitution.

### 3. Insight & Memory Palace
*   **Memory Palace**: Stores diverse data (Health, Pulses, Dialogue) into categorized "Wings" and "Rooms".
*   **Socratic Insights**: AI-generated analysis derived from the intersection of biometric trends and Memory Palace fragments.

---

## Technical Standards & Persistence Integrity

### 1. Data Write Safety (Atomic Updates)
To prevent data loss during sequential input flows (like the "Identity Forge"), the following rules apply:
*   **Read-Before-Write**: ViewModels must fetch the current character state from the repository immediately before applying a `copy()` and saving. This prevents "stale data" from overwriting fields updated in previous steps (e.g., Name overwriting Personality).
*   **Mutex Protection**: Critical repositories (especially `CharacterRepository`) must utilize a `Mutex` lock for write operations to ensure atomicity and prevent race conditions across background threads.
*   **Full Field Mapping**: Mappers must account for *every* field in the domain model. Partial updates at the repository layer are forbidden to ensure the database remains the true reflection of the domain state.

### 2. Navigation Integrity
*   **State-Aware Navigation**: Apps must wait for a "Success" state from the ViewModel before navigating away from data-entry screens.
*   **Startup Verification**: The `isCreationComplete` flag must be checked robustly at startup (e.g., during the Loading Screen) to ensure the user is directed to the correct entry point based on persisted data.

### 3. UI & State Management
*   **State Management**: `StateFlow` and `collectAsStateWithLifecycle`. UI logic must handle properties from different modules using local capture to satisfy Kotlin's smart-cast requirements.
*   **Dependency Injection**: Hilt. All repository implementations must be bound to domain interfaces in Hilt modules to allow feature modules to remain decoupled from the data implementation.

### 4. Infrastructure
*   **Persistence**: Secure Room database utilizing SQLCipher for encrypted storage.
*   **AI**: Hybrid approach using Gemini Nano (Local) and Gemini Flash (Cloud), orchestrated by `AiProvider`.
