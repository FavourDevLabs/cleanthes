package dev.favourdevlabs.cleanthes.security;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * RFC 6238 TOTP implementation. No external dependencies.
 * Zero Android imports — fully unit-testable on bare JVM.
 */
public final class TOTPGenerator {

	private static final String HMAC_SHA1 = "HmacSHA1";
	private static final String BASE32_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

	private TOTPGenerator() {
	}

	/** Generate a code using standard defaults: 6 digits, 30-second window. */
	public static String generate(String base32Secret) throws Exception {
		return generate(base32Secret, 6, 30);
	}

	/**
	 * Generate a TOTP code.
	 *
	 * @param base32Secret The Base32-encoded shared secret the service gave you
	 * @param digits       Code length — 6 for almost every service; Steam uses 5
	 * @param period       Time step in seconds — almost always 30
	 */
	public static String generate(String base32Secret, int digits, int period) throws Exception {
		byte[] key = decodeBase32(base32Secret);
		long timeStep = System.currentTimeMillis() / 1000L / period;
		return hotp(key, timeStep, digits);
	}

	/**
	 * Seconds remaining in the current TOTP window.
	 * Feed this into the countdown bar in your UI.
	 */
	public static int getSecondsRemaining(int period) {
		long epochSeconds = System.currentTimeMillis() / 1000L;
		return (int) (period - (epochSeconds % period));
	}

	// -----------------------------------------------------------------------
	// HOTP core — RFC 4226 §5
	// -----------------------------------------------------------------------

	private static String hotp(byte[] key, long counter, int digits) throws Exception {
		// Step 1: counter as 8-byte big-endian (network byte order)
		byte[] msg = new byte[8];
		long tmp = counter;
		for (int i = 7; i >= 0; i--) {
			msg[i] = (byte) (tmp & 0xFF);
			tmp >>= 8;
		}

		// Step 2: HMAC-SHA1(key, msg) → 20-byte hash
		Mac mac = Mac.getInstance(HMAC_SHA1);
		mac.init(new SecretKeySpec(key, HMAC_SHA1));
		byte[] hash = mac.doFinal(msg);

		// Step 3: dynamic truncation — offset = low nibble of last byte
		int offset = hash[19] & 0x0F;
		int p = ((hash[offset] & 0x7F) << 24)
				| ((hash[offset + 1] & 0xFF) << 16)
				| ((hash[offset + 2] & 0xFF) << 8)
				| (hash[offset + 3] & 0xFF);

		// Step 4: modulo + zero-pad to required length
		int otp = p % (int) Math.pow(10, digits);
		return String.format("%0" + digits + "d", otp);
	}

	// -----------------------------------------------------------------------
	// Base32 decoder — RFC 4648 §6
	// -----------------------------------------------------------------------

	/**
	 * Decodes a Base32 string to raw bytes.
	 * Handles lowercase, whitespace, and '=' padding gracefully.
	 * Package-private so the unit test can call it directly.
	 */
	static byte[] decodeBase32(String input) {
		String s = input.toUpperCase().replaceAll("[=\\s]", "");

		int outLen = (s.length() * 5) / 8;
		byte[] out = new byte[outLen];
		int buf = 0;
		int bitsLeft = 0;
		int idx = 0;

		for (int i = 0; i < s.length(); i++) {
			int v = BASE32_CHARS.indexOf(s.charAt(i));
			if (v < 0)
				throw new IllegalArgumentException(
						"Invalid Base32 character '" + s.charAt(i) + "' at position " + i);
			buf = (buf << 5) | v;
			bitsLeft += 5;
			if (bitsLeft >= 8) {
				bitsLeft -= 8;
				out[idx++] = (byte) (buf >> bitsLeft);
			}
		}
		return out;
	}
}
