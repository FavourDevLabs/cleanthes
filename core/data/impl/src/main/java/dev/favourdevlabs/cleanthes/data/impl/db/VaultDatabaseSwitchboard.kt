package dev.favourdevlabs.cleanthes.data.impl.db

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.favourdevlabs.cleanthes.domain.model.VaultProfile
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns per-profile CleanthesDatabase instances and tracks which profile
 * the current session is bound to. Replaces the old static-singleton
 * CleanthesDatabase.getInstance() — the correct DB file can only be
 * known after password verification selects a profile, not at DI
 * graph construction time.
 *
 * Both profiles' DBs are opened lazily and independently; opening one
 * never touches the other's file.
 */
@Singleton
class VaultDatabaseSwitchboard
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val filenameProvider: VaultFilenameProvider,
    ) {
        private val instances = mutableMapOf<VaultProfile, CleanthesDatabase>()

        @Volatile
        private var activeProfile: VaultProfile? = null

        @Synchronized
        private fun getOrCreate(profile: VaultProfile): CleanthesDatabase =
            instances.getOrPut(profile) {
                Room.databaseBuilder(
                    context.applicationContext,
                    CleanthesDatabase::class.java,
                    filenameProvider.dbFileName(profile),
                )
                    .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
                    .addMigrations(
                        CleanthesDatabase.MIGRATION_1_2,
                        CleanthesDatabase.MIGRATION_2_3,
                        CleanthesDatabase.MIGRATION_3_4,
                    )
                    .build()
            }

        /** Called once, right after password verification selects a profile. */
        fun activate(profile: VaultProfile) {
            activeProfile = profile
        }

        /** Called on lock/logout — clears which profile subsequent DAO calls resolve to. */
        fun deactivate() {
            activeProfile = null
        }

        fun currentProfile(): VaultProfile? = activeProfile

        fun vaultDao(): VaultDao =
            getOrCreate(activeProfile ?: error("No vault profile active — session is locked"))
                .vaultDao()

        fun auditLogDao(): AuditLogDao =
            getOrCreate(activeProfile ?: error("No vault profile active — session is locked"))
                .auditLogDao()

        /**
         * Deletes a profile's database file entirely, closing the open
         * connection first if any. Distinct from vaultDao().deleteAll() —
         * this removes the file from disk, not just its rows.
         */
        @Synchronized
        fun destroy(profile: VaultProfile) {
            instances.remove(profile)?.close()
            context.applicationContext.deleteDatabase(filenameProvider.dbFileName(profile))
        }
    }

