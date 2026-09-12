package com.neon.ascent.data.repository

import com.neon.ascent.core.domain.character.repository.CharacterRepository
import com.neon.ascent.data.local.BiohackingDao
import com.neon.ascent.model.BioAgeResult
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BioAgePredictor @Inject constructor(
    private val bioAgeRepository: BioAgeRepository,
    private val biohackingDao: BiohackingDao,
    private val characterRepository: CharacterRepository
) {
    val lastResultFlow: Flow<BioAgeResult?> = combine(
        biohackingDao.getBiohackingData(0),
        characterRepository.getUserCharacter()
    ) { data, char ->
        val jsonStr = data?.extractedBiomarkersJson
        if (jsonStr != null) {
            val biomarkers = Json.decodeFromString<Map<String, Float>>(jsonStr)
            val result = bioAgeRepository.predictBiologicalAge(biomarkers)
            val chronoAge = calculateAge(char?.dob ?: "2000.01.01")
            result.copy(ageGap = result.biologicalAge - chronoAge)
        } else {
            null
        }
    }

    suspend fun getLastResult(): BioAgeResult? = lastResultFlow.firstOrNull()

    // To match the user's expected snippet for chronological age comparison
    suspend fun getChronologicalAge(): Int {
        val char = characterRepository.getUserCharacter().firstOrNull()
        return calculateAge(char?.dob ?: "2000.01.01")
    }

    private fun calculateAge(dob: String): Int {
        return try {
            val formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")
            val birthDate = LocalDate.parse(dob, formatter)
            val currentDate = LocalDate.now()
            Period.between(birthDate, currentDate).years
        } catch (e: Exception) {
            0
        }
    }
}
