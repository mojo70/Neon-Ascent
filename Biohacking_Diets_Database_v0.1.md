# Biohacking Diets Database (Initial Seed)
**Version:** 0.1 – Parallel to Supplements & Protocols DBs (Oct 2026)  
**Purpose:** Structured starting point for an app database of dietary patterns.  
**Notes:**  
- Effects are generally modest, context-dependent, and often additive with foundational behaviors (sleep, training, protein adequacy).  
- “Helpful under X” means the strongest evidence or clearest signal appears in that population or condition.  
- Condition-specific claims appear only where published human evidence is solid. Personal differential responses belong in individual user logs — not in the shared database.  
- Not medical advice. Always consult a clinician, especially with existing conditions or medications.  
- Plant-based / plant-forward / vegan patterns are excluded from the seed due to high residual confounding and quality-vs-binary issues.

## Classification Model
**Primary filter for the app:** Outcome Domain (what the user wants to move).  
**Secondary tags:** Diet Category + Mechanism (used for stacking logic and interactions).

### Outcome Domains (shared with Supplements & Protocols DBs)
- **Attention / Focus**
- **Memory**
- **Mental Energy / Fatigue Resistance**
- **Stress Resilience / Mood**
- **Sleep Architecture**
- **Foundational / Nutrient Status**
- **Physical Performance / Recovery**
- **Longevity / Healthspan**

### Diet Categories
- Time-Restricted / Fasting
- Pattern / Regional
- Macronutrient-Focused
- Food-Quality / Whole-Food
- App-Specific / Custom
- Stacked / Combined

### Mechanism Tags (examples)
- Insulin sensitivity / glycemic control
- Protein leverage / muscle protein synthesis
- Metabolic flexibility
- Caloric density control
- Circadian alignment
- Nutrient density

## Evidence-Grade Rubric
| Grade | Label | Criteria | Example use in app |
|-------|-------|----------|--------------------|
| **A** | Strong | Multiple high-quality RCTs, meta-analyses, or large prospective cohorts with consistent, clinically meaningful effects in the stated population/context. Low risk of bias overall. | Primary recommendation filter |
| **B** | Moderate | Several RCTs or one solid meta-analysis / well-controlled cohort; effects are real but modest, or limited to specific conditions. Some methodological limits. | Secondary / contextual filter |
| **C** | Limited | Few small trials, mixed results, or high risk of bias. Signal exists but not reliable. | Research / optional tag only |
| **D** | Preliminary | Mostly preclinical, mechanistic, single weak studies, or null results in the relevant population. | Hide by default or “low confidence” |

Grades are context-specific.

---

## Data Schema (for future app / structured export)
Each entry follows:
- **Name** / Aliases / Cyber name  
- **Status**: Established | Popular / Community | App-Specific  
- **Primary outcome domains** (list)  
- **Diet category** + **Mechanism tags**  
- **Best evidence contexts**  
- **Evidence grade** (per rubric, context-aware)  
- **Typical parameters** (macros, windows, food rules, frequency)  
- **Onset / timeline**  
- **Key links**  
- **Safety / notes**

---

## Entries

### Cyber Cyclical Diet (CCD)
- **Status**: App-Specific (updated 2026 version of Faigin NHE diet principles)  
- **Primary outcome domains**: Physical Performance / Recovery, Longevity / Healthspan, Mental Energy / Fatigue Resistance  
- **Diet category**: App-Specific / Custom, Macronutrient-Focused  
- **Mechanism tags**: Protein leverage / muscle protein synthesis, Metabolic flexibility, Insulin sensitivity / glycemic control  
- **Best evidence contexts**: Adults seeking sustainable body-composition improvement with resistance training; people who prefer a lower-carb baseline with periodic higher-carb meals  
- **Evidence grade**: B for higher-protein modest-carb patterns on body composition and satiety when combined with resistance training; C for precise cyclical timing superiority over consistent moderate carbohydrate intake (mechanistic support exists; outcome data mixed)  
- **Typical parameters**:  
  - Protein: 1.6–2.2 g/kg body weight daily (prioritize leucine-rich whole-food sources).  
  - Most days: modest net carbs (~50–100 g). Whole-food fats. Eat to satiety; no required calorie counting.  
  - 1–2 higher-carb meals per week (preferably post-training or evening). Align with training days when practical.  
  - Net carbs for whole foods (total carbohydrate minus fiber). Treat packaged foods more cautiously.  
  - Free / near-free vegetables (do not count toward daily net target in normal portions): leafy greens, broccoli, cauliflower, cabbage, Brussels sprouts, asparagus, zucchini, cucumber, celery, mushrooms, peppers, and similar non-starchy vegetables.  
  - Starchy vegetables, fruit, grains, legumes, and sugary foods count fully.  
- **Onset**: Acute satiety and energy shifts within days; measurable body-composition changes typically over weeks when paired with training and adequate sleep.  
- **Key links**: Higher protein meta-analyses (e.g., Morton et al. on muscle protein synthesis); Aragon & Schoenfeld 2013 on nutrient timing [PMID: 24299050](https://pubmed.ncbi.nlm.nih.gov/24299050/); general carb-cycling reviews showing total intake dominates timing.  
- **Safety / notes**: Not a ketogenic protocol. Adjust carbohydrate upward if energy, training performance, or thyroid symptoms decline. Individual glucose responses vary. Training and sleep are required co-pillars but live outside this diet entry. Mechanistic hormonal claims from the original 2000 NHE are retained only where supported; precise meal-timing superiority is not strongly outcome-supported.  

### Mediterranean Diet
- **Status**: Established  
- **Primary outcome domains**: Longevity / Healthspan, Physical Performance / Recovery  
- **Diet category**: Pattern / Regional  
- **Mechanism tags**: Nutrient density, Insulin sensitivity / glycemic control  
- **Best evidence contexts**: General adult populations; cardiovascular risk reduction  
- **Evidence grade**: A (large prospective cohorts and trials for CVD and all-cause mortality)  
- **Typical parameters**: High in vegetables, fruit, legumes, nuts, olive oil, fish; moderate dairy and poultry; limited red and processed meat; whole grains.  
- **Onset**: Risk-factor improvements in weeks to months; mortality associations from multi-year adherence.  
- **Key links**: PREDIMED trial; multiple meta-analyses of Mediterranean patterns.  
- **Safety / notes**: Strongest long-term hard-outcome signal among common patterns. Compatible with higher protein targets.  

### Time-Restricted Eating (16:8-style)
- **Status**: Established / Popular  
- **Primary outcome domains**: Physical Performance / Recovery, Longevity / Healthspan, Mental Energy / Fatigue Resistance  
- **Diet category**: Time-Restricted / Fasting  
- **Mechanism tags**: Circadian alignment, Caloric density control  
- **Best evidence contexts**: Overweight adults; adherence-focused users  
- **Evidence grade**: B for short-to-medium-term weight and metabolic markers when calorie intake declines; C for unique benefits beyond calorie reduction in controlled trials  
- **Typical parameters**: 8-hour eating window (commonly 12:00–20:00 or similar); 16-hour fast. Protein and food quality still prioritized inside the window.  
- **Onset**: Appetite and weight shifts within 1–4 weeks for many.  
- **Key links**: Multiple RCTs and meta-analyses on TRE (e.g., 2020–2025 reviews).  
- **Safety / notes**: Not superior to continuous energy restriction when calories are matched. Avoid if history of disordered eating. Hydration and electrolytes matter during the fast.  

### Ketogenic Diet (context-specific)
- **Status**: Established for specific uses  
- **Primary outcome domains**: Physical Performance / Recovery, Mental Energy / Fatigue Resistance  
- **Diet category**: Macronutrient-Focused  
- **Mechanism tags**: Metabolic flexibility, Insulin sensitivity / glycemic control  
- **Best evidence contexts**: Epilepsy (classic); short-term weight loss; some metabolic syndrome markers  
- **Evidence grade**: A for refractory epilepsy; B for short-term weight and glycemic control; C/D for superior long-term longevity or general performance versus other calorie-matched patterns  
- **Typical parameters**: Typically <50 g net carbs/day to maintain nutritional ketosis; high fat, moderate protein.  
- **Onset**: Ketosis in 2–7 days; weight loss front-loaded.  
- **Key links**: Classic epilepsy trials; meta-analyses on weight and T2D markers.  
- **Safety / notes**: Monitor electrolytes, fiber, and micronutrients. Not required for the CCD. Performance may decline initially in high-intensity training.  

### High-Protein / Protein-Leverage Focus
- **Status**: Established  
- **Primary outcome domains**: Physical Performance / Recovery, Mental Energy / Fatigue Resistance  
- **Diet category**: Macronutrient-Focused  
- **Mechanism tags**: Protein leverage / muscle protein synthesis  
- **Best evidence contexts**: Adults doing resistance training; older adults preserving lean mass  
- **Evidence grade**: A/B for muscle retention and satiety at 1.6–2.2 g/kg  
- **Typical parameters**: 1.6–2.2 g protein/kg daily, distributed across meals; remaining calories from fat and carbohydrate per preference.  
- **Onset**: Satiety within days; lean-mass effects over weeks of training.  
- **Key links**: Morton et al. meta-analysis on protein and resistance training.  
- **Safety / notes**: Safe for healthy kidneys. Forms the protein foundation of CCD.  

---

**Seed status**: CCD is the primary app-specific entry. Additional established patterns included for reference. Expand as needed with the same schema.
