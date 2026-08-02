package dev.favourdevlabs.cleanthes.data.impl.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import dev.favourdevlabs.cleanthes.data.impl.entities.CitadelEntry

@Dao
interface CitadelDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entry: CitadelEntry): Long

    @Update
    suspend fun update(entry: CitadelEntry): Int

    @Transaction
    suspend fun updateAll(entries: List<CitadelEntry>) {
        entries.forEach { update(it) }
    }

    @Query("DELETE FROM citadel_entries WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    @Query("DELETE FROM citadel_entries")
    suspend fun deleteAll(): Int

    @Query("SELECT * FROM citadel_entries ORDER BY isFavorite DESC, title ASC")
    suspend fun getAllEntries(): List<CitadelEntry>

    @Query("SELECT * FROM citadel_entries WHERE id = :id LIMIT 1")
    suspend fun getEntryById(id: Long): CitadelEntry?

    @Query("""
        SELECT * FROM citadel_entries
        WHERE title LIKE '%' || :query || '%'
           OR username LIKE '%' || :query || '%'
        ORDER BY isFavorite DESC, title ASC
    """)
    suspend fun searchEntries(query: String): List<CitadelEntry>

    @Query("""
        SELECT * FROM citadel_entries
        WHERE category = :category
        ORDER BY isFavorite DESC, title ASC
    """)
    suspend fun getEntriesByCategory(category: String): List<CitadelEntry>

    @Query("""
        SELECT * FROM citadel_entries
        WHERE website LIKE '%' || :domain || '%'
           OR title LIKE '%' || :domain || '%'
        ORDER BY isFavorite DESC, title ASC
    """)
    suspend fun getEntriesByDomainCandidate(domain: String): List<CitadelEntry>

    @Query("SELECT * FROM citadel_entries WHERE isFavorite = 1 ORDER BY title ASC")
    suspend fun getFavoriteEntries(): List<CitadelEntry>

    @Query("SELECT COUNT(*) FROM citadel_entries")
    suspend fun getEntryCount(): Int

    @Query("SELECT DISTINCT category FROM citadel_entries ORDER BY category ASC")
    suspend fun getAllCategories(): List<String>
}
