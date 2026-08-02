package dev.favourdevlabs.cleanthes.domain.model

/**
 * Identifies which credential set / database file a session is bound
 * to. REAL and DECOY are structurally identical — same schema, same
 * DAOs, same crypto path. Nothing downstream of authentication may
 * branch on which profile is active; doing so would create a
 * behavioral signal an attacker could detect.
 *
 * Deliberately holds no filename — actual DB/prefs filenames are
 * derived per-install (see CitadelFilenameProvider) so that neither
 * profile's on-disk name reveals which one is the decoy to anyone
 * with raw filesystem access.
 */
enum class CitadelProfile {
    REAL,
    DECOY,
}
