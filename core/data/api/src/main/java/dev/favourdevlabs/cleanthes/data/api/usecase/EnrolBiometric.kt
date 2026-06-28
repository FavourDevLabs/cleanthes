package dev.favourdevlabs.cleanthes.data.api.usecase

import javax.crypto.Cipher
import javax.crypto.SecretKey

interface EnrolBiometric {
    suspend operator fun invoke(vaultKey: SecretKey, unlockedCipher: Cipher)
}

