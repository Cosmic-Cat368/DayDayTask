package com.example.taskrecall.ui.archive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskrecall.data.db.ArchivedTaskWithCompletions
import com.example.taskrecall.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class ArchiveViewModel @Inject constructor(
    taskRepository: TaskRepository,
) : ViewModel() {

    val archivedTasks: StateFlow<List<ArchivedTaskWithCompletions>> =
        taskRepository.observeArchivedTasks().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )
}
