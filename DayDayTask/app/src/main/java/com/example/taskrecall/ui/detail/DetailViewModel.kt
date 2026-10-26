package com.example.taskrecall.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskrecall.data.db.TaskEntity
import com.example.taskrecall.data.repository.TaskRepository
import com.example.taskrecall.domain.PeriodKey
import com.example.taskrecall.domain.StatusResolver
import com.example.taskrecall.domain.TaskStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class DetailUiState(
    val isLoading: Boolean = true,
    val task: TaskEntity? = null,
    val status: TaskStatus = TaskStatus.PENDING,
    val deleted: Boolean = false,
)

/**
 * 详情页 ViewModel。
 *
 * 状态同样通过 StatusResolver 动态计算，不把逾期 FAILED 写库。
 */
@HiltViewModel
class DetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val taskRepository: TaskRepository,
) : ViewModel() {

    private val taskId: Long = savedStateHandle.get<Long>("taskId") ?: 0L
    private val now = MutableStateFlow(System.currentTimeMillis())

    val uiState: StateFlow<DetailUiState> = combine(
        taskRepository.observeTask(taskId),
        taskRepository.observeAllCompletions(),
        now,
    ) { task, completions, currentTime ->
        if (task == null) {
            DetailUiState(isLoading = false, task = null, deleted = true)
        } else {
            val periodKey = PeriodKey.forTaskAt(task.type, currentTime)
            val completion = completions.firstOrNull {
                it.taskId == task.id && it.periodKey == periodKey
            }
            DetailUiState(
                isLoading = false,
                task = task,
                status = StatusResolver.displayStatus(task, completion, currentTime),
                deleted = task.deletedAt != null,
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DetailUiState(),
    )

    init {
        viewModelScope.launch {
            while (isActive) {
                now.value = System.currentTimeMillis()
                delay(30_000)
            }
        }
    }

    fun delete() {
        viewModelScope.launch {
            taskRepository.softDelete(taskId)
        }
    }
}
