package dev.favourdevlabs.cleanthes.data.fakes

import dev.favourdevlabs.cleanthes.domain.model.VaultItem
import dev.favourdevlabs.cleanthes.domain.usecase.GetVaultEntry
import javax.crypto.SecretKey

class FakeGetVaultEntry : GetVaultEntry {

    var result: VaultItem? = null
    var shouldThrow: Boolean = false

    override suspend fun invoke(id: Long, key: SecretKey): VaultItem? {
        if (shouldThrow) throw RuntimeException("FakeGetVaultEntry error")
        return result
    }
}
