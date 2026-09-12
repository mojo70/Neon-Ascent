package com.neon.ascent.data.chronicle

import android.util.Log
import com.neon.ascent.core.domain.chronicle.ChronicleEntry
import com.neon.ascent.core.domain.chronicle.ChronicleRepository
import com.neon.ascent.data.local.ChatDao
import com.neon.ascent.data.local.JournalDao
import com.neon.ascent.data.local.LoreDao
import com.neon.ascent.data.mapper.toChronicleEntry
import com.neon.ascent.model.ChatMessage
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChronicleImportUseCase @Inject constructor(
    private val journalDao: JournalDao,
    private val loreDao: LoreDao,
    private val chatDao: ChatDao,
    private val chronicleRepository: ChronicleRepository
) {
    suspend fun runImport(): ImportResult {
        var importedJournal = 0
        var importedShard = 0
        var importedFragment = 0
        var importedChat = 0

        // 1. Copy journal_entries -> CHRONICLE/ORIGIN
        val journalEntries = runCatching { journalDao.getAllEntries().first() }.getOrDefault(emptyList())
        journalEntries.forEach { j ->
            val imported = chronicleRepository.importIfAbsent(j.toChronicleEntry())
            if (imported) importedJournal++
        }
        logDebug("ChronicleImportJob", "IMPORT_JOURNAL $importedJournal")

        // 2. Copy data_shards -> CHRONICLE/SHARD
        val shards = runCatching { loreDao.getAllDataShards().first() }.getOrDefault(emptyList())
        shards.forEach { shard ->
            val contentStr = if (shard.title.isNotBlank() && shard.content.isNotBlank() && shard.title != shard.content) {
                "${shard.title}\n\n${shard.content}"
            } else {
                shard.content.ifBlank { shard.title }
            }
            val entry = ChronicleEntry(
                source = "shard",
                sourceId = shard.id,
                wing = "CHRONICLE",
                room = "SHARD",
                content = contentStr,
                timestamp = shard.droppedAt,
                hearted = false
            )
            val imported = chronicleRepository.importIfAbsent(entry)
            if (imported) importedShard++
        }
        logDebug("ChronicleImportJob", "IMPORT_SHARD $importedShard")

        // 3. Copy memory_fragments -> CHRONICLE/FRAGMENT
        val fragments = runCatching { loreDao.getAllMemoryFragments().first() }.getOrDefault(emptyList())
        fragments.forEach { frag ->
            val body = if (frag.isUnlocked && frag.decryptedContent.isNotBlank()) frag.decryptedContent else frag.corruptedContent
            val contentStr = if (frag.title.isNotBlank() && frag.title != body) {
                "${frag.title}\n\n$body"
            } else {
                body
            }
            val entry = ChronicleEntry(
                source = "fragment",
                sourceId = frag.id,
                wing = "CHRONICLE",
                room = "FRAGMENT",
                content = contentStr,
                timestamp = System.currentTimeMillis(),
                hearted = false
            )
            val imported = chronicleRepository.importIfAbsent(entry)
            if (imported) importedFragment++
        }
        logDebug("ChronicleImportJob", "IMPORT_FRAGMENT $importedFragment")

        // 4. Copy chat_messages -> DIALOGUE / contactName
        val sessions = runCatching { chatDao.getChatSessions().first() }.getOrDefault(emptyList())
        val allMessages = mutableListOf<Pair<String, ChatMessage>>()

        for (session in sessions) {
            val msgs = runCatching { chatDao.getMessagesForSession(session.sessionId).first() }.getOrDefault(emptyList())
            val contact = session.contactName.ifBlank { "UNKNOWN" }
            msgs.forEach { msg ->
                allMessages.add(Pair(contact, msg))
            }
        }

        val cutoff90Days = System.currentTimeMillis() - 90L * 24 * 60 * 60 * 1000
        val isThousands = allMessages.size > 2000
        val messagesToProcess = if (isThousands) {
            logWarn("ChronicleImportJob", "IMPORT_CHAT_TRUNCATED")
            allMessages.filter { it.second.timestamp >= cutoff90Days }
        } else {
            allMessages
        }

        messagesToProcess.forEach { (contact, msg) ->
            val sender = msg.senderName.ifBlank { if (msg.isFromUser) "User" else contact }
            val entry = ChronicleEntry(
                source = "chat",
                sourceId = "chat_${msg.id}",
                wing = "DIALOGUE",
                room = contact,
                content = "$sender: ${msg.text}",
                timestamp = msg.timestamp,
                hearted = false
            )
            val imported = chronicleRepository.importIfAbsent(entry)
            if (imported) importedChat++
        }
        logDebug("ChronicleImportJob", "IMPORT_CHAT $importedChat")

        logDebug("ChronicleImportJob", "IMPORT_JOURNAL $importedJournal / IMPORT_SHARD $importedShard / IMPORT_FRAGMENT $importedFragment / IMPORT_CHAT $importedChat")

        return ImportResult(importedJournal, importedShard, importedFragment, importedChat)
    }

    private fun logDebug(tag: String, msg: String) {
        try {
            Log.d(tag, msg)
        } catch (_: Throwable) {
            println("[$tag] $msg")
        }
    }

    private fun logWarn(tag: String, msg: String) {
        try {
            Log.w(tag, msg)
        } catch (_: Throwable) {
            println("[$tag] $msg")
        }
    }
}

data class ImportResult(
    val importedJournal: Int,
    val importedShard: Int,
    val importedFragment: Int,
    val importedChat: Int
)
