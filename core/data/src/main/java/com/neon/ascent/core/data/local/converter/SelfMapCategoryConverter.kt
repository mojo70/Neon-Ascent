package com.neon.ascent.core.data.local.converter

import androidx.room.TypeConverter
import com.neon.ascent.core.domain.selfmap.SelfMapCategory

class SelfMapCategoryConverter {
    @TypeConverter
    fun fromCategory(category: SelfMapCategory): String = category.name

    @TypeConverter
    fun toCategory(name: String): SelfMapCategory = runCatching {
        SelfMapCategory.valueOf(name)
    }.getOrDefault(SelfMapCategory.CUSTOM)
}
