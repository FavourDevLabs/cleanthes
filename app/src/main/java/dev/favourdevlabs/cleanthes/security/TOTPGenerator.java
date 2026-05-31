package dev.favourdevlabs.cleanthes.security;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * RFC 6238 TOTP — supports SHA1, SHA256, SHA512.
 * No Android imports. Fully unit-testable on bare JVM.
 */
public final class TOTPGenerator {

	private static final String BASE32_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

	private TOTPGenerator() {
	}

	// --- Public API ----------------------------------------------------------

	/** Defaults: 6 digits, 30s window, SHA1. Covers ~99% of real-world services. */
	public static String generate(String base32Secret) throws Exception {
		return generate(base32Secret, 6, 30, "SHA1");
	}

	/** Custom digits and period, SHA1 algorithm. */
	public static String generate(String base32Secret, int digits, int period) throws Exception {
		return generate(base32Secret, digits, period, "SHA1");
	}

	/**
	 * Full control. Algorithm is the value from the otpauth:// URI — "SHA1",
	 * "SHA256", or "SHA512". Any unrecognised value falls back to SHA1.
	 */
	public static String generate(String base32Secret, int digits,
			int period, String algorithm) throws Exception {
		byte[] key = decodeBase32(base32Secret);
		long timeStep = System.currentTimeMillis() / 1000L / period;
		return hotp(key, timeStep, digits, toJavaMacAlgorithm(algorithm));
	}

	/** Seconds remaining in the current time window. Drives the countdown bar. */
	public static int getSecondsRemaining(int period) {
		long epochSeconds = System.currentTimeMillis() / 1000L;
		return (int) (period - (epochSeconds % period));
	}

	// --- HOTP core — RFC 4226 §5 --------------------------------------------

	private static String hotp(byte[] key, long counter,
			int digits, String macAlgorithm) throws Exception {
		// Counter as 8-byte big-endian
		byte[] msg = new byte[8];
		long tmp = counter;
		for (int i = 7; i >= 0; i--) {
			msg[i] = (byte) (tmp & 0xFF);
			tmp >>= 8;
		}

		Mac mac = Mac.getInstance(macAlgorithm);
		mac.init(new SecretKeySpec(key, macAlgorithm));
		byte[] hash = mac.doFinal(msg);

		// Dynamic truncation
		int offset = hash[hash.length - 1] & 0x0F;
		int p = ((hash[offset] & 0x7F) << 24)
				| ((hash[offset + 1] & 0xFF) << 16)
				| ((hash[offset + 2] & 0xFF) << 8)
				| (hash[offset + 3] & 0xFF);

		int otp = p % (int) Math.pow(10, digits);
		return String.format("%0" + digits + "d", otp);
	}

	// --- Algorithm mapping --------------------------------------------------

	/**
	 * Maps the algorithm string from the otpauth:// URI to the Java Mac name.
	 * The URI uses "SHA1", "SHA256", "SHA512".
	 * Java's Mac.getInstance() expects "HmacSHA1", "HmacSHA256", "HmacSHA512".
	 */
	private static String toJavaMacAlgorithm(String otpAlgorithm) {
		if (otpAlgorithm == null)
			return "HmacSHA1";
		switch (otpAlgorithm.toUpperCase()) {
			case "SHA256":
				return "HmacSHA256";
			case "SHA512":
				return "HmacSHA512";
			default:
				return "HmacSHA1";
		}
	}

	// --- Base32 decoder — RFC 4648 §6 ---------------------------------------

	static byte[] decodeBase32(String input) {
		String s = input.toUpperCase().replaceAll("[=\\s]", "");
		byte[] out = new byte[(s.length() * 5) / 8];
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
