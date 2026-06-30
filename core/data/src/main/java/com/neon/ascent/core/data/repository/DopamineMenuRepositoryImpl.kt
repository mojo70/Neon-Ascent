package com.neon.ascent.core.data.repository

import com.neon.ascent.core.data.local.dao.DopamineMenuDao
import com.neon.ascent.core.data.local.entity.toDomain
import com.neon.ascent.core.data.local.entity.toEntity
import com.neon.ascent.core.domain.model.DopamineCategory
import com.neon.ascent.core.domain.model.DopamineMenuItem
import com.neon.ascent.core.domain.model.EnergyLevel
import com.neon.ascent.core.domain.model.SpecialType
import com.neon.ascent.core.domain.repository.DopamineMenuRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DopamineMenuRepositoryImpl @Inject constructor(
    private val dopamineMenuDao: DopamineMenuDao
) : DopamineMenuRepository {

    override fun getAllItems(): Flow<List<DopamineMenuItem>> {
        return dopamineMenuDao.getAllItems().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getItemsByEnergyLevel(energyLevel: EnergyLevel): Flow<List<DopamineMenuItem>> {
        return dopamineMenuDao.getItemsByEnergyLevel(energyLevel).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getItemById(id: String): DopamineMenuItem? {
        return dopamineMenuDao.getItemById(id)?.toDomain()
    }

    override suspend fun upsertItem(item: DopamineMenuItem) {
        dopamineMenuDao.upsertItem(item.toEntity())
    }

    override suspend fun deleteItem(item: DopamineMenuItem) {
        dopamineMenuDao.deleteItem(item.toEntity())
    }

    override suspend fun logUsage(id: String, timestamp: Instant) {
        dopamineMenuDao.logUsage(id, timestamp)
    }

    override suspend fun seedDefaultMenu() {
        if (dopamineMenuDao.getItemCount() > 0) return

        val defaultItems = listOf(
            DopamineMenuItem(
                id = UUID.randomUUID().toString(),
                title = "Cold Water Face Splash",
                description = "Shock the vagus nerve with 30s of cold water for immediate neural reset.",
                durationMinutes = 1,
                category = DopamineCategory.RESET,
                specialTags = listOf(SpecialType.PERCEPTION),
                energyLevel = EnergyLevel.LOW
            ),
            DopamineMenuItem(
                id = UUID.randomUUID().toString(),
                title = "Box Breathing (4-4-4-4)",
                description = "Standard tactical breathing for autonomic nervous system regulation.",
                durationMinutes = 4,
                category = DopamineCategory.RESET,
                specialTags = listOf(SpecialType.INTELLIGENCE),
                energyLevel = EnergyLevel.LOW
            ),
            DopamineMenuItem(
                id = UUID.randomUUID().toString(),
                title = "Neon District Power Walk",
                description = "Zone 2 aerobic effort. Clear metabolic waste and boost endorphins.",
                durationMinutes = 15,
                category = DopamineCategory.MOVEMENT,
                specialTags = listOf(SpecialType.ENDURANCE),
                energyLevel = EnergyLevel.MEDIUM
            ),
            DopamineMenuItem(
                id = UUID.randomUUID().toString(),
                title = "Direct Sunlight Exposure",
                description = "Synchronize circadian oscillators with 5-10m of morning photons.",
                durationMinutes = 5,
                category = DopamineCategory.SENSORY,
                specialTags = listOf(SpecialType.PERCEPTION),
                energyLevel = EnergyLevel.LOW
            ),
            DopamineMenuItem(
                id = UUID.randomUUID().toString(),
                title = "High-Intensity Swings",
                description = "Explosive movement to spike testosterone and growth hormone.",
                durationMinutes = 5,
                category = DopamineCategory.MOVEMENT,
                specialTags = listOf(SpecialType.STRENGTH, SpecialType.AGILITY),
                energyLevel = EnergyLevel.HIGH
            ),
            DopamineMenuItem(
                id = UUID.randomUUID().toString(),
                title = "Neural Dump Journaling",
                description = "Externalize cognitive load. 5 minutes of stream-of-consciousness writing.",
                durationMinutes = 5,
                category = DopamineCategory.CREATIVE,
                specialTags = listOf(SpecialType.INTELLIGENCE),
                energyLevel = EnergyLevel.MEDIUM
            ),
            DopamineMenuItem(
                id = UUID.randomUUID().toString(),
                title = "Vocal Toning / Humming",
                description = "Stimulate the vagus nerve through mechanical vibration in the throat.",
                durationMinutes = 2,
                category = DopamineCategory.SENSORY,
                specialTags = listOf(SpecialType.CHARISMA),
                energyLevel = EnergyLevel.LOW
            ),
            DopamineMenuItem(
                id = UUID.randomUUID().toString(),
                title = "Zero-Out Workspace",
                description = "Minimize visual noise. Return all objects to their primary nodes.",
                durationMinutes = 5,
                category = DopamineCategory.PRODUCTIVE,
                specialTags = listOf(SpecialType.AGILITY, SpecialType.PERCEPTION),
                energyLevel = EnergyLevel.MEDIUM
            ),
            DopamineMenuItem(
                id = UUID.randomUUID().toString(),
                title = "Social Uplink (Text/Call)",
                description = "Ping a high-affinity node in your social network.",
                durationMinutes = 3,
                category = DopamineCategory.SOCIAL,
                specialTags = listOf(SpecialType.CHARISMA, SpecialType.LUCK),
                energyLevel = EnergyLevel.LOW
            ),
            DopamineMenuItem(
                id = UUID.randomUUID().toString(),
                title = "Red Light Photobiomodulation",
                description = "Enhance mitochondrial function and cellular recovery.",
                durationMinutes = 10,
                category = DopamineCategory.SENSORY,
                specialTags = listOf(SpecialType.ENDURANCE),
                energyLevel = EnergyLevel.LOW
            ),
            DopamineMenuItem(
                id = UUID.randomUUID().toString(),
                title = "Grip Strength Protocol",
                description = "Crush-grip intervals. High correlation with longevity and CNS health.",
                durationMinutes = 3,
                category = DopamineCategory.MOVEMENT,
                specialTags = listOf(SpecialType.STRENGTH),
                energyLevel = EnergyLevel.MEDIUM
            ),
            DopamineMenuItem(
                id = UUID.randomUUID().toString(),
                title = "System Maintenance (Plant Care)",
                description = "Nurture organic life. Lower cortisol through biophilic interaction.",
                durationMinutes = 5,
                category = DopamineCategory.CREATIVE,
                specialTags = listOf(SpecialType.PERCEPTION, SpecialType.LUCK),
                energyLevel = EnergyLevel.LOW
            )
        )

        defaultItems.forEach { upsertItem(it) }
    }
}
