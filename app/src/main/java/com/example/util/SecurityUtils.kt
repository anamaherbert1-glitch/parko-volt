package com.example.util

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object SecurityUtils {
    private val random = SecureRandom()
    private const val PBKDF2_ITERATIONS = 24_000
    private const val KEYSTORE_ALIAS = "parko_local_aes_key"
    private const val GCM_TAG_BITS = 128
    private const val IV_LEN_BYTES = 12

    // Ephemeral fallback key used strictly when AndroidKeyStore is unavailable (e.g. JVM unit tests).
    @Volatile
    private var jvmTestKey: SecretKey? = null

    fun generateSalt(): String {
        val bytes = ByteArray(16)
        random.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun hashPassword(password: String, saltHex: String): String {
        return hashSecret(password, saltHex)
    }

    fun hashSecret(secret: String, saltHex: String): String {
        val saltBytes = saltHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        val spec = PBEKeySpec(secret.toCharArray(), saltBytes, PBKDF2_ITERATIONS, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val hash = factory.generateSecret(spec).encoded
        return hash.joinToString("") { "%02x".format(it) }
    }

    fun verifyPassword(password: String, saltHex: String, expectedHash: String): Boolean {
        return verifySecret(password, saltHex, expectedHash)
    }

    fun verifySecret(secret: String, saltHex: String, expectedHash: String): Boolean {
        if (secret.isBlank() || saltHex.isBlank() || expectedHash.isBlank()) return false
        val actual = hashSecret(secret, saltHex)
        if (actual.length != expectedHash.length) return false
        var diff = 0
        for (i in actual.indices) {
            diff = diff or (actual[i].code xor expectedHash[i].code)
        }
        return diff == 0
    }

    /**
     * Generates a random 6-digit confidential withdrawal code (100000..999999).
     */
    fun generateWithdrawalSecretCode(): String {
        val num = 100_000 + random.nextInt(900_000)
        return num.toString()
    }

    /**
     * Generates an opaque, unpredictable token for QR codes so no PII (name, phone, plate)
     * or secret withdrawal code is embedded directly inside the QR payload.
     */
    fun generateOpaqueQrToken(): String {
        val bytes = ByteArray(12)
        random.nextBytes(bytes)
        val hex = bytes.joinToString("") { "%02X".format(it) }
        return "PKO-$hex"
    }

    fun generateSessionReference(): String {
        val num = 1000 + random.nextInt(9000)
        val chars = ('A'..'Z')
        val c1 = chars.random()
        val c2 = chars.random()
        return "PK-$num$c1$c2"
    }

    private fun getOrCreateAesKey(): SecretKey {
        return try {
            val ks = KeyStore.getInstance("AndroidKeyStore")
            ks.load(null)
            val existing = ks.getKey(KEYSTORE_ALIAS, null) as? SecretKey
            if (existing != null) return existing
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            val spec = KeyGenParameterSpec.Builder(
                KEYSTORE_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
            keyGenerator.init(spec)
            keyGenerator.generateKey()
        } catch (_: Throwable) {
            jvmTestKey ?: synchronized(this) {
                jvmTestKey ?: run {
                    val keyBytes = ByteArray(32)
                    random.nextBytes(keyBytes)
                    SecretKeySpec(keyBytes, "AES").also { jvmTestKey = it }
                }
            }
        }
    }

    fun encryptSensitiveText(plainText: String): String? {
        if (plainText.isBlank()) return null
        return try {
            val key = getOrCreateAesKey()
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val iv = cipher.iv
            val cipherBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            val combined = ByteArray(iv.size + cipherBytes.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(cipherBytes, 0, combined, iv.size, cipherBytes.size)
            Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (_: Throwable) {
            null
        }
    }

    fun decryptSensitiveText(cipherPayload: String?): String? {
        if (cipherPayload.isNullOrBlank()) return null
        return try {
            val combined = Base64.decode(cipherPayload, Base64.NO_WRAP)
            if (combined.size <= IV_LEN_BYTES) return null
            val iv = combined.copyOfRange(0, IV_LEN_BYTES)
            val cipherBytes = combined.copyOfRange(IV_LEN_BYTES, combined.size)
            val key = getOrCreateAesKey()
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
            String(cipher.doFinal(cipherBytes), Charsets.UTF_8)
        } catch (_: Throwable) {
            null
        }
    }

    fun normalizeTogoPhone(raw: String): String {
        val digits = raw.filter { it.isDigit() }
        val local8 = when {
            digits.startsWith("228") && digits.length >= 11 -> digits.substring(3, 11)
            digits.length == 8 -> digits
            digits.length > 8 -> digits.takeLast(8)
            else -> return raw.trim()
        }
        return "+228 ${local8.substring(0, 2)} ${local8.substring(2, 4)} ${local8.substring(4, 6)} ${local8.substring(6, 8)}"
    }

    fun isValidTogoPhone(raw: String): Boolean {
        val digits = raw.filter { it.isDigit() }
        val local8 = if (digits.startsWith("228") && digits.length == 11) {
            digits.substring(3)
        } else {
            digits
        }
        return local8.length == 8 && (local8.startsWith("9") || local8.startsWith("7") || local8.startsWith("2"))
    }
}
