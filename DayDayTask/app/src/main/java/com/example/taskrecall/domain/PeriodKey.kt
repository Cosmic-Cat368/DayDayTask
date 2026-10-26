package com.example.taskrecall.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.IsoFields
import java.time.temporal.TemporalAdjusters
import java.util.Locale

/**
 * 规格：完成记录的周期键。
 *
 * - DAILY：yyyy-MM-dd
 * - WEEKLY：ISO 周，形如 2026-W42，周首为周一
 * - SINGLE / LONGTERM_A / LONGTERM_B：固定串 once
 *
 * 说明：规格写明格式 "yyyy-'W'ww" 且使用 Locale.UK。
 * 但跨年那一周（例如 2025-12-29 属于 2026-W01）如果用日历年份 yyyy，
 * 会出现 2025-W01 这种错误。为满足“ISO 周”的硬性要求，这里使用
 * ISO week-based-year（等价于 YYYY）来拼接，最终文本格式仍是 yyyy-Www。
 */
object PeriodKey {

    const val ONCE: String = "once"

    private val dailyFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.UK)

    /** 日常：yyyy-MM-dd。 */
    fun daily(date: LocalDate): String = date.format(dailyFormatter)

    /** 周常：ISO 周，周首周一，例如 2026-W01。 */
    fun weekly(date: LocalDate): String {
        val monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val weekBasedYear = monday.get(IsoFields.WEEK_BASED_YEAR)
        val week = monday.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
        return String.format(Locale.UK, "%04d-W%02d", weekBasedYear, week)
    }

    /**
     * 按任务类型和“当前时间”取周期键。
     * SINGLE / LONGTERM 固定 once；日常按当天；周常按当前 ISO 周。
     */
    fun forTaskAt(
        taskType: TaskType,
        instant: Long,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): String {
        val date = DateTimeUtils.toLocalDate(instant, zoneId)
        return when (taskType) {
            TaskType.DAILY -> daily(date)
            TaskType.WEEKLY -> weekly(date)
            TaskType.SINGLE,
            TaskType.LONGTERM_A,
            TaskType.LONGTERM_B,
            -> ONCE
        }
    }

    /**
     * 按任务类型和指定日期取周期键。
     * 历史记录查看时，用选中日期而不是今天。
     */
    fun forTaskOnDate(taskType: TaskType, date: LocalDate): String = when (taskType) {
        TaskType.DAILY -> daily(date)
        TaskType.WEEKLY -> weekly(date)
        TaskType.SINGLE,
        TaskType.LONGTERM_A,
        TaskType.LONGTERM_B,
        -> ONCE
    }
}
