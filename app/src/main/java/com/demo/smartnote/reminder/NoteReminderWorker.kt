package com.demo.smartnote.reminder

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.demo.smartnote.data.NoteDatabase

/** 笔记定时提醒到点后的 Worker：读笔记 → 发通知 → 顺手把 reminderAt 清空 */
class NoteReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val noteId = inputData.getLong(KEY_NOTE_ID, -1L)
        if (noteId <= 0) return Result.success()

        val db = NoteDatabase.get(applicationContext)
        val note = db.noteDao().getById(noteId) ?: return Result.success()

        Notifications.showNoteReminder(
            context = applicationContext,
            title = note.displayTitle.ifBlank { "笔记提醒" },
            content = note.content.take(60)
        )
        // 一次性提醒，响过就清掉，避免界面上还挂着「已设置」
        db.noteDao().setReminder(noteId, null)
        return Result.success()
    }

    companion object {
        const val KEY_NOTE_ID = "noteId"
    }
}
