package com.example.taskorganizer

import androidx.activity.ComponentActivity
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Handles the optional identity session only. Organizer data remains in EncryptedTaskStore. */
data class SignedInAccount(
    val displayName: String,
    val email: String,
    val photoUrl: String? = null
)

class FirebaseAccountManager(private val activity: ComponentActivity) {
    private val auth = FirebaseAuth.getInstance()
    private val credentialManager = CredentialManager.create(activity)
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val resultPreferences = activity.applicationContext.getSharedPreferences(ACCOUNT_RESULT_PREFERENCES, ComponentActivity.MODE_PRIVATE)

    fun currentAccount(): SignedInAccount? = auth.currentUser?.let { user ->
        SignedInAccount(
            displayName = user.displayName?.ifBlank { null } ?: "Utente Google",
            email = user.email?.ifBlank { null } ?: "Account Google",
            photoUrl = user.photoUrl?.toString()
        )
    }

    fun takePendingResultMessage(): String? = resultPreferences.getString(PENDING_RESULT_MESSAGE, null).also {
        resultPreferences.edit().remove(PENDING_RESULT_MESSAGE).apply()
    }

    fun signInWithGoogle(onResult: (SignedInAccount?, String?) -> Unit) {
        requestGoogleToken { idToken, error ->
            if (idToken == null) {
                onResult(null, error)
                return@requestGoogleToken
            }
            auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null))
                .addOnCompleteListener(activity) { task ->
                    if (task.isSuccessful) onResult(currentAccount(), null)
                    else onResult(null, "Non è stato possibile completare l'accesso. Riprova tra poco.")
                }
        }
    }

    fun signOut(onComplete: () -> Unit) {
        auth.signOut()
        appScope.launch {
            runCatching { credentialManager.clearCredentialState(ClearCredentialStateRequest()) }
            onComplete()
        }
    }

    /** Firebase requires a recent provider confirmation before permanently deleting an account. */
    fun deleteAccountWithGoogleConfirmation(onResult: (Boolean, String?) -> Unit) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            onResult(false, "Non risulta alcun account collegato.")
            return
        }
        requestGoogleToken { idToken, error ->
            if (idToken == null) {
                onResult(false, error)
                return@requestGoogleToken
            }
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            currentUser.reauthenticate(credential).addOnCompleteListener { reauth ->
                if (!reauth.isSuccessful) {
                    finishDeletion(onResult, false, "La conferma dell'account non è riuscita. Riprova e scegli il tuo account Google.")
                    return@addOnCompleteListener
                }
                currentUser.delete().addOnCompleteListener { deletion ->
                    if (deletion.isSuccessful) {
                        appScope.launch {
                            runCatching { credentialManager.clearCredentialState(ClearCredentialStateRequest()) }
                            finishDeletion(onResult, true, "Account eliminato correttamente.")
                        }
                    } else {
                        finishDeletion(onResult, false, "Non è stato possibile eliminare l'account. Riprova tra poco.")
                    }
                }
            }
        }
    }

    private fun requestGoogleToken(onResult: (String?, String?) -> Unit) {
        // This is the explicit button flow: unlike the optional bottom sheet,
        // it keeps working when the user has disabled Google sign-in prompts.
        val option = GetSignInWithGoogleOption.Builder(
            activity.getString(R.string.default_web_client_id)
        )
            .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        appScope.launch {
            try {
                val credential = credentialManager.getCredential(activity, request).credential
                if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    onResult(GoogleIdTokenCredential.createFrom(credential.data).idToken, null)
                } else {
                    onResult(null, "L'account selezionato non può essere usato per l'accesso Google.")
                }
            } catch (error: GetCredentialException) {
                Log.w("TaskOrganizerAuth", "Google credential request failed", error)
                onResult(null, "Accesso annullato o non disponibile. Puoi riprovare quando vuoi.")
            }
        }
    }

    private fun finishDeletion(onResult: (Boolean, String?) -> Unit, deleted: Boolean, message: String) {
        // The Firebase task can finish while the activity is stopped. Preserve its result so the
        // next visible session can report the real outcome instead of leaving the person unsure.
        resultPreferences.edit().putString(PENDING_RESULT_MESSAGE, message).apply()
        onResult(deleted, message)
    }

    private companion object {
        const val ACCOUNT_RESULT_PREFERENCES = "task_organizer_account_results"
        const val PENDING_RESULT_MESSAGE = "pending_account_result"
    }
}
