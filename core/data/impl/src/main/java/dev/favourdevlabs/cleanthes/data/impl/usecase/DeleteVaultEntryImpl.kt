package dev.favourdevlabs.cleanthes.data.impl.usecase

import dev.favourdevlabs.cleanthes.data.api.VaultRepository
import dev.favourdevlabs.cleanthes.domain.usecase.DeleteVaultEntry
import javax.inject.Inject

class DeleteVaultEntryImpl @Inject constructor(
    private val repository: VaultRepository,
) : DeleteVaultEntry {
    override suspend fun invoke(id: Long): Int = repository.deleteEntry(id)
}

