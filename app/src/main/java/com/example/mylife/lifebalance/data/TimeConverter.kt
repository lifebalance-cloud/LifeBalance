package com.example.mylife.lifebalance.data

import androidx.room.TypeConverter
import org.threeten.bp.LocalTime

class TimeConverter {
    @TypeConverter
    fun fromString(value: String?): LocalTime? =
        value?.let { LocalTime.parse(it) }

    @TypeConverter
    fun timeToString(time: LocalTime?): String? =
        time?.toString()
}

