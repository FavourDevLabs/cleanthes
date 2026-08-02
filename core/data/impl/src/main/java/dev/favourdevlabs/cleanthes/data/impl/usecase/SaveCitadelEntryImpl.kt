package dev.favourdevlabs.cleanthes.data.impl.usecase

import dev.favourdevlabs.cleanthes.data.api.CitadelRepository
import dev.favourdevlabs.cleanthes.domain.usecase.SaveCitadelEntry
import javax.inject.Inject

class SaveCitadelEntryImpl @Inject constructor(
    private val repository: CitadelRepository,
) : SaveCitadelEntry {
    override suspend fun invoke(params: SaveCitadelEntry.Params): Long = when (params) {
        is SaveCitadelEntry.Params.New -> repository.addEntry(
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
        is SaveCitadelEntry.Params.Edit -> repository.updateEntry(
            item          = params.item,
            plainPassword = params.plainPassword,
            key           = params.key,
        ).toLong()
    }
}
