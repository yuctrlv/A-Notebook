package com.demo.smartnote.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

/**
 * 路由：list -> detail/{id} -> edit?noteId=xx；list -> tasks
 *
 * @param startDestination 从通知点进来时直接落到 "tasks"
 */
@Composable
fun SmartNoteNavHost(
    viewModel: NotesViewModel,
    startDestination: String = "list"
) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = startDestination) {
        composable("list") {
            NoteListScreen(
                viewModel = viewModel,
                onAddNote = { categoryId ->
                    // 把当前选中的分类带过去，新建页就不用再手动选一次
                    navController.navigate("edit?categoryId=${categoryId ?: -1L}")
                },
                onOpenNote = { id -> navController.navigate("detail/$id") },
                onOpenTasks = { navController.navigate("tasks") }
            )
        }

        composable("tasks") {
            TasksScreen(
                viewModel = viewModel,
                onBack = {
                    // 从通知直接进来的话，返回应该回首页而不是退出
                    if (!navController.popBackStack()) navController.navigate("list")
                }
            )
        }

        composable(
            route = "edit?noteId={noteId}&categoryId={categoryId}",
            arguments = listOf(
                navArgument("noteId") {
                    type = NavType.LongType
                    defaultValue = -1L
                },
                navArgument("categoryId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { entry ->
            val id = entry.arguments?.getLong("noteId")?.takeIf { it > 0 }
            val presetCategoryId = entry.arguments?.getLong("categoryId")?.takeIf { it > 0 }
            NoteEditScreen(
                noteId = id,
                initialCategoryId = presetCategoryId,
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
