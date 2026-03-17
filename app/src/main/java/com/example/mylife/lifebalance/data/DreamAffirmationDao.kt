package com.example.mylife.lifebalance.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DreamAffirmationDao {
    @Query("SELECT * FROM dream_affirmations WHERE sectorId = :sectorId ORDER BY createdAt DESC")
    fun getAffirmationsBySectorId(sectorId: Int): Flow<List<DreamAffirmation>>
    
    @Query("SELECT * FROM dream_affirmations ORDER BY createdAt DESC")
    fun getAllAffirmations(): Flow<List<DreamAffirmation>>
    
    @Query("SELECT * FROM dream_affirmations WHERE id = :id LIMIT 1")
    suspend fun getAffirmationById(id: Long): DreamAffirmation?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAffirmation(affirmation: DreamAffirmation): Long
    
    @Update
    suspend fun updateAffirmation(affirmation: DreamAffirmation)
    
    @Delete
    suspend fun deleteAffirmation(affirmation: DreamAffirmation)
    
    @Query("DELETE FROM dream_affirmations WHERE id = :id")
    suspend fun deleteAffirmationById(id: Long)
    
    @Query("DELETE FROM dream_affirmations WHERE sectorId = :sectorId")
    suspend fun deleteAffirmationsBySectorId(sectorId: Int)
}

