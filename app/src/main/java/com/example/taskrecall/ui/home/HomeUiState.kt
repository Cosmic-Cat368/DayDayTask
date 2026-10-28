package com.example.taskrecall.ui.home

import com.example.taskrecall.data.db.TaskEntity
import com.example.taskrecall.domain.TaskStatus
import com.example.taskrecall.domain.TaskType
import java.time.LocalDate
import java.time.YearMonth

/** 主页中一行任务需要的展示数据。 */
data class TaskRowUi(
    val task: TaskEntity,
    val status: TaskStatus,
    val periodKey: String,
    val historyKind: HistoryKind? = null,
)

/** 已真删除的日常/周常在历史日期中的只读记录。 */
data class ArchivedRecordUi(
    val archiveId: Long,
    val title: String,
    val type: TaskType,
    val status: TaskStatus,
    val periodKey: String,
)

enum class HistoryKind {
    DUE,
    DAILY,
    WEEKLY,
}

/**
 * 每个可拖动分区的稳定 key。
 * 同一任务在不同区拥有独立顺序，例如 LONGTERM_A 的今日区和长期区。
 */
object TaskSectionKey {
    const val TODAY = "today"
    const val NEXT_WEEK = "next_week"
    const val DAILY = "daily"
    const val WEEKLY = "weekly"
    const val LONGTERM_A = "longterm_a"
    const val LONGTERM_B = "longterm_b"
}

/**
 * 主页完整状态。
 * 日历、今日/指定日期、日常、周常、长期、近一周都由同一个 UiState 驱动。
 */
data class HomeUiState(
    val today: LocalDate = LocalDate.now(),
    val selectedDate: LocalDate = LocalDate.now(),
    val visibleMonth: YearMonth = YearMonth.now(),
    val showNextWeek: Boolean = true,
    val isLoading: Boolean = true,
    val markedDates: Set<LocalDate> = emptySet(),
    val dailyTasks: List<TaskRowUi> = emptyList(),
    val weeklyTasks: List<TaskRowUi> = emptyList(),
    val todayTasks: List<TaskRowUi> = emptyList(),
    val nextWeekTasks: List<TaskRowUi> = emptyList(),
    val longtermATasks: List<TaskRowUi> = emptyList(),
    val longtermBTasks: List<TaskRowUi> = emptyList(),
    /** 选中非今天时，当天到期的任务及日常/周常记录。 */
    val selectedDateItems: List<TaskRowUi> = emptyList(),
    /** 已删除日常/周常在该日期保留下来的只读归档记录。 */
    val archivedDateItems: List<ArchivedRecordUi> = emptyList(),
) {
    val isTodaySelected: Boolean
        get() = selectedDate == today
}
