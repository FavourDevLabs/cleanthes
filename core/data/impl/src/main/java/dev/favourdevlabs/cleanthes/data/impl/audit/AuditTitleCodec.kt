package dev.favourdevlabs.cleanthes.data.impl.audit

import dev.favourdevlabs.cleanthes.data.impl.crypto.FieldCipher
import javax.crypto.SecretKey
import javax.inject.Inject

/**
 * Seals and opens the entry-title snapshot stored in the audit log.
 *
 * Stored form: "enc:v1:" followed by the AES-GCM string produced by [FieldCipher].
 * Anything that is not in that form, or that cannot be decrypted with the given key,
 * is treated as unreadable and yields null. The codec never returns stored plaintext.
 */
class AuditTitleCodec
    @Inject
    constructor(
        private val cipher: FieldCipher,
    ) {
        fun seal(
            title: String?,
            key: SecretKey,
        ): String? {
            if (title.isNullOrBlank()) return null
            return PREFIX + cipher.encrypt(title, key)
        }

        fun open(
            stored: String?,
            key: SecretKey,
        ): String? {
            if (stored == null || !stored.startsWith(PREFIX)) return null
            return try {
                cipher.decrypt(stored.removePrefix(PREFIX), key)
            } catch (_: Exception) {
                null
            }
        }

        companion object {
            const val PREFIX = "enc:v1:"
        }
    }
