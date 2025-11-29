package com.neon.ascent.model

data class StrengthBenchmark(
    val ageGroup: String,
    val sex: String,
    val weightClass: String,
    val bench1RM: Int, // e.g., IPF standards
    // Add squat, deadlift from USAPL/IPF data
)

val IPF_BENCHMARKS = listOf(
    StrengthBenchmark("18-39", "Male", "59kg", 100),
    // Full table: Pull from CSV later. Realistic 1-10 scale: 1=below avg, 10=world record
    // E.g., Score = (user1RM / worldRecord) * 10, clamped 1-10
)
