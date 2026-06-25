package dev.favourdevlabs.cleanthes.data.usecase

import android.util.Base64
import dev.favourdevlabs.cleanthes.domain.usecase.UnlockVault
import dev.favourdevlabs.cleanthes.security.KeyDerivation
import dev.favourdevlabs.cleanthes.security.session.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class UnlockVaultImpl @Inject constructor(
    private val sessionManager: SessionManager,
) : UnlockVault {
    override suspend fun invoke(params: UnlockVault.Params) =
        withContext(Dispatchers.IO) {
            val vaultKey = when (params) {
                is UnlockVault.Params.Password -> {
                    val saltBytes    = Base64.decode(params.encSalt, Base64.DEFAULT)
                    val pwdDerivedKey = KeyDerivation.deriveKey(
                        params.masterPassword.toCharArray(), saltBytes
                    )
                    KeyDerivation.unwrapKey(params.wrappedVaultKey, pwdDerivedKey)
                }
                is UnlockVault.Params.Biometric -> params.vaultKey
            }
            sessionManager.setSessionKey(vaultKey)
        }
}

