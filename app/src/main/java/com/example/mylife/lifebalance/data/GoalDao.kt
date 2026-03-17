package com.example.mylife.lifebalance.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {

    @Query("SELECT * FROM Goal WHERE sphereId = :sphereId ORDER BY deadline")
    fun getGoalsForSphere(sphereId: Int): Flow<List<Goal>>

    @Query("SELECT * FROM Goal ORDER BY deadline")
    fun getAllGoals(): Flow<List<Goal>>

    @Query("SELECT * FROM Goal WHERE id = :goalId LIMIT 1")
    fun getGoalById(goalId: Int): Flow<Goal?>

    @Insert
    suspend fun insertGoal(goal: Goal): Long

    @Update
    suspend fun updateGoal(goal: Goal)

    @Delete
    suspend fun deleteGoal(goal: Goal)
}
