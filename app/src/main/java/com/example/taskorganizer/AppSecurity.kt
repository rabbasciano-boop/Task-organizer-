package com.example.taskorganizer

import android.content.Context
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** Keeps only a salted, deliberately slow PIN verifier; the PIN itself is never stored. */
class AppSecurity(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    fun hasPin(): Boolean = preferences.contains(PIN_HASH) && preferences.contains(PIN_SALT)

    fun setPin(pin: String) {
        val salt = ByteArray(SALT_SIZE).also { SecureRandom().nextBytes(it) }
        preferences.edit()
            .putString(PIN_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString(PIN_HASH, Base64.encodeToString(derive(pin, salt), Base64.NO_WRAP))
            .remove(FAILED_ATTEMPTS)
            .remove(LOCKED_UNTIL)
            .apply()
    }

    fun verifyPin(pin: String): Boolean = verifyPinWithRateLimit(pin) is PinVerification.Verified

    fun verifyPinWithRateLimit(pin: String): PinVerification {
        val now = System.currentTimeMillis()
        val lockedUntil = preferences.getLong(LOCKED_UNTIL, 0L)
        if (lockedUntil > now) return PinVerification.Locked(lockedUntil)

        val salt = preferences.getString(PIN_SALT, null)?.let { Base64.decode(it, Base64.NO_WRAP) }
            ?: return PinVerification.Incorrect(0, 0L)
        val expected = preferences.getString(PIN_HASH, null)?.let { Base64.decode(it, Base64.NO_WRAP) }
            ?: return PinVerification.Incorrect(0, 0L)
        if (MessageDigest.isEqual(expected, derive(pin, salt))) {
            preferences.edit().remove(FAILED_ATTEMPTS).remove(LOCKED_UNTIL).apply()
            return PinVerification.Verified
        }

        val failedAttempts = preferences.getInt(FAILED_ATTEMPTS, 0) + 1
        val delay = delayFor(failedAttempts)
        preferences.edit()
            .putInt(FAILED_ATTEMPTS, failedAttempts)
            .putLong(LOCKED_UNTIL, if (delay > 0) now + delay else 0L)
            .apply()
        return PinVerification.Incorrect(failedAttempts, delay)
    }

    fun remainingLockMillis(now: Long = System.currentTimeMillis()): Long =
        (preferences.getLong(LOCKED_UNTIL, 0L) - now).coerceAtLeast(0L)

    fun isBiometricEnabled(): Boolean = preferences.getBoolean(BIOMETRIC_ENABLED, false)

    fun setBiometricEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(BIOMETRIC_ENABLED, enabled).apply()
    }

    fun clear() {
        preferences.edit()
            .remove(PIN_HASH)
            .remove(PIN_SALT)
            .remove(BIOMETRIC_ENABLED)
            .remove(FAILED_ATTEMPTS)
            .remove(LOCKED_UNTIL)
            .apply()
    }

    private fun delayFor(failedAttempts: Int): Long = when (failedAttempts) {
        1, 2 -> 0L
        3 -> 30_000L
        4 -> 60_000L
        5 -> 5 * 60_000L
        else -> 15 * 60_000L
    }

    private fun derive(pin: String, salt: ByteArray): ByteArray {
        val specification = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, HASH_BITS)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(specification).encoded
        } finally {
            specification.clearPassword()
        }
    }

    private companion object {
        const val PREFERENCES = "task_organizer_security"
        const val PIN_HASH = "pin_hash"
        const val PIN_SALT = "pin_salt"
        const val BIOMETRIC_ENABLED = "biometric_enabled"
        const val FAILED_ATTEMPTS = "failed_attempts"
        const val LOCKED_UNTIL = "locked_until"
        const val SALT_SIZE = 16
        const val ITERATIONS = 210_000
        const val HASH_BITS = 256
    }
}

sealed interface PinVerification {
    data object Verified : PinVerification
    data class Incorrect(val failedAttempts: Int, val delayMillis: Long) : PinVerification
    data class Locked(val untilMillis: Long) : PinVerification
}
