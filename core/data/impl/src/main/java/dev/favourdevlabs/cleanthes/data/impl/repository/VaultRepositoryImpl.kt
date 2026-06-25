package dev.favourdevlabs.cleanthes.data.impl.repository

import dev.favourdevlabs.cleanthes.data.api.VaultRepository
import dev.favourdevlabs.cleanthes.data.impl.db.VaultDao
import dev.favourdevlabs.cleanthes.data.impl.entities.VaultEntry
import dev.favourdevlabs.cleanthes.data.impl.mapper.toDomain
import dev.favourdevlabs.cleanthes.data.impl.mapper.toEntity
import dev.favourdevlabs.cleanthes.domain.model.VaultItem
import dev.favourdevlabs.cleanthes.security.CryptoManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.crypto.SecretKey
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VaultRepositoryImpl @Inject constructor(
    private val vaultDao: VaultDao,
) : VaultRepository {

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
        val entry = VaultEntry(
            title             = title,
            username          = userName,
            encryptedPassword = encPwd,
            website           = website,
            category          = category,
            notes             = notes,
            isFavorite        = isFavorite,
            createdAt         = now,
            updatedAt         = now,
            totpSecret        = encTotp,
            totpIssuer        = totpIssuer,
            totpDigits        = totpDigits,
            totpPeriod        = totpPeriod,
            totpAlgorithm     = totpAlgorithm ?: "SHA1",
        )
        val id = vaultDao.insert(entry)
        if (id != -1L) entry.id = id
        id
    }

    override suspend fun updateEntry(
        item: VaultItem,
        plainPassword: String,
        key: SecretKey,
    ): Int = withContext(Dispatchers.IO) {
        val entity = item.toEntity().apply {
            encryptedPassword = CryptoManager.encrypt(plainPassword, key)
            totpSecret = if (!item.totpSecret.isNullOrEmpty())
                CryptoManager.encrypt(item.totpSecret!!, key) else null
            updatedAt = System.currentTimeMillis()
        }
        vaultDao.update(entity)
    }

    override suspend fun deleteEntry(id: Long): Int =
        withContext(Dispatchers.IO) { vaultDao.deleteById(id) }

    override suspend fun wipeVault(): Int =
        withContext(Dispatchers.IO) { vaultDao.deleteAll() }

    override suspend fun getAllEntries(key: SecretKey): List<VaultItem> =
        withContext(Dispatchers.IO) {
            vaultDao.getAllEntries().map { decrypt(it, key).toDomain() }
        }

    override suspend fun getEntryById(id: Long, key: SecretKey): VaultItem? =
        withContext(Dispatchers.IO) {
            vaultDao.getEntryById(id)?.let { decrypt(it, key).toDomain() }
        }

    override suspend fun searchEntries(query: String, key: SecretKey): List<VaultItem> =
        withContext(Dispatchers.IO) {
            vaultDao.searchEntries(query).map { decrypt(it, key).toDomain() }
        }

    override suspend fun getEntriesByCategory(category: String, key: SecretKey): List<VaultItem> =
        withContext(Dispatchers.IO) {
            vaultDao.getEntriesByCategory(category).map { decrypt(it, key).toDomain() }
        }

    override suspend fun getFavoriteEntries(key: SecretKey): List<VaultItem> =
        withContext(Dispatchers.IO) {
            vaultDao.getFavoriteEntries().map { decrypt(it, key).toDomain() }
        }

    override suspend fun getAllCategories(): List<String> =
        withContext(Dispatchers.IO) { vaultDao.getAllCategories() }

    override suspend fun getEntryCount(): Int =
        withContext(Dispatchers.IO) { vaultDao.getEntryCount() }

    private fun decrypt(entry: VaultEntry, key: SecretKey): VaultEntry {
        entry.encryptedPassword = CryptoManager.decrypt(entry.encryptedPassword, key)
        if (!entry.totpSecret.isNullOrEmpty()) {
            entry.totpSecret = CryptoManager.decrypt(entry.totpSecret!!, key)
        }
        return entry
    }
}

