package dev.favourdevlabs.cleanthes.data.impl.db

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.favourdevlabs.cleanthes.domain.model.CitadelProfile
import java.security.MessageDigest
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

private const val SALT_STORE_NAME = "app_config"
private const val SALT_KEY = "install_id"
private const val SALT_LENGTH_BYTES = 32

/**
 * Derives DB and prefs filenames that reveal nothing about which
 * CitadelProfile they belong to — no real/decoy suffix, no
 * predictable naming. Both profiles' filenames are equally opaque, so
 * an inspector with raw filesystem access (not just app UI access)
 * cannot tell which of two files is the decoy, or that either is
 * decoy-related at all.
 *
 * Filenames are derived from a random salt generated once per install
 * — NOT from the profile name alone — so they can't be precomputed or
 * recognized across installs either.
 */
@Singleton
class CitadelFilenameProvider
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private val salt: ByteArray by lazy { loadOrCreateSalt() }

        fun dbFileName(profile: CitadelProfile): String = "cleanthes_${derive(profile, "db")}.db"

        fun prefsFileName(profile: CitadelProfile): String = "cleanthes_${derive(profile, "prefs")}"

        private fun derive(profile: CitadelProfile, tag: String): String {
            val digest = MessageDigest.getInstance("SHA-256")
            digest.update(salt)
            digest.update(profile.name.toByteArray(Charsets.UTF_8))
            digest.update(tag.toByteArray(Charsets.UTF_8))
            val hash = digest.digest()
            return hash.take(8).joinToString("") { "%02x".format(it) }
        }

        private fun loadOrCreateSalt(): ByteArray {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            val prefs = EncryptedSharedPreferences.create(
                context,
                SALT_STORE_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )

            val existing = prefs.getString(SALT_KEY, null)
            if (existing != null) return Base64.decode(existing, Base64.NO_WRAP)

            val newSalt = ByteArray(SALT_LENGTH_BYTES).also { SecureRandom().nextBytes(it) }
            prefs.edit()
                .putString(SALT_KEY, Base64.encodeToString(newSalt, Base64.NO_WRAP))
                .apply()
            return newSalt
        }
    }
