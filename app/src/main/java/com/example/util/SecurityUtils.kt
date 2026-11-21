package com.example.util

import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object SecurityUtils {
    private val random = SecureRandom()

    fun generateSalt(): String {
        val bytes = ByteArray(16)
        random.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun hashPassword(password: String, saltHex: String): String {
        val saltBytes = saltHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        val spec = PBEKeySpec(password.toCharArray(), saltBytes, 12000, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val hash = factory.generateSecret(spec).encoded
        return hash.joinToString("") { "%02x".format(it) }
    }

    fun verifyPassword(password: String, saltHex: String, expectedHash: String): Boolean {
        if (saltHex.isBlank() || expectedHash.isBlank()) return false
        val actual = hashPassword(password, saltHex)
        if (actual.length != expectedHash.length) return false
        var diff = 0
        for (i in actual.indices) {
            diff = diff or (actual[i].code xor expectedHash[i].code)
        }
        return diff == 0
    }

    /**
     * Generates an opaque, unpredictable token for QR codes so no PII (name, phone, plate)
     * is embedded directly inside the QR payload.
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
