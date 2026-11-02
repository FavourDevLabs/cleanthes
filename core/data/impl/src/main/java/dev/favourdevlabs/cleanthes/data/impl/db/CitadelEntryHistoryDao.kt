package dev.favourdevlabs.cleanthes.data.impl.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import dev.favourdevlabs.cleanthes.data.impl.entities.CitadelEntryHistory

@Dao
interface CitadelEntryHistoryDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(history: CitadelEntryHistory): Long

    @Query("SELECT * FROM citadel_entry_history WHERE entryId = :entryId ORDER BY timestamp DESC")
    suspend fun getHistoryForEntry(entryId: Long): List<CitadelEntryHistory>

    @Query("SELECT * FROM citadel_entry_history WHERE id = :id LIMIT 1")
    suspend fun getHistoryById(id: Long): CitadelEntryHistory?

    @Query("DELETE FROM citadel_entry_history WHERE entryId = :entryId")
    suspend fun deleteHistoryForEntry(entryId: Long): Int

    @Query("SELECT * FROM citadel_entry_history")
    suspend fun getAll(): List<CitadelEntryHistory>

    @Update
    suspend fun updateAll(items: List<CitadelEntryHistory>)

    @Query("DELETE FROM citadel_entry_history WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>): Int
}
