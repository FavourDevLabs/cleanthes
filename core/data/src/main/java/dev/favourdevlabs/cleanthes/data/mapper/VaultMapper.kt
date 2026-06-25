package dev.favourdevlabs.cleanthes.data.mapper

import dev.favourdevlabs.cleanthes.data.entities.VaultEntry
import dev.favourdevlabs.cleanthes.domain.model.VaultItem

/**
 * Maps between the Room entity (VaultEntry) and the domain model (VaultItem).
 * At this point both password and totpSecret are already decrypted —
 * VaultRepository handles crypto before calling these.
 */
internal fun VaultEntry.toDomain(): VaultItem = VaultItem(
    id            = id,
    title         = title,
    username      = username,
    password      = encryptedPassword,   // decrypted by repo before mapping
    website       = website,
    category      = category,
    notes         = notes,
    createdAt     = createdAt,
    updatedAt     = updatedAt,
    isFavorite    = isFavorite,
    totpSecret    = totpSecret,          // decrypted by repo before mapping
    totpIssuer    = totpIssuer,
    totpDigits    = totpDigits,
    totpPeriod    = totpPeriod,
    totpAlgorithm = totpAlgorithm,
)

internal fun VaultItem.toEntity(): VaultEntry = VaultEntry(
    id                = id,
    title             = title,
    username          = username,
    encryptedPassword = password,        // caller must encrypt before mapping back
    website           = website,
    category          = category,
    notes             = notes,
    createdAt         = createdAt,
    updatedAt         = updatedAt,
    isFavorite        = isFavorite,
    totpSecret        = totpSecret,      // caller must encrypt before mapping back
    totpIssuer        = totpIssuer,
    totpDigits        = totpDigits,
    totpPeriod        = totpPeriod,
    totpAlgorithm     = totpAlgorithm,
)
