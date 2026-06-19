package dev.favourdevlabs.cleanthes.domain.usecase

import android.util.Base64
import dev.favourdevlabs.cleanthes.security.KeyDerivation
import dev.favourdevlabs.cleanthes.security.SessionManager
import javax.inject.Inject
import javax.crypto.SecretKey

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UnlockVaultUseCase @Inject constructor(
    private val sessionManager: SessionManager,
) {
    sealed interface Params {
        data class Password(val masterPassword: String, val encSalt: String, val wrappedVaultKey: String,) : Params
        data class Biometric(val vaultKey: SecretKey) : Params
    }

    @Throws(Exception::class)
    suspend operator fun invoke(params: Params) = withContext(Dispatchers.IO) {
        val vaultKey = when (params) {
            is Params.Password -> {
                val saltBytes = Base64.decode(params.encSalt, Base64.DEFAULT)
                val pwdDerivedKey = KeyDerivation.deriveKey(params.masterPassword.toCharArray(), saltBytes)
                KeyDerivation.unwrapKey(params.wrappedVaultKey, pwdDerivedKey)
            }
            is Params.Biometric -> params.vaultKey
        }
        sessionManager.setSessionKey(vaultKey)
    }
}
