package com.example.mylife.lifebalance.data
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import org.threeten.bp.LocalDate

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE sphereId = :sphereId")
    fun getTasksBySphereId(sphereId: Long): Flow<List<Task>>
    
    @Query("SELECT * FROM tasks WHERE date = :date ORDER BY id")
    fun getTasksByDate(date: LocalDate): Flow<List<Task>>
    
    @Query("SELECT * FROM tasks WHERE date >= :startDate AND date <= :endDate ORDER BY date, id")
    fun getTasksByDateRange(startDate: LocalDate, endDate: LocalDate): Flow<List<Task>>
    
    @Query("SELECT * FROM tasks ORDER BY date, id")
    fun getAllTasks(): Flow<List<Task>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task): Long
    @Update
    suspend fun updateTask(task: Task)
    @Delete
    suspend fun deleteTask(task: Task)
    @Query("DELETE FROM tasks WHERE sphereId = :sphereId")
    suspend fun deleteTasksBySphereId(sphereId: Long)
    
    @Query("DELETE FROM tasks WHERE title = :title AND sphereId = :sphereId AND date > :endDate")
    suspend fun deleteTasksAfterDate(title: String, sphereId: Long, endDate: LocalDate)
}
