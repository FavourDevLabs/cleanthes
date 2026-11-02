package dev.favourdevlabs.cleanthes.data.impl.audit

import dev.favourdevlabs.cleanthes.data.api.AuditLogRepository
import dev.favourdevlabs.cleanthes.data.impl.usecase.GetAuditLogImpl
import dev.favourdevlabs.cleanthes.data.impl.usecase.RecordAuditEventImpl
import dev.favourdevlabs.cleanthes.domain.model.AuditLogItem
import dev.favourdevlabs.cleanthes.domain.usecase.RecordAuditEvent
import dev.favourdevlabs.cleanthes.security.session.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

class AuditUseCasesTest {
    private val key = SecretKeySpec(ByteArray(32) { 1 }, "AES")
    private val codec = AuditTitleCodec(FakeFieldCipher())

    private class FakeSession(var key: SecretKey?) : SessionManager {
        override val lockState: StateFlow<Boolean> = MutableStateFlow(key == null)

        override fun setSessionKey(key: SecretKey) {
            this.key = key
        }

        override suspend fun <T> withSessionKey(block: suspend (SecretKey) -> T): T? {
            val current = key ?: return null
            return block(current)
        }

        override fun clearSession() {
            key = null
        }

        override fun refreshSession() = Unit

        override fun notifyAppBackgrounded() = Unit

        override fun notifyAppForegrounded() = Unit

        override fun getLastActiveTimestamp(): Long = 0L
    }

    private class FakeRepository : AuditLogRepository {
        val stored = mutableListOf<AuditLogItem>()

        override suspend fun recordEvent(
            eventType: String,
            entryId: Long?,
            entryTitle: String?,
        ) {
            stored += AuditLogItem(id = stored.size + 1L, eventType = eventType, entryId = entryId, entryTitle = entryTitle)
        }

        override suspend fun getAllEvents(): List<AuditLogItem> = stored.toList()
    }

    @Test
    fun `record stores a sealed title when unlocked`() =
        runTest {
            val repo = FakeRepository()
            RecordAuditEventImpl(repo, FakeSession(key), codec)
                .invoke(RecordAuditEvent.EventType.ENTRY_VIEWED, 7L, "Gmail")
            val stored = repo.stored.single().entryTitle!!
            assertTrue(stored.startsWith(AuditTitleCodec.PREFIX))
            assertFalse(stored.contains("Gmail"))
        }

    @Test
    fun `record stores no title when locked`() =
        runTest {
            val repo = FakeRepository()
            RecordAuditEventImpl(repo, FakeSession(null), codec)
                .invoke(RecordAuditEvent.EventType.ENTRY_VIEWED, 7L, "Gmail")
            assertNull(repo.stored.single().entryTitle)
        }

    @Test
    fun `record keeps events that have no title`() =
        runTest {
            val repo = FakeRepository()
            RecordAuditEventImpl(repo, FakeSession(key), codec)
                .invoke(RecordAuditEvent.EventType.EXPORT, null, null)
            assertNull(repo.stored.single().entryTitle)
            assertEquals("EXPORT", repo.stored.single().eventType)
        }

    @Test
    fun `get decrypts titles when unlocked`() =
        runTest {
            val repo = FakeRepository()
            RecordAuditEventImpl(repo, FakeSession(key), codec)
                .invoke(RecordAuditEvent.EventType.ENTRY_EDITED, 3L, "Bank")
            val items = GetAuditLogImpl(repo, FakeSession(key), codec).invoke()
            assertEquals("Bank", items.single().entryTitle)
        }

    @Test
    fun `get returns null titles when locked`() =
        runTest {
            val repo = FakeRepository()
            RecordAuditEventImpl(repo, FakeSession(key), codec)
                .invoke(RecordAuditEvent.EventType.ENTRY_EDITED, 3L, "Bank")
            val items = GetAuditLogImpl(repo, FakeSession(null), codec).invoke()
            assertNull(items.single().entryTitle)
        }

    @Test
    fun `get returns null for a legacy plaintext row`() =
        runTest {
            val repo = FakeRepository()
            repo.recordEvent("ENTRY_VIEWED", 1L, "Plaintext Title")
            val items = GetAuditLogImpl(repo, FakeSession(key), codec).invoke()
            assertNull(items.single().entryTitle)
        }
}
