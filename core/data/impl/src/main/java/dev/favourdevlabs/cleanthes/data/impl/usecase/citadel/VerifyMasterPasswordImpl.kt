package dev.favourdevlabs.cleanthes.data.impl.usecase.citadel

import dev.favourdevlabs.cleanthes.data.api.usecase.LoadCitadelCredentials
import dev.favourdevlabs.cleanthes.domain.model.CitadelProfile
import dev.favourdevlabs.cleanthes.domain.usecase.VerifyMasterPassword
import dev.favourdevlabs.cleanthes.security.KeyDerivation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class VerifyMasterPasswordImpl
    @Inject
    constructor(
        private val loadCitadelCredentials: LoadCitadelCredentials,
    ) : VerifyMasterPassword {
        override suspend fun invoke(password: String): Boolean {
            val real = loadCitadelCredentials(CitadelProfile.REAL)
            val salt = real.authSalt ?: return false
            val hash = real.masterHash ?: return false
            return withContext(Dispatchers.IO) {
                KeyDerivation.verifyMasterPassword(password.toCharArray(), salt, hash)
            }
        }
    }
