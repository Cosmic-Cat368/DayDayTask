package com.example.taskrecall.domain

import com.example.taskrecall.data.db.TaskEntity

/**
 * 规格十一：逾期状态动态计算。
 *
 * 这个函数是 UI 与导出共用的唯一入口：
 * - 有完成记录且不是 PENDING，直接返回记录状态；
 * - 没有完成记录时，SINGLE / LONGTERM_A 且 dueAt 早于 now，动态返回 FAILED；
 * - 其余返回 PENDING。
 *
 * 注意：这里不写数据库。逾期只是显示结果。
 */
object StatusResolver {

    fun baseStatus(completion: CompletionLike?): TaskStatus =
        completion?.status ?: TaskStatus.PENDING

    fun displayStatus(
        task: TaskEntity,
        completion: CompletionLike?,
        now: Long,
    ): TaskStatus {
        val base = baseStatus(completion)
        if (base != TaskStatus.PENDING) return base

        val dueAt = task.dueAt
        val overdue = when (task.type) {
            TaskType.SINGLE,
            TaskType.LONGTERM_A,
            -> dueAt != null && dueAt < now

            TaskType.LONGTERM_B,
            TaskType.DAILY,
            TaskType.WEEKLY,
            -> false
        }
        return if (overdue) TaskStatus.FAILED else TaskStatus.PENDING
    }

    /**
     * 圆点点击时的数据库状态循环。
     *
     * 使用数据库中的“基础状态”，而不是动态显示状态：
     * 逾期但未点过的任务显示红色，但它的基础状态仍是 PENDING，
     * 因此点一下会直接变成 DONE，而不是陷在动态 FAILED 里无法完成。
     */
    fun nextBaseStatus(base: TaskStatus): TaskStatus = when (base) {
        TaskStatus.PENDING -> TaskStatus.DONE
        TaskStatus.DONE -> TaskStatus.FAILED
        TaskStatus.FAILED -> TaskStatus.PENDING
    }
}

/**
 * 只依赖 status 的最小接口，便于测试时构造伪完成记录。
 */
interface CompletionLike {
    val status: TaskStatus
}
