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

    data class ReferenceRange(
        val min: Double?,
        val max: Double?,
        val unit: String,
        val displayText: String
    )

    val REFERENCE_RANGES: Map<String, ReferenceRange> = mapOf(
        TOTAL_T to ReferenceRange(300.0, 1000.0, "ng/dL", "300 – 1000 ng/dL"),
        FREE_T to ReferenceRange(5.0, 21.0, "pg/mL", "5.0 – 21.0 pg/mL"),
        SHBG to ReferenceRange(10.0, 50.0, "nmol/L", "10 – 50 nmol/L"),
        E2 to ReferenceRange(10.0, 40.0, "pg/mL", "10 – 40 pg/mL"),
        A1C to ReferenceRange(null, 5.7, "%", "< 5.7 %"),
        LDL to ReferenceRange(null, 100.0, "mg/dL", "< 100 mg/dL"),
        HDL to ReferenceRange(40.0, null, "mg/dL", "> 40 mg/dL"),
        TRIG to ReferenceRange(null, 150.0, "mg/dL", "< 150 mg/dL"),
        TC to ReferenceRange(null, 200.0, "mg/dL", "< 200 mg/dL"),
        TSH to ReferenceRange(0.4, 4.0, "mIU/L", "0.4 – 4.0 mIU/L"),
        FT3 to ReferenceRange(2.0, 4.4, "pg/mL", "2.0 – 4.4 pg/mL"),
        FT4 to ReferenceRange(0.8, 1.8, "ng/dL", "0.8 – 1.8 ng/dL"),
        FERRITIN to ReferenceRange(30.0, 400.0, "ng/mL", "30 – 400 ng/mL"),
        VIT_D to ReferenceRange(30.0, 100.0, "ng/mL", "30 – 100 ng/mL"),
        HSCRP to ReferenceRange(null, 1.0, "mg/L", "< 1.0 mg/L"),
        GLUCOSE to ReferenceRange(70.0, 99.0, "mg/dL", "70 – 99 mg/dL"),
        ALT to ReferenceRange(7.0, 56.0, "U/L", "7 – 56 U/L"),
        AST to ReferenceRange(10.0, 40.0, "U/L", "10 – 40 U/L"),
        CREATININE to ReferenceRange(0.7, 1.3, "mg/dL", "0.7 – 1.3 mg/dL"),
        HGB to ReferenceRange(13.8, 17.2, "g/dL", "13.8 – 17.2 g/dL"),
        WBC to ReferenceRange(4.5, 11.0, "k/uL", "4.5 – 11.0 k/uL")
    )
}
