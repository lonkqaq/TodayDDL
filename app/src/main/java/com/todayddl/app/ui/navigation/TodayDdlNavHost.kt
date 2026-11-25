package com.todayddl.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.todayddl.app.ui.editor.TaskEditorScreen
import com.todayddl.app.ui.home.HomeScreen
import com.todayddl.app.ui.settings.SettingsScreen

object Routes {
    const val HOME = "home"
    const val EDITOR_BASE = "editor"
    const val EDITOR_ROUTE = "editor?taskId={taskId}"
    const val SETTINGS = "settings"

    fun editorRoute(taskId: Long? = null): String =
        if (taskId != null && taskId > 0L) "$EDITOR_BASE?taskId=$taskId" else "$EDITOR_BASE?taskId=-1"
}

@Composable
fun TodayDdlNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.HOME
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                onAddTaskClick = { navController.navigate(Routes.editorRoute()) },
                onEditTaskClick = { taskId -> navController.navigate(Routes.editorRoute(taskId)) },
                onSettingsClick = { navController.navigate(Routes.SETTINGS) }
            )
        }

        composable(
            route = Routes.EDITOR_ROUTE,
            arguments = listOf(
                navArgument("taskId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) {
            TaskEditorScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
