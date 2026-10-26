package com.neon.ascent.core.domain.labs.models

data class BiohackingProtocolOverride(
    val preferredFrequency: String? = null,
    val durationNote: String? = null,
    val notes: String? = null,
    val isPaused: Boolean = false
)

data class ProtocolStackingNote(
    val title: String,
    val description: String,
    val evidenceGradeTag: String, // "Grade A" | "Grade B" | "Caution"
    val relatedProtocolOrCategory: String? = null
)

data class BiohackingProtocol(
    val id: String,
    val name: String,
    val aliases: List<String> = emptyList(),
    val status: String = "Established", // Established | Research / Investigational
    val primaryOutcomeDomains: List<String> = emptyList(),
    val protocolCategory: String = "",
    val mechanismTags: List<String> = emptyList(),
    val evidenceGrade: String = "B", // A | B | C | D
    val bestContexts: String = "",
    val typicalParameters: String = "",
    val onset: String = "",
    val keyLinks: List<String> = emptyList(),
    val safetyNotes: String = "",
    val notes: String = "",
    val stackingNotes: List<ProtocolStackingNote> = emptyList()
)

val SEED_BIOHACKING_PROTOCOLS: List<BiohackingProtocol> = listOf(
    BiohackingProtocol(
        id = "finnish_sauna",
        name = "Finnish-Style Sauna Bathing",
        aliases = listOf("Finnish Sauna", "Sauna Bathing", "Sauna"),
        status = "Established",
        primaryOutcomeDomains = listOf("Longevity / Healthspan", "Physical Performance / Recovery", "Stress Resilience / Mood"),
        protocolCategory = "Thermal / Hormetic Stress",
        mechanismTags = listOf("Hormesis / Heat shock proteins", "Cardiovascular adaptation"),
        bestContexts = "Middle-aged adults (large Finnish cohorts)",
        evidenceGrade = "A",
        typicalParameters = "15–20+ min at 80–100 °C (dry), 4–7 sessions/week for strongest signal; 2–3×/week still beneficial",
        onset = "Acute cardiovascular effects immediate; mortality associations from chronic multi-year use",
        keyLinks = listOf("https://pubmed.ncbi.nlm.nih.gov/25705890/", "https://pubmed.ncbi.nlm.nih.gov/30486813/"),
        safetyNotes = "Hydrate; avoid if unstable angina, recent MI, or severe hypotension. Alcohol increases risk.",
        notes = "Strongest population-level longevity signal among thermal protocols. Dose-response with no clear threshold in the data.",
        stackingNotes = listOf(
            ProtocolStackingNote(
                title = "Sauna Frequency Dose-Response",
                description = "Strongest mortality reduction observed at 4–7 sessions/week; 2–3 sessions/week still beneficial.",
                evidenceGradeTag = "Grade A",
                relatedProtocolOrCategory = "Thermal / Hormetic Stress"
            )
        )
    ),
    BiohackingProtocol(
        id = "cold_water_immersion",
        name = "Cold-Water Immersion (Post-Exercise)",
        aliases = listOf("CWI", "Ice Bath", "Cold Plunge"),
        status = "Established",
        primaryOutcomeDomains = listOf("Physical Performance / Recovery"),
        protocolCategory = "Recovery Modalities",
        mechanismTags = listOf("Local cooling", "reduced inflammation / DOMS"),
        bestContexts = "Physically active adults after strenuous or eccentric exercise",
        evidenceGrade = "B",
        typicalParameters = "10–15 min at 5–15 °C (whole or partial body)",
        onset = "Benefits for soreness and CK primarily at 24 h",
        keyLinks = listOf("https://pubmed.ncbi.nlm.nih.gov/36527593/"),
        safetyNotes = "Cold shock risk; avoid with certain cardiac conditions.",
        notes = "Periodize rather than use after every strength session if hypertrophy is the goal.",
        stackingNotes = listOf(
            ProtocolStackingNote(
                title = "Acute DOMS vs. Chronic Hypertrophy Trade-off",
                description = "Effective for acute soreness reduction (DOMS), but routine immediate post-lifting use may blunt chronic muscle hypertrophy gains.",
                evidenceGradeTag = "Caution",
                relatedProtocolOrCategory = "Resistance Training Progressive Overload"
            )
        )
    ),
    BiohackingProtocol(
        id = "morning_outdoor_light",
        name = "Morning Outdoor Light Exposure",
        aliases = listOf("Morning Light", "Circadian Sunlight"),
        status = "Established",
        primaryOutcomeDomains = listOf("Sleep Architecture", "Mental Energy / Fatigue Resistance", "Stress Resilience / Mood"),
        protocolCategory = "Photobiomodulation & Circadian Light",
        mechanismTags = listOf("Circadian phase advance (melanopsin → SCN)"),
        bestContexts = "Healthy adults with delayed sleep phase or indoor lifestyles; seasonal affective patterns",
        evidenceGrade = "A",
        typicalParameters = "5–30 min outdoors within 30–60 min of waking (cloudy days require longer). Avoid sunglasses if safe.",
        onset = "Phase advance measurable within days; sleep benefits accumulate over 1–2 weeks",
        keyLinks = listOf("https://pubmed.ncbi.nlm.nih.gov/23910656/"),
        safetyNotes = "Eye protection in extreme brightness; caution in bipolar disorder with bright light.",
        notes = "Highest-leverage free circadian protocol. Outdoor lux >> indoor.",
        stackingNotes = listOf(
            ProtocolStackingNote(
                title = "Circadian Light Alignment Stack",
                description = "Morning outdoor light exposure combined with evening dim light restriction produces strong additive circadian phase advance and sleep efficiency.",
                evidenceGradeTag = "Grade A",
                relatedProtocolOrCategory = "Evening Light Restriction / Dim Light"
            )
        )
    ),
    BiohackingProtocol(
        id = "zone_2_aerobic",
        name = "Zone 2 Aerobic Training",
        aliases = listOf("Zone 2", "Base Cardio", "Conversational Pace Cardio"),
        status = "Established",
        primaryOutcomeDomains = listOf("Longevity / Healthspan", "Mental Energy / Fatigue Resistance", "Physical Performance / Recovery"),
        protocolCategory = "Structured Training",
        mechanismTags = listOf("Mitochondrial biogenesis", "fat oxidation", "VO2max support"),
        bestContexts = "General population for mortality; endurance athletes for polarized training base",
        evidenceGrade = "A",
        typicalParameters = "Conversational pace (lactate <2 mmol/L or talk test), 150–300+ min/week total moderate aerobic; 2–4 sessions of 30–60 min",
        onset = "Fitness gains in weeks; mortality associations from long-term habits",
        keyLinks = listOf("https://pubmed.ncbi.nlm.nih.gov/30326054/"),
        safetyNotes = "Start low if sedentary; medical clearance if CVD risk.",
        notes = "Base of polarized model (≈80% easy). High-intensity intervals raise VO2max faster per minute but Zone 2 builds sustainable volume.",
        stackingNotes = listOf(
            ProtocolStackingNote(
                title = "Zone 2 + Resistance Training Baseline",
                description = "Combining moderate aerobic volume with regular resistance training yields lower all-cause mortality than either protocol alone.",
                evidenceGradeTag = "Grade A",
                relatedProtocolOrCategory = "Resistance Training (Longevity Dose)"
            )
        )
    ),
    BiohackingProtocol(
        id = "resistance_training_longevity",
        name = "Resistance Training (Longevity Dose)",
        aliases = listOf("Strength Training (Longevity)", "Lifting for Healthspan"),
        status = "Established",
        primaryOutcomeDomains = listOf("Longevity / Healthspan", "Physical Performance / Recovery"),
        protocolCategory = "Structured Training",
        mechanismTags = listOf("Muscle protein synthesis", "mechanical loading", "sarcopenia prevention"),
        bestContexts = "Adults ≥18; strongest mortality signal in middle-aged and older",
        evidenceGrade = "A",
        typicalParameters = "30–120 min/week total; 2–3 sessions targeting major muscle groups. Lowest risk around 60–120 min/week in recent analyses.",
        onset = "Strength/muscle in weeks; mortality associations from multi-year adherence",
        keyLinks = listOf("https://pubmed.ncbi.nlm.nih.gov/35131965/"),
        safetyNotes = "Technique first; progressive overload.",
        notes = "Combined with aerobic yields lower risk than either alone. No clear benefit above ~120 min/week for mortality in some analyses.",
        stackingNotes = listOf(
            ProtocolStackingNote(
                title = "Aerobic + Resistance Synergy",
                description = "Combining resistance training with aerobic fitness produces the lowest combined mortality hazard ratio in large prospective cohorts.",
                evidenceGradeTag = "Grade A",
                relatedProtocolOrCategory = "Zone 2 Aerobic Training"
            )
        )
    ),
    BiohackingProtocol(
        id = "cbt_i",
        name = "Cognitive Behavioral Therapy for Insomnia (CBT-I)",
        aliases = listOf("CBT-I", "CBTI"),
        status = "Established",
        primaryOutcomeDomains = listOf("Sleep Architecture"),
        protocolCategory = "Sleep Optimization",
        mechanismTags = listOf("Cognitive behavioral restructuring", "sleep restriction", "stimulus control"),
        bestContexts = "Adults with chronic insomnia (with or without comorbidity)",
        evidenceGrade = "A",
        typicalParameters = "Multi-component (sleep restriction + stimulus control + cognitive restructuring); 4–8 sessions or digital equivalent",
        onset = "Significant improvement within weeks; effects durable at 6–12 months",
        keyLinks = listOf("https://pubmed.ncbi.nlm.nih.gov/26054060/"),
        safetyNotes = "Temporary sleep restriction can increase daytime sleepiness; professional guidance preferred.",
        notes = "Strongest non-pharmacologic sleep intervention. Digital versions effective but in-person often superior."
    ),
    BiohackingProtocol(
        id = "slow_paced_breathing",
        name = "Slow-Paced Breathing (~6 breaths/min)",
        aliases = listOf("Resonance Breathing", "Coherence Breathing"),
        status = "Established",
        primaryOutcomeDomains = listOf("Stress Resilience / Mood", "Physical Performance / Recovery"),
        protocolCategory = "Breathwork & Autonomic Regulation",
        mechanismTags = listOf("Parasympathetic activation", "respiratory sinus arrhythmia", "baroreflex"),
        bestContexts = "Healthy adults and prehypertensive/hypertensive populations",
        evidenceGrade = "B",
        typicalParameters = "5–10 min at ~6 cycles/min (inhale:exhale often 1:1 or longer exhale), daily or multiple times",
        onset = "Immediate HRV and HR effects; BP reductions over weeks in some trials",
        keyLinks = listOf("https://pubmed.ncbi.nlm.nih.gov/35623448/"),
        safetyNotes = "Avoid forced hyperventilation if prone to lightheadedness.",
        notes = "Resonance frequency around 0.1 Hz. Pairs well with HRV biofeedback.",
        stackingNotes = listOf(
            ProtocolStackingNote(
                title = "Breathing + HRV Biofeedback Synergy",
                description = "Resonance frequency breathing (~6 breaths/min) paired with real-time HRV biofeedback enhances autonomic self-regulation and stress reduction.",
                evidenceGradeTag = "Grade B",
                relatedProtocolOrCategory = "HRV Biofeedback"
            )
        )
    ),
    BiohackingProtocol(
        id = "hrv_biofeedback",
        name = "HRV Biofeedback",
        aliases = listOf("Heart Rate Variability Biofeedback"),
        status = "Established",
        primaryOutcomeDomains = listOf("Stress Resilience / Mood"),
        protocolCategory = "Breathwork & Autonomic Regulation",
        mechanismTags = listOf("Parasympathetic activation", "resonance breathing with feedback"),
        bestContexts = "Individuals with elevated stress or anxiety (clinical and non-clinical)",
        evidenceGrade = "B",
        typicalParameters = "10–20 min sessions, several times/week, using wearable or app feedback at resonance frequency",
        onset = "Acute; cumulative over 4–8 weeks",
        keyLinks = listOf("https://pubmed.ncbi.nlm.nih.gov/28478782/"),
        safetyNotes = "Generally well-tolerated.",
        notes = "Builds on slow breathing; feedback improves adherence and precision."
    ),
    BiohackingProtocol(
        id = "photobiomodulation_pre_exercise",
        name = "Photobiomodulation / Red-Near Infrared Light (Pre-Exercise)",
        aliases = listOf("Red Light Therapy", "NIR Light", "PBMT"),
        status = "Established",
        primaryOutcomeDomains = listOf("Physical Performance / Recovery"),
        protocolCategory = "Photobiomodulation & Circadian Light",
        mechanismTags = listOf("Photobiomodulation (cytochrome c oxidase, mitochondrial)"),
        bestContexts = "Healthy adults and athletes, pre-exercise application",
        evidenceGrade = "B",
        typicalParameters = "Red (~660 nm) or NIR (~810–850 nm), 5–6 J/cm² per point or total dose 20–60 J, applied to muscle groups before exercise",
        onset = "Acute performance and 24 h recovery markers",
        keyLinks = listOf("https://pubmed.ncbi.nlm.nih.gov/"),
        safetyNotes = "Eye protection with lasers; low risk with LED panels at appropriate doses.",
        notes = "Pre-exercise timing appears more consistent than post. Dose and wavelength matter."
    ),
    BiohackingProtocol(
        id = "contrast_water_therapy",
        name = "Contrast Water Therapy",
        aliases = listOf("Contrast Bath", "Hot-Cold Therapy"),
        status = "Established",
        primaryOutcomeDomains = listOf("Physical Performance / Recovery"),
        protocolCategory = "Recovery Modalities",
        mechanismTags = listOf("Alternating vasoconstriction/vasodilation"),
        bestContexts = "Athletes after intense or damaging exercise",
        evidenceGrade = "B",
        typicalParameters = "Alternating 1–2 min hot (38–42 °C) and cold (10–15 °C), 3–6 cycles, total 10–20 min",
        onset = "Benefits at 24–48 h",
        keyLinks = listOf("https://pubmed.ncbi.nlm.nih.gov/23626806/"),
        safetyNotes = "Temperature extremes; cardiovascular caution.",
        notes = "Practical alternative when pure cold is unavailable."
    ),
    BiohackingProtocol(
        id = "evening_light_restriction",
        name = "Evening Light Restriction / Dim Light",
        aliases = listOf("Dim Light Evening", "Blue Light Restriction"),
        status = "Established",
        primaryOutcomeDomains = listOf("Sleep Architecture"),
        protocolCategory = "Photobiomodulation & Circadian Light",
        mechanismTags = listOf("Reduced circadian delay (melanopsin)"),
        bestContexts = "Individuals with delayed sleep phase or evening screen use",
        evidenceGrade = "A",
        typicalParameters = "Dim lights (<50–100 lux) and blue-light reduction 1–2 h before bed; warm bulbs or screens",
        onset = "Phase effects within days",
        keyLinks = listOf("https://pubmed.ncbi.nlm.nih.gov/"),
        safetyNotes = "None significant.",
        notes = "Complements morning light. Amber glasses can help.",
        stackingNotes = listOf(
            ProtocolStackingNote(
                title = "Circadian Light Complement",
                description = "Evening dim light or blue-light blocking amber glasses complements morning outdoor light to preserve nocturnal melatonin release.",
                evidenceGradeTag = "Grade A",
                relatedProtocolOrCategory = "Morning Outdoor Light Exposure"
            )
        )
    ),
    BiohackingProtocol(
        id = "sleep_restriction_therapy",
        name = "Sleep Restriction Therapy (CBT-I Component)",
        aliases = listOf("SRT"),
        status = "Established",
        primaryOutcomeDomains = listOf("Sleep Architecture"),
        protocolCategory = "Sleep Optimization",
        mechanismTags = listOf("Sleep drive consolidation"),
        bestContexts = "Chronic insomnia",
        evidenceGrade = "A",
        typicalParameters = "Limit time in bed to approximate average sleep time, then titrate up as efficiency improves. Usually guided.",
        onset = "Rapid improvement in efficiency; temporary daytime sleepiness",
        keyLinks = listOf("https://pubmed.ncbi.nlm.nih.gov/"),
        safetyNotes = "Professional supervision recommended; not for untreated sleep apnea or certain psychiatric conditions.",
        notes = "Core active ingredient of CBT-I."
    ),
    BiohackingProtocol(
        id = "stimulus_control_therapy",
        name = "Stimulus Control Therapy",
        aliases = listOf("Stimulus Control"),
        status = "Established",
        primaryOutcomeDomains = listOf("Sleep Architecture"),
        protocolCategory = "Sleep Optimization",
        mechanismTags = listOf("Classical conditioning / bed-sleep association"),
        bestContexts = "Chronic insomnia",
        evidenceGrade = "A",
        typicalParameters = "Bed only for sleep/sex; get up if not asleep in ~20 min; consistent wake time",
        onset = "Weeks",
        keyLinks = listOf("https://pubmed.ncbi.nlm.nih.gov/"),
        safetyNotes = "Low.",
        notes = "Simple behavioral rules with high impact when followed."
    ),
    BiohackingProtocol(
        id = "hiit_4x4",
        name = "High-Intensity Interval Training (HIIT / 4×4)",
        aliases = listOf("HIIT", "Norwegian 4x4", "Interval Training"),
        status = "Established",
        primaryOutcomeDomains = listOf("Longevity / Healthspan", "Physical Performance / Recovery", "Mental Energy / Fatigue Resistance"),
        protocolCategory = "Structured Training",
        mechanismTags = listOf("VO2max", "stroke volume", "mitochondrial respiration"),
        bestContexts = "Sedentary to recreationally active adults",
        evidenceGrade = "A",
        typicalParameters = "4 × 4 min at 90–95% HRmax with 3 min active recovery; 1–2×/week. Or other HIIT formats.",
        onset = "VO2max improvements in 4–8 weeks",
        keyLinks = listOf("https://pubmed.ncbi.nlm.nih.gov/"),
        safetyNotes = "Medical clearance if high CVD risk; not daily.",
        notes = "Complements Zone 2 in polarized model. Higher intensity per minute than steady state."
    ),
    BiohackingProtocol(
        id = "yoga_nidra_nsdr",
        name = "Yoga Nidra / Non-Sleep Deep Rest (NSDR)",
        aliases = listOf("Yoga Nidra", "NSDR", "Guided Body Scan"),
        status = "Established",
        primaryOutcomeDomains = listOf("Stress Resilience / Mood", "Sleep Architecture", "Mental Energy / Fatigue Resistance"),
        protocolCategory = "Neuro / Cognitive",
        mechanismTags = listOf("Parasympathetic shift", "slow-wave EEG while awake"),
        bestContexts = "Individuals with elevated stress, anxiety, or insomnia symptoms",
        evidenceGrade = "B",
        typicalParameters = "10–30 min guided body-scan / rotation of consciousness, daily or as needed",
        onset = "Acute subjective calm; cumulative with regular practice",
        keyLinks = listOf("https://pubmed.ncbi.nlm.nih.gov/"),
        safetyNotes = "Low; may fall asleep.",
        notes = "Not a sleep replacement. Evidence stronger for structured yoga nidra than generic “NSDR” audio."
    ),
    BiohackingProtocol(
        id = "heat_acclimation",
        name = "Heat Acclimation / Repeated Heat Exposure",
        aliases = listOf("Heat Acclimatization", "Heat Adaptation"),
        status = "Established",
        primaryOutcomeDomains = listOf("Physical Performance / Recovery", "Longevity / Healthspan"),
        protocolCategory = "Thermal / Hormetic Stress",
        mechanismTags = listOf("Plasma volume expansion", "HSP induction", "cardiovascular adaptation"),
        bestContexts = "Athletes preparing for heat; general population for vascular benefits",
        evidenceGrade = "A",
        typicalParameters = "Repeated sauna or hot-water immersion sessions over 7–14 days",
        onset = "Physiological adaptations within 1–2 weeks",
        keyLinks = listOf("https://pubmed.ncbi.nlm.nih.gov/"),
        safetyNotes = "Same as sauna.",
        notes = "Overlaps with Finnish sauna protocol; can be sport-specific."
    ),
    BiohackingProtocol(
        id = "progressive_muscle_relaxation",
        name = "Progressive Muscle Relaxation",
        aliases = listOf("PMR"),
        status = "Established",
        primaryOutcomeDomains = listOf("Stress Resilience / Mood", "Sleep Architecture"),
        protocolCategory = "Neuro / Cognitive",
        mechanismTags = listOf("Somatic tension reduction"),
        bestContexts = "Anxiety, stress, insomnia adjunct",
        evidenceGrade = "B",
        typicalParameters = "10–20 min systematic tense-and-release cycles, daily",
        onset = "Acute",
        keyLinks = listOf("https://pubmed.ncbi.nlm.nih.gov/"),
        safetyNotes = "Low.",
        notes = "Simple, no equipment. Often combined with CBT-I or breathing."
    ),
    BiohackingProtocol(
        id = "blue_light_blocking_glasses",
        name = "Blue-Light Blocking in Evening (Amber Glasses)",
        aliases = listOf("Blue Blockers", "Amber Glasses"),
        status = "Established",
        primaryOutcomeDomains = listOf("Sleep Architecture"),
        protocolCategory = "Photobiomodulation & Circadian Light",
        mechanismTags = listOf("Reduced melanopsin stimulation"),
        bestContexts = "Evening screen users, shift workers, delayed sleep phase",
        evidenceGrade = "B",
        typicalParameters = "Amber or orange glasses 1–3 h before bed",
        onset = "Same night melatonin preservation",
        keyLinks = listOf("https://pubmed.ncbi.nlm.nih.gov/"),
        safetyNotes = "Low.",
        notes = "Practical tool when full light restriction is hard."
    ),
    BiohackingProtocol(
        id = "active_recovery",
        name = "Active Recovery / Low-Intensity Movement",
        aliases = listOf("Active Recovery", "Flush Workout"),
        status = "Established",
        primaryOutcomeDomains = listOf("Physical Performance / Recovery"),
        protocolCategory = "Recovery Modalities",
        mechanismTags = listOf("Blood flow", "lactate clearance"),
        bestContexts = "Post-intense training",
        evidenceGrade = "B",
        typicalParameters = "10–30 min very easy cycling, walking, or swimming",
        onset = "Immediate to 24 h",
        keyLinks = listOf("https://pubmed.ncbi.nlm.nih.gov/"),
        safetyNotes = "Low.",
        notes = "Accessible baseline recovery tool."
    ),
    BiohackingProtocol(
        id = "consistent_sleep_wake_timing",
        name = "Consistent Sleep-Wake Timing (Social Jet Lag Reduction)",
        aliases = listOf("Circadian Consistency", "Social Jet Lag Reduction"),
        status = "Established",
        primaryOutcomeDomains = listOf("Sleep Architecture", "Longevity / Healthspan", "Mental Energy / Fatigue Resistance"),
        protocolCategory = "Sleep Optimization",
        mechanismTags = listOf("Circadian alignment"),
        bestContexts = "General population; stronger in those with large weekday-weekend shifts",
        evidenceGrade = "A",
        typicalParameters = "Wake time variance <30–60 min across week; fixed bedtime window",
        onset = "Days to weeks",
        keyLinks = listOf("https://pubmed.ncbi.nlm.nih.gov/"),
        safetyNotes = "None.",
        notes = "Free, high-leverage foundational protocol."
    ),
    BiohackingProtocol(
        id = "resistance_training_progressive_overload",
        name = "Resistance Training Progressive Overload",
        aliases = listOf("Progressive Overload", "Hypertrophy Lifting"),
        status = "Established",
        primaryOutcomeDomains = listOf("Physical Performance / Recovery"),
        protocolCategory = "Structured Training",
        mechanismTags = listOf("Muscle protein synthesis", "mechanical tension"),
        bestContexts = "Healthy adults seeking strength or hypertrophy",
        evidenceGrade = "A",
        typicalParameters = "2–4 sets of 6–15 reps near failure, progressive load or volume, major movements, 2–4×/week",
        onset = "Strength neural adaptations in weeks; hypertrophy in 6–12+ weeks",
        keyLinks = listOf("https://pubmed.ncbi.nlm.nih.gov/"),
        safetyNotes = "Form and recovery.",
        notes = "Complements the longevity-dose entry; this is the performance version."
    ),
    BiohackingProtocol(
        id = "hot_water_immersion",
        name = "Hot Water Immersion / Hot Baths",
        aliases = listOf("Hot Bath", "Hot Soak"),
        status = "Established",
        primaryOutcomeDomains = listOf("Sleep Architecture", "Stress Resilience / Mood", "Physical Performance / Recovery"),
        protocolCategory = "Thermal / Hormetic Stress",
        mechanismTags = listOf("Core temperature drop after heating", "passive heat stress"),
        bestContexts = "Individuals with sleep-onset difficulty; recovery",
        evidenceGrade = "B",
        typicalParameters = "10–20 min at 40–42 °C, 1–2 h before bed for sleep",
        onset = "Same night for sleep",
        keyLinks = listOf("https://pubmed.ncbi.nlm.nih.gov/"),
        safetyNotes = "Hydration, temperature limits, cardiovascular caution.",
        notes = "Accessible alternative or adjunct to sauna."
    ),
    BiohackingProtocol(
        id = "meditation_mindfulness",
        name = "Meditation / Mindfulness (Structured)",
        aliases = listOf("Mindfulness Meditation", "MBSR"),
        status = "Established",
        primaryOutcomeDomains = listOf("Stress Resilience / Mood", "Attention / Focus"),
        protocolCategory = "Neuro / Cognitive",
        mechanismTags = listOf("Attention training", "reduced default-mode activity"),
        bestContexts = "Elevated stress, anxiety, or attention goals",
        evidenceGrade = "B",
        typicalParameters = "10–20 min daily focused-attention or open-monitoring practice",
        onset = "Acute calm; structural/functional changes with months of practice",
        keyLinks = listOf("https://pubmed.ncbi.nlm.nih.gov/"),
        safetyNotes = "Low; rare increases in anxiety in sensitive individuals.",
        notes = "Broad category; specific protocols (MBSR) have stronger evidence."
    ),
    BiohackingProtocol(
        id = "intermittent_hypoxic_training",
        name = "Intermittent Hypoxic Training / Altitude Simulation",
        aliases = listOf("IHT", "Hypoxic Training", "Altitude Simulation"),
        status = "Research / Investigational",
        primaryOutcomeDomains = listOf("Physical Performance / Recovery", "Longevity / Healthspan"),
        protocolCategory = "Thermal / Hormetic Stress",
        mechanismTags = listOf("HIF-1α", "erythropoiesis", "mitochondrial"),
        bestContexts = "Endurance athletes for performance; limited general health data",
        evidenceGrade = "B",
        typicalParameters = "Intermittent hypoxia protocols (specific FiO2 and duration vary by device/protocol)",
        onset = "Weeks",
        keyLinks = listOf("https://pubmed.ncbi.nlm.nih.gov/"),
        safetyNotes = "Medical supervision recommended; contraindicated in certain conditions.",
        notes = "Marked investigational for non-athletic use. Overlaps with some cold/heat hormesis concepts."
    ),
    BiohackingProtocol(
        id = "compression_garments",
        name = "Compression Garments (Post-Exercise)",
        aliases = listOf("Compression Tights", "Compression Sleeves"),
        status = "Established",
        primaryOutcomeDomains = listOf("Physical Performance / Recovery"),
        protocolCategory = "Recovery Modalities",
        mechanismTags = listOf("Reduced swelling", "proprioceptive feedback"),
        bestContexts = "Athletes after intense exercise",
        evidenceGrade = "B",
        typicalParameters = "Graduated compression worn 1–24+ h post-exercise",
        onset = "24 h",
        keyLinks = listOf("https://pubmed.ncbi.nlm.nih.gov/"),
        safetyNotes = "Low.",
        notes = "Practical, low-effort adjunct."
    ),
    BiohackingProtocol(
        id = "polarized_training_model",
        name = "Polarized Training Model (Zone 2 + HIIT)",
        aliases = listOf("Polarized Training", "80/20 Endurance"),
        status = "Established",
        primaryOutcomeDomains = listOf("Longevity / Healthspan", "Physical Performance / Recovery"),
        protocolCategory = "Structured Training",
        mechanismTags = listOf("Combined mitochondrial volume + respiration"),
        bestContexts = "Endurance athletes and recreationally active adults",
        evidenceGrade = "A",
        typicalParameters = "≈80% volume at Zone 2, ≈20% at high intensity (e.g., 1 weekly 4×4), plus strength",
        onset = "Weeks to months",
        keyLinks = listOf("https://pubmed.ncbi.nlm.nih.gov/"),
        safetyNotes = "Same as component protocols.",
        notes = "Practical synthesis of Zone 2 and HIIT entries.",
        stackingNotes = listOf(
            ProtocolStackingNote(
                title = "Polarized Endurance Synthesis",
                description = "Integrates ≈80% Zone 2 aerobic volume with ≈20% high-intensity interval training (HIIT 4x4) and resistance training for optimal cardiorespiratory fitness.",
                evidenceGradeTag = "Grade A",
                relatedProtocolOrCategory = "Zone 2 Aerobic Training"
            )
        )
    )
)
