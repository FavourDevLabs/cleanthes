package dev.favourdevlabs.cleanthes.security.session

/**
 * Captures which Activity + extras were on screen the moment a lock-triggered
 * redirect to Login fires, so the app can return the user to that exact
 * screen after successful re-authentication instead of always defaulting
 * to Home. Cleared once consumed — a stale/never-consumed value must not
 * leak into a later, unrelated unlock.
 */
interface LastScreenHolder {
    fun capture(
        className: String,
        extras: Map<String, Any?>,
    )

    fun consume(): Pair<String, Map<String, Any?>>?
}
