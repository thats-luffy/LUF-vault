package com.example.utils

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import java.io.File

object FileUtils {
    fun getFileName(context: Context, uri: Uri): String {
        var name = ""
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) {
                        name = it.getString(index)
                    }
                }
            }
        }
        if (name.isEmpty()) {
            name = uri.path ?: ""
            val cut = name.lastIndexOf('/')
            if (cut != -1) {
                name = name.substring(cut + 1)
            }
        }
        return name
    }

    fun getFileSize(context: Context, uri: Uri): Long {
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val index = it.getColumnIndex(OpenableColumns.SIZE)
                    if (index != -1) {
                        return it.getLong(index)
                    }
                }
            }
        } else if (uri.scheme == "file") {
            val path = uri.path
            if (path != null) {
                val file = File(path)
                if (file.exists()) {
                    return file.length()
                }
            }
        }
        return 0L
    }

    fun getCategoryFromExtension(extension: String): String {
        val ext = extension.lowercase()
        return when {
            ext in listOf("jpg", "jpeg", "png", "gif", "bmp", "webp", "heic", "heif") -> "Images"
            ext in listOf("mp4", "mkv", "avi", "mov", "3gp", "webm", "flv", "wmv") -> "Videos"
            ext in listOf("mp3", "wav", "ogg", "m4a", "flac", "aac") -> "Audio"
            ext in listOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "epub") -> "Documents"
            ext in listOf("zip", "rar", "7z", "tar", "gz") -> "Archives"
            ext == "apk" -> "APK"
            else -> "Others"
        }
    }

    fun getCategoryFromMime(mimeType: String?, name: String): String {
        val extension = getExtension(name)
        if (extension.isNotEmpty()) {
            return getCategoryFromExtension(extension)
        }
        val mime = mimeType?.lowercase() ?: return "Others"
        return when {
            mime.startsWith("image/") -> "Images"
            mime.startsWith("video/") -> "Videos"
            mime.startsWith("audio/") -> "Audio"
            mime.contains("pdf") || mime.contains("document") || mime.contains("sheet") || mime.contains("presentation") || mime.startsWith("text/") -> "Documents"
            mime.contains("zip") || mime.contains("rar") || mime.contains("compressed") || mime.contains("archive") -> "Archives"
            mime == "application/vnd.android.package-archive" -> "APK"
            else -> "Others"
        }
    }

    fun getExtension(name: String): String {
        val lastDot = name.lastIndexOf('.')
        return if (lastDot != -1 && lastDot < name.length - 1) {
            name.substring(lastDot + 1).lowercase()
        } else {
            ""
        }
    }

    fun getMimeType(context: Context, uri: Uri, name: String): String {
        val extension = getExtension(name)
        var mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        if (mime == null) {
            mime = context.contentResolver.getType(uri)
        }
        return mime ?: "application/octet-stream"
    }

    fun formatFileSize(size: Long): String {
        if (size <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        val formattedValue = String.format("%.2f", size / Math.pow(1024.0, digitGroups.toDouble()))
        return "$formattedValue ${units[digitGroups]}"
    }

    fun getAvailableStorage(context: Context): Long {
        val file = context.filesDir
        return file.usableSpace
    }
}
