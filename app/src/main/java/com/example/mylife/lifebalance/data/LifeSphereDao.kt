package com.example.mylife.lifebalance.data
import androidx.room.*
import kotlinx.coroutines.flow.Flow
@Dao
interface LifeSphereDao {
    @Query("SELECT * FROM life_spheres ORDER BY `order` ASC")
    fun getAllSpheres(): Flow<List<LifeSphere>>
    @Query("SELECT * FROM life_spheres WHERE id = :id")
    suspend fun getSphereById(id: Long): LifeSphere?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSphere(sphere: LifeSphere): Long
    @Update
    suspend fun updateSphere(sphere: LifeSphere)
    @Delete
    suspend fun deleteSphere(sphere: LifeSphere)
    @Query("SELECT COUNT(*) FROM life_spheres")
    suspend fun getSphereCount(): Int
}
