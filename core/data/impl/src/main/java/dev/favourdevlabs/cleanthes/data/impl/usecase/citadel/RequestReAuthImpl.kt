package dev.favourdevlabs.cleanthes.data.impl.usecase.citadel

import dev.favourdevlabs.cleanthes.data.api.usecase.LoadCitadelCredentials
import dev.favourdevlabs.cleanthes.domain.model.CitadelProfile
import dev.favourdevlabs.cleanthes.domain.usecase.GetActiveCitadelProfile
import dev.favourdevlabs.cleanthes.domain.usecase.RequestReAuth
import dev.favourdevlabs.cleanthes.security.KeystoreManager
import javax.inject.Inject

class RequestReAuthImpl
    @Inject
    constructor(
        private val getActiveCitadelProfile: GetActiveCitadelProfile,
        private val loadCitadelCredentials: LoadCitadelCredentials,
    ) : RequestReAuth {
        override suspend fun invoke(action: RequestReAuth.SensitiveAction): RequestReAuth.Challenge {
            val profile = getActiveCitadelProfile() ?: return RequestReAuth.Challenge.NotRequired
            if (profile == CitadelProfile.DECOY) return RequestReAuth.Challenge.NotRequired

            val real = loadCitadelCredentials(CitadelProfile.REAL)
            return if (real.biometricEnabled) {
                RequestReAuth.Challenge.Biometric(KeystoreManager.getEncryptCipher())
            } else {
                RequestReAuth.Challenge.MasterPassword
            }
        }
    }
