package dev.favourdevlabs.cleanthes.data.fakes

import dev.favourdevlabs.cleanthes.domain.model.VaultItem
import dev.favourdevlabs.cleanthes.domain.usecase.GetVaultEntries
import javax.crypto.SecretKey

class FakeGetVaultEntries : GetVaultEntries {

    var result: GetVaultEntries.Result = GetVaultEntries.Result(
        entries    = emptyList(),
        categories = emptyList(),
    )
    var shouldThrow: Boolean = false

    override suspend fun invoke(key: SecretKey): GetVaultEntries.Result {
        if (shouldThrow) throw RuntimeException("FakeGetVaultEntries error")
        return result
    }

    // Test helper
    fun setEntries(items: List<VaultItem>, categories: List<String> = emptyList()) {
        result = GetVaultEntries.Result(entries = items, categories = categories)
    }
}
