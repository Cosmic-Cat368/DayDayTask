package com.example.taskrecall.ui.addtask

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.taskrecall.domain.DateTimeUtils
import com.example.taskrecall.domain.TaskType
import com.example.taskrecall.ui.components.description
import com.example.taskrecall.ui.components.displayName
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.util.Locale
import kotlinx.coroutines.launch

/**
 * 规格八：添加任务页。
 *
 * 两个入口（主页 + / 先选日期）都走这个页面。
 * 简写必填；详细 Markdown 选填；类型五选一。
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddTaskScreen(
    viewModel: AddTaskViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    var type by rememberSaveable { mutableStateOf(TaskType.SINGLE) }
    var shortText by rememberSaveable { mutableStateOf("") }
    var detailField by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(""))
    }
    var dueDateText by rememberSaveable { mutableStateOf(viewModel.prefillDate.toString()) }
    var startDateText by rememberSaveable { mutableStateOf(viewModel.prefillDate.toString()) }
    var hasTime by rememberSaveable { mutableStateOf(false) }
    var hour by rememberSaveable { mutableIntStateOf(9) }
    var minute by rememberSaveable { mutableIntStateOf(0) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var imageImporting by remember { mutableStateOf(false) }

    val dueDate = LocalDate.parse(dueDateText)
    val startDate = LocalDate.parse(startDateText)
    val saveState by viewModel.saveState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            imageImporting = true
            val relativePath = viewModel.importImage(uri)
            imageImporting = false
            if (relativePath == null) {
                snackbarHostState.showSnackbar("图片导入失败，请换一张图片重试")
            } else {
                val insertion = "\n![图片]($relativePath)\n"
                val text = detailField.text
                val start = detailField.selection.start.coerceIn(0, text.length)
                val end = detailField.selection.end.coerceIn(start, text.length)
                val newText = text.replaceRange(start, end, insertion)
                detailField = TextFieldValue(
                    text = newText,
                    selection = TextRange(start + insertion.length),
                )
            }
        }
    }

    LaunchedEffect(saveState) {
        when (val state = saveState) {
            SaveState.Saved -> onSaved()
            is SaveState.Error -> {
                snackbarHostState.showSnackbar(state.message)
                viewModel.consumeError()
            }

            else -> Unit
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("添加任务") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                        )
                    }
                },
                actions = {
                    if (saveState is SaveState.Saving) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .size(22.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        TextButton(
                            onClick = {
                                viewModel.save(
                                    type = type,
                                    shortText = shortText,
                                    detailMarkdown = detailField.text,
                                    dueDate = dueDate,
                                    dueTime = if (hasTime) LocalTime.of(hour, minute) else null,
                                    startDate = startDate,
                                )
                            },
                        ) {
                            Text("保存", fontWeight = FontWeight.SemiBold)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "任务类型",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                TaskType.entries.forEach { option ->
                    FilterChip(
                        selected = type == option,
                        onClick = { type = option },
                        label = { Text(option.displayName()) },
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
            ) {
                Text(
                    text = type.description(),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }

            OutlinedTextField(
                value = shortText,
                onValueChange = { shortText = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("简写（必填）") },
                placeholder = { Text("例如：读完《小王子》") },
                singleLine = true,
            )

            if (type != TaskType.LONGTERM_B) {
                Text(
                    text = when (type) {
                        TaskType.SINGLE -> "到期日期"
                        TaskType.LONGTERM_A -> "截止日期"
                        TaskType.DAILY -> "起始日期"
                        TaskType.WEEKLY -> "起始周"
                        TaskType.LONGTERM_B -> ""
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )

                val isStartDate = type == TaskType.DAILY || type == TaskType.WEEKLY
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = if (isStartDate) {
                            DateTimeUtils.formatFullDate(startDate)
                        } else {
                            DateTimeUtils.formatFullDate(dueDate)
                        },
                    )
                }

                if (type == TaskType.WEEKLY) {
                    Text(
                        text = "周常按 ISO 周统计，周一为一周开始。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (type == TaskType.SINGLE || type == TaskType.LONGTERM_A) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "设置具体时间",
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(
                                text = if (hasTime) {
                                    String.format(Locale.UK, "%02d:%02d", hour, minute)
                                } else {
                                    "不设置时间时，按当天结束前完成"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = hasTime,
                            onCheckedChange = { hasTime = it },
                        )
                    }

                    if (hasTime) {
                        OutlinedButton(
                            onClick = { showTimePicker = true },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Default.Schedule, contentDescription = null)
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(String.format(Locale.UK, "时间：%02d:%02d", hour, minute))
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "详细说明（Markdown）",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    onClick = { imagePicker.launch("image/*") },
                    enabled = !imageImporting,
                ) {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = null,
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                    Text(if (imageImporting) "导入中…" else "插入图片")
                }
            }

            OutlinedTextField(
                value = detailField,
                onValueChange = { detailField = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                label = { Text("可写 Markdown，图片使用相对路径") },
                placeholder = { Text("支持标题、列表、粗体、链接和图片…") },
                textStyle = MaterialTheme.typography.bodyMedium,
            )

            Text(
                text = "图片会被复制到应用私有目录，Markdown 中只保存 images/ 相对路径。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showDatePicker) {
        AppDatePickerDialog(
            initialDate = if (type == TaskType.DAILY || type == TaskType.WEEKLY) startDate else dueDate,
            onDismiss = { showDatePicker = false },
            onConfirm = { picked ->
                if (type == TaskType.DAILY) {
                    startDateText = picked.toString()
                } else if (type == TaskType.WEEKLY) {
                    // 起始周统一显示为该周周一，与周期键规则一致。
                    startDateText = DateTimeUtils.startOfWeek(picked).toString()
                } else {
                    dueDateText = picked.toString()
                }
                showDatePicker = false
            },
        )
    }

    if (showTimePicker) {
        AppTimePickerDialog(
            initialHour = hour,
            initialMinute = minute,
            onDismiss = { showTimePicker = false },
            onConfirm = { pickedHour, pickedMinute ->
                hour = pickedHour
                minute = pickedMinute
                showTimePicker = false
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppDatePickerDialog(
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
) {
    val initialMillis = initialDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val millis = datePickerState.selectedDateMillis ?: initialMillis
                    // Material3 DatePicker 的选择值以 UTC 零点表示日期。
                    val picked = Instant.ofEpochMilli(millis)
                        .atZone(ZoneOffset.UTC)
                        .toLocalDate()
                    onConfirm(picked)
                },
            ) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    ) {
        DatePicker(state = datePickerState)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit,
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true,
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择时间") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                TimePicker(state = timePickerState)
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(timePickerState.hour, timePickerState.minute) }) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}
