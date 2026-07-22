package dev.favourdevlabs.cleanthes.data.impl.repository

import dev.favourdevlabs.cleanthes.data.api.AuditLogRepository
import dev.favourdevlabs.cleanthes.data.impl.db.VaultDatabaseSwitchboard
import dev.favourdevlabs.cleanthes.data.impl.entities.AuditLogEntry
import dev.favourdevlabs.cleanthes.data.impl.mapper.toDomain
import dev.favourdevlabs.cleanthes.domain.model.AuditLogItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

private val RETENTION_MILLIS = TimeUnit.DAYS.toMillis(30)

@Singleton
class AuditLogRepositoryImpl
    @Inject
    constructor(
        private val switchboard: VaultDatabaseSwitchboard,
    ) : AuditLogRepository {
        override suspend fun recordEvent(
            eventType: String,
            entryId: Long?,
            entryTitle: String?,
        ): Unit =
            withContext(Dispatchers.IO) {
                val now = System.currentTimeMillis()
                switchboard.auditLogDao().purgeOlderThan(now - RETENTION_MILLIS)
                switchboard.auditLogDao().insert(
                    AuditLogEntry(
                        eventType = eventType,
                        entryId = entryId,
                        entryTitle = entryTitle,
                        timestamp = now,
                    ),
                )
            }

        override suspend fun getAllEvents(): List<AuditLogItem> =
            withContext(Dispatchers.IO) {
                switchboard.auditLogDao().getAllEntries().map { it.toDomain() }
            }
    }
