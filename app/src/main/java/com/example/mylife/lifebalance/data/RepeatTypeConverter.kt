package com.example.mylife.lifebalance.data

import androidx.room.TypeConverter

class RepeatTypeConverter {
    @TypeConverter
    fun fromString(value: String?): RepeatType? =
        value?.let { RepeatType.valueOf(it) }

    @TypeConverter
    fun repeatTypeToString(repeatType: RepeatType?): String? =
        repeatType?.name
}





















