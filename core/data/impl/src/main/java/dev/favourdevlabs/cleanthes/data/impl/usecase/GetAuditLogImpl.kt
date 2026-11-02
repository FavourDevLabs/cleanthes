package dev.favourdevlabs.cleanthes.data.impl.usecase

import dev.favourdevlabs.cleanthes.data.api.AuditLogRepository
import dev.favourdevlabs.cleanthes.data.impl.audit.AuditTitleCodec
import dev.favourdevlabs.cleanthes.domain.model.AuditLogItem
import dev.favourdevlabs.cleanthes.domain.usecase.GetAuditLog
import dev.favourdevlabs.cleanthes.security.session.SessionManager
import javax.inject.Inject

class GetAuditLogImpl
    @Inject
    constructor(
        private val repository: AuditLogRepository,
        private val sessionManager: SessionManager,
        private val codec: AuditTitleCodec,
    ) : GetAuditLog {
        override suspend fun invoke(): List<AuditLogItem> {
            val events = repository.getAllEvents()
            // Locked, unreadable or legacy values all come back as null titles.
            return sessionManager.withSessionKey { key ->
                events.map { it.copy(entryTitle = codec.open(it.entryTitle, key)) }
            } ?: events.map { it.copy(entryTitle = null) }
        }
    }
