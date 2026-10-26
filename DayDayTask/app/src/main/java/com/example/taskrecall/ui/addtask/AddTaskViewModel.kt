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
 * 添加任务 ViewModel。
 *
 * 规格八：日期可预填、可改。预填日期来自主页选中的日期。
 * 规格十六：图片入库前复制到私有目录，Markdown 里只写相对路径。
 */
@HiltViewModel
class AddTaskViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val taskRepository: TaskRepository,
    private val imageStorage: ImageStorage,
) : ViewModel() {

    val prefillDate: LocalDate = runCatching {
        savedStateHandle.get<String>("date")?.let(LocalDate::parse)
    }.getOrNull() ?: LocalDate.now()

    private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveState: StateFlow<SaveState> = _saveState.asStateFlow()

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

        val now = System.currentTimeMillis()
        val dueAt = when (type) {
            TaskType.SINGLE,
            TaskType.LONGTERM_A,
            -> DateTimeUtils.atDateTime(dueDate, dueTime)

            TaskType.LONGTERM_B -> null

            TaskType.DAILY,
            TaskType.WEEKLY,
            -> null
        }
        val startAt = when (type) {
            TaskType.DAILY -> DateTimeUtils.startOfDay(startDate)
            // 周常的起始周按 ISO 周首周一存储。
            TaskType.WEEKLY -> DateTimeUtils.startOfDay(DateTimeUtils.startOfWeek(startDate))
            else -> null
        }

        val task = TaskEntity(
            type = type,
            shortText = title,
            detailMarkdown = detailMarkdown.trim().ifBlank { null },
            createdAt = now,
            dueAt = dueAt,
            startAt = startAt,
        )

        _saveState.value = SaveState.Saving
        viewModelScope.launch {
            runCatching { taskRepository.addTask(task) }
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
     * 立即复制的好处是实现最简单；代价是用户放弃编辑时可能留下未引用图片。
     * 规格未要求清理孤儿图片，这里在注释中说明。
     */
    suspend fun importImage(uri: Uri): String? = imageStorage.importImage(uri)

    fun consumeError() {
        if (_saveState.value is SaveState.Error) {
            _saveState.value = SaveState.Idle
        }
    }
}
