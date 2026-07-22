package dev.favourdevlabs.cleanthes.domain.model

/**
 * Identifies which vault credential set / database file a session is
 * bound to. REAL and DECOY are structurally identical — same schema,
 * same DAOs, same crypto path. Nothing downstream of authentication
 * may branch on which profile is active; doing so would create a
 * behavioral signal an attacker could detect.
 */
enum class VaultProfile(val dbFileName: String) {
    REAL("cleanthes_real.db"),
    DECOY("cleanthes_decoy.db"),
}
