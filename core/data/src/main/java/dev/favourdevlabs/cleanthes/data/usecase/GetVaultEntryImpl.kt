package dev.favourdevlabs.cleanthes.data.usecase

import dev.favourdevlabs.cleanthes.data.mapper.toDomain
import dev.favourdevlabs.cleanthes.data.repository.VaultRepository
import dev.favourdevlabs.cleanthes.domain.usecase.GetVaultEntry
import dev.favourdevlabs.cleanthes.domain.model.VaultItem
import javax.crypto.SecretKey
import javax.inject.Inject

class GetVaultEntryImpl @Inject constructor(
    private val repository: VaultRepository,
) : GetVaultEntry {
    override suspend fun invoke(id: Long, key: SecretKey): VaultItem? =
        repository.getEntryById(id, key)?.toDomain()
}

