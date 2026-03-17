package com.example.mylife.lifebalance.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DreamSectorPhotoDao {
    @Query("SELECT * FROM dream_sector_photos WHERE sectorId = :sectorId ORDER BY `order` ASC")
    fun getPhotosBySectorId(sectorId: Int): Flow<List<DreamSectorPhoto>>
    
    @Query("SELECT * FROM dream_sector_photos WHERE sectorId = :sectorId ORDER BY `order` ASC")
    suspend fun getPhotosBySectorIdSync(sectorId: Int): List<DreamSectorPhoto>
    
    @Query("SELECT * FROM dream_sector_photos")
    fun getAllPhotos(): Flow<List<DreamSectorPhoto>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhoto(photo: DreamSectorPhoto): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhotos(photos: List<DreamSectorPhoto>)
    
    @Update
    suspend fun updatePhoto(photo: DreamSectorPhoto)
    
    @Delete
    suspend fun deletePhoto(photo: DreamSectorPhoto)
    
    @Query("DELETE FROM dream_sector_photos WHERE sectorId = :sectorId")
    suspend fun deletePhotosBySectorId(sectorId: Int)
    
    @Query("DELETE FROM dream_sector_photos WHERE id = :photoId")
    suspend fun deletePhotoById(photoId: Long)
}

