package com.example.taskrecall.domain

import kotlinx.serialization.Serializable

/**
 * 规格：三态状态。
 *
 * PENDING -> DONE -> FAILED -> PENDING 由圆点点击循环。
 * 逾期造成的 FAILED 只在显示时动态计算，不写入数据库。
 */
@Serializable
enum class TaskStatus {
    PENDING,
    DONE,
    FAILED,
}
