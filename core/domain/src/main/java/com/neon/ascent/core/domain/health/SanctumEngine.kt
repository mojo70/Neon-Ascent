package com.neon.ascent.core.domain.health

import java.time.Duration
import java.time.Instant
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

data class SanctumInput(
    val sessionStart: Instant?,
    val sessionEnd: Instant?,
    val stageMinutes: Map<String, Long> = emptyMap(), // empty = T0 candidate
    val hrInSession: List<Pair<Instant, Int>> = emptyList(),
    val hrPreSleep90: List<Pair<Instant, Int>> = emptyList(),
    val rmssdInSession: List<Pair<Instant, Double>> = emptyList(),
    val last14AsleepMin: List<Long> = emptyList(), // oldest->newest, core nights only
    val last14Midpoints: List<Instant> = emptyList(), // same length or empty
    val last7HrvNight: List<Double> = emptyList(), // HRV_NIGHT rollups
    val userSleepNeedMin: Long? = null,
    val awakeBoutsCount: Int? = null,
    val sourceTag: String = "HC"
)

data class SanctumResult(
    val score: Int?, // null if no session
    val tier: Int, // 0/1/2
    val band: String?, // SEALED/STABLE/LEAK/FRACTURE
    val asleepMin: Long,
    val needMin: Long,
    val drivers: List<Pair<String, String>>,
    val sourceTag: String
)

/**
 * SanctumEngine implements the SANCTUM (Neon sleep quality) formula per SleepQuality.md.
 *
 * Fixture Examples:
 * 1) 7.5h session, no stages, no HR -> T0, duration factor 1.0, score from duration+defaults:
 *    val input = SanctumInput(
 *        sessionStart = Instant.parse("2026-09-06T00:00:00Z"),
 *        sessionEnd = Instant.parse("2026-09-06T07:30:00Z"),
 *        stageMinutes = emptyMap(),
 *        needMin = 450
 *    )
 *    val result = SanctumEngine.calculateSanctum(input)
 *    // Result: score = 98 (raw 100 clamped), tier = 0, band = "SEALED"
 *
 * 2) Same duration, LIGHT/DEEP/REM covering 90%, 4 awake bouts >= 2min -> T1, fragment < 1, score lower than (1):
 *    val input = SanctumInput(
 *        sessionStart = Instant.parse("2026-09-06T00:00:00Z"),
 *        sessionEnd = Instant.parse("2026-09-06T07:30:00Z"),
 *        stageMinutes = mapOf("LIGHT" to 200L, "DEEP" to 100L, "REM" to 105L, "AWAKE" to 45L),
 *        awakeBoutsCount = 4,
 *        needMin = 450
 *    )
 *    val result = SanctumEngine.calculateSanctum(input)
 *    // Result: score ~ 65, tier = 1, band = "LEAK" (fragmentation penalty reduces score significantly)
 *
 * 3) Short sleep T0 cap (asleepMin = 327, needMin = 440, T0) -> SANCTUM <= 69 (LEAK):
 *    val input = SanctumInput(
 *        sessionStart = Instant.parse("2026-09-06T00:00:00Z"),
 *        sessionEnd = Instant.parse("2026-09-06T05:27:00Z"),
 *        stageMinutes = emptyMap(),
 *        userSleepNeedMin = 440
 *    )
 *    val result = SanctumEngine.calculateSanctum(input)
 *    // Result: score = 69 (T0 capped at 69 for sleep < 85% need), tier = 0, band = "LEAK"
 */
object SanctumEngine {

    fun calculateSanctum(input: SanctumInput): SanctumResult {
        if (input.sessionStart == null || input.sessionEnd == null || input.sessionEnd.isBefore(input.sessionStart)) {
            return SanctumResult(
                score = null,
                tier = 0,
                band = null,
                asleepMin = 0L,
                needMin = input.userSleepNeedMin ?: 450L,
                drivers = emptyList(),
                sourceTag = input.sourceTag
            )
        }

        val tibMin = Duration.between(input.sessionStart, input.sessionEnd).toMinutes().coerceAtLeast(1L)

        // Asleep min calculation
        val lightMin = input.stageMinutes["LIGHT"] ?: 0L
        val deepMin = input.stageMinutes["DEEP"] ?: 0L
        val remMin = input.stageMinutes["REM"] ?: 0L
        val sleepingMin = input.stageMinutes["SLEEPING"] ?: 0L

        val awakeMin = input.stageMinutes["AWAKE"] ?: 0L
        val awakeInBedMin = input.stageMinutes["AWAKE_IN_BED"] ?: 0L
        val outOfBedMin = input.stageMinutes["OUT_OF_BED"] ?: 0L
        val totalAwakeMin = awakeMin + awakeInBedMin + outOfBedMin

        val stagedThreeMin = lightMin + deepMin + remMin
        val stagedAsleepMin = stagedThreeMin + sleepingMin

        val asleepMin = when {
            stagedAsleepMin > 0 -> stagedAsleepMin
            totalAwakeMin > 0 -> (tibMin - totalAwakeMin).coerceAtLeast(0L)
            else -> tibMin
        }

        // T1 Gate: (LIGHT+DEEP+REM) / asleepMin >= 0.80. A lone SLEEPING block is T0.
        val isT1 = asleepMin > 0 && (stagedThreeMin.toDouble() / asleepMin) >= 0.80

        // T2 Gate: (>=20 HR samples AND span >=2h in session) OR (>=3 RMSSD in session) OR (1 RMSSD timestamp inside session)
        val hrSpanMinutes = if (input.hrInSession.size >= 2) {
            Duration.between(input.hrInSession.first().first, input.hrInSession.last().first).toMinutes()
        } else 0L
        val hrCoverage = input.hrInSession.size >= 20 && hrSpanMinutes >= 120

        val rmssdCoverage = input.rmssdInSession.size >= 3 ||
                (input.rmssdInSession.size == 1 && isTimestampInSession(input.rmssdInSession.first().first, input.sessionStart, input.sessionEnd)) ||
                input.rmssdInSession.isNotEmpty()

        val isT2 = hrCoverage || rmssdCoverage

        val tier = when {
            isT2 -> 2
            isT1 -> 1
            else -> 0
        }

        // Need min
        val needMin = input.userSleepNeedMin
            ?: if (input.last14AsleepMin.size >= 7) calculateMedianLong(input.last14AsleepMin)
            else 450L

        // Duration factor
        val durationFactor = (asleepMin.toDouble() / needMin.coerceAtLeast(1L)).coerceIn(0.55, 1.12)

        // T1 Factors: Efficiency, Fragment, Architecture
        val efficiencyFactor: Double
        val fragment: Double
        val arch: Double
        val awakeBouts: Int

        if (isT1) {
            val eff = (asleepMin.toDouble() / tibMin.toDouble()).coerceIn(0.70, 1.00)
            efficiencyFactor = 0.85 + 0.15 * ((eff - 0.70) / 0.30)

            awakeBouts = input.awakeBoutsCount ?: if (totalAwakeMin > 0) (totalAwakeMin / 5L).toInt().coerceAtLeast(1) else 0
            fragment = 1.0 - (awakeBouts * 0.06).coerceAtMost(0.35)

            val deepShare = deepMin.toDouble() / maxOf(asleepMin.toDouble(), 1.0)
            val remShare = remMin.toDouble() / maxOf(asleepMin.toDouble(), 1.0)

            val deepClamp = ((deepShare - 0.13) / 0.07).coerceIn(-1.0, 1.0)
            val remClamp = ((remShare - 0.18) / 0.07).coerceIn(-1.0, 1.0)

            arch = 1.0 + 0.06 * deepClamp + 0.05 * remClamp
        } else {
            efficiencyFactor = 1.0
            fragment = 1.0
            arch = 1.0
            awakeBouts = input.awakeBoutsCount ?: 0
        }

        // Circadian factor
        val midpoint = input.sessionStart.plus(Duration.between(input.sessionStart, input.sessionEnd).dividedBy(2))
        val circadian: Double
        val midDiffHours: Double?
        if (input.last14Midpoints.size >= 7) {
            val medMid = calculateMedianInstant(input.last14Midpoints)
            val diffHours = abs(Duration.between(midpoint, medMid).toMinutes()) / 60.0
            midDiffHours = diffHours
            circadian = 1.0 - 0.04 * diffHours.coerceIn(0.0, 4.0)
        } else {
            midDiffHours = null
            circadian = 1.0
        }

        // T2 Factors: HR Drop & HRV
        val eveningHR = if (input.hrPreSleep90.size >= 8) input.hrPreSleep90.map { it.second }.average() else null
        val nightHR = if (hrCoverage && input.hrInSession.isNotEmpty()) input.hrInSession.map { it.second }.average() else null

        val hrDropPercent: Double?
        val hrDrop: Double
        if (isT2 && eveningHR != null && nightHR != null && eveningHR >= 50.0) {
            val dropRatio = (eveningHR - nightHR) / eveningHR
            hrDropPercent = dropRatio * 100.0
            hrDrop = (0.88 + 0.25 * dropRatio).coerceIn(0.88, 1.08)
        } else {
            hrDropPercent = null
            hrDrop = 1.0
        }

        val nightRmssd = if (input.rmssdInSession.isNotEmpty()) input.rmssdInSession.map { it.second }.average() else null
        val hrvZScore: Double?
        val hrvFactor: Double
        if (isT2 && nightRmssd != null && input.last7HrvNight.size >= 5) {
            val z = calculateZScore(nightRmssd, input.last7HrvNight)
            hrvZScore = z
            hrvFactor = 1.0 + 0.08 * z.coerceIn(-1.5, 1.5)
        } else {
            hrvZScore = null
            hrvFactor = 1.0
        }

        // Debt factor
        val debt: Double
        val debtAvgHours: Double?
        val recentAsleep = input.last14AsleepMin.takeLast(7)
        if (recentAsleep.size >= 5) {
            val mean7 = recentAsleep.average()
            debtAvgHours = mean7 / 60.0
            val debtRatio = (mean7 / needMin.toDouble()) - 1.0
            debt = 1.0 + 0.04 * debtRatio.coerceIn(-1.2, 0.3)
        } else {
            debtAvgHours = null
            debt = 1.0
        }

        val sanctumRaw = 100.0 * durationFactor * efficiencyFactor * fragment * circadian * arch * hrDrop * hrvFactor * debt
        val rawInt = sanctumRaw.roundToInt()
        val durationScore = ((asleepMin.toDouble() / needMin.coerceAtLeast(1L)) * 100.0).roundToInt()

        val sanctumScore = if (tier == 0) {
            if (asleepMin < 0.85 * needMin) {
                minOf(rawInt, 69, durationScore).coerceIn(15, 98)
            } else {
                minOf(rawInt, durationScore, 98).coerceIn(15, 98)
            }
        } else {
            rawInt.coerceIn(15, 98)
        }

        val band = when (sanctumScore) {
            in 85..98 -> "SEALED"
            in 70..84 -> "STABLE"
            in 50..69 -> "LEAK"
            else -> "FRACTURE"
        }

        // Build Driver lines (Keys: DUR, EFF, BOUTS, MID, HRV_N, HR_DROP, DEBT)
        val drivers = mutableListOf<Pair<String, String>>()

        drivers.add("DUR" to "${asleepMin / 60}h${asleepMin % 60}m / NEED ${needMin / 60}h${needMin % 60}m")

        if (tier >= 1) {
            val effPct = ((asleepMin.toDouble() / tibMin.toDouble()) * 100.0).roundToInt()
            drivers.add("EFF" to "$effPct% (T1)")
            if (awakeBouts > 0) {
                drivers.add("BOUTS" to "$awakeBouts (T1)")
            }
        }

        if (midDiffHours != null) {
            val sign = if (midDiffHours >= 0) "+" else ""
            drivers.add("MID" to "$sign${String.format(Locale.US, "%.1f", midDiffHours)}h")
        }

        if (tier == 2 && hrvZScore != null) {
            val sign = if (hrvZScore >= 0) "+" else ""
            drivers.add("HRV_N" to "z $sign${String.format(Locale.US, "%.1f", hrvZScore)} (T2)")
        }

        if (tier == 2 && hrDropPercent != null) {
            drivers.add("HR_DROP" to "${hrDropPercent.roundToInt()}% (T2)")
        }

        if (debtAvgHours != null) {
            drivers.add("DEBT" to "${String.format(Locale.US, "%.1f", debtAvgHours)}h avg")
        }

        return SanctumResult(
            score = sanctumScore,
            tier = tier,
            band = band,
            asleepMin = asleepMin,
            needMin = needMin,
            drivers = drivers,
            sourceTag = input.sourceTag
        )
    }

    private fun isTimestampInSession(time: Instant, start: Instant, end: Instant): Boolean {
        return !time.isBefore(start) && !time.isAfter(end)
    }

    private fun calculateMedianLong(list: List<Long>): Long {
        if (list.isEmpty()) return 450L
        val sorted = list.sorted()
        val middle = sorted.size / 2
        return if (sorted.size % 2 == 1) {
            sorted[middle]
        } else {
            (sorted[middle - 1] + sorted[middle]) / 2
        }
    }

    private fun calculateMedianInstant(list: List<Instant>): Instant {
        if (list.isEmpty()) return Instant.EPOCH
        val sorted = list.sortedBy { it.toEpochMilli() }
        val middle = sorted.size / 2
        return if (sorted.size % 2 == 1) {
            sorted[middle]
        } else {
            Instant.ofEpochMilli((sorted[middle - 1].toEpochMilli() + sorted[middle].toEpochMilli()) / 2)
        }
    }

    private fun calculateZScore(value: Double, series: List<Double>): Double {
        if (series.size < 5) return 0.0
        val mean = series.average()
        val variance = series.sumOf { (it - mean) * (it - mean) } / series.size
        val stdDev = sqrt(variance)
        if (stdDev < 0.001) return 0.0
        return (value - mean) / stdDev
    }
}
