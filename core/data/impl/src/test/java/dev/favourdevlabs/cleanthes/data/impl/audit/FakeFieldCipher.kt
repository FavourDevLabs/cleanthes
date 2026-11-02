package dev.favourdevlabs.cleanthes.data.impl.audit

import dev.favourdevlabs.cleanthes.data.impl.crypto.FieldCipher
import javax.crypto.SecretKey

/** Not real encryption: it only proves the right key is used in the right place. */
internal class FakeFieldCipher : FieldCipher() {
    override fun encrypt(
        plaintext: String,
        key: SecretKey,
    ): String = "${key.encoded[0]}|${plaintext.reversed()}"

    override fun decrypt(
        encrypted: String,
        key: SecretKey,
    ): String {
        val parts = encrypted.split("|", limit = 2)
        require(parts.size == 2 && parts[0] == key.encoded[0].toString()) { "wrong key or malformed" }
        return parts[1].reversed()
    }
}
