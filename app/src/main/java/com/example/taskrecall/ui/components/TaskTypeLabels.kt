package com.example.taskrecall.ui.components

import com.example.taskrecall.domain.TaskStatus
import com.example.taskrecall.domain.TaskType

/** 规格五种类型在 UI 中的中文名称。 */
fun TaskType.displayName(): String = when (this) {
    TaskType.SINGLE -> "单次任务"
    TaskType.LONGTERM_A -> "时限型长期"
    TaskType.LONGTERM_B -> "目标型长期"
    TaskType.DAILY -> "日常"
    TaskType.WEEKLY -> "周常"
}

/** 添加页中每种类型的一句话说明。 */
fun TaskType.description(): String = when (this) {
    TaskType.SINGLE -> "指定日期，完成一次"
    TaskType.LONGTERM_A -> "有截止时间，截止日进入今日区"
    TaskType.LONGTERM_B -> "没有时限，手动完成"
    TaskType.DAILY -> "每天一次，次日重新开始"
    TaskType.WEEKLY -> "每周一次，周一重新开始"
}

fun TaskStatus.displayName(): String = when (this) {
    TaskStatus.PENDING -> "未完成"
    TaskStatus.DONE -> "已完成"
    TaskStatus.FAILED -> "未完成（失败）"
}
