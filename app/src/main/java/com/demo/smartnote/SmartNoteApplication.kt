package com.demo.smartnote

import android.app.Application
import com.demo.smartnote.data.NoteDatabase
import com.demo.smartnote.data.NoteRepository
import com.demo.smartnote.data.TaskRepository
import com.demo.smartnote.reminder.ReminderStore

/**
 * 演示工程没上 Hilt，依赖在 Application 里手动组装。
 */
class SmartNoteApplication : Application() {

    val database: NoteDatabase by lazy { NoteDatabase.get(this) }

    val repository: NoteRepository by lazy {
        NoteRepository(database.noteDao(), database.categoryDao())
    }

    val taskRepository: TaskRepository by lazy {
        TaskRepository(database.taskDao(), ReminderStore(this), this)
    }
}
