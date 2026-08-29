package dev.favourdevlabs.cleanthes.data.api.usecase

interface CheckPasswordBreach {
    sealed interface Result {
        data object Safe : Result
        data class Breached(val breachCount: Int) : Result
        data object CheckFailed : Result
    }

    suspend operator fun invoke(password: String): Result
}
