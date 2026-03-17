package com.example.mylife.lifebalance.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "idea_notes")
data class IdeaNote(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val folderId: Long? = null, // null означает, что заметка не принадлежит папке
    val text: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)








