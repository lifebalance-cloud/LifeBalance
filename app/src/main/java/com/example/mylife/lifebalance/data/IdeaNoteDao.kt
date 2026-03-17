package com.example.mylife.lifebalance.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface IdeaNoteDao {
    @Query("SELECT * FROM idea_notes WHERE folderId IS NULL ORDER BY updatedAt DESC")
    fun getNotesWithoutFolder(): Flow<List<IdeaNote>>

    @Query("SELECT * FROM idea_notes WHERE folderId = :folderId ORDER BY updatedAt DESC")
    fun getNotesByFolderId(folderId: Long): Flow<List<IdeaNote>>

    @Query("SELECT * FROM idea_notes WHERE text LIKE '%' || :query || '%' ORDER BY updatedAt DESC")
    fun searchNotes(query: String): Flow<List<IdeaNote>>

    @Query("SELECT * FROM idea_notes WHERE id = :id")
    suspend fun getNoteById(id: Long): IdeaNote?

    @Insert
    suspend fun insertNote(note: IdeaNote): Long

    @Update
    suspend fun updateNote(note: IdeaNote)

    @Delete
    suspend fun deleteNote(note: IdeaNote)

    @Query("DELETE FROM idea_notes WHERE folderId = :folderId")
    suspend fun deleteNotesByFolderId(folderId: Long)
}








