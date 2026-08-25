package com.neon.ascent.core.domain.repository

import com.neon.ascent.core.domain.codex.models.BiomarkerSample
import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface BiomarkerRepository {
    suspend fun saveSample(sample: BiomarkerSample)
    suspend fun deleteSample(id: String)
    fun getSamplesForMarker(markerKey: String): Flow<List<BiomarkerSample>>
    fun getAllSamplesBetween(from: Instant, to: Instant): Flow<List<BiomarkerSample>>
    fun getLatestPerMarker(): Flow<List<BiomarkerSample>>
}
