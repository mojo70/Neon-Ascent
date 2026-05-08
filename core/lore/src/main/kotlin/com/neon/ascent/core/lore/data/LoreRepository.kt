package com.neon.ascent.core.lore.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LoreRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val json = Json { 
        ignoreUnknownKeys = true 
    }

    private val megacorps by lazy {
        loadMegacorpsFromAssets()
    }

    fun getMegacorp(id: String): Megacorp? = megacorps[id]
    fun getAllMegacorps(): List<Megacorp> = megacorps.values.toList()

    private fun loadMegacorpsFromAssets(): Map<String, Megacorp> {
        val megacorpsMap = mutableMapOf<String, Megacorp>()
        try {
            val folder = "lore/megacorps"
            val files = context.assets.list(folder) ?: return emptyMap()
            
            for (fileName in files) {
                if (fileName.endsWith(".json")) {
                    val content = context.assets.open("$folder/$fileName")
                        .bufferedReader().use { it.readText() }
                    val megacorp = json.decodeFromString<Megacorp>(content)
                    megacorpsMap[megacorp.id] = megacorp
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return megacorpsMap
    }
}
