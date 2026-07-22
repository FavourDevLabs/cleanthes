package dev.favourdevlabs.cleanthes.data.impl.prefs

import dev.favourdevlabs.cleanthes.domain.model.VaultProfile

internal fun prefsName(profile: VaultProfile): String =
    when (profile) {
        VaultProfile.REAL -> "vault_secure_prefs_real"
        VaultProfile.DECOY -> "vault_secure_prefs_decoy"
    }

internal const val KEY_VAULT_EXISTS                = "vault_exists"
internal const val KEY_AUTH_SALT                   = "auth_salt"
internal const val KEY_ENC_SALT                    = "enc_salt"
internal const val KEY_MASTER_HASH                 = "master_hash"
internal const val KEY_BIOMETRIC_ENABLED           = "biometric_enabled"
internal const val KEY_WRAPPED_VAULT_KEY_PASSWORD  = "wrapped_vault_key_password"
internal const val KEY_WRAPPED_VAULT_KEY_BIOMETRIC = "wrapped_vault_key_biometric"
internal const val KEY_BIOMETRIC_IV                = "biometric_iv"
