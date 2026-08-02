package dev.favourdevlabs.cleanthes.data.impl.mapper

import dev.favourdevlabs.cleanthes.data.impl.entities.CitadelEntry
import dev.favourdevlabs.cleanthes.domain.model.CitadelItem

internal fun CitadelEntry.toDomain(): CitadelItem = CitadelItem(
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

internal fun CitadelItem.toEntity(): CitadelEntry = CitadelEntry(
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
