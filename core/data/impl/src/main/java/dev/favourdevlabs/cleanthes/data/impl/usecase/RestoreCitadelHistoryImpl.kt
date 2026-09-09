package dev.favourdevlabs.cleanthes.data.impl.usecase

import dev.favourdevlabs.cleanthes.data.api.CitadelRepository
import dev.favourdevlabs.cleanthes.domain.usecase.RestoreCitadelHistory
import javax.crypto.SecretKey
import javax.inject.Inject

class RestoreCitadelHistoryImpl
    @Inject
    constructor(
        private val repository: CitadelRepository,
    ) : RestoreCitadelHistory {
        override suspend fun invoke(historyId: Long, key: SecretKey): Int =
            repository.restoreFromHistory(historyId, key)
    }
