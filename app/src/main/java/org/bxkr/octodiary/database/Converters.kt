package org.bxkr.octodiary.database

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Date

import org.bxkr.octodiary.models.events.Homework
import org.bxkr.octodiary.models.events.Mark
import org.bxkr.octodiary.models.events.Material
import org.bxkr.octodiary.models.marklistsubject.Period
import org.bxkr.octodiary.models.classranking.PreviousRank
import org.bxkr.octodiary.models.classranking.Rank
import org.bxkr.octodiary.models.visits.Visit

class Converters {
    private val gson = Gson()
    
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }
    
    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
    
    @TypeConverter
    fun fromStringList(value: String?): List<String> {
        if (value == null || value.isEmpty()) return emptyList()
        val listType = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, listType)
    }
    
    @TypeConverter
    fun stringListToString(list: List<String>?): String {
        return gson.toJson(list ?: emptyList<String>())
    }
    
    @TypeConverter
    fun fromLongList(value: String?): List<Long> {
        if (value == null || value.isEmpty()) return emptyList()
        val listType = object : TypeToken<List<Long>>() {}.type
        return gson.fromJson(value, listType)
    }
    
    @TypeConverter
    fun longListToString(list: List<Long>?): String {
        return gson.toJson(list ?: emptyList<Long>())
    }
    
    @TypeConverter
    fun fromIntList(value: String?): List<Int> {
        if (value == null || value.isEmpty()) return emptyList()
        val listType = object : TypeToken<List<Int>>() {}.type
        return gson.fromJson(value, listType)
    }
    
    @TypeConverter
    fun intListToString(list: List<Int>?): String {
        return gson.toJson(list ?: emptyList<Int>())
    }

    @TypeConverter
    fun fromHomework(value: String?): Homework? {
        return value?.let { gson.fromJson(it, Homework::class.java) }
    }

    @TypeConverter
    fun toHomework(homework: Homework?): String? {
        return gson.toJson(homework)
    }

    @TypeConverter
    fun fromMarkList(value: String?): List<Mark>? {
        if (value == null || value.isEmpty()) return emptyList()
        val listType = object : TypeToken<List<Mark>>() {}.type
        return gson.fromJson(value, listType)
    }

    @TypeConverter
    fun toMarkList(list: List<Mark>?): String? {
        return gson.toJson(list)
    }

    @TypeConverter
    fun fromMaterialList(value: String?): List<Material>? {
        if (value == null || value.isEmpty()) return emptyList()
        val listType = object : TypeToken<List<Material>>() {}.type
        return gson.fromJson(value, listType)
    }

    @TypeConverter
    fun toMaterialList(list: List<Material>?): String? {
        return gson.toJson(list)
    }

    @TypeConverter
    fun fromPeriodList(value: String?): List<Period>? {
        if (value == null || value.isEmpty()) return emptyList()
        val listType = object : TypeToken<List<Period>>() {}.type
        return gson.fromJson(value, listType)
    }

    @TypeConverter
    fun toPeriodList(list: List<Period>?): String? {
        return gson.toJson(list)
    }

    @TypeConverter
    fun fromPreviousRank(value: String?): PreviousRank? {
        return value?.let { gson.fromJson(it, PreviousRank::class.java) }
    }

    @TypeConverter
    fun toPreviousRank(obj: PreviousRank?): String? {
        return gson.toJson(obj)
    }

    @TypeConverter
    fun fromRank(value: String?): Rank? {
        return value?.let { gson.fromJson(it, Rank::class.java) }
    }

    @TypeConverter
    fun toRank(obj: Rank?): String? {
        return gson.toJson(obj)
    }

    @TypeConverter
    fun fromVisitList(value: String?): List<Visit>? {
        if (value == null || value.isEmpty()) return emptyList()
        val listType = object : TypeToken<List<Visit>>() {}.type
        return gson.fromJson(value, listType)
    }

    @TypeConverter
    fun toVisitList(list: List<Visit>?): String? {
        return gson.toJson(list)
    }
}