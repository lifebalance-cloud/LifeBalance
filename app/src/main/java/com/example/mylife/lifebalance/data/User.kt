package com.example.mylife.lifebalance.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey
    val uid: String,
    val email: String,
    val displayName: String? = null,
    val photoUrl: String? = null,
    val lastSyncTimestamp: Long = 0L,
    val isOnline: Boolean = false
)



