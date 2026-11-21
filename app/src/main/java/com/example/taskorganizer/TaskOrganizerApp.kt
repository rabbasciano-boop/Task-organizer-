package com.example.taskorganizer

import android.Manifest
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import android.graphics.drawable.ColorDrawable
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.taskorganizer.ui.theme.TaskOrganizerTheme
import com.example.taskorganizer.ui.theme.OrganizerPalette
import com.example.taskorganizer.ui.theme.OrganizerTheme
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.text.NumberFormat
import java.net.URL
import java.io.ByteArrayOutputStream
import java.util.Locale
import javax.net.ssl.HttpsURLConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
    onSecurityChanged: () -> Unit
) {
    val context = LocalContext.current
    val store = remember { EncryptedTaskStore(context) }
    val appScope = rememberCoroutineScope()
    val archiveUpdateMutex = remember { Mutex() }
    val profile = remember { context.getSharedPreferences("task_organizer_profile", android.content.Context.MODE_PRIVATE) }
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
    val customCategories = remember(archiveLoadResult) { mutableStateListOf<String>().also { it.addAll(archive.customCategories) } }
    val customCategoryIcons = remember(archiveLoadResult) { mutableStateMapOf<String, String>().also { it.putAll(archive.customCategoryIcons) } }
    val deletedTasks = remember(archiveLoadResult) { mutableStateListOf<OrganizerTask>().also { it.addAll(archive.deletedTasks) } }
    val listItems = remember(archiveLoadResult) { mutableStateListOf<OrganizerListItem>().also { it.addAll(archive.listItems) } }
    var selectedScreenIndex by rememberSaveable { mutableIntStateOf(0) }
    var tabHistory by rememberSaveable { mutableStateOf(emptyList<Int>()) }
    var editorTaskId by rememberSaveable { mutableStateOf<String?>(null) }
    val editorTask = editorTaskId?.let { id -> tasks.firstOrNull { it.id == id } }
    var editorOpen by rememberSaveable { mutableStateOf(false) }
    var backupOpen by rememberSaveable { mutableStateOf(false) }
    var privacyInfoOpen by rememberSaveable { mutableStateOf(false) }
    var storageError by remember { mutableStateOf<String?>(null) }
    val selectedScreen = OrganizerScreen.all[selectedScreenIndex.coerceIn(0, OrganizerScreen.all.lastIndex)]

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
    }

    fun refreshWidgets(archive: TaskArchive) {
        appScope.launch(Dispatchers.IO) { TaskWidgetUpdater.updateAll(context, archive) }
    }

    fun updateArchive(change: (TaskArchive) -> TaskArchive, afterSaved: (TaskArchive) -> Unit = {}) {
        appScope.launch {
            archiveUpdateMutex.withLock {
                val result = withContext(Dispatchers.IO) { runCatching { store.update(change) } }
                result.onSuccess { savedArchive ->
                    applyArchive(savedArchive)
                    refreshWidgets(savedArchive)
                    afterSaved(savedArchive)
                }.onFailure {
                    storageError = "Non è stato possibile salvare le modifiche. Le tue attività non sono state cambiate: prova di nuovo o libera spazio sul telefono."
                }
            }
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                appScope.launch {
                    archiveUpdateMutex.withLock {
                        when (val latest = withContext(Dispatchers.IO) { store.loadSafely() }) {
                            is TaskArchiveLoadResult.Loaded -> applyArchive(latest.archive)
                            is TaskArchiveLoadResult.Unreadable -> archiveLoadResult = latest
                        }
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
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
        }) {
            appScope.launch(Dispatchers.IO) { TaskReminderScheduler.schedule(context, task) }
            editorOpen = false
            editorTaskId = null
            navigateToTab(1)
        }
    }

    fun toggleTaskById(taskId: String) {
        updateArchive(change = { disk ->
            RecurrenceTiming.toggleTask(disk, taskId)
        }) { updated ->
            if (updated.tasks.any { it.id == taskId }) {
                appScope.launch(Dispatchers.IO) {
                    RecurrenceTiming.reminderTasks(updated, taskId).forEach { TaskReminderScheduler.schedule(context, it) }
                }
            }
        }
    }

    fun completeTaskById(taskId: String) {
        updateArchive(change = { disk ->
            val current = disk.tasks.firstOrNull { it.id == taskId } ?: return@updateArchive disk
            if (current.completed) disk else RecurrenceTiming.updateTask(disk, current.copy(completed = true))
        }) { updated ->
            if (updated.tasks.any { it.id == taskId }) {
                appScope.launch(Dispatchers.IO) {
                    RecurrenceTiming.reminderTasks(updated, taskId).forEach { TaskReminderScheduler.schedule(context, it) }
                }
            }
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
            TaskReminderScheduler.cancel(context, taskId)
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
                appScope.launch(Dispatchers.IO) { TaskReminderScheduler.schedule(context, task) }
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
            appScope.launch(Dispatchers.IO) {
                updated.tasks.filter { it.id in taskIds }.forEach { TaskReminderScheduler.schedule(context, it) }
            }
        }
    }

    fun permanentlyDeleteTasksById(taskIds: Set<String>) {
        if (taskIds.isEmpty()) return
        updateArchive(change = { disk -> disk.copy(deletedTasks = disk.deletedTasks.filterNot { it.id in taskIds }) })
    }

    fun clearLocalData() {
        val remindersToCancel = (tasks + deletedTasks).toList()
        appScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    store.clearAll()
                    profile.edit().clear().commit()
                    context.getSharedPreferences("task_organizer_settings", android.content.Context.MODE_PRIVATE)
                        .edit().clear().commit()
                    context.getSharedPreferences(TaskOrganizerWidgetProvider.WIDGET_PREFERENCES, android.content.Context.MODE_PRIVATE)
                        .edit().clear().commit()
                    security.clear()
                }
            }
            result.onSuccess {
                remindersToCancel.forEach { TaskReminderScheduler.cancel(context, it) }
                tasks.clear()
                deletedTasks.clear()
                customCategories.clear()
                customCategoryIcons.clear()
                listItems.clear()
                onSecurityChanged()
                refreshWidgets(TaskArchive())
            }.onFailure {
                storageError = "Non è stato possibile completare la cancellazione dei dati locali. Controlla lo spazio disponibile e riprova."
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
            onImported = { importedArchive, onCompleted ->
                val previousTasks = (tasks + deletedTasks).toList()
                appScope.launch {
                    archiveUpdateMutex.withLock {
                        val result = withContext(Dispatchers.IO) {
                            runCatching {
                                store.replaceArchive(importedArchive)
                                previousTasks.forEach { TaskReminderScheduler.cancel(context, it) }
                                importedArchive.tasks.forEach { TaskReminderScheduler.schedule(context, it) }
                                TaskWidgetUpdater.updateAll(context, importedArchive)
                            }
                        }
                        result.onSuccess { applyArchive(importedArchive) }
                        onCompleted(result.map { Unit })
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
            OrganizerNavigationBar(
                selected = selectedScreen,
                onSelected = { navigateToTab(OrganizerScreen.all.indexOf(it)) },
                barColor = CardBlue,
                mutedColor = SoftText,
                selectedColor = Gold,
                indicatorColor = CardBlueLight
            )
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
                    onAddLog = { title, category, minutes, date ->
                        if (!date.isAfter(LocalDate.now()) && minutes >= 1) {
                            updateArchive(change = { disk ->
                                disk.copy(
                                    tasks = disk.tasks + OrganizerTask(
                                        title = title,
                                        date = date.toString(),
                                        time = LocalTime.now().withSecond(0).withNano(0).toString(),
                                        category = category,
                                        note = "Durata: $minutes min",
                                        completed = true,
                                        reminderEnabled = false
                                    )
                                )
                            })
                        }
                    },
                    onToggleTask = ::toggleTaskById,
                    onCompleteTask = ::completeTaskById,
                    onDeleteTask = ::deleteTaskById,
                    onRestoreTask = ::restoreTaskById,
                    onEdit = openEditTask,
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
                OrganizerScreen.Options -> SettingsScreen(
                    selectedTheme = selectedTheme,
                    followSystemTheme = followSystemTheme,
                    onThemeChanged = onThemeChanged,
                    onFollowSystemThemeChanged = onFollowSystemThemeChanged,
                    security = security,
                    biometricAvailable = biometricAvailable,
                    onSecurityChanged = onSecurityChanged,
                    onOpenBackup = { backupOpen = true },
                    nickname = nickname,
                    onNicknameChanged = { newNickname ->
                        nickname = newNickname.ifBlank { "Utente" }
                        profile.edit().putString("nickname", nickname).apply()
                    },
                    onOpenPrivacy = { privacyInfoOpen = true },
                    signedInAccount = signedInAccount,
                    accountWorking = accountWorking,
                    accountMessage = accountMessage,
                    onGoogleSignIn = onGoogleSignIn,
                    onSignOut = { deleteLocalData ->
                        onSignOut {
                            if (deleteLocalData) {
                                clearLocalData()
                                nickname = "Utente"
                            }
                        }
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

