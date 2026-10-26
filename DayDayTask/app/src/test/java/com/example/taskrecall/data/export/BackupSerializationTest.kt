package com.example.taskrecall.data.export

import com.example.taskrecall.domain.TaskStatus
import com.example.taskrecall.domain.TaskType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupSerializationTest {

    @Test
    fun backupJson_roundTripsTaskEnumsAndDeletedRecords() {
        val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }
        val backup = BackupBundle(
            exportedAt = 1_700_000_000_000L,
            tasks = listOf(
                BackupTask(
                    id = 7,
                    type = TaskType.LONGTERM_A,
                    shortText = "长期任务",
                    detailMarkdown = "![图](images/a.jpg)",
                    createdAt = 100,
                    dueAt = 200,
                    displayStatus = TaskStatus.FAILED,
                ),
                BackupTask(
                    id = 8,
                    type = TaskType.DAILY,
                    shortText = "日常",
                    createdAt = 101,
                    startAt = 50,
                    deletedAt = 300,
                ),
            ),
            completions = listOf(
                BackupCompletion(8, "2026-10-25", TaskStatus.DONE),
            ),
            orders = listOf(
                BackupOrder("today", 7, 0),
            ),
        )

        val encoded = json.encodeToString(backup)
        val decoded = json.decodeFromString<BackupBundle>(encoded)

        assertEquals(backup, decoded)
        assertTrue(encoded.contains("LONGTERM_A"))
        assertTrue(encoded.contains("FAILED"))
    }
}
