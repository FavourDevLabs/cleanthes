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
 *
 * There is deliberately no raw key getter. [withSessionKey] is the only way
 * to access the session key — it checks the lock state at call time and
 * never lets a bare SecretKey reference escape into caller-held state, so a
 * later [clearSession] cannot be bypassed by a reference grabbed earlier.
 * NOTE: this does not cancel a [block] already in flight if the session
 * clears mid-execution — only bounds how long a caller can hold the key.
 */
interface SessionManager {
    /** true = locked / no usable key. Replays current value to new subscribers. */
    val lockState: StateFlow<Boolean>
    fun setSessionKey(key: SecretKey)
    /**
     * Runs [block] with the current session key if unlocked, returning its
     * result. Returns null without invoking [block] if locked/expired.
     */
    suspend fun <T> withSessionKey(block: suspend (SecretKey) -> T): T?
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
