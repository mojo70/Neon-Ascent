package com.neon.ascent.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.neon.ascent.core.domain.labs.models.Supplement
import com.neon.ascent.core.domain.labs.models.SupplementStackingNote

@Entity(tableName = "supplements")
data class SupplementEntity(
    @PrimaryKey val id: String,
    val name: String,
    val aliasesJson: String = "[]",
    val status: String,
    val primaryOutcomeDomainsJson: String = "[]",
    val mechanismTagsJson: String = "[]",
    val evidenceGrade: String,
    val bestContexts: String,
    val typicalDose: String,
    val onset: String,
    val keyLinksJson: String = "[]",
    val safetyNotes: String,
    val isResearchChemical: Boolean,
    val stackingNotesJson: String = "[]"
)

fun SupplementEntity.toDomain(): Supplement {
    val gson = Gson()
    val listType = object : TypeToken<List<String>>() {}.type

    val aliasesList: List<String> = try {
        gson.fromJson(aliasesJson, listType) ?: emptyList()
    } catch (_: Exception) {
        emptyList()
    }

    val outcomeDomainsList: List<String> = try {
        gson.fromJson(primaryOutcomeDomainsJson, listType) ?: emptyList()
    } catch (_: Exception) {
        emptyList()
    }

    val mechanismsList: List<String> = try {
        gson.fromJson(mechanismTagsJson, listType) ?: emptyList()
    } catch (_: Exception) {
        emptyList()
    }

    val linksList: List<String> = try {
        gson.fromJson(keyLinksJson, listType) ?: emptyList()
    } catch (_: Exception) {
        emptyList()
    }

    val stackingNotesList: List<SupplementStackingNote> = try {
        val stackingType = object : TypeToken<List<SupplementStackingNote>>() {}.type
        gson.fromJson(stackingNotesJson, stackingType) ?: emptyList()
    } catch (_: Exception) {
        emptyList()
    }

    return Supplement(
        id = id,
        name = name,
        aliases = aliasesList,
        status = status,
        primaryOutcomeDomains = outcomeDomainsList,
        mechanismTags = mechanismsList,
        evidenceGrade = evidenceGrade,
        bestContexts = bestContexts,
        typicalDose = typicalDose,
        onset = onset,
        keyLinks = linksList,
        safetyNotes = safetyNotes,
        isResearchChemical = isResearchChemical,
        stackingNotes = stackingNotesList
    )
}

fun Supplement.toEntity(): SupplementEntity {
    val gson = Gson()
    return SupplementEntity(
        id = id,
        name = name,
        aliasesJson = gson.toJson(aliases),
        status = status,
        primaryOutcomeDomainsJson = gson.toJson(primaryOutcomeDomains),
        mechanismTagsJson = gson.toJson(mechanismTags),
        evidenceGrade = evidenceGrade,
        bestContexts = bestContexts,
        typicalDose = typicalDose,
        onset = onset,
        keyLinksJson = gson.toJson(keyLinks),
        safetyNotes = safetyNotes,
        isResearchChemical = isResearchChemical,
        stackingNotesJson = gson.toJson(stackingNotes)
    )
}
