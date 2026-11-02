package dev.favourdevlabs.cleanthes.data.impl.audit

import dev.favourdevlabs.cleanthes.data.impl.db.AuditLogDao
import dev.favourdevlabs.cleanthes.data.impl.db.CitadelEntryHistoryDao
import dev.favourdevlabs.cleanthes.data.impl.entities.AuditLogEntry
import dev.favourdevlabs.cleanthes.data.impl.entities.CitadelEntryHistory
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import javax.crypto.spec.SecretKeySpec

class AuditAndHistoryReencryptorTest {
    private val oldKey = SecretKeySpec(ByteArray(32) { 1 }, "AES")
    private val newKey = SecretKeySpec(ByteArray(32) { 2 }, "AES")
    private val cipher = FakeFieldCipher()
    private val codec = AuditTitleCodec(cipher)

    private class FakeHistoryDao(val rows: MutableList<CitadelEntryHistory>) : CitadelEntryHistoryDao {
        override suspend fun insert(history: CitadelEntryHistory): Long = error("unused")

        override suspend fun getHistoryForEntry(entryId: Long): List<CitadelEntryHistory> = error("unused")

        override suspend fun getHistoryById(id: Long): CitadelEntryHistory? = error("unused")

        override suspend fun deleteHistoryForEntry(entryId: Long): Int = error("unused")

        override suspend fun getAll(): List<CitadelEntryHistory> = rows.toList()

        override suspend fun updateAll(items: List<CitadelEntryHistory>) {
            items.forEach { item ->
                val index = rows.indexOfFirst { it.id == item.id }
                if (index >= 0) rows[index] = item
            }
        }

        override suspend fun deleteByIds(ids: List<Long>): Int {
            val before = rows.size
            rows.removeAll { it.id in ids }
            return before - rows.size
        }
    }

    private class FakeAuditDao(val rows: MutableList<AuditLogEntry>) : AuditLogDao {
        override suspend fun insert(entry: AuditLogEntry): Long = error("unused")

        override suspend fun getAllEntries(): List<AuditLogEntry> = rows.toList()

        override suspend fun getRecentEntries(limit: Int): List<AuditLogEntry> = error("unused")

        override suspend fun purgeOlderThan(cutoffTimestamp: Long): Int = error("unused")

        override suspend fun deleteAll(): Int = error("unused")

        override suspend fun updateTitle(
            id: Long,
            title: String?,
        ): Int {
            val index = rows.indexOfFirst { it.id == id }
            if (index >= 0) rows[index] = rows[index].copy(entryTitle = title)
            return if (index >= 0) 1 else 0
        }
    }

    private fun historyRow(
        id: Long,
        key: SecretKeySpec,
        website: String? = null,
    ) = CitadelEntryHistory(
        id = id,
        entryId = 1,
        title = cipher.encrypt("title$id", key),
        username = cipher.encrypt("user$id", key),
        encryptedPassword = cipher.encrypt("pw$id", key),
        website = website?.let { cipher.encrypt(it, key) },
        notes = null,
        totpSecret = "",
        timestamp = id,
    )

    private fun reencryptor(
        history: MutableList<CitadelEntryHistory>,
        audit: MutableList<AuditLogEntry>,
    ) = AuditAndHistoryReencryptor({ FakeHistoryDao(history) }, { FakeAuditDao(audit) }, cipher, codec)

    @Test
    fun `history fields move to the new key and null or empty values are preserved`() =
        runTest {
            val history = mutableListOf(historyRow(1, oldKey, website = "example.com"))
            reencryptor(history, mutableListOf()).reencrypt(oldKey, newKey)
            val row = history.single()
            assertEquals("title1", cipher.decrypt(row.title, newKey))
            assertEquals("user1", cipher.decrypt(row.username, newKey))
            assertEquals("pw1", cipher.decrypt(row.encryptedPassword, newKey))
            assertEquals("example.com", cipher.decrypt(row.website!!, newKey))
            assertNull(row.notes)
            assertEquals("", row.totpSecret)
        }

    @Test
    fun `an unreadable history row is deleted and readable rows survive`() =
        runTest {
            val stranger = SecretKeySpec(ByteArray(32) { 9 }, "AES")
            val history = mutableListOf(historyRow(1, oldKey), historyRow(2, stranger), historyRow(3, oldKey))
            reencryptor(history, mutableListOf()).reencrypt(oldKey, newKey)
            assertEquals(listOf(1L, 3L), history.map { it.id })
            assertEquals("pw3", cipher.decrypt(history.last().encryptedPassword, newKey))
        }

    @Test
    fun `audit titles move to the new key`() =
        runTest {
            val audit =
                mutableListOf(AuditLogEntry(id = 1, eventType = "ENTRY_VIEWED", entryId = 5, entryTitle = codec.seal("Gmail", oldKey)))
            reencryptor(mutableListOf(), audit).reencrypt(oldKey, newKey)
            assertEquals("Gmail", codec.open(audit.single().entryTitle, newKey))
            assertNull(codec.open(audit.single().entryTitle, oldKey))
        }

    @Test
    fun `audit rows without a title are left alone and legacy plaintext is cleared`() =
        runTest {
            val audit =
                mutableListOf(
                    AuditLogEntry(id = 1, eventType = "EXPORT", entryTitle = null),
                    AuditLogEntry(id = 2, eventType = "ENTRY_VIEWED", entryTitle = "Plaintext"),
                )
            reencryptor(mutableListOf(), audit).reencrypt(oldKey, newKey)
            assertNull(audit[0].entryTitle)
            assertNull(audit[1].entryTitle)
        }
}
