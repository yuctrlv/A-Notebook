package com.demo.smartnote.data

import android.content.Context
import com.demo.smartnote.reminder.ReminderScheduler
import com.demo.smartnote.reminder.ReminderSettings
import com.demo.smartnote.reminder.ReminderStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.LocalDate

/**
 * 每日任务的数据入口：任务模板 + 按日期的完成记录 + 提醒设置。
 * 界面只跟它打交道，具体是 Room 还是 WorkManager 由它负责。
 */
class TaskRepository(
    private val dao: TaskDao,
    private val reminderStore: ReminderStore,
    private val appContext: Context
) {

    /** 今天的日期字符串，全 App 统一用它当 key */
    fun today(): String = LocalDate.now().toString()

    val tasks: Flow<List<DailyTask>> = dao.observeTasks()

    val reminder: Flow<ReminderSettings> = reminderStore.settings

    fun doneIds(date: String): Flow<List<Long>> = dao.observeDoneIds(date)

    suspend fun addTask(title: String) {
        val clean = title.trim()
        if (clean.isEmpty()) return
        dao.insertTask(DailyTask(title = clean))
    }

    suspend fun renameTask(task: DailyTask, title: String) {
        val clean = title.trim()
        if (clean.isEmpty()) return
        dao.updateTask(task.copy(title = clean))
    }

    suspend fun deleteTask(task: DailyTask) {
        dao.deleteRecordsOf(task.id)
        dao.deleteTask(task)
    }

    /** 勾选 / 取消勾选：有记录就删，没记录就写一条 done=true */
    suspend fun toggleDone(taskId: Long, date: String) {
        if (dao.isDone(taskId, date) > 0) {
            dao.deleteRecord(taskId, date)
        } else {
            dao.upsertRecord(TaskRecord(taskId = taskId, date = date, done = true))
        }
    }

    suspend fun setReminderEnabled(enabled: Boolean) {
        reminderStore.setEnabled(enabled)
        if (enabled) {
            val s = currentSettings()
            ReminderScheduler.scheduleNext(appContext, s.hour, s.minute)
        } else {
            ReminderScheduler.cancel(appContext)
        }
    }

    suspend fun setReminderTime(hour: Int, minute: Int) {
        reminderStore.setTime(hour, minute)
        val s = currentSettings()
        if (s.enabled) ReminderScheduler.scheduleNext(appContext, hour, minute)
    }

    private suspend fun currentSettings(): ReminderSettings = reminder.first()

    // ---------- 单条笔记的定时提醒（复用同一个 appContext） ----------

    fun scheduleNoteReminder(noteId: Long, atMillis: Long) {
        com.demo.smartnote.reminder.NoteReminderScheduler.schedule(appContext, noteId, atMillis)
    }

    fun cancelNoteReminder(noteId: Long) {
        com.demo.smartnote.reminder.NoteReminderScheduler.cancel(appContext, noteId)
    }
}
