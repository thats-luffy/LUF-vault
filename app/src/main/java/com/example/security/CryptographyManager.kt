package com.example.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.InputStream
import java.io.OutputStream
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

object CryptographyManager {
    private const val ANDROID_KEY_STORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "LufVaultFileKeyEncryptionKey"
    private const val TRANSFORMATION = "${KeyProperties.KEY_ALGORITHM_AES}/${KeyProperties.BLOCK_MODE_CBC}/${KeyProperties.ENCRYPTION_PADDING_PKCS7}"

    init {
        getOrCreateSecretKey()
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
        keyStore.getKey(KEY_ALIAS, null)?.let { return it as SecretKey }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
            .build()

        keyGenerator.init(keyGenParameterSpec)
        return keyGenerator.generateKey()
    }

    /**
     * Encrypts the source stream and writes the IV (16 bytes) followed by the ciphertext to the dest stream.
     */
    fun encrypt(inputStream: InputStream, outputStream: OutputStream) {
        val key = getOrCreateSecretKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)

        val iv = cipher.iv
        outputStream.write(iv) // Write IV (16 bytes)

        CipherOutputStream(outputStream, cipher).use { cos ->
            val buffer = ByteArray(1024 * 1024) // 1MB buffer
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                cos.write(buffer, 0, bytesRead)
            }
        }
    }

    /**
     * Decrypts the source stream by reading the IV first, initializing the cipher, and writing plain text to the dest stream.
     */
    fun decrypt(inputStream: InputStream, outputStream: OutputStream) {
        val key = getOrCreateSecretKey()
        val iv = ByteArray(16)
        var bytesRead = 0
        while (bytesRead < 16) {
            val result = inputStream.read(iv, bytesRead, 16 - bytesRead)
            if (result == -1) break
            bytesRead += result
        }

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(iv))

        CipherInputStream(inputStream, cipher).use { cis ->
            val buffer = ByteArray(1024 * 1024) // 1MB buffer
            var bytesReadIn: Int
            while (cis.read(buffer).also { bytesReadIn = it } != -1) {
                outputStream.write(buffer, 0, bytesReadIn)
            }
        }
    }
}
