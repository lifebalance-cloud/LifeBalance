package com.example.mylife.lifebalance.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dream_sector_photos")
data class DreamSectorPhoto(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sectorId: Int,
    val photoUri: String, // Локальный URI в приватном хранилище
    val firebaseStoragePath: String? = null, // Путь в Firebase Storage (для синхронизации)
    val order: Int = 0, // Порядок фото в секторе (0 или 1, максимум 2 фото)
    val createdAt: Long = System.currentTimeMillis()
)

