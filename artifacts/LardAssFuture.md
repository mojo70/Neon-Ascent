# Neon Ascent – LardAss Future (ML Prediction & Coaching)

**Status**: Design lock (future scope)  
**Updated**: November 9, 2026  
**Repo**: `mojo70/Neon-Ascent`  
**Depends on**: `LardAss.md`, `SelfMap.md`, `SelfMapIntake.md`  
**Studio pack**: None yet (future workstream)

Future LardAss workstream. Not Slice 1. Not current intake.

---

## Product call

Once the Self Map is complete enough (attachment structure with importance), a later LardAss slice can use it for:

1. **Situational reflection**  
   Given a user-logged situation or hypothetical, surface “this looks like a defense of a high-importance node” as a reflection aid. No invented emotion score. User confirms or dismisses.

2. **1:1 coaching through tough situations**  
   Deterministic shell + narrow model calls. Guide offers a short, grounded coaching beat (what attachment is in play, what expectation is being violated, one optional reframe or next action). User stays in control. No unsolicited advice. No Faith Lens injection.

3. **ML prediction layer (later)**  
   After sufficient labeled data (user-confirmed reactions + context + map state), train a local or privacy-preserving model to estimate reaction strength or likely defensive pattern. Prediction is a signal, never a write. Always shown with confidence and the source nodes. Auto-demote if eval MAE is worse than the simple importance heuristic.

Full “here’s exactly how you would act” prediction is out of scope. Action depends on more than the map.

---

## Locked constraints (inherit from LardAss)

- Deterministic shell owns the flow. Model is a narrow tool.
- Hard prompt isolation. No CYBR-TES inheritance.
- User confirms every write and every coaching suggestion that would change state.
- No invented emotion scores or black-box classification in the product UI.
- Local-first preferred. Cloud only if explicitly opted in and for non-core prediction.
- Eval gate required before any prediction surface ships.
- No fifth wing. Lives on Avatar / LABS / Codex as appropriate.
- Backup + P2P completeness for any new persisted fields.

---

## Out of scope for this lock

- Implementation details of the model architecture.
- Training data collection pipeline (separate lock when ready).
- Replacing the current Self Map intake.
- Automatic behavior change or unsolicited interventions.

---

## Next steps when this workstream starts

1. Define the minimal situation log schema (what was threatened, which node, user-reported reaction).
2. Build the deterministic “high-importance node threatened” reflection first.
3. Add coaching templates + optional narrow model refinement.
4. Only after labeled data exists, evaluate a prediction model against the simple heuristic.
