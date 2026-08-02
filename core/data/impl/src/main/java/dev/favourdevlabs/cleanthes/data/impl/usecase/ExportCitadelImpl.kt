package dev.favourdevlabs.cleanthes.data.impl.usecase

import android.util.Base64
import dev.favourdevlabs.cleanthes.data.impl.export.CitadelExportSerializer
import dev.favourdevlabs.cleanthes.domain.usecase.ExportCitadel
import dev.favourdevlabs.cleanthes.domain.usecase.GetCitadelEntries
import dev.favourdevlabs.cleanthes.security.CryptoManager
import dev.favourdevlabs.cleanthes.security.KeyDerivation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import javax.crypto.SecretKey
import javax.inject.Inject

class ExportCitadelImpl
    @Inject
    constructor(
        private val getCitadelEntries: GetCitadelEntries,
    ) : ExportCitadel {
        override suspend fun invoke(
            exportPassword: String,
            key: SecretKey,
        ): String =
            withContext(Dispatchers.IO) {
                val entries = getCitadelEntries(key).entries
                val plaintextJson = CitadelExportSerializer.serialize(entries)

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
