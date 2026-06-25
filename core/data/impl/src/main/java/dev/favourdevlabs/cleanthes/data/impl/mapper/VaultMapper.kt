package dev.favourdevlabs.cleanthes.data.impl.mapper

import dev.favourdevlabs.cleanthes.data.impl.entities.VaultEntry
import dev.favourdevlabs.cleanthes.domain.model.VaultItem

internal fun VaultEntry.toDomain(): VaultItem = VaultItem(
    id            = id,
    title         = title,
    username      = username,
    password      = encryptedPassword,
    website       = website,
    category      = category,
    notes         = notes,
    createdAt     = createdAt,
    updatedAt     = updatedAt,
    isFavorite    = isFavorite,
    totpSecret    = totpSecret,
    totpIssuer    = totpIssuer,
    totpDigits    = totpDigits,
    totpPeriod    = totpPeriod,
    totpAlgorithm = totpAlgorithm,
)

internal fun VaultItem.toEntity(): VaultEntry = VaultEntry(
    id                = id,
    title             = title,
    username          = username,
    encryptedPassword = password,
    website           = website,
    category          = category,
    notes             = notes,
    createdAt         = createdAt,
    updatedAt         = updatedAt,
    isFavorite        = isFavorite,
    totpSecret        = totpSecret,
    totpIssuer        = totpIssuer,
    totpDigits        = totpDigits,
    totpPeriod        = totpPeriod,
    totpAlgorithm     = totpAlgorithm,
)

