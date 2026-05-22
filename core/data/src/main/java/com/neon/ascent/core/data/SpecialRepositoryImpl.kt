package com.neon.ascent.core.data

import com.neon.ascent.core.data.local.dao.SpecialDao
import com.neon.ascent.core.data.local.entity.toDomain
import com.neon.ascent.core.data.local.entity.toEntity
import com.neon.ascent.core.domain.SpecialRepository
import com.neon.ascent.core.domain.model.BenchmarkTest
import com.neon.ascent.core.domain.model.SpecialAttribute
import com.neon.ascent.core.domain.model.SpecialType
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpecialRepositoryImpl @Inject constructor(
    private val dao: SpecialDao
) : SpecialRepository {

    init {
        @OptIn(DelicateCoroutinesApi::class)
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val current = dao.getAllSpecialAttributes().first()
                if (current.isEmpty()) {
                    SpecialType.entries.forEach { type ->
                        dao.upsertSpecialAttribute(
                            com.neon.ascent.core.data.local.entity.SpecialAttributeEntity(
                                type = type,
                                currentValue = 5,
                                percentile = 50,
                                totalXp = 0
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                // Log error
            }
        }
    }

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

    override suspend fun deleteBenchmarkHistory(attribute: SpecialType) {
        dao.deleteBenchmarkHistory(attribute.name)
    }

    override fun getBenchmarkHistory(attribute: SpecialType): Flow<List<BenchmarkTest>> {
        return dao.getBenchmarkHistory(attribute.name).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun resetSpecialAttributes() {
        dao.deleteAllSpecialAttributes()
        dao.deleteAllBenchmarks()
        SpecialType.entries.forEach { type ->
            dao.upsertSpecialAttribute(
                com.neon.ascent.core.data.local.entity.SpecialAttributeEntity(
                    type = type,
                    currentValue = 5,
                    percentile = 50,
                    totalXp = 0
                )
            )
        }
    }
}
