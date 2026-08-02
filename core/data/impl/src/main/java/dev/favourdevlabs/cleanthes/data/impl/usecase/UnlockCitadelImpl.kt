package dev.favourdevlabs.cleanthes.data.impl.usecase

import android.util.Base64
import dev.favourdevlabs.cleanthes.domain.usecase.UnlockCitadel
import dev.favourdevlabs.cleanthes.security.KeyDerivation
import dev.favourdevlabs.cleanthes.security.session.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class UnlockCitadelImpl @Inject constructor(
    private val sessionManager: SessionManager,
) : UnlockCitadel {
    override suspend fun invoke(params: UnlockCitadel.Params) =
        withContext(Dispatchers.IO) {
            val citadelKey = when (params) {
                is UnlockCitadel.Params.Password -> {
                    val saltBytes     = Base64.decode(params.encSalt, Base64.DEFAULT)
                    val pwdDerivedKey = KeyDerivation.deriveKey(
                        params.masterPassword.toCharArray(), saltBytes
                    )
                    KeyDerivation.unwrapKey(params.wrappedCitadelKey, pwdDerivedKey)
                }
                is UnlockCitadel.Params.Biometric -> params.citadelKey
            }
            sessionManager.setSessionKey(citadelKey)
        }
}
