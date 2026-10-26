package com.example.taskrecall.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskrecall.data.export.ExportResult
import com.example.taskrecall.data.export.ExportManager
import com.example.taskrecall.data.export.ImportManager
import com.example.taskrecall.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 设置页 ViewModel。
 *
 * 规格十三：近一周区开关、导出、导入、关于/版本。
 * 导入必须先经过覆盖确认，确认逻辑在 UI 里，执行入口在本 ViewModel。
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val exportManager: ExportManager,
    private val importManager: ImportManager,
) : ViewModel() {

    val showNextWeek: StateFlow<Boolean> = settingsRepository.showNextWeek.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = true,
    )

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    private val _pendingImport = MutableStateFlow<Uri?>(null)
    val pendingImport: StateFlow<Uri?> = _pendingImport.asStateFlow()

    private val _shareRequest = MutableStateFlow<ExportResult?>(null)
    val shareRequest: StateFlow<ExportResult?> = _shareRequest.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun setShowNextWeek(show: Boolean) {
        viewModelScope.launch {
            settingsRepository.setShowNextWeek(show)
        }
    }

    fun export() {
        if (_isExporting.value || _isImporting.value) return
        viewModelScope.launch {
            _isExporting.value = true
            runCatching { exportManager.export() }
                .onSuccess { result ->
                    _shareRequest.value = result
                    _message.value = "备份已生成：${result.taskCount} 个任务，${result.imageCount} 张图片"
                }
                .onFailure { error ->
                    _message.value = "导出失败：${error.message ?: "未知错误"}"
                }
            _isExporting.value = false
        }
    }

    fun requestImport(uri: Uri) {
        _pendingImport.value = uri
    }

    fun cancelImport() {
        _pendingImport.value = null
    }

    fun confirmImport() {
        val uri = _pendingImport.value ?: return
        if (_isImporting.value) return
        _pendingImport.value = null
        viewModelScope.launch {
            _isImporting.value = true
            runCatching { importManager.importFrom(uri) }
                .onSuccess { result ->
                    _message.value =
                        "导入完成：${result.taskCount} 个任务，${result.completionCount} 条记录，${result.imageCount} 张图片"
                }
                .onFailure { error ->
                    _message.value = "导入失败：${error.message ?: "文件无法读取"}"
                }
            _isImporting.value = false
        }
    }

    fun consumeShareRequest() {
        _shareRequest.value = null
    }

    fun consumeMessage() {
        _message.value = null
    }
}
