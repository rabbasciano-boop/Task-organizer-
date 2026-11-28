package com.example.taskorganizer

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.LocalDate
import java.security.KeyStore
import java.security.SecureRandom
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

data class Subtask(val id: String = UUID.randomUUID().toString(), val title: String, val done: Boolean = false)

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
    val deletedAt: Long = 0L,
    val recurrence: String = "none",
    val seriesId: String? = null,
    val recurrenceAnchorDate: String? = null,
    val subtasks: List<Subtask> = emptyList()
)

data class OrganizerListItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val listType: String = "todo",
    val amountCents: Long? = null,
    val completed: Boolean = false,
    /** todo, shopping, doing or done. Existing entries derive a safe default when read. */
    val kanbanStage: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

/** A worked time interval. Dates and hours use the same ISO/24-hour format as activities. */
data class WorkLogEntry(
    val id: String = UUID.randomUUID().toString(),
    val date: String,
    val startTime: String,
    val endTime: String,
    val breakMinutes: Int = 0,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class WorkShiftPreset(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val startTime: String,
    val endTime: String,
    val breakMinutes: Int = 0
)

data class WorkLogSettings(
    /** Stored as cents so money is never rounded by a floating-point calculation. */
    val hourlyRateCents: Long = 0L,
    val presets: List<WorkShiftPreset> = listOf(
        WorkShiftPreset(name = "Mattina", startTime = "09:00", endTime = "13:00"),
        WorkShiftPreset(name = "Pomeriggio", startTime = "14:00", endTime = "18:00"),
        WorkShiftPreset(name = "Giornata intera", startTime = "09:00", endTime = "18:00", breakMinutes = 60)
    )
)

/** A damaged imported entry must never make the whole encrypted archive unreadable. */
internal fun validWorkLogEntry(date: String, startTime: String, endTime: String): Boolean =
    runCatching { LocalDate.parse(date) }.isSuccess &&
        normalizeTime(startTime) != null && normalizeTime(endTime) != null

data class TaskArchive(
    val tasks: List<OrganizerTask> = emptyList(),
    val customCategories: List<String> = emptyList(),
    val customCategoryIcons: Map<String, String> = emptyMap(),
    val deletedTasks: List<OrganizerTask> = emptyList(),
    val listItems: List<OrganizerListItem> = emptyList(),
    val workLogEntries: List<WorkLogEntry> = emptyList(),
    val workLogSettings: WorkLogSettings = WorkLogSettings(),
    /** A running timer is persisted so elapsed time is correct after leaving or reopening the app. */
    val activeWorkTimerStartedAt: Long? = null
)

/** Keeps the newest item for each id so a malformed imported archive cannot break lazy lists. */
internal fun normalizeArchiveIds(archive: TaskArchive): TaskArchive {
    fun newestTasks(items: List<OrganizerTask>): List<OrganizerTask> = items
        .groupBy { it.id }
        .values
        .map { duplicates -> duplicates.maxWithOrNull(compareBy<OrganizerTask> { it.createdAt }.thenBy { it.deletedAt })!! }

    val activeTasks = newestTasks(archive.tasks)
    val deletedTasks = newestTasks(archive.deletedTasks)
        .filterNot { deleted -> activeTasks.any { active -> active.id == deleted.id } }
    val listItems = archive.listItems
        .groupBy { it.id }
        .values
        .map { duplicates -> duplicates.maxByOrNull { it.createdAt }!! }
    val workLogEntries = archive.workLogEntries
        .groupBy { it.id }
        .values
        .map { duplicates -> duplicates.maxByOrNull { it.createdAt }!! }
    return archive.copy(tasks = activeTasks, deletedTasks = deletedTasks, listItems = listItems, workLogEntries = workLogEntries)
}

/** The UI must distinguish a new empty archive from an existing archive that cannot be read. */
sealed interface TaskArchiveLoadResult {
    data class Loaded(val archive: TaskArchive) : TaskArchiveLoadResult
    data class Unreadable(val error: Exception) : TaskArchiveLoadResult
}

data class ArchiveRestoreOutcome(val safetyCopyCreated: Boolean)

/** Stores the organizer only in the app-private folder, encrypted with an Android Keystore key. */
class EncryptedTaskStore(context: Context, keyAlias: String = KEY_ALIAS, profileId: String? = null) {
    private val appContext = context.applicationContext
    private val keyAlias = if (profileId == null) keyAlias else "$keyAlias.$profileId"
    private val storageDir = if (profileId == null) appContext.filesDir else {
        require(profileId.matches(Regex("[a-z0-9_]{1,80}"))) { "Profilo non valido" }
        File(File(appContext.filesDir, "organizer-profiles"), profileId).apply {
            check(isDirectory || mkdirs()) { "Impossibile aprire l'archivio del profilo" }
        }
    }
    private val dataFile = File(storageDir, "task-organizer.enc")
    private val atomicFile = AtomicFile(dataFile)
    private val restoreSafetyFile = File(storageDir, "task-organizer-before-restore.enc")

    fun hasArchive(): Boolean = synchronized(archiveLock) {
        dataFile.exists() || File(dataFile.path + ".bak").exists() || File(dataFile.path + ".new").exists()
    }

    /** Copy without overwriting existing data; keep the original until explicitly removed. */
    internal fun copyIntoEmptyProfile(target: EncryptedTaskStore) = synchronized(archiveLock) {
        check(hasArchive()) { "Nessun archivio precedente da associare" }
        val source = loadUnlocked()
        if (target.hasArchive()) {
            check(target.loadUnlocked() == source) { "Il profilo contiene già dati: non verranno sovrascritti" }
        } else {
            target.saveUnlocked(source)
        }
        check(target.loadUnlocked() == source) { "Impossibile verificare la copia dell'archivio" }
    }

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

    /** Applies a conditional change without rewriting or recreating an archive for a stale action. */
    internal fun updateIf(transform: (TaskArchive) -> TaskArchive?): TaskArchive? = synchronized(archiveLock) {
        if (!hasArchive()) return@synchronized null
        val updated = transform(loadUnlocked()) ?: return@synchronized null
        saveUnlocked(updated)
        updated
    }

    /**
     * Creates a portable encrypted copy of organizer data. The password is never stored and the
     * local Keystore key, PIN, profile and sign-in state are deliberately excluded.
     */
    fun exportPasswordProtectedBackup(password: CharArray): ByteArray = synchronized(archiveLock) {
        require(password.size >= MIN_BACKUP_PASSWORD_LENGTH) { "La password deve contenere almeno 8 caratteri." }
        val archive = loadUnlocked()
        val salt = ByteArray(BACKUP_SALT_SIZE).also(SecureRandom()::nextBytes)
        val iv = ByteArray(BACKUP_IV_SIZE).also(SecureRandom()::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, backupSecretKey(password, salt), GCMParameterSpec(128, iv))
        val encrypted = cipher.doFinal(
            encode(archive)
        )
        ByteArrayOutputStream().use { output ->
            output.write(BACKUP_MAGIC)
            output.write(BACKUP_VERSION)
            output.write(salt.size)
            output.write(iv.size)
            output.write(salt)
            output.write(iv)
            output.write(encrypted)
            output.toByteArray()
        }
    }

    /** Decodes a verified backup without changing the local archive. */
    fun decodePasswordProtectedBackup(data: ByteArray, password: CharArray): TaskArchive = synchronized(archiveLock) {
        require(password.size >= MIN_BACKUP_PASSWORD_LENGTH) { "La password deve contenere almeno 8 caratteri." }
        require(data.size >= BACKUP_MAGIC.size + 3) { "File di backup non valido." }
        require(data.copyOfRange(0, BACKUP_MAGIC.size).contentEquals(BACKUP_MAGIC)) { "Questo file non è un backup di Task Organizer." }
        var position = BACKUP_MAGIC.size
        require(data[position++].toInt() == BACKUP_VERSION) { "Versione del backup non supportata." }
        val saltSize = data[position++].toInt() and 0xFF
        val ivSize = data[position++].toInt() and 0xFF
        require(saltSize == BACKUP_SALT_SIZE && ivSize in 12..32 && data.size > position + saltSize + ivSize) { "File di backup non valido." }
        val salt = data.copyOfRange(position, position + saltSize)
        position += saltSize
        val iv = data.copyOfRange(position, position + ivSize)
        position += ivSize
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, backupSecretKey(password, salt), GCMParameterSpec(128, iv))
        decode(cipher.doFinal(data.copyOfRange(position, data.size)))
    }

    /** Replaces local organizer data only after a complete, password-verified backup is decoded. */
    fun importPasswordProtectedBackup(data: ByteArray, password: CharArray): TaskArchive =
        decodePasswordProtectedBackup(data, password).also(::replaceArchive)

    /**
     * Keeps one encrypted, app-private copy of the current archive before a restore replaces it.
     * If the safety copy cannot be written, the restore is stopped and no existing data is touched.
     */
    fun replaceArchiveWithSafetyCopy(archive: TaskArchive): ArchiveRestoreOutcome = synchronized(archiveLock) {
        val safetyCopyCreated = if (dataFile.exists()) {
            copyCurrentArchiveForRestoreUnlocked()
            true
        } else {
            false
        }
        saveUnlocked(archive)
        ArchiveRestoreOutcome(safetyCopyCreated)
    }

    /** Used by the already verified portable-backup import API. UI restores use the safety-copy variant. */
    fun replaceArchive(archive: TaskArchive) = synchronized(archiveLock) { saveUnlocked(archive) }

    private fun copyCurrentArchiveForRestoreUnlocked() {
        val safetyAtomicFile = AtomicFile(restoreSafetyFile)
        val output = safetyAtomicFile.startWrite()
        try {
            dataFile.inputStream().use { input -> input.copyTo(output) }
            output.flush()
            output.fd.sync()
            safetyAtomicFile.finishWrite(output)
        } catch (error: Exception) {
            safetyAtomicFile.failWrite(output)
            throw error
        }
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
        if (!hasArchive()) return TaskArchive()
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
                    encode(archive)
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

    /** Removes organizer files without touching the Keystore key. Safe for isolated test data. */
    fun clear() {
        synchronized(archiveLock) {
            atomicFile.delete()
            recoveryFiles().forEach(File::delete)
            restoreSafetyFile.delete()
        }
    }

    /** Called only after an explicit confirmed deletion from the account screen. */
    fun clearAll() {
        synchronized(archiveLock) {
            atomicFile.delete()
            recoveryFiles().forEach(File::delete)
            restoreSafetyFile.delete()
            // The encrypted files are already gone, so a Keystore failure must not prevent the
            // rest of the privacy cleanup (preferences and PIN) from continuing.
            runCatching { KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.deleteEntry(keyAlias) }
        }
    }

    private fun recoveryFiles(): List<File> = storageDir.listFiles()
        ?.filter { it.isFile && it.name.startsWith(RECOVERY_PREFIX) && it.name.endsWith(".enc") }
        .orEmpty()

    private fun encode(archive: TaskArchive): ByteArray {
        val tasks = archive.tasks
        val customCategories = archive.customCategories
        val customCategoryIcons = archive.customCategoryIcons
        val deletedTasks = archive.deletedTasks
        val listItems = archive.listItems
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
                    .put("kanbanStage", item.kanbanStage)
                    .put("createdAt", item.createdAt)
            )
        }
        val categories = JSONArray()
        customCategories.distinct().forEach { categories.put(it) }
        val categoryIcons = JSONObject()
        customCategoryIcons.forEach { (category, icon) -> categoryIcons.put(category, icon) }
        val workLogEntries = JSONArray()
        archive.workLogEntries.forEach { entry ->
            workLogEntries.put(
                JSONObject()
                    .put("id", entry.id).put("date", entry.date)
                    .put("startTime", entry.startTime).put("endTime", entry.endTime)
                    .put("breakMinutes", entry.breakMinutes).put("notes", entry.notes)
                    .put("createdAt", entry.createdAt)
            )
        }
        val presets = JSONArray()
        archive.workLogSettings.presets.forEach { preset ->
            presets.put(
                JSONObject()
                    .put("id", preset.id).put("name", preset.name)
                    .put("startTime", preset.startTime).put("endTime", preset.endTime)
                    .put("breakMinutes", preset.breakMinutes)
            )
        }
        val workLogSettings = JSONObject()
            .put("hourlyRateCents", archive.workLogSettings.hourlyRateCents)
            .put("presets", presets)
        return JSONObject().put("version", 8).put("tasks", items).put("deletedTasks", deletedItems).put("listItems", listEntries).put("customCategories", categories).put("customCategoryIcons", categoryIcons).put("workLogEntries", workLogEntries).put("workLogSettings", workLogSettings).put("activeWorkTimerStartedAt", archive.activeWorkTimerStartedAt ?: 0L).toString().toByteArray(Charsets.UTF_8)
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
                deletedAt = if (deleted) item.optLong("deletedAt", System.currentTimeMillis()) else 0L,
                recurrence = item.optString("recurrence", "none"),
                seriesId = item.optString("seriesId").ifBlank { null },
                recurrenceAnchorDate = item.optString("recurrenceAnchorDate").ifBlank { null },
                subtasks = decodeSubtasks(item.optJSONArray("subtasks"))
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
                    kanbanStage = item.optString("kanbanStage"),
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
        val workLogEntries = archive.optJSONArray("workLogEntries")?.let { entries ->
            (0 until entries.length()).mapNotNull { index ->
                val entry = entries.optJSONObject(index) ?: return@mapNotNull null
                val date = entry.optString("date")
                val startTime = normalizeTime(entry.optString("startTime"))
                val endTime = normalizeTime(entry.optString("endTime"))
                if (startTime == null || endTime == null || !validWorkLogEntry(date, startTime, endTime)) return@mapNotNull null
                WorkLogEntry(
                    id = entry.optString("id").ifBlank { UUID.randomUUID().toString() },
                    date = date,
                    startTime = startTime,
                    endTime = endTime,
                    breakMinutes = entry.optInt("breakMinutes").coerceIn(0, 480),
                    notes = entry.optString("notes"),
                    createdAt = entry.optLong("createdAt", System.currentTimeMillis())
                )
            }
        }.orEmpty()
        val defaultWorkSettings = WorkLogSettings()
        val workLogSettingsJson = archive.optJSONObject("workLogSettings")
        val workLogSettings = WorkLogSettings(
            hourlyRateCents = workLogSettingsJson?.optLong("hourlyRateCents", defaultWorkSettings.hourlyRateCents)
                ?.coerceIn(0L, 9_999_999_00L) ?: defaultWorkSettings.hourlyRateCents,
            presets = workLogSettingsJson?.optJSONArray("presets")?.let { presets ->
                (0 until presets.length()).mapNotNull { index ->
                    val preset = presets.optJSONObject(index) ?: return@mapNotNull null
                    val name = preset.optString("name").trim()
                    val startTime = normalizeTime(preset.optString("startTime"))
                    val endTime = normalizeTime(preset.optString("endTime"))
                    if (name.isBlank() || startTime == null || endTime == null) return@mapNotNull null
                    WorkShiftPreset(
                        id = preset.optString("id").ifBlank { UUID.randomUUID().toString() },
                        name = name,
                        startTime = startTime,
                        endTime = endTime,
                        breakMinutes = preset.optInt("breakMinutes").coerceIn(0, 480)
                    )
                }
            } ?: defaultWorkSettings.presets
        )
        val activeWorkTimerStartedAt = archive.optLong("activeWorkTimerStartedAt", 0L).takeIf { it > 0L }
        return normalizeArchiveIds(TaskArchive(tasks, customCategories, customCategoryIcons, deletedTasks, listItems, workLogEntries, workLogSettings, activeWorkTimerStartedAt))
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
        .put("recurrence", task.recurrence)
        .put("seriesId", task.seriesId ?: "")
        .put("recurrenceAnchorDate", task.recurrenceAnchorDate ?: "")
        .apply {
            if (task.subtasks.isNotEmpty()) put("subtasks", JSONArray().apply {
                task.subtasks.forEach { subtask -> put(JSONObject().put("id", subtask.id).put("title", subtask.title).put("done", subtask.done)) }
            })
        }

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
        (keyStore.getKey(keyAlias, null) as? SecretKey)?.let { return it }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }

    private fun backupSecretKey(password: CharArray, salt: ByteArray): SecretKey {
        val specification = PBEKeySpec(password, salt, BACKUP_ITERATIONS, 256)
        return try {
            SecretKeySpec(
                SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(specification).encoded,
                "AES"
            )
        } finally {
            specification.clearPassword()
        }
    }

    private companion object {
        const val KEY_ALIAS = "task_organizer_local_key"
        const val RECOVERY_PREFIX = "task-organizer-unreadable-"
        const val TRASH_RETENTION_MILLIS = 30L * 24 * 60 * 60 * 1000
        const val MIN_BACKUP_PASSWORD_LENGTH = 8
        const val BACKUP_VERSION = 1
        const val BACKUP_SALT_SIZE = 32
        const val BACKUP_IV_SIZE = 12
        const val BACKUP_ITERATIONS = 310_000
        val BACKUP_MAGIC = byteArrayOf('T'.code.toByte(), 'O'.code.toByte(), 'B'.code.toByte(), 'K'.code.toByte())
        val archiveLock = Any()
    }
}
