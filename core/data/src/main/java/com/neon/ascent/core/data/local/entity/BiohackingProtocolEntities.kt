package com.neon.ascent.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.neon.ascent.core.domain.labs.models.BiohackingProtocol
import com.neon.ascent.core.domain.labs.models.ProtocolStackingNote

@Entity(tableName = "biohacking_protocols")
data class BiohackingProtocolEntity(
    @PrimaryKey val id: String,
    val name: String,
    val aliasesJson: String = "[]",
    val status: String,
    val primaryOutcomeDomainsJson: String = "[]",
    val protocolCategory: String,
    val mechanismTagsJson: String = "[]",
    val evidenceGrade: String,
    val bestContexts: String,
    val typicalParameters: String,
    val onset: String,
    val keyLinksJson: String = "[]",
    val safetyNotes: String,
    val notes: String = "",
    val stackingNotesJson: String = "[]"
)

fun BiohackingProtocolEntity.toDomain(): BiohackingProtocol {
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

    val stackingNotesList: List<ProtocolStackingNote> = try {
        val stackingType = object : TypeToken<List<ProtocolStackingNote>>() {}.type
        gson.fromJson(stackingNotesJson, stackingType) ?: emptyList()
    } catch (_: Exception) {
        emptyList()
    }

    return BiohackingProtocol(
        id = id,
        name = name,
        aliases = aliasesList,
        status = status,
        primaryOutcomeDomains = outcomeDomainsList,
        protocolCategory = protocolCategory,
        mechanismTags = mechanismsList,
        evidenceGrade = evidenceGrade,
        bestContexts = bestContexts,
        typicalParameters = typicalParameters,
        onset = onset,
        keyLinks = linksList,
        safetyNotes = safetyNotes,
        notes = notes,
        stackingNotes = stackingNotesList
    )
}

fun BiohackingProtocol.toEntity(): BiohackingProtocolEntity {
    val gson = Gson()
    return BiohackingProtocolEntity(
        id = id,
        name = name,
        aliasesJson = gson.toJson(aliases),
        status = status,
        primaryOutcomeDomainsJson = gson.toJson(primaryOutcomeDomains),
        protocolCategory = protocolCategory,
        mechanismTagsJson = gson.toJson(mechanismTags),
        evidenceGrade = evidenceGrade,
        bestContexts = bestContexts,
        typicalParameters = typicalParameters,
        onset = onset,
        keyLinksJson = gson.toJson(keyLinks),
        safetyNotes = safetyNotes,
        notes = notes,
        stackingNotesJson = gson.toJson(stackingNotes)
    )
}
