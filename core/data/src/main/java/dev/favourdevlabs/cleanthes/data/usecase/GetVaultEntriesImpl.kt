package dev.favourdevlabs.cleanthes.data.usecase

import dev.favourdevlabs.cleanthes.data.mapper.toDomain
import dev.favourdevlabs.cleanthes.data.repository.VaultRepository
import dev.favourdevlabs.cleanthes.domain.usecase.GetVaultEntries
import javax.crypto.SecretKey
import javax.inject.Inject

class GetVaultEntriesImpl @Inject constructor(
    private val repository: VaultRepository,
) : GetVaultEntries {
    override suspend fun invoke(key: SecretKey): GetVaultEntries.Result =
        GetVaultEntries.Result(
            entries    = repository.getAllEntries(key).map { it.toDomain() },
            categories = repository.getAllCategories(),
        )
}

