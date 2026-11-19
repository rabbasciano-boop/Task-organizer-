package com.example.taskorganizer

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.KeyStore
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class OrganizerTask(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val date: String,
    val time: String? = null,
    val endTime: String? = null,
    val category: String = "Personale",
    val categoryIcon: String? = null,
    val priority: String = "Media",
    val note: String = "",
    val completed: Boolean = false,
    val reminderEnabled: Boolean = true,
    val reminderMinutesBefore: Int = 15,
    val reminderRepeatMinutes: Int = 0,
    val alarmEnabled: Boolean = false,
    val cardColor: String = "default",
    val examStatus: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val deletedAt: Long = 0L
)

data class OrganizerListItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val listType: String = "todo",
    val amountCents: Long? = null,
    val completed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class TaskArchive(
    val tasks: List<OrganizerTask> = emptyList(),
    val customCategories: List<String> = emptyList(),
    val customCategoryIcons: Map<String, String> = emptyMap(),
    val deletedTasks: List<OrganizerTask> = emptyList(),
    val listItems: List<OrganizerListItem> = emptyList()
)

/** The UI must distinguish a new empty archive from an existing archive that cannot be read. */
sealed interface TaskArchiveLoadResult {
    data class Loaded(val archive: TaskArchive) : TaskArchiveLoadResult
    data class Unreadable(val error: Exception) : TaskArchiveLoadResult
}

/** Stores the organizer only in the app-private folder, encrypted with an Android Keystore key. */
class EncryptedTaskStore(context: Context) {
    private val appContext = context.applicationContext
    private val dataFile = File(appContext.filesDir, "task-organizer.enc")
    private val atomicFile = AtomicFile(dataFile)

    fun load(): TaskArchive = synchronized(archiveLock) { loadUnlocked() }

    fun loadSafely(): TaskArchiveLoadResult = try {
        TaskArchiveLoadResult.Loaded(loadAndPruneExpiredTrash())
    } catch (error: Exception) {
        TaskArchiveLoadResult.Unreadable(error)
    }

    /**
     * Applies a small change to the latest archive while holding the same lock used by the widget.
     * This avoids writing an old in-memory copy over a change made from a widget.
     */
    fun update(transform: (TaskArchive) -> TaskArchive): TaskArchive = synchronized(archiveLock) {
        val updated = transform(loadUnlocked())
        saveUnlocked(updated)
        updated
    }

    /** Removes items that have remained in the recycle bin for longer than 30 days. */
    private fun loadAndPruneExpiredTrash(now: Long = System.currentTimeMillis()): TaskArchive = synchronized(archiveLock) {
        val archive = loadUnlocked()
        val retained = archive.deletedTasks.filter { task ->
            val deletedAt = task.deletedAt.takeIf { it > 0L } ?: now
            now - deletedAt < TRASH_RETENTION_MILLIS
        }
        if (retained.size == archive.deletedTasks.size) return archive
        return archive.copy(deletedTasks = retained).also(::saveUnlocked)
    }

    /**
     * Used only after the person explicitly chooses to start again from the recovery screen.
     * The unreadable encrypted file is copied first and is never silently replaced.
     */
    fun preserveUnreadableArchiveAndClear(): Boolean = synchronized(archiveLock) {
        if (!dataFile.exists()) return true
        return try {
            val recoveryFile = File(
                dataFile.parentFile,
                "$RECOVERY_PREFIX${System.currentTimeMillis()}.enc"
            )
            dataFile.copyTo(recoveryFile)
            // Keep one recovery copy only: it is a last-resort safeguard, not a permanent archive.
            recoveryFiles().filter { it != recoveryFile }.forEach(File::delete)
            atomicFile.delete()
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun loadUnlocked(): TaskArchive {
        // Never convert an unreadable existing archive into an empty list:
        // otherwise a later save could overwrite the user's real data.
        if (!dataFile.exists()) return TaskArchive()
        return atomicFile.openRead().use { decode(decrypt(it.readBytes())) }
    }

    fun save(
        tasks: List<OrganizerTask>,
        customCategories: List<String>,
        customCategoryIcons: Map<String, String>,
        deletedTasks: List<OrganizerTask> = emptyList(),
        listItems: List<OrganizerListItem> = emptyList()
    ) = synchronized(archiveLock) {
        saveUnlocked(TaskArchive(tasks, customCategories, customCategoryIcons, deletedTasks, listItems))
    }

    private fun saveUnlocked(archive: TaskArchive) {
        val stream = atomicFile.startWrite()
        try {
            stream.write(
                encrypt(
                    encode(
                        archive.tasks,
                        archive.customCategories,
                        archive.customCategoryIcons,
                        archive.deletedTasks,
                        archive.listItems
                    )
                )
            )
            stream.flush()
            stream.fd.sync()
            atomicFile.finishWrite(stream)
        } catch (error: Exception) {
            atomicFile.failWrite(stream)
            throw error
        }
    }

    /** Called only after an explicit confirmed deletion from the account screen. */
    fun clear() {
        synchronized(archiveLock) {
            atomicFile.delete()
            recoveryFiles().forEach(File::delete)
            KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.deleteEntry(KEY_ALIAS)
        }
    }

    private fun recoveryFiles(): List<File> = appContext.filesDir.listFiles()
        ?.filter { it.isFile && it.name.startsWith(RECOVERY_PREFIX) && it.name.endsWith(".enc") }
        .orEmpty()

    private fun encode(
        tasks: List<OrganizerTask>,
        customCategories: List<String>,
        customCategoryIcons: Map<String, String>,
        deletedTasks: List<OrganizerTask>,
        listItems: List<OrganizerListItem>
    ): ByteArray {
        val items = JSONArray()
        tasks.forEach { task -> items.put(taskJson(task)) }
        val deletedItems = JSONArray()
        deletedTasks.forEach { task -> deletedItems.put(taskJson(task)) }
        val listEntries = JSONArray()
        listItems.forEach { item ->
            listEntries.put(
                JSONObject()
                    .put("id", item.id)
                    .put("title", item.title)
                    .put("listType", item.listType)
                    .put("amountCents", item.amountCents ?: -1L)
                    .put("completed", item.completed)
                    .put("createdAt", item.createdAt)
            )
        }
        val categories = JSONArray()
        customCategories.distinct().forEach { categories.put(it) }
        val categoryIcons = JSONObject()
        customCategoryIcons.forEach { (category, icon) -> categoryIcons.put(category, icon) }
        return JSONObject().put("version", 6).put("tasks", items).put("deletedTasks", deletedItems).put("listItems", listEntries).put("customCategories", categories).put("customCategoryIcons", categoryIcons).toString().toByteArray(Charsets.UTF_8)
    }

    private fun decode(bytes: ByteArray): TaskArchive {
        val archive = JSONObject(bytes.toString(Charsets.UTF_8))
        fun tasksFrom(items: JSONArray, deleted: Boolean = false): List<OrganizerTask> = List(items.length()) { index ->
            val item = items.getJSONObject(index)
            OrganizerTask(
                id = item.getString("id"),
                title = item.getString("title"),
                date = item.getString("date"),
                time = item.optString("time").ifBlank { null },
                endTime = item.optString("endTime").ifBlank { null },
                category = item.optString("category", "Personale"),
                categoryIcon = item.optString("categoryIcon").ifBlank { null },
                priority = item.optString("priority", "Media"),
                note = item.optString("note"),
                completed = item.optBoolean("completed"),
                reminderEnabled = item.optBoolean("reminderEnabled", true),
                reminderMinutesBefore = item.optInt("reminderMinutesBefore", 15),
                reminderRepeatMinutes = item.optInt("reminderRepeatMinutes", 0),
                alarmEnabled = item.optBoolean("alarmEnabled", false),
                cardColor = item.optString("cardColor", "default"),
                examStatus = item.optString("examStatus").ifBlank { null },
                createdAt = item.optLong("createdAt", System.currentTimeMillis()),
                deletedAt = if (deleted) item.optLong("deletedAt", System.currentTimeMillis()) else 0L
            )
        }
        val tasks = tasksFrom(archive.getJSONArray("tasks"))
        val deletedTasks = archive.optJSONArray("deletedTasks")?.let { tasksFrom(it, deleted = true) }.orEmpty()
        val listItems = archive.optJSONArray("listItems")?.let { entries ->
            List(entries.length()) { index ->
                val item = entries.getJSONObject(index)
                OrganizerListItem(
                    id = item.getString("id"),
                    title = item.getString("title"),
                    listType = item.optString("listType", "todo"),
                    amountCents = item.optLong("amountCents", -1L).takeIf { it >= 0 },
                    completed = item.optBoolean("completed"),
                    createdAt = item.optLong("createdAt", System.currentTimeMillis())
                )
            }
        }.orEmpty()
        val customCategories = archive.optJSONArray("customCategories")?.let { categories ->
            List(categories.length()) { index -> categories.optString(index) }.filter { it.isNotBlank() }
        }.orEmpty()
        val customCategoryIcons = archive.optJSONObject("customCategoryIcons")?.let { icons ->
            buildMap {
                val keys = icons.keys()
                while (keys.hasNext()) {
                    val category = keys.next()
                    icons.optString(category).takeIf { it.isNotBlank() }?.let { put(category, it) }
                }
            }
        }.orEmpty()
        return TaskArchive(tasks, customCategories, customCategoryIcons, deletedTasks, listItems)
    }

    private fun taskJson(task: OrganizerTask): JSONObject = JSONObject()
        .put("id", task.id)
        .put("title", task.title)
        .put("date", task.date)
        .put("time", task.time ?: "")
        .put("endTime", task.endTime ?: "")
        .put("category", task.category)
        .put("categoryIcon", task.categoryIcon ?: "")
        .put("priority", task.priority)
        .put("note", task.note)
        .put("completed", task.completed)
        .put("reminderEnabled", task.reminderEnabled)
        .put("reminderMinutesBefore", task.reminderMinutesBefore)
        .put("reminderRepeatMinutes", task.reminderRepeatMinutes)
        .put("alarmEnabled", task.alarmEnabled)
        .put("cardColor", task.cardColor)
        .put("examStatus", task.examStatus ?: "")
        .put("createdAt", task.createdAt)
        .put("deletedAt", task.deletedAt)

    private fun encrypt(plainText: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val encrypted = cipher.doFinal(plainText)
        return byteArrayOf(cipher.iv.size.toByte()) + cipher.iv + encrypted
    }

    private fun decrypt(encryptedFile: ByteArray): ByteArray {
        require(encryptedFile.isNotEmpty()) { "Archivio vuoto" }
        val ivSize = encryptedFile.first().toInt() and 0xFF
        require(ivSize in 12..32 && encryptedFile.size > ivSize + 1) { "Archivio non valido" }
        val iv = encryptedFile.copyOfRange(1, ivSize + 1)
        val encrypted = encryptedFile.copyOfRange(ivSize + 1, encryptedFile.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, iv))
        return cipher.doFinal(encrypted)
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }

    private companion object {
        const val KEY_ALIAS = "task_organizer_local_key"
        const val RECOVERY_PREFIX = "task-organizer-unreadable-"
        const val TRASH_RETENTION_MILLIS = 30L * 24 * 60 * 60 * 1000
        val archiveLock = Any()
    }
}
