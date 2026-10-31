package com.demo.smartnote

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.demo.smartnote.ui.NotesViewModel
import com.demo.smartnote.ui.SmartNoteNavHost
import com.demo.smartnote.ui.theme.SmartNoteTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // 从通知点进来时带 route=tasks，直接落到今日任务页
        val startRoute = if (intent?.getStringExtra(EXTRA_ROUTE) == "tasks") "tasks" else "list"
        setContent {
            SmartNoteTheme {
                val app = application as SmartNoteApplication
                // Activity 作用域共享同一个 ViewModel
                val notesViewModel: NotesViewModel = viewModel(
                    factory = NotesViewModel.factory(app.repository, app.taskRepository)
                )
                SmartNoteNavHost(viewModel = notesViewModel, startDestination = startRoute)
            }
        }
    }

    companion object {
        const val EXTRA_ROUTE = "route"
    }
}
