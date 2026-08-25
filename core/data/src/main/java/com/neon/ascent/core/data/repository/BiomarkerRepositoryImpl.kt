package com.neon.ascent.core.data.repository

import com.neon.ascent.core.data.local.dao.BiomarkerDao
import com.neon.ascent.core.data.mapper.toDomain
import com.neon.ascent.core.data.mapper.toEntity
import com.neon.ascent.core.domain.codex.models.BiomarkerSample
import com.neon.ascent.core.domain.repository.BiomarkerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BiomarkerRepositoryImpl @Inject constructor(
    private val biomarkerDao: BiomarkerDao
) : BiomarkerRepository {

    override suspend fun saveSample(sample: BiomarkerSample) {
        biomarkerDao.upsertSample(sample.toEntity())
    }

    override suspend fun deleteSample(id: String) {
        biomarkerDao.deleteSample(id)
    }

    override fun getSamplesForMarker(markerKey: String): Flow<List<BiomarkerSample>> =
        biomarkerDao.getSamplesForMarker(markerKey).map { entities ->
            entities.map { it.toDomain() }
        }

    override fun getAllSamplesBetween(from: Instant, to: Instant): Flow<List<BiomarkerSample>> =
        biomarkerDao.getAllSamplesBetween(from, to).map { entities ->
            entities.map { it.toDomain() }
        }

    override fun getLatestPerMarker(): Flow<List<BiomarkerSample>> =
        biomarkerDao.getLatestPerMarker().map { entities ->
            entities.map { it.toDomain() }
        }
}
