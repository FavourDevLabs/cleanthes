package dev.favourdevlabs.cleanthes.security.session

import kotlinx.coroutines.flow.StateFlow
import javax.crypto.SecretKey

/**
 * Owns the lifecycle of the in-memory vault decryption key for the current
 * unlocked session. [lockState] is the single source of truth for whether
 * the app is currently locked — Activities and ViewModels should observe
 * it rather than polling.
 */
interface SessionManager {
    /** true = locked / no usable key. Replays current value to new subscribers. */
    val lockState: StateFlow<Boolean>

    fun setSessionKey(key: SecretKey)

    /**
     * Returns the current session key, or null if locked/expired.
     * NOTE: the returned reference is NOT invalidated by a later clearSession()
     * call — callers must not cache this beyond the immediate operation.
     */
    fun getSessionKey(): SecretKey?

    fun clearSession()

    fun refreshSession()

    /**
     * Returns the epoch-millis timestamp of the last time the session was
     * started or refreshed, or 0L if no session has ever been active.
     * Intended for future inactivity-based mechanisms (auto-lock, etc.) to
     * read elapsed time since last activity.
     */
    fun getLastActiveTimestamp(): Long
}
