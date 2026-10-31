package com.demo.smartnote.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

/**
 * 路由：list -> detail/{id} -> edit?noteId=xx
 */
@Composable
fun SmartNoteNavHost(viewModel: NotesViewModel) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "list") {
        composable("list") {
            NoteListScreen(
                viewModel = viewModel,
                onAddNote = { navController.navigate("edit") },
                onOpenNote = { id -> navController.navigate("detail/$id") }
            )
        }

        composable(
            route = "edit?noteId={noteId}",
            arguments = listOf(
                navArgument("noteId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { entry ->
            val id = entry.arguments?.getLong("noteId")?.takeIf { it > 0 }
            NoteEditScreen(
                noteId = id,
                viewModel = viewModel,
                onDone = { navController.popBackStack() }
            )
        }

        composable(
            route = "detail/{noteId}",
            arguments = listOf(navArgument("noteId") { type = NavType.LongType })
        ) { entry ->
            val id = entry.arguments?.getLong("noteId") ?: return@composable
            NoteDetailScreen(
                noteId = id,
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate("edit?noteId=$it") }
            )
        }
    }
}
