package com.neon.ascent.core.data.mapper

import com.neon.ascent.core.data.local.entity.AdaptedProtocolEntity
import com.neon.ascent.core.data.local.entity.ProtocolEntity
import com.neon.ascent.core.domain.goals.models.AdaptedProtocol
import com.neon.ascent.core.domain.goals.models.Protocol

fun ProtocolEntity.toDomain() = Protocol(
    id = id,
    title = title,
    description = description,
    category = category,
    canonicalSteps = canonicalSteps,
    source = source,
    specialTags = specialTags,
    defaultDurationDays = defaultDurationDays,
    isCanonical = isCanonical
)

fun Protocol.toEntity() = ProtocolEntity(
    id = id,
    title = title,
    description = description,
    category = category,
    canonicalSteps = canonicalSteps,
    source = source,
    specialTags = specialTags,
    defaultDurationDays = defaultDurationDays,
    isCanonical = isCanonical
)

fun AdaptedProtocolEntity.toDomain() = AdaptedProtocol(
    id = id,
    protocolId = protocolId,
    directiveId = directiveId,
    adaptedTitle = adaptedTitle,
    adaptedSteps = adaptedSteps,
    userNotes = userNotes,
    lastSyncTimestamp = lastSyncTimestamp
)

fun AdaptedProtocol.toEntity() = AdaptedProtocolEntity(
    id = id,
    protocolId = protocolId,
    directiveId = directiveId,
    adaptedTitle = adaptedTitle,
    adaptedSteps = adaptedSteps,
    userNotes = userNotes,
    lastSyncTimestamp = lastSyncTimestamp
)
