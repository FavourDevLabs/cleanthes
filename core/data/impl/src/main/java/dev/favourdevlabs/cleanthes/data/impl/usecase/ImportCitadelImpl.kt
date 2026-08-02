package dev.favourdevlabs.cleanthes.data.impl.usecase

import android.util.Base64
import dev.favourdevlabs.cleanthes.data.impl.export.CitadelExportSerializer
import dev.favourdevlabs.cleanthes.domain.usecase.GetCitadelEntries
import dev.favourdevlabs.cleanthes.domain.usecase.ImportCitadel
import dev.favourdevlabs.cleanthes.domain.usecase.SaveCitadelEntry
import dev.favourdevlabs.cleanthes.security.CryptoManager
import dev.favourdevlabs.cleanthes.security.KeyDerivation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import javax.crypto.SecretKey
import javax.inject.Inject

class ImportCitadelImpl
    @Inject
    constructor(
        private val getCitadelEntries: GetCitadelEntries,
        private val saveCitadelEntry: SaveCitadelEntry,
    ) : ImportCitadel {
        override suspend fun invoke(
            encryptedBlob: String,
            exportPassword: String,
            key: SecretKey,
        ): ImportCitadel.Result =
            withContext(Dispatchers.IO) {
                val envelope = JSONObject(encryptedBlob)
                val salt = Base64.decode(envelope.getString("salt"), Base64.NO_WRAP)
                val ciphertext = envelope.getString("ciphertext")

                val exportKey = KeyDerivation.deriveKey(exportPassword.toCharArray(), salt)
                val plaintextJson = CryptoManager.decrypt(ciphertext, exportKey)
                val importedItems = CitadelExportSerializer.deserialize(plaintextJson)

                val existing = getCitadelEntries(key).entries
                val existingKeys = existing.map { it.title.lowercase() to it.username.lowercase() }.toSet()

                var imported = 0
                var skipped = 0
                importedItems.forEach { item ->
                    val itemKey = item.title.lowercase() to item.username.lowercase()
                    if (itemKey in existingKeys) {
                        skipped++
                    } else {
                        saveCitadelEntry(
                            SaveCitadelEntry.Params.New(
                                title = item.title,
                                username = item.username,
                                plainPassword = item.password,
                                website = item.website,
                                category = item.category,
                                notes = item.notes,
                                isFavorite = item.isFavorite,
                                totpSecret = item.totpSecret,
                                totpIssuer = item.totpIssuer,
                                totpDigits = item.totpDigits,
                                totpPeriod = item.totpPeriod,
                                totpAlgorithm = item.totpAlgorithm,
                                key = key,
                            ),
                        )
                        imported++
                    }
                }

                ImportCitadel.Result(imported = imported, skipped = skipped)
            }
    }
