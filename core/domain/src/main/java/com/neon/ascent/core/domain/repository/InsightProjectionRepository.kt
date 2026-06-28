package com.neon.ascent.core.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Repository for derived projections (insights, recommendations) 
 * built from biometric events and user actions.
 */
interface InsightProjectionRepository {
    fun getLatestInsight(): Flow<SocraticInsight?>
    fun getLatestRecommendation(): Flow<RecommendationProjection?>
}

data class SocraticInsight(
    val content: String,
    val sourceMetrics: List<String>,
    val timestamp: Long
)

data class RecommendationProjection(
    val content: String,
    val relatedDirectiveId: String?,
    val relatedSpecialAttribute: String?,
    val timestamp: Long
)
