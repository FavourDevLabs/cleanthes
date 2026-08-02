package dev.favourdevlabs.cleanthes.data.fakes

import dev.favourdevlabs.cleanthes.domain.model.CitadelItem
import dev.favourdevlabs.cleanthes.domain.usecase.GetCitadelEntries
import javax.crypto.SecretKey

class FakeGetCitadelEntries : GetCitadelEntries {

    var result: GetCitadelEntries.Result = GetCitadelEntries.Result(
        entries    = emptyList(),
        categories = emptyList(),
    )
    var shouldThrow: Boolean = false

    override suspend fun invoke(key: SecretKey): GetCitadelEntries.Result {
        if (shouldThrow) throw RuntimeException("FakeGetCitadelEntries error")
        return result
    }

    // Test helper
    fun setEntries(items: List<CitadelItem>, categories: List<String> = emptyList()) {
        result = GetCitadelEntries.Result(entries = items, categories = categories)
    }
}
