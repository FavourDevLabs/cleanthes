package dev.favourdevlabs.cleanthes.data.impl.mapper

import dev.favourdevlabs.cleanthes.data.impl.entities.AuditLogEntry
import dev.favourdevlabs.cleanthes.domain.model.AuditLogItem

internal fun AuditLogEntry.toDomain(): AuditLogItem =
    AuditLogItem(
        id = id,
        eventType = eventType,
        entryId = entryId,
        entryTitle = entryTitle,
        timestamp = timestamp,
    )
