package com.neon.ascent.core.data.repository

import com.neon.ascent.core.data.local.dao.ReconstitutionDao
import com.neon.ascent.core.data.local.entity.SavedReconstitutionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ReconstitutionRepositoryTest {

    private class FakeReconstitutionDao : ReconstitutionDao {
        private val mixes = mutableListOf<SavedReconstitutionEntity>()
        private val flow = MutableStateFlow<List<SavedReconstitutionEntity>>(emptyList())

        private fun updateFlow() {
            flow.value = mixes.toList().sortedByDescending { it.createdAt }
        }

        override fun getAllSavedMixes(): Flow<List<SavedReconstitutionEntity>> = flow

        override suspend fun getSavedMixById(id: String): SavedReconstitutionEntity? {
            return mixes.find { it.id == id }
        }

        override suspend fun insertSavedMix(mix: SavedReconstitutionEntity) {
            mixes.removeAll { it.id == mix.id }
            mixes.add(mix)
            updateFlow()
        }

        override suspend fun deleteSavedMix(id: String) {
            mixes.removeAll { it.id == id }
            updateFlow()
        }
    }

    private lateinit var fakeDao: FakeReconstitutionDao
    private lateinit var repository: ReconstitutionRepository

    @Before
    fun setUp() {
        fakeDao = FakeReconstitutionDao()
        repository = ReconstitutionRepository(fakeDao)
    }

    @Test
    fun `test save and pre-fill roundtrip`() = runBlocking {
        val mix = SavedReconstitutionEntity(
            id = "test_mix_1",
            name = "BPC-157 Standard",
            vialMg = "10",
            waterMl = "2",
            concentrationMgPerMl = "5",
            mixedDate = "2026-10-11",
            notes = "Reconstituted with BAC water",
            componentsJson = "BPC-157:5|TB-500:5"
        )

        repository.saveMix(mix)

        val list = repository.savedMixes.first()
        assertEquals(1, list.size)

        val saved = list[0]
        assertEquals("test_mix_1", saved.id)
        assertEquals("BPC-157 Standard", saved.name)
        assertEquals("10", saved.vialMg)
        assertEquals("2", saved.waterMl)
        assertEquals("5", saved.concentrationMgPerMl)
        assertEquals("2026-10-11", saved.mixedDate)
        assertEquals("Reconstituted with BAC water", saved.notes)
        assertEquals("BPC-157:5|TB-500:5", saved.componentsJson)
    }

    @Test
    fun `test delete saved mix`() = runBlocking {
        val mix = SavedReconstitutionEntity(
            id = "test_mix_delete",
            name = "To Delete",
            vialMg = "5",
            waterMl = "1",
            concentrationMgPerMl = "5"
        )

        repository.saveMix(mix)

        var list = repository.savedMixes.first()
        assertEquals(1, list.size)

        repository.deleteMix("test_mix_delete")

        list = repository.savedMixes.first()
        assertTrue(list.isEmpty())
    }
}
