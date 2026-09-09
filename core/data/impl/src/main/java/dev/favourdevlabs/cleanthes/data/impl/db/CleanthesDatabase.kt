package dev.favourdevlabs.cleanthes.data.impl.db

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import dev.favourdevlabs.cleanthes.data.impl.entities.CitadelEntry
import dev.favourdevlabs.cleanthes.data.impl.entities.AuditLogEntry
import dev.favourdevlabs.cleanthes.data.impl.entities.CitadelEntryHistory

@Database(
    entities = [CitadelEntry::class, AuditLogEntry::class, CitadelEntryHistory::class],
    version = 5,
    exportSchema = true
)
abstract class CleanthesDatabase : RoomDatabase() {

    abstract fun citadelDao(): CitadelDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun citadelEntryHistoryDao(): CitadelEntryHistoryDao

    companion object {

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE citadel_entries ADD COLUMN totpSecret TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE citadel_entries ADD COLUMN totpIssuer TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE citadel_entries ADD COLUMN totpDigits INTEGER NOT NULL DEFAULT 6")
                db.execSQL("ALTER TABLE citadel_entries ADD COLUMN totpPeriod INTEGER NOT NULL DEFAULT 30")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE citadel_entries ADD COLUMN totpAlgorithm TEXT NOT NULL DEFAULT 'SHA1'")
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

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS citadel_entry_history (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "entryId INTEGER NOT NULL, " +
                    "title TEXT NOT NULL, " +
                    "username TEXT NOT NULL, " +
                    "encryptedPassword TEXT NOT NULL, " +
                    "website TEXT, " +
                    "notes TEXT, " +
                    "totpSecret TEXT, " +
                    "timestamp INTEGER NOT NULL, " +
                    "FOREIGN KEY(entryId) REFERENCES citadel_entries(id) ON DELETE CASCADE)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_citadel_entry_history_entryId ON citadel_entry_history(entryId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_citadel_entry_history_timestamp ON citadel_entry_history(timestamp)")
            }
        }

    }

  }
