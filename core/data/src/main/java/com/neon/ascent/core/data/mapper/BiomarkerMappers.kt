package com.neon.ascent.core.data.mapper

import com.neon.ascent.core.data.local.entity.BiomarkerSampleEntity
import com.neon.ascent.core.domain.codex.models.BiomarkerSample

fun BiomarkerSampleEntity.toDomain() = BiomarkerSample(
    id = id,
    markerKey = markerKey,
    displayName = displayName,
    value = value,
    unit = unit,
    drawnAt = drawnAt,
    source = source,
    notes = notes
)

fun BiomarkerSample.toEntity() = BiomarkerSampleEntity(
    id = id,
    markerKey = markerKey,
    displayName = displayName,
    value = value,
    unit = unit,
    drawnAt = drawnAt,
    source = source,
    notes = notes
)
