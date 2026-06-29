package dev.favourdevlabs.cleanthes.data.fakes

import dev.favourdevlabs.cleanthes.domain.usecase.DeleteVaultEntry

class FakeDeleteVaultEntry : DeleteVaultEntry {

    val deletedIds = mutableListOf<Long>()
    var shouldThrow: Boolean = false

    override suspend fun invoke(id: Long): Int {
        if (shouldThrow) throw RuntimeException("FakeDeleteVaultEntry error")
        deletedIds.add(id)
        return 1
    }

    // Test helpers
    fun wasDeleted(id: Long): Boolean = id in deletedIds
    fun reset() = deletedIds.clear()
}
