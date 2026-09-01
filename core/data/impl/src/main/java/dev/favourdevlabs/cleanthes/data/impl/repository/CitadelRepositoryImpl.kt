package dev.favourdevlabs.cleanthes.data.impl.repository

import dev.favourdevlabs.cleanthes.data.api.CitadelRepository
import dev.favourdevlabs.cleanthes.data.impl.db.CitadelDatabaseSwitchboard
import dev.favourdevlabs.cleanthes.data.impl.entities.CitadelEntry
import dev.favourdevlabs.cleanthes.data.impl.mapper.toDomain
import dev.favourdevlabs.cleanthes.data.impl.mapper.toEntity
import dev.favourdevlabs.cleanthes.domain.model.CitadelItem
import dev.favourdevlabs.cleanthes.security.CryptoManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.crypto.SecretKey
import javax.inject.Inject
import javax.inject.Singleton

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
        val entity = item.toEntity().apply {
    title = CryptoManager.encrypt(item.title, key)
    username = CryptoManager.encrypt(item.username, key)
    encryptedPassword = CryptoManager.encrypt(plainPassword, key)
    website = item.website?.let { CryptoManager.encrypt(it, key) }
    notes = item.notes?.let { CryptoManager.encrypt(it, key) }
    totpSecret = if (!item.totpSecret.isNullOrEmpty())
        CryptoManager.encrypt(item.totpSecret!!, key) else null
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
