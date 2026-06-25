package dev.favourdevlabs.cleanthes.data.impl.usecase

import dev.favourdevlabs.cleanthes.data.api.VaultRepository
import dev.favourdevlabs.cleanthes.domain.model.VaultItem
import dev.favourdevlabs.cleanthes.domain.usecase.GetVaultEntry
import javax.crypto.SecretKey
import javax.inject.Inject

class GetVaultEntryImpl @Inject constructor(
    private val repository: VaultRepository,
) : GetVaultEntry {
    override suspend fun invoke(id: Long, key: SecretKey): VaultItem? =
        repository.getEntryById(id, key)
}

