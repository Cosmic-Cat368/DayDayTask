package com.example.taskrecall.domain

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

/**
 * 时间工具集中放在这里，避免 UI、导出、测试各自实现一套边界规则。
 */
object DateTimeUtils {

    fun toLocalDate(epochMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): LocalDate =
        Instant.ofEpochMilli(epochMillis).atZone(zoneId).toLocalDate()

    fun toLocalTime(epochMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): LocalTime =
        Instant.ofEpochMilli(epochMillis).atZone(zoneId).toLocalTime()

    fun startOfDay(date: LocalDate, zoneId: ZoneId = ZoneId.systemDefault()): Long =
        date.atStartOfDay(zoneId).toInstant().toEpochMilli()

    /**
     * 一天的最后毫秒。
     * 日期选择器允许“不设置具体时间”，此时 dueAt 落在当天 23:59:59.999，
     * 表示“这一天结束前完成即可”，也避免当天一整天被误判为逾期。
     */
    fun endOfDay(date: LocalDate, zoneId: ZoneId = ZoneId.systemDefault()): Long =
        date.atTime(LocalTime.MAX).atZone(zoneId).toInstant().toEpochMilli()

    fun atDateTime(
        date: LocalDate,
        time: LocalTime?,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): Long = if (time == null) {
        endOfDay(date, zoneId)
    } else {
        date.atTime(time).atZone(zoneId).toInstant().toEpochMilli()
    }

    fun monthRange(
        month: YearMonth,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): Pair<Long, Long> = startOfDay(month.atDay(1), zoneId) to endOfDay(month.atEndOfMonth(), zoneId)

    fun startOfWeek(date: LocalDate): LocalDate =
        date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    fun endOfWeek(date: LocalDate): LocalDate =
        date.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))

    fun nextWeekWindow(
        today: LocalDate,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): Pair<Long, Long> = endOfDay(today, zoneId) to endOfDay(today.plusDays(7), zoneId)

    /**
     * 没有填写具体分钟时，约定 dueAt 为当天 23:59:59.999。
     * 只有今日任务区需要显示分钟；这里用于判断是否显示“全天”。
     */
    fun isDateOnly(epochMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): Boolean {
        val time = toLocalTime(epochMillis, zoneId)
        return time.hour == 23 && time.minute == 59
    }

    fun formatMonthTitle(month: YearMonth): String =
        month.format(DateTimeFormatter.ofPattern("yyyy年M月", Locale.CHINA))

    fun formatMonthDay(epochMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): String =
        toLocalDate(epochMillis, zoneId).format(DateTimeFormatter.ofPattern("M月d日", Locale.CHINA))

    fun formatFullDate(epochMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): String =
        toLocalDate(epochMillis, zoneId).format(DateTimeFormatter.ofPattern("yyyy年M月d日", Locale.CHINA))

    fun formatDate(date: LocalDate): String =
        date.format(DateTimeFormatter.ofPattern("M月d日", Locale.CHINA))

    fun formatFullDate(date: LocalDate): String =
        date.format(DateTimeFormatter.ofPattern("yyyy年M月d日", Locale.CHINA))

    fun formatTime(epochMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): String =
        toLocalTime(epochMillis, zoneId).format(DateTimeFormatter.ofPattern("HH:mm", Locale.UK))

    /** 今日任务区：日期型任务显示“全天”，否则显示 HH:mm（规格：显示分钟）。 */
    fun formatTodayDue(epochMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): String =
        if (isDateOnly(epochMillis, zoneId)) "全天" else formatTime(epochMillis, zoneId)

    /** 近一周区：规格要求不显示分钟，这里只显示日期。 */
    fun formatNextWeekDue(epochMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): String =
        formatMonthDay(epochMillis, zoneId)

    fun formatDateTimeForEdit(epochMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): String {
        val dateTime = Instant.ofEpochMilli(epochMillis).atZone(zoneId).toLocalDateTime()
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.UK))
    }

    fun formatDateTime(dateTime: LocalDateTime): String =
        dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.UK))
}
