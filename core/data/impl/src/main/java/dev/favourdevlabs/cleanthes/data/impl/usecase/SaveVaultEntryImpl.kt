package dev.favourdevlabs.cleanthes.data.impl.usecase

import dev.favourdevlabs.cleanthes.data.api.VaultRepository
import dev.favourdevlabs.cleanthes.domain.usecase.SaveVaultEntry
import javax.inject.Inject

class SaveVaultEntryImpl @Inject constructor(
    private val repository: VaultRepository,
) : SaveVaultEntry {
    override suspend fun invoke(params: SaveVaultEntry.Params): Long = when (params) {
        is SaveVaultEntry.Params.New -> repository.addEntry(
            title           = params.title,
            userName        = params.username,
            plainPassword   = params.plainPassword,
            website         = params.website,
            category        = params.category,
            notes           = params.notes,
            isFavorite      = params.isFavorite,
            plainTotpSecret = params.totpSecret,
            totpIssuer      = params.totpIssuer,
            totpDigits      = params.totpDigits,
            totpPeriod      = params.totpPeriod,
            totpAlgorithm   = params.totpAlgorithm,
            key             = params.key,
        )
        is SaveVaultEntry.Params.Edit -> repository.updateEntry(
            item          = params.item,
            plainPassword = params.plainPassword,
            key           = params.key,
        ).toLong()
    }
}

