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

internal val NightBlue get() = OrganizerPalette.background
internal val CardBlue get() = OrganizerPalette.card
internal val CardBlueLight get() = OrganizerPalette.cardLight
internal val Gold get() = OrganizerPalette.accent
internal val SoftText get() = OrganizerPalette.muted
internal val MainText get() = OrganizerPalette.text
internal val AccentSecondary get() = OrganizerPalette.accentSecondary
internal val Success get() = OrganizerPalette.success
internal val Danger get() = OrganizerPalette.danger
internal const val DefaultCardColor = "default"
internal val TaskCardColorOptions = listOf(
    DefaultCardColor to "Predefinito",
    "oceano" to "Oceano",
    "salvia" to "Salvia",
    "prugna" to "Prugna",
    "corallo" to "Corallo",
    "miele" to "Miele"
)


open class TaskOrganizerActivity : FragmentActivity() {
    private var widgetQuickAdd by mutableStateOf(false)
    private var notificationPermissionAllowed by mutableStateOf(true)
    private var pendingBackupImportUri by mutableStateOf<Uri?>(null)
    private var pendingBackupExportUri by mutableStateOf<Uri?>(null)
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
                uiSession.clearBackupExportPassword()
            }
            pendingBackupExportUri = uri
        }
        widgetQuickAdd = intent.getBooleanExtra(EXTRA_OPEN_NEW_TASK, false)
        val initialSettings = getSharedPreferences("task_organizer_settings", MODE_PRIVATE)
        val initialTheme = OrganizerTheme.fromStorage(
            initialSettings.getString("theme_mode", null),
            initialSettings.getBoolean("dark_theme", false)
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
            val accountManager = remember { FirebaseAccountManager(this@TaskOrganizerActivity) }
            var signedInAccount by remember { mutableStateOf(accountManager.currentAccount()) }
            var accountWorking by remember { mutableStateOf(false) }
            var accountMessage by remember { mutableStateOf<String?>(null) }
            var selectedTheme by remember {
                mutableStateOf(
                    OrganizerTheme.fromStorage(
                        preferences.getString("theme_mode", null),
                        preferences.getBoolean("dark_theme", false)
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
                delay(1_250)
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
                } else {
                    androidx.compose.foundation.layout.Box {
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
                            Thread { TaskWidgetUpdater.updateAll(this@TaskOrganizerActivity) }.start()
                        },
                        onFollowSystemThemeChanged = { enabled ->
                            followSystemTheme = enabled
                            preferences.edit().putBoolean("follow_system_theme", enabled).apply()
                            Thread { TaskWidgetUpdater.updateAll(this@TaskOrganizerActivity) }.start()
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
                            uiSession.setBackupExportPassword(password)
                            backupExportLauncher.launch(fileName)
                        },
                        takeBackupExportPassword = uiSession::takeBackupExportPassword,
                        onBackupExportHandled = { pendingBackupExportUri = null },
                        onSecurityChanged = {
                            updateSecureWindow()
                            if (!security.hasPin()) session.unlock()
                        }
                        )
                        if (!session.isUnlocked) {
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
        private const val EXTRA_OPEN_NEW_TASK = "open_new_task"
        private const val NOTIFICATION_PERMISSION_REQUEST = 1
    }
}


@Composable
internal fun OptionField(label: String, value: String, options: List<String>, onSelected: (String) -> Unit) {
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
internal fun editorFieldColors() = OutlinedTextFieldDefaults.colors(
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

@Composable
internal fun compactEditorFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = MainText,
    unfocusedTextColor = MainText,
    focusedContainerColor = CardBlueLight.copy(alpha = 0.62f),
    unfocusedContainerColor = CardBlueLight.copy(alpha = 0.48f),
    focusedBorderColor = Gold.copy(alpha = 0.75f),
    unfocusedBorderColor = SoftText.copy(alpha = 0.22f),
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

