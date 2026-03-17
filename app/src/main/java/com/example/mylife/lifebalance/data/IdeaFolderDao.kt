package com.example.mylife.lifebalance.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface IdeaFolderDao {
    @Query("SELECT * FROM idea_folders ORDER BY createdAt ASC")
    fun getAllFolders(): Flow<List<IdeaFolder>>

    @Query("SELECT * FROM idea_folders WHERE id = :id")
    suspend fun getFolderById(id: Long): IdeaFolder?

    @Insert
    suspend fun insertFolder(folder: IdeaFolder): Long

    @Update
    suspend fun updateFolder(folder: IdeaFolder)

    @Delete
    suspend fun deleteFolder(folder: IdeaFolder)
}








