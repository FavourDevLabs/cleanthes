package dev.favourdevlabs.cleanthes.data.impl.usecase

import dev.favourdevlabs.cleanthes.data.api.AuditLogRepository
import dev.favourdevlabs.cleanthes.domain.model.AuditLogItem
import dev.favourdevlabs.cleanthes.domain.usecase.GetAuditLog
import javax.inject.Inject

class GetAuditLogImpl
    @Inject
    constructor(
        private val repository: AuditLogRepository,
    ) : GetAuditLog {
        override suspend fun invoke(): List<AuditLogItem> = repository.getAllEvents()
    }
