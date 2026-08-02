package dev.favourdevlabs.cleanthes.data.impl.usecase.citadel

import dev.favourdevlabs.cleanthes.data.impl.db.CitadelDatabaseSwitchboard
import dev.favourdevlabs.cleanthes.domain.model.CitadelProfile
import dev.favourdevlabs.cleanthes.domain.usecase.ActivateCitadelProfile
import javax.inject.Inject

class ActivateCitadelProfileImpl @Inject constructor(
    private val switchboard: CitadelDatabaseSwitchboard,
) : ActivateCitadelProfile {
    override suspend fun invoke(profile: CitadelProfile) {
        switchboard.activate(profile)
    }
}
