package dev.favourdevlabs.cleanthes.attestation.impl

/**
 * Minimal DER reader — only what's needed to walk Google's KeyDescription
 * extension. Not a general ASN.1 parser: no support for indefinite-length
 * encoding, no OID decoding beyond skip, no context-specific tag interpretation
 * beyond what attestation actually uses.
 */
internal class DerReader(private val data: ByteArray) {
    var pos = 0
        private set

    fun hasRemaining(limit: Int = data.size) = pos < limit

    fun readTag(): Int = data[pos++].toInt() and 0xFF

    fun readLength(): Int {
        val first = data[pos++].toInt() and 0xFF
        if (first < 0x80) return first
        val numBytes = first and 0x7F
        var length = 0
        repeat(numBytes) { length = (length shl 8) or (data[pos++].toInt() and 0xFF) }
        return length
    }

    /** Reads tag+length, returns the value's end offset (exclusive). Caller reads [pos, end). */
    fun enterTlv(): Int {
        readTag()
        val len = readLength()
        return pos + len
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
