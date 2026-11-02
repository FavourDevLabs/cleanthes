package dev.favourdevlabs.cleanthes.data.impl.audit

import dev.favourdevlabs.cleanthes.data.impl.crypto.FieldCipher
import dev.favourdevlabs.cleanthes.data.impl.db.AuditLogDao
import dev.favourdevlabs.cleanthes.data.impl.db.CitadelDatabaseSwitchboard
import dev.favourdevlabs.cleanthes.data.impl.db.CitadelEntryHistoryDao
import dev.favourdevlabs.cleanthes.data.impl.entities.CitadelEntryHistory
import javax.crypto.SecretKey
import javax.inject.Inject

/**
 * Re-encrypts the data that [CitadelRepository.reencryptAllEntries] does not cover when the vault
 * key rotates: history snapshots and audit-log title snapshots. Call it in the same transaction.
 *
 * A history row that cannot be decrypted with the old key is unrecoverable, so it is deleted
 * instead of failing the whole rotation. An audit title that cannot be opened is cleared.
 */
class AuditAndHistoryReencryptor internal constructor(
    private val historyDao: () -> CitadelEntryHistoryDao,
    private val auditDao: () -> AuditLogDao,
    private val cipher: FieldCipher,
    private val codec: AuditTitleCodec,
) {
    @Inject
    constructor(
        switchboard: CitadelDatabaseSwitchboard,
        cipher: FieldCipher,
        codec: AuditTitleCodec,
    ) : this(
        { switchboard.citadelEntryHistoryDao() },
        { switchboard.auditLogDao() },
        cipher,
        codec,
    )

    suspend fun reencrypt(
        oldKey: SecretKey,
        newKey: SecretKey,
    ) {
        reencryptHistory(oldKey, newKey)
        reencryptAuditTitles(oldKey, newKey)
    }

    private suspend fun reencryptHistory(
        oldKey: SecretKey,
        newKey: SecretKey,
    ) {
        val dao = historyDao()
        val updated = mutableListOf<CitadelEntryHistory>()
        val unreadableIds = mutableListOf<Long>()
        for (row in dao.getAll()) {
            try {
                updated +=
                    row.copy(
                        title = swap(row.title, oldKey, newKey),
                        username = swap(row.username, oldKey, newKey),
                        encryptedPassword = swap(row.encryptedPassword, oldKey, newKey),
                        website = swapOptional(row.website, oldKey, newKey),
                        notes = swapOptional(row.notes, oldKey, newKey),
                        totpSecret = swapOptional(row.totpSecret, oldKey, newKey),
                    )
            } catch (_: Exception) {
                unreadableIds += row.id
            }
        }
        if (updated.isNotEmpty()) dao.updateAll(updated)
        unreadableIds.chunked(DELETE_CHUNK).forEach { dao.deleteByIds(it) }
    }

    private suspend fun reencryptAuditTitles(
        oldKey: SecretKey,
        newKey: SecretKey,
    ) {
        val dao = auditDao()
        for (row in dao.getAllEntries()) {
            val stored = row.entryTitle ?: continue
            val plain = codec.open(stored, oldKey)
            dao.updateTitle(row.id, plain?.let { codec.seal(it, newKey) })
        }
    }

    private fun swap(
        value: String,
        oldKey: SecretKey,
        newKey: SecretKey,
    ): String = cipher.encrypt(cipher.decrypt(value, oldKey), newKey)

    private fun swapOptional(
        value: String?,
        oldKey: SecretKey,
        newKey: SecretKey,
    ): String? = if (value.isNullOrEmpty()) value else swap(value, oldKey, newKey)

    private companion object {
        const val DELETE_CHUNK = 500
    }
}
