package com.neon.ascent.core.data.repository

import com.neon.ascent.core.data.local.dao.InsightDao
import com.neon.ascent.core.data.local.entity.SocraticInsightEntity
import com.neon.ascent.core.domain.repository.InsightProjectionRepository
import com.neon.ascent.core.domain.repository.RecommendationProjection
import com.neon.ascent.core.domain.repository.SocraticInsight
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InsightProjectionRepositoryImpl @Inject constructor(
    private val insightDao: InsightDao
) : InsightProjectionRepository {

    override fun getLatestInsight(): Flow<SocraticInsight?> {
        return insightDao.getLatestInsight().map { entity ->
            entity?.toDomain()
        }
    }

    override fun getLatestRecommendation(): Flow<RecommendationProjection?> {
        // Implementation for materializing recommendations could be added here
        // or handled by a similar processor. For now, we'll return null or 
        // a basic materialized recommendation if available in a separate table.
        return kotlinx.coroutines.flow.flowOf(null)
    }
}

fun SocraticInsightEntity.toDomain() = SocraticInsight(
    content = content,
    sourceMetrics = listOf("BIOMETRICS", "ACTIONS"), // Can be derived from basedOnEventIds if needed
    timestamp = generatedAt.toEpochMilli()
)
