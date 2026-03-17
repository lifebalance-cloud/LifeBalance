package com.example.mylife.lifebalance.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dream_affirmations")
data class DreamAffirmation(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sectorId: Int,
    val text: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

