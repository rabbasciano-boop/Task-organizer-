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
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.ActivityResultLauncher
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
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

private val NightBlue get() = OrganizerPalette.background
private val CardBlue get() = OrganizerPalette.card
private val CardBlueLight get() = OrganizerPalette.cardLight
private val Gold get() = OrganizerPalette.accent
private val SoftText get() = OrganizerPalette.muted
private val MainText get() = OrganizerPalette.text
private val AccentSecondary get() = OrganizerPalette.accentSecondary
private val Success get() = OrganizerPalette.success
private val Danger get() = OrganizerPalette.danger
private const val DefaultCardColor = "default"
private val TaskCardColorOptions = listOf(
    DefaultCardColor to "Predefinito",
    "oceano" to "Oceano",
    "salvia" to "Salvia",
    "prugna" to "Prugna",
    "corallo" to "Corallo",
    "miele" to "Miele"
)

private fun taskCardBackground(color: String): Color = when (color) {
    "oceano" -> Color(0xFF23456E)
    "salvia" -> Color(0xFF245548)
    "prugna" -> Color(0xFF4A345D)
    "corallo" -> Color(0xFF6B3940)
    "miele" -> Color(0xFF635021)
    else -> CardBlue
}

private fun taskCardTextColor(task: OrganizerTask): Color = if (task.cardColor == DefaultCardColor) MainText else Color(0xFFF7F9FC)
private fun taskCardMutedColor(task: OrganizerTask): Color = if (task.cardColor == DefaultCardColor) SoftText else Color(0xFFD8E1EC)
private fun taskCompletionColor(task: OrganizerTask): Color = when {
    task.cardColor == DefaultCardColor && task.completed -> Success
    task.cardColor == DefaultCardColor -> Gold
    task.completed -> Color(0xFF7EE2B1)
    else -> Color(0xFFE8C877)
}
private val CustomCategoryIconOptions = listOf("🏷️", "📌", "❤️", "🏃", "💊", "✈️", "🎨", "💰", "🛒", "🐾", "🏡", "🎯")

private fun categoryIcon(category: String, iconOverride: String? = null): String = iconOverride ?: when (category) {
    "Studio" -> "📚"
    "Personale" -> "🏠"
    "Lavoro" -> "💼"
    "Esami" -> "🎓"
    else -> "🏷️"
}

private fun categoryDisplay(category: String, iconOverride: String? = null): String = "${categoryIcon(category, iconOverride)}  $category"

private fun auroraBackgroundBrush(): Brush? = when (OrganizerPalette.currentTheme) {
    OrganizerTheme.AURORA_MIDNIGHT -> Brush.linearGradient(
        colors = listOf(Color(0xFF4C1D95), Color(0xFF241344), Color(0xFF123842), Color(0xFF0F766E))
    )
    OrganizerTheme.AURORA_NORDIC -> Brush.linearGradient(
        colors = listOf(Color(0xFFDDF5F0), Color(0xFFDCE8FF), Color(0xFFF4E4F5), Color(0xFFE1F6F1))
    )
    else -> null
}

private fun Modifier.organizerBackground(): Modifier = auroraBackgroundBrush()?.let { background(it) } ?: background(NightBlue)

class MainActivity : FragmentActivity() {
    private var widgetQuickAdd by mutableStateOf(false)
    private var notificationPermissionAllowed by mutableStateOf(true)
    private var pendingBackupImportUri by mutableStateOf<Uri?>(null)
    private var pendingBackupExportUri by mutableStateOf<Uri?>(null)
    private var pendingBackupExportPassword: CharArray? = null
    private lateinit var backupImportLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var backupExportLauncher: ActivityResultLauncher<String>
    private val uiSession by lazy(LazyThreadSafetyMode.NONE) {
        ViewModelProvider(this, UiSessionViewModelFactory(AppSecurity(this).hasPin()))[UiSessionViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        backupImportLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            pendingBackupImportUri = uri
        }
        backupExportLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
            if (uri == null) {
                pendingBackupExportPassword?.fill('\u0000')
                pendingBackupExportPassword = null
            }
            pendingBackupExportUri = uri
        }
        widgetQuickAdd = intent.getBooleanExtra(EXTRA_OPEN_NEW_TASK, false)
        val initialSettings = getSharedPreferences("task_organizer_settings", MODE_PRIVATE)
        val initialTheme = OrganizerTheme.fromStorage(
            initialSettings.getString("theme_mode", null),
            initialSettings.getBoolean("dark_theme", true)
        )
        val initialEffectiveTheme = if (initialSettings.getBoolean("follow_system_theme", false)) systemTheme() else initialTheme
        updateSystemBars(initialEffectiveTheme)
        window.setBackgroundDrawable(ColorDrawable(initialBackgroundColor(initialEffectiveTheme)))
        updateSecureWindow()
        TaskReminderScheduler.ensureNotificationChannels(this)
        notificationPermissionAllowed = TaskReminderScheduler.notificationsAllowed(this)
        setContent {
            val preferences = remember { getSharedPreferences("task_organizer_settings", MODE_PRIVATE) }
            val security = remember { AppSecurity(this) }
            val session = uiSession
            val accountManager = remember { FirebaseAccountManager(this@MainActivity) }
            var signedInAccount by remember { mutableStateOf(accountManager.currentAccount()) }
            var accountWorking by remember { mutableStateOf(false) }
            var accountMessage by remember { mutableStateOf<String?>(null) }
            var selectedTheme by remember {
                mutableStateOf(
                    OrganizerTheme.fromStorage(
                        preferences.getString("theme_mode", null),
                        preferences.getBoolean("dark_theme", true)
                    )
                )
            }
            var followSystemTheme by remember { mutableStateOf(preferences.getBoolean("follow_system_theme", false)) }
            val effectiveTheme = if (followSystemTheme) {
                if (isSystemInDarkTheme()) OrganizerTheme.ORIGINAL_DARK else OrganizerTheme.CARTA_DORATA
            } else selectedTheme
            LaunchedEffect(effectiveTheme) { updateSystemBars(effectiveTheme) }
            var privacyAcknowledged by remember { mutableStateOf(preferences.getBoolean("privacy_acknowledged", false)) }
            LaunchedEffect(session.showOpening) {
                if (!session.showOpening) return@LaunchedEffect
                delay(450)
                session.showOpening = false
            }
            TaskOrganizerTheme(effectiveTheme) {
                if (session.showOpening) {
                    OpeningScreen()
                } else if (!privacyAcknowledged) {
                    PrivacyConsentScreen(onContinueAsGuest = {
                        preferences.edit().putBoolean("privacy_acknowledged", true).apply()
                        privacyAcknowledged = true
                    })
                } else if (session.isUnlocked) {
                    TaskOrganizerApp(
                        selectedTheme = effectiveTheme,
                        followSystemTheme = followSystemTheme,
                        onThemeChanged = { theme ->
                            selectedTheme = theme
                            preferences.edit()
                                .putString("theme_mode", theme.storageKey)
                                .putBoolean("dark_theme", theme.isDark)
                                .putBoolean("follow_system_theme", false)
                                .apply()
                            followSystemTheme = false
                            Thread { TaskWidgetUpdater.updateAll(this@MainActivity) }.start()
                        },
                        onFollowSystemThemeChanged = { enabled ->
                            followSystemTheme = enabled
                            preferences.edit().putBoolean("follow_system_theme", enabled).apply()
                            Thread { TaskWidgetUpdater.updateAll(this@MainActivity) }.start()
                        },
                        security = security,
                        biometricAvailable = canUseBiometrics(),
                        openNewFromWidget = widgetQuickAdd,
                        onWidgetQuickAddHandled = { widgetQuickAdd = false },
                        signedInAccount = signedInAccount,
                        accountWorking = accountWorking,
                        accountMessage = accountMessage,
                        onGoogleSignIn = {
                            accountWorking = true
                            accountMessage = null
                            accountManager.signInWithGoogle { account, message ->
                                signedInAccount = account
                                accountWorking = false
                                accountMessage = message
                            }
                        },
                        onSignOut = { onFinished ->
                            accountWorking = true
                            accountManager.signOut {
                                signedInAccount = null
                                accountWorking = false
                                onFinished()
                            }
                        },
                        onDeleteAccount = { onFinished ->
                            accountWorking = true
                            accountMessage = null
                            accountManager.deleteAccountWithGoogleConfirmation { deleted, message ->
                                if (deleted) signedInAccount = null
                                accountWorking = false
                                accountMessage = message
                                onFinished(deleted)
                            }
                        },
                        notificationPermissionAllowed = notificationPermissionAllowed,
                        onRequestNotificationPermission = ::requestNotificationPermission,
                        pendingBackupImportUri = pendingBackupImportUri,
                        onOpenBackupImport = {
                            backupImportLauncher.launch(arrayOf("application/octet-stream", "application/*"))
                        },
                        onBackupImportHandled = { pendingBackupImportUri = null },
                        pendingBackupExportUri = pendingBackupExportUri,
                        onCreateBackupExport = { fileName, password ->
                            pendingBackupExportPassword?.fill('\u0000')
                            pendingBackupExportPassword = password
                            backupExportLauncher.launch(fileName)
                        },
                        takeBackupExportPassword = {
                            pendingBackupExportPassword.also { pendingBackupExportPassword = null }
                        },
                        onBackupExportHandled = { pendingBackupExportUri = null },
                        onSecurityChanged = {
                            updateSecureWindow()
                            if (!security.hasPin()) session.unlock()
                        }
                    )
                } else {
                    AppLockScreen(
                        security = security,
                        biometricAvailable = canUseBiometrics(),
                        onUnlocked = session::unlock,
                        onRequestBiometric = { onResult -> authenticateWithBiometrics(onResult) }
                    )
                }
            }
        }
    }

    private fun updateSystemBars(theme: OrganizerTheme) {
        val style = if (theme.isDark) {
            SystemBarStyle.dark(AndroidColor.TRANSPARENT)
        } else {
            SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
        }
        enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
    }

    private fun systemTheme(): OrganizerTheme = if (
        resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    ) OrganizerTheme.ORIGINAL_DARK else OrganizerTheme.CARTA_DORATA

    private fun initialBackgroundColor(theme: OrganizerTheme): Int = when (theme) {
        OrganizerTheme.ORIGINAL_DARK -> AndroidColor.rgb(13, 27, 47)
        OrganizerTheme.CARTA_DORATA -> AndroidColor.rgb(246, 240, 229)
        OrganizerTheme.AURORA_MIDNIGHT -> AndroidColor.rgb(7, 10, 24)
        OrganizerTheme.AURORA_NORDIC -> AndroidColor.rgb(238, 246, 248)
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations && AppSecurity(this).hasPin()) uiSession.lock()
    }

    override fun onResume() {
        super.onResume()
        notificationPermissionAllowed = TaskReminderScheduler.notificationsAllowed(this)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST) {
            notificationPermissionAllowed = TaskReminderScheduler.notificationsAllowed(this)
        }
    }

    private fun requestNotificationPermission() {
        if (TaskReminderScheduler.notificationsAllowed(this)) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val preferences = getSharedPreferences("task_organizer_settings", MODE_PRIVATE)
            if (!preferences.getBoolean("notification_permission_requested", false)) {
                preferences.edit().putBoolean("notification_permission_requested", true).apply()
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), NOTIFICATION_PERMISSION_REQUEST)
                return
            }
        }
        startActivity(
            android.content.Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        )
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(EXTRA_OPEN_NEW_TASK, false)) widgetQuickAdd = true
    }

    private fun canUseBiometrics(): Boolean = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.P ->
            BiometricManager.from(this).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS
        else -> false
    }

    private fun updateSecureWindow() {
        if (AppSecurity(this).hasPin()) {
            window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    private fun authenticateWithBiometrics(onResult: (Boolean, String?) -> Unit) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            onResult(false, "L'impronta non è disponibile su questo telefono.")
            return
        }
        val prompt = BiometricPrompt(this, mainExecutor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onResult(true, null)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                onResult(false, errString.toString())
            }
        })
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Sblocca Task Organizer")
            .setSubtitle("Conferma la tua identità")
            .setNegativeButtonText("Usa il PIN")
            .build()
        prompt.authenticate(promptInfo)
    }

    companion object {
        const val EXTRA_OPEN_NEW_TASK = "open_new_task"
        private const val NOTIFICATION_PERMISSION_REQUEST = 1
    }
}

@Composable
private fun OpeningScreen() {
    Box(
        modifier = Modifier.fillMaxSize().organizerBackground(),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.splash_mark),
            contentDescription = "Task Organizer",
            modifier = Modifier.size(260.dp),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
private fun PrivacyConsentScreen(onContinueAsGuest: () -> Unit) {
    var confirmed by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxSize().organizerBackground()) {
        PageColumn(safeBottom = true, safeTop = true) {
            Text("Prima di iniziare", color = MainText, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("La tua privacy viene prima dell'organizzazione.", color = SoftText)
            InfoCard(
                "I tuoi dati restano qui",
                "Attività, note, liste, categorie, preferenze e PIN restano nel telefono in un archivio cifrato. Non vengono inviati agli sviluppatori.",
                "Solo sul telefono"
            )
            InfoCard(
                "Account facoltativo",
                "Potrai accedere con Google o Microsoft soltanto se vorrai usare backup, calendario o servizi collegati. L'accesso non caricherà automaticamente le tue attività.",
                "Potrai decidere in seguito"
            )
            InfoCard(
                "Niente pubblicità o tracciamento",
                "Non vendiamo dati, non mostriamo pubblicità e non analizziamo le tue attività. Le notifiche verranno chieste solo quando deciderai di attivarle.",
                "Trasparenza"
            )
            Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = confirmed, onCheckedChange = { confirmed = it })
                Text("Ho letto come vengono trattati i miei dati.", color = MainText, fontSize = 14.sp, modifier = Modifier.padding(start = 6.dp))
            }
            Button(
                onClick = onContinueAsGuest,
                enabled = confirmed,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = NightBlue)
            ) { Text("Continua come ospite", fontWeight = FontWeight.Bold) }
            Text("Potrai aggiungere un account in qualsiasi momento dalla scheda Account.", color = SoftText, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun ArchiveRecoveryScreen(onRetry: () -> Unit, onStartFresh: (onFinished: (Boolean) -> Unit) -> Unit) {
    var showConfirmation by remember { mutableStateOf(false) }
    var recoveryError by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxSize().organizerBackground()) {
        PageColumn(safeBottom = true, safeTop = true) {
            Text("Archivio da recuperare", color = MainText, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Task Organizer non riesce a leggere l'archivio locale. Per proteggere le tue attività non abbiamo modificato né cancellato nulla.", color = SoftText)
            InfoCard(
                "I dati restano al sicuro",
                "Potrebbe trattarsi di un file danneggiato o di una chiave di sicurezza non più disponibile sul telefono. Puoi riprovare oppure iniziare con un archivio nuovo.",
                "Nessuna cancellazione automatica"
            )
            Button(
                onClick = { recoveryError = false; onRetry() },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = NightBlue)
            ) { Text("Riprova", fontWeight = FontWeight.Bold) }
            OutlinedButton(
                onClick = { showConfirmation = true },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) { Text("Inizia con un nuovo archivio", color = MainText) }
            if (recoveryError) {
                Text("Non è stato possibile preparare una copia di sicurezza. I tuoi dati non sono stati toccati.", color = Danger, fontSize = 13.sp)
            }
        }
    }
    if (showConfirmation) {
        AlertDialog(
            onDismissRequest = { showConfirmation = false },
            title = { Text("Iniziare da zero?", color = MainText) },
            text = { Text("Prima conserveremo una copia dell'archivio non leggibile sul telefono. Solo dopo verrà creato un nuovo archivio vuoto.", color = SoftText) },
            confirmButton = {
                TextButton(onClick = {
                    showConfirmation = false
                    onStartFresh { succeeded -> recoveryError = !succeeded }
                }) { Text("Conserva copia e continua", color = Gold) }
            },
            dismissButton = { TextButton(onClick = { showConfirmation = false }) { Text("Annulla", color = SoftText) } }
        )
    }
}

@Composable
private fun ArchiveLoadingScreen() {
    Box(modifier = Modifier.fillMaxSize().organizerBackground(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Image(
                painter = painterResource(R.drawable.splash_mark),
                contentDescription = "Task Organizer",
                modifier = Modifier.size(180.dp),
                contentScale = ContentScale.Fit
            )
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(0.42f).height(5.dp),
                color = Gold,
                trackColor = CardBlueLight
            )
        }
    }
}

@Composable
private fun TaskOrganizerApp(
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
        ArchiveLoadingScreen()
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
        }) { savedArchive ->
            refreshWidgets(savedArchive)
            TaskReminderScheduler.schedule(context, task)
            editorOpen = false
            editorTaskId = null
            navigateToTab(1)
        }
    }

    fun changeTask(task: OrganizerTask) {
        updateArchive(change = { disk ->
            RecurrenceTiming.updateTask(disk, task)
        }) { updated ->
            if (updated.tasks.any { it.id == task.id }) {
                refreshWidgets(updated)
                RecurrenceTiming.reminderTasks(updated, task.id).forEach { TaskReminderScheduler.schedule(context, it) }
            }
        }
    }

    fun deleteTask(task: OrganizerTask) {
        updateArchive(change = { disk ->
            disk.copy(
                tasks = disk.tasks.filterNot { it.id == task.id },
                deletedTasks = disk.deletedTasks.filterNot { it.id == task.id } + task.copy(deletedAt = System.currentTimeMillis())
            )
        }) { savedArchive ->
            refreshWidgets(savedArchive)
            TaskReminderScheduler.cancel(context, task)
        }
    }

    fun restoreTask(task: OrganizerTask) {
        updateArchive(change = { disk ->
            disk.copy(
                tasks = disk.tasks.filterNot { it.id == task.id } + task.copy(deletedAt = 0L),
                deletedTasks = disk.deletedTasks.filterNot { it.id == task.id }
            )
        }) { savedArchive ->
            refreshWidgets(savedArchive)
            TaskReminderScheduler.schedule(context, task)
        }
    }

    fun permanentlyDeleteTask(task: OrganizerTask) {
        updateArchive(change = { disk ->
            disk.copy(deletedTasks = disk.deletedTasks.filterNot { it.id == task.id })
        }) { savedArchive ->
            refreshWidgets(savedArchive)
        }
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
            onImported = { importedArchive ->
                val previousTasks = (tasks + deletedTasks).toList()
                applyArchive(importedArchive)
                appScope.launch(Dispatchers.IO) {
                    previousTasks.forEach { TaskReminderScheduler.cancel(context, it) }
                    importedArchive.tasks.forEach { TaskReminderScheduler.schedule(context, it) }
                    TaskWidgetUpdater.updateAll(context, importedArchive)
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
        Box(modifier = Modifier.fillMaxSize().organizerBackground().padding(padding)) {
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
                        updateArchive(change = { disk ->
                            disk.copy(
                                tasks = disk.tasks + OrganizerTask(
                                    title = title,
                                    date = date.toString(),
                                    time = LocalTime.now().withSecond(0).withNano(0).toString(),
                                    category = category,
                                    note = "Log registrato · $minutes min",
                                    completed = true,
                                    reminderEnabled = false
                                )
                            )
                        })
                    },
                    onChange = ::changeTask,
                    onDelete = ::deleteTask,
                    onEdit = openEditTask,
                    onManageAccount = { navigateToTab(4) },
                    onOpenPrivacy = { privacyInfoOpen = true }
                )
                OrganizerScreen.Activities -> ActivitiesScreen(tasks, deletedTasks, openNewTask, ::changeTask, openEditTask, ::deleteTask, ::restoreTask, ::permanentlyDeleteTask)
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
                OrganizerScreen.Calendar -> CalendarScreen(tasks, openNewTask, ::changeTask, openEditTask)
                OrganizerScreen.Account -> AccountScreen(
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
                    },
                    onOpenBackup = { backupOpen = true }
                )
                OrganizerScreen.Options -> SettingsScreen(
                    selectedTheme = selectedTheme,
                    followSystemTheme = followSystemTheme,
                    onThemeChanged = onThemeChanged,
                    onFollowSystemThemeChanged = onFollowSystemThemeChanged,
                    security = security,
                    biometricAvailable = biometricAvailable,
                    onSecurityChanged = onSecurityChanged,
                    onOpenBackup = { backupOpen = true }
                )
            }
        }
    }
}

private data class KanbanColumn(val id: String, val label: String, val icon: String)

private val KanbanColumns = listOf(
    KanbanColumn("todo", "Da fare", "📥"),
    KanbanColumn("shopping", "Spesa", "🛒"),
    KanbanColumn("doing", "In corso", "⚡"),
    KanbanColumn("done", "Fatto", "✓")
)

private fun kanbanStageOf(item: OrganizerListItem): String = when {
    item.completed -> "done"
    item.kanbanStage in KanbanColumns.map { it.id } -> item.kanbanStage
    item.listType == "shopping" -> "shopping"
    else -> "todo"
}

@Composable
private fun KanbanListsScreen(
    items: List<OrganizerListItem>,
    onAdd: (OrganizerListItem) -> Unit,
    onChange: (OrganizerListItem) -> Unit,
    onDelete: (OrganizerListItem) -> Unit
) {
    var activeColumn by rememberSaveable { mutableStateOf("todo") }
    var title by rememberSaveable { mutableStateOf("") }
    var amount by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf("") }
    val active = KanbanColumns.first { it.id == activeColumn }
    val visibleItems = items.filter { kanbanStageOf(it) == activeColumn }.sortedByDescending { it.createdAt }

    PageColumn {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("Flusso operativo", color = SoftText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text("Lavagna Kanban", color = MainText, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            }
            Text("4 corsie", color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.background(CardBlue, RoundedCornerShape(50)).padding(horizontal = 12.dp, vertical = 7.dp))
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            KanbanColumns.forEach { column ->
                val selected = column.id == activeColumn
                val count = items.count { kanbanStageOf(it) == column.id }
                TextButton(
                    onClick = { activeColumn = column.id; error = "" },
                    modifier = Modifier.weight(1f).background(if (selected) Gold else CardBlue, RoundedCornerShape(14.dp)).padding(vertical = 4.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(column.icon, fontSize = 14.sp)
                        Text(column.label, color = if (selected) NightBlue else SoftText, fontSize = 9.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        Text(count.toString(), color = if (selected) NightBlue else MainText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = CardBlue), shape = RoundedCornerShape(20.dp)) {
            Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("Aggiungi in ${active.icon} ${active.label}", color = MainText, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text(if (activeColumn == "shopping") "Cosa devi comprare?" else "Scrivi una nuova voce...") },
                    singleLine = true,
                    colors = editorFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                if (activeColumn == "shopping") {
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        placeholder = { Text("Spesa prevista · facoltativa") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = editorFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (error.isNotBlank()) Text(error, color = Danger, fontSize = 12.sp)
                Button(
                    onClick = {
                        val cents = if (activeColumn == "shopping") parseAmountCents(amount) else null
                        error = when {
                            title.isBlank() -> "Scrivi prima una voce."
                            activeColumn == "shopping" && amount.isNotBlank() && cents == null -> "Inserisci un importo valido, ad esempio 3,50."
                            else -> ""
                        }
                        if (error.isBlank()) {
                            onAdd(
                                OrganizerListItem(
                                    title = title.trim(),
                                    listType = if (activeColumn == "shopping") "shopping" else "todo",
                                    amountCents = cents,
                                    completed = activeColumn == "done",
                                    kanbanStage = activeColumn
                                )
                            )
                            title = ""
                            amount = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = NightBlue)
                ) { Text("+ Card", fontWeight = FontWeight.Bold) }
            }
        }

        if (visibleItems.isEmpty()) {
            EmptyState("Nessuna card in questa corsia", "Aggiungi una voce oppure sposta qui una card esistente.")
        } else {
            visibleItems.forEach { item ->
                Card(colors = CardDefaults.cardColors(containerColor = CardBlue), shape = RoundedCornerShape(20.dp)) {
                    Column(modifier = Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text(
                                    if (item.listType == "shopping") "🛒 Spesa" else "📌 Lista",
                                    color = if (item.listType == "shopping") Gold else SoftText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(item.title, color = if (item.completed) SoftText else MainText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                item.amountCents?.let { Text(formatCurrency(it), color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                            }
                            TextButton(onClick = { onDelete(item) }) { Text("×", color = Danger, fontSize = 20.sp) }
                        }
                        HorizontalDivider(color = CardBlueLight)
                        Text("Sposta in:", color = SoftText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            KanbanColumns.filter { it.id != activeColumn }.forEach { target ->
                                TextButton(
                                    onClick = {
                                        onChange(
                                            item.copy(
                                                kanbanStage = target.id,
                                                listType = if (target.id == "shopping") "shopping" else "todo",
                                                completed = target.id == "done"
                                            )
                                        )
                                    },
                                    modifier = Modifier.weight(1f).background(CardBlueLight, RoundedCornerShape(10.dp))
                                ) { Text("${target.icon} ${target.label}", color = Gold, fontSize = 9.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PageColumn(safeBottom: Boolean = false, safeTop: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier.fillMaxWidth().widthIn(max = 600.dp)
                .then(if (safeTop) Modifier.statusBarsPadding() else Modifier)
                .then(if (safeBottom) Modifier.navigationBarsPadding() else Modifier)
                .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = content
        )
    }
}

@Composable
private fun LazyPageColumn(safeBottom: Boolean = false, safeTop: Boolean = false, content: LazyListScope.() -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().widthIn(max = 600.dp)
                .then(if (safeTop) Modifier.statusBarsPadding() else Modifier)
                .then(if (safeBottom) Modifier.navigationBarsPadding() else Modifier),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = content
        )
    }
}

@Composable
private fun TodayScreen(
    tasks: List<OrganizerTask>,
    nickname: String,
    signedInAccount: SignedInAccount?,
    onAdd: () -> Unit,
    onAddLog: (String, String, Int, LocalDate) -> Unit,
    onChange: (OrganizerTask) -> Unit,
    onDelete: (OrganizerTask) -> Unit,
    onEdit: (OrganizerTask) -> Unit,
    onManageAccount: () -> Unit,
    onOpenPrivacy: () -> Unit
) {
    var selectedDateValue by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    val selectedDate = LocalDate.parse(selectedDateValue)
    val weekStart = selectedDate.with(DayOfWeek.MONDAY)
    val weekDays = (0L..6L).map { weekStart.plusDays(it) }
    var agendaFilter by rememberSaveable { mutableStateOf("all") }
    var activeTimerTitle by rememberSaveable { mutableStateOf("Attività personale") }
    var activeTimerCategory by rememberSaveable { mutableStateOf("Personale") }
    var timerRunning by rememberSaveable { mutableStateOf(false) }
    var timerSeconds by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(timerRunning) {
        while (timerRunning) {
            delay(1_000)
            timerSeconds += 1
        }
    }
    val selectedTasks = tasks.filter { it.date == selectedDateValue }.sortedBy { it.time ?: "99:99" }
    val visibleTasks = selectedTasks.filter { task ->
        when (agendaFilter) {
            "scheduled" -> !task.completed
            "logged" -> task.completed
            else -> true
        }
    }
    val loggedToday = selectedTasks.count { it.completed }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.ITALY) }
    val monthFormatter = remember { DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ITALY) }
    var profileMenuOpen by remember { mutableStateOf(false) }

    PageColumn {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Task Organizer", color = Gold, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            signedInAccount?.let { account ->
                Box(contentAlignment = Alignment.TopEnd) {
                    AccountAvatar(
                        photoUrl = account.photoUrl,
                        initial = account.email.firstOrNull()?.uppercase() ?: "U",
                        onClick = { profileMenuOpen = true }
                    )
                    DropdownMenu(
                        expanded = profileMenuOpen,
                        onDismissRequest = { profileMenuOpen = false },
                        containerColor = CardBlue,
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                            Text(account.displayName, color = MainText, fontWeight = FontWeight.Bold)
                            Text(account.email, color = SoftText, fontSize = 12.sp)
                        }
                        HorizontalDivider(color = CardBlueLight)
                        DropdownMenuItem(
                            text = { Text("Gestisci account", color = MainText) },
                            onClick = { profileMenuOpen = false; onManageAccount() }
                        )
                        DropdownMenuItem(
                            text = { Text("Privacy e dati", color = MainText) },
                            onClick = { profileMenuOpen = false; onOpenPrivacy() }
                        )
                    }
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(monthFormatter.format(selectedDate).replaceFirstChar { it.titlecase(Locale.ITALY) }, color = SoftText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text(greetingFor(nickname), color = MainText, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Il tuo calendario e registro attività", color = SoftText, fontSize = 13.sp)
        }

        var weekDragDistance by remember { mutableStateOf(0f) }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Scorri per cambiare settimana · tocca una data per vedere gli impegni", color = SoftText, fontSize = 11.sp)
            Row(
                modifier = Modifier.fillMaxWidth().pointerInput(selectedDateValue) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { _, dragAmount ->
                            weekDragDistance += dragAmount
                        },
                        onDragEnd = {
                            when {
                                weekDragDistance <= -80f -> selectedDateValue = selectedDate.plusWeeks(1).toString()
                                weekDragDistance >= 80f -> selectedDateValue = selectedDate.minusWeeks(1).toString()
                            }
                            weekDragDistance = 0f
                        },
                        onDragCancel = { weekDragDistance = 0f }
                    )
                },
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                weekDays.forEach { date ->
                    val isSelected = date == selectedDate
                    val hasScheduled = tasks.any { it.date == date.toString() && !it.completed }
                    val hasLogged = tasks.any { it.date == date.toString() && it.completed }
                    Column(
                        modifier = Modifier.weight(1f).background(if (isSelected) Gold else CardBlue, RoundedCornerShape(18.dp))
                            .clickable { selectedDateValue = date.toString() }.padding(vertical = 9.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(date.dayOfWeek.name.take(3).lowercase().replaceFirstChar { it.titlecase(Locale.ITALY) }, color = if (isSelected) NightBlue else SoftText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(date.dayOfMonth.toString(), color = if (isSelected) NightBlue else MainText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            if (hasScheduled) Box(Modifier.size(5.dp).background(if (isSelected) NightBlue else Gold, RoundedCornerShape(50)))
                            if (hasLogged) Box(Modifier.size(5.dp).background(if (isSelected) NightBlue else Success, RoundedCornerShape(50)))
                        }
                    }
                }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = CardBlueLight), shape = RoundedCornerShape(22.dp)) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Registra attività live", color = MainText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(activeTimerTitle, color = SoftText, fontSize = 12.sp)
                    }
                    Text(formatLiveTimer(timerSeconds), color = Gold, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("💼 Lavoro" to "Lavoro", "🏃 Sport" to "Sport", "📚 Studio" to "Studio").forEach { (label, category) ->
                        TextButton(onClick = { activeTimerTitle = label.drop(3); activeTimerCategory = category }) { Text(label, color = SoftText, fontSize = 11.sp) }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { timerRunning = !timerRunning }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) {
                        Text(if (timerRunning) "Pausa" else "Avvia", color = Gold, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {
                            onAddLog(activeTimerTitle, activeTimerCategory, (timerSeconds / 60).coerceAtLeast(1), selectedDate)
                            timerRunning = false
                            timerSeconds = 0
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = NightBlue)
                    ) { Text("Salva", fontWeight = FontWeight.Bold) }
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            listOf("all" to "Tutto (${selectedTasks.size})", "scheduled" to "📅 Programmate", "logged" to "✓ Registrate").forEach { (key, label) ->
                val active = agendaFilter == key
                TextButton(
                    onClick = { agendaFilter = key },
                    modifier = Modifier.weight(1f).background(if (active) CardBlueLight else CardBlue, RoundedCornerShape(16.dp))
                ) { Text(label, color = if (active) Gold else SoftText, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) }
            }
        }

        Text(dateFormatter.format(selectedDate).replaceFirstChar { it.titlecase(Locale.ITALY) }, color = MainText, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        if (visibleTasks.isEmpty()) {
            EmptyState("Nessuna attività in questa data", if (agendaFilter == "logged") "Non hai ancora registrato attività." else "Puoi pianificare un nuovo impegno quando vuoi.")
        } else {
            visibleTasks.forEach { task ->
                HomeAgendaTaskCard(task = task, onToggle = { onChange(task.copy(completed = !task.completed)) }, onDelete = { onDelete(task) }, onEdit = { onEdit(task) })
            }
        }
        Button(
            onClick = onAdd, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = NightBlue)
        ) { Text("+  Registra / Pianifica", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
    }
}

private fun formatLiveTimer(seconds: Int): String {
    val hours = seconds / 3_600
    val minutes = (seconds % 3_600) / 60
    val remainingSeconds = seconds % 60
    return "%02d:%02d:%02d".format(Locale.ROOT, hours, minutes, remainingSeconds)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeAgendaTaskCard(
    task: OrganizerTask,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { target ->
            when (target) {
                SwipeToDismissBoxValue.StartToEnd -> onToggle()
                SwipeToDismissBoxValue.EndToStart -> onDelete()
                SwipeToDismissBoxValue.Settled -> Unit
            }
            // Keep the card visible: completion updates it and deletion sends it to the recoverable trash.
            false
        }
    )
    SwipeToDismissBox(
        state = dismissState,
        modifier = Modifier.fillMaxWidth(),
        backgroundContent = {
            Row(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.weight(1f).fillMaxHeight().background(Success).padding(start = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("✓", color = NightBlue, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("Completata", color = NightBlue, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Row(
                    modifier = Modifier.weight(1f).fillMaxHeight().background(Danger).padding(end = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    Text("Elimina", color = MainText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.size(8.dp))
                    Text("🗑", fontSize = 18.sp)
                }
            }
        }
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
            Text(task.time ?: "--:--", color = SoftText, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 13.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBlue),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.weight(1f).clickable(onClick = onEdit)
            ) {
                Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier.size(32.dp).background(if (task.completed) Success else CardBlueLight, RoundedCornerShape(50)).clickable(onClick = onToggle),
                        contentAlignment = Alignment.Center
                    ) { Text(if (task.completed) "✓" else "○", color = if (task.completed) NightBlue else Gold, fontWeight = FontWeight.Bold) }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(task.title, color = MainText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("${categoryDisplay(task.category, task.categoryIcon)}${if (task.completed) " · registrata" else ""}", color = SoftText, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountAvatar(photoUrl: String?, initial: String, onClick: () -> Unit) {
    var profileImage by remember(photoUrl) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(photoUrl) {
        profileImage = photoUrl?.let { address ->
            withContext(Dispatchers.IO) {
                loadProfileImage(address)
            }
        }
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = Success.copy(alpha = 0.14f)),
        shape = RoundedCornerShape(50.dp),
        modifier = Modifier.size(36.dp).clickable(onClick = onClick).semantics {
            contentDescription = "Apri menu account"
            role = Role.Button
        }
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            profileImage?.let { image ->
                Image(
                    bitmap = image.asImageBitmap(),
                    contentDescription = "Account Google collegato",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } ?: Text(initial, color = Success, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/** Downloads the optional Google avatar without storing it locally. */
private fun loadProfileImage(address: String): Bitmap? {
    val url = runCatching { URL(address) }.getOrNull() ?: return null
    if (url.protocol.lowercase(Locale.ROOT) != "https") return null
    val connection = runCatching { url.openConnection() as? HttpsURLConnection }.getOrNull() ?: return null
    return try {
        connection.connectTimeout = 4_000
        connection.readTimeout = 4_000
        connection.instanceFollowRedirects = false
        connection.requestMethod = "GET"
        connection.connect()
        if (connection.responseCode != HttpsURLConnection.HTTP_OK || connection.contentLength > MAX_PROFILE_IMAGE_BYTES) {
            null
        } else {
            val bytes = connection.inputStream.use { input ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(8 * 1024)
                var total = 0
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    total += read
                    if (total > MAX_PROFILE_IMAGE_BYTES) return@use null
                    output.write(buffer, 0, read)
                }
                output.toByteArray()
            } ?: return null
            decodeProfileImage(bytes)
        }
    } catch (_: Exception) {
        null
    } finally {
        connection.disconnect()
    }
}

private fun decodeProfileImage(bytes: ByteArray): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    var sampleSize = 1
    while (bounds.outWidth / sampleSize > 512 || bounds.outHeight / sampleSize > 512) sampleSize *= 2
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply {
        inSampleSize = sampleSize
    })
}

private const val MAX_PROFILE_IMAGE_BYTES = 2 * 1024 * 1024

@Composable
private fun ListsScreen(
    items: List<OrganizerListItem>,
    onBack: () -> Unit,
    onAdd: (OrganizerListItem) -> Unit,
    onChange: (OrganizerListItem) -> Unit,
    onDelete: (OrganizerListItem) -> Unit
) {
    var listType by remember { mutableStateOf("todo") }
    var adding by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    val visibleItems = items.filter { it.listType == listType }.sortedWith(compareBy<OrganizerListItem> { it.completed }.thenBy { it.createdAt })
    val pending = visibleItems.count { !it.completed }
    val shoppingTotal = visibleItems.filter { !it.completed }.sumOf { it.amountCents ?: 0L }

    Box(modifier = Modifier.fillMaxSize().organizerBackground()) {
        PageColumn(safeBottom = true, safeTop = true) {
            TextButton(onClick = onBack, modifier = Modifier.align(Alignment.Start)) { Text("← Torna all'organizer", color = Gold) }
            Text("Liste", color = MainText, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Organizza le cose da fare e la spesa in un unico posto.", color = SoftText, fontSize = 13.sp)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { listType = "todo"; adding = false },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (listType == "todo") Gold else CardBlueLight, contentColor = if (listType == "todo") NightBlue else MainText)
                ) { Text("Da fare") }
                Button(
                    onClick = { listType = "shopping"; adding = false },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (listType == "shopping") Gold else CardBlueLight, contentColor = if (listType == "shopping") NightBlue else MainText)
                ) { Text("Spesa") }
            }
            Card(colors = CardDefaults.cardColors(containerColor = CardBlue), shape = RoundedCornerShape(20.dp)) {
                Row(modifier = Modifier.fillMaxWidth().padding(18.dp), horizontalArrangement = Arrangement.SpaceAround) {
                    Stat("$pending", if (listType == "todo") "da fare" else "da comprare")
                    Stat("${visibleItems.count { it.completed }}", "completati")
                    if (listType == "shopping") Stat(formatCurrency(shoppingTotal), "prevista") else Stat("${visibleItems.size}", "totali")
                }
            }
            if (adding) {
                Card(colors = CardDefaults.cardColors(containerColor = CardBlue), shape = RoundedCornerShape(20.dp)) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(if (listType == "todo") "Nuova voce" else "Aggiungi alla spesa", color = MainText, fontWeight = FontWeight.Bold)
                        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Cosa devi ${if (listType == "todo") "fare" else "comprare"}?") }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = editorFieldColors())
                        if (listType == "shopping") {
                            OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Spesa prevista · facoltativa") }, placeholder = { Text("Es. 3,50") }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), colors = editorFieldColors())
                        }
                        if (error.isNotBlank()) Text(error, color = Danger, fontSize = 13.sp)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            TextButton(onClick = { adding = false; title = ""; amount = ""; error = "" }) { Text("Annulla", color = SoftText) }
                            TextButton(onClick = {
                                val cents = if (listType == "shopping") parseAmountCents(amount) else null
                                error = when {
                                    title.isBlank() -> "Scrivi una voce per la lista."
                                    listType == "shopping" && amount.isNotBlank() && cents == null -> "Inserisci un importo valido, ad esempio 3,50."
                                    else -> ""
                                }
                                if (error.isBlank()) {
                                    onAdd(OrganizerListItem(title = title.trim(), listType = listType, amountCents = cents))
                                    title = ""; amount = ""; adding = false
                                }
                            }) { Text("Aggiungi", color = Gold, fontWeight = FontWeight.Bold) }
                        }
                    }
                }
            } else {
                OutlinedButton(onClick = { adding = true }, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(16.dp)) {
                    Text(if (listType == "todo") "+ Aggiungi voce" else "+ Aggiungi prodotto", color = Gold, fontWeight = FontWeight.Bold)
                }
            }
            if (visibleItems.isEmpty()) {
                EmptyState(if (listType == "todo") "Niente da fare" else "Lista spesa vuota", if (listType == "todo") "Aggiungi le tue piccole attività quotidiane." else "Aggiungi quello che ti serve e, se vuoi, la spesa prevista.")
            } else {
                visibleItems.forEach { item ->
                    Card(colors = CardDefaults.cardColors(containerColor = CardBlue), shape = RoundedCornerShape(18.dp)) {
                        Row(modifier = Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                            val toggleDescription = stringResource(if (item.completed) R.string.reopen_task else R.string.complete_task, item.title)
                            Box(
                                modifier = Modifier.size(48.dp).clickable { onChange(item.copy(completed = !item.completed)) }.semantics {
                                    contentDescription = toggleDescription
                                    role = Role.Button
                                },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(if (item.completed) "✓" else "○", color = if (item.completed) Success else Gold, fontSize = 24.sp)
                            }
                            Spacer(Modifier.size(4.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.title, color = if (item.completed) SoftText else MainText, fontWeight = FontWeight.Bold)
                                item.amountCents?.let { Text(formatCurrency(it), color = Gold, fontSize = 13.sp) }
                            }
                            TextButton(onClick = { onDelete(item) }) { Text("Elimina", color = Danger, fontSize = 12.sp) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivitiesScreen(
    tasks: List<OrganizerTask>,
    deletedTasks: List<OrganizerTask>,
    onAdd: () -> Unit,
    onChange: (OrganizerTask) -> Unit,
    onEdit: (OrganizerTask) -> Unit,
    onDelete: (OrganizerTask) -> Unit,
    onRestore: (OrganizerTask) -> Unit,
    onPermanentlyDelete: (OrganizerTask) -> Unit
) {
    var selectedMonth by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf("") }
    var selectedStatus by rememberSaveable { mutableStateOf("all") }
    var filterOpen by rememberSaveable { mutableStateOf<String?>(null) }
    var taskToDeleteId by rememberSaveable { mutableStateOf<String?>(null) }
    val taskToDelete = taskToDeleteId?.let { id -> tasks.firstOrNull { it.id == id } }
    var trashOpen by rememberSaveable { mutableStateOf(false) }
    val monthOptions = listOf("" to "Tutti i mesi") + tasks.map { it.date.take(7) }.distinct().sorted().map { it to formatMonthKey(it) }
    val categoryOptions = listOf("" to "Tutte le categorie") + tasks.map { it.category to it.categoryIcon }
        .distinctBy { it.first }
        .sortedBy { it.first }
        .map { (category, icon) -> category to categoryDisplay(category, icon) }
    val statusOptions = listOf("all" to "Tutte", "todo" to "Da fare", "done" to "Completate")
    val sortedTasks by remember {
        derivedStateOf {
            tasks.filter { task ->
            (selectedMonth.isBlank() || task.date.startsWith(selectedMonth)) &&
                (selectedCategory.isBlank() || task.category == selectedCategory) &&
                when (selectedStatus) {
                    "todo" -> !task.completed
                    "done" -> task.completed
                    else -> true
                }
            }.sortedWith(compareBy<OrganizerTask> { it.completed }.thenBy { it.date }.thenBy { it.time ?: "99:99" })
        }
    }
    val completed = tasks.count { it.completed }

    LazyPageColumn {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Attività", color = MainText, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Tutto quello che devi fare, in un unico posto.", color = SoftText)
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = CardBlue), shape = RoundedCornerShape(24.dp)) {
                Row(modifier = Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.SpaceAround) {
                    Stat("${tasks.size}", "totali")
                    Stat("$completed", "completate")
                    Stat("${tasks.size - completed}", "da fare")
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = CardBlue), shape = RoundedCornerShape(20.dp)) {
                Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Filtra attività", color = MainText, fontWeight = FontWeight.Bold)
                    Text("Tocca una voce per scegliere", color = SoftText, fontSize = 12.sp)
                    FilterChoice("Mese", monthOptions.firstOrNull { it.first == selectedMonth }?.second ?: "Tutti i mesi") { filterOpen = "month" }
                    FilterChoice("Categoria", categoryOptions.firstOrNull { it.first == selectedCategory }?.second ?: "Tutte le categorie") { filterOpen = "category" }
                    FilterChoice("Stato", statusOptions.firstOrNull { it.first == selectedStatus }?.second ?: "Tutte") { filterOpen = "status" }
                }
            }
        }
        item { Text("${sortedTasks.size} attività mostrate", color = SoftText, fontSize = 13.sp) }
        if (deletedTasks.isNotEmpty()) {
            item {
                OutlinedButton(onClick = { trashOpen = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                    Text("Cestino (${deletedTasks.size})", color = Gold, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        if (sortedTasks.isEmpty()) {
            item { EmptyState("Nessuna attività trovata", "Prova a cambiare i filtri oppure aggiungi una nuova attività.") }
        } else {
            items(sortedTasks, key = { it.id }) { task ->
                TaskCard(task, onToggle = { onChange(task.copy(completed = !task.completed)) }, onEdit = { onEdit(task) }, onDelete = { taskToDeleteId = task.id })
            }
        }
        item {
            OutlinedButton(onClick = onAdd, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp)) {
                Text("+ Nuova attività", color = Gold, fontWeight = FontWeight.Bold)
            }
        }
    }
    filterOpen?.let { filter ->
        val title = when (filter) {
            "month" -> "Filtra per mese"
            "category" -> "Filtra per categoria"
            else -> "Filtra per stato"
        }
        val options = when (filter) {
            "month" -> monthOptions
            "category" -> categoryOptions
            else -> statusOptions
        }
        AlertDialog(
            onDismissRequest = { filterOpen = null },
            containerColor = CardBlue,
            title = { Text(title, color = MainText) },
            text = {
                Column {
                    options.forEach { (value, label) ->
                        Text(label, color = MainText, modifier = Modifier.fillMaxWidth().clickable {
                            when (filter) {
                                "month" -> selectedMonth = value
                                "category" -> selectedCategory = value
                                else -> selectedStatus = value
                            }
                            filterOpen = null
                        }.padding(vertical = 12.dp))
                    }
                }
            },
            confirmButton = { TextButton(onClick = { filterOpen = null }) { Text("Chiudi", color = Gold) } }
        )
    }
    taskToDelete?.let { task ->
        AlertDialog(
            onDismissRequest = { taskToDeleteId = null },
            containerColor = CardBlue,
            title = { Text("Eliminare attività?", color = MainText) },
            text = { Text("Vuoi spostare “${task.title}” nel Cestino? Potrai ripristinarla in seguito.", color = SoftText) },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(task)
                    taskToDeleteId = null
                }) { Text("Elimina", color = Danger) }
            },
            dismissButton = { TextButton(onClick = { taskToDeleteId = null }) { Text("Annulla", color = SoftText) } }
        )
    }
    if (trashOpen) {
        TrashDialog(
            tasks = deletedTasks.sortedByDescending { it.createdAt },
            onDismiss = { trashOpen = false },
            onRestore = onRestore,
            onPermanentlyDelete = onPermanentlyDelete
        )
    }
}

@Composable
private fun TrashDialog(
    tasks: List<OrganizerTask>,
    onDismiss: () -> Unit,
    onRestore: (OrganizerTask) -> Unit,
    onPermanentlyDelete: (OrganizerTask) -> Unit
) {
    var permanentDeleteTask by remember { mutableStateOf<OrganizerTask?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBlue,
        title = { Text("Cestino", color = MainText) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Le attività nel Cestino possono essere ripristinate per 30 giorni.", color = SoftText, fontSize = 13.sp)
                tasks.forEach { task ->
                    Card(colors = CardDefaults.cardColors(containerColor = CardBlueLight), shape = RoundedCornerShape(14.dp)) {
                        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                            Text(task.title, color = MainText, fontWeight = FontWeight.Bold)
                            Text("${formatItalianDate(task.date)}${formatTimeRange(task)?.let { " · $it" } ?: ""}", color = SoftText, fontSize = 12.sp)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                TextButton(onClick = { onRestore(task) }) { Text("Ripristina", color = Success) }
                                TextButton(onClick = { permanentDeleteTask = task }) { Text("Elimina", color = Danger) }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Chiudi", color = Gold) } }
    )
    permanentDeleteTask?.let { task ->
        AlertDialog(
            onDismissRequest = { permanentDeleteTask = null },
            containerColor = CardBlue,
            title = { Text("Eliminare definitivamente?", color = MainText) },
            text = { Text("“${task.title}” non potrà più essere recuperata dal Cestino.", color = SoftText) },
            confirmButton = {
                TextButton(onClick = {
                    onPermanentlyDelete(task)
                    permanentDeleteTask = null
                }) { Text("Elimina definitivamente", color = Danger) }
            },
            dismissButton = { TextButton(onClick = { permanentDeleteTask = null }) { Text("Annulla", color = SoftText) } }
        )
    }
}

@Composable
private fun CompactTaskCard(task: OrganizerTask, onToggle: () -> Unit, onEdit: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = CardBlueLight), shape = RoundedCornerShape(18.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(48.dp).clickable(onClick = onToggle).semantics {
                    contentDescription = if (task.completed) "Segna ${task.title} come da fare" else "Segna ${task.title} come completata"
                    role = Role.Button
                },
                contentAlignment = Alignment.Center
            ) {
                Text(if (task.completed) "✓" else "○", color = taskCompletionColor(task), fontSize = 24.sp)
            }
            Spacer(Modifier.size(4.dp))
            Column(modifier = Modifier.weight(1f).clickable(onClick = onEdit)) {
                Text(task.title, color = if (task.completed) SoftText else MainText, fontWeight = FontWeight.Bold)
                Text(listOfNotNull(formatTimeRange(task), categoryDisplay(task.category, task.categoryIcon)).joinToString(" · "), color = SoftText, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun TaskCard(task: OrganizerTask, onToggle: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    val titleColor = taskCardTextColor(task)
    val mutedColor = taskCardMutedColor(task)
    Card(colors = CardDefaults.cardColors(containerColor = taskCardBackground(task.cardColor)), shape = RoundedCornerShape(20.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(48.dp).clickable(onClick = onToggle).semantics {
                        contentDescription = if (task.completed) "Segna ${task.title} come da fare" else "Segna ${task.title} come completata"
                        role = Role.Button
                    },
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (task.completed) "✓" else "○", color = taskCompletionColor(task), fontSize = 25.sp)
                }
                Spacer(Modifier.size(2.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(task.title, color = if (task.completed) mutedColor else titleColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("${formatItalianDate(task.date)}${formatTimeRange(task)?.let { " · $it" } ?: ""}", color = mutedColor, fontSize = 13.sp)
                }
            }
            Text("${categoryDisplay(task.category, task.categoryIcon)} · priorità ${task.priority.lowercase()}", color = Gold, fontSize = 13.sp)
            task.examStatus?.let { Text("Esame: $it", color = Gold, fontSize = 13.sp) }
            if (task.note.isNotBlank()) Text(task.note, color = mutedColor, fontSize = 14.sp)
            HorizontalDivider(color = if (task.cardColor == DefaultCardColor) CardBlueLight else mutedColor.copy(alpha = 0.25f))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = onToggle) { Text(if (task.completed) "Riapri" else "Completa", color = Success) }
                TextButton(onClick = onEdit) { Text("Modifica", color = Gold) }
                TextButton(onClick = onDelete) { Text("Elimina", color = Danger) }
            }
        }
    }
}

@Composable
private fun CalendarScreen(
    tasks: List<OrganizerTask>,
    onAdd: () -> Unit,
    onChange: (OrganizerTask) -> Unit,
    onEdit: (OrganizerTask) -> Unit
) {
    var displayedMonthValue by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
    var selectedDateValue by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    val displayedMonth = YearMonth.parse(displayedMonthValue)
    val selectedDate = LocalDate.parse(selectedDateValue)
    val monthFormatter = remember { DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ITALY) }
    val firstDayOffset = displayedMonth.atDay(1).dayOfWeek.value - 1
    val daysInMonth = displayedMonth.lengthOfMonth()
    val gridCells = (0 until 42).toList()
    val selectedTasks by remember(selectedDateValue) {
        derivedStateOf {
            tasks.filter { it.date == selectedDateValue }.sortedBy { it.time ?: "99:99" }
        }
    }
    val previousMonthDescription = stringResource(R.string.previous_month)
    val nextMonthDescription = stringResource(R.string.next_month)

    LazyPageColumn {
        item { Text("Calendario", color = MainText, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
        item {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(
                    onClick = { displayedMonthValue = displayedMonth.minusMonths(1).toString() },
                    modifier = Modifier.size(48.dp).semantics { contentDescription = previousMonthDescription }
                ) { Icon(painterResource(R.drawable.ic_arrow_left), contentDescription = null, tint = Gold) }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(displayedMonth.format(monthFormatter).replaceFirstChar { it.titlecase(Locale.ITALY) }, color = Gold, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    TextButton(onClick = { displayedMonthValue = YearMonth.now().toString(); selectedDateValue = LocalDate.now().toString() }) { Text("Oggi", color = SoftText, fontSize = 12.sp) }
                }
                TextButton(
                    onClick = { displayedMonthValue = displayedMonth.plusMonths(1).toString() },
                    modifier = Modifier.size(48.dp).semantics { contentDescription = nextMonthDescription }
                ) { Icon(painterResource(R.drawable.ic_arrow_right), contentDescription = null, tint = Gold) }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = CardBlue), shape = RoundedCornerShape(20.dp)) {
                Column(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        listOf("L", "M", "M", "G", "V", "S", "D").forEach { day ->
                            Text(day, modifier = Modifier.weight(1f), color = Gold, fontSize = 12.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                        }
                    }
                    gridCells.chunked(7).forEach { week ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            week.forEach { cell ->
                                val dayNumber = cell - firstDayOffset + 1
                                val date = if (dayNumber in 1..daysInMonth) displayedMonth.atDay(dayNumber) else null
                                val hasTasks = date != null && tasks.any { it.date == date.toString() }
                                val isSelected = date == selectedDate
                                CalendarDayCell(date, hasTasks, isSelected) { clickedDate -> selectedDateValue = clickedDate.toString() }
                            }
                        }
                    }
                }
            }
        }
        item { Text("${formatItalianDate(selectedDate.toString())}", color = MainText, fontSize = 19.sp, fontWeight = FontWeight.Bold) }
        if (selectedTasks.isEmpty()) {
            item { EmptyState("Nessun impegno", "Non hai attività previste per questo giorno.") }
        } else {
            items(selectedTasks, key = { it.id }) { task ->
                CompactTaskCard(
                    task,
                    onToggle = { onChange(task.copy(completed = !task.completed)) },
                    onEdit = { onEdit(task) }
                )
            }
        }
        item {
            OutlinedButton(onClick = onAdd, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp)) {
                Text("+ Nuova attività", color = Gold, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun RowScope.CalendarDayCell(date: LocalDate?, hasTasks: Boolean, isSelected: Boolean, onSelected: (LocalDate) -> Unit) {
    Box(
        modifier = Modifier.weight(1f).height(48.dp).background(
            color = when {
                isSelected -> Gold
                date == LocalDate.now() -> CardBlueLight
                else -> Color.Transparent
            },
            shape = RoundedCornerShape(10.dp)
        ).then(if (date != null) Modifier.clickable { onSelected(date) } else Modifier),
        contentAlignment = Alignment.Center
    ) {
        if (date != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(date.dayOfMonth.toString(), color = if (isSelected) NightBlue else MainText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                if (hasTasks) Text("•", color = if (isSelected) NightBlue else Gold, fontSize = 16.sp, lineHeight = 12.sp)
            }
        }
    }
}

@Composable
private fun AccountScreen(
    nickname: String,
    onNicknameChanged: (String) -> Unit,
    onOpenPrivacy: () -> Unit,
    signedInAccount: SignedInAccount?,
    accountWorking: Boolean,
    accountMessage: String?,
    onGoogleSignIn: () -> Unit,
    onSignOut: (deleteLocalData: Boolean) -> Unit,
    onDeleteAccount: (deleteLocalData: Boolean) -> Unit,
    onOpenBackup: () -> Unit
) {
    var nicknameDialogOpen by remember { mutableStateOf(false) }
    var nicknameDraft by remember(nickname) { mutableStateOf(nickname) }
    var signOutDialogOpen by remember { mutableStateOf(false) }
    var deleteAccountDialogOpen by remember { mutableStateOf(false) }
    PageColumn {
        Text("Account", color = MainText, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Profilo, dati e collegamenti personali.", color = SoftText)
        Text("STATO ACCESSO", color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        if (signedInAccount == null) {
            InfoCard("Stai usando l'app come ospite", "Puoi organizzare tutte le attività senza creare un account. L'accesso non invia né carica automaticamente le tue attività.", "Accesso facoltativo")
            Button(
                onClick = onGoogleSignIn,
                enabled = !accountWorking,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = NightBlue)
            ) {
                if (accountWorking) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = NightBlue, strokeWidth = 2.dp)
                else Text("Accedi con Google", fontWeight = FontWeight.Bold)
            }
        } else {
            InfoCard("Connessa come ${signedInAccount.displayName}", signedInAccount.email, "Google · Account facoltativo")
            OutlinedButton(
                onClick = { signOutDialogOpen = true },
                enabled = !accountWorking,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(16.dp)
            ) { Text("Esci dall'account", color = Gold, fontWeight = FontWeight.Bold) }
            TextButton(onClick = { deleteAccountDialogOpen = true }, enabled = !accountWorking, modifier = Modifier.fillMaxWidth()) {
                Text("Elimina account", color = Danger, fontWeight = FontWeight.SemiBold)
            }
        }
        accountMessage?.let { InfoCard("Accesso", it, "Puoi riprovare") }
        Text("PROFILO", color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Card(colors = CardDefaults.cardColors(containerColor = CardBlue), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth().clickable { nicknameDialogOpen = true }) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text("Il tuo nickname", color = MainText, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(if (nickname.isBlank()) "Tocca qui per sceglierlo" else nickname, color = Gold, fontSize = 15.sp)
            }
        }
        Text("DATI", color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        InfoCard("Archivio locale cifrato", "Le attività restano su questo telefono e sono protette da una chiave del dispositivo.", "Sempre protetto")
        OutlinedButton(onClick = onOpenPrivacy, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(16.dp)) {
            Text("Privacy e dati", color = Gold, fontWeight = FontWeight.Bold)
        }
        InfoCard("Backup cifrato", "Crea un file protetto da password e conservalo dove preferisci: Drive, OneDrive, disco esterno o altro spazio personale.", "Disponibile")
        OutlinedButton(onClick = onOpenBackup, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(16.dp)) {
            Text("Apri backup", color = Gold, fontWeight = FontWeight.Bold)
        }
        Text("COLLEGAMENTI", color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        InfoCard("Calendario personale", "Potrai collegare Google Calendar o Outlook per vedere gli impegni personali senza mescolare i dati con quelli del PC.", "Prossimamente")
    }
    if (signOutDialogOpen) {
        AlertDialog(
            onDismissRequest = { signOutDialogOpen = false },
            containerColor = CardBlue,
            title = { Text("Uscire dall'account?", color = MainText) },
            text = { Text("Puoi mantenere le attività cifrate sul telefono oppure cancellarle definitivamente da questo dispositivo.", color = SoftText) },
            confirmButton = {
                TextButton(onClick = { signOutDialogOpen = false; onSignOut(false) }) {
                    Text("Mantieni i dati", color = Gold)
                }
            },
            dismissButton = {
                TextButton(onClick = { signOutDialogOpen = false; onSignOut(true) }) {
                    Text("Cancella dati locali", color = Danger)
                }
            }
        )
    }
    if (deleteAccountDialogOpen) {
        AlertDialog(
            onDismissRequest = { deleteAccountDialogOpen = false },
            containerColor = CardBlue,
            title = { Text("Eliminare l'account?", color = MainText) },
            text = { Text("Google chiederà una nuova conferma. L'account Firebase verrà eliminato; le attività locali potrai mantenerle oppure eliminarle anche dal telefono.", color = SoftText) },
            confirmButton = {
                TextButton(onClick = { deleteAccountDialogOpen = false; onDeleteAccount(false) }) {
                    Text("Mantieni dati locali", color = Gold)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteAccountDialogOpen = false; onDeleteAccount(true) }) {
                    Text("Elimina anche i dati", color = Danger)
                }
            }
        )
    }
    if (nicknameDialogOpen) {
        AlertDialog(
            onDismissRequest = { nicknameDialogOpen = false }, containerColor = CardBlue,
            title = { Text("Come vuoi essere chiamata?", color = MainText) },
            text = { OutlinedTextField(value = nicknameDraft, onValueChange = { nicknameDraft = it }, label = { Text("Nickname") }, singleLine = true, colors = editorFieldColors()) },
            confirmButton = { TextButton(onClick = { onNicknameChanged(nicknameDraft.trim()); nicknameDialogOpen = false }) { Text("Salva", color = Gold) } },
            dismissButton = { TextButton(onClick = { nicknameDialogOpen = false }) { Text("Annulla", color = SoftText) } }
        )
    }
}

@Composable
private fun PrivacyInfoScreen(onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().organizerBackground()) {
        PageColumn(safeBottom = true, safeTop = true) {
            Text("Privacy e dati", color = MainText, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Informazioni sempre disponibili e comprensibili.", color = SoftText)
            InfoCard("Dati sul telefono", "Attività, note, liste, categorie, preferenze e PIN restano nel telefono in un archivio cifrato. Non vengono inviati agli sviluppatori.", "Locale e cifrato")
            InfoCard("Account facoltativo", "L'accesso Google serve solo a riconoscere il tuo profilo nell'app. Non carica automaticamente le attività, non attiva backup e non dà accesso ai tuoi file.", "A tua scelta")
            InfoCard("Backup sotto il tuo controllo", "Puoi creare un file cifrato con password da Opzioni. L'app non lo invia da sola su Drive, OneDrive o su server propri: scegli tu dove conservarlo.", "Manuale e cifrato")
            InfoCard("Niente tracciamento", "Non vendiamo dati, non mostriamo pubblicità e non analizziamo le tue attività. Le notifiche saranno richieste solo quando deciderai di usarle.", "Nessuna pubblicità")
            InfoCard("Il tuo controllo", "Quando uscirai dall'account potrai scegliere se mantenere o cancellare i dati locali. L'eliminazione dell'account sarà una procedura separata e protetta.", "Decidi tu")
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(16.dp)) {
                Text("Torna all'account", color = Gold, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    selectedTheme: OrganizerTheme,
    followSystemTheme: Boolean,
    onThemeChanged: (OrganizerTheme) -> Unit,
    onFollowSystemThemeChanged: (Boolean) -> Unit,
    security: AppSecurity,
    biometricAvailable: Boolean,
    onSecurityChanged: () -> Unit,
    onOpenBackup: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pinDialogOpen by remember { mutableStateOf(false) }
    var disableDialogOpen by remember { mutableStateOf(false) }
    var pinEnabled by remember { mutableStateOf(security.hasPin()) }
    var biometricEnabled by remember { mutableStateOf(security.isBiometricEnabled()) }
    var hideWidgetTitles by remember { mutableStateOf(TaskWidgetUpdater.areTaskTitlesHidden(context)) }
    PageColumn {
        Text("Opzioni", color = MainText, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Aspetto, promemoria e schermata Home.", color = SoftText)
        Text("ASPETTO", color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Card(colors = CardDefaults.cardColors(containerColor = CardBlue), shape = RoundedCornerShape(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("Segui il tema del telefono", color = MainText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("Usa Originale Dark quando il telefono è scuro e Carta Dorata quando è chiaro.", color = SoftText, fontSize = 13.sp)
                }
                Switch(checked = followSystemTheme, onCheckedChange = onFollowSystemThemeChanged)
            }
        }
        Text("Temi gratuiti", color = SoftText, fontSize = 13.sp)
        ThemeChoiceCard(OrganizerTheme.ORIGINAL_DARK, "🌙 Originale Dark", "Blu notte classico con accenti oro", selectedTheme, false, onThemeChanged)
        ThemeChoiceCard(OrganizerTheme.CARTA_DORATA, "📜 Carta Dorata", "Chiaro caldo, morbido e leggibile", selectedTheme, false, onThemeChanged)
        Text("Temi Plus", color = SoftText, fontSize = 13.sp)
        ThemeChoiceCard(OrganizerTheme.AURORA_MIDNIGHT, "✨ Aurora Midnight", "Vetro scuro con bagliori ciano e viola", selectedTheme, true, onThemeChanged)
        ThemeChoiceCard(OrganizerTheme.AURORA_NORDIC, "❄️ Aurora Nordic", "Vetro chiaro iridescente, menta e cielo", selectedTheme, true, onThemeChanged)
        Text("SCHERMATA HOME", color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        InfoCard(
            "Widget per la schermata Home",
            "Tieni premuto uno spazio libero nella schermata Home, tocca Widget e cerca Task Organizer. Puoi scegliere Progresso, Riepilogo o Agenda: mostrano le attività di oggi e il tasto + apre subito una nuova attività.",
            "Gratuito"
        )
        Card(colors = CardDefaults.cardColors(containerColor = CardBlue), shape = RoundedCornerShape(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("Nascondi titoli nei widget", color = MainText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("Sulla schermata Home vedrai “Attività pianificata” invece del titolo. Categoria e orario restano visibili.", color = SoftText, fontSize = 13.sp)
                }
                Switch(
                    checked = hideWidgetTitles,
                    onCheckedChange = { hidden ->
                        hideWidgetTitles = hidden
                        TaskWidgetUpdater.setTaskTitlesHidden(context, hidden)
                        scope.launch(Dispatchers.IO) { TaskWidgetUpdater.updateAll(context) }
                    }
                )
            }
        }
        Text("ATTIVITÀ", color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        InfoCard("Promemoria e sveglie", "Li imposti in ogni attività: puoi scegliere anticipo, ripetizione, notifica o sveglia.", "Disponibile")
        Text("DATI E BACKUP", color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Card(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenBackup),
            colors = CardDefaults.cardColors(containerColor = CardBlue),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Backup cifrato", color = MainText, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text("Crea una copia protetta da password oppure ripristina un tuo backup. PIN, account e foto profilo non vengono mai inclusi.", color = SoftText, fontSize = 13.sp)
                Text("Apri backup →", color = Gold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
        Text("SICUREZZA APP", color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Card(
            modifier = Modifier.fillMaxWidth().clickable { pinDialogOpen = true },
            colors = CardDefaults.cardColors(containerColor = CardBlue),
            shape = RoundedCornerShape(20.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Blocco app", color = MainText, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text(
                        if (pinEnabled) "Il PIN protegge l'accesso quando riapri l'app. Tocca la scheda per cambiarlo." else "Attiva un PIN personale per proteggere le tue attività.",
                        color = SoftText,
                        fontSize = 14.sp
                    )
                }
                Switch(
                    checked = pinEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled) pinDialogOpen = true else disableDialogOpen = true
                    }
                )
            }
        }
        if (pinEnabled && biometricAvailable) {
            Card(colors = CardDefaults.cardColors(containerColor = CardBlue), shape = RoundedCornerShape(20.dp)) {
                Row(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Sblocco biometrico", color = MainText, fontWeight = FontWeight.Bold)
                        Text("Usa impronta o riconoscimento configurato sul telefono.", color = SoftText, fontSize = 13.sp)
                    }
                    Switch(checked = biometricEnabled, onCheckedChange = {
                        biometricEnabled = it
                        security.setBiometricEnabled(it)
                    })
                }
            }
        }
        Text("INFORMAZIONI", color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        InfoCard("Compatibilità", "L'app funziona da Android 7.0 in poi. L'impronta richiede Android 9 o successivo e un telefono compatibile.", "Android 7.0+")
    }
    if (pinDialogOpen) {
        PinSetupDialog(
            security = security,
            changing = pinEnabled,
            onDismiss = { pinDialogOpen = false },
            onSave = {
                security.setPin(it)
                pinEnabled = true
                pinDialogOpen = false
                onSecurityChanged()
            }
        )
    }
    if (disableDialogOpen) {
        AlertDialog(
            onDismissRequest = { disableDialogOpen = false },
            containerColor = CardBlue,
            title = { Text("Disattivare il blocco?", color = MainText) },
            text = { Text("L'app tornerà ad aprirsi senza PIN.", color = SoftText) },
            confirmButton = {
                TextButton(onClick = {
                    security.clear()
                    pinEnabled = false
                    biometricEnabled = false
                    disableDialogOpen = false
                    onSecurityChanged()
                }) { Text("Disattiva", color = Danger) }
            },
            dismissButton = { TextButton(onClick = { disableDialogOpen = false }) { Text("Annulla", color = SoftText) } }
        )
    }
}

@Composable
private fun BackupScreen(
    store: EncryptedTaskStore,
    onBack: () -> Unit,
    pendingImportUri: Uri?,
    onChooseImport: () -> Unit,
    onImportUriHandled: () -> Unit,
    pendingExportUri: Uri?,
    onCreateExport: (String, CharArray) -> Unit,
    takeExportPassword: () -> CharArray?,
    onExportUriHandled: () -> Unit,
    onImported: (TaskArchive) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var exportPassword by remember { mutableStateOf("") }
    var exportConfirmation by remember { mutableStateOf("") }
    var importedBytes by remember { mutableStateOf<ByteArray?>(null) }
    var importPassword by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var working by remember { mutableStateOf(false) }

    LaunchedEffect(pendingImportUri) {
        val uri = pendingImportUri ?: return@LaunchedEffect
        working = true
        val result = withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use(::readBackupBytes)
                    ?: error("Non è stato possibile leggere il file scelto.")
            }
        }
        working = false
        result.onSuccess {
            importedBytes = it
            importPassword = ""
            message = "File selezionato. Inserisci la password del backup per controllarlo e ripristinarlo."
        }.onFailure {
            importedBytes = null
            message = "Il file non può essere letto: scegli un backup di Task Organizer integro."
        }
        // Clear the hand-off only after the read finishes. Clearing it before this point
        // changes the LaunchedEffect key and cancels the read, leaving the loader visible.
        onImportUriHandled()
    }
    LaunchedEffect(pendingExportUri) {
        val uri = pendingExportUri ?: return@LaunchedEffect
        val password = takeExportPassword()
        if (password == null) {
            onExportUriHandled()
            return@LaunchedEffect
        }
        working = true
        val result = withContext(Dispatchers.IO) {
            runCatching {
                try {
                    val encrypted = store.exportPasswordProtectedBackup(password)
                    context.contentResolver.openOutputStream(uri)?.use { output ->
                        output.write(encrypted)
                        output.flush()
                    } ?: error("Non è stato possibile creare il file scelto.")
                } finally {
                    password.fill('\u0000')
                }
            }
        }
        working = false
        result.onSuccess {
            exportPassword = ""
            exportConfirmation = ""
            message = "Backup creato. Conservalo in un luogo sicuro: senza la password non potrà essere recuperato."
        }.onFailure {
            message = "Non è stato possibile creare il backup. Controlla lo spazio disponibile e riprova."
        }
        onExportUriHandled()
    }

    Box(modifier = Modifier.fillMaxSize().organizerBackground()) {
        PageColumn(safeBottom = true, safeTop = true) {
            TextButton(onClick = onBack, modifier = Modifier.align(Alignment.Start)) { Text("← Torna alle opzioni", color = Gold) }
            Text("Backup cifrato", color = MainText, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("I backup contengono solo attività, liste e categorie. PIN, account e foto profilo restano esclusi.", color = SoftText)
            InfoCard("La password è solo tua", "Non viene salvata nell'app e non può essere recuperata. Puoi conservare il file dove preferisci, anche su Drive o OneDrive.", "Protetto")

            Text("CREA UNA COPIA", color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = exportPassword,
                onValueChange = { exportPassword = it },
                label = { Text("Password del backup") },
                placeholder = { Text("Almeno 8 caratteri") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                colors = editorFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = if (exportPassword.length < 8) {
                    "Scegli almeno 8 caratteri (${exportPassword.length}/8)."
                } else {
                    "Password valida."
                },
                color = if (exportPassword.length < 8) SoftText else Success,
                fontSize = 12.sp
            )
            OutlinedTextField(
                value = exportConfirmation,
                onValueChange = { exportConfirmation = it },
                label = { Text("Ripeti la password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                colors = editorFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = when {
                    exportConfirmation.isEmpty() -> "Ripeti la password per confermarla."
                    exportConfirmation != exportPassword -> "Le password non coincidono."
                    else -> "Le password coincidono."
                },
                color = when {
                    exportConfirmation.isEmpty() -> SoftText
                    exportConfirmation != exportPassword -> Danger
                    else -> Success
                },
                fontSize = 12.sp
            )
            Text("Per una protezione migliore scegli una frase lunga e unica, non una password breve.", color = SoftText, fontSize = 12.sp)
            Button(
                onClick = {
                    when {
                        exportPassword.length < 8 -> message = "Scegli una password di almeno 8 caratteri."
                        exportPassword != exportConfirmation -> message = "Le due password non coincidono."
                        else -> {
                            onCreateExport("task-organizer-backup-${LocalDate.now()}.tob", exportPassword.toCharArray())
                        }
                    }
                },
                enabled = !working && exportPassword.length >= 8 && exportPassword == exportConfirmation,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = NightBlue)
            ) { Text("Crea backup", fontWeight = FontWeight.Bold) }

            HorizontalDivider(color = CardBlueLight)
            Text("RIPRISTINA UNA COPIA", color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("Scegli un file .tob. Se hai il PIN attivo, al rientro dall'archivio Android dovrai sbloccare l'app.", color = SoftText, fontSize = 13.sp)
            OutlinedButton(
                onClick = onChooseImport,
                enabled = !working,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(16.dp)
            ) { Text("Scegli il backup da ripristinare", color = Gold, fontWeight = FontWeight.Bold) }
            if (importedBytes != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBlueLight),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Backup selezionato", color = MainText, fontWeight = FontWeight.Bold)
                        Text("Inserisci la password scelta al momento della creazione. Le attività, liste e categorie attuali verranno sostituite.", color = SoftText, fontSize = 13.sp)
                    }
                }
                OutlinedTextField(
                    value = importPassword,
                    onValueChange = { importPassword = it },
                    label = { Text("Password del backup") },
                    placeholder = { Text("La password scelta durante la creazione") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    colors = editorFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = if (importPassword.length < 8) {
                        "Inserisci almeno 8 caratteri (${importPassword.length}/8). Non è il PIN dell'app."
                    } else {
                        "Password pronta. Il ripristino sostituirà i dati attuali."
                    },
                    color = if (importPassword.length < 8) SoftText else Success,
                    fontSize = 12.sp
                )
                Button(
                    onClick = {
                        val data = importedBytes
                        if (importPassword.length < 8 || data == null) {
                            message = "Inserisci la password usata per creare il backup."
                        } else {
                            working = true
                            scope.launch {
                                val result = withContext(Dispatchers.IO) {
                                    runCatching {
                                        val passwordChars = importPassword.toCharArray()
                                        try {
                                            store.importPasswordProtectedBackup(data, passwordChars)
                                        } finally {
                                            passwordChars.fill('\u0000')
                                        }
                                    }
                                }
                                working = false
                                result.onSuccess { archive ->
                                    importPassword = ""
                                    importedBytes = null
                                    onImported(archive)
                                    message = "Backup ripristinato. Promemoria e widget sono stati aggiornati."
                                }.onFailure {
                                    message = "Password errata oppure backup non valido. I dati attuali non sono stati modificati."
                                }
                            }
                        }
                    },
                    enabled = !working && importPassword.length >= 8,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Danger, contentColor = MainText)
                ) { Text("Ripristina e sostituisci i dati", fontWeight = FontWeight.Bold) }
            }
            if (working) CircularProgressIndicator(color = Gold, modifier = Modifier.align(Alignment.CenterHorizontally))
            message?.let { Text(it, color = SoftText, fontSize = 13.sp) }
        }
    }
}

private fun readBackupBytes(input: java.io.InputStream): ByteArray {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(8 * 1024)
    var total = 0
    while (true) {
        val read = input.read(buffer)
        if (read < 0) break
        total += read
        require(total <= MAX_BACKUP_FILE_BYTES) { "Il file è troppo grande." }
        output.write(buffer, 0, read)
    }
    return output.toByteArray()
}

private const val MAX_BACKUP_FILE_BYTES = 10 * 1024 * 1024

@Composable
private fun ThemeChoiceCard(
    theme: OrganizerTheme,
    title: String,
    subtitle: String,
    selectedTheme: OrganizerTheme,
    isPlus: Boolean,
    onThemeChanged: (OrganizerTheme) -> Unit
) {
    val previewBackground = when (theme) {
        OrganizerTheme.ORIGINAL_DARK -> Color(0xFF152036)
        OrganizerTheme.CARTA_DORATA -> Color(0xFFFFFDF8)
        OrganizerTheme.AURORA_MIDNIGHT -> Color(0xFF171E3A)
        OrganizerTheme.AURORA_NORDIC -> Color(0xFFF0F9F8)
    }
    val previewText = if (theme.isDark) Color(0xFFF8FAFC) else Color(0xFF14243B)
    val previewAccent = when (theme) {
        OrganizerTheme.ORIGINAL_DARK -> Color(0xFFD7B46A)
        OrganizerTheme.CARTA_DORATA -> Color(0xFFA47528)
        OrganizerTheme.AURORA_MIDNIGHT -> Color(0xFF38BDF8)
        OrganizerTheme.AURORA_NORDIC -> Color(0xFF0D9488)
    }
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onThemeChanged(theme) },
        colors = CardDefaults.cardColors(containerColor = previewBackground),
        shape = RoundedCornerShape(20.dp),
        border = if (selectedTheme == theme) BorderStroke(2.dp, previewAccent) else null
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = previewText, fontWeight = FontWeight.Bold)
                Text(subtitle, color = previewText.copy(alpha = 0.72f), fontSize = 13.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                if (isPlus) {
                    Text("PLUS", color = previewAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(if (selectedTheme == theme) "✓ Selezionato" else "Prova", color = previewAccent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                } else {
                    Switch(
                        checked = selectedTheme == theme,
                        onCheckedChange = { onThemeChanged(theme) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PinSetupDialog(
    security: AppSecurity,
    changing: Boolean,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var currentPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBlue,
        title = { Text(if (changing) "Cambia il PIN" else "Crea il tuo PIN", color = MainText) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Usa almeno 6 cifre. Il PIN non verrà mai salvato in chiaro.", color = SoftText, fontSize = 13.sp)
                if (changing) {
                    PinField("PIN attuale", currentPin) { currentPin = it }
                }
                PinField("Nuovo PIN", newPin) { newPin = it }
                PinField("Conferma nuovo PIN", confirmPin) { confirmPin = it }
                if (error.isNotBlank()) Text(error, color = Danger, fontSize = 13.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                error = when {
                    changing && !security.verifyPin(currentPin) -> "Il PIN attuale non è corretto."
                    !newPin.matches(Regex("\\d{6,}")) -> "Il PIN deve contenere almeno 6 cifre."
                    newPin != confirmPin -> "I due PIN non coincidono."
                    else -> ""
                }
                if (error.isBlank()) onSave(newPin)
            }) { Text("Salva", color = Gold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla", color = SoftText) } }
    )
}

@Composable
private fun PinField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { entered -> onChange(entered.filter(Char::isDigit).take(12)) },
        label = { Text(label) },
        placeholder = { Text(if (label == "Nuovo PIN") "Almeno 6 cifre" else "••••••") },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        colors = editorFieldColors(),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun AppLockScreen(
    security: AppSecurity,
    biometricAvailable: Boolean,
    onUnlocked: () -> Unit,
    onRequestBiometric: ((Boolean, String?) -> Unit) -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize().organizerBackground(), contentAlignment = Alignment.Center) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            colors = CardDefaults.cardColors(containerColor = CardBlue),
            shape = RoundedCornerShape(28.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("✦", color = Gold, fontSize = 34.sp)
                Text("Task Organizer", color = MainText, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Inserisci il PIN per accedere alle tue attività.", color = SoftText, textAlign = TextAlign.Center)
                PinField("PIN", pin) { pin = it }
                if (error.isNotBlank()) Text(error, color = Danger, fontSize = 13.sp, textAlign = TextAlign.Center)
                Button(
                    onClick = {
                        when (val result = security.verifyPinWithRateLimit(pin)) {
                            PinVerification.Verified -> {
                                pin = ""
                                error = ""
                                onUnlocked()
                            }
                            is PinVerification.Incorrect -> {
                                error = if (result.delayMillis > 0) {
                                    "Troppi tentativi. Attendi ${formatLockDelay(result.delayMillis)} prima di riprovare."
                                } else {
                                    "PIN non corretto. Riprova."
                                }
                            }
                            is PinVerification.Locked -> {
                                error = "Troppi tentativi. Attendi ${formatLockDelay((result.untilMillis - System.currentTimeMillis()).coerceAtLeast(0L))} prima di riprovare."
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = NightBlue)
                ) { Text("Sblocca", fontWeight = FontWeight.Bold) }
                if (biometricAvailable && security.isBiometricEnabled()) {
                    OutlinedButton(onClick = {
                        onRequestBiometric { success, message ->
                            if (success) onUnlocked() else error = message.orEmpty()
                        }
                    }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                        Text("Usa impronta", color = Gold, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

private fun formatLockDelay(milliseconds: Long): String {
    val seconds = ((milliseconds + 999) / 1000).coerceAtLeast(1)
    return if (seconds < 60) "$seconds secondi" else "${(seconds + 59) / 60} minuti"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskEditorScreen(
    task: OrganizerTask?,
    customCategories: List<String>,
    customCategoryIcons: Map<String, String>,
    onAddCustomCategory: (String, String) -> Unit,
    onDeleteCustomCategory: (String) -> Unit,
    onCancel: () -> Unit,
    onSave: (OrganizerTask) -> Unit,
    notificationPermissionAllowed: Boolean,
    onRequestNotificationPermission: () -> Unit
) {
    val context = LocalContext.current
    var title by rememberSaveable(task?.id) { mutableStateOf(task?.title.orEmpty()) }
    var date by rememberSaveable(task?.id) { mutableStateOf(task?.date?.let(::formatItalianDate) ?: LocalDate.now().format(ItalianDateFormatter)) }
    var time by rememberSaveable(task?.id) { mutableStateOf(task?.time.orEmpty()) }
    var endTime by rememberSaveable(task?.id) { mutableStateOf(task?.endTime.orEmpty()) }
    var category by rememberSaveable(task?.id) { mutableStateOf(task?.category ?: "Personale") }
    var categoryIcon by rememberSaveable(task?.id) { mutableStateOf(task?.categoryIcon ?: customCategoryIcons[task?.category]) }
    var priority by rememberSaveable(task?.id) { mutableStateOf(task?.priority ?: "Media") }
    var note by rememberSaveable(task?.id) { mutableStateOf(task?.note.orEmpty()) }
    var examStatus by rememberSaveable(task?.id) { mutableStateOf(task?.examStatus ?: "Da prenotare") }
    var reminderEnabled by rememberSaveable(task?.id) { mutableStateOf(task?.reminderEnabled ?: true) }
    var reminderMinutesBefore by rememberSaveable(task?.id) { mutableStateOf(task?.reminderMinutesBefore ?: 15) }
    var reminderRepeatMinutes by rememberSaveable(task?.id) { mutableStateOf(task?.reminderRepeatMinutes ?: 0) }
    var alarmEnabled by rememberSaveable(task?.id) { mutableStateOf(task?.alarmEnabled ?: false) }
    var cardColor by rememberSaveable(task?.id) { mutableStateOf(task?.cardColor ?: DefaultCardColor) }
    var recurrence by rememberSaveable(task?.id) { mutableStateOf(task?.recurrence ?: "none") }
    var error by rememberSaveable(task?.id) { mutableStateOf("") }
    var discardDialogOpen by rememberSaveable(task?.id) { mutableStateOf(false) }
    var datePickerOpen by rememberSaveable(task?.id) { mutableStateOf(false) }
    val initialDate = remember(task?.id) { task?.date?.let(::formatItalianDate) ?: LocalDate.now().format(ItalianDateFormatter) }
    val hasUnsavedChanges = title != task?.title.orEmpty() ||
        date != initialDate ||
        time != task?.time.orEmpty() ||
        endTime != task?.endTime.orEmpty() ||
        category != (task?.category ?: "Personale") ||
        categoryIcon != (task?.categoryIcon ?: customCategoryIcons[task?.category]) ||
        priority != (task?.priority ?: "Media") ||
        note != task?.note.orEmpty() ||
        examStatus != (task?.examStatus ?: "Da prenotare") ||
        reminderEnabled != (task?.reminderEnabled ?: true) ||
        reminderMinutesBefore != (task?.reminderMinutesBefore ?: 15) ||
        reminderRepeatMinutes != (task?.reminderRepeatMinutes ?: 0) ||
        alarmEnabled != (task?.alarmEnabled ?: false) ||
        cardColor != (task?.cardColor ?: DefaultCardColor) ||
        recurrence != (task?.recurrence ?: "none")

    fun requestClose() {
        if (hasUnsavedChanges) discardDialogOpen = true else onCancel()
    }
    BackHandler(onBack = ::requestClose)

    Box(modifier = Modifier.fillMaxSize().organizerBackground()) {
    PageColumn(safeBottom = true) {
        Text(if (task == null) "Nuova attività" else "Modifica attività", color = MainText, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Inserisci qui la tua attività", color = SoftText)
        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Titolo") }, placeholder = { Text("Es. Ripasso capitolo 3") }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = editorFieldColors())
        OutlinedTextField(
            value = date,
            onValueChange = { date = it },
            label = { Text("Data") },
            placeholder = { Text("GG-MM-AAAA oppure GG/MM/AAAA") },
            trailingIcon = {
                IconButton(onClick = { datePickerOpen = true }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_nav_calendar),
                        contentDescription = "Apri calendario",
                        tint = Gold
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = editorFieldColors()
        )
        if (datePickerOpen) {
            val initialDateMillis = remember(date) { parseItalianDate(date)?.toPickerMillis() }
            val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialDateMillis)
            DatePickerDialog(
                onDismissRequest = { datePickerOpen = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { selectedMillis ->
                            date = selectedMillis.toPickerDate().format(ItalianDateFormatter)
                        }
                        datePickerOpen = false
                    }) { Text("Conferma", color = Gold) }
                },
                dismissButton = { TextButton(onClick = { datePickerOpen = false }) { Text("Annulla", color = SoftText) } },
                colors = androidx.compose.material3.DatePickerDefaults.colors(containerColor = CardBlue)
            ) {
                DatePicker(state = datePickerState)
            }
        }
        TimeField(label = "Orario di inizio", value = time, onValueChange = { time = it })
        TimeField(label = "Orario di fine", value = endTime, optional = true, onValueChange = { endTime = it })
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("Promemoria", color = MainText, fontWeight = FontWeight.SemiBold)
                Text("Scegli quando ricevere l'avviso", color = SoftText, fontSize = 13.sp)
            }
            Switch(checked = reminderEnabled && time.isNotBlank(), onCheckedChange = { reminderEnabled = it }, enabled = time.isNotBlank())
        }
        if (reminderEnabled && time.isNotBlank()) {
            if (!notificationPermissionAllowed) {
                Text("Le notifiche sono disattivate: puoi comunque salvare l'attività, ma Android non potrà avvisarti.", color = Danger, fontSize = 13.sp)
                OutlinedButton(
                    onClick = onRequestNotificationPermission,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("Attiva notifiche", color = Gold, fontWeight = FontWeight.SemiBold) }
            }
            val advanceOptions = linkedMapOf(0 to "All'orario", 5 to "5 minuti prima", 10 to "10 minuti prima", 15 to "15 minuti prima", 30 to "30 minuti prima", 60 to "1 ora prima", 120 to "2 ore prima", 1440 to "1 giorno prima")
            val repeatOptions = linkedMapOf(0 to "Non ripetere", 5 to "Ogni 5 minuti", 10 to "Ogni 10 minuti", 15 to "Ogni 15 minuti", 30 to "Ogni 30 minuti", 60 to "Ogni ora")
            OptionField("Avvisami", advanceOptions[reminderMinutesBefore] ?: "15 minuti prima", advanceOptions.values.toList()) { choice ->
                reminderMinutesBefore = advanceOptions.entries.firstOrNull { it.value == choice }?.key ?: 15
            }
            OptionField("Ripeti avviso", repeatOptions[reminderRepeatMinutes] ?: "Non ripetere", repeatOptions.values.toList()) { choice ->
                reminderRepeatMinutes = repeatOptions.entries.firstOrNull { it.value == choice }?.key ?: 0
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Modalità sveglia", color = MainText, fontWeight = FontWeight.SemiBold)
                Text("Suono e vibrazione per gli impegni importanti", color = SoftText, fontSize = 13.sp)
            }
            Switch(
                checked = alarmEnabled && reminderEnabled && time.isNotBlank(),
                onCheckedChange = { alarmEnabled = it },
                enabled = reminderEnabled && time.isNotBlank()
            )
        }
        if (alarmEnabled && reminderEnabled && time.isNotBlank() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !TaskReminderScheduler.canUseExactAlarms(context)) {
            Text("Per far suonare la sveglia all'orario esatto, abilita “Sveglie e promemoria” nelle impostazioni Android.", color = SoftText, fontSize = 13.sp)
            OutlinedButton(
                onClick = {
                    context.startActivity(
                        android.content.Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                            .setData(android.net.Uri.parse("package:${context.packageName}"))
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) { Text("Consenti sveglie precise", color = Gold, fontWeight = FontWeight.SemiBold) }
        }
        CategoryField(
            value = category,
            icon = categoryIcon,
            customCategories = customCategories,
            customCategoryIcons = customCategoryIcons,
            onSelected = {
                category = it
                categoryIcon = customCategoryIcons[it]
            },
            onAddCustomCategory = { newCategory, icon ->
                onAddCustomCategory(newCategory, icon)
                category = newCategory
                categoryIcon = icon
            },
            onDeleteCustomCategory = onDeleteCustomCategory
        )
        OptionField("Priorità", priority, listOf("Bassa", "Media", "Alta")) { priority = it }
        val recurrenceOptions = linkedMapOf("none" to "Non ripetere", "daily" to "Ogni giorno", "weekly" to "Ogni settimana", "monthly" to "Ogni mese")
        OptionField("Ripetizione", recurrenceOptions[recurrence] ?: "Non ripetere", recurrenceOptions.values.toList()) { choice ->
            recurrence = recurrenceOptions.entries.firstOrNull { it.value == choice }?.key ?: "none"
        }
        TaskColorPicker(selectedColor = cardColor, onSelected = { cardColor = it })
        if (category == "Esami") {
            OptionField("Stato dell'esame", examStatus, listOf("Da prenotare", "Prenotato", "Fatto")) { examStatus = it }
        }
        OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Note") }, placeholder = { Text("Es. svolta metà attività") }, modifier = Modifier.fillMaxWidth(), minLines = 3, colors = editorFieldColors())
        if (error.isNotBlank()) Text(error, color = Danger, fontSize = 13.sp)
        Button(
            onClick = {
                val parsedDate = parseItalianDate(date)
                val normalizedTime = normalizeTime(time)
                val normalizedEndTime = normalizeTime(endTime)
                val validTime = time.isBlank() || normalizedTime != null
                val validEndTime = endTime.isBlank() || normalizedEndTime != null
                error = when {
                    title.isBlank() -> "Inserisci un titolo."
                    parsedDate == null -> "La data deve essere nel formato GG-MM-AAAA."
                    !validTime -> "L'orario deve essere nel formato 00:00."
                    !validEndTime -> "L'orario di fine deve essere nel formato 00:00."
                    normalizedTime != null && normalizedEndTime != null && normalizedEndTime <= normalizedTime -> "L'orario di fine deve essere successivo all'inizio."
                    else -> ""
                }
                if (error.isBlank()) {
                    val recurrenceScheduleChanged = task != null &&
                        (task.date != parsedDate.toString() || task.recurrence != recurrence)
                    onSave(
                        OrganizerTask(
                            id = task?.id ?: java.util.UUID.randomUUID().toString(), title = title.trim(), date = parsedDate.toString(),
                            time = normalizedTime, endTime = normalizedEndTime, category = category, categoryIcon = categoryIcon, priority = priority, note = note.trim(),
                            completed = task?.completed ?: false, reminderEnabled = reminderEnabled && normalizedTime != null, reminderMinutesBefore = reminderMinutesBefore, reminderRepeatMinutes = reminderRepeatMinutes, alarmEnabled = alarmEnabled && reminderEnabled && normalizedTime != null, cardColor = cardColor, examStatus = if (category == "Esami") examStatus else null,
                            createdAt = task?.createdAt ?: System.currentTimeMillis(), deletedAt = task?.deletedAt ?: 0L, recurrence = recurrence,
                            seriesId = if (recurrence == "none") null else task?.seriesId,
                            recurrenceAnchorDate = if (recurrence == "none" || recurrenceScheduleChanged) null else task?.recurrenceAnchorDate
                        )
                    )
                }
            },
            modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = NightBlue)
        ) { Text("Salva attività", fontWeight = FontWeight.Bold) }
        TextButton(onClick = ::requestClose, modifier = Modifier.fillMaxWidth()) { Text("Annulla", color = SoftText) }
    }
    }
    if (discardDialogOpen) {
        AlertDialog(
            onDismissRequest = { discardDialogOpen = false },
            containerColor = CardBlue,
            title = { Text("Scartare le modifiche?", color = MainText) },
            text = { Text("Le modifiche non salvate a questa attività andranno perse.", color = SoftText) },
            confirmButton = {
                TextButton(onClick = {
                    discardDialogOpen = false
                    onCancel()
                }) { Text("Scarta", color = Danger) }
            },
            dismissButton = { TextButton(onClick = { discardDialogOpen = false }) { Text("Continua a modificare", color = Gold) } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeField(label: String, value: String, optional: Boolean = false, onValueChange: (String) -> Unit) {
    var pickerOpen by remember { mutableStateOf(false) }
    val normalized = normalizeTime(value)
    val initialHour = normalized?.substringBefore(":")?.toIntOrNull() ?: 0
    val initialMinute = normalized?.substringAfter(":")?.toIntOrNull() ?: 0
    val pickerState = rememberTimePickerState(initialHour = initialHour, initialMinute = initialMinute, is24Hour = true)

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(if (optional) "00:00 — facoltativo" else "00:00 — formato 24 ore") },
        trailingIcon = { TextButton(onClick = { pickerOpen = true }) { Text("Scegli", color = Gold) } },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        colors = editorFieldColors()
    )
    if (pickerOpen) {
        AlertDialog(
            onDismissRequest = { pickerOpen = false },
            containerColor = CardBlue,
            title = { Text("Scegli l'orario", color = MainText) },
            text = { TimePicker(state = pickerState) },
            confirmButton = {
                TextButton(onClick = {
                    onValueChange(String.format(Locale.ITALY, "%02d:%02d", pickerState.hour, pickerState.minute))
                    pickerOpen = false
                }) { Text("Conferma", color = Gold) }
            },
            dismissButton = { TextButton(onClick = { pickerOpen = false }) { Text("Annulla", color = SoftText) } }
        )
    }
}

@Composable
private fun TaskColorPicker(selectedColor: String, onSelected: (String) -> Unit) {
    Text("Colore della card", color = SoftText, fontSize = 13.sp)
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        TaskCardColorOptions.forEach { (color, label) ->
            Card(
                modifier = Modifier.size(44.dp).clickable { onSelected(color) },
                colors = CardDefaults.cardColors(containerColor = taskCardBackground(color)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(if (selectedColor == color) "✓" else "", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
    Text(TaskCardColorOptions.firstOrNull { it.first == selectedColor }?.second ?: "Predefinito", color = Gold, fontSize = 12.sp)
}

@Composable
private fun CustomCategoryIconPicker(selectedIcon: String, onSelected: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CustomCategoryIconOptions.chunked(6).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                row.forEach { icon ->
                    Card(
                        modifier = Modifier.size(38.dp).clickable { onSelected(icon) },
                        colors = CardDefaults.cardColors(containerColor = if (icon == selectedIcon) Gold else CardBlueLight),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(icon, fontSize = 18.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryField(
    value: String,
    icon: String?,
    customCategories: List<String>,
    customCategoryIcons: Map<String, String>,
    onSelected: (String) -> Unit,
    onAddCustomCategory: (String, String) -> Unit,
    onDeleteCustomCategory: (String) -> Unit
) {
    var pickerOpen by remember { mutableStateOf(false) }
    var customDialogOpen by remember { mutableStateOf(false) }
    var customName by remember { mutableStateOf("") }
    var customIcon by remember { mutableStateOf(CustomCategoryIconOptions.first()) }
    var categoryToDelete by remember { mutableStateOf<String?>(null) }
    val standardCategories = listOf("Studio", "Personale", "Lavoro", "Esami")
    val categories = standardCategories + customCategories.filter { it !in standardCategories }

    Text("Categoria", color = SoftText, fontSize = 13.sp)
    OutlinedButton(
        onClick = { pickerOpen = true },
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, SoftText),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = CardBlue, contentColor = MainText)
    ) {
        Text(categoryDisplay(value, icon), color = MainText, modifier = Modifier.fillMaxWidth())
        Text("⌄", color = Gold, fontSize = 20.sp)
    }
    if (pickerOpen) {
        AlertDialog(
            onDismissRequest = { pickerOpen = false },
            containerColor = CardBlue,
            title = { Text("Categoria", color = MainText) },
            text = {
                Column {
                    categories.forEach { category ->
                        val isCustomCategory = category in customCategories
                        Text(
                            categoryDisplay(category, customCategoryIcons[category]),
                            color = if (category == value) Gold else MainText,
                            modifier = Modifier.fillMaxWidth().combinedClickable(
                                onClick = { onSelected(category); pickerOpen = false },
                                onLongClick = if (isCustomCategory) {
                                    { categoryToDelete = category; pickerOpen = false }
                                } else null
                            ).padding(vertical = 12.dp)
                        )
                    }
                    HorizontalDivider(color = CardBlueLight)
                    Text("Altro", color = Gold, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth().clickable { pickerOpen = false; customDialogOpen = true }.padding(vertical = 14.dp))
                }
            },
            confirmButton = { TextButton(onClick = { pickerOpen = false }) { Text("Chiudi", color = Gold) } }
        )
    }
    if (customDialogOpen) {
        AlertDialog(
            onDismissRequest = { customDialogOpen = false },
            containerColor = CardBlue,
            title = { Text("Nuova categoria", color = MainText) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = customName,
                        onValueChange = { customName = it },
                        label = { Text("Nome della categoria") },
                        placeholder = { Text("Es. Salute") },
                        singleLine = true,
                        colors = editorFieldColors()
                    )
                    Text("Scegli un'icona", color = SoftText, fontSize = 13.sp)
                    CustomCategoryIconPicker(selectedIcon = customIcon, onSelected = { customIcon = it })
                    OutlinedTextField(
                        value = customIcon,
                        onValueChange = { customIcon = it },
                        label = { Text("Oppure usa una tua emoji") },
                        placeholder = { Text("Es. 🎧") },
                        singleLine = true,
                        colors = editorFieldColors()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val category = customName.trim()
                    if (category.isNotBlank()) {
                        onAddCustomCategory(category, customIcon.ifBlank { CustomCategoryIconOptions.first() })
                        customName = ""
                        customIcon = CustomCategoryIconOptions.first()
                        customDialogOpen = false
                    }
                }) { Text("Aggiungi", color = Gold) }
            },
            dismissButton = { TextButton(onClick = { customDialogOpen = false }) { Text("Annulla", color = SoftText) } }
        )
    }
    categoryToDelete?.let { category ->
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            containerColor = CardBlue,
            title = { Text("Eliminare $category?", color = MainText) },
            text = { Text("La categoria sparirà dall'elenco, ma le attività già associate non verranno cancellate.", color = SoftText) },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteCustomCategory(category)
                    categoryToDelete = null
                }) { Text("Elimina", color = Danger) }
            },
            dismissButton = { TextButton(onClick = { categoryToDelete = null }) { Text("Annulla", color = SoftText) } }
        )
    }
}

@Composable
private fun OptionField(label: String, value: String, options: List<String>, onSelected: (String) -> Unit) {
    var pickerOpen by remember { mutableStateOf(false) }
    Text(label, color = SoftText, fontSize = 13.sp)
    OutlinedButton(
        onClick = { pickerOpen = true },
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, SoftText),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = CardBlue, contentColor = MainText)
    ) {
        Text(value, color = MainText, modifier = Modifier.fillMaxWidth())
        Text("⌄", color = Gold, fontSize = 20.sp)
    }
    if (pickerOpen) {
        AlertDialog(
            onDismissRequest = { pickerOpen = false },
            containerColor = CardBlue,
            title = { Text(label, color = MainText) },
            text = {
                Column {
                    options.forEach { option ->
                        Text(option, color = if (option == value) Gold else MainText, modifier = Modifier.fillMaxWidth().clickable { onSelected(option); pickerOpen = false }.padding(vertical = 12.dp))
                    }
                }
            },
            confirmButton = { TextButton(onClick = { pickerOpen = false }) { Text("Chiudi", color = Gold) } }
        )
    }
}

@Composable
private fun editorFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = MainText,
    unfocusedTextColor = MainText,
    focusedContainerColor = CardBlue,
    unfocusedContainerColor = CardBlue,
    focusedBorderColor = Gold,
    unfocusedBorderColor = SoftText,
    focusedLabelColor = Gold,
    unfocusedLabelColor = SoftText,
    cursorColor = Gold,
    focusedPlaceholderColor = SoftText,
    unfocusedPlaceholderColor = SoftText
)

@Preview(showBackground = true, backgroundColor = 0xFF0D1B2F)
@Composable
private fun TaskOrganizerPreview() {
    val context = LocalContext.current
    TaskOrganizerTheme(OrganizerTheme.ORIGINAL_DARK) {
        TaskOrganizerApp(
            selectedTheme = OrganizerTheme.ORIGINAL_DARK,
            followSystemTheme = false,
            onThemeChanged = {},
            onFollowSystemThemeChanged = {},
            security = AppSecurity(context),
            biometricAvailable = false,
            openNewFromWidget = false,
            onWidgetQuickAddHandled = {},
            signedInAccount = null,
            accountWorking = false,
            accountMessage = null,
            onGoogleSignIn = {},
            onSignOut = { it() },
            onDeleteAccount = { it(false) },
            notificationPermissionAllowed = true,
            onRequestNotificationPermission = {},
            pendingBackupImportUri = null,
            onOpenBackupImport = {},
            onBackupImportHandled = {},
            pendingBackupExportUri = null,
            onCreateBackupExport = { _, password -> password.fill('\u0000') },
            takeBackupExportPassword = { null },
            onBackupExportHandled = {},
            onSecurityChanged = {}
        )
    }
}

