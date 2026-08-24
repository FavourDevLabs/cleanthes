package dev.favourdevlabs.cleanthes.attestation.impl

/**
 * Minimal DER reader — only what's needed to walk Google's KeyDescription
 * extension and the nested AuthorizationList/RootOfTrust structures inside it.
 * Not a general ASN.1 parser.
 */
internal class DerReader(private val data: ByteArray) {
    var pos = 0
        private set

    fun hasRemaining(limit: Int = data.size) = pos < limit

    /** Tag class 0 = universal, 2 = context-specific (the only ones we see here). */
    private fun readGenericTag(): Int {
        val first = data[pos++].toInt() and 0xFF
        var number = first and 0x1F
        if (number == 0x1F) {
            // high-tag-number form: base-128, continuation bit on all but the last byte
            number = 0
            while (true) {
                val b = data[pos++].toInt() and 0xFF
                number = (number shl 7) or (b and 0x7F)
                if (b and 0x80 == 0) break
            }
        }
        return number
    }

    private fun readLength(): Int {
        val first = data[pos++].toInt() and 0xFF
        if (first < 0x80) return first
        val numBytes = first and 0x7F
        var length = 0
        repeat(numBytes) { length = (length shl 8) or (data[pos++].toInt() and 0xFF) }
        return length
    }

    /** Reads tag+length, returns the value's end offset (exclusive). Caller reads [pos, end). */
    fun enterTlv(): Int {
        readGenericTag()
        val len = readLength()
        return pos + len
    }

    /** Same as enterTlv, but also returns the tag number — needed to identify which
     *  AuthorizationList field (e.g. ROOT_OF_TRUST = 704) we're looking at. */
    fun enterTlvWithTagNumber(): Pair<Int, Int> {
        val number = readGenericTag()
        val len = readLength()
        return number to (pos + len)
    }

    fun readInteger(end: Int): Long {
        var value = 0L
        while (pos < end) value = (value shl 8) or (data[pos++].toLong() and 0xFF)
        return value
    }

    fun readEnumerated(end: Int): Int = readInteger(end).toInt()

    fun readOctetString(end: Int): ByteArray {
        val bytes = data.copyOfRange(pos, end)
        pos = end
        return bytes
    }

    fun skip(end: Int) { pos = end }
}
