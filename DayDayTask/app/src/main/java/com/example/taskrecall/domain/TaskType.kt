package com.example.taskrecall.domain

import kotlinx.serialization.Serializable

/**
 * 规格：五种任务类型。
 *
 * 注意：枚举名会直接写入 Room 的 type 列，也会写入导出 JSON，
 * 因此不要随意重命名。
 */
@Serializable
enum class TaskType {
    /** 单次任务：有 dueAt，可精确到分钟。 */
    SINGLE,

    /** 时限型长期：有截止时间，只在截止日当天进入今日区，同时常驻长期区。 */
    LONGTERM_A,

    /** 目标型长期：无时限，手动完成，只出现在长期区目标型子区。 */
    LONGTERM_B,

    /** 日常：按天记录，次日自动回到 PENDING。 */
    DAILY,

    /** 周常：按 ISO 周记录，周一自动回到 PENDING。 */
    WEEKLY,
}
