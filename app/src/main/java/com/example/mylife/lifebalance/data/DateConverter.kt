package com.example.mylife.lifebalance.data

import androidx.room.TypeConverter
// Вместо: import java.time.LocalDate
import org.threeten.bp.LocalDate

class DateConverter {

    @TypeConverter
    fun fromString(value: String?): LocalDate? =
        value?.let { LocalDate.parse(it) }

    @TypeConverter
    fun dateToString(date: LocalDate?): String? =
        date?.toString()
}
