package com.example.taskrecall.data.db

import androidx.room.TypeConverter
import com.example.taskrecall.domain.TaskStatus
import com.example.taskrecall.domain.TaskType

/** Room 枚举转换：存枚举名，便于导出 JSON 与数据库排查。 */
class Converters {

    @TypeConverter
    fun fromTaskType(value: TaskType): String = value.name

    @TypeConverter
    fun toTaskType(value: String): TaskType = TaskType.valueOf(value)

    @TypeConverter
    fun fromTaskStatus(value: TaskStatus): String = value.name

    @TypeConverter
    fun toTaskStatus(value: String): TaskStatus = TaskStatus.valueOf(value)
}
