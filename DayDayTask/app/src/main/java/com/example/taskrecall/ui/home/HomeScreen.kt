package com.example.taskrecall.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.taskrecall.domain.DateTimeUtils
import com.example.taskrecall.domain.TaskType
import com.example.taskrecall.ui.components.ReorderableTaskList
import com.example.taskrecall.ui.components.TaskCard
import com.example.taskrecall.ui.components.TaskRecallCalendar
import com.example.taskrecall.ui.components.displayName
import java.time.LocalDate

/**
 * 主页。
 *
 * 结构：
 * - 日历（可点未来日期，点月份标题可快速跳年/月/日）
 * - 标签页：今日任务 / 日常 / 周常 / 长期任务
 * - 默认停留在今日任务
 *
 * 日常、周常、长期不再纵向堆叠在主页，而是各自一个标签页。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onSelectDate: (LocalDate) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onGoToday: () -> Unit,
    onToggleTask: (com.example.taskrecall.data.db.TaskEntity) -> Unit,
    onOpenTask: (Long) -> Unit,
    onAddTask: (LocalDate) -> Unit,
    onReorderTasks: (String, List<Long>) -> Unit,
    onOpenSettings: () -> Unit,
) {
    var nextWeekCollapsed by rememberSaveable { mutableStateOf(false) }
    var selectedTab by rememberSaveable { mutableIntStateOf(TAB_TASKS) }

    val handleSelectDate: (LocalDate) -> Unit = { date ->
        onSelectDate(date)
        selectedTab = TAB_TASKS
    }
    val handleGoToday: () -> Unit = {
        onGoToday()
        selectedTab = TAB_TASKS
    }

    val taskTabLabel = if (uiState.isTodaySelected) "今日任务" else "当天任务"

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "DayDayTask",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = DateTimeUtils.formatFullDate(uiState.today),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "设置",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onAddTask(uiState.selectedDate) },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("添加任务") },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "calendar") {
                TaskRecallCalendar(
                    month = uiState.visibleMonth,
                    selectedDate = uiState.selectedDate,
                    today = uiState.today,
                    markedDates = uiState.markedDates,
                    onSelectDate = handleSelectDate,
                    onPreviousMonth = onPreviousMonth,
                    onNextMonth = onNextMonth,
                    onToday = handleGoToday,
                )
            }

            item(key = "tabs") {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.primary,
                ) {
                    listOf(taskTabLabel, "日常", "周常", "长期任务").forEachIndexed { index, label ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    text = label,
                                    fontWeight = if (selectedTab == index) {
                                        FontWeight.SemiBold
                                    } else {
                                        FontWeight.Normal
                                    },
                                )
                            },
                        )
                    }
                }
            }

            when (selectedTab) {
                TAB_TASKS -> {
                    if (uiState.isTodaySelected) {
                        item(key = "today_header") {
                            SectionHeader(title = "今日任务", count = uiState.todayTasks.size)
                        }
                        if (uiState.todayTasks.isEmpty()) {
                            item(key = "today_empty") {
                                EmptyHint("今天暂时没有到期的任务")
                            }
                        } else {
                            item(key = "today_list") {
                                ReorderableTaskList(
                                    items = uiState.todayTasks,
                                    itemKey = { it.task.id },
                                    onReorder = { ids ->
                                        onReorderTasks(TaskSectionKey.TODAY, ids)
                                    },
                                ) { row, cardModifier, handleModifier ->
                                    TaskCard(
                                        title = row.task.shortText,
                                        type = row.task.type,
                                        status = row.status,
                                        metaText = row.task.dueAt?.let { due ->
                                            if (row.task.type == TaskType.LONGTERM_A) {
                                                "截止 ${DateTimeUtils.formatTodayDue(due)}"
                                            } else {
                                                DateTimeUtils.formatTodayDue(due)
                                            }
                                        },
                                        onOpen = { onOpenTask(row.task.id) },
                                        onToggle = { onToggleTask(row.task) },
                                        modifier = cardModifier,
                                        dragHandleModifier = handleModifier,
                                    )
                                }
                            }
                        }

                        if (uiState.showNextWeek) {
                            item(key = "next_week_divider") {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { nextWeekCollapsed = !nextWeekCollapsed }
                                        .padding(vertical = 8.dp),
                                ) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(bottom = 8.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant,
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = "近一周要结束的任务",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.weight(1f),
                                        )
                                        Icon(
                                            imageVector = if (nextWeekCollapsed) {
                                                Icons.Default.ExpandMore
                                            } else {
                                                Icons.Default.ExpandLess
                                            },
                                            contentDescription = if (nextWeekCollapsed) "展开" else "收起",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }

                            item(key = "next_week_tasks") {
                                AnimatedVisibility(
                                    visible = !nextWeekCollapsed,
                                    enter = fadeIn() + expandVertically(),
                                    exit = fadeOut() + shrinkVertically(),
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        if (uiState.nextWeekTasks.isEmpty()) {
                                            EmptyHint("未来 7 天没有要结束的任务")
                                        } else {
                                            ReorderableTaskList(
                                                items = uiState.nextWeekTasks,
                                                itemKey = { it.task.id },
                                                onReorder = { ids ->
                                                    onReorderTasks(TaskSectionKey.NEXT_WEEK, ids)
                                                },
                                            ) { row, cardModifier, handleModifier ->
                                                TaskCard(
                                                    title = row.task.shortText,
                                                    type = row.task.type,
                                                    status = row.status,
                                                    metaText = row.task.dueAt?.let {
                                                        "截止 ${DateTimeUtils.formatNextWeekDue(it)}"
                                                    },
                                                    onOpen = { onOpenTask(row.task.id) },
                                                    onToggle = { onToggleTask(row.task) },
                                                    modifier = cardModifier,
                                                    dragHandleModifier = handleModifier,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        item(key = "selected_date_header") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = DateTimeUtils.formatFullDate(uiState.selectedDate),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f),
                                )
                                TextButton(onClick = handleGoToday) {
                                    Text("回到今天")
                                }
                            }
                        }
                        if (uiState.selectedDateItems.isEmpty()) {
                            item(key = "selected_date_empty") {
                                EmptyHint("这一天没有任务或日常/周常记录")
                            }
                        } else {
                            items(
                                uiState.selectedDateItems,
                                key = { "selected_${it.task.id}_${it.periodKey}" },
                            ) { row ->
                                TaskCard(
                                    title = row.task.shortText,
                                    type = row.task.type,
                                    status = row.status,
                                    metaText = selectedDateMeta(row),
                                    onOpen = { onOpenTask(row.task.id) },
                                    onToggle = {},
                                    statusEnabled = false,
                                    trailingText = row.status.displayName(),
                                )
                            }
                        }
                    }
                }

                TAB_DAILY -> {
                    item(key = "daily_header") {
                        TabIntro(
                            title = "日常",
                            subtitle = "每天一个状态，次日自动重新开始",
                            count = uiState.dailyTasks.size,
                        )
                    }
                    if (uiState.dailyTasks.isEmpty()) {
                        item(key = "daily_empty") {
                            EmptyHint("暂时没有设置日常/周常哦")
                        }
                    } else {
                        item(key = "daily_list") {
                            ReorderableTaskList(
                                items = uiState.dailyTasks,
                                itemKey = { it.task.id },
                                onReorder = { ids ->
                                    onReorderTasks(TaskSectionKey.DAILY, ids)
                                },
                            ) { row, cardModifier, handleModifier ->
                                TaskCard(
                                    title = row.task.shortText,
                                    type = row.task.type,
                                    status = row.status,
                                    metaText = "每天",
                                    onOpen = { onOpenTask(row.task.id) },
                                    onToggle = { onToggleTask(row.task) },
                                    modifier = cardModifier,
                                    dragHandleModifier = handleModifier,
                                )
                            }
                        }
                    }
                }

                TAB_WEEKLY -> {
                    item(key = "weekly_header") {
                        TabIntro(
                            title = "周常",
                            subtitle = "每周一个状态，周一自动重新开始",
                            count = uiState.weeklyTasks.size,
                        )
                    }
                    if (uiState.weeklyTasks.isEmpty()) {
                        item(key = "weekly_empty") {
                            EmptyHint("暂时没有设置日常/周常哦")
                        }
                    } else {
                        item(key = "weekly_list") {
                            ReorderableTaskList(
                                items = uiState.weeklyTasks,
                                itemKey = { it.task.id },
                                onReorder = { ids ->
                                    onReorderTasks(TaskSectionKey.WEEKLY, ids)
                                },
                            ) { row, cardModifier, handleModifier ->
                                TaskCard(
                                    title = row.task.shortText,
                                    type = row.task.type,
                                    status = row.status,
                                    metaText = "每周一重新开始",
                                    onOpen = { onOpenTask(row.task.id) },
                                    onToggle = { onToggleTask(row.task) },
                                    modifier = cardModifier,
                                    dragHandleModifier = handleModifier,
                                )
                            }
                        }
                    }
                }

                TAB_LONGTERM -> {
                    item(key = "longterm_header") {
                        TabIntro(
                            title = "长期任务",
                            subtitle = "时限型会在截止日进入今日任务",
                            count = uiState.longtermATasks.size + uiState.longtermBTasks.size,
                        )
                    }

                    item(key = "longterm_a_subheader") {
                        SubSectionHeader("时限型")
                    }
                    if (uiState.longtermATasks.isEmpty()) {
                        item(key = "longterm_a_empty") {
                            EmptyHint("还没有时限型长期任务")
                        }
                    } else {
                        item(key = "longterm_a_list") {
                            ReorderableTaskList(
                                items = uiState.longtermATasks,
                                itemKey = { it.task.id },
                                onReorder = { ids ->
                                    onReorderTasks(TaskSectionKey.LONGTERM_A, ids)
                                },
                            ) { row, cardModifier, handleModifier ->
                                TaskCard(
                                    title = row.task.shortText,
                                    type = row.task.type,
                                    status = row.status,
                                    metaText = row.task.dueAt?.let {
                                        "截止 ${DateTimeUtils.formatMonthDay(it)}"
                                    },
                                    onOpen = { onOpenTask(row.task.id) },
                                    onToggle = { onToggleTask(row.task) },
                                    modifier = cardModifier,
                                    dragHandleModifier = handleModifier,
                                )
                            }
                        }
                    }

                    item(key = "longterm_b_subheader") {
                        SubSectionHeader("目标型")
                    }
                    if (uiState.longtermBTasks.isEmpty()) {
                        item(key = "longterm_b_empty") {
                            EmptyHint("还没有目标型长期任务")
                        }
                    } else {
                        item(key = "longterm_b_list") {
                            ReorderableTaskList(
                                items = uiState.longtermBTasks,
                                itemKey = { it.task.id },
                                onReorder = { ids ->
                                    onReorderTasks(TaskSectionKey.LONGTERM_B, ids)
                                },
                            ) { row, cardModifier, handleModifier ->
                                TaskCard(
                                    title = row.task.shortText,
                                    type = row.task.type,
                                    status = row.status,
                                    metaText = "目标",
                                    onOpen = { onOpenTask(row.task.id) },
                                    onToggle = { onToggleTask(row.task) },
                                    modifier = cardModifier,
                                    dragHandleModifier = handleModifier,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private const val TAB_TASKS = 0
private const val TAB_DAILY = 1
private const val TAB_WEEKLY = 2
private const val TAB_LONGTERM = 3

@Composable
private fun SectionHeader(title: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        if (count > 0) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TabIntro(title: String, subtitle: String, count: Int) {
    Column(
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        SectionHeader(title = title, count = count)
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SubSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
    )
}

@Composable
private fun EmptyHint(text: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
        )
    }
}

private fun selectedDateMeta(row: TaskRowUi): String = when (row.historyKind) {
    HistoryKind.DAILY -> "日常 · 当天记录"
    HistoryKind.WEEKLY -> "周常 · 本周记录"
    HistoryKind.DUE, null -> when (row.task.type) {
        TaskType.LONGTERM_A -> "时限型 · 当天到期"
        else -> "当天到期"
    }
}
