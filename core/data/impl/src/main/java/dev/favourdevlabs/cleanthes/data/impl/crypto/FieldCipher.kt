package dev.favourdevlabs.cleanthes.data.impl.crypto

import dev.favourdevlabs.cleanthes.security.CryptoManager
import javax.crypto.SecretKey
import javax.inject.Inject

/**
 * Thin seam over [CryptoManager] so logic that encrypts stored strings can be unit-tested on the JVM.
 * [CryptoManager] uses android.util.Base64, which plain JVM tests cannot run.
 */
open class FieldCipher
    @Inject
    constructor() {
        open fun encrypt(
            plaintext: String,
            key: SecretKey,
        ): String = CryptoManager.encrypt(plaintext, key)

        open fun decrypt(
            encrypted: String,
            key: SecretKey,
        ): String = CryptoManager.decrypt(encrypted, key)
    }
