package dev.favourdevlabs.cleanthes.data.impl.usecase.citadel

import dev.favourdevlabs.cleanthes.data.impl.db.CitadelDatabaseSwitchboard
import dev.favourdevlabs.cleanthes.domain.model.CitadelProfile
import dev.favourdevlabs.cleanthes.domain.usecase.GetActiveCitadelProfile
import javax.inject.Inject

class GetActiveCitadelProfileImpl @Inject constructor(
    private val switchboard: CitadelDatabaseSwitchboard,
) : GetActiveCitadelProfile {
    override suspend fun invoke(): CitadelProfile? = switchboard.currentProfile()
}
