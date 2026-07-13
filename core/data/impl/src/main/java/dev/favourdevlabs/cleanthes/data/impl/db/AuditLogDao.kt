package dev.favourdevlabs.cleanthes.data.impl.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.favourdevlabs.cleanthes.data.impl.entities.AuditLogEntry

@Dao
interface AuditLogDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entry: AuditLogEntry): Long

    @Query("SELECT * FROM audit_log ORDER BY timestamp DESC")
    suspend fun getAllEntries(): List<AuditLogEntry>

    @Query("SELECT * FROM audit_log ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentEntries(limit: Int): List<AuditLogEntry>

    @Query("DELETE FROM audit_log WHERE timestamp < :cutoffTimestamp")
    suspend fun purgeOlderThan(cutoffTimestamp: Long): Int

    @Query("DELETE FROM audit_log")
    suspend fun deleteAll(): Int
}
