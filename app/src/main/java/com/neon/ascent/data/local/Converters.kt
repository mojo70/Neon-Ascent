package com.neon.ascent.data.local

import androidx.room.ProvidedTypeConverter
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.neon.ascent.data.local.entity.LoreChapter
import com.neon.ascent.domain.model.SpecialType
import com.neon.ascent.model.QuickHackType
import com.neon.ascent.model.Rarity

@ProvidedTypeConverter
class Converters {

    private val gson = Gson()

    @TypeConverter
    fun fromStringList(value: String?): List<String> {
        if (value.isNullOrEmpty()) return emptyList()
        return gson.fromJson(value, object : TypeToken<List<String>>() {}.type)
    }

    @TypeConverter
    fun toStringList(list: List<String>?): String = gson.toJson(list ?: emptyList<String>())

    @TypeConverter
    fun fromStringMap(value: String?): Map<String, Int> {
        if (value.isNullOrEmpty()) return emptyMap()
        return gson.fromJson(value, object : TypeToken<Map<String, Int>>() {}.type)
    }

    @TypeConverter
    fun toStringMap(map: Map<String, Int>?): String = gson.toJson(map ?: emptyMap<String, Int>())

    @TypeConverter
    fun fromLoreChapterList(value: String?): List<LoreChapter> {
        if (value.isNullOrEmpty()) return emptyList()
        return gson.fromJson(value, object : TypeToken<List<LoreChapter>>() {}.type)
    }

    @TypeConverter
    fun toLoreChapterList(list: List<LoreChapter>?): String = gson.toJson(list ?: emptyList<LoreChapter>())

    @TypeConverter
    fun fromSpecialType(value: SpecialType?): String? = value?.name

    @TypeConverter
    fun toSpecialType(value: String?): SpecialType? = value?.let { SpecialType.valueOf(it) }

    @TypeConverter
    fun fromRarity(value: Rarity?): String? = value?.name

    @TypeConverter
    fun toRarity(value: String?): Rarity? = value?.let { Rarity.valueOf(it) }

    @TypeConverter
    fun fromQuickHackType(value: QuickHackType?): String? = value?.name

    @TypeConverter
    fun toQuickHackType(value: String?): QuickHackType? = value?.let { QuickHackType.valueOf(it) }

    @TypeConverter
    fun fromChatActionList(value: String?): List<com.neon.ascent.model.ChatAction> {
        if (value.isNullOrEmpty()) return emptyList()
        return gson.fromJson(value, object : TypeToken<List<com.neon.ascent.model.ChatAction>>() {}.type)
    }

    @TypeConverter
    fun toChatActionList(list: List<com.neon.ascent.model.ChatAction>?): String = gson.toJson(list ?: emptyList<com.neon.ascent.model.ChatAction>())
}
