package com.example.ui

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.VaultFile
import com.example.data.VaultFileRepository
import com.example.security.SecurityManager
import com.example.utils.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import com.example.utils.Localization
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream

class VaultViewModel(
    private val repository: VaultFileRepository,
    private val context: Context
) : ViewModel() {

    // Setup state
    var setupPassword by mutableStateOf("")
    var setupConfirmPassword by mutableStateOf("")
    var setupBiometricEnabled by mutableStateOf(false)
    var setupKeyFileUri by mutableStateOf<Uri?>(null)
    var setupKeyFileName by mutableStateOf("")
    var setupKeyFileHash by mutableStateOf("")

    // Auth state
    var inputPassword by mutableStateOf("")
    var isAuthenticated by mutableStateOf(false)
    var authErrorMsg by mutableStateOf<String?>(null)
    var isKeyFileVerified by mutableStateOf(false)
    var openedKeyFileName by mutableStateOf("")

    // Lockout state
    var remainingLockTimeText by mutableStateOf("")
    var isLockedOut by mutableStateOf(false)

    // Gallery state
    var searchQuery by mutableStateOf("")
    var sortBy by mutableStateOf("DATE") // DATE, NAME, SIZE
    var activeTab by mutableStateOf("Images")

    // Multi-select
    var selectedFileIds by mutableStateOf(setOf<Int>())

    // Import state
    var importUris by mutableStateOf<List<Uri>>(emptyList())
    var importRequiredStorage by mutableStateOf(0L)
    var importAvailableStorage by mutableStateOf(0L)
    var importProgress by mutableStateOf(0)
    var isImporting by mutableStateOf(false)
    var importOption by mutableStateOf("COPY") // COPY, MOVE

    // Export state
    var exportProgress by mutableStateOf(0)
    var isExporting by mutableStateOf(false)

    // Settings state
    var preventScreenshots by mutableStateOf(SecurityManager.getPreventScreenshots(context))
    var preventRecordings by mutableStateOf(SecurityManager.getPreventRecordings(context))
    var biometricSettingEnabled by mutableStateOf(SecurityManager.getBiometricEnabled(context))
    var lockTimeoutMinutes by mutableStateOf(SecurityManager.getLockTimeoutMinutes(context))
    var appThemeSetting by mutableStateOf(SecurityManager.getTheme(context)) // system, light, dark
    var customLauncherName by mutableStateOf(SecurityManager.getCustomAppName(context))
    var appLanguageSetting by mutableStateOf(SecurityManager.getLanguage(context))
    var selectedLauncherAlias by mutableStateOf(SecurityManager.getSelectedAlias(context))

    // Database flow of all files
    private val _allFiles = repository.getAllFiles()

    // Reactive Storage flow: sums sizes of all secure files inside the vault
    val vaultStorageUsage: StateFlow<Long> = _allFiles.map { files ->
        files.sumOf { it.fileSize }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0L
    )

    val deviceUsableSpace: Long
        get() = context.filesDir.usableSpace

    val deviceTotalSpace: Long
        get() = context.filesDir.totalSpace
    
    // Combine and search/sort vault files
    val filteredFiles: StateFlow<List<VaultFile>> = combine(
        _allFiles,
        snapshotFlow { searchQuery },
        snapshotFlow { sortBy }
    ) { files, query, sort ->
        val filtered = if (query.isEmpty()) {
            files
        } else {
            files.filter { it.originalName.contains(query, ignoreCase = true) }
        }

        when (sort) {
            "NAME" -> filtered.sortedBy { it.originalName.lowercase() }
            "SIZE" -> filtered.sortedByDescending { it.fileSize }
            else -> filtered.sortedByDescending { it.addedDate }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val handler = Handler(Looper.getMainLooper())
    private val lockoutTicker = object : Runnable {
        override fun run() {
            checkLockout()
            if (isLockedOut) {
                handler.postDelayed(this, 1000)
            }
        }
    }

    init {
        checkLockout()
    }

    private fun checkLockout() {
        val remaining = SecurityManager.getRemainingLockTime(context)
        isLockedOut = remaining > 0L
        if (isLockedOut) {
            val minutes = (remaining / 1000) / 60
            val seconds = (remaining / 1000) % 60
            remainingLockTimeText = String.format("%02d:%02d", minutes, seconds)
        } else {
            remainingLockTimeText = ""
        }
    }

    fun startLockoutTicker() {
        handler.removeCallbacks(lockoutTicker)
        handler.post(lockoutTicker)
    }

    fun stopLockoutTicker() {
        handler.removeCallbacks(lockoutTicker)
    }

    override fun onCleared() {
        super.onCleared()
        stopLockoutTicker()
    }

    // --- SETUP WIZARD ---

    fun onKeyFileSelected(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val name = FileUtils.getFileName(context, uri)
                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    val bytes = inputStream.readBytes()
                    val hash = SecurityManager.hashBytes(bytes)
                    setupKeyFileUri = uri
                    setupKeyFileName = name
                    setupKeyFileHash = hash
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun completeSetup(onSuccess: () -> Unit) {
        val isEn = appLanguageSetting == "en"
        if (setupPassword.length < 6) {
            authErrorMsg = if (isEn) "Password must be at least 6 characters" else "رمز عبور باید حداقل ۶ کاراکتر باشد"
            return
        }
        if (setupPassword != setupConfirmPassword) {
            authErrorMsg = if (isEn) "Passwords do not match" else "رمز عبور با تکرار آن مطابقت ندارد"
            return
        }
        if (setupKeyFileHash.isEmpty()) {
            authErrorMsg = if (isEn) "Please select your key file first" else "لطفاً ابتدا فایل کلید خود را انتخاب کنید"
            return
        }

        SecurityManager.savePassword(context, setupPassword)
        SecurityManager.saveKeyFileFingerprint(context, setupKeyFileHash)
        SecurityManager.setKeyFileName(context, setupKeyFileName)
        SecurityManager.setBiometricEnabled(context, setupBiometricEnabled)
        SecurityManager.setSetupCompleted(context, true)
        authErrorMsg = null
        onSuccess()
    }

    // --- INTENT KEY FILE VERIFICATION ---

    fun verifyIntentKeyFile(uri: Uri) {
        openedKeyFileName = FileUtils.getFileName(context, uri)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    val bytes = inputStream.readBytes()
                    val hash = SecurityManager.hashBytes(bytes)
                    val isMatch = SecurityManager.verifyKeyFileFingerprint(context, hash)
                    isKeyFileVerified = isMatch
                    if (!isMatch) {
                        authErrorMsg = "فایل کلید نامعتبر است!"
                    } else {
                        authErrorMsg = null
                    }
                } else {
                    isKeyFileVerified = false
                    authErrorMsg = "امکان خواندن فایل کلید وجود ندارد!"
                }
            } catch (e: Exception) {
                isKeyFileVerified = false
                authErrorMsg = "خطا در خواندن فایل کلید!"
            }
        }
    }

    // --- LOGIN AUTHENTICATION ---

    fun authenticateWithPassword(onSuccess: () -> Unit) {
        if (isLockedOut) return

        if (SecurityManager.verifyPassword(context, inputPassword)) {
            SecurityManager.resetFailedAttempts(context)
            isAuthenticated = true
            authErrorMsg = null
            onSuccess()
        } else {
            val attempts = SecurityManager.recordFailedAttempt(context)
            if (attempts >= 5) {
                isLockedOut = true
                startLockoutTicker()
            } else {
                authErrorMsg = "رمز عبور اشتباه است! ${5 - attempts} تلاش باقی‌مانده."
            }
        }
    }

    fun authenticateBiometrically(onSuccess: () -> Unit) {
        if (isLockedOut) return
        isAuthenticated = true
        authErrorMsg = null
        onSuccess()
    }

    // --- GALLERY ACTIONS ---

    fun selectImportFiles(uris: List<Uri>) {
        importUris = uris
        calculateRequiredImportStorage()
    }

    private fun calculateRequiredImportStorage() {
        viewModelScope.launch(Dispatchers.IO) {
            var totalSize = 0L
            importUris.forEach { uri ->
                totalSize += FileUtils.getFileSize(context, uri)
            }
            importRequiredStorage = totalSize
            importAvailableStorage = FileUtils.getAvailableStorage(context)
        }
    }

    fun startImport(onComplete: () -> Unit) {
        if (importRequiredStorage > importAvailableStorage) return
        isImporting = true
        importProgress = 0

        viewModelScope.launch {
            val total = importUris.size
            var completedCount = 0
            importUris.forEach { uri ->
                val imported = repository.importFile(context, uri, importOption) { _ -> }
                completedCount++
                importProgress = ((completedCount.toFloat() / total) * 100).toInt()
            }
            isImporting = false
            importProgress = 100
            importUris = emptyList()
            onComplete()
        }
    }

    fun toggleSelectFile(id: Int) {
        selectedFileIds = if (selectedFileIds.contains(id)) {
            selectedFileIds - id
        } else {
            selectedFileIds + id
        }
    }

    fun clearSelection() {
        selectedFileIds = emptySet()
    }

    fun deleteSelectedFiles() {
        val idsToDelete = selectedFileIds.toList()
        viewModelScope.launch {
            idsToDelete.forEach { id ->
                val file = repository.getFileById(id)
                if (file != null) {
                    repository.deleteFile(context, file)
                }
            }
            clearSelection()
        }
    }

    fun exportSelectedFiles(onComplete: () -> Unit) {
        val idsToExport = selectedFileIds.toList()
        isExporting = true
        exportProgress = 0

        viewModelScope.launch {
            val total = idsToExport.size
            var completedCount = 0
            idsToExport.forEach { id ->
                val file = repository.getFileById(id)
                if (file != null) {
                    repository.exportFile(context, file, deleteAfterExport = false) { _ -> }
                }
                completedCount++
                exportProgress = ((completedCount.toFloat() / total) * 100).toInt()
            }
            isExporting = false
            exportProgress = 100
            clearSelection()
            onComplete()
        }
    }

    // --- SETTINGS ACTIONS ---

    fun updatePreventScreenshots(prevent: Boolean) {
        preventScreenshots = prevent
        SecurityManager.setPreventScreenshots(context, prevent)
    }

    fun updatePreventRecordings(prevent: Boolean) {
        preventRecordings = prevent
        SecurityManager.setPreventRecordings(context, prevent)
    }

    fun updateBiometricSetting(enable: Boolean) {
        biometricSettingEnabled = enable
        SecurityManager.setBiometricEnabled(context, enable)
    }

    fun updateLockTimeout(mins: Int) {
        lockTimeoutMinutes = mins
        SecurityManager.setLockTimeoutMinutes(context, mins)
    }

    fun updateTheme(theme: String) {
        appThemeSetting = theme
        SecurityManager.setTheme(context, theme)
    }

    fun updateCustomLauncherName(name: String) {
        customLauncherName = name
        SecurityManager.setCustomAppName(context, name)
    }

    fun updateLanguage(lang: String) {
        appLanguageSetting = lang
        SecurityManager.setLanguage(context, lang)
    }

    fun updateSelectedLauncherAlias(alias: String) {
        selectedLauncherAlias = alias
        SecurityManager.setSelectedAlias(context, alias)
        SecurityManager.applySelectedAlias(context, alias)
    }

    fun changePassword(newPass: String) {
        SecurityManager.savePassword(context, newPass)
    }

    fun updateSecretKeyFile(hash: String, name: String) {
        SecurityManager.saveKeyFileFingerprint(context, hash)
        SecurityManager.setKeyFileName(context, name)
    }
}

// Composable snapshot helper
private fun <T> snapshotFlow(block: () -> T): kotlinx.coroutines.flow.Flow<T> =
    androidx.compose.runtime.snapshotFlow(block)

class VaultViewModelFactory(
    private val repository: VaultFileRepository,
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VaultViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return VaultViewModel(repository, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
