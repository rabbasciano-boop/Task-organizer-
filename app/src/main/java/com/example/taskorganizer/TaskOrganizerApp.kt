package com.example.taskorganizer

import android.net.Uri
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import androidx.activity.ComponentActivity
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.taskorganizer.ui.theme.OrganizerTheme
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
@Composable
internal fun TaskOrganizerApp(
    selectedTheme: OrganizerTheme,
    followSystemTheme: Boolean,
    onThemeChanged: (OrganizerTheme) -> Unit,
    onFollowSystemThemeChanged: (Boolean) -> Unit,
    security: AppSecurity,
    biometricAvailable: Boolean,
    onRequestBiometric: ((Boolean, String?) -> Unit) -> Unit,
    openNewFromWidget: Boolean,
    onWidgetQuickAddHandled: () -> Unit,
    signedInAccount: SignedInAccount?,
    accountWorking: Boolean,
    accountMessage: String?,
    onGoogleSignIn: () -> Unit,
    onSignOut: (onFinished: () -> Unit) -> Unit,
    onDeleteAccount: (onFinished: (Boolean) -> Unit) -> Unit,
    notificationPermissionAllowed: Boolean,
    onRequestNotificationPermission: () -> Unit,
    pendingBackupImportUri: Uri?,
    onOpenBackupImport: () -> Unit,
    onBackupImportHandled: () -> Unit,
    pendingBackupExportUri: Uri?,
    onCreateBackupExport: (String, CharArray) -> Unit,
    takeBackupExportPassword: () -> CharArray?,
    onBackupExportHandled: () -> Unit,
    onExportWorkCsv: (String, String) -> Unit,
    onSecurityChanged: () -> Unit,
    profileId: String
) {
    val context = LocalContext.current
    val store = remember(profileId) { EncryptedTaskStore(context, profileId = profileId) }
    val appScope = rememberCoroutineScope()
    val cleanupScope = (context as? ComponentActivity)?.lifecycleScope ?: appScope
    val archiveUpdateMutex = remember { Mutex() }
    val profile = remember(profileId) { context.getSharedPreferences("task_organizer_profile_$profileId", android.content.Context.MODE_PRIVATE) }
    var nickname by remember { mutableStateOf(profile.getString("nickname", "Utente").orEmpty().ifBlank { "Utente" }) }
    var archiveLoadResult by remember { mutableStateOf<TaskArchiveLoadResult?>(null) }
    LaunchedEffect(store) {
        archiveLoadResult = withContext(Dispatchers.IO) { store.loadSafely() }
    }
    if (archiveLoadResult == null) {
        OpeningScreen()
        return
    }
    if (archiveLoadResult is TaskArchiveLoadResult.Unreadable) {
        ArchiveRecoveryScreen(
            onRetry = {
                appScope.launch {
                    archiveLoadResult = withContext(Dispatchers.IO) { store.loadSafely() }
                }
            },
            onStartFresh = { onFinished ->
                appScope.launch {
                    val cleared = withContext(Dispatchers.IO) { store.preserveUnreadableArchiveAndClear() }
                    if (cleared) {
                        archiveLoadResult = withContext(Dispatchers.IO) { store.loadSafely() }
                    }
                    onFinished(cleared)
                }
            }
        )
        return
    }
    val archive = (archiveLoadResult as TaskArchiveLoadResult.Loaded).archive
    val tasks = remember(archiveLoadResult) { mutableStateListOf<OrganizerTask>().also { it.addAll(archive.tasks) } }
    var backupLastAt by remember(profileId) { mutableStateOf(profile.getLong(LAST_BACKUP_AT, 0L).takeIf { it > 0L }) }
    var backupSnoozedUntil by remember(profileId) { mutableStateOf(profile.getLong(BACKUP_CARD_SNOOZED_UNTIL, 0L).takeIf { it > 0L }) }
    val customCategories = remember(archiveLoadResult) { mutableStateListOf<String>().also { it.addAll(archive.customCategories) } }
    val customCategoryIcons = remember(archiveLoadResult) { mutableStateMapOf<String, String>().also { it.putAll(archive.customCategoryIcons) } }
    val deletedTasks = remember(archiveLoadResult) { mutableStateListOf<OrganizerTask>().also { it.addAll(archive.deletedTasks) } }
    val listItems = remember(archiveLoadResult) { mutableStateListOf<OrganizerListItem>().also { it.addAll(archive.listItems) } }
    val workLogEntries = remember(archiveLoadResult) { mutableStateListOf<WorkLogEntry>().also { it.addAll(archive.workLogEntries) } }
    var workLogSettings by remember(archiveLoadResult) { mutableStateOf(archive.workLogSettings) }
    var activeWorkTimerStartedAt by remember(archiveLoadResult) { mutableStateOf(archive.activeWorkTimerStartedAt) }
    var selectedScreenIndex by rememberSaveable { mutableIntStateOf(0) }
    var tabHistory by rememberSaveable { mutableStateOf(emptyList<Int>()) }
    var editorTaskId by rememberSaveable { mutableStateOf<String?>(null) }
    val editorTask = editorTaskId?.let { id -> tasks.firstOrNull { it.id == id } }
    var editorOpen by rememberSaveable { mutableStateOf(false) }
    var backupOpen by rememberSaveable { mutableStateOf(false) }
    var privacyInfoOpen by rememberSaveable { mutableStateOf(false) }
    var storageError by remember { mutableStateOf<String?>(null) }
    val selectedScreen = OrganizerScreen.all[selectedScreenIndex.coerceIn(0, OrganizerScreen.all.lastIndex)]
    LaunchedEffect(profileId) {
        withContext(Dispatchers.IO) { runCatching { BackupReminder.schedule(context, profileId) } }
    }

    LaunchedEffect(pendingBackupImportUri, pendingBackupExportUri) {
        // The document picker briefly places the app in background. Once the PIN is entered,
        // return the person to Backup and resume the exact pending operation.
        if (pendingBackupImportUri != null || pendingBackupExportUri != null) backupOpen = true
    }

    fun navigateToTab(tab: Int) {
        if (tab != selectedScreenIndex) {
            tabHistory = if (tabHistory.lastOrNull() == tab) tabHistory.dropLast(1) else tabHistory + selectedScreenIndex
            selectedScreenIndex = tab
        }
    }

    fun applyArchive(updated: TaskArchive) {
        tasks.clear()
        tasks.addAll(updated.tasks)
        customCategories.clear()
        customCategories.addAll(updated.customCategories)
        customCategoryIcons.clear()
        customCategoryIcons.putAll(updated.customCategoryIcons)
        deletedTasks.clear()
        deletedTasks.addAll(updated.deletedTasks)
        listItems.clear()
        listItems.addAll(updated.listItems)
        workLogEntries.clear()
        workLogEntries.addAll(updated.workLogEntries)
        workLogSettings = updated.workLogSettings
        activeWorkTimerStartedAt = updated.activeWorkTimerStartedAt
    }

    fun refreshWidgets() {
        appScope.launch(Dispatchers.IO) { TaskWidgetUpdater.updateAll(context) }
    }

    fun updateArchive(change: (TaskArchive) -> TaskArchive, onFailure: () -> Unit = {}, afterSaved: suspend (TaskArchive) -> Unit = {}) {
        appScope.launch {
            archiveUpdateMutex.withLock {
                val result = withContext(Dispatchers.IO) { runCatching { store.update(change) } }
                val savedArchive = result.getOrNull()
                if (savedArchive != null) {
                    applyArchive(savedArchive)
                    refreshWidgets()
                    appScope.launch(Dispatchers.IO) { runCatching { BackupReminder.schedule(context, profileId) } }
                    afterSaved(savedArchive)
                } else {
                    storageError = "Non è stato possibile salvare le modifiche. Le tue attività non sono state cambiate: prova di nuovo o libera spazio sul telefono."
                    onFailure()
                }
            }
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, profileId) {
        fun reloadArchive() {
            appScope.launch {
                archiveUpdateMutex.withLock {
                    when (val latest = withContext(Dispatchers.IO) {
                        store.loadSafely().also { result ->
                            if (result is TaskArchiveLoadResult.Loaded) runCatching { BackupReminder.schedule(context, profileId) }
                        }
                    }) {
                        is TaskArchiveLoadResult.Loaded -> applyArchive(latest.archive)
                        is TaskArchiveLoadResult.Unreadable -> archiveLoadResult = latest
                    }
                }
            }
        }
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) reloadArchive()
        }
        val changes = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.getStringExtra(EXTRA_CHANGED_PROFILE) == profileId) reloadArchive()
            }
        }
        ContextCompat.registerReceiver(context, changes, IntentFilter(ACTION_ARCHIVE_CHANGED), ContextCompat.RECEIVER_NOT_EXPORTED)
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer); context.unregisterReceiver(changes) }
    }

    LaunchedEffect(openNewFromWidget) {
        if (openNewFromWidget) {
            editorTaskId = null
            editorOpen = true
            onWidgetQuickAddHandled()
        }
    }

    fun saveTask(task: OrganizerTask) {
        updateArchive(change = { disk ->
            val index = disk.tasks.indexOfFirst { it.id == task.id }
            disk.copy(tasks = if (index >= 0) disk.tasks.toMutableList().also { it[index] = task } else disk.tasks + task)
        }) { saved ->
            withContext(Dispatchers.IO) {
                saved.tasks.firstOrNull { it.id == task.id }?.let { TaskReminderScheduler.schedule(context, it, profileId) }
            }
            editorOpen = false
            editorTaskId = null
            navigateToTab(1)
        }
    }

    fun quickAddTask(task: OrganizerTask, onFinished: (QuickTaskSaveResult) -> Unit) {
        updateArchive(change = { disk ->
            if (disk.tasks.any { it.id == task.id } || disk.deletedTasks.any { it.id == task.id }) disk
            else disk.copy(tasks = disk.tasks + task)
        }, onFailure = { onFinished(QuickTaskSaveResult.FAILED) }) { saved ->
            val actual = saved.tasks.firstOrNull { it.id == task.id }
            if (actual == null) {
                onFinished(QuickTaskSaveResult.FAILED)
            } else {
                val reminderScheduled = withContext(Dispatchers.IO) { runCatching { TaskReminderScheduler.schedule(context, actual, profileId) }.isSuccess }
                onFinished(if (reminderScheduled) QuickTaskSaveResult.SAVED else QuickTaskSaveResult.SAVED_WITHOUT_REMINDER)
            }
        }
    }

    fun toggleTaskById(taskId: String) {
        var becameCompleted = false
        updateArchive(change = { disk ->
            val wasCompleted = disk.tasks.firstOrNull { it.id == taskId }?.completed
            val updated = RecurrenceTiming.toggleTask(disk, taskId)
            becameCompleted = wasCompleted == false && updated.tasks.firstOrNull { it.id == taskId }?.completed == true
            updated
        }) { updated ->
            if (updated.tasks.any { it.id == taskId }) {
                withContext(Dispatchers.IO) {
                    RecurrenceTiming.reminderTasks(updated, taskId).forEach { TaskReminderScheduler.schedule(context, it, profileId) }
                }
            }
            if (becameCompleted) ThemeCompletionSound.play(context, selectedTheme)
        }
    }

    fun completeTaskById(taskId: String) {
        var becameCompleted = false
        updateArchive(change = { disk ->
            val current = disk.tasks.firstOrNull { it.id == taskId } ?: return@updateArchive disk
            if (current.completed) disk else RecurrenceTiming.updateTask(disk, current.copy(completed = true)).also { becameCompleted = true }
        }) { updated ->
            if (updated.tasks.any { it.id == taskId }) {
                withContext(Dispatchers.IO) {
                    RecurrenceTiming.reminderTasks(updated, taskId).forEach { TaskReminderScheduler.schedule(context, it, profileId) }
                }
            }
            if (becameCompleted) ThemeCompletionSound.play(context, selectedTheme)
        }
    }

    fun deleteTaskById(taskId: String) {
        updateArchive(change = { disk ->
            val current = disk.tasks.firstOrNull { it.id == taskId } ?: return@updateArchive disk
            disk.copy(
                tasks = disk.tasks.filterNot { it.id == taskId },
                deletedTasks = disk.deletedTasks.filterNot { it.id == taskId } + current.copy(deletedAt = System.currentTimeMillis())
            )
        }) {
            withContext(Dispatchers.IO) { TaskReminderScheduler.cancel(context, taskId, profileId) }
        }
    }

    fun restoreTaskById(taskId: String) {
        updateArchive(change = { disk ->
            val current = disk.deletedTasks.firstOrNull { it.id == taskId } ?: return@updateArchive disk
            disk.copy(
                tasks = disk.tasks.filterNot { it.id == taskId } + current.copy(deletedAt = 0L),
                deletedTasks = disk.deletedTasks.filterNot { it.id == taskId }
            )
        }) { updated ->
            updated.tasks.firstOrNull { it.id == taskId }?.let { task ->
                withContext(Dispatchers.IO) { TaskReminderScheduler.schedule(context, task, profileId) }
            }
        }
    }

    fun permanentlyDeleteTaskById(taskId: String) {
        updateArchive(change = { disk ->
            disk.copy(deletedTasks = disk.deletedTasks.filterNot { it.id == taskId })
        })
    }

    fun restoreTasksById(taskIds: Set<String>) {
        if (taskIds.isEmpty()) return
        updateArchive(change = { disk ->
            val restored = disk.deletedTasks.filter { it.id in taskIds }.map { it.copy(deletedAt = 0L) }
            disk.copy(tasks = disk.tasks.filterNot { it.id in taskIds } + restored, deletedTasks = disk.deletedTasks.filterNot { it.id in taskIds })
        }) { updated ->
            withContext(Dispatchers.IO) {
                updated.tasks.filter { it.id in taskIds }.forEach { TaskReminderScheduler.schedule(context, it, profileId) }
            }
        }
    }

    fun permanentlyDeleteTasksById(taskIds: Set<String>) {
        if (taskIds.isEmpty()) return
        updateArchive(change = { disk -> disk.copy(deletedTasks = disk.deletedTasks.filterNot { it.id in taskIds }) })
    }

    fun clearLocalData(onFinished: (Boolean) -> Unit = {}) {
        val remindersToCancel = (tasks + deletedTasks).toList()
        cleanupScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    store.clearAll()
                    LegacyProfileAssociation.deleteAssociatedLegacy(context, profileId)
                    check(profile.edit().clear().commit()) { "Profilo locale non cancellato" }
                }
            }
            result.onSuccess {
                remindersToCancel.forEach { TaskReminderScheduler.cancel(context, it, profileId) }
                BackupReminder.cancel(context, profileId)
                backupLastAt = null
                backupSnoozedUntil = null
                tasks.clear()
                deletedTasks.clear()
                customCategories.clear()
                customCategoryIcons.clear()
                listItems.clear()
                workLogEntries.clear()
                workLogSettings = WorkLogSettings()
                activeWorkTimerStartedAt = null
                refreshWidgets()
                onFinished(true)
            }.onFailure {
                storageError = "Non è stato possibile completare la cancellazione dei dati locali. Controlla lo spazio disponibile e riprova."
                onFinished(false)
            }
        }
    }

    storageError?.let { message ->
        AlertDialog(
            onDismissRequest = { storageError = null },
            containerColor = CardBlue,
            title = { Text("Salvataggio non riuscito", color = MainText) },
            text = { Text(message, color = SoftText) },
            confirmButton = { TextButton(onClick = { storageError = null }) { Text("Va bene", color = Gold) } }
        )
    }

    BackHandler(
        enabled = storageError == null && (editorOpen || backupOpen || privacyInfoOpen || tabHistory.isNotEmpty())
    ) {
        when {
            editorOpen -> {
                editorOpen = false
                editorTaskId = null
            }
            backupOpen -> backupOpen = false
            privacyInfoOpen -> privacyInfoOpen = false
            tabHistory.isNotEmpty() -> {
                selectedScreenIndex = tabHistory.last()
                tabHistory = tabHistory.dropLast(1)
            }
        }
    }

    if (privacyInfoOpen) {
        PrivacyInfoScreen(onBack = { privacyInfoOpen = false })
        return
    }

    if (backupOpen) {
        BackupScreen(
            store = store,
            onBack = { backupOpen = false },
            pendingImportUri = pendingBackupImportUri,
            onChooseImport = onOpenBackupImport,
            onImportUriHandled = onBackupImportHandled,
            pendingExportUri = pendingBackupExportUri,
            onCreateExport = onCreateBackupExport,
            takeExportPassword = takeBackupExportPassword,
            onExportUriHandled = onBackupExportHandled,
            onExportSucceeded = {
                val now = System.currentTimeMillis()
                val recorded = withContext(Dispatchers.IO) {
                    runCatching {
                        check(LocalProfiles.mayAccess(profileId, LocalProfiles.currentId(), LocalProfiles.isSwitching()))
                        BackupReminder.markBackup(context, profileId, now)
                    }.isSuccess
                }
                if (recorded) { backupLastAt = now; backupSnoozedUntil = null }
                recorded
            },
            onImported = { importedArchive, onCompleted ->
                val previousTasks = (tasks + deletedTasks).toList()
                appScope.launch {
                    archiveUpdateMutex.withLock {
                        val result = withContext(Dispatchers.IO) {
                            runCatching {
                                val restoreOutcome = store.replaceArchiveWithSafetyCopy(importedArchive)
                                previousTasks.forEach { TaskReminderScheduler.cancel(context, it, profileId) }
                                importedArchive.tasks.forEach { TaskReminderScheduler.schedule(context, it, profileId) }
                                TaskWidgetUpdater.updateAll(context)
                                restoreOutcome
                            }
                        }
                        result.onSuccess { applyArchive(importedArchive) }
                        onCompleted(result)
                    }
                }
            }
        )
        return
    }

    if (editorOpen) {
        TaskEditorScreen(
            task = editorTask,
            customCategories = customCategories,
            customCategoryIcons = customCategoryIcons,
            onAddCustomCategory = { newCategory, icon ->
                updateArchive(change = { disk ->
                    disk.copy(
                        customCategories = (disk.customCategories + newCategory).distinct(),
                        customCategoryIcons = disk.customCategoryIcons + (newCategory to icon)
                    )
                })
            },
            onDeleteCustomCategory = { category ->
                updateArchive(change = { disk ->
                    disk.copy(
                        customCategories = disk.customCategories.filterNot { it == category },
                        customCategoryIcons = disk.customCategoryIcons.filterKeys { it != category }
                    )
                })
            },
            onCancel = {
                editorOpen = false
                editorTaskId = null
            },
            onSave = ::saveTask,
            notificationPermissionAllowed = notificationPermissionAllowed,
            onRequestNotificationPermission = onRequestNotificationPermission
        )
        return
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = NightBlue,
        bottomBar = {
            if (selectedScreen != OrganizerScreen.TimeLog) {
                OrganizerNavigationBar(
                    selected = selectedScreen,
                    onSelected = { navigateToTab(OrganizerScreen.all.indexOf(it)) },
                    barColor = CardBlue,
                    mutedColor = SoftText,
                    selectedColor = Gold,
                    indicatorColor = CardBlueLight
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().organizerBackground().padding(padding).consumeWindowInsets(padding)) {
            val openNewTask = {
                editorTaskId = null
                editorOpen = true
            }
            val openEditTask: (OrganizerTask) -> Unit = {
                editorTaskId = it.id
                editorOpen = true
            }
            when (selectedScreen) {
                OrganizerScreen.Today -> TodayScreen(
                    tasks = tasks,
                    nickname = nickname,
                    signedInAccount = signedInAccount,
                    onAdd = openNewTask,
                    onQuickAdd = ::quickAddTask,
                    backupLastAt = backupLastAt,
                    backupSnoozedUntil = backupSnoozedUntil,
                    onOpenBackup = { backupOpen = true },
                    onSnoozeBackup = {
                        val until = System.currentTimeMillis() + BACKUP_CARD_SNOOZE
                        appScope.launch {
                            val saved = withContext(Dispatchers.IO) { profile.edit().putLong(BACKUP_CARD_SNOOZED_UNTIL, until).commit() }
                            if (saved) backupSnoozedUntil = until else storageError = context.getString(R.string.backup_snooze_error)
                        }
                    },
                    onToggleTask = ::toggleTaskById,
                    onCompleteTask = ::completeTaskById,
                    onDeleteTask = ::deleteTaskById,
                    onRestoreTask = ::restoreTaskById,
                    onEdit = openEditTask,
                    onOpenActivities = { navigateToTab(1) },
                    onOpenLists = { navigateToTab(2) },
                    onOpenCalendar = { navigateToTab(3) },
                    onOpenTimeLog = { navigateToTab(5) },
                    onManageAccount = { navigateToTab(4) },
                    onOpenPrivacy = { privacyInfoOpen = true }
                )
                OrganizerScreen.Activities -> ActivitiesScreen(tasks, deletedTasks, openNewTask, ::toggleTaskById, openEditTask, ::deleteTaskById, ::restoreTaskById, ::permanentlyDeleteTaskById, ::restoreTasksById, ::permanentlyDeleteTasksById)
                OrganizerScreen.Lists -> KanbanListsScreen(
                    items = listItems,
                    onAdd = { item -> updateArchive(change = { disk -> disk.copy(listItems = disk.listItems + item) }) },
                    onChange = { item ->
                        updateArchive(change = { disk ->
                            val index = disk.listItems.indexOfFirst { it.id == item.id }
                            if (index < 0) disk else disk.copy(listItems = disk.listItems.toMutableList().also { it[index] = item })
                        })
                    },
                    onDelete = { item -> updateArchive(change = { disk -> disk.copy(listItems = disk.listItems.filterNot { it.id == item.id }) }) }
                )
                OrganizerScreen.Calendar -> CalendarScreen(tasks, openNewTask, ::toggleTaskById, openEditTask)
                OrganizerScreen.TimeLog -> TimeLogScreen(
                    profileId = profileId,
                    entries = workLogEntries.toList(),
                    settings = workLogSettings,
                    activeTimerStartedAt = activeWorkTimerStartedAt,
                    onSaveEntry = { entry, onFinished ->
                        updateArchive(change = { disk ->
                            val index = disk.workLogEntries.indexOfFirst { it.id == entry.id }
                            disk.copy(workLogEntries = if (index >= 0) disk.workLogEntries.toMutableList().also { it[index] = entry } else disk.workLogEntries + entry)
                        }, onFailure = { onFinished(false) }) { onFinished(true) }
                    },
                    onDeleteEntry = { entryId, onFinished ->
                        updateArchive(change = { disk -> disk.copy(workLogEntries = disk.workLogEntries.filterNot { it.id == entryId }) }, onFailure = { onFinished(false) }) { onFinished(true) }
                    },
                    onSaveSettings = { settings -> updateArchive(change = { disk -> disk.copy(workLogSettings = settings) }) },
                    onStartTimer = { updateArchive(change = { disk -> if (disk.activeWorkTimerStartedAt == null) disk.copy(activeWorkTimerStartedAt = System.currentTimeMillis()) else disk }) },
                    onStopTimer = { recordedEntries, onFinished ->
                        updateArchive(change = { disk ->
                            val ids = recordedEntries.map { it.id }.toSet()
                            disk.copy(
                                workLogEntries = disk.workLogEntries.filterNot { it.id in ids } + recordedEntries,
                                activeWorkTimerStartedAt = null
                            )
                        }, onFailure = { onFinished(false) }) { onFinished(true) }
                    },
                    onExportCsv = onExportWorkCsv,
                    onBack = { navigateToTab(0) }
                )
                OrganizerScreen.Options -> SettingsScreen(
                    selectedTheme = selectedTheme,
                    followSystemTheme = followSystemTheme,
                    onThemeChanged = onThemeChanged,
                    onFollowSystemThemeChanged = onFollowSystemThemeChanged,
                    security = security,
                    biometricAvailable = biometricAvailable,
                    onRequestBiometric = onRequestBiometric,
                    onSecurityChanged = onSecurityChanged,
                    onOpenBackup = { backupOpen = true },
                    nickname = nickname,
                    onNicknameChanged = { newNickname ->
                        nickname = newNickname.ifBlank { "Utente" }
                        profile.edit().putString("nickname", nickname).apply()
                    },
                    signedInAccount = signedInAccount,
                    accountWorking = accountWorking,
                    accountMessage = accountMessage,
                    onGoogleSignIn = onGoogleSignIn,
                    onSignOut = { deleteLocalData ->
                        if (deleteLocalData) clearLocalData { cleared -> if (cleared) onSignOut {} }
                        else onSignOut {}
                    },
                    onDeleteAccount = { deleteLocalData ->
                        onDeleteAccount { deleted ->
                            if (deleted && deleteLocalData) {
                                clearLocalData()
                                nickname = "Utente"
                            }
                        }
                    }
                )
            }
        }
    }
}

