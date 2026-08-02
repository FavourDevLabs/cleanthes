package dev.favourdevlabs.cleanthes.data.impl.usecase

import dev.favourdevlabs.cleanthes.data.api.CitadelRepository
import dev.favourdevlabs.cleanthes.domain.usecase.DeleteCitadelEntry
import javax.inject.Inject

class DeleteCitadelEntryImpl @Inject constructor(
    private val repository: CitadelRepository,
) : DeleteCitadelEntry {
    override suspend fun invoke(id: Long): Int = repository.deleteEntry(id)
}
