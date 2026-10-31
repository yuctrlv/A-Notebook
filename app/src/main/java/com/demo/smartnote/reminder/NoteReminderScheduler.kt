package com.demo.smartnote.reminder

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * 单条笔记的「定时提醒」：到点弹一条通知，提醒你看这条笔记。
 * 每个笔记用独立的 work 名，互不影响；重复设置会覆盖旧的。
 */
object NoteReminderScheduler {

    private fun workName(noteId: Long) = "note_reminder_$noteId"

    fun schedule(context: Context, noteId: Long, atMillis: Long) {
        val delay = atMillis - System.currentTimeMillis()
        // 时间已经过去就没必要排了
        if (delay <= 0) return

        val request = OneTimeWorkRequestBuilder<NoteReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(Data.Builder().putLong(NoteReminderWorker.KEY_NOTE_ID, noteId).build())
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(workName(noteId), ExistingWorkPolicy.REPLACE, request)
    }

    fun cancel(context: Context, noteId: Long) {
        WorkManager.getInstance(context).cancelUniqueWork(workName(noteId))
    }
}
