package dev.favourdevlabs.cleanthes.data.impl.usecase

import android.util.Base64
import dev.favourdevlabs.cleanthes.data.impl.export.VaultExportSerializer
import dev.favourdevlabs.cleanthes.domain.usecase.ExportVault
import dev.favourdevlabs.cleanthes.domain.usecase.GetVaultEntries
import dev.favourdevlabs.cleanthes.security.CryptoManager
import dev.favourdevlabs.cleanthes.security.KeyDerivation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import javax.crypto.SecretKey
import javax.inject.Inject

class ExportVaultImpl
    @Inject
    constructor(
        private val getVaultEntries: GetVaultEntries,
    ) : ExportVault {
        override suspend fun invoke(
            exportPassword: String,
            key: SecretKey,
        ): String =
            withContext(Dispatchers.IO) {
                val entries = getVaultEntries(key).entries
                val plaintextJson = VaultExportSerializer.serialize(entries)

                val salt = KeyDerivation.generateSalt()
                val exportKey = KeyDerivation.deriveKey(exportPassword.toCharArray(), salt)
                val ciphertext = CryptoManager.encrypt(plaintextJson, exportKey)

                JSONObject()
                    .apply {
                        put("salt", Base64.encodeToString(salt, Base64.NO_WRAP))
                        put("ciphertext", ciphertext)
                    }.toString()
            }
    }
