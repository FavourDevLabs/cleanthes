package dev.favourdevlabs.cleanthes.data.impl.usecase

import dev.favourdevlabs.cleanthes.data.api.CitadelRepository
import dev.favourdevlabs.cleanthes.domain.model.CitadelItem
import dev.favourdevlabs.cleanthes.domain.usecase.GetCitadelEntry
import javax.crypto.SecretKey
import javax.inject.Inject

class GetCitadelEntryImpl @Inject constructor(
    private val repository: CitadelRepository,
) : GetCitadelEntry {
    override suspend fun invoke(id: Long, key: SecretKey): CitadelItem? =
        repository.getEntryById(id, key)
}
