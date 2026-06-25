package dev.favourdevlabs.cleanthes.data.api

import dev.favourdevlabs.cleanthes.domain.model.VaultItem
import javax.crypto.SecretKey

interface VaultRepository {

    suspend fun addEntry(
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
    ): Long

    suspend fun updateEntry(
        item: VaultItem,
        plainPassword: String,
        key: SecretKey,
    ): Int

    suspend fun deleteEntry(id: Long): Int

    suspend fun wipeVault(): Int

    suspend fun getAllEntries(key: SecretKey): List<VaultItem>

    suspend fun getEntryById(id: Long, key: SecretKey): VaultItem?

    suspend fun searchEntries(query: String, key: SecretKey): List<VaultItem>

    suspend fun getEntriesByCategory(category: String, key: SecretKey): List<VaultItem>

    suspend fun getFavoriteEntries(key: SecretKey): List<VaultItem>

    suspend fun getAllCategories(): List<String>

    suspend fun getEntryCount(): Int
}

