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
    BackHandler(enabled = section != "root") { section = "root" }
    when (section) {
        "root" -> SettingsHubScreen(signedInAccount = signedInAccount, onOpen = { section = it })
        "account" -> AccountHubScreen(
            signedInAccount = signedInAccount,
            onBack = { section = "root" },
            onOpenProfile = { section = "accountProfile" },
            onOpenBackup = onOpenBackup,
            onOpenPrivacy = onOpenPrivacy,
            onOpenCalendar = { section = "accountCalendar" }
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
            onBack = { section = "account" }
        )
        "accountCalendar" -> AccountCalendarSettingsScreen(onBack = { section = "account" })
        "appearance" -> AppearanceSettingsScreen(
            selectedTheme = selectedTheme,
            followSystemTheme = followSystemTheme,
            onThemeChanged = onThemeChanged,
            onFollowSystemThemeChanged = onFollowSystemThemeChanged,
            onBack = { section = "root" }
        )
        "home" -> HomeSettingsScreen(onBack = { section = "root" })
        "activities" -> ActivitySettingsScreen(onBack = { section = "root" })
        "security" -> SecuritySettingsScreen(
            security = security,
            biometricAvailable = biometricAvailable,
            onSecurityChanged = onSecurityChanged,
            onBack = { section = "root" }
        )
        "information" -> InformationSettingsScreen(onBack = { section = "root" })
    }
}
