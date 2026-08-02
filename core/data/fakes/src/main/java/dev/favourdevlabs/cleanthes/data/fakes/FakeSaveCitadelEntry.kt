package dev.favourdevlabs.cleanthes.data.fakes

import dev.favourdevlabs.cleanthes.domain.usecase.SaveCitadelEntry

class FakeSaveCitadelEntry : SaveCitadelEntry {

    val calls = mutableListOf<SaveCitadelEntry.Params>()
    var shouldThrow: Boolean = false
    var returnId: Long = 1L

    override suspend fun invoke(params: SaveCitadelEntry.Params): Long {
        if (shouldThrow) throw RuntimeException("FakeSaveCitadelEntry error")
        calls.add(params)
        return returnId
    }

    // Test helpers
    fun lastCall(): SaveCitadelEntry.Params = calls.last()
    fun callCount(): Int = calls.size
    fun reset() = calls.clear()
}
