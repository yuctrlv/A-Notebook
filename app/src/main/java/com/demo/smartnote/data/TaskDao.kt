package com.demo.smartnote.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Query("SELECT * FROM daily_tasks ORDER BY id")
    fun observeTasks(): Flow<List<DailyTask>>

    /** 某天已完成的任务 id 集合 */
    @Query("SELECT taskId FROM task_records WHERE date = :date AND done = 1")
    fun observeDoneIds(date: String): Flow<List<Long>>

    @Query("SELECT COUNT(*) FROM daily_tasks")
    suspend fun countTasks(): Int

    /** 某天还没完成的任务数（提醒通知里用） */
    @Query(
        """
        SELECT COUNT(*) FROM daily_tasks t
        WHERE NOT EXISTS (
            SELECT 1 FROM task_records r
            WHERE r.taskId = t.id AND r.date = :date AND r.done = 1
        )
        """
    )
    suspend fun countPending(date: String): Int

    @Query("SELECT COUNT(*) FROM task_records WHERE taskId = :taskId AND date = :date AND done = 1")
    suspend fun isDone(taskId: Long, date: String): Int

    @Insert
    suspend fun insertTask(task: DailyTask): Long

    @Update
    suspend fun updateTask(task: DailyTask)

    @Delete
    suspend fun deleteTask(task: DailyTask)

    @Query("DELETE FROM task_records WHERE taskId = :taskId")
    suspend fun deleteRecordsOf(taskId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRecord(record: TaskRecord)

    @Query("DELETE FROM task_records WHERE taskId = :taskId AND date = :date")
    suspend fun deleteRecord(taskId: Long, date: String)
}
