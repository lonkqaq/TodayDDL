package com.todayddl.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.todayddl.app.data.local.entity.TaskEntity
import com.todayddl.app.data.local.model.TaskWithTag
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Transaction
    @Query("SELECT * FROM tasks")
    fun observeAllTasksWithTag(): Flow<List<TaskWithTag>>

    @Transaction
    @Query("SELECT * FROM tasks WHERE isCompleted = 0 AND dueDateEpochDay = :epochDay")
    suspend fun getIncompleteTasksWithTagForDay(epochDay: Long): List<TaskWithTag>

    @Transaction
    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getTaskWithTagById(id: Long): TaskWithTag?

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getTaskById(id: Long): TaskEntity?

    @Query("SELECT * FROM tasks WHERE isCompleted = 0")
    suspend fun getIncompleteTasksOnce(): List<TaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: TaskEntity): Long

    @Update
    suspend fun update(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE tasks SET isCompleted = :isCompleted, completedAtMillis = :completedAtMillis WHERE id = :id")
    suspend fun updateCompletion(id: Long, isCompleted: Boolean, completedAtMillis: Long?)
}
