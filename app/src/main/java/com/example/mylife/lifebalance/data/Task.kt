package com.example.mylife.lifebalance.data
import androidx.room.Entity
import androidx.room.PrimaryKey
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalTime

@Entity(tableName = "tasks")
data class Task (
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sphereId: Long,
    val title: String,
    val description: String,
    val isCompleted: Boolean = false,
    val date: LocalDate = LocalDate.now(),
    val time: LocalTime? = null,
    val hasNotification: Boolean = false,
    val notificationSound: String = "default",
    val autoReschedule: Boolean = false,
    val repeatType: RepeatType = RepeatType.NONE,
    val repeatEndDate: LocalDate? = null
)
