package dev.favourdevlabs.cleanthes.data.fakes

import dev.favourdevlabs.cleanthes.domain.model.CitadelItem
import dev.favourdevlabs.cleanthes.domain.usecase.GetCitadelEntry
import javax.crypto.SecretKey

class FakeGetCitadelEntry : GetCitadelEntry {

    var result: CitadelItem? = null
    var shouldThrow: Boolean = false

    override suspend fun invoke(id: Long, key: SecretKey): CitadelItem? {
        if (shouldThrow) throw RuntimeException("FakeGetCitadelEntry error")
        return result
    }
}
