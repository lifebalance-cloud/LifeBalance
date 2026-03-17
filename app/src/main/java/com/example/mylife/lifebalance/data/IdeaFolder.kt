package com.example.mylife.lifebalance.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "idea_folders")
data class IdeaFolder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)








