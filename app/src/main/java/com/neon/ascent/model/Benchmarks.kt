package com.neon.ascent.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "strength_benchmarks")
data class StrengthBenchmark(
    val ageGroup: String,
    val sex: String,
    val weightClass: String,
    val bench1RM: Int, // e.g., IPF standards
    val squat1RM: Int? = null,
    val deadlift1RM: Int? = null,
    @PrimaryKey(autoGenerate = true) val id: Int = 0
)

val IPF_BENCHMARKS = listOf(
    StrengthBenchmark("18-39", "Male", "59kg", 100),
    // Full table: Pull from CSV later. Realistic 1-10 scale: 1=below avg, 10=world record
    // E.g., Score = (user1RM / worldRecord) * 10, clamped 1-10
)
