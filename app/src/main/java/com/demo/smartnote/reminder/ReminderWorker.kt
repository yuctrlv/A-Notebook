package com.demo.smartnote.reminder

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.demo.smartnote.data.NoteDatabase
import java.time.LocalDate

/**
 * 到点后执行：读今天的任务完成情况 → 发通知 → 排明天的提醒。
 *
 * 注意：这里是协程 Worker，直接读 Room 是安全的（Room 禁止主线程，但 Worker 在后台线程）。
 */
class ReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val hour = inputData.getInt(KEY_HOUR, 20)
        val minute = inputData.getInt(KEY_MINUTE, 0)

        val dao = NoteDatabase.get(applicationContext).taskDao()
        val today = LocalDate.now().toString()   // yyyy-MM-dd

        val total = dao.countTasks()
        val pending = dao.countPending(today)

        Notifications.showDailyReminder(applicationContext, pending, total)

        // 滚到明天同一时刻
        ReminderScheduler.scheduleNext(applicationContext, hour, minute)
        return Result.success()
    }

    companion object {
        const val KEY_HOUR = "hour"
        const val KEY_MINUTE = "minute"
    }
}
