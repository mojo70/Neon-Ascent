package com.neon.ascent.data.repository

import android.content.Context
import com.neon.ascent.data.local.BenchmarkDao
import com.neon.ascent.model.StrengthBenchmark
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BenchmarkRepository @Inject constructor(
    private val benchmarkDao: BenchmarkDao,
    @ApplicationContext private val context: Context
) {
    val allBenchmarks: Flow<List<StrengthBenchmark>> = benchmarkDao.getAllBenchmarks()

    suspend fun getBenchmark(sex: String, ageGroup: String, weightClass: String): StrengthBenchmark? {
        return benchmarkDao.getBenchmark(sex, ageGroup, weightClass)
    }

    suspend fun populateBenchmarksFromCsv() = withContext(Dispatchers.IO) {
        try {
            val benchmarks = mutableListOf<StrengthBenchmark>()
            context.assets.open("benchmarks.csv").use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    reader.lineSequence()
                        .filter { it.isNotBlank() }
                        .forEach { line ->
                            val parts = line.split(",")
                            if (parts.size >= 4) {
                                benchmarks.add(
                                    StrengthBenchmark(
                                        ageGroup = parts[0].trim(),
                                        sex = parts[1].trim(),
                                        weightClass = parts[2].trim(),
                                        bench1RM = parts[3].trim().toIntOrNull() ?: 0,
                                        squat1RM = parts.getOrNull(4)?.trim()?.toIntOrNull(),
                                        deadlift1RM = parts.getOrNull(5)?.trim()?.toIntOrNull()
                                    )
                                )
                            }
                        }
                }
            }
            if (benchmarks.isNotEmpty()) {
                benchmarkDao.deleteAllBenchmarks()
                benchmarkDao.insertBenchmarks(benchmarks)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
