package live.mehiz.mpvkt.library

import java.security.MessageDigest
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Locked folders: a folder the owner marks private disappears from the library unless
 * they unlock it with a PIN. The rules live here, Android-free, so they are testable.
 *
 * The PIN is never stored: only a salted PBKDF2 hash. Verification compares in constant
 * time, and verification failures are indistinguishable from a wrong PIN by design.
 */
@Suppress("TooManyFunctions")  // small, pure, single-purpose helpers
object LibraryLock {

  /** Shortest PIN we accept. Four digits is the smallest that is still usable on a phone. */
  const val MIN_PIN_LENGTH = 4
  const val MAX_PIN_LENGTH = 12

  /** PBKDF2 rounds. High enough to be real, low enough to unlock quickly on a phone. */
  const val ITERATIONS = 20_000

  /** Preferred first; the SHA1 variant is the fallback for API 21-25. */
  private val ALGORITHMS = listOf(
    "PBKDF2WithHmacSHA256" to PREFIX_SHA256,
    "PBKDF2WithHmacSHA1" to PREFIX_SHA1,
  )
  private const val KEY_BITS = 256
  private const val PREFIX_SHA256 = "sha256:"
  private const val PREFIX_SHA1 = "sha1:"

  /** Digits only, reasonable length: a PIN, not a password. */
  fun isValidPin(pin: String): Boolean =
    pin.length in MIN_PIN_LENGTH..MAX_PIN_LENGTH && pin.all { it.isDigit() }

  /** A fresh random salt, hex encoded, for a new PIN. */
  fun newSalt(bytes: ByteArray = randomBytes(16)): String = bytes.toHex()

  fun randomBytes(size: Int): ByteArray {
    val out = ByteArray(size)
    java.security.SecureRandom().nextBytes(out)
    return out
  }

  /**
   * Salted PBKDF2 hash of [pin], hex encoded, **prefixed with the PRF that produced it**
   * (`sha256:` or `sha1:`). Empty pin or salt yields "".
   *
   * PBKDF2WithHmacSHA256 only exists on API 26+, so older devices use the SHA1 PRF. The
   * prefix is what makes verification match the algorithm that created the hash instead of
   * silently comparing SHA1 output against a SHA256 expectation.
   */
  fun hash(pin: String, salt: String, iterations: Int = ITERATIONS): String {
    if (pin.isEmpty() || salt.isEmpty() || iterations <= 0) return ""
    for ((algorithm, prefix) in ALGORITHMS) {
      rawHash(pin, salt, iterations, algorithm)?.let { return prefix + it }
    }
    return ""
  }

  /** Hex digest from [algorithm], or null when the platform lacks that PRF. */
  private fun rawHash(pin: String, salt: String, iterations: Int, algorithm: String): String? {
    val chars = pin.toCharArray()
    val spec = PBEKeySpec(chars, salt.toByteArray(Charsets.UTF_8), iterations, KEY_BITS)
    return try {
      SecretKeyFactory.getInstance(algorithm).generateSecret(spec).encoded.toHex()
    } catch (e: java.security.NoSuchAlgorithmException) {
      null
    } finally {
      spec.clearPassword()
      chars.fill('\u0000')
    }
  }

  /**
   * True when [pin] produces [expected] under [salt]. Constant-time comparison.
   *
   * [expected] may be prefixed (`sha256:…` / `sha1:…`) or a legacy bare hex digest, which
   * is treated as SHA-256 — that is what every stored hash created before the prefix was
   * introduced is.
   */
  fun verify(pin: String, salt: String, expected: String, iterations: Int = ITERATIONS): Boolean {
    if (expected.isEmpty() || pin.isEmpty() || salt.isEmpty()) return false
    val algorithm: String
    val wantHex: String
    when {
      expected.startsWith(PREFIX_SHA256) -> {
        algorithm = "PBKDF2WithHmacSHA256"
        wantHex = expected.removePrefix(PREFIX_SHA256)
      }
      expected.startsWith(PREFIX_SHA1) -> {
        algorithm = "PBKDF2WithHmacSHA1"
        wantHex = expected.removePrefix(PREFIX_SHA1)
      }
      else -> {
        algorithm = "PBKDF2WithHmacSHA256"
        wantHex = expected
      }
    }
    if (wantHex.isEmpty()) return false
    val actual = rawHash(pin, salt, iterations, algorithm) ?: return false
    return hashEquals(actual, wantHex)
  }

  /**
   * Locked folders that are not currently unlocked must vanish from the library.
   * Unlocking is per session, so this recomputes as soon as the session expires.
   */
  fun exclusions(lockedFolders: Set<String>, unlockedFolders: Set<String>): LibraryExclusions =
    LibraryExclusions(folders = lockedFolders - unlockedFolders)

  /** Whether an unlocked folder should be locked again. 0 (or less) means "never". */
  fun shouldAutoLock(lastUnlockAt: Long, now: Long, autoLockMinutes: Int): Boolean {
    if (autoLockMinutes <= 0) return false
    if (lastUnlockAt <= 0) return false
    if (now < lastUnlockAt) return false
    return now - lastUnlockAt >= autoLockMinutes * 60_000L
  }

  /** A folder needs a PIN set before it can be locked. */
  fun canLock(pinHash: String): Boolean = pinHash.isNotEmpty()

  private fun ByteArray.toHex(): String {
    val out = StringBuilder(size * 2)
    for (b in this) {
      val v = b.toInt() and 0xFF
      out.append(HEX[v ushr 4]).append(HEX[v and 0x0F])
    }
    return out.toString()
  }

  private val HEX = "0123456789abcdef".toCharArray()

  /** Exposed for the PIN screen: does this string look like a stored hash? */
  fun looksLikeHash(value: String): Boolean {
    val hex = value.removePrefix(PREFIX_SHA256).removePrefix(PREFIX_SHA1)
    return hex.length == KEY_BITS / 4 && hex.all { it in '0'..'9' || it in 'a'..'f' }
  }

  /** Constant-time compare used by tests and by any caller holding two hashes. */
  fun hashEquals(a: String, b: String): Boolean {
    if (a.length != b.length) return false
    var diff = 0
    for (i in a.indices) diff = diff or (a[i].code xor b[i].code)
    return diff == 0
  }

  /** Digest helper kept for callers that want a quick fingerprint of a folder name. */
  fun folderFingerprint(folder: String): String =
    MessageDigest.getInstance("SHA-256").digest(folder.toByteArray(Charsets.UTF_8)).toHex().take(16)
}
