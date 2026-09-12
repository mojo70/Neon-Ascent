package com.neon.ascent.data.mapper

import com.neon.ascent.core.domain.chronicle.ChronicleEntry
import com.neon.ascent.model.JournalEntry

fun JournalEntry.toChronicleEntry(): ChronicleEntry {
    return ChronicleEntry(
        source = "journal",
        sourceId = id,
        wing = "CHRONICLE",
        room = "ORIGIN",
        content = text,
        timestamp = timestamp,
        hearted = isHearted
    )
}
