package dev.favourdevlabs.cleanthes.data.impl.usecase.vault

import dev.favourdevlabs.cleanthes.data.impl.db.VaultDatabaseSwitchboard
import dev.favourdevlabs.cleanthes.domain.model.VaultProfile
import dev.favourdevlabs.cleanthes.domain.usecase.GetActiveVaultProfile
import javax.inject.Inject

class GetActiveVaultProfileImpl @Inject constructor(
    private val switchboard: VaultDatabaseSwitchboard,
) : GetActiveVaultProfile {
    override suspend fun invoke(): VaultProfile? = switchboard.currentProfile()
}
