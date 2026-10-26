package com.example.taskrecall.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskrecall.data.export.ImportManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface IncomingImportState {
    data object Idle : IncomingImportState
    data class Confirm(val uri: Uri) : IncomingImportState
    data object Importing : IncomingImportState
    data class Success(
        val taskCount: Int,
        val completionCount: Int,
        val imageCount: Int,
    ) : IncomingImportState

    data class Error(val message: String) : IncomingImportState
}

/**
 * 处理“从文件管理器/微信直接打开 zip”的导入流程。
 *
 * 规格十二：导入是覆盖导入，必须先提示“将覆盖当前数据”，
 * 用户确认后才交给 ImportManager。
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val importManager: ImportManager,
) : ViewModel() {

    private val _importState = MutableStateFlow<IncomingImportState>(IncomingImportState.Idle)
    val importState: StateFlow<IncomingImportState> = _importState.asStateFlow()

    fun offerImport(uri: Uri) {
        if (_importState.value is IncomingImportState.Importing) return
        _importState.value = IncomingImportState.Confirm(uri)
    }

    fun confirmImport() {
        val state = _importState.value as? IncomingImportState.Confirm ?: return
        _importState.value = IncomingImportState.Importing
        viewModelScope.launch {
            runCatching { importManager.importFrom(state.uri) }
                .onSuccess { result ->
                    _importState.value = IncomingImportState.Success(
                        taskCount = result.taskCount,
                        completionCount = result.completionCount,
                        imageCount = result.imageCount,
                    )
                }
                .onFailure { error ->
                    _importState.value = IncomingImportState.Error(
                        error.message ?: "导入失败，文件可能不是有效的备份",
                    )
                }
        }
    }

    fun cancelImport() {
        if (_importState.value is IncomingImportState.Importing) return
        _importState.value = IncomingImportState.Idle
    }

    fun dismissImportResult() {
        _importState.value = IncomingImportState.Idle
    }
}
