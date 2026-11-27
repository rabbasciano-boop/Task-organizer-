package com.example.taskorganizer

import android.content.Context
import android.content.ContextWrapper
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.UUID

class ProfileStoreIsolationTest {
    @Test fun archivesAndPortableBackupsStaySeparateAndLegacyCopyDoesNotOverwrite() {
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        val token = UUID.randomUUID().toString().replace("-", "")
        val root = File(base.filesDir, "isolated-profile-test-$token")
        check(root.mkdir())
        val context = object : ContextWrapper(base) {
            override fun getApplicationContext(): Context = this
            override fun getFilesDir(): File = root
            override fun getSharedPreferences(name: String, mode: Int) = base.getSharedPreferences("isolated-$token-$name", mode)
        }
        val alias = "task_organizer_isolation_test_$token"
        val legacy = EncryptedTaskStore(context, keyAlias = alias)
        val original = EncryptedTaskStore(context, keyAlias = alias, profileId = "account_original")
        val other = EncryptedTaskStore(context, keyAlias = alias, profileId = "account_other")
        val guest = EncryptedTaskStore(context, keyAlias = alias, profileId = LocalProfiles.GUEST)
        try {
            val task = OrganizerTask(title = "Attività test", date = "2026-11-27")
            val source = TaskArchive(tasks = listOf(task))
            val otherData = TaskArchive(tasks = listOf(task.copy(id = UUID.randomUUID().toString(), title = "Altro profilo")))
            legacy.replaceArchive(source)
            other.replaceArchive(otherData)
            assertTrue(runCatching { LegacyProfileAssociation.associate(context, "account_other", legacy, other) }.isFailure)
            assertTrue(LegacyProfileAssociation.needsConfirmation(context, "account_original"))
            LegacyProfileAssociation.associate(context, "account_original", legacy, original)
            assertEquals(source, legacy.load())
            assertEquals(source, original.load())
            assertEquals(otherData, other.load())
            assertTrue(!guest.hasArchive())
            assertTrue(guest.load().tasks.isEmpty())
            assertTrue(!LegacyProfileAssociation.needsConfirmation(context, "account_original"))
            assertTrue(runCatching { LegacyProfileAssociation.associate(context, "account_other", legacy, other) }.isFailure)

            assertTrue(runCatching { legacy.copyIntoEmptyProfile(other) }.isFailure)
            assertEquals(otherData, other.load())
            assertEquals(source, legacy.load())

            val password = "password-test-sicura".toCharArray()
            val backup = original.exportPasswordProtectedBackup(password)
            assertEquals(source, guest.decodePasswordProtectedBackup(backup, password))
            assertTrue(runCatching { guest.decodePasswordProtectedBackup(backup, "password-errata".toCharArray()) }.isFailure)
            assertTrue(!guest.hasArchive())
            assertTrue(guest.load().tasks.isEmpty())
            guest.importPasswordProtectedBackup(backup, password)
            assertEquals(source, guest.load())
            assertEquals(source, original.load())
            assertEquals(otherData, other.load())
            other.clearAll()
            assertTrue(!other.hasArchive())
            assertEquals(source, original.load())
            assertEquals(source, legacy.load())
            LegacyProfileAssociation.deleteAssociatedLegacy(context, "account_original", legacy)
            assertTrue(!legacy.hasArchive())
            assertEquals(source, original.load())
        } finally {
            listOf(legacy, original, other, guest).forEach { it.clearAll() }
            listOf("account_original", "account_other", LocalProfiles.GUEST).forEach {
                File(File(root, "organizer-profiles"), it).delete()
            }
            File(root, "organizer-profiles").delete()
            root.delete()
            listOf("task_organizer_profile_association", "task_organizer_profile", "task_organizer_profile_account_original").forEach {
                base.deleteSharedPreferences("isolated-$token-$it")
            }
        }
    }
}
