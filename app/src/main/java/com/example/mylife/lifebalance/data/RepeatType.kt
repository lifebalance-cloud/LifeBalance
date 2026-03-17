package com.example.mylife.lifebalance.data

import android.content.Context
import com.example.lifebalance.R

enum class RepeatType(val displayName: String) {
    NONE("Не повторяется"),
    DAILY("Каждый день"),
    WEEKDAYS("Каждый день ПН-ПТ"),
    WEEKLY("Каждую неделю"),
    MONTHLY("Каждый месяц"),
    YEARLY("Каждый год");
    
    /**
     * Возвращает локализованное имя типа повторения
     */
    fun getLocalizedDisplayName(context: Context): String {
        return when (this) {
            NONE -> context.resources.getString(R.string.repeat_none)
            DAILY -> context.resources.getString(R.string.repeat_daily)
            WEEKDAYS -> context.resources.getString(R.string.repeat_weekdays)
            WEEKLY -> context.resources.getString(R.string.repeat_weekly)
            MONTHLY -> context.resources.getString(R.string.repeat_monthly)
            YEARLY -> context.resources.getString(R.string.repeat_yearly)
        }
    }
}





















