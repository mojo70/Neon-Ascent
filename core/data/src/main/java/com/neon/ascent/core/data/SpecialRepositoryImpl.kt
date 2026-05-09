package com.neon.ascent.core.data

import com.neon.ascent.core.domain.SpecialRepository
import com.neon.ascent.core.domain.model.BenchmarkTest
import com.neon.ascent.core.domain.model.SpecialAttribute
import com.neon.ascent.core.domain.model.SpecialType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

class SpecialRepositoryImpl @Inject constructor() : SpecialRepository {
    override suspend fun updateSpecialAttribute(attribute: SpecialAttribute) {
        // TODO: Implement Room persistence
    }

    override fun getSpecialAttribute(type: SpecialType): Flow<SpecialAttribute?> {
        // TODO: Implement Room persistence
        return flowOf(null)
    }

    override fun getAllSpecialAttributes(): Flow<List<SpecialAttribute>> {
        // TODO: Implement Room persistence
        return flowOf(emptyList())
    }

    override suspend fun saveBenchmark(test: BenchmarkTest) {
        // TODO: Implement Room persistence
    }

    override fun getBenchmarkHistory(attribute: SpecialType): Flow<List<BenchmarkTest>> {
        // TODO: Implement Room persistence
        return flowOf(emptyList())
    }
}
