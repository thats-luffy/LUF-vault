package com.example

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import com.example.data.AppDatabase
import com.example.data.VaultFileRepository
import com.example.security.SecurityManager
import com.example.ui.*
import com.example.ui.theme.MyApplicationTheme

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Edge-to-Edge full bleed support
        enableEdgeToEdge()

        // 2. Initialize database and repository
        val db = AppDatabase.getDatabase(applicationContext)
        val repo = VaultFileRepository(db.vaultFileDao())

        // 3. Setup ViewModel with custom factory
        val viewModel = ViewModelProvider(
            this,
            VaultViewModelFactory(repo, applicationContext)
        )[VaultViewModel::class.java]

        // 4. Read launcher intent data and check setup status
        val isSetupComplete = SecurityManager.isSetupCompleted(this)
        val action = intent?.action
        val dataUri = intent?.data

        val isLauncherLaunch = action == Intent.ACTION_MAIN

        // 5. REQUIREMENT: Subsequent launch from launcher must exit immediately with NO UI
        if (isSetupComplete && isLauncherLaunch) {
            finish()
            return
        }

        // 6. Verify key file hash if opened via Action View (Open With)
        if (isSetupComplete && action == Intent.ACTION_VIEW && dataUri != null) {
            viewModel.verifyIntentKeyFile(dataUri)
        } else if (isSetupComplete) {
            viewModel.isKeyFileVerified = false
        }

        setContent {
            // Apply themes dynamically (Light/Dark/System)
            val appTheme = viewModel.appThemeSetting
            val isDark = when (appTheme) {
                "light" -> false
                "dark" -> true
                else -> isSystemInDarkTheme()
            }
            com.example.ui.isAppDarkTheme = isDark

            // Secure Flag content protection (Screenshots & Recordings)
            LaunchedEffect(viewModel.preventScreenshots, viewModel.preventRecordings) {
                if (viewModel.preventScreenshots || viewModel.preventRecordings) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }

            // Silent exit when opened key file is invalid or incorrect
            LaunchedEffect(viewModel.authErrorMsg) {
                if (isSetupComplete && action == Intent.ACTION_VIEW && dataUri != null && viewModel.authErrorMsg != null) {
                    finish()
                }
            }

            MyApplicationTheme(darkTheme = isDark) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var currentScreen by remember { mutableStateOf(if (!isSetupComplete) "setup" else "auth") }

                    when (currentScreen) {
                        "setup" -> {
                            SetupWizardScreen(viewModel = viewModel) {
                                // Setup finishes -> close immediately
                                finish()
                            }
                        }
                        "auth" -> {
                            if (action != Intent.ACTION_VIEW || dataUri == null) {
                                DirectAttemptScreen()
                            } else if (viewModel.authErrorMsg == "فایل کلید نامعتبر است!") {
                                InvalidKeyFileScreen()
                            } else if (viewModel.isKeyFileVerified) {
                                LoginScreen(viewModel = viewModel) {
                                    currentScreen = "dashboard"
                                }
                            } else {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = ElectricBlue)
                                }
                            }
                        }
                        "dashboard" -> {
                            var showSettings by remember { mutableStateOf(false) }
                            if (showSettings) {
                                SettingsScreen(viewModel = viewModel) {
                                    showSettings = false
                                }
                            } else {
                                DashboardScreen(viewModel = viewModel) {
                                    showSettings = true
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
