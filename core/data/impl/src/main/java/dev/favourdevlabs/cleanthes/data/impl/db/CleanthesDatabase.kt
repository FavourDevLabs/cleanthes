package dev.favourdevlabs.cleanthes.data.impl.db

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import dev.favourdevlabs.cleanthes.data.impl.entities.VaultEntry
import dev.favourdevlabs.cleanthes.data.impl.entities.AuditLogEntry

@Database(
    entities = [VaultEntry::class, AuditLogEntry::class],
    version = 4,
    exportSchema = true
)
abstract class CleanthesDatabase : RoomDatabase() {

    abstract fun vaultDao(): VaultDao
    abstract fun auditLogDao(): AuditLogDao

    companion object {

        @Volatile
        private var INSTANCE: CleanthesDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE vault_entries ADD COLUMN totpSecret TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE vault_entries ADD COLUMN totpIssuer TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE vault_entries ADD COLUMN totpDigits INTEGER NOT NULL DEFAULT 6")
                db.execSQL("ALTER TABLE vault_entries ADD COLUMN totpPeriod INTEGER NOT NULL DEFAULT 30")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE vault_entries ADD COLUMN totpAlgorithm TEXT NOT NULL DEFAULT 'SHA1'")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS audit_log (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "eventType TEXT NOT NULL, " +
                    "entryId INTEGER, " +
                    "entryTitle TEXT, " +
                    "timestamp INTEGER NOT NULL)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_audit_log_timestamp ON audit_log(timestamp)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_audit_log_eventType ON audit_log(eventType)")
            }
        }

        fun getInstance(context: Context): CleanthesDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    CleanthesDatabase::class.java,
                    "cleanthes.db"
                )
                .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .build()
                .also { INSTANCE = it }
            }
        }
    }
}

