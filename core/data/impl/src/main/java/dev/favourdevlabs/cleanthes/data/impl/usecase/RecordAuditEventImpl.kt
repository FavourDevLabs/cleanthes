package dev.favourdevlabs.cleanthes.data.impl.usecase

import dev.favourdevlabs.cleanthes.data.api.AuditLogRepository
import dev.favourdevlabs.cleanthes.data.impl.audit.AuditTitleCodec
import dev.favourdevlabs.cleanthes.domain.usecase.RecordAuditEvent
import dev.favourdevlabs.cleanthes.security.session.SessionManager
import javax.inject.Inject

class RecordAuditEventImpl
    @Inject
    constructor(
        private val repository: AuditLogRepository,
        private val sessionManager: SessionManager,
        private val codec: AuditTitleCodec,
    ) : RecordAuditEvent {
        override suspend fun invoke(
            eventType: RecordAuditEvent.EventType,
            entryId: Long?,
            entryTitle: String?,
        ) {
            // Titles are sealed with the vault key. With no session key (locked) nothing is stored,
            // so a title can never reach the database in plaintext.
            val sealed =
                entryTitle?.let { title ->
                    sessionManager.withSessionKey { key -> codec.seal(title, key) }
                }
            repository.recordEvent(eventType.name, entryId, sealed)
        }
    }
