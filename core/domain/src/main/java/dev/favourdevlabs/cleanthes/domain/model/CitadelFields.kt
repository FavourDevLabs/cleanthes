package dev.favourdevlabs.cleanthes.domain.model

/**
 * The set of a Citadel entry's fields that are versioned in history and
 * compared when computing what changed between two points in time.
 * Implemented by both CitadelItem (the current, live entry) and
 * CitadelHistoryItem (a past snapshot) so the two can be diffed directly.
 */
interface CitadelFields {
    val title: String
    val username: String
    val password: String
    val website: String?
    val notes: String?
    val totpSecret: String?
}
