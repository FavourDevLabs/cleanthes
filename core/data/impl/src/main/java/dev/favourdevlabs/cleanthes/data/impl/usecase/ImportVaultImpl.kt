package dev.favourdevlabs.cleanthes.data.impl.usecase

import android.util.Base64
import dev.favourdevlabs.cleanthes.data.impl.export.VaultExportSerializer
import dev.favourdevlabs.cleanthes.domain.usecase.GetVaultEntries
import dev.favourdevlabs.cleanthes.domain.usecase.ImportVault
import dev.favourdevlabs.cleanthes.domain.usecase.SaveVaultEntry
import dev.favourdevlabs.cleanthes.security.CryptoManager
import dev.favourdevlabs.cleanthes.security.KeyDerivation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import javax.crypto.SecretKey
import javax.inject.Inject

class ImportVaultImpl
    @Inject
    constructor(
        private val getVaultEntries: GetVaultEntries,
        private val saveVaultEntry: SaveVaultEntry,
    ) : ImportVault {
        override suspend fun invoke(
            encryptedBlob: String,
            exportPassword: String,
            key: SecretKey,
        ): ImportVault.Result =
            withContext(Dispatchers.IO) {
                val envelope = JSONObject(encryptedBlob)
                val salt = Base64.decode(envelope.getString("salt"), Base64.NO_WRAP)
                val ciphertext = envelope.getString("ciphertext")

                val exportKey = KeyDerivation.deriveKey(exportPassword.toCharArray(), salt)
                val plaintextJson = CryptoManager.decrypt(ciphertext, exportKey)
                val importedItems = VaultExportSerializer.deserialize(plaintextJson)

                val existing = getVaultEntries(key).entries
                val existingKeys = existing.map { it.title.lowercase() to it.username.lowercase() }.toSet()

                var imported = 0
                var skipped = 0
                importedItems.forEach { item ->
                    val itemKey = item.title.lowercase() to item.username.lowercase()
                    if (itemKey in existingKeys) {
                        skipped++
                    } else {
                        saveVaultEntry(
                            SaveVaultEntry.Params.New(
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

                ImportVault.Result(imported = imported, skipped = skipped)
            }
    }
