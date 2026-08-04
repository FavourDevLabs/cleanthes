package dev.favourdevlabs.cleanthes.security.session

import kotlinx.coroutines.flow.StateFlow
import javax.crypto.SecretKey

/**
 * Owns the lifecycle of the in-memory citadel decryption key for the current
 * unlocked session. [lockState] is the single source of truth for whether
 * the app is currently locked — Activities and ViewModels should observe
 * it rather than polling.
 *
 * Auto-lock policy: a 30s grace period is given when the app is fully
 * backgrounded (all activities stopped) before the session is cleared, and
 * a 2min foreground inactivity timeout clears the session even while the
 * app stays open, if [refreshSession] is not called in that window.
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
     * Called by the app-level lifecycle tracker when the last activity stops
     * (app fully backgrounded). Starts the background-grace timer.
     */
    fun notifyAppBackgrounded()
    /**
     * Called by the app-level lifecycle tracker when the first activity
     * starts (app returns to foreground). Cancels any pending
     * background-grace lock.
     */
    fun notifyAppForegrounded()
    /**
     * Returns the epoch-millis timestamp of the last time the session was
     * started or refreshed, or 0L if no session has ever been active.
     */
    fun getLastActiveTimestamp(): Long
}
