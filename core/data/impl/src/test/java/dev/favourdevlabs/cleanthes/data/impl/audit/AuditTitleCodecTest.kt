package dev.favourdevlabs.cleanthes.data.impl.audit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import javax.crypto.spec.SecretKeySpec

class AuditTitleCodecTest {
    private val keyA = SecretKeySpec(ByteArray(32) { 1 }, "AES")
    private val keyB = SecretKeySpec(ByteArray(32) { 2 }, "AES")
    private val codec = AuditTitleCodec(FakeFieldCipher())

    @Test
    fun `seal returns null for null or blank titles`() {
        assertNull(codec.seal(null, keyA))
        assertNull(codec.seal("   ", keyA))
    }

    @Test
    fun `seal marks the value and does not contain the plaintext`() {
        val sealed = codec.seal("Gmail", keyA)!!
        assertTrue(sealed.startsWith(AuditTitleCodec.PREFIX))
        assertFalse(sealed.contains("Gmail"))
    }

    @Test
    fun `open round-trips with the same key`() {
        assertEquals("Gmail", codec.open(codec.seal("Gmail", keyA), keyA))
    }

    @Test
    fun `open returns null with the wrong key`() {
        assertNull(codec.open(codec.seal("Gmail", keyA), keyB))
    }

    @Test
    fun `open never returns legacy plaintext`() {
        assertNull(codec.open("Gmail", keyA))
    }

    @Test
    fun `open returns null for null and for a tampered value`() {
        assertNull(codec.open(null, keyA))
        assertNull(codec.open(AuditTitleCodec.PREFIX + "garbage", keyA))
    }
}
