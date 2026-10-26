package com.example.taskrecall.domain

import com.example.taskrecall.data.db.TaskEntity
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PeriodKeyTest {

    @Test
    fun dailyKey_usesIsoLocalDate() {
        assertEquals("2026-10-25", PeriodKey.daily(LocalDate.of(2026, 10, 25)))
    }

    @Test
    fun weeklyKey_usesIsoWeekAndMondayBoundary() {
        // 2025-12-29 是周一，属于 ISO 2026-W01。
        assertEquals("2026-W01", PeriodKey.weekly(LocalDate.of(2025, 12, 29)))
        // 2025-12-28 是周日，仍属于 2025-W52。
        assertEquals("2025-W52", PeriodKey.weekly(LocalDate.of(2025, 12, 28)))
        // 同一周任意一天都得到相同键。
        assertEquals(
            PeriodKey.weekly(LocalDate.of(2026, 1, 4)),
            PeriodKey.weekly(LocalDate.of(2026, 1, 5).minusDays(1)),
        )
    }

    @Test
    fun singleAndLongtermUseFixedOnceKey() {
        val now = 1_700_000_000_000L
        assertEquals(PeriodKey.ONCE, PeriodKey.forTaskAt(TaskType.SINGLE, now))
        assertEquals(PeriodKey.ONCE, PeriodKey.forTaskAt(TaskType.LONGTERM_A, now))
        assertEquals(PeriodKey.ONCE, PeriodKey.forTaskAt(TaskType.LONGTERM_B, now))
    }
}

class StatusResolverTest {

    private fun task(
        type: TaskType,
        dueAt: Long? = null,
    ) = TaskEntity(
        id = 1,
        type = type,
        shortText = "测试任务",
        createdAt = 0,
        dueAt = dueAt,
    )

    @Test
    fun overdueSingleWithoutCompletion_isDynamicallyFailed() {
        val now = 1_000_000L
        val status = StatusResolver.displayStatus(
            task = task(TaskType.SINGLE, dueAt = now - 1),
            completion = null,
            now = now,
        )
        assertEquals(TaskStatus.FAILED, status)
    }

    @Test
    fun completedOverdueSingle_staysDone() {
        val now = 1_000_000L
        val completion = object : CompletionLike {
            override val status = TaskStatus.DONE
        }
        val status = StatusResolver.displayStatus(
            task = task(TaskType.SINGLE, dueAt = now - 1),
            completion = completion,
            now = now,
        )
        assertEquals(TaskStatus.DONE, status)
    }

    @Test
    fun longtermAOverdue_isFailed_dailyIsNot() {
        val now = 1_000_000L
        assertEquals(
            TaskStatus.FAILED,
            StatusResolver.displayStatus(task(TaskType.LONGTERM_A, now - 1), null, now),
        )
        assertEquals(
            TaskStatus.PENDING,
            StatusResolver.displayStatus(task(TaskType.DAILY, now - 1), null, now),
        )
    }

    @Test
    fun longtermBAndFutureSingle_arePending() {
        val now = 1_000_000L
        assertEquals(
            TaskStatus.PENDING,
            StatusResolver.displayStatus(task(TaskType.LONGTERM_B), null, now),
        )
        assertEquals(
            TaskStatus.PENDING,
            StatusResolver.displayStatus(task(TaskType.SINGLE, now + 1), null, now),
        )
    }

    @Test
    fun baseStatusCycle_isPinDoneFailedPin() {
        assertEquals(TaskStatus.DONE, StatusResolver.nextBaseStatus(TaskStatus.PENDING))
        assertEquals(TaskStatus.FAILED, StatusResolver.nextBaseStatus(TaskStatus.DONE))
        assertEquals(TaskStatus.PENDING, StatusResolver.nextBaseStatus(TaskStatus.FAILED))
    }
}

class MarkdownImagesTest {

    @Test
    fun relativeImagesAreExtractedAndRewrittenOnlyInMemory() {
        val markdown = """
            # 标题

            ![图](images/abc.jpg)

            ![第二张](images/def.png "说明")
        """.trimIndent()

        val paths = MarkdownImages.referencedPaths(markdown)
        assertEquals(listOf("images/abc.jpg", "images/def.png"), paths)

        val rewritten = MarkdownImages.forMarkwon(markdown)
        assertTrue(rewritten.contains("taskrecall://local/images/abc.jpg"))
        assertTrue(rewritten.contains("taskrecall://local/images/def.png"))
        assertTrue(!rewritten.contains("](images/"))
    }
}
