package com.neon.ascent.core.data.local.converter

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.neon.ascent.core.domain.model.DataSource
import com.neon.ascent.core.domain.model.SpecialType
import com.neon.ascent.core.domain.model.TestType
import java.time.Instant

class InstantConverter {
    @TypeConverter
    fun fromInstant(instant: Instant?): Long? = instant?.toEpochMilli()

    @TypeConverter
    fun toInstant(value: Long?): Instant? = value?.let { Instant.ofEpochMilli(it) }
}

class SpecialTypeConverter {
    @TypeConverter
    fun fromSpecialType(type: SpecialType): String = type.name

    @TypeConverter
    fun toSpecialType(name: String): SpecialType = SpecialType.valueOf(name)
}

class TestTypeConverter {
    @TypeConverter
    fun fromTestType(type: TestType): String = type.name

    @TypeConverter
    fun toTestType(name: String): TestType = TestType.valueOf(name)
}

class DataSourceConverter {
    @TypeConverter
    fun fromDataSource(source: DataSource): String = source.name

    @TypeConverter
    fun toDataSource(name: String): DataSource = DataSource.valueOf(name)
}

class StringMapConverter {
    private val gson = Gson()

    @TypeConverter
    fun fromMap(map: Map<String, String>?): String? {
        return map?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toMap(json: String?): Map<String, String> {
        return json?.let {
            gson.fromJson(it, object : TypeToken<Map<String, String>>() {}.type)
        } ?: emptyMap()
    }
}

class SpecialTypeListConverter {
    @TypeConverter
    fun fromList(list: List<SpecialType>): String = list.joinToString(",") { it.name }

    @TypeConverter
    fun toList(data: String): List<SpecialType> = if (data.isBlank()) emptyList() else data.split(",").map { SpecialType.valueOf(it) }
}

class StringListConverter {
    @TypeConverter
    fun fromList(list: List<String>?): String? = list?.joinToString(",")

    @TypeConverter
    fun toList(data: String?): List<String>? = data?.split(",")?.filter { it.isNotBlank() }
}
