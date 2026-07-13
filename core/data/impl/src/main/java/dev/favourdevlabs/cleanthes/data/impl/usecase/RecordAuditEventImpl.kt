package dev.favourdevlabs.cleanthes.data.impl.usecase

import dev.favourdevlabs.cleanthes.data.api.AuditLogRepository
import dev.favourdevlabs.cleanthes.domain.usecase.RecordAuditEvent
import javax.inject.Inject

class RecordAuditEventImpl
    @Inject
    constructor(
        private val repository: AuditLogRepository,
    ) : RecordAuditEvent {
        override suspend fun invoke(
            eventType: RecordAuditEvent.EventType,
            entryId: Long?,
            entryTitle: String?,
        ) = repository.recordEvent(eventType.name, entryId, entryTitle)
    }
