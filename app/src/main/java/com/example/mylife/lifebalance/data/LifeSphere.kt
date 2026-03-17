package com.example.mylife.lifebalance.data
import androidx.room.Entity
import androidx.room.PrimaryKey
@Entity(tableName = "life_spheres")
data class LifeSphere (
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    var score: Int = 0, // 0-10
    val colorIndex: Int = 0, // индекс цвета из палитры
    val order: Int = 0 // порядок отображения

)
