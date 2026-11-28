package com.example.taskorganizer

import android.content.Context
import android.content.ContextWrapper
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SubtaskBackupCompatibilityTest {
    @Test fun originalVersionSevenBackupRemainsReadable() = withStore { store ->
        val fixture = InstrumentationRegistry.getInstrumentation().context.assets.open("legacy_v7_synthetic.tob").use { it.readBytes() }
        val legacy = store.importPasswordProtectedBackup(fixture, "password-prova".toCharArray())
        assertEquals("Vecchia attività", legacy.tasks.single().title)
        assertEquals("Nota di prova", legacy.tasks.single().note)
        assertTrue(legacy.tasks.single().subtasks.isEmpty())
        assertEquals(legacy, store.load())
    }

    @Test fun backupsAndTrashRetainStepsAndLimitOversizedInput() = withStore { store ->
        val password = "password-prova".toCharArray()
        for (count in listOf(0, 1, 20, 25)) {
            val steps = (1..count).map { Subtask("step-$it", "Passaggio $it", it % 2 == 0) }
            val task = OrganizerTask(id = "task", title = "Prova", date = "2026-11-28", subtasks = steps)
            val archive = TaskArchive(tasks = listOf(task), deletedTasks = listOf(task.copy(id = "trash", deletedAt = 1234L)))
            store.replaceArchive(archive)
            val restored = store.decodePasswordProtectedBackup(store.exportPasswordProtectedBackup(password), password)
            val expected = steps.take(MAX_SUBTASKS)
            assertEquals(expected, restored.tasks.single().subtasks)
            assertEquals(expected, restored.deletedTasks.single().subtasks)
        }
    }

    @Test fun ignoresMalformedEmptyAndDuplicateSteps() {
        val input = JSONArray().put("not an object")
            .put(JSONObject().put("id", "one").put("title", "   "))
            .put(JSONObject().put("id", "one").put("title", "A".repeat(90)).put("done", true))
            .put(JSONObject().put("id", "one").put("title", "Duplicato"))
        val result = decodeSubtasks(input)
        assertEquals(1, result.size)
        assertEquals(80, result.single().title.length)
        assertTrue(result.single().done)
        assertFalse(result.single().id.isBlank())
    }

    private fun withStore(block: (EncryptedTaskStore) -> Unit) {
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        val token = UUID.randomUUID().toString().replace("-", "")
        val root = File(base.cacheDir, "subtask-test-$token")
        check(root.mkdir())
        val isolated = object : ContextWrapper(base) {
            override fun getApplicationContext(): Context = this
            override fun getFilesDir(): File = root
        }
        val store = EncryptedTaskStore(isolated, keyAlias = "subtask_test_$token")
        try { block(store) } finally { store.clearAll(); root.delete() }
    }
}
