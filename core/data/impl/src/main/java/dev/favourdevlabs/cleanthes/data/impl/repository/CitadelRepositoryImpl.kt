package dev.favourdevlabs.cleanthes.data.impl.repository

import dev.favourdevlabs.cleanthes.data.api.CitadelRepository
import dev.favourdevlabs.cleanthes.data.impl.db.CitadelDatabaseSwitchboard
import dev.favourdevlabs.cleanthes.data.impl.entities.CitadelEntry
import dev.favourdevlabs.cleanthes.data.impl.entities.CitadelEntryHistory
import dev.favourdevlabs.cleanthes.data.impl.mapper.toDomain
import dev.favourdevlabs.cleanthes.data.impl.mapper.toEntity
import dev.favourdevlabs.cleanthes.domain.model.CitadelItem
import dev.favourdevlabs.cleanthes.security.CryptoManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.crypto.SecretKey
import javax.inject.Inject
import javax.inject.Singleton
import dev.favourdevlabs.cleanthes.domain.model.CitadelHistoryItem

@Singleton
class CitadelRepositoryImpl @Inject constructor(
    private val switchboard: CitadelDatabaseSwitchboard,
) : CitadelRepository {

    override suspend fun addEntry(
        title: String,
        userName: String,
        plainPassword: String,
        website: String?,
        category: String,
        notes: String?,
        isFavorite: Boolean,
        plainTotpSecret: String?,
        totpIssuer: String?,
        totpDigits: Int,
        totpPeriod: Int,
        totpAlgorithm: String?,
        key: SecretKey,
    ): Long = withContext(Dispatchers.IO) {
        val encPwd  = CryptoManager.encrypt(plainPassword, key)
val encTotp = if (!plainTotpSecret.isNullOrEmpty())
    CryptoManager.encrypt(plainTotpSecret, key) else null

val now = System.currentTimeMillis()
val entry = CitadelEntry(
    title             = CryptoManager.encrypt(title, key),
    username          = CryptoManager.encrypt(userName, key),
    encryptedPassword = encPwd,
    website           = website?.let { CryptoManager.encrypt(it, key) },
    category          = category,
    notes             = notes?.let { CryptoManager.encrypt(it, key) },
            isFavorite        = isFavorite,
            createdAt         = now,
            updatedAt         = now,
            totpSecret        = encTotp,
            totpIssuer        = totpIssuer,
            totpDigits        = totpDigits,
            totpPeriod        = totpPeriod,
            totpAlgorithm     = totpAlgorithm ?: "SHA1",
        )
        val id = switchboard.citadelDao().insert(entry)
        if (id != -1L) entry.id = id
        id
    }

    override suspend fun updateEntry(
    item: CitadelItem,
    plainPassword: String,
    key: SecretKey,
): Int = withContext(Dispatchers.IO) {
    val existing = switchboard.citadelDao().getEntryById(item.id)

    val newEncryptedPassword = CryptoManager.encrypt(plainPassword, key)
    val newTotpSecret = if (!item.totpSecret.isNullOrEmpty())
        CryptoManager.encrypt(item.totpSecret!!, key) else null

    if (existing != null) {
        val existingTitle = CryptoManager.decrypt(existing.title, key)
        val existingUsername = CryptoManager.decrypt(existing.username, key)
        val existingPassword = CryptoManager.decrypt(existing.encryptedPassword, key)
        val existingWebsite = if (!existing.website.isNullOrEmpty())
            CryptoManager.decrypt(existing.website!!, key) else null
                val existingNotes = if (!existing.notes.isNullOrEmpty())
            CryptoManager.decrypt(existing.notes!!, key) else null
        val existingTotpSecret = if (!existing.totpSecret.isNullOrEmpty())
            CryptoManager.decrypt(existing.totpSecret!!, key) else null

        val contentChanged =
            existingTitle != item.title ||
                existingUsername != item.username ||
                existingPassword != plainPassword ||
                existingWebsite != item.website ||
                existingNotes != item.notes ||
                existingTotpSecret != item.totpSecret

        if (contentChanged) {
            switchboard.citadelEntryHistoryDao().insert(
                CitadelEntryHistory(
                    entryId = existing.id,
                    title = existing.title,
                    username = existing.username,
                    encryptedPassword = existing.encryptedPassword,
                    website = existing.website,
                    notes = existing.notes,
                    totpSecret = existing.totpSecret,
                    timestamp = System.currentTimeMillis(),
                )
            )
        }
    }

    val entity = item.toEntity().apply {
        title = CryptoManager.encrypt(item.title, key)
        username = CryptoManager.encrypt(item.username, key)
        encryptedPassword = newEncryptedPassword
        website = item.website?.let { CryptoManager.encrypt(it, key) }
        notes = item.notes?.let { CryptoManager.encrypt(it, key) }
        totpSecret = newTotpSecret
        updatedAt = System.currentTimeMillis()
    }
    switchboard.citadelDao().update(entity)
}   

    override suspend fun deleteEntry(id: Long): Int =
        withContext(Dispatchers.IO) { switchboard.citadelDao().deleteById(id) }

    override suspend fun wipeCitadel(): Int =
        withContext(Dispatchers.IO) { switchboard.citadelDao().deleteAll() }

    override suspend fun getAllEntries(key: SecretKey): List<CitadelItem> =
    withContext(Dispatchers.IO) {
        switchboard.citadelDao().getAllEntries()
            .map { decrypt(it, key) }
            .sortedWith(compareByDescending<CitadelEntry> { it.isFavorite }.thenBy { it.title })
            .map { it.toDomain() }
    }

    override suspend fun getEntryById(id: Long, key: SecretKey): CitadelItem? =
        withContext(Dispatchers.IO) {
            switchboard.citadelDao().getEntryById(id)?.let { decrypt(it, key).toDomain() }
        }

    override suspend fun searchEntries(query: String, key: SecretKey): List<CitadelItem> =
    withContext(Dispatchers.IO) {
        switchboard.citadelDao().getAllEntries()
            .map { decrypt(it, key) }
            .sortedWith(compareByDescending<CitadelEntry> { it.isFavorite }.thenBy { it.title })
            .filter { entry ->
                entry.title.contains(query, ignoreCase = true) ||
                    entry.username.contains(query, ignoreCase = true)
            }
            .map { it.toDomain() }
    }

    override suspend fun getEntriesByDomainCandidate(domain: String, key: SecretKey): List<CitadelItem> =
    withContext(Dispatchers.IO) {
        switchboard.citadelDao().getAllEntries()
            .map { decrypt(it, key) }
            .sortedWith(compareByDescending<CitadelEntry> { it.isFavorite }.thenBy { it.title })
            .filter { entry ->
                (entry.website?.contains(domain, ignoreCase = true) == true) ||
                    entry.title.contains(domain, ignoreCase = true)
            }
            .map { it.toDomain() }
    }

    override suspend fun getEntriesByCategory(category: String, key: SecretKey): List<CitadelItem> =
        withContext(Dispatchers.IO) {
            switchboard.citadelDao().getEntriesByCategory(category).map { decrypt(it, key).toDomain() }
        }

    override suspend fun getFavoriteEntries(key: SecretKey): List<CitadelItem> =
    withContext(Dispatchers.IO) {
        switchboard.citadelDao().getFavoriteEntries()
            .map { decrypt(it, key) }
            .sortedBy { it.title }
            .map { it.toDomain() }
    }

    override suspend fun getAllCategories(): List<String> =
        withContext(Dispatchers.IO) { switchboard.citadelDao().getAllCategories() }

    override suspend fun getEntryCount(): Int =
        withContext(Dispatchers.IO) { switchboard.citadelDao().getEntryCount() }

    override suspend fun reencryptAllEntries(oldKey: SecretKey, newKey: SecretKey): Unit =
        withContext(Dispatchers.IO) {
            val entries = switchboard.citadelDao().getAllEntries()
            val reencrypted = entries.map { entry ->
                val decryptedTitle = CryptoManager.decrypt(entry.title, oldKey)
                val decryptedUsername = CryptoManager.decrypt(entry.username, oldKey)
                val decryptedPassword = CryptoManager.decrypt(entry.encryptedPassword, oldKey)
                val decryptedWebsite = if (!entry.website.isNullOrEmpty()) {
                    CryptoManager.decrypt(entry.website!!, oldKey)
                } else null
                val decryptedNotes = if (!entry.notes.isNullOrEmpty()) {
                    CryptoManager.decrypt(entry.notes!!, oldKey)
                } else null
                val decryptedTotp = if (!entry.totpSecret.isNullOrEmpty()) {
                    CryptoManager.decrypt(entry.totpSecret!!, oldKey)
                } else null

                entry.copy(
                    title = CryptoManager.encrypt(decryptedTitle, newKey),
                    username = CryptoManager.encrypt(decryptedUsername, newKey),
                    encryptedPassword = CryptoManager.encrypt(decryptedPassword, newKey),
                    website = decryptedWebsite?.let { CryptoManager.encrypt(it, newKey) },
                    notes = decryptedNotes?.let { CryptoManager.encrypt(it, newKey) },
                    totpSecret = decryptedTotp?.let { CryptoManager.encrypt(it, newKey) },
                )
            }
            switchboard.citadelDao().updateAll(reencrypted)
        }

    override suspend fun getHistoryForEntry(entryId: Long, key: SecretKey): List<CitadelHistoryItem> =
        withContext(Dispatchers.IO) {
            switchboard.citadelEntryHistoryDao().getHistoryForEntry(entryId).map { history ->
                CitadelHistoryItem(
                    id = history.id,
                    entryId = history.entryId,
                    title = CryptoManager.decrypt(history.title, key),
                    username = CryptoManager.decrypt(history.username, key),
                    password = CryptoManager.decrypt(history.encryptedPassword, key),
                    website = history.website?.let { CryptoManager.decrypt(it, key) },
                    notes = history.notes?.let { CryptoManager.decrypt(it, key) },
                    totpSecret = history.totpSecret?.let { CryptoManager.decrypt(it, key) },   
                    timestamp = history.timestamp,
                )
            }
        }

    override suspend fun restoreFromHistory(historyId: Long, key: SecretKey): Int =
        withContext(Dispatchers.IO) {
            val history = switchboard.citadelEntryHistoryDao().getHistoryById(historyId)
                ?: return@withContext 0
            val current = switchboard.citadelDao().getEntryById(history.entryId)
                ?: return@withContext 0

            // Snapshot the CURRENT state before overwriting it — a restore is
            // itself an undoable edit, not a destructive overwrite.
            switchboard.citadelEntryHistoryDao().insert(
                CitadelEntryHistory(
                    entryId = current.id,
                    title = current.title,
                    username = current.username,
                    encryptedPassword = current.encryptedPassword,
                    website = current.website,
                    notes = current.notes,
                    totpSecret = current.totpSecret,
                    timestamp = System.currentTimeMillis(),
                )
            )

            val restored = current.copy(
                title = history.title,
                username = history.username,
                encryptedPassword = history.encryptedPassword,
                website = history.website,
                notes = history.notes,
                totpSecret = history.totpSecret,
                updatedAt = System.currentTimeMillis(),
            )
            switchboard.citadelDao().update(restored)
        }   

    private fun decrypt(entry: CitadelEntry, key: SecretKey): CitadelEntry {
    entry.title = CryptoManager.decrypt(entry.title, key)
    entry.username = CryptoManager.decrypt(entry.username, key)
    entry.encryptedPassword = CryptoManager.decrypt(entry.encryptedPassword, key)
    if (!entry.website.isNullOrEmpty()) {
        entry.website = CryptoManager.decrypt(entry.website!!, key)
    }
    if (!entry.notes.isNullOrEmpty()) {
        entry.notes = CryptoManager.decrypt(entry.notes!!, key)
    }
    if (!entry.totpSecret.isNullOrEmpty()) {
        entry.totpSecret = CryptoManager.decrypt(entry.totpSecret!!, key)
    }
    return entry
}


}
