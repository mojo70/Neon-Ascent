package com.neon.ascent.core.data.repository

import com.neon.ascent.core.data.local.dao.SanctuaryDao
import com.neon.ascent.core.data.local.entity.SanctuaryPassageEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeSanctuaryDao : SanctuaryDao {
    private val passages = mutableMapOf<String, SanctuaryPassageEntity>()

    override fun getAllPassages(): Flow<List<SanctuaryPassageEntity>> =
        flowOf(passages.values.sortedBy { it.sortOrder })

    override suspend fun getPassageById(id: String): SanctuaryPassageEntity? = passages[id]

    override fun getPassagesByTheme(theme: String): Flow<List<SanctuaryPassageEntity>> =
        flowOf(passages.values.filter { p -> p.themes.any { it.equals(theme, ignoreCase = true) } }.sortedBy { it.sortOrder })

    override fun getPassagesByTag(tag: String): Flow<List<SanctuaryPassageEntity>> =
        flowOf(passages.values.filter { p -> p.tags.any { it.equals(tag, ignoreCase = true) } }.sortedBy { it.sortOrder })

    override suspend fun getPassageCount(): Int = passages.size

    override suspend fun insertPassagesIfAbsent(passages: List<SanctuaryPassageEntity>) {
        passages.forEach { if (!this.passages.containsKey(it.id)) this.passages[it.id] = it }
    }

    override suspend fun insertOrUpdatePassages(passages: List<SanctuaryPassageEntity>) {
        passages.forEach { this.passages[it.id] = it }
    }
}

class SanctuaryRepositoryTest {

    @Test
    fun observeAllPassages_returnsSeedPassagesWhenEmpty() = runBlocking {
        val dao = FakeSanctuaryDao()
        val repository = SanctuaryRepositoryImpl(dao)

        val passages = repository.observeAllPassages().first()
        assertEquals(16, passages.size)
        assertEquals("psalm_23", passages[0].id)
    }

    @Test
    fun seedDefaultPassagesIfAbsent_populatesDao() = runBlocking {
        val dao = FakeSanctuaryDao()
        val repository = SanctuaryRepositoryImpl(dao)

        repository.seedDefaultPassagesIfAbsent()
        assertEquals(16, dao.getPassageCount())
    }

    @Test
    fun getPassageById_returnsCorrectPassage() = runBlocking {
        val dao = FakeSanctuaryDao()
        val repository = SanctuaryRepositoryImpl(dao)

        val passage = repository.getPassageById("psalm_91")
        assertNotNull(passage)
        assertEquals("Psalm 91", passage?.reference)
        assertEquals("My Refuge and My Fortress", passage?.title)
    }

    @Test
    fun observePassagesByTheme_filtersCorrectly() = runBlocking {
        val dao = FakeSanctuaryDao()
        val repository = SanctuaryRepositoryImpl(dao)
        repository.seedDefaultPassagesIfAbsent()

        val warfarePassages = repository.observePassagesByTheme("warfare").first()
        assertTrue(warfarePassages.any { it.id == "eph_6_10_18" })
    }

    @Test
    fun observePassagesByTag_filtersCorrectly() = runBlocking {
        val dao = FakeSanctuaryDao()
        val repository = SanctuaryRepositoryImpl(dao)
        repository.seedDefaultPassagesIfAbsent()

        val priorityPassages = repository.observePassagesByTag("biggs-priority").first()
        assertEquals(3, priorityPassages.size)
        assertTrue(priorityPassages.all { it.tags.contains("biggs-priority") })
    }
}
