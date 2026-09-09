package dev.favourdevlabs.cleanthes.data.impl.usecase

import dev.favourdevlabs.cleanthes.data.api.CitadelRepository
import dev.favourdevlabs.cleanthes.domain.model.CitadelHistoryItem
import dev.favourdevlabs.cleanthes.domain.usecase.GetCitadelHistory
import javax.crypto.SecretKey
import javax.inject.Inject

class GetCitadelHistoryImpl
    @Inject
    constructor(
        private val repository: CitadelRepository,
    ) : GetCitadelHistory {
        override suspend fun invoke(entryId: Long, key: SecretKey): List<CitadelHistoryItem> =
            repository.getHistoryForEntry(entryId, key)
    }
