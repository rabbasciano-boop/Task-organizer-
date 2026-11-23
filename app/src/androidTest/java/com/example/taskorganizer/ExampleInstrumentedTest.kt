package com.example.taskorganizer

import android.content.Context
import android.content.ContextWrapper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
@Ignore("Eseguire solo su emulatore o dispositivo dedicato: non sul telefono personale.")
class EncryptedTaskStoreInstrumentedTest {
    private fun isolatedStore(): Pair<Context, EncryptedTaskStore> {
        // The target context gives Android a real private directory. Only the dedicated subdirectory below is used.
        val baseContext = InstrumentationRegistry.getInstrumentation().targetContext
        val testContext = IsolatedTestContext(baseContext)
        testContext.filesDir.mkdirs()
        val store = EncryptedTaskStore(testContext, keyAlias = "task_organizer_test_key")
        store.clear()
        testContext.filesDir.listFiles()
            ?.filter { it.name.startsWith("task-organizer-unreadable-") }
            ?.forEach(File::delete)
        return testContext to store
    }

    private fun cleanUp(testContext: Context, store: EncryptedTaskStore) {
        store.clear()
        testContext.filesDir.listFiles()
            ?.filter { it.name.startsWith("task-organizer-unreadable-") }
            ?.forEach(File::delete)
    }

    @Test
    fun encryptsAndReadsBackAnArchive() {
        val (testContext, store) = isolatedStore()
        try {
            val task = OrganizerTask(title = "Test cifratura", date = "2026-11-18", time = "10:00")

            store.save(listOf(task), listOf("Studio"), mapOf("Studio" to "📚"))

            val loaded = store.load()
            assertEquals(listOf(task), loaded.tasks)
            assertEquals(listOf("Studio"), loaded.customCategories)
            assertEquals("📚", loaded.customCategoryIcons["Studio"])
        } finally {
            cleanUp(testContext, store)
        }
    }

    @Test
    fun unreadableArchiveIsPreservedBeforeStartingFresh() {
        val (testContext, store) = isolatedStore()
        try {
            File(testContext.filesDir, "task-organizer.enc").writeBytes(byteArrayOf(1, 2, 3, 4))

            assertTrue(store.loadSafely() is TaskArchiveLoadResult.Unreadable)
            assertTrue(store.preserveUnreadableArchiveAndClear())

            assertEquals(TaskArchive(), store.load())
            assertTrue(
                testContext.filesDir.listFiles()
                    ?.any { it.name.startsWith("task-organizer-unreadable-") } == true
            )
        } finally {
            cleanUp(testContext, store)
        }
    }

    @Test
    fun exportsAndImportsAPasswordProtectedBackup() {
        val (testContext, store) = isolatedStore()
        try {
            val task = OrganizerTask(title = "Backup test", date = "2026-12-01", recurrence = "weekly")
            store.save(listOf(task), listOf("Studio"), mapOf("Studio" to "📚"))

            val password = "password-di-prova".toCharArray()
            val backup = try {
                store.exportPasswordProtectedBackup(password)
            } finally {
                password.fill('\u0000')
            }
            store.clear()
            val importPassword = "password-di-prova".toCharArray()
            val restored = try {
                store.importPasswordProtectedBackup(backup, importPassword)
            } finally {
                importPassword.fill('\u0000')
            }

            assertEquals(listOf(task), restored.tasks)
            assertEquals(listOf("Studio"), restored.customCategories)
        } finally {
            cleanUp(testContext, store)
        }
    }

    private class IsolatedTestContext(base: Context) : ContextWrapper(base) {
        override fun getApplicationContext(): Context = this

        // The instrumentation package has its own private files directory, separate from the app's data.
        override fun getFilesDir(): File = baseContext.getDir("encrypted-task-store-test", Context.MODE_PRIVATE)
    }
}
