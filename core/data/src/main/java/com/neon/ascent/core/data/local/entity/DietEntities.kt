package com.neon.ascent.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.neon.ascent.core.domain.labs.models.Diet
import com.neon.ascent.core.domain.labs.models.DietParameters
import com.neon.ascent.core.domain.labs.models.DietStackingNote

@Entity(tableName = "diets")
data class DietEntity(
    @PrimaryKey val id: String,
    val name: String,
    val status: String,
    val primaryOutcomeDomains: List<String>,
    val dietCategory: String,
    val mechanismTags: List<String>,
    val evidenceGrade: String,
    val typicalParametersJson: String,
    val onset: String,
    val keyLinks: List<String>,
    val safetyNotes: String,
    val isAppSpecific: Boolean,
    val stackingNotesJson: String = "[]"
)

fun DietEntity.toDomain(): Diet {
    val gson = Gson()
    val params = try {
        gson.fromJson(typicalParametersJson, DietParameters::class.java)
    } catch (_: Exception) {
        DietParameters(rawParametersDescription = typicalParametersJson)
    }

    val stacking = try {
        gson.fromJson<List<DietStackingNote>>(
            stackingNotesJson,
            object : TypeToken<List<DietStackingNote>>() {}.type
        ) ?: emptyList()
    } catch (_: Exception) {
        emptyList()
    }

    return Diet(
        id = id,
        name = name,
        status = status,
        primaryOutcomeDomains = primaryOutcomeDomains,
        dietCategory = dietCategory,
        mechanismTags = mechanismTags,
        evidenceGrade = evidenceGrade,
        typicalParameters = params ?: DietParameters(),
        onset = onset,
        keyLinks = keyLinks,
        safetyNotes = safetyNotes,
        isAppSpecific = isAppSpecific,
        stackingNotes = stacking
    )
}

fun Diet.toEntity(): DietEntity {
    val gson = Gson()
    return DietEntity(
        id = id,
        name = name,
        status = status,
        primaryOutcomeDomains = primaryOutcomeDomains,
        dietCategory = dietCategory,
        mechanismTags = mechanismTags,
        evidenceGrade = evidenceGrade,
        typicalParametersJson = gson.toJson(typicalParameters),
        onset = onset,
        keyLinks = keyLinks,
        safetyNotes = safetyNotes,
        isAppSpecific = isAppSpecific,
        stackingNotesJson = gson.toJson(stackingNotes)
    )
}
