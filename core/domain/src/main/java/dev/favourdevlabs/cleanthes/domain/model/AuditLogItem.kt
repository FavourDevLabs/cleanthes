package dev.favourdevlabs.cleanthes.domain.model

/**
 * Pure domain model — no Room annotations, no Android imports.
 * AuditLogEntry in :core:data:impl maps to/from this.
 */
data class AuditLogItem(
    val id: Long = 0,
    val eventType: String = "",
    val entryId: Long? = null,
    val entryTitle: String? = null,
    val timestamp: Long = 0,
)
