package com.example.taskrecall.ui.addtask

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskrecall.data.db.TaskEntity
import com.example.taskrecall.data.repository.ImageStorage
import com.example.taskrecall.data.repository.TaskRepository
import com.example.taskrecall.domain.DateTimeUtils
import com.example.taskrecall.domain.TaskType
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface SaveState {
    data object Idle : SaveState
    data object Saving : SaveState
    data object Saved : SaveState
    data class Error(val message: String) : SaveState
}

/**
 * 添加 / 编辑任务 ViewModel。
 *
 * 路由带 taskId 时进入编辑模式：加载原任务，保存时更新而不是新增。
 * 为避免破坏已有完成记录，编辑模式下任务类型不可修改。
 */
@HiltViewModel
class AddTaskViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val taskRepository: TaskRepository,
    private val imageStorage: ImageStorage,
) : ViewModel() {

    private val editingTaskId: Long? = savedStateHandle.get<Long>("taskId")

    val isEditing: Boolean = editingTaskId != null

    private val _editingTask = MutableStateFlow<TaskEntity?>(null)
    val editingTask: StateFlow<TaskEntity?> = _editingTask.asStateFlow()

    val prefillDate: LocalDate = runCatching {
        savedStateHandle.get<String>("date")?.let(LocalDate::parse)
    }.getOrNull() ?: LocalDate.now()

    private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveState: StateFlow<SaveState> = _saveState.asStateFlow()

    init {
        if (editingTaskId != null) {
            viewModelScope.launch {
                _editingTask.value = taskRepository.getTask(editingTaskId)
            }
        }
    }

    fun save(
        type: TaskType,
        shortText: String,
        detailMarkdown: String,
        dueDate: LocalDate,
        dueTime: LocalTime?,
        startDate: LocalDate,
    ) {
        if (_saveState.value is SaveState.Saving) return
        val title = shortText.trim()
        if (title.isEmpty()) {
            _saveState.value = SaveState.Error("简写不能为空")
            return
        }

        val existing = _editingTask.value
        if (isEditing && existing == null) {
            _saveState.value = SaveState.Error("任务不存在或已被删除")
            return
        }

        val effectiveType = existing?.type ?: type
        val dueAt = when (effectiveType) {
            TaskType.SINGLE,
            TaskType.LONGTERM_A,
            -> DateTimeUtils.atDateTime(dueDate, dueTime)

            TaskType.LONGTERM_B -> null

            TaskType.DAILY,
            TaskType.WEEKLY,
            -> null
        }
        val startAt = when (effectiveType) {
            TaskType.DAILY -> DateTimeUtils.startOfDay(startDate)
            // 周常的起始周按 ISO 周首周一存储。
            TaskType.WEEKLY -> DateTimeUtils.startOfDay(DateTimeUtils.startOfWeek(startDate))
            else -> null
        }

        val now = System.currentTimeMillis()
        val task = existing?.copy(
            shortText = title,
            detailMarkdown = detailMarkdown.trim().ifBlank { null },
            dueAt = dueAt,
            startAt = startAt,
        ) ?: TaskEntity(
            type = effectiveType,
            shortText = title,
            detailMarkdown = detailMarkdown.trim().ifBlank { null },
            createdAt = now,
            dueAt = dueAt,
            startAt = startAt,
        )

        _saveState.value = SaveState.Saving
        viewModelScope.launch {
            runCatching {
                if (existing != null) {
                    taskRepository.updateTask(task)
                } else {
                    taskRepository.addTask(task)
                }
            }
                .onSuccess { _saveState.value = SaveState.Saved }
                .onFailure { throwable ->
                    _saveState.value = SaveState.Error(
                        throwable.message ?: "保存失败，请重试",
                    )
                }
        }
    }

    /**
     * 复制图片并返回 Markdown 相对路径；失败返回 null。
     */
    suspend fun importImage(uri: Uri): String? = imageStorage.importImage(uri)

    fun consumeError() {
        if (_saveState.value is SaveState.Error) {
            _saveState.value = SaveState.Idle
        }
    }
}
