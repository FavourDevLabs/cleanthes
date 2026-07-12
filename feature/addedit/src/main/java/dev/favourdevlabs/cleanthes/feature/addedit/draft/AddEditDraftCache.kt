package dev.favourdevlabs.cleanthes.feature.addedit.draft

import dev.favourdevlabs.cleanthes.feature.addedit.AddEditUiState
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds one in-flight, unsaved AddEdit form state across a lock-triggered
 * redirect to Login. Keyed by entryId so a draft for entry #7 never leaks
 * into a fresh "new entry" screen or a different entry. Consumed once —
 * a stale draft must not silently reappear on an unrelated future visit.
 */
@Singleton
class AddEditDraftCache
    @Inject
    constructor() {
        @Volatile private var entryId: Long? = null

        @Volatile private var draft: AddEditUiState? = null

        fun save(
            entryId: Long,
            state: AddEditUiState,
        ) {
            this.entryId = entryId
            this.draft = state
        }

        fun consume(entryId: Long): AddEditUiState? {
            if (this.entryId != entryId) return null
            val value = draft
            this.entryId = null
            draft = null
            return value
        }
    }
