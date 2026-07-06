package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vault_files")
data class VaultFile(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val originalName: String,
    val encryptedFileName: String,
    val fileSize: Long,
    val category: String, // Images, Videos, Audio, Documents, Archives, APK, Others
    val mimeType: String,
    val addedDate: Long = System.currentTimeMillis(),
    val originalPath: String? = null
)
