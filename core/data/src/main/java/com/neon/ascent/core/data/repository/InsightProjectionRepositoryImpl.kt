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
        return neuralMemoryDao.getMemoriesByWing("HEALTH").map { memories ->
            val hrMemory = memories.find { it.room == "HRV_STATS" }
            if (hrMemory != null) {
                SocraticInsight(
                    content = "HRV looking strong. Recovery is optimal.",
                    sourceMetrics = listOf("HRV"),
                    timestamp = hrMemory.timestamp
                )
            } else {
                null
            }
        }
    }

    override fun getLatestRecommendation(): Flow<RecommendationProjection?> {
        return neuralMemoryDao.getMemoriesByWing("HEALTH").map { memories ->
            val hrvMemory = memories.find { it.room == "HRV_STATS" }
            if (hrvMemory != null) {
                RecommendationProjection(
                    content = "Body Battery solid. Consider adding the mobility micro-mission to today's Strength protocol.",
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
