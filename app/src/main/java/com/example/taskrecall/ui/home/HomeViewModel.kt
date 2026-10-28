package com.example.taskrecall.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskrecall.data.db.ArchivedTaskWithCompletions
import com.example.taskrecall.data.db.CompletionEntity
import com.example.taskrecall.data.db.TaskEntity
import com.example.taskrecall.data.db.TaskOrderEntity
import com.example.taskrecall.data.repository.SettingsRepository
import com.example.taskrecall.data.repository.TaskRepository
import com.example.taskrecall.domain.DateTimeUtils
import com.example.taskrecall.domain.PeriodKey
import com.example.taskrecall.domain.StatusResolver
import com.example.taskrecall.domain.TaskStatus
import com.example.taskrecall.domain.TaskType
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 主页 ViewModel。
 *
 * 所有板块都由“任务 + 完成记录 + 近一周开关 + 选中日期 + 当前时间”聚合得出。
 * 当前时间每 30 秒推进一次，因此逾期红色不需要任何后台任务，也会自动出现。
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    private val selectedDate = MutableStateFlow(LocalDate.now())
    private val visibleMonth = MutableStateFlow(YearMonth.now())
    private val now = MutableStateFlow(System.currentTimeMillis())

    private val contentFlow = combine(
        taskRepository.observeAllActiveTasks(),
        taskRepository.observeAllCompletions(),
        taskRepository.observeTaskOrders(),
        taskRepository.observeArchivedTasks(),
        settingsRepository.showNextWeek,
    ) { tasks, completions, orders, archived, showNextWeek ->
        HomeContent(tasks, completions, orders, archived, showNextWeek)
    }

    val uiState: StateFlow<HomeUiState> = combine(
        contentFlow,
        selectedDate,
        visibleMonth,
        now,
    ) { content, selected, month, currentTime ->
        buildState(content, selected, month, currentTime)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(),
    )

    init {
        viewModelScope.launch {
            while (isActive) {
                now.value = System.currentTimeMillis()
                delay(30_000)
            }
        }
    }

    fun selectDate(date: LocalDate) {
        selectedDate.value = date
        visibleMonth.value = YearMonth.from(date)
    }

    fun previousMonth() {
        visibleMonth.update { it.minusMonths(1) }
    }

    fun nextMonth() {
        visibleMonth.update { it.plusMonths(1) }
    }

    fun goToToday() {
        val today = DateTimeUtils.toLocalDate(now.value)
        selectedDate.value = today
        visibleMonth.value = YearMonth.from(today)
    }

    /** 今日/日常/周常/长期区的圆点点击。历史区不会调用。 */
    fun toggleStatus(task: TaskEntity) {
        viewModelScope.launch {
            taskRepository.toggleStatus(task, System.currentTimeMillis())
        }
    }

    /**
     * 历史日期视图的圆点点击。
     * 使用该行在选中日期/周对应的 periodKey，而不是当前周期。
     */
    fun toggleSelectedDateTask(row: TaskRowUi) {
        val today = DateTimeUtils.toLocalDate(now.value)
        // 只允许修改过去日期；未来日期保持只读。
        if (selectedDate.value >= today) return
        viewModelScope.launch {
            taskRepository.toggleStatusForPeriod(row.task, row.periodKey)
        }
    }

    /**
     * 某个区拖拽结束后的新顺序。
     * 只有该区任务 id 会写入，不影响其它区，也不影响任务本身的时间字段。
     */
    fun reorderTasks(sectionKey: String, orderedTaskIds: List<Long>) {
        viewModelScope.launch {
            taskRepository.reorderSection(sectionKey, orderedTaskIds)
        }
    }

    private fun buildState(
        content: HomeContent,
        selected: LocalDate,
        month: YearMonth,
        currentTime: Long,
    ): HomeUiState {
        val today = DateTimeUtils.toLocalDate(currentTime)
        val todayStart = DateTimeUtils.startOfDay(today)
        val todayEnd = DateTimeUtils.endOfDay(today)
        val weekEnd = DateTimeUtils.endOfDay(today.plusDays(7))
        val currentConsumptions = content.completions.associateBy { it.taskId to it.periodKey }

        fun row(task: TaskEntity, periodKey: String): TaskRowUi {
            val completion = currentConsumptions[task.id to periodKey]
            return TaskRowUi(
                task = task,
                status = StatusResolver.displayStatus(task, completion, currentTime),
                periodKey = periodKey,
            )
        }

        val todayRows = content.tasks
            .asSequence()
            .filter { task ->
                (task.type == TaskType.SINGLE || task.type == TaskType.LONGTERM_A) &&
                    task.dueAt != null &&
                    task.dueAt in todayStart..todayEnd
            }
            .map { task -> row(task, PeriodKey.ONCE) }
            .toList()

        val nextWeekRows = content.tasks
            .asSequence()
            .filter { task ->
                task.type == TaskType.SINGLE &&
                    task.dueAt != null &&
                    task.dueAt > todayEnd &&
                    task.dueAt <= weekEnd
            }
            .map { task -> row(task, PeriodKey.ONCE) }
            .toList()

        val dailyKey = PeriodKey.daily(today)
        val weeklyKey = PeriodKey.weekly(today)

        val dailyRows = content.tasks
            .filter {
                it.type == TaskType.DAILY &&
                    (it.startAt == null || it.startAt <= todayEnd)
            }
            .map { task -> row(task, dailyKey) }

        val weeklyRows = content.tasks
            .filter {
                it.type == TaskType.WEEKLY &&
                    (it.startAt == null || it.startAt <= todayEnd)
            }
            .map { task -> row(task, weeklyKey) }

        val longtermA = content.tasks
            .filter { it.type == TaskType.LONGTERM_A }
            .map { task -> row(task, PeriodKey.ONCE) }

        val longtermB = content.tasks
            .filter { it.type == TaskType.LONGTERM_B }
            .map { task -> row(task, PeriodKey.ONCE) }

        // 用户拖拽过的区按持久化顺序；没拖拽过则保持原来的时间/创建顺序。
        val orderLookup = content.orders.associate {
            (it.sectionKey to it.taskId) to it.sortOrder
        }

        fun ordered(
            rows: List<TaskRowUi>,
            sectionKey: String,
            fallback: Comparator<TaskRowUi>,
        ): List<TaskRowUi> = rows.sortedWith(
            compareBy<TaskRowUi> {
                orderLookup[sectionKey to it.task.id] ?: Int.MAX_VALUE
            }
                .then(fallback)
                .thenBy { it.task.id },
        )

        val orderedToday = ordered(
            todayRows,
            TaskSectionKey.TODAY,
            compareBy<TaskRowUi> { it.task.dueAt ?: Long.MAX_VALUE },
        )
        val orderedNextWeek = ordered(
            nextWeekRows,
            TaskSectionKey.NEXT_WEEK,
            compareBy<TaskRowUi> { it.task.dueAt ?: Long.MAX_VALUE },
        )
        val orderedDaily = ordered(
            dailyRows,
            TaskSectionKey.DAILY,
            compareBy<TaskRowUi> { it.task.createdAt },
        )
        val orderedWeekly = ordered(
            weeklyRows,
            TaskSectionKey.WEEKLY,
            compareBy<TaskRowUi> { it.task.createdAt },
        )
        val orderedLongtermA = ordered(
            longtermA,
            TaskSectionKey.LONGTERM_A,
            compareBy<TaskRowUi> { it.task.dueAt ?: Long.MAX_VALUE }
                .thenBy { it.task.createdAt },
        )
        val orderedLongtermB = ordered(
            longtermB,
            TaskSectionKey.LONGTERM_B,
            compareBy<TaskRowUi> { it.task.createdAt },
        )

        // 日历着色只认 SINGLE，与完成状态无关。
        val markedDates = content.tasks
            .asSequence()
            .filter { it.type == TaskType.SINGLE && it.dueAt != null }
            .mapNotNull { task -> task.dueAt?.let { DateTimeUtils.toLocalDate(it) } }
            .toSet()

        val selectedDateItems = if (selected != today) {
            buildSelectedDateItems(content, selected, currentConsumptions, currentTime)
        } else {
            emptyList()
        }
        val archivedDateItems = if (selected != today) {
            buildArchivedDateItems(content, selected)
        } else {
            emptyList()
        }

        return HomeUiState(
            today = today,
            selectedDate = selected,
            visibleMonth = month,
            showNextWeek = content.showNextWeek,
            isLoading = false,
            markedDates = markedDates,
            dailyTasks = orderedDaily,
            weeklyTasks = orderedWeekly,
            todayTasks = orderedToday,
            nextWeekTasks = orderedNextWeek,
            longtermATasks = orderedLongtermA,
            longtermBTasks = orderedLongtermB,
            selectedDateItems = selectedDateItems,
            archivedDateItems = archivedDateItems,
        )
    }

    private fun buildArchivedDateItems(
        content: HomeContent,
        selected: LocalDate,
    ): List<ArchivedRecordUi> {
        val dailyKey = PeriodKey.daily(selected)
        val weeklyKey = PeriodKey.weekly(selected)

        return content.archived.flatMap { archived ->
            val periodKey = when (archived.task.type) {
                TaskType.DAILY -> dailyKey
                TaskType.WEEKLY -> weeklyKey
                else -> return@flatMap emptyList()
            }
            val completion = archived.completions.firstOrNull {
                it.periodKey == periodKey && it.status != TaskStatus.PENDING
            } ?: return@flatMap emptyList()
            listOf(
                ArchivedRecordUi(
                    archiveId = archived.task.id,
                    title = archived.task.shortText,
                    type = archived.task.type,
                    status = completion.status,
                    periodKey = periodKey,
                ),
            )
        }.sortedBy { it.title }
    }

    private fun buildSelectedDateItems(
        content: HomeContent,
        selected: LocalDate,
        completionIndex: Map<Pair<Long, String>, CompletionEntity>,
        currentTime: Long,
    ): List<TaskRowUi> {
        val dayStart = DateTimeUtils.startOfDay(selected)
        val dayEnd = DateTimeUtils.endOfDay(selected)
        val dailyKey = PeriodKey.daily(selected)
        val weeklyKey = PeriodKey.weekly(selected)

        val dueRows = content.tasks
            .asSequence()
            .filter { task ->
                (task.type == TaskType.SINGLE || task.type == TaskType.LONGTERM_A) &&
                    task.dueAt != null &&
                    task.dueAt in dayStart..dayEnd
            }
            .sortedBy { it.dueAt }
            .map { task ->
                TaskRowUi(
                    task = task,
                    status = StatusResolver.displayStatus(
                        task = task,
                        completion = completionIndex[task.id to PeriodKey.ONCE],
                        now = currentTime,
                    ),
                    periodKey = PeriodKey.ONCE,
                    historyKind = HistoryKind.DUE,
                )
            }
            .toList()

        val dailyRows = content.tasks
            .asSequence()
            .filter {
                it.type == TaskType.DAILY &&
                    (it.startAt == null || it.startAt <= dayEnd)
            }
            .map { task ->
                val completion = completionIndex[task.id to dailyKey]
                TaskRowUi(
                    task = task,
                    status = completion?.status ?: TaskStatus.PENDING,
                    periodKey = dailyKey,
                    historyKind = HistoryKind.DAILY,
                )
            }
            .sortedBy { it.task.createdAt }
            .toList()

        val weeklyRows = content.tasks
            .asSequence()
            .filter {
                it.type == TaskType.WEEKLY &&
                    (it.startAt == null || it.startAt <= dayEnd)
            }
            .map { task ->
                val completion = completionIndex[task.id to weeklyKey]
                TaskRowUi(
                    task = task,
                    status = completion?.status ?: TaskStatus.PENDING,
                    periodKey = weeklyKey,
                    historyKind = HistoryKind.WEEKLY,
                )
            }
            .sortedBy { it.task.createdAt }
            .toList()

        return dueRows + dailyRows + weeklyRows
    }

    private data class HomeContent(
        val tasks: List<TaskEntity>,
        val completions: List<CompletionEntity>,
        val orders: List<TaskOrderEntity>,
        val archived: List<ArchivedTaskWithCompletions>,
        val showNextWeek: Boolean,
    )
}
