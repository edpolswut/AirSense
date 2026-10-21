package br.airsense.security

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.inject.Inject

/**
 * Abstração do hash de senha. Hoje é SHA-256 com salt; para trocar por PBKDF2
 * no futuro basta criar outra implementação e mudar o binding no RepositoryModule.
 */
interface PasswordHasher {
    fun generateSalt(): String
    fun hash(password: String, salt: String): String
    fun verify(password: String, salt: String, expectedHash: String): Boolean
}

class Sha256PasswordHasher @Inject constructor() : PasswordHasher {

    private val secureRandom = SecureRandom()

    override fun generateSalt(): String {
        val bytes = ByteArray(SALT_SIZE_BYTES)
        secureRandom.nextBytes(bytes)
        return Base64.getEncoder().encodeToString(bytes)
    }

    override fun hash(password: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(Base64.getDecoder().decode(salt))
        digest.update(password.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(digest.digest())
    }

    override fun verify(password: String, salt: String, expectedHash: String): Boolean {
        val actual = hash(password, salt).toByteArray(Charsets.UTF_8)
        val expected = expectedHash.toByteArray(Charsets.UTF_8)
        // Comparação em tempo constante.
        return MessageDigest.isEqual(actual, expected)
    }

    private companion object {
        const val SALT_SIZE_BYTES = 16
    }
}