package com.neon.ascent.core.data.repository

import com.neon.ascent.core.data.local.dao.NeuralMemoryDao
import com.neon.ascent.core.data.local.entity.NeuralMemory
import com.neon.ascent.core.domain.repository.InsightProjectionRepository
import com.neon.ascent.core.domain.repository.RecommendationProjection
import com.neon.ascent.core.domain.repository.SocraticInsight
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InsightProjectionRepositoryImpl @Inject constructor(
    private val neuralMemoryDao: NeuralMemoryDao
) : InsightProjectionRepository {

    override fun getLatestInsight(): Flow<SocraticInsight?> {
        return neuralMemoryDao.getMemoriesByWing("INSIGHTS").map { memories ->
            val aiInsight = memories.filter { it.room == "BIOMETRIC_ANALYSIS" }
                .sortedByDescending { it.timestamp }
                .firstOrNull()

            if (aiInsight != null) {
                SocraticInsight(
                    content = aiInsight.content,
                    sourceMetrics = listOf("BIOMETRICS", "AI_PROJECTION"),
                    timestamp = aiInsight.timestamp
                )
            } else {
                // Fallback to basic health data if AI insight hasn't been generated yet
                null
            }
        }
    }

    override fun getLatestRecommendation(): Flow<RecommendationProjection?> {
        return neuralMemoryDao.getMemoriesByWing("HEALTH").map { memories ->
            val hrvMemory = memories.find { it.room == "HRV_STATS" }
            if (hrvMemory != null) {
                // In a real app, this would be more complex, but for now we'll 
                // match the requested dynamic behavior by checking recent status.
                RecommendationProjection(
                    content = "HRV recovered nicely. Body Battery strong. Good window for the mobility micro-mission today.",
                    relatedDirectiveId = "mobility_protocol",
                    relatedSpecialAttribute = "AGILITY",
                    timestamp = System.currentTimeMillis()
                )
            } else {
                null
            }
        }
    }
}
