package dev.favourdevlabs.cleanthes.data.api

import dev.favourdevlabs.cleanthes.domain.model.CitadelItem
import javax.crypto.SecretKey

interface CitadelRepository {

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
        item: CitadelItem,
        plainPassword: String,
        key: SecretKey,
    ): Int

    suspend fun deleteEntry(id: Long): Int

    suspend fun wipeCitadel(): Int

    suspend fun getAllEntries(key: SecretKey): List<CitadelItem>

    suspend fun getEntryById(id: Long, key: SecretKey): CitadelItem?

    suspend fun searchEntries(query: String, key: SecretKey): List<CitadelItem>

    suspend fun getEntriesByDomainCandidate(domain: String, key: SecretKey): List<CitadelItem>

    suspend fun getEntriesByCategory(category: String, key: SecretKey): List<CitadelItem>

    suspend fun getFavoriteEntries(key: SecretKey): List<CitadelItem>

    suspend fun getAllCategories(): List<String>

    suspend fun getEntryCount(): Int

    suspend fun reencryptAllEntries(oldKey: SecretKey, newKey: SecretKey)
}
