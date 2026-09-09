package dev.favourdevlabs.cleanthes.domain.model

/**
 * A single decrypted historical snapshot of a CitadelItem's sensitive
 * fields as they were before some earlier edit. Distinct from CitadelItem
 * itself since history is always read-only and scoped to one past point
 * in time, never the "current" entry.
 */
data class CitadelHistoryItem(
    val id: Long,
    val entryId: Long,
    val title: String,
    val username: String,
    val password: String,
    val website: String?,
    val notes: String?,
    val totpSecret: String?,
    val timestamp: Long,
)
