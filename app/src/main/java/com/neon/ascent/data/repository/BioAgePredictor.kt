package com.neon.ascent.data.repository

import com.neon.ascent.data.local.BiohackingDao
import com.neon.ascent.model.BioAgeResult
import com.neon.ascent.model.UserCharacter
import com.neon.ascent.data.local.UserCharacterDao
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BioAgePredictor @Inject constructor(
    private val bioAgeRepository: BioAgeRepository,
    private val biohackingDao: BiohackingDao,
    private val userCharacterDao: UserCharacterDao
) {
    val lastResultFlow: Flow<BioAgeResult?> = combine(
        biohackingDao.getBiohackingData(0),
        userCharacterDao.getUserCharacter()
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
        val char = userCharacterDao.getUserCharacter().firstOrNull()
        return calculateAge(char?.dob ?: "2000.01.01")
    }

    private fun calculateAge(dob: String): Int {
        return try {
            val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy.MM.dd")
            val birthDate = java.time.LocalDate.parse(dob, formatter)
            val currentDate = java.time.LocalDate.now()
            java.time.Period.between(birthDate, currentDate).years
        } catch (e: Exception) {
            0
        }
    }
}
