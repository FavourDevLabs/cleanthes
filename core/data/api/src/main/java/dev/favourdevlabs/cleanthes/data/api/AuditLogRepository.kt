package dev.favourdevlabs.cleanthes.data.api

import dev.favourdevlabs.cleanthes.domain.model.AuditLogItem

interface AuditLogRepository {
    suspend fun recordEvent(
        eventType: String,
        entryId: Long?,
        entryTitle: String?,
    )

    suspend fun getAllEvents(): List<AuditLogItem>
}
