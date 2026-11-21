package com.example.taskorganizer

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.example.taskorganizer.ui.theme.OrganizerTheme

@Composable
fun SettingsScreen(
    selectedTheme: OrganizerTheme,
    followSystemTheme: Boolean,
    onThemeChanged: (OrganizerTheme) -> Unit,
    onFollowSystemThemeChanged: (Boolean) -> Unit,
    security: AppSecurity,
    biometricAvailable: Boolean,
    onSecurityChanged: () -> Unit,
    onOpenBackup: () -> Unit,
    nickname: String,
    onNicknameChanged: (String) -> Unit,
    onOpenPrivacy: () -> Unit,
    signedInAccount: SignedInAccount?,
    accountWorking: Boolean,
    accountMessage: String?,
    onGoogleSignIn: () -> Unit,
    onSignOut: (deleteLocalData: Boolean) -> Unit,
    onDeleteAccount: (deleteLocalData: Boolean) -> Unit
) {
    var section by rememberSaveable { mutableStateOf("root") }
    var sectionHistory by rememberSaveable { mutableStateOf(emptyList<String>()) }
    fun openSection(destination: String) {
        if (destination != section) {
            sectionHistory = sectionHistory + section
            section = destination
        }
    }
    fun goBack() {
        section = sectionHistory.lastOrNull() ?: "root"
        sectionHistory = sectionHistory.dropLast(1)
    }
    BackHandler(enabled = section != "root") { goBack() }
    when (section) {
        "root" -> SettingsHubScreen(signedInAccount = signedInAccount, onOpen = ::openSection)
        "account" -> AccountHubScreen(
            signedInAccount = signedInAccount,
            onBack = ::goBack,
            onOpenProfile = { openSection("accountProfile") },
            onOpenBackup = onOpenBackup,
            onOpenPrivacy = onOpenPrivacy,
            onOpenCalendar = { openSection("accountCalendar") }
        )
        "accountProfile" -> AccountProfileScreen(
            nickname = nickname,
            onNicknameChanged = onNicknameChanged,
            signedInAccount = signedInAccount,
            accountWorking = accountWorking,
            accountMessage = accountMessage,
            onGoogleSignIn = onGoogleSignIn,
            onSignOut = onSignOut,
            onDeleteAccount = onDeleteAccount,
            onBack = ::goBack
        )
        "accountCalendar" -> AccountCalendarSettingsScreen(onBack = ::goBack)
        "appearance" -> AppearanceSettingsScreen(
            selectedTheme = selectedTheme,
            followSystemTheme = followSystemTheme,
            onThemeChanged = onThemeChanged,
            onFollowSystemThemeChanged = onFollowSystemThemeChanged,
            onBack = ::goBack
        )
        "home" -> HomeSettingsScreen(onBack = ::goBack)
        "activities" -> ActivitySettingsScreen(onBack = ::goBack)
        "security" -> SecuritySettingsScreen(
            security = security,
            biometricAvailable = biometricAvailable,
            onSecurityChanged = onSecurityChanged,
            onBack = ::goBack
        )
        "information" -> InformationSettingsScreen(onBack = ::goBack)
    }
}
