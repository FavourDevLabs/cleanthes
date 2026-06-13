package dev.favourdevlabs.cleanthes.domain.usecase

import android.util.Base64
import dev.favourdevlabs.cleanthes.security.KeyDerivation
import dev.favourdevlabs.cleanthes.security.SessionManager
import javax.inject.Inject

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UnlockVaultUseCase @Inject constructor(
    private val sessionManager: SessionManager,
) {
    sealed interface Params {
        data class Password(val masterPassword: String, val encSalt: String) : Params
        data class Biometric(val biometricSecret: String, val encSalt: String) : Params
    }

    @Throws(Exception::class)
    suspend operator fun invoke(params: Params) = withContext(Dispatchers.IO) {
        val (source, encSalt) = when (params) {
            is Params.Password  -> params.masterPassword to params.encSalt
            is Params.Biometric -> params.biometricSecret to params.encSalt
        }
        val saltBytes = Base64.decode(encSalt, Base64.DEFAULT)
        val key       = KeyDerivation.deriveKey(source.toCharArray(), saltBytes)
        sessionManager.setSessionKey(key)
    }
}
