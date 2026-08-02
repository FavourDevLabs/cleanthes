package dev.favourdevlabs.cleanthes.data.impl.usecase

import dev.favourdevlabs.cleanthes.data.api.CitadelRepository
import dev.favourdevlabs.cleanthes.domain.usecase.GetCitadelEntries
import javax.crypto.SecretKey
import javax.inject.Inject

class GetCitadelEntriesImpl @Inject constructor(
    private val repository: CitadelRepository,
) : GetCitadelEntries {
    override suspend fun invoke(key: SecretKey): GetCitadelEntries.Result =
        GetCitadelEntries.Result(
            entries    = repository.getAllEntries(key),
            categories = repository.getAllCategories(),
        )
}
