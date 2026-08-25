package com.neon.ascent.core.domain.codex.models

import java.time.Instant

data class BiomarkerSample(
    val id: String,
    val markerKey: String,
    val displayName: String,
    val value: Double,
    val unit: String,
    val drawnAt: Instant,
    val source: String,
    val notes: String?
)

data class BiomarkerStatus(
    val latest: BiomarkerSample,
    val previous: BiomarkerSample?,
    val delta: Double?,
    val history: List<BiomarkerSample>
)

object BiomarkerKeys {
    const val TOTAL_T = "total_t"
    const val FREE_T = "free_t"
    const val SHBG = "shbg"
    const val E2 = "e2"
    const val A1C = "a1c"
    const val LDL = "ldl"
    const val HDL = "hdl"
    const val TRIG = "trig"
    const val TC = "tc"
    const val TSH = "tsh"
    const val FT3 = "ft3"
    const val FT4 = "ft4"
    const val FERRITIN = "ferritin"
    const val VIT_D = "vit_d"
    const val HSCRP = "hscrp"
    const val GLUCOSE = "glucose"
    const val ALT = "alt"
    const val AST = "ast"
    const val CREATININE = "creatinine"
    const val HGB = "hgb"
    const val WBC = "wbc"
    const val BIO_AGE = "bio_age"

    val SEED_DATA = mapOf(
        TOTAL_T to "Total Testosterone",
        FREE_T to "Free Testosterone",
        SHBG to "SHBG",
        E2 to "Estradiol",
        A1C to "HbA1c",
        LDL to "LDL Cholesterol",
        HDL to "HDL Cholesterol",
        TRIG to "Triglycerides",
        TC to "Total Cholesterol",
        TSH to "TSH",
        FT3 to "Free T3",
        FT4 to "Free T4",
        FERRITIN to "Ferritin",
        VIT_D to "Vitamin D",
        HSCRP to "hs-CRP",
        GLUCOSE to "Glucose",
        ALT to "ALT",
        AST to "AST",
        CREATININE to "Creatinine",
        HGB to "Hemoglobin",
        WBC to "WBC Count",
        BIO_AGE to "Biological Age"
    )
}
