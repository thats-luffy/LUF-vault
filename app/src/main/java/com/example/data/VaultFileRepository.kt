package com.example.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.example.security.CryptographyManager
import com.example.utils.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.util.UUID

class VaultFileRepository(private val dao: VaultFileDao) {

    fun getAllFiles(): Flow<List<VaultFile>> = dao.getAllFiles()

    fun getFilesByCategory(category: String): Flow<List<VaultFile>> = dao.getFilesByCategory(category)

    suspend fun getFileById(id: Int): VaultFile? = dao.getFileById(id)

    suspend fun deleteFile(context: Context, vaultFile: VaultFile) = withContext(Dispatchers.IO) {
        // Delete the physical encrypted file
        val file = File(context.filesDir, vaultFile.encryptedFileName)
        if (file.exists()) {
            file.delete()
        }
        // Delete metadata from room
        dao.deleteFile(vaultFile)
    }

    /**
     * Imports a file from Uri, encrypts it, stores it in app private storage, and adds to database.
     * Optionally deletes the original file (move operation).
     */
    suspend fun importFile(
        context: Context,
        uri: Uri,
        operationType: String, // "COPY" or "MOVE"
        onProgress: (Int) -> Unit
    ): VaultFile? = withContext(Dispatchers.IO) {
        try {
            val originalName = FileUtils.getFileName(context, uri)
            val size = FileUtils.getFileSize(context, uri)
            val mimeType = FileUtils.getMimeType(context, uri, originalName)
            val category = FileUtils.getCategoryFromMime(mimeType, originalName)

            // 1. Check storage availability
            val available = FileUtils.getAvailableStorage(context)
            if (available < size * 1.1) { // 10% safety margin for encryption overhead
                return@withContext null
            }

            // 2. Prepare encrypted target file
            val encryptedFileName = "enc_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.bin"
            val destFile = File(context.filesDir, encryptedFileName)

            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            if (inputStream == null) return@withContext null

            // 3. Encrypt and save
            destFile.outputStream().use { outputStream ->
                inputStream.use { stream ->
                    CryptographyManager.encrypt(stream, outputStream)
                }
            }
            onProgress(100)

            // 4. Create entity
            val vaultFile = VaultFile(
                originalName = originalName,
                encryptedFileName = encryptedFileName,
                fileSize = size,
                category = category,
                mimeType = mimeType,
                originalPath = uri.toString()
            )

            // 5. Save to database
            val id = dao.insertFile(vaultFile)

            // 6. If MOVE, delete the original file
            if (operationType == "MOVE") {
                try {
                    context.contentResolver.delete(uri, null, null)
                } catch (e: Exception) {
                    Log.e("VaultFileRepository", "Could not delete original file via resolver", e)
                    // Fallback: if file uri, delete directly
                    if (uri.scheme == "file") {
                        val originalFile = uri.path?.let { File(it) }
                        if (originalFile != null && originalFile.exists()) {
                            originalFile.delete()
                        }
                    }
                }
            }

            return@withContext vaultFile.copy(id = id.toInt())
        } catch (e: Exception) {
            Log.e("VaultFileRepository", "Error importing file", e)
            return@withContext null
        }
    }

    /**
     * Decrypts the file and exports it back to Downloads/LUF/.
     * Optionally deletes the encrypted file inside vault if move operation.
     */
    suspend fun exportFile(
        context: Context,
        vaultFile: VaultFile,
        deleteAfterExport: Boolean,
        onProgress: (Int) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val encryptedFile = File(context.filesDir, vaultFile.encryptedFileName)
            if (!encryptedFile.exists()) return@withContext false

            val success: Boolean
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Export using MediaStore on Q+
                val contentValues = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, vaultFile.originalName)
                    put(MediaStore.Downloads.MIME_TYPE, vaultFile.mimeType)
                    put(MediaStore.Downloads.RELATIVE_PATH, "Download/LUF")
                }
                val resolver = context.contentResolver
                val targetUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (targetUri == null) return@withContext false

                resolver.openOutputStream(targetUri)?.use { outputStream ->
                    encryptedFile.inputStream().use { inputStream ->
                        CryptographyManager.decrypt(inputStream, outputStream)
                    }
                }
                success = true
            } else {
                // Direct file system access on pre-Q
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val lufDir = File(downloadsDir, "LUF")
                if (!lufDir.exists()) {
                    lufDir.mkdirs()
                }
                val targetFile = File(lufDir, vaultFile.originalName)
                targetFile.outputStream().use { outputStream ->
                    encryptedFile.inputStream().use { inputStream ->
                        CryptographyManager.decrypt(inputStream, outputStream)
                    }
                }
                success = true
            }

            onProgress(100)

            if (success && deleteAfterExport) {
                deleteFile(context, vaultFile)
            }

            return@withContext success
        } catch (e: Exception) {
            Log.e("VaultFileRepository", "Error exporting file", e)
            return@withContext false
        }
    }
}
