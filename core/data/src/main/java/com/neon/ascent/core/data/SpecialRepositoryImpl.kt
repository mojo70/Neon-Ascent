package com.neon.ascent.core.data

import com.neon.ascent.core.data.local.dao.SpecialDao
import com.neon.ascent.core.data.local.entity.toDomain
import com.neon.ascent.core.data.local.entity.toEntity
import com.neon.ascent.core.domain.SpecialRepository
import com.neon.ascent.core.domain.model.BenchmarkTest
import com.neon.ascent.core.domain.model.SpecialAttribute
import com.neon.ascent.core.domain.model.SpecialType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpecialRepositoryImpl @Inject constructor(
    private val dao: SpecialDao
) : SpecialRepository {

    override suspend fun updateSpecialAttribute(attribute: SpecialAttribute) {
        dao.upsertSpecialAttribute(attribute.toEntity())
    }

    override fun getSpecialAttribute(type: SpecialType): Flow<SpecialAttribute?> {
        return dao.getSpecialAttribute(type.name).map { it?.toDomain() }
    }

    override fun getAllSpecialAttributes(): Flow<List<SpecialAttribute>> {
        return dao.getAllSpecialAttributes().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun saveBenchmark(test: BenchmarkTest) {
        dao.insertBenchmark(test.toEntity())
    }

    override fun getBenchmarkHistory(attribute: SpecialType): Flow<List<BenchmarkTest>> {
        return dao.getBenchmarkHistory(attribute.name).map { entities ->
            entities.map { it.toDomain() }
        }
    }
}
