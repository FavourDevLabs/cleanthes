package dev.favourdevlabs.cleanthes.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import dev.favourdevlabs.cleanthes.data.entities.VaultEntry

@Dao
interface VaultDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entry: VaultEntry): Long

    @Update
    suspend fun update(entry: VaultEntry): Int

    @Query("DELETE FROM vault_entries WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    @Query("DELETE FROM vault_entries")
    suspend fun deleteAll(): Int

    @Query("SELECT * FROM vault_entries ORDER BY isFavorite DESC, title ASC")
    suspend fun getAllEntries(): List<VaultEntry>

    @Query("SELECT * FROM vault_entries WHERE id = :id LIMIT 1")
    suspend fun getEntryById(id: Long): VaultEntry?

    @Query("""
        SELECT * FROM vault_entries
        WHERE title LIKE '%' || :query || '%'
           OR username LIKE '%' || :query || '%'
        ORDER BY isFavorite DESC, title ASC
    """)
    suspend fun searchEntries(query: String): List<VaultEntry>

    @Query("""
        SELECT * FROM vault_entries
        WHERE category = :category
        ORDER BY isFavorite DESC, title ASC
    """)
    suspend fun getEntriesByCategory(category: String): List<VaultEntry>

    @Query("SELECT * FROM vault_entries WHERE isFavorite = 1 ORDER BY title ASC")
    suspend fun getFavoriteEntries(): List<VaultEntry>

    @Query("SELECT COUNT(*) FROM vault_entries")
    suspend fun getEntryCount(): Int

    @Query("SELECT DISTINCT category FROM vault_entries ORDER BY category ASC")
    suspend fun getAllCategories(): List<String>
}

