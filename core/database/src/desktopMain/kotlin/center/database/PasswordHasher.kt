package center.database

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

internal class PasswordHasher {
    private val random = SecureRandom()
    private val encoder = Base64.getEncoder().withoutPadding()
    private val decoder = Base64.getDecoder()

    fun hash(password: String): String {
        val salt = ByteArray(16).also(random::nextBytes)
        return "$ALGORITHM\$${encoder.encodeToString(salt)}\$${encoder.encodeToString(derive(password, salt))}"
    }

    fun verify(
        password: String,
        stored: String,
    ): Boolean {
        val parts = stored.split('$')
        if (parts.size != 3 || parts[0] != ALGORITHM) return false
        val (salt, expected) =
            try {
                decoder.decode(parts[1]) to decoder.decode(parts[2])
            } catch (_: IllegalArgumentException) {
                return false
            }
        return salt.size == 16 && expected.size == 32 && MessageDigest.isEqual(expected, derive(password, salt))
    }

    private fun derive(
        password: String,
        salt: ByteArray,
    ): ByteArray {
        val characters = password.toCharArray()
        val spec = PBEKeySpec(characters, salt, 600_000, 256)
        characters.fill('\u0000')
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private companion object {
        const val ALGORITHM = "pbkdf2-sha256-v1"
    }
}
