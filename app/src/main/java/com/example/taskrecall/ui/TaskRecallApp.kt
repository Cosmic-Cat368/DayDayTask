package com.example.taskrecall.ui

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.taskrecall.ui.addtask.AddTaskScreen
import com.example.taskrecall.ui.detail.DetailScreen
import com.example.taskrecall.ui.home.HomeScreen
import com.example.taskrecall.ui.home.HomeViewModel
import com.example.taskrecall.ui.settings.SettingsScreen
import java.time.LocalDate

/**
 * 单 Activity 内的全部页面导航。
 *
 * 路由：
 * - home
 * - add?date=yyyy-MM-dd（日期可选，用于“先选日期再添加”）
 * - detail/{taskId}
 * - settings
 */
@Composable
fun TaskRecallApp(
    initialImportUri: Uri? = null,
    onImportUriConsumed: () -> Unit = {},
) {
    val navController = rememberNavController()
    val mainViewModel: MainViewModel = hiltViewModel()
    val importState by mainViewModel.importState.collectAsStateWithLifecycle()

    LaunchedEffect(initialImportUri) {
        if (initialImportUri != null) {
            mainViewModel.offerImport(initialImportUri)
            onImportUriConsumed()
        }
    }

    NavHost(
        navController = navController,
        startDestination = "home",
    ) {
        composable("home") {
            val homeViewModel: HomeViewModel = hiltViewModel()
            val state by homeViewModel.uiState.collectAsStateWithLifecycle()
            HomeScreen(
                uiState = state,
                onSelectDate = homeViewModel::selectDate,
                onPreviousMonth = homeViewModel::previousMonth,
                onNextMonth = homeViewModel::nextMonth,
                onGoToday = homeViewModel::goToToday,
                onToggleTask = homeViewModel::toggleStatus,
                onOpenTask = { taskId ->
                    navController.navigate("detail/$taskId")
                },
                onAddTask = { date ->
                    navController.navigate("add?date=${date}")
                },
                onReorderTasks = homeViewModel::reorderTasks,
                onOpenSettings = { navController.navigate("settings") },
            )
        }

        composable(
            route = "add?date={date}",
            arguments = listOf(
                navArgument("date") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) {
            AddTaskScreen(
                viewModel = hiltViewModel(),
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
            )
        }

        composable(
            route = "detail/{taskId}",
            arguments = listOf(
                navArgument("taskId") { type = NavType.LongType },
            ),
        ) {
            DetailScreen(
                viewModel = hiltViewModel(),
                onBack = { navController.popBackStack() },
            )
        }

        composable("settings") {
            SettingsScreen(
                viewModel = hiltViewModel(),
                onBack = { navController.popBackStack() },
            )
        }
    }

    IncomingImportDialogs(
        state = importState,
        onConfirm = mainViewModel::confirmImport,
        onCancel = mainViewModel::cancelImport,
        onDismissResult = mainViewModel::dismissImportResult,
    )
}

/**
 * 从外部 zip 打开时的全局覆盖确认。
 * 与设置页的导入确认文案保持一致。
 */
@Composable
private fun IncomingImportDialogs(
    state: IncomingImportState,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    onDismissResult: () -> Unit,
) {
    when (state) {
        IncomingImportState.Idle -> Unit

        is IncomingImportState.Confirm -> AlertDialog(
            onDismissRequest = onCancel,
            title = { Text("将覆盖当前数据") },
            text = {
                Text("检测到备份文件。导入会用备份中的任务、完成记录和图片替换当前数据，此操作不可撤销。")
            },
            confirmButton = {
                TextButton(onClick = onConfirm) { Text("确认覆盖") }
            },
            dismissButton = {
                TextButton(onClick = onCancel) { Text("取消") }
            },
        )

        IncomingImportState.Importing -> AlertDialog(
            onDismissRequest = {},
            title = { Text("正在导入") },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    CircularProgressIndicator()
                    Text("正在恢复任务、完成记录和图片…")
                }
            },
            confirmButton = {},
        )

        is IncomingImportState.Success -> AlertDialog(
            onDismissRequest = onDismissResult,
            title = { Text("导入完成") },
            text = {
                Text(
                    "已恢复 ${state.taskCount} 个任务、${state.completionCount} 条完成记录、${state.imageCount} 张图片。",
                )
            },
            confirmButton = {
                TextButton(onClick = onDismissResult) { Text("完成") }
            },
        )

        is IncomingImportState.Error -> AlertDialog(
            onDismissRequest = onDismissResult,
            title = { Text("导入失败") },
            text = { Text(state.message) },
            confirmButton = {
                TextButton(onClick = onDismissResult) { Text("知道了") }
            },
        )
    }
}
