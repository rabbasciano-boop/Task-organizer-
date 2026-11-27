package com.example.taskorganizer

import android.content.Context
import android.app.NotificationManager
import com.google.firebase.auth.FirebaseAuth
import java.security.MessageDigest

/** Profile names never contain email addresses or provider identifiers in clear text. */
internal object LocalProfiles {
    const val GUEST = "guest"
    @Volatile private var switching = false

    fun beginSwitch() { switching = true }
    fun endSwitch() { switching = false }
    fun isSwitching(): Boolean = switching

    fun mayAccess(expected: String?, active: String, switching: Boolean): Boolean =
        !switching && !expected.isNullOrBlank() && expected == active

    fun accountId(providerSubject: String): String {
        require(providerSubject.isNotBlank())
        val digest = MessageDigest.getInstance("SHA-256").digest(providerSubject.toByteArray(Charsets.UTF_8))
        return "account_" + digest.joinToString("") { "%02x".format(it.toInt() and 0xff) }
    }

    fun currentId(): String {
        val user = FirebaseAuth.getInstance().currentUser ?: return GUEST
        val googleId = user.providerData.firstOrNull { it.providerId == "google.com" }?.uid
        return accountId(requireNotNull(googleId?.takeIf { it.isNotBlank() }) { "Identità Google non disponibile" })
    }

    fun currentStore(context: Context): EncryptedTaskStore = EncryptedTaskStore(context, profileId = currentId())

    fun clearUnscopedNotificationsOnce(context: Context) {
        val prefs = context.getSharedPreferences("task_organizer_profile_safety", Context.MODE_PRIVATE)
        if (prefs.getBoolean("unscoped_notifications_cleared", false)) return
        context.getSystemService(NotificationManager::class.java).cancelAll()
        prefs.edit().putBoolean("unscoped_notifications_cleared", true).commit()
    }
}

/** The old archive is never deleted or overwritten by the association. */
internal object LegacyProfileAssociation {
    private const val PREFS = "task_organizer_profile_association"
    private const val OWNER = "legacy_owner"
    private const val COMPLETE = "legacy_copy_verified"
    private val lock = Any()

    fun needsConfirmation(context: Context, profileId: String): Boolean = synchronized(lock) {
        if (profileId == LocalProfiles.GUEST || !EncryptedTaskStore(context).hasArchive()) return@synchronized false
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val owner = prefs.getString(OWNER, null)
        !prefs.getBoolean(COMPLETE, false) && (owner == null || owner == profileId)
    }

    fun associate(
        context: Context,
        profileId: String,
        legacy: EncryptedTaskStore = EncryptedTaskStore(context),
        target: EncryptedTaskStore = EncryptedTaskStore(context, profileId = profileId)
    ) = synchronized(lock) {
        require(profileId != LocalProfiles.GUEST)
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val owner = prefs.getString(OWNER, null)
        check(owner == null || owner == profileId) { "L'archivio precedente è già associato a un altro account" }
        check(legacy.hasArchive()) { "Archivio precedente non trovato" }
        if (target.hasArchive()) {
            check(target.load() == legacy.load()) { "Questo profilo contiene già dati; l'archivio precedente non è stato modificato" }
        }
        // Reserve the owner first; a crash during copying can only be retried by this same account.
        check(prefs.edit().putString(OWNER, profileId).commit()) { "Impossibile registrare l'associazione" }
        legacy.copyIntoEmptyProfile(target)
        val oldProfile = context.getSharedPreferences("task_organizer_profile", Context.MODE_PRIVATE)
        val newProfile = context.getSharedPreferences("task_organizer_profile_$profileId", Context.MODE_PRIVATE)
        if (!newProfile.contains("nickname") && oldProfile.contains("nickname")) {
            check(newProfile.edit().putString("nickname", oldProfile.getString("nickname", "Utente")).commit())
        }
        check(prefs.edit().putBoolean(COMPLETE, true).commit()) { "Impossibile completare l'associazione" }
    }

    /** Only called after explicitly confirming deletion of the associated profile's local data. */
    fun deleteAssociatedLegacy(context: Context, profileId: String, legacy: EncryptedTaskStore = EncryptedTaskStore(context)) = synchronized(lock) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getString(OWNER, null) == profileId) {
            legacy.clearAll()
            check(context.getSharedPreferences("task_organizer_profile", Context.MODE_PRIVATE).edit().clear().commit()) {
                "Impossibile cancellare il profilo precedente"
            }
            check(prefs.edit().clear().commit()) { "Impossibile completare la cancellazione" }
        }
    }
}
