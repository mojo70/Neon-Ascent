package com.neon.ascent.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.neon.ascent.core.data.local.converter.DataSourceConverter
import com.neon.ascent.core.data.local.converter.InstantConverter
import com.neon.ascent.core.data.local.converter.SpecialTypeConverter
import com.neon.ascent.core.data.local.converter.StringMapConverter
import com.neon.ascent.core.data.local.converter.TestTypeConverter
import com.neon.ascent.core.domain.model.BenchmarkTest
import com.neon.ascent.core.domain.model.DataSource
import com.neon.ascent.core.domain.model.SpecialType
import com.neon.ascent.core.domain.model.TestType
import java.time.Instant

@Entity(tableName = "benchmark_tests")
@TypeConverters(
    InstantConverter::class,
    SpecialTypeConverter::class,
    TestTypeConverter::class,
    DataSourceConverter::class,
    StringMapConverter::class
)
data class BenchmarkTestEntity(
    @PrimaryKey
    val id: String,

    val attribute: SpecialType,
    val testType: TestType,
    val rawScore: Double,
    val normalizedScore: Double,
    val percentile: Int?,

    val metadata: Map<String, String> = emptyMap(),   // JSON serialized
    val timestamp: Instant = Instant.now(),
    val source: DataSource
)

fun BenchmarkTestEntity.toDomain() = BenchmarkTest(
    id = id,
    attribute = attribute,
    testType = testType,
    rawScore = rawScore,
    normalizedScore = normalizedScore,
    percentile = percentile,
    metadata = metadata,
    timestamp = timestamp,
    source = source
)

fun BenchmarkTest.toEntity() = BenchmarkTestEntity(
    id = id,
    attribute = attribute,
    testType = testType,
    rawScore = rawScore,
    normalizedScore = normalizedScore,
    percentile = percentile,
    metadata = metadata,
    timestamp = timestamp,
    source = source
)
