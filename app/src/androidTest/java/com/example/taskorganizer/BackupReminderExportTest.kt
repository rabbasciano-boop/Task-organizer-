package com.example.taskorganizer

import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.example.taskorganizer.ui.theme.OrganizerTheme
import com.example.taskorganizer.ui.theme.TaskOrganizerTheme
import java.io.File
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class BackupReminderExportTest {
    @get:Rule val compose = createComposeRule()

    @Test fun successfulExportRecordsOnlyAfterFileIsWritten() = withStore { store, root ->
        val output = File(root, "test-backup.tob")
        val recorded = AtomicBoolean(false)
        val handled = AtomicBoolean(false)
        compose.setContent {
            TaskOrganizerTheme(OrganizerTheme.CARTA_DORATA) {
                BackupScreen(
                    store = store, onBack = {}, pendingImportUri = null, onChooseImport = {}, onImportUriHandled = {},
                    pendingExportUri = Uri.fromFile(output), onCreateExport = { _, _ -> },
                    takeExportPassword = { "password-prova".toCharArray() }, onExportUriHandled = { handled.set(true) },
                    onExportSucceeded = { recorded.set(output.length() > 0); true }, onImported = { _, _ -> }
                )
            }
        }
        compose.waitUntil(30_000) { handled.get() }
        assertTrue(recorded.get())
        assertTrue(output.length() > 0)
        assertEquals("Prova", store.decodePasswordProtectedBackup(output.readBytes(), "password-prova".toCharArray()).tasks.single().title)
        assertTrue(output.delete())
    }

    @Test fun failedExportDoesNotRecordBackup() = withStore { store, _ ->
        val recorded = AtomicBoolean(false)
        val handled = AtomicBoolean(false)
        compose.setContent {
            TaskOrganizerTheme(OrganizerTheme.CARTA_DORATA) {
                BackupScreen(
                    store = store, onBack = {}, pendingImportUri = null, onChooseImport = {}, onImportUriHandled = {},
                    pendingExportUri = Uri.parse("content://com.example.taskorganizer.missing/file"), onCreateExport = { _, _ -> },
                    takeExportPassword = { "password-prova".toCharArray() }, onExportUriHandled = { handled.set(true) },
                    onExportSucceeded = { recorded.set(true); true }, onImported = { _, _ -> }
                )
            }
        }
        compose.waitUntil(30_000) { handled.get() }
        assertFalse(recorded.get())
    }

    private fun withStore(block: (EncryptedTaskStore, File) -> Unit) {
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        val token = UUID.randomUUID().toString().replace("-", "")
        val root = File(base.cacheDir, "backup-reminder-test-$token")
        check(root.mkdir())
        val isolated = object : ContextWrapper(base) {
            override fun getApplicationContext(): Context = this
            override fun getFilesDir(): File = root
        }
        val store = EncryptedTaskStore(isolated, keyAlias = "backup_reminder_test_$token")
        try {
            store.replaceArchive(TaskArchive(tasks = listOf(OrganizerTask(title = "Prova", date = "2026-11-28"))))
            block(store, root)
        } finally { store.clearAll(); root.delete() }
    }
}
