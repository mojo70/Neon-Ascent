# Biohacking Protocols Database (Initial Seed)
**Version:** 0.1 – Parallel to Supplements DB (Oct 2026)  
**Purpose:** Structured starting point for an app database of non-diet biohacking protocols.  
**Notes:**  
- Effects are generally modest, context-dependent, and often additive with foundational behaviors (sleep, training, nutrition).  
- “Helpful under X” means the strongest evidence or clearest signal appears in that population or condition.  
- Research-chemical / investigational modalities are clearly marked.  
- Condition-specific claims appear only where published human evidence is solid. Personal differential responses belong in individual user logs — not in the shared database.  
- Not medical advice. Always consult a clinician, especially with existing conditions or medications.

## Classification Model
**Primary filter for the app:** Outcome Domain (what the user wants to move).  
**Secondary tags:** Protocol Category + Mechanism (used for stacking logic and interactions).

### Outcome Domains (shared with Supplements DB)
- **Attention / Focus**
- **Memory**
- **Mental Energy / Fatigue Resistance**
- **Stress Resilience / Mood**
- **Sleep Architecture**
- **Foundational / Nutrient Status** (light / circadian relevant)
- **Physical Performance / Recovery**
- **Longevity / Healthspan**

### Protocol Categories
- Thermal / Hormetic Stress
- Photobiomodulation & Circadian Light
- Breathwork & Autonomic Regulation
- Sleep Optimization
- Recovery Modalities
- Structured Training
- Neuro / Cognitive
- Stacked / Combined

### Mechanism Tags (examples)
- Hormesis / Heat shock proteins
- Circadian phase advance
- Parasympathetic activation / HRV
- Mitochondrial / VO2max
- Muscle protein synthesis / mechanical loading
- Photobiomodulation (cytochrome c oxidase)
- Cognitive behavioral restructuring

## Evidence-Grade Rubric
| Grade | Label | Criteria | Example use in app |
|-------|-------|----------|--------------------|
| **A** | Strong | Multiple high-quality RCTs, meta-analyses, or large prospective cohorts with consistent, clinically meaningful effects in the stated population/context. Low risk of bias overall. | Primary recommendation filter |
| **B** | Moderate | Several RCTs or one solid meta-analysis / well-controlled cohort; effects are real but modest, or limited to specific conditions. Some methodological limits. | Secondary / contextual filter |
| **C** | Limited | Few small trials, mixed results, or high risk of bias. Signal exists but not reliable. | Research / optional tag only |
| **D** | Preliminary | Mostly preclinical, mechanistic, single weak studies, or null results in the relevant population. | Hide by default or “low confidence” |

Grades are context-specific (e.g., Finnish sauna frequency = A for CVD mortality signal in observational data; cold-water immersion = B for acute DOMS reduction, lower for chronic adaptation).

---

## Data Schema (for future app / structured export)
Each entry follows:
- **Name** / Aliases  
- **Status**: Established | Research / Investigational  
- **Primary outcome domains** (list)  
- **Protocol category** + **Mechanism tags**  
- **Best evidence contexts**  
- **Evidence grade** (per rubric, context-aware)  
- **Typical parameters** (duration, frequency, intensity, temperature, etc.)  
- **Onset / timeline**  
- **Key PubMed links**  
- **Safety / notes** / equipment

---

## Entries

### Finnish-Style Sauna Bathing
- **Status**: Established  
- **Primary outcome domains**: Longevity / Healthspan, Physical Performance / Recovery, Stress Resilience / Mood  
- **Protocol category**: Thermal / Hormetic Stress  
- **Mechanism tags**: Hormesis / Heat shock proteins, Cardiovascular adaptation  
- **Best evidence contexts**: Middle-aged adults (large Finnish cohorts)  
- **Evidence grade**: A (dose-response CVD and all-cause mortality in prospective cohorts); B for mechanistic HSP induction  
- **Typical parameters**: 15–20+ min at 80–100 °C (dry), 4–7 sessions/week for strongest signal; 2–3×/week still beneficial  
- **Onset**: Acute cardiovascular effects immediate; mortality associations from chronic multi-year use  
- **Key links**: Laukkanen et al. 2015 JAMA Intern Med [PMID: 25705890](https://pubmed.ncbi.nlm.nih.gov/25705890/); Laukkanen et al. 2018 BMC Med [PMID: 30486813](https://pubmed.ncbi.nlm.nih.gov/30486813/)  
- **Safety**: Hydrate; avoid if unstable angina, recent MI, or severe hypotension. Alcohol increases risk.  
- **Notes**: Strongest population-level longevity signal among thermal protocols. Dose-response with no clear threshold in the data.

### Cold-Water Immersion (Post-Exercise)
- **Status**: Established  
- **Primary outcome domains**: Physical Performance / Recovery  
- **Protocol category**: Recovery Modalities  
- **Mechanism tags**: Local cooling, reduced inflammation / DOMS  
- **Best evidence contexts**: Physically active adults after strenuous or eccentric exercise  
- **Evidence grade**: B (superior to passive recovery for DOMS and some performance markers; may blunt chronic hypertrophy/strength gains if used routinely)  
- **Typical parameters**: 10–15 min at 5–15 °C (whole or partial body)  
- **Onset**: Benefits for soreness and CK primarily at 24 h  
- **Key links**: Moore et al. 2022 Sports Med meta [PMID: 36527593](https://pubmed.ncbi.nlm.nih.gov/36527593/); network meta-analyses 2024–2025  
- **Safety**: Cold shock risk; avoid with certain cardiac conditions.  
- **Notes**: Periodize rather than use after every strength session if hypertrophy is the goal.

### Morning Outdoor Light Exposure
- **Status**: Established  
- **Primary outcome domains**: Sleep Architecture, Mental Energy / Fatigue Resistance, Stress Resilience / Mood  
- **Protocol category**: Photobiomodulation & Circadian Light  
- **Mechanism tags**: Circadian phase advance (melanopsin → SCN)  
- **Best evidence contexts**: Healthy adults with delayed sleep phase or indoor lifestyles; seasonal affective patterns  
- **Evidence grade**: A for circadian phase-shifting mechanism and sleep timing; B for mood and daytime alertness in real-world use  
- **Typical parameters**: 5–30 min outdoors within 30–60 min of waking (cloudy days require longer). Avoid sunglasses if safe.  
- **Onset**: Phase advance measurable within days; sleep benefits accumulate over 1–2 weeks  
- **Key links**: Wright et al. 2013 Curr Biol (camping) [PMID: 23910656](https://pubmed.ncbi.nlm.nih.gov/23910656/); Zeitzer et al. illuminance-response curve  
- **Safety**: Eye protection in extreme brightness; caution in bipolar disorder with bright light.  
- **Notes**: Highest-leverage free circadian protocol. Outdoor lux >> indoor.

### Zone 2 Aerobic Training
- **Status**: Established  
- **Primary outcome domains**: Longevity / Healthspan, Mental Energy / Fatigue Resistance, Physical Performance / Recovery  
- **Protocol category**: Structured Training  
- **Mechanism tags**: Mitochondrial biogenesis, fat oxidation, VO2max support  
- **Best evidence contexts**: General population for mortality; endurance athletes for polarized training base  
- **Evidence grade**: A for aerobic fitness and mortality dose-response (large cohorts); B for specific “Zone 2” superiority over matched volume of higher intensity  
- **Typical parameters**: Conversational pace (lactate <2 mmol/L or talk test), 150–300+ min/week total moderate aerobic; 2–4 sessions of 30–60 min  
- **Onset**: Fitness gains in weeks; mortality associations from long-term habits  
- **Key links**: Mandsager et al. 2018 JAMA Netw Open (VO2max) [PMID: 30326054](https://pubmed.ncbi.nlm.nih.gov/30326054/); WHO/CDC physical activity guidelines  
- **Safety**: Start low if sedentary; medical clearance if CVD risk.  
- **Notes**: Base of polarized model (≈80% easy). High-intensity intervals raise VO2max faster per minute but Zone 2 builds sustainable volume.

### Resistance Training (Longevity Dose)
- **Status**: Established  
- **Primary outcome domains**: Longevity / Healthspan, Physical Performance / Recovery  
- **Protocol category**: Structured Training  
- **Mechanism tags**: Muscle protein synthesis, mechanical loading, sarcopenia prevention  
- **Best evidence contexts**: Adults ≥18; strongest mortality signal in middle-aged and older  
- **Evidence grade**: A (any resistance training lowers all-cause mortality 12–17%; J- or L-shaped dose-response)  
- **Typical parameters**: 30–120 min/week total; 2–3 sessions targeting major muscle groups. Lowest risk around 60–120 min/week in recent analyses.  
- **Onset**: Strength/muscle in weeks; mortality associations from multi-year adherence  
- **Key links**: Momma et al. 2022 BJSM meta [PMID: 35131965](https://pubmed.ncbi.nlm.nih.gov/35131965/); 2026 BJSM long-term cohort  
- **Safety**: Technique first; progressive overload.  
- **Notes**: Combined with aerobic yields lower risk than either alone. No clear benefit above ~120 min/week for mortality in some analyses.

### Cognitive Behavioral Therapy for Insomnia (CBT-I)
- **Status**: Established  
- **Primary outcome domains**: Sleep Architecture  
- **Protocol category**: Sleep Optimization  
- **Mechanism tags**: Cognitive behavioral restructuring, sleep restriction, stimulus control  
- **Best evidence contexts**: Adults with chronic insomnia (with or without comorbidity)  
- **Evidence grade**: A (first-line, large effects, sustained)  
- **Typical parameters**: Multi-component (sleep restriction + stimulus control + cognitive restructuring); 4–8 sessions or digital equivalent  
- **Onset**: Significant improvement within weeks; effects durable at 6–12 months  
- **Key links**: Multiple metas including Trauer et al., Edinger et al. AASM guidelines [PMID: 26054060](https://pubmed.ncbi.nlm.nih.gov/26054060/)  
- **Safety**: Temporary sleep restriction can increase daytime sleepiness; professional guidance preferred.  
- **Notes**: Strongest non-pharmacologic sleep intervention. Digital versions effective but in-person often superior.

### Slow-Paced Breathing (~6 breaths/min)
- **Status**: Established  
- **Primary outcome domains**: Stress Resilience / Mood, Physical Performance / Recovery  
- **Protocol category**: Breathwork & Autonomic Regulation  
- **Mechanism tags**: Parasympathetic activation, respiratory sinus arrhythmia, baroreflex  
- **Best evidence contexts**: Healthy adults and prehypertensive/hypertensive populations  
- **Evidence grade**: B (acute and short-term HRV increase, modest BP reduction)  
- **Typical parameters**: 5–10 min at ~6 cycles/min (inhale:exhale often 1:1 or longer exhale), daily or multiple times  
- **Onset**: Immediate HRV and HR effects; BP reductions over weeks in some trials  
- **Key links**: Laborde et al. 2022 Neurosci Biobehav Rev meta [PMID: 35623448](https://pubmed.ncbi.nlm.nih.gov/35623448/); Chaddha et al. BP meta  
- **Safety**: Avoid forced hyperventilation if prone to lightheadedness.  
- **Notes**: Resonance frequency around 0.1 Hz. Pairs well with HRV biofeedback.

### HRV Biofeedback
- **Status**: Established  
- **Primary outcome domains**: Stress Resilience / Mood  
- **Protocol category**: Breathwork & Autonomic Regulation  
- **Mechanism tags**: Parasympathetic activation, resonance breathing with feedback  
- **Best evidence contexts**: Individuals with elevated stress or anxiety (clinical and non-clinical)  
- **Evidence grade**: B (large pre-post and between-group effects on self-reported stress/anxiety)  
- **Typical parameters**: 10–20 min sessions, several times/week, using wearable or app feedback at resonance frequency  
- **Onset**: Acute; cumulative over 4–8 weeks  
- **Key links**: Goessl et al. 2017 Psychol Med meta [PMID: 28478782](https://pubmed.ncbi.nlm.nih.gov/28478782/); Lehrer et al. reviews  
- **Safety**: Generally well-tolerated.  
- **Notes**: Builds on slow breathing; feedback improves adherence and precision.

### Photobiomodulation / Red-Near Infrared Light (Pre-Exercise)
- **Status**: Established (with parameter caveats)  
- **Primary outcome domains**: Physical Performance / Recovery  
- **Protocol category**: Photobiomodulation & Circadian Light  
- **Mechanism tags**: Photobiomodulation (cytochrome c oxidase, mitochondrial)  
- **Best evidence contexts**: Healthy adults and athletes, pre-exercise application  
- **Evidence grade**: B (modest improvements in endurance, strength recovery, DOMS in meta-analyses; highly parameter-dependent)  
- **Typical parameters**: Red (~660 nm) or NIR (~810–850 nm), 5–6 J/cm² per point or total dose 20–60 J, applied to muscle groups before exercise  
- **Onset**: Acute performance and 24 h recovery markers  
- **Key links**: Leal-Junior et al. reviews; 2024–2025 metas on pre-exercise PBMT  
- **Safety**: Eye protection with lasers; low risk with LED panels at appropriate doses.  
- **Notes**: Pre-exercise timing appears more consistent than post. Dose and wavelength matter.

### Contrast Water Therapy
- **Status**: Established  
- **Primary outcome domains**: Physical Performance / Recovery  
- **Protocol category**: Recovery Modalities  
- **Mechanism tags**: Alternating vasoconstriction/vasodilation  
- **Best evidence contexts**: Athletes after intense or damaging exercise  
- **Evidence grade**: B (better than passive rest for soreness and strength recovery; similar to CWI in many comparisons)  
- **Typical parameters**: Alternating 1–2 min hot (38–42 °C) and cold (10–15 °C), 3–6 cycles, total 10–20 min  
- **Onset**: Benefits at 24–48 h  
- **Key links**: Bieuzen et al. 2013 PLoS One meta [PMID: 23626806](https://pubmed.ncbi.nlm.nih.gov/23626806/)  
- **Safety**: Temperature extremes; cardiovascular caution.  
- **Notes**: Practical alternative when pure cold is unavailable.

### Evening Light Restriction / Dim Light
- **Status**: Established  
- **Primary outcome domains**: Sleep Architecture  
- **Protocol category**: Photobiomodulation & Circadian Light  
- **Mechanism tags**: Reduced circadian delay (melanopsin)  
- **Best evidence contexts**: Individuals with delayed sleep phase or evening screen use  
- **Evidence grade**: A for mechanism; B for real-world sleep improvement when combined with morning light  
- **Typical parameters**: Dim lights (<50–100 lux) and blue-light reduction 1–2 h before bed; warm bulbs or screens  
- **Onset**: Phase effects within days  
- **Key links**: Phase-response curve literature; screen-use studies  
- **Safety**: None significant.  
- **Notes**: Complements morning light. Amber glasses can help.

### Sleep Restriction Therapy (CBT-I Component)
- **Status**: Established  
- **Primary outcome domains**: Sleep Architecture  
- **Protocol category**: Sleep Optimization  
- **Mechanism tags**: Sleep drive consolidation  
- **Best evidence contexts**: Chronic insomnia  
- **Evidence grade**: A (most effective single CBT-I component in network metas)  
- **Typical parameters**: Limit time in bed to approximate average sleep time, then titrate up as efficiency improves. Usually guided.  
- **Onset**: Rapid improvement in efficiency; temporary daytime sleepiness  
- **Key links**: Component network meta-analyses 2024  
- **Safety**: Professional supervision recommended; not for untreated sleep apnea or certain psychiatric conditions.  
- **Notes**: Core active ingredient of CBT-I.

### Stimulus Control Therapy
- **Status**: Established  
- **Primary outcome domains**: Sleep Architecture  
- **Protocol category**: Sleep Optimization  
- **Mechanism tags**: Classical conditioning / bed-sleep association  
- **Best evidence contexts**: Chronic insomnia  
- **Evidence grade**: A (strong component of CBT-I)  
- **Typical parameters**: Bed only for sleep/sex; get up if not asleep in ~20 min; consistent wake time  
- **Onset**: Weeks  
- **Key links**: CBT-I component metas  
- **Safety**: Low.  
- **Notes**: Simple behavioral rules with high impact when followed.

### High-Intensity Interval Training (HIIT / 4×4)
- **Status**: Established  
- **Primary outcome domains**: Longevity / Healthspan, Physical Performance / Recovery, Mental Energy / Fatigue Resistance  
- **Protocol category**: Structured Training  
- **Mechanism tags**: VO2max, stroke volume, mitochondrial respiration  
- **Best evidence contexts**: Sedentary to recreationally active adults  
- **Evidence grade**: A for VO2max gains (time-efficient); B for mortality (via fitness)  
- **Typical parameters**: 4 × 4 min at 90–95% HRmax with 3 min active recovery; 1–2×/week. Or other HIIT formats.  
- **Onset**: VO2max improvements in 4–8 weeks  
- **Key links**: Helgerud et al. 2007; multiple HIIT metas  
- **Safety**: Medical clearance if high CVD risk; not daily.  
- **Notes**: Complements Zone 2 in polarized model. Higher intensity per minute than steady state.

### Yoga Nidra / Non-Sleep Deep Rest (NSDR)
- **Status**: Established (yoga nidra literature) / Research (NSDR branding)  
- **Primary outcome domains**: Stress Resilience / Mood, Sleep Architecture, Mental Energy / Fatigue Resistance  
- **Protocol category**: Neuro / Cognitive  
- **Mechanism tags**: Parasympathetic shift, slow-wave EEG while awake  
- **Best evidence contexts**: Individuals with elevated stress, anxiety, or insomnia symptoms  
- **Evidence grade**: B for stress/anxiety reduction (yoga nidra metas); C for specific performance or dopamine claims  
- **Typical parameters**: 10–30 min guided body-scan / rotation of consciousness, daily or as needed  
- **Onset**: Acute subjective calm; cumulative with regular practice  
- **Key links**: Yoga nidra systematic reviews and RCTs (e.g., Datta 2021 insomnia comparison)  
- **Safety**: Low; may fall asleep.  
- **Notes**: Not a sleep replacement. Evidence stronger for structured yoga nidra than generic “NSDR” audio.

### Heat Acclimation / Repeated Heat Exposure
- **Status**: Established  
- **Primary outcome domains**: Physical Performance / Recovery, Longevity / Healthspan  
- **Protocol category**: Thermal / Hormetic Stress  
- **Mechanism tags**: Plasma volume expansion, HSP induction, cardiovascular adaptation  
- **Best evidence contexts**: Athletes preparing for heat; general population for vascular benefits  
- **Evidence grade**: A for athletic heat acclimation; B for broader healthspan mechanisms  
- **Typical parameters**: Repeated sauna or hot-water immersion sessions over 7–14 days  
- **Onset**: Physiological adaptations within 1–2 weeks  
- **Key links**: Heat acclimation literature; sauna HSP studies  
- **Safety**: Same as sauna.  
- **Notes**: Overlaps with Finnish sauna protocol; can be sport-specific.

### Progressive Muscle Relaxation
- **Status**: Established  
- **Primary outcome domains**: Stress Resilience / Mood, Sleep Architecture  
- **Protocol category**: Neuro / Cognitive  
- **Mechanism tags**: Somatic tension reduction  
- **Best evidence contexts**: Anxiety, stress, insomnia adjunct  
- **Evidence grade**: B (modest but consistent reductions in anxiety and improved sleep onset)  
- **Typical parameters**: 10–20 min systematic tense-and-release cycles, daily  
- **Onset**: Acute  
- **Key links**: Multiple anxiety and sleep metas  
- **Safety**: Low.  
- **Notes**: Simple, no equipment. Often combined with CBT-I or breathing.

### Blue-Light Blocking in Evening (Amber Glasses)
- **Status**: Established  
- **Primary outcome domains**: Sleep Architecture  
- **Protocol category**: Photobiomodulation & Circadian Light  
- **Mechanism tags**: Reduced melanopsin stimulation  
- **Best evidence contexts**: Evening screen users, shift workers, delayed sleep phase  
- **Evidence grade**: B (small but consistent melatonin and sleep benefits in trials)  
- **Typical parameters**: Amber or orange glasses 1–3 h before bed  
- **Onset**: Same night melatonin preservation  
- **Key links**: Blue-light blocking RCTs  
- **Safety**: Low.  
- **Notes**: Practical tool when full light restriction is hard.

### Active Recovery / Low-Intensity Movement
- **Status**: Established  
- **Primary outcome domains**: Physical Performance / Recovery  
- **Protocol category**: Recovery Modalities  
- **Mechanism tags**: Blood flow, lactate clearance  
- **Best evidence contexts**: Post-intense training  
- **Evidence grade**: B (better than complete rest for some markers; inferior to CWI for DOMS in several comparisons)  
- **Typical parameters**: 10–30 min very easy cycling, walking, or swimming  
- **Onset**: Immediate to 24 h  
- **Key links**: Recovery modality metas  
- **Safety**: Low.  
- **Notes**: Accessible baseline recovery tool.

### Consistent Sleep-Wake Timing (Social Jet Lag Reduction)
- **Status**: Established  
- **Primary outcome domains**: Sleep Architecture, Longevity / Healthspan, Mental Energy / Fatigue Resistance  
- **Protocol category**: Sleep Optimization  
- **Mechanism tags**: Circadian alignment  
- **Best evidence contexts**: General population; stronger in those with large weekday-weekend shifts  
- **Evidence grade**: A for circadian health mechanisms; B–A for metabolic and mood associations  
- **Typical parameters**: Wake time variance <30–60 min across week; fixed bedtime window  
- **Onset**: Days to weeks  
- **Key links**: Social jet lag literature (Wittmann, Roenneberg)  
- **Safety**: None.  
- **Notes**: Free, high-leverage foundational protocol.

### Resistance Training Progressive Overload
- **Status**: Established  
- **Primary outcome domains**: Physical Performance / Recovery  
- **Protocol category**: Structured Training  
- **Mechanism tags**: Muscle protein synthesis, mechanical tension  
- **Best evidence contexts**: Healthy adults seeking strength or hypertrophy  
- **Evidence grade**: A  
- **Typical parameters**: 2–4 sets of 6–15 reps near failure, progressive load or volume, major movements, 2–4×/week  
- **Onset**: Strength neural adaptations in weeks; hypertrophy in 6–12+ weeks  
- **Key links**: ACSM position stands; hypertrophy metas  
- **Safety**: Form and recovery.  
- **Notes**: Complements the longevity-dose entry; this is the performance version.

### Hot Water Immersion / Hot Baths
- **Status**: Established  
- **Primary outcome domains**: Sleep Architecture, Stress Resilience / Mood, Physical Performance / Recovery  
- **Protocol category**: Thermal / Hormetic Stress  
- **Mechanism tags**: Core temperature drop after heating, passive heat stress  
- **Best evidence contexts**: Individuals with sleep-onset difficulty; recovery  
- **Evidence grade**: B (evening hot baths improve sleep onset; passive heating has cardiovascular benefits)  
- **Typical parameters**: 10–20 min at 40–42 °C, 1–2 h before bed for sleep  
- **Onset**: Same night for sleep  
- **Key links**: Hot-bath sleep metas  
- **Safety**: Hydration, temperature limits, cardiovascular caution.  
- **Notes**: Accessible alternative or adjunct to sauna.

### Meditation / Mindfulness (Structured)
- **Status**: Established  
- **Primary outcome domains**: Stress Resilience / Mood, Attention / Focus  
- **Protocol category**: Neuro / Cognitive  
- **Mechanism tags**: Attention training, reduced default-mode activity  
- **Best evidence contexts**: Elevated stress, anxiety, or attention goals  
- **Evidence grade**: B (consistent moderate effects on stress and anxiety; smaller for attention in healthy adults)  
- **Typical parameters**: 10–20 min daily focused-attention or open-monitoring practice  
- **Onset**: Acute calm; structural/functional changes with months of practice  
- **Key links**: Multiple mindfulness metas (e.g., Goyal et al.)  
- **Safety**: Low; rare increases in anxiety in sensitive individuals.  
- **Notes**: Broad category; specific protocols (MBSR) have stronger evidence.

### Intermittent Hypoxic Training / Altitude Simulation
- **Status**: Research / Investigational  
- **Primary outcome domains**: Physical Performance / Recovery, Longevity / Healthspan  
- **Protocol category**: Thermal / Hormetic Stress (hypoxic hormesis)  
- **Mechanism tags**: HIF-1α, erythropoiesis, mitochondrial  
- **Best evidence contexts**: Endurance athletes for performance; limited general health data  
- **Evidence grade**: B for athletic performance adaptations; C–D for broad longevity claims  
- **Typical parameters**: Intermittent hypoxia protocols (specific FiO2 and duration vary by device/protocol)  
- **Onset**: Weeks  
- **Key links**: Altitude training and IHT reviews  
- **Safety**: Medical supervision recommended; contraindicated in certain conditions.  
- **Notes**: Marked investigational for non-athletic use. Overlaps with some cold/heat hormesis concepts.

### Compression Garments (Post-Exercise)
- **Status**: Established  
- **Primary outcome domains**: Physical Performance / Recovery  
- **Protocol category**: Recovery Modalities  
- **Mechanism tags**: Reduced swelling, proprioceptive feedback  
- **Best evidence contexts**: Athletes after intense exercise  
- **Evidence grade**: B–C (small benefits for soreness and some performance markers; high heterogeneity)  
- **Typical parameters**: Graduated compression worn 1–24+ h post-exercise  
- **Onset**: 24 h  
- **Key links**: Compression recovery metas  
- **Safety**: Low.  
- **Notes**: Practical, low-effort adjunct.

### Polarized Training Model (Zone 2 + HIIT)
- **Status**: Established  
- **Primary outcome domains**: Longevity / Healthspan, Physical Performance / Recovery  
- **Protocol category**: Structured Training / Stacked  
- **Mechanism tags**: Combined mitochondrial volume + respiration  
- **Best evidence contexts**: Endurance athletes and recreationally active adults  
- **Evidence grade**: A for performance in endurance sports; B for general population longevity (via fitness)  
- **Typical parameters**: ≈80% volume at Zone 2, ≈20% at high intensity (e.g., 1 weekly 4×4), plus strength  
- **Onset**: Weeks to months  
- **Key links**: Seiler polarized training literature; elite athlete observational data  
- **Safety**: Same as component protocols.  
- **Notes**: Practical synthesis of Zone 2 and HIIT entries.

---

**Total seeded entries: 25**

This is a living document. Additional protocols (e.g., specific cold exposure dosing outside of recovery, more stacked routines, device-based interventions with stronger data) can be added in future versions while preserving the schema and evidence standards.
