package dev.favourdevlabs.cleanthes.data.fakes

import dev.favourdevlabs.cleanthes.domain.usecase.SaveVaultEntry

class FakeSaveVaultEntry : SaveVaultEntry {

    val calls = mutableListOf<SaveVaultEntry.Params>()
    var shouldThrow: Boolean = false
    var returnId: Long = 1L

    override suspend fun invoke(params: SaveVaultEntry.Params): Long {
        if (shouldThrow) throw RuntimeException("FakeSaveVaultEntry error")
        calls.add(params)
        return returnId
    }

    // Test helpers
    fun lastCall(): SaveVaultEntry.Params = calls.last()
    fun callCount(): Int = calls.size
    fun reset() = calls.clear()
}
