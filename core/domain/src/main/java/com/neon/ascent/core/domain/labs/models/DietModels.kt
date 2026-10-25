package com.neon.ascent.core.domain.labs.models

data class DietParameters(
    val proteinGramsPerKgMin: Double? = null,
    val proteinGramsPerKgMax: Double? = null,
    val baselineNetCarbsGramsMin: Int? = null,
    val baselineNetCarbsGramsMax: Int? = null,
    val higherCarbMealsPerWeekMin: Int? = null,
    val higherCarbMealsPerWeekMax: Int? = null,
    val higherCarbTimingPreference: String? = null,
    val eatingWindowHours: Int? = null,
    val fastingWindowHours: Int? = null,
    val freeVegetablesDescription: String? = null,
    val netCarbCalculationRule: String? = null,
    val rawParametersDescription: String? = null
)

data class DietOverrides(
    val preferredProteinGramsPerKg: Double? = null,
    val preferredModestCarbTargetGrams: Int? = null,
    val preferredHigherCarbMealDays: List<String> = emptyList(),
    val preferredHigherCarbMealsPerWeek: Int? = null,
    val freeListExceptions: List<String> = emptyList(),
    val notes: String? = null
)

data class DietStackingNote(
    val title: String,
    val description: String,
    val evidenceGradeTag: String,
    val relatedDietOrProtocol: String? = null
)

data class Diet(
    val id: String,
    val name: String,
    val status: String,
    val primaryOutcomeDomains: List<String>,
    val dietCategory: String,
    val mechanismTags: List<String>,
    val evidenceGrade: String,
    val typicalParameters: DietParameters,
    val onset: String,
    val keyLinks: List<String>,
    val safetyNotes: String,
    val isAppSpecific: Boolean = false,
    val stackingNotes: List<DietStackingNote> = emptyList()
)

val CYBER_CYCLICAL_DIET = Diet(
    id = "cyber_cyclical_diet",
    name = "Cyber Cyclical Diet (CCD)",
    status = "App-Specific",
    primaryOutcomeDomains = listOf(
        "Physical Performance / Recovery",
        "Longevity / Healthspan",
        "Mental Energy / Fatigue Resistance"
    ),
    dietCategory = "App-Specific / Custom, Macronutrient-Focused",
    mechanismTags = listOf(
        "Protein leverage / muscle protein synthesis",
        "Metabolic flexibility",
        "Insulin sensitivity / glycemic control"
    ),
    evidenceGrade = "B for higher-protein modest-carb patterns on body composition and satiety when combined with resistance training; C for precise cyclical timing superiority over consistent moderate carbohydrate intake",
    typicalParameters = DietParameters(
        proteinGramsPerKgMin = 1.6,
        proteinGramsPerKgMax = 2.2,
        baselineNetCarbsGramsMin = 50,
        baselineNetCarbsGramsMax = 100,
        higherCarbMealsPerWeekMin = 1,
        higherCarbMealsPerWeekMax = 2,
        higherCarbTimingPreference = "Post-training or evening preferred",
        freeVegetablesDescription = "Free / near-free non-starchy vegetables (do not count toward daily net target in normal portions): leafy greens, broccoli, cauliflower, cabbage, Brussels sprouts, asparagus, zucchini, cucumber, celery, mushrooms, peppers",
        netCarbCalculationRule = "Net carbs for whole foods (total carbohydrate minus fiber). Treat packaged foods more cautiously.",
        rawParametersDescription = "Protein: 1.6–2.2 g/kg body weight daily. Most days: modest net carbs (~50–100 g). 1–2 higher-carb meals per week (post-training or evening preferred). Free non-starchy vegetables. Net carbs for whole foods."
    ),
    onset = "Acute satiety and energy shifts within days; measurable body-composition changes typically over weeks when paired with training and adequate sleep.",
    keyLinks = listOf(
        "https://pubmed.ncbi.nlm.nih.gov/24299050/",
        "Morton et al. meta-analysis on protein and resistance training"
    ),
    safetyNotes = "Not a ketogenic protocol. Adjust carbohydrate upward if energy, training performance, or thyroid symptoms decline. Individual glucose responses vary. Training and sleep are required co-pillars.",
    isAppSpecific = true,
    stackingNotes = listOf(
        DietStackingNote(
            title = "CCD + Resistance Training",
            description = "1.6–2.2 g/kg daily protein target directly supports muscle protein synthesis and lean-mass retention during training blocks.",
            evidenceGradeTag = "Grade A",
            relatedDietOrProtocol = "High-Protein / Protein-Leverage Focus"
        ),
        DietStackingNote(
            title = "CCD Higher-Carb Refeeds",
            description = "1–2 higher-carb meals/week are preferred post-training or in the evening; total weekly carbohydrate balance dominates exact meal timing.",
            evidenceGradeTag = "Grade B",
            relatedDietOrProtocol = "CyberCrapp Protocol"
        ),
        DietStackingNote(
            title = "TRE + CCD Stacking",
            description = "When pairing 16:8 Time-Restricted Eating with CCD, the 8-hour window still applies; prioritize protein floor distribution inside the eating window.",
            evidenceGradeTag = "Grade B",
            relatedDietOrProtocol = "Time-Restricted Eating (16:8)"
        )
    )
)

val MEDITERRANEAN_DIET = Diet(
    id = "mediterranean_diet",
    name = "Mediterranean Diet",
    status = "Established",
    primaryOutcomeDomains = listOf(
        "Longevity / Healthspan",
        "Physical Performance / Recovery"
    ),
    dietCategory = "Pattern / Regional",
    mechanismTags = listOf(
        "Nutrient density",
        "Insulin sensitivity / glycemic control"
    ),
    evidenceGrade = "A (large prospective cohorts and trials for CVD and all-cause mortality)",
    typicalParameters = DietParameters(
        rawParametersDescription = "High in vegetables, fruit, legumes, nuts, olive oil, fish; moderate dairy and poultry; limited red and processed meat; whole grains."
    ),
    onset = "Risk-factor improvements in weeks to months; mortality associations from multi-year adherence.",
    keyLinks = listOf(
        "PREDIMED trial",
        "Meta-analyses of Mediterranean patterns"
    ),
    safetyNotes = "Strongest long-term hard-outcome signal among common patterns. Compatible with higher protein targets.",
    isAppSpecific = false,
    stackingNotes = listOf(
        DietStackingNote(
            title = "Mediterranean + High Protein",
            description = "Fully compatible with higher daily protein targets (1.6–2.2 g/kg) by emphasizing lean poultry, fish, legumes, and Greek yogurt.",
            evidenceGradeTag = "Grade A",
            relatedDietOrProtocol = "High-Protein / Protein-Leverage Focus"
        )
    )
)

val TIME_RESTRICTED_EATING = Diet(
    id = "time_restricted_eating_16_8",
    name = "Time-Restricted Eating (16:8)",
    status = "Established / Popular",
    primaryOutcomeDomains = listOf(
        "Physical Performance / Recovery",
        "Longevity / Healthspan",
        "Mental Energy / Fatigue Resistance"
    ),
    dietCategory = "Time-Restricted / Fasting",
    mechanismTags = listOf(
        "Circadian alignment",
        "Caloric density control"
    ),
    evidenceGrade = "B for short-to-medium-term weight and metabolic markers when calorie intake declines; C for unique benefits beyond calorie reduction in controlled trials",
    typicalParameters = DietParameters(
        eatingWindowHours = 8,
        fastingWindowHours = 16,
        rawParametersDescription = "8-hour eating window (commonly 12:00–20:00); 16-hour fast. Protein and food quality still prioritized inside the window."
    ),
    onset = "Appetite and weight shifts within 1–4 weeks for many.",
    keyLinks = listOf(
        "RCTs and meta-analyses on TRE (2020–2025)"
    ),
    safetyNotes = "Not superior to continuous energy restriction when calories are matched. Avoid if history of disordered eating. Hydration and electrolytes matter during the fast.",
    isAppSpecific = false,
    stackingNotes = listOf(
        DietStackingNote(
            title = "TRE + CCD / High Protein",
            description = "Eating window constraints remain active; ensure daily protein floor (1.6–2.2 g/kg) is met across meals inside the eating window.",
            evidenceGradeTag = "Grade B",
            relatedDietOrProtocol = "Cyber Cyclical Diet (CCD)"
        )
    )
)

val KETOGENIC_DIET = Diet(
    id = "ketogenic_diet",
    name = "Ketogenic Diet (Context-Specific)",
    status = "Established for specific uses",
    primaryOutcomeDomains = listOf(
        "Physical Performance / Recovery",
        "Mental Energy / Fatigue Resistance"
    ),
    dietCategory = "Macronutrient-Focused",
    mechanismTags = listOf(
        "Metabolic flexibility",
        "Insulin sensitivity / glycemic control"
    ),
    evidenceGrade = "A for refractory epilepsy; B for short-term weight and glycemic control; C/D for superior long-term longevity or general performance versus other calorie-matched patterns",
    typicalParameters = DietParameters(
        baselineNetCarbsGramsMax = 50,
        rawParametersDescription = "Typically <50 g net carbs/day to maintain nutritional ketosis; high fat, moderate protein."
    ),
    onset = "Ketosis in 2–7 days; weight loss front-loaded.",
    keyLinks = listOf(
        "Classic epilepsy trials",
        "Meta-analyses on weight and T2D markers"
    ),
    safetyNotes = "Monitor electrolytes, fiber, and micronutrients. Not required for the CCD. Performance may decline initially in high-intensity training.",
    isAppSpecific = false,
    stackingNotes = listOf(
        DietStackingNote(
            title = "Keto vs CCD Transition",
            description = "Unlike strict Ketogenic diets (<50g net carbs continuous), CCD incorporates periodic higher-carb refeeds to support metabolic flexibility without permanent ketosis.",
            evidenceGradeTag = "Grade B",
            relatedDietOrProtocol = "Cyber Cyclical Diet (CCD)"
        )
    )
)

val HIGH_PROTEIN_FOCUS = Diet(
    id = "high_protein_focus",
    name = "High-Protein / Protein-Leverage Focus",
    status = "Established",
    primaryOutcomeDomains = listOf(
        "Physical Performance / Recovery",
        "Mental Energy / Fatigue Resistance"
    ),
    dietCategory = "Macronutrient-Focused",
    mechanismTags = listOf(
        "Protein leverage / muscle protein synthesis"
    ),
    evidenceGrade = "A/B for muscle retention and satiety at 1.6–2.2 g/kg",
    typicalParameters = DietParameters(
        proteinGramsPerKgMin = 1.6,
        proteinGramsPerKgMax = 2.2,
        rawParametersDescription = "1.6–2.2 g protein/kg daily, distributed across meals; remaining calories from fat and carbohydrate per preference."
    ),
    onset = "Satiety within days; lean-mass effects over weeks of training.",
    keyLinks = listOf(
        "Morton et al. meta-analysis on protein and resistance training"
    ),
    safetyNotes = "Safe for healthy kidneys. Forms the protein foundation of CCD.",
    isAppSpecific = false,
    stackingNotes = listOf(
        DietStackingNote(
            title = "High Protein Foundation",
            description = "Forms the foundational protein anchor for CCD, body-repositioning protocols, and resistance training blocks.",
            evidenceGradeTag = "Grade A",
            relatedDietOrProtocol = "Cyber Cyclical Diet (CCD)"
        )
    )
)

val SEED_DIETS = listOf(
    CYBER_CYCLICAL_DIET,
    MEDITERRANEAN_DIET,
    TIME_RESTRICTED_EATING,
    KETOGENIC_DIET,
    HIGH_PROTEIN_FOCUS
)
