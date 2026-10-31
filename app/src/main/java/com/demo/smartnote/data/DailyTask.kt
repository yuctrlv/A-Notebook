package com.demo.smartnote.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 每日任务模板：这里的一行 = 一个「每天都要做」的任务。
 * 完成状态不放在这里，而是按日期记在 [TaskRecord] 里，
 * 这样同一条任务每天的完成情况互不影响，也不会因为跨天而丢历史。
 */
@Entity(tableName = "daily_tasks")
data class DailyTask(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 任务完成记录：一条记录 = 某天某个任务做没做。
 * 主键是 (taskId, date)，同一天重复勾选只会更新这一行。
 */
@Entity(tableName = "task_records", primaryKeys = ["taskId", "date"])
data class TaskRecord(
    val taskId: Long,
    val date: String,          // 格式 yyyy-MM-dd
    val done: Boolean,
    val updatedAt: Long = System.currentTimeMillis()
)
