package dev.favourdevlabs.cleanthes.data.impl.usecase.vault

import dev.favourdevlabs.cleanthes.data.impl.db.VaultDatabaseSwitchboard
import dev.favourdevlabs.cleanthes.domain.model.VaultProfile
import dev.favourdevlabs.cleanthes.domain.usecase.ActivateVaultProfile
import javax.inject.Inject

class ActivateVaultProfileImpl @Inject constructor(
    private val switchboard: VaultDatabaseSwitchboard,
) : ActivateVaultProfile {
    override suspend fun invoke(profile: VaultProfile) {
        switchboard.activate(profile)
    }
}
